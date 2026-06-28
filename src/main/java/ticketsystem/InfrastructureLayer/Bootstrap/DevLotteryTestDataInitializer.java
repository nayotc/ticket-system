package ticketsystem.InfrastructureLayer.Bootstrap;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import ticketsystem.ApplicationLayer.LotteryService;
import ticketsystem.ApplicationLayer.UserService;
import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.IRepository.ILotteryRepository;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.DomainLayer.event.Element;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.event.EventCategory;
import ticketsystem.DomainLayer.event.EventLocation;
import ticketsystem.DomainLayer.event.Pair;
import ticketsystem.DomainLayer.event.SaleStatus;
import ticketsystem.DomainLayer.event.SeatingArea;
import ticketsystem.DomainLayer.event.StandingArea;
import ticketsystem.DomainLayer.lottery.Lottery;
import ticketsystem.DomainLayer.lottery.LotteryRegistration;
import ticketsystem.DomainLayer.lottery.LotteryStatus;
import ticketsystem.DomainLayer.user.Member;

@Component
@Profile("lottery-test")
@Order(30)
public class DevLotteryTestDataInitializer implements CommandLineRunner {

    private static final long COMPANY_ID = 1L;
    private static final long OWNER_ID = 1L;

    private static final String PRE_SALE_WINNER_EVENT_NAME = "אירוע בדיקת מכירה מוקדמת";
    private static final String LOTTERY_REGISTRATION_EVENT_NAME = "פסטיבל אורות הלילה";

    private static final String REGISTRATION_TEST_USERNAME = "test@test.com";
    private static final String REDACTED_PASSWORD = "<redacted>";

    private static final String WINNER_USERNAME = "owner@test.com";

    private static final String FOUNDER_USERNAME = "founder@test.com";
    private static final String FOUNDER_PASSWORD = "123456";

    private final IEventRepository eventRepository;
    private final ILotteryRepository lotteryRepository;
    private final IUserRepository userRepository;
    private final UserService userService;
    private final LotteryService lotteryService;

    public DevLotteryTestDataInitializer(
            IEventRepository eventRepository,
            ILotteryRepository lotteryRepository,
            IUserRepository userRepository,
            UserService userService,
            LotteryService lotteryService
    ) {
        this.eventRepository = eventRepository;
        this.lotteryRepository = lotteryRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.lotteryService = lotteryService;
    }

    @Override
    public void run(String... args) {
        try {
            Event preSaleWinnerEvent = ensurePreSaleWinnerEvent();
            Event lotteryRegistrationEvent = ensureLotteryRegistrationEvent();

            long preSaleLotteryId = ensurePreSaleLotteryAndRunDraw(preSaleWinnerEvent.getId());
            ensureOpenRegistrationLottery(lotteryRegistrationEvent.getId());

            printWinnerCode(preSaleWinnerEvent.getId(), preSaleLotteryId);

            System.out.println("Registration test username: " + REGISTRATION_TEST_USERNAME);
            System.out.println("Registration test password: " + REDACTED_PASSWORD);
            System.out.println("Winner username: " + WINNER_USERNAME);
            System.out.println("Winner password: " + REDACTED_PASSWORD);
            System.out.println("Use the printed/generated code from this run.");

        } catch (Exception e) {
            System.out.println("Failed to initialize temporary lottery test data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Event ensurePreSaleWinnerEvent() {
        Event existing = eventRepository.getAllEvents().stream()
                .filter(event -> COMPANY_ID == event.getCompanyId())
                .filter(event -> PRE_SALE_WINNER_EVENT_NAME.equals(event.getName()))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            existing.setStatus(Event.eventStatus.ACTIVE);
            existing.setSaleStatus(SaleStatus.PRE_SALE);
            existing.setTrafficThreshold(100L);
            existing.setRate(4.99);
            eventRepository.updateEvent(existing);

            System.out.println("Updated pre-sale lottery test event: " + existing.getId());
            return existing;
        }

        Event event = new Event(
                LocalDateTime.of(2026, 6, 5, 20, 45),
                PRE_SALE_WINNER_EVENT_NAME,
                COMPANY_ID,
                OWNER_ID,
                EventLocation.TEL_AVIV,
                100L,
                EventCategory.CONCERT,
                "Lottery Test Artist",
                new BigDecimal("350.00"),
                new Pair<>(40, 60)
        );

        addValidTicketSelectionMap(event);
        event.setStatus(Event.eventStatus.ACTIVE);
        event.setSaleStatus(SaleStatus.PRE_SALE);
        event.setRate(4.99);

        eventRepository.addEvent(event);

        System.out.println("Created pre-sale lottery test event: " + event.getId());
        return event;
    }

    private Event ensureLotteryRegistrationEvent() {
        Event existing = eventRepository.getAllEvents().stream()
                .filter(event -> LOTTERY_REGISTRATION_EVENT_NAME.equals(event.getName()))
                .filter(event -> EventLocation.TEL_AVIV.equals(event.getLocation()))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            existing.setStatus(Event.eventStatus.ACTIVE);
            existing.setSaleStatus(SaleStatus.NOT_STARTED);
            existing.setTrafficThreshold(100L);
            existing.setRate(5.0);
            eventRepository.updateEvent(existing);

            System.out.println("Updated lottery registration test event: " + existing.getId());
            return existing;
        }

        Event event = new Event(
                LocalDateTime.of(2026, 10, 24, 21, 0),
                LOTTERY_REGISTRATION_EVENT_NAME,
                2L,
                OWNER_ID,
                EventLocation.TEL_AVIV,
                100L,
                EventCategory.CONCERT,
                "The Night Lights",
                new BigDecimal("249"),
                new Pair<>(20, 30)
        );

        addDefaultCatalogMap(event);
        event.setStatus(Event.eventStatus.ACTIVE);
        event.setSaleStatus(SaleStatus.NOT_STARTED);
        event.setRate(5.0);

        eventRepository.addEvent(event);

        System.out.println("Created lottery registration test event: " + event.getId());
        return event;
    }

    private long ensurePreSaleLotteryAndRunDraw(Long eventId) {
        if (eventId == null) {
            throw new IllegalStateException("Pre-sale event id was not generated");
        }

        Member winner = userRepository.getMemberByUsername(WINNER_USERNAME);

        if (winner == null) {
            throw new IllegalStateException("Test winner user was not found: " + WINNER_USERNAME);
        }

        Lottery lottery = lotteryRepository.findByEventId(eventId);
        boolean isNewLottery = false;

        if (lottery == null) {
            lottery = new Lottery(
                    eventId,
                    1
            );
            isNewLottery = true;
        }

        lottery.setWinnersNumber(1);
        lottery.setStatus(LotteryStatus.OPEN);
        safelyRegisterMember(lottery, winner.getId());

        if (isNewLottery) {
            lotteryRepository.addLottery(lottery);
            System.out.println("Created pre-sale lottery for event: " + eventId);
        } else {
            lotteryRepository.update(lottery);
            System.out.println("Updated existing pre-sale lottery for event: " + eventId);
        }

        String founderToken = loginAsFounder();

        lotteryService.closeLotteryRegistration(founderToken, lottery.getLotteryId(), COMPANY_ID);
        lotteryService.conductLotteryDraw(founderToken, lottery.getLotteryId(), COMPANY_ID);

        System.out.println("Conducted lottery draw for event: " + eventId);

        return lottery.getLotteryId();
    }

    private void ensureOpenRegistrationLottery(Long eventId) {
        if (eventId == null) {
            throw new IllegalStateException("Lottery registration event id was not generated");
        }

        Lottery lottery = lotteryRepository.findByEventId(eventId);
        boolean isNewLottery = false;

        if (lottery == null) {
            lottery = new Lottery(
                    eventId,
                    5
            );
            isNewLottery = true;
        }

        lottery.setWinnersNumber(5);
        lottery.setStatus(LotteryStatus.OPEN);

        if (isNewLottery) {
            lotteryRepository.addLottery(lottery);
            System.out.println("Created open registration lottery for event: " + eventId);
        } else {
            lotteryRepository.update(lottery);
            System.out.println("Updated open registration lottery for event: " + eventId);
        }
    }

    private String loginAsFounder() {
        String guestToken = userService.visitSystem();
        return userService.login(guestToken, FOUNDER_USERNAME, FOUNDER_PASSWORD);
    }

    private void safelyRegisterMember(Lottery lottery, long memberId) {
        try {
            lottery.registerMember(memberId);
        } catch (IllegalArgumentException | IllegalStateException ignored) {
            // The member may already be registered from a previous run.
        }
    }

    private void printWinnerCode(Long eventId, long lotteryId) {
        Member winner = userRepository.getMemberByUsername(WINNER_USERNAME);

        if (winner == null) {
            System.out.println("Cannot print winner code: winner user was not found.");
            return;
        }

        Lottery lottery = lotteryRepository.findById(lotteryId);

        if (lottery == null) {
            System.out.println("Cannot print winner code: lottery was not found.");
            return;
        }

        for (LotteryRegistration registration : lottery.getRegistrations()) {
            if (registration.getMemberId() == winner.getId() && registration.isWinner()) {
                System.out.println("====================================================");
                System.out.println("LOTTERY TEST WINNER CODE");
                System.out.println("Event id: " + eventId);
                System.out.println("Winner username: " + WINNER_USERNAME);
                System.out.println("Winner code: " + registration.getAuthCode());
                System.out.println("====================================================");
                return;
            }
        }

        System.out.println("Winner code was not found for user: " + WINNER_USERNAME);
    }

    private void addValidTicketSelectionMap(Event event) {
        event.getMap().addElement(new Element(
                "במה מרכזית",
                new Pair<>(25, 4),
                new Pair<>(20, 5)
        ));

        event.getMap().addElement(new SeatingArea(
                "אזור ישיבה",
                new Pair<>(30, 12),
                new Pair<>(10, 5),
                10,
                20,
                new BigDecimal("350.00")
        ));

        event.getMap().addElement(new StandingArea(
                "אזור עמידה",
                new Pair<>(25, 19),
                new Pair<>(20, 10),
                500,
                new BigDecimal("250.00")
        ));

        event.getMap().addElement(new Element(
                "בר",
                new Pair<>(44, 30),
                new Pair<>(5, 4)
        ));

        event.getMap().addElement(new Element(
                "יציאת חירום",
                new Pair<>(15, 11),
                new Pair<>(4, 3)
        ));

        event.getMap().addElement(new Element(
                "עזרה ראשונה",
                new Pair<>(38, 30),
                new Pair<>(5, 4)
        ));
    }

    private void addDefaultCatalogMap(Event event) {
        event.getMap().addElement(new Element(
                "במה מרכזית",
                new Pair<>(8, 2),
                new Pair<>(8, 3)
        ));

        event.getMap().addElement(new SeatingArea(
                "יציע ישיבה",
                new Pair<>(4, 7),
                new Pair<>(10, 6),
                6,
                10,
                new BigDecimal("200.00")
        ));

        event.getMap().addElement(new StandingArea(
                "רחבת עמידה",
                new Pair<>(16, 7),
                new Pair<>(8, 8),
                120,
                new BigDecimal("150.00")
        ));

        event.getMap().addElement(new Element(
                "כניסה",
                new Pair<>(1, 1),
                new Pair<>(3, 3)
        ));
    }
}