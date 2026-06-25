package ticketsystem.InfrastructureLayer.Bootstrap;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import ticketsystem.ApplicationLayer.CompanyService;
import ticketsystem.ApplicationLayer.EventService;
import ticketsystem.ApplicationLayer.UserService;
import ticketsystem.DTO.CompanyDTO;
import ticketsystem.DTO.Event.EventMapDTO;
import ticketsystem.DTO.Event.IMapElementDTO;
import ticketsystem.DTO.Event.PairDTO;
import ticketsystem.DTO.Event.SeatDTO;
import ticketsystem.DTO.Event.SeatingAreaDTO;
import ticketsystem.DTO.Event.StandingAreaDTO;
import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.event.EventCategory;
import ticketsystem.DomainLayer.event.EventLocation;

/**
 * Initializes the system from an external initial-state file for the final version scenario.
 *
 * <p>The initializer intentionally creates the requested state through the
 * application services, so the same use-case validations, permissions, and
 * event-map validations are exercised during startup.</p>
 *
 * <p>The initializer is disabled by default and is activated only when
 * ticketsystem.initial-state.enabled=true is configured. This prevents tests
 * and regular local runs from accidentally polluting the database.</p>
 */
@Component
@Order(100)
@ConditionalOnProperty(
        prefix = "ticketsystem.initial-state",
        name = "enabled",
        havingValue = "true"
)
public class InitialStateFileInitializer implements CommandLineRunner {

    private final ResourceLoader resourceLoader;
    private final UserService userService;
    private final CompanyService companyService;
    private final EventService eventService;
    private final IEventRepository eventRepository;
    private final ObjectMapper objectMapper;

    @Value("${ticketsystem.initial-state.file:classpath:initial-state-v3.json}")
    private String initialStateFile;

    public InitialStateFileInitializer(
            ResourceLoader resourceLoader,
            UserService userService,
            CompanyService companyService,
            EventService eventService,
            IEventRepository eventRepository
    ) {
        this.resourceLoader = resourceLoader;
        this.userService = userService;
        this.companyService = companyService;
        this.eventService = eventService;
        this.eventRepository = eventRepository;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    /**
     * Loads the configured initial-state file and executes the required final-version scenario:
     *
     * <ol>
     *     <li>Register User1.</li>
     *     <li>Register User2.</li>
     *     <li>Register User3, who is over 18 years old.</li>
     *     <li>Register User4.</li>
     *     <li>Log in User1.</li>
     *     <li>Create company C1.</li>
     *     <li>Create event E1 with 10 standing tickets and 10 seats.</li>
     *     <li>Create event E2 with 10 standing tickets and 10 seats, each ticket costing 10 dollars.</li>
     *     <li>Log out User1.</li>
     * </ol>
     *
     * <p>The system admin is expected to be created by the existing system-admin bootstrap
     * configuration before or during application initialization.</p>
     *
     * @param args command-line arguments supplied by Spring Boot
     * @throws Exception if the initial-state file cannot be read or if any use-case fails
     */
    @Override
    public void run(String... args) throws Exception {
        InitialStateConfig config = loadConfig();

        System.out.println("Starting final-version initial-state bootstrap from: " + initialStateFile);

        registerUsers(config.users());

        UserConfig user1 = config.user("user1");
        String user1Token = login(user1);

        CompanyBootstrapResult companyResult = createOrFindCompany(user1Token, config.company().name());
        Long companyId = companyResult.companyId();

        List<Long> eventIds = new ArrayList<>();
        for (EventConfig eventConfig : config.events()) {
            EventBootstrapResult eventResult = createOrFindEvent(user1Token, companyId, eventConfig);
            eventIds.add(eventResult.eventId());

            if (eventResult.created()) {
                defineEventMap(user1Token, eventResult.eventId(), eventConfig);
            } else {
                System.out.println(
                        "Initial-state event already existed, skipping map definition "
                                + "to avoid overwriting existing event map and area prices: "
                                + eventConfig.name()
                );
            }
        }

        logoutAndExit(user1Token);

        System.out.println(
                "Final-version initial-state bootstrap completed. companyId="
                        + companyId
                        + ", eventIds="
                        + eventIds
        );
    }

    private InitialStateConfig loadConfig() throws Exception {
        Resource resource = resourceLoader.getResource(initialStateFile);
        if (!resource.exists()) {
            throw new IllegalStateException("Initial-state file was not found: " + initialStateFile);
        }

        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, InitialStateConfig.class);
        }
    }

    private void registerUsers(List<UserConfig> users) {
        for (UserConfig user : users) {
            boolean alreadyExists = userService.findMemberByUsername(user.username()).isPresent();

            if (alreadyExists) {
                System.out.println("Initial-state user already exists, skipping signup: " + user.username());
                continue;
            }

            String guestToken = userService.visitSystem();
            userService.signUp(
                    guestToken,
                    user.username(),
                    user.password(),
                    user.fullName(),
                    user.phone(),
                    LocalDate.parse(user.birthDate())
            );
            userService.exit(guestToken);

            System.out.println("Initial-state user registered: " + user.username());
        }
    }

    private String login(UserConfig user) {
        String guestToken = userService.visitSystem();
        return userService.login(guestToken, user.username(), user.password());
    }

    private CompanyBootstrapResult createOrFindCompany(String userToken, String companyName) throws Exception {
        Optional<CompanyDTO> existingCompany = companyService.getAllCompanies()
                .stream()
                .filter(company -> company.getName().equalsIgnoreCase(companyName))
                .findFirst();

        if (existingCompany.isPresent()) {
            System.out.println("Initial-state company already exists, reusing: " + companyName);
            return new CompanyBootstrapResult(existingCompany.get().getId(), false);
        }

        CompanyDTO createdCompany = companyService.createProductionCompany(userToken, companyName);
        System.out.println("Initial-state company created: " + companyName + " [ID: " + createdCompany.getId() + "]");
        return new CompanyBootstrapResult(createdCompany.getId(), true);
    }

    private EventBootstrapResult createOrFindEvent(String userToken, Long companyId, EventConfig eventConfig) {
        Optional<Event> existingEvent = eventRepository.getAllEvents()
                .stream()
                .filter(event -> companyId.equals(event.getCompanyId()))
                .filter(event -> eventConfig.name().equalsIgnoreCase(event.getName()))
                .findFirst();

        if (existingEvent.isPresent()) {
            System.out.println("Initial-state event already exists, reusing: " + eventConfig.name());
            return new EventBootstrapResult(existingEvent.get().getId(), false);
        }

        Long eventId = eventService.insertEvent(
                userToken,
                eventConfig.name(),
                companyId,
                LocalDateTime.parse(eventConfig.date()),
                EventLocation.valueOf(eventConfig.location()),
                eventConfig.trafficThreshold(),
                EventCategory.valueOf(eventConfig.category()),
                eventConfig.artist(),
                BigDecimal.valueOf(eventConfig.eventBasePrice()),
                eventConfig.mapHeight(),
                eventConfig.mapWidth()
        );

        System.out.println("Initial-state event created: " + eventConfig.name() + " [ID: " + eventId + "]");
        return new EventBootstrapResult(eventId, true);
    }

    /**
     * Defines the event map required by the final-version initial-state scenario.
     *
     * <p>Each event contains one standing area and one seating area. The standing
     * capacity and the seating layout are loaded from the external initial-state file.
     * For example, a seating layout of 2x5 creates exactly 10 seats.</p>
     *
     * @param userToken token of User1, who owns the production company
     * @param eventId ID of the created event
     * @param eventConfig event configuration loaded from the initial-state file
     */
    private void defineEventMap(String userToken, Long eventId, EventConfig eventConfig) {
        List<IMapElementDTO> elements = new ArrayList<>();

        StandingZoneConfig standingZone = eventConfig.standingZone();
        elements.add(new StandingAreaDTO(
                0L,
                standingZone.name(),
                new PairDTO<>(0, 0),
                new PairDTO<>(6, 5),
                "StandingArea",
                false,
                BigDecimal.valueOf(standingZone.price()),
                standingZone.capacity(),
                0L,
                0L
        ));

        SeatingZoneConfig seatingZone = eventConfig.seatingZone();
        elements.add(new SeatingAreaDTO(
                0L,
                seatingZone.name(),
                new PairDTO<>(0, 6),
                new PairDTO<>(10, 10),
                "SeatingArea",
                false,
                BigDecimal.valueOf(seatingZone.price()),
                seatingZone.rows(),
                seatingZone.columns(),
                List.<SeatDTO>of()
        ));

        EventMapDTO mapDTO = new EventMapDTO(
                new PairDTO<>(eventConfig.mapHeight(), eventConfig.mapWidth()),
                elements,
                false
        );

        eventService.defineEventMap(userToken, eventId, mapDTO);

        System.out.println(
                "Initial-state event map defined for eventId=" + eventId
                        + ". Standing capacity=" + standingZone.capacity()
                        + ", standing price=" + standingZone.price()
                        + ", seating layout=" + seatingZone.rows() + "x" + seatingZone.columns()
                        + ", seating price=" + seatingZone.price()
        );
    }

    private void logoutAndExit(String memberToken) {
        if (memberToken == null || memberToken.isBlank()) {
            return;
        }

        String guestToken = userService.logOut(memberToken);
        userService.exit(guestToken);
    }

    private record InitialStateConfig(
            List<UserConfig> users,
            CompanyConfig company,
            List<EventConfig> events
    ) {
        private UserConfig user(String id) {
            return users.stream()
                    .filter(user -> user.id().equalsIgnoreCase(id))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Missing user in initial-state file: " + id));
        }
    }

    private record UserConfig(
            String id,
            String username,
            String password,
            String fullName,
            String phone,
            String birthDate
    ) {
    }

    private record CompanyConfig(
            String name
    ) {
    }

    private record EventConfig(
            String name,
            String artist,
            String date,
            String location,
            String category,
            Long trafficThreshold,
            int eventBasePrice,
            int mapHeight,
            int mapWidth,
            StandingZoneConfig standingZone,
            SeatingZoneConfig seatingZone
    ) {
    }

    private record StandingZoneConfig(
            String name,
            long capacity,
            int price
    ) {
    }

    private record SeatingZoneConfig(
            String name,
            int rows,
            int columns,
            int price
    ) {
    }

    private record CompanyBootstrapResult(Long companyId, boolean created) {
    }

    private record EventBootstrapResult(Long eventId, boolean created) {
    }
}