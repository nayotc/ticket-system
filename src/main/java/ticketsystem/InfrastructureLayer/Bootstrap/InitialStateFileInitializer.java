package ticketsystem.InfrastructureLayer.Bootstrap;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
import ticketsystem.ApplicationLayer.MembershipService;
import ticketsystem.ApplicationLayer.UserService;
import ticketsystem.DTO.CompanyDTO;
import ticketsystem.DTO.DiscountDTO;
import ticketsystem.DTO.DiscountPolicyDTO;
import ticketsystem.DTO.Event.EventMapDTO;
import ticketsystem.DTO.Event.IMapElementDTO;
import ticketsystem.DTO.Event.PairDTO;
import ticketsystem.DTO.Event.SeatDTO;
import ticketsystem.DTO.Event.SeatingAreaDTO;
import ticketsystem.DTO.Event.StandingAreaDTO;
import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.discount.DiscountCompositionType;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.event.EventCategory;
import ticketsystem.DomainLayer.event.EventLocation;
import ticketsystem.DomainLayer.user.Member;
import ticketsystem.DomainLayer.user.Permission;

/**
 * Initializes the system from an external initial-state file for Version 3.
 *
 * <p>The initializer intentionally creates the requested state through the
 * application services, so the same use-case validations, permissions, role
 * approvals, policy updates, and event-map validations are exercised during
 * startup.</p>
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
    private final MembershipService membershipService;
    private final EventService eventService;
    private final IEventRepository eventRepository;
    private final ObjectMapper objectMapper;

    @Value("${ticketsystem.initial-state.file:classpath:initial-state-v3.json}")
    private String initialStateFile;

    public InitialStateFileInitializer(
            ResourceLoader resourceLoader,
            UserService userService,
            CompanyService companyService,
            MembershipService membershipService,
            EventService eventService,
            IEventRepository eventRepository
    ) {
        this.resourceLoader = resourceLoader;
        this.userService = userService;
        this.companyService = companyService;
        this.membershipService = membershipService;
        this.eventService = eventService;
        this.eventRepository = eventRepository;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    /**
     * Loads the configured initial-state file and executes the required use-case sequence:
     *
     * <ol>
     *     <li>Register users u1, u2, u3, u4.</li>
     *     <li>Log in u1.</li>
     *     <li>u1 opens production company p1.</li>
     *     <li>u1 appoints u2 as owner.</li>
     *     <li>u2 logs in and confirms the appointment.</li>
     *     <li>u2 appoints u3 as manager with venue-layout permission only.</li>
     *     <li>u3 logs in and confirms the appointment.</li>
     *     <li>u2 adds event e1 and defines the required event map.</li>
     *     <li>u2 adds the company coupon discount sale123.</li>
     *     <li>All logged-in users are logged out.</li>
     * </ol>
     *
     * @param args command-line arguments supplied by Spring Boot
     * @throws Exception if the initial-state file cannot be read or if any use-case fails
     */
    @Override
    public void run(String... args) throws Exception {
        InitialStateConfig config = loadConfig();

        System.out.println("Starting Version 3 initial-state bootstrap from: " + initialStateFile);

        registerUsers(config.users());

        String u1Token = login(config.user("u1"));
        Long companyId = createOrFindCompany(u1Token, config.company().name());

        membershipService.requestOwnerAssignment(u1Token, companyId, config.user("u2").username());
        String u2Token = login(config.user("u2"));
        membershipService.approveAssignment(u2Token, companyId);

        Set<Permission> u3Permissions = EnumSet.of(Permission.CONFIGURE_HALL_AND_MAP);
        membershipService.requestManagerAssignment(u2Token, companyId, config.user("u3").username(), u3Permissions);
        String u3Token = login(config.user("u3"));
        membershipService.approveAssignment(u3Token, companyId);

        Long eventId = createOrFindEvent(u2Token, companyId, config.event());
        defineEventMap(u2Token, eventId, config.event());

        addCompanyCouponDiscount(u2Token, companyId, config.couponDiscount());

        logoutAndExit(u1Token);
        logoutAndExit(u2Token);
        logoutAndExit(u3Token);

        System.out.println(
                "Version 3 initial-state bootstrap completed successfully. "
                        + "companyId=" + companyId + ", eventId=" + eventId
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
            boolean alreadyExists = userService.getAllUsers()
                    .stream()
                    .anyMatch(existing -> existing.getUserName().equalsIgnoreCase(user.username()));

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

    private Long createOrFindCompany(String u1Token, String companyName) throws Exception {
        Optional<CompanyDTO> existingCompany = companyService.getAllCompanies()
                .stream()
                .filter(company -> company.getName().equalsIgnoreCase(companyName))
                .findFirst();

        if (existingCompany.isPresent()) {
            System.out.println("Initial-state company already exists, reusing: " + companyName);
            return existingCompany.get().getId();
        }

        CompanyDTO createdCompany = companyService.createProductionCompany(u1Token, companyName);
        System.out.println("Initial-state company created: " + companyName + " [ID: " + createdCompany.getId() + "]");
        return createdCompany.getId();
    }

    private Long createOrFindEvent(String u2Token, Long companyId, EventConfig eventConfig) {
        Optional<Event> existingEvent = eventRepository.getAllEvents()
                .stream()
                .filter(event -> companyId.equals(event.getCompanyId()))
                .filter(event -> eventConfig.name().equalsIgnoreCase(event.getName()))
                .findFirst();

        if (existingEvent.isPresent()) {
            System.out.println("Initial-state event already exists, reusing: " + eventConfig.name());
            return existingEvent.get().getId();
        }

        Long eventId = eventService.insertEvent(
                u2Token,
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
        return eventId;
    }

    private void defineEventMap(String u2Token, Long eventId, EventConfig eventConfig) {
        List<IMapElementDTO> elements = new ArrayList<>();

        StandingZoneConfig standingZone = eventConfig.standingZone();
        elements.add(new StandingAreaDTO(
                0L,
                standingZone.name(),
                new PairDTO<>(0, 0),
                new PairDTO<>(6, 5),
                "StandingArea",
                false,
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
                seatingZone.rows(),
                seatingZone.columns(),
                List.<SeatDTO>of()
        ));

        EventMapDTO mapDTO = new EventMapDTO(
                new PairDTO<>(eventConfig.mapHeight(), eventConfig.mapWidth()),
                elements,
                false
        );

        eventService.defineEventMap(u2Token, eventId, mapDTO);

        System.out.println(
                "Initial-state event map defined for eventId=" + eventId
                        + ". Standing capacity=" + standingZone.capacity()
                        + ", seating layout=" + seatingZone.rows() + "x" + seatingZone.columns()
        );
    }

    private void addCompanyCouponDiscount(
            String u2Token,
            Long companyId,
            CouponDiscountConfig couponConfig
    ) throws Exception {
        DiscountDTO coupon = new DiscountDTO();
        coupon.setName(couponConfig.name());
        coupon.setType("COUPON");
        coupon.setCouponCode(couponConfig.code());
        coupon.setPercentage(BigDecimal.valueOf(couponConfig.percentage()));
        coupon.setEndTime(LocalDateTime.parse(couponConfig.expiresAt()));

        DiscountPolicyDTO policyDTO = new DiscountPolicyDTO(
                DiscountCompositionType.valueOf(couponConfig.compositionType()),
                List.of(coupon)
        );

        companyService.setCompanyDiscountPolicy(u2Token, companyId, policyDTO);

        System.out.println(
                "Initial-state company coupon discount configured: code="
                        + couponConfig.code()
                        + ", percentage=" + couponConfig.percentage()
                        + "%"
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
            EventConfig event,
            CouponDiscountConfig couponDiscount
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
            long capacity
    ) {
    }

    private record SeatingZoneConfig(
            String name,
            int rows,
            int columns
    ) {
    }

    private record CouponDiscountConfig(
            String name,
            String code,
            int percentage,
            String compositionType,
            String expiresAt
    ) {
    }
}