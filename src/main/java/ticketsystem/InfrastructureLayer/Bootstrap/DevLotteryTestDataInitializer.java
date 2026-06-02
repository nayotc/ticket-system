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

    /*
     * eventId=1 is created by DevEventDataInitializer with a valid ticket-selection map,
     * so it is the safest event for checking that a pre-sale winner can continue
     * into the ticket selection page.
     */
    private static final long PRE_SALE_WINNER_EVENT_ID = 1L;

    /*
     * eventId=30 appears in the Home featured events as "פסטיבל אורות הלילה",
     * so we use it to demonstrate lottery registration from the event card.
     */
    private static final long LOTTERY_REGISTRATION_EVENT_ID = 30L;

    private static final String REGISTRATION_TEST_USERNAME = "test@test.com";
    private static final String REGISTRATION_TEST_PASSWORD = "123456";

    private static final String WINNER_USERNAME = "owner@test.com";
    private static final String WINNER_PASSWORD = "123456";

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
            ensurePreSaleWinnerEvent();
            ensureLotteryRegistrationEvent();

            long preSaleLotteryId = ensurePreSaleLotteryAndRunDraw();
            ensureOpenRegistrationLottery();

            printWinnerCode(preSaleLotteryId);

            System.out.println("Registration test username: " + REGISTRATION_TEST_USERNAME);
            System.out.println("Registration test password: " + REGISTRATION_TEST_PASSWORD);
            System.out.println("Winner username: " + WINNER_USERNAME);
            System.out.println("Winner password: " + WINNER_PASSWORD);
            System.out.println("Use the printed/generated code from this run.");

        } catch (Exception e) {
            System.out.println("Failed to initialize temporary lottery test data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void ensurePreSaleWinnerEvent() {
        Event existing = eventRepository.getEventById(PRE_SALE_WINNER_EVENT_ID);

        if (existing != null) {
            existing.setStatus(Event.eventStatus.ACTIVE);
            existing.setSaleStatus(SaleStatus.PRE_SALE);
            existing.setTrafficThreshold(100L);
            existing.setRate(4.99);
            eventRepository.updateEvent(existing);

            System.out.println("Updated pre-sale lottery test event: " + PRE_SALE_WINNER_EVENT_ID);
            return;
        }

        Event event = new Event(
                PRE_SALE_WINNER_EVENT_ID,
                LocalDateTime.of(2026, 6, 5, 20, 45),
                "אירוע בדיקת מכירה מוקדמת",
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

        System.out.println("Created pre-sale lottery test event: " + PRE_SALE_WINNER_EVENT_ID);
    }

    private void ensureLotteryRegistrationEvent() {
        Event existing = eventRepository.getEventById(LOTTERY_REGISTRATION_EVENT_ID);

        if (existing != null) {
            existing.setStatus(Event.eventStatus.ACTIVE);
            existing.setSaleStatus(SaleStatus.NOT_STARTED);
            existing.setTrafficThreshold(100L);
            existing.setRate(5.0);
            eventRepository.updateEvent(existing);

            System.out.println("Updated lottery registration test event: " + LOTTERY_REGISTRATION_EVENT_ID);
            return;
        }

        Event event = new Event(
                LOTTERY_REGISTRATION_EVENT_ID,
                LocalDateTime.of(2026, 10, 24, 21, 0),
                "פסטיבל אורות הלילה",
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

        System.out.println("Created lottery registration test event: " + LOTTERY_REGISTRATION_EVENT_ID);
    }

    private long ensurePreSaleLotteryAndRunDraw() {
        Member winner = userRepository.getMemberByUsername(WINNER_USERNAME);

        if (winner == null) {
            throw new IllegalStateException("Test winner user was not found: " + WINNER_USERNAME);
        }

        Lottery lottery = lotteryRepository.findByEventId(PRE_SALE_WINNER_EVENT_ID);
        boolean isNewLottery = false;

        if (lottery == null) {
            lottery = new Lottery(
                    lotteryRepository.generateNextLotteryId(),
                    PRE_SALE_WINNER_EVENT_ID,
                    1
            );
            isNewLottery = true;
        }

        lottery.setWinnersNumber(1);
        lottery.setStatus(LotteryStatus.OPEN);
        safelyRegisterMember(lottery, winner.getId());

        if (isNewLottery) {
            lotteryRepository.addLottery(lottery);
            System.out.println("Created pre-sale lottery for event: " + PRE_SALE_WINNER_EVENT_ID);
        } else {
            lotteryRepository.update(lottery);
            System.out.println("Updated existing pre-sale lottery for event: " + PRE_SALE_WINNER_EVENT_ID);
        }

        String founderToken = loginAsFounder();

        lotteryService.closeLotteryRegistration(founderToken, lottery.getLotteryId(), COMPANY_ID);
        lotteryService.conductLotteryDraw(founderToken, lottery.getLotteryId(), COMPANY_ID);

        System.out.println("Conducted lottery draw for event: " + PRE_SALE_WINNER_EVENT_ID);

        return lottery.getLotteryId();
    }

    private void ensureOpenRegistrationLottery() {
        Lottery lottery = lotteryRepository.findByEventId(LOTTERY_REGISTRATION_EVENT_ID);
        boolean isNewLottery = false;

        if (lottery == null) {
            lottery = new Lottery(
                    lotteryRepository.generateNextLotteryId(),
                    LOTTERY_REGISTRATION_EVENT_ID,
                    5
            );
            isNewLottery = true;
        }

        lottery.setWinnersNumber(5);
        lottery.setStatus(LotteryStatus.OPEN);

        if (isNewLottery) {
            lotteryRepository.addLottery(lottery);
            System.out.println("Created open registration lottery for event: " + LOTTERY_REGISTRATION_EVENT_ID);
        } else {
            lotteryRepository.update(lottery);
            System.out.println("Updated open registration lottery for event: " + LOTTERY_REGISTRATION_EVENT_ID);
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

    private void printWinnerCode(long lotteryId) {
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
                System.out.println("Event id: " + PRE_SALE_WINNER_EVENT_ID);
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
                1L,
                "במה מרכזית",
                new Pair<>(25, 4),
                new Pair<>(20, 5)
        ));

        event.getMap().addElement(new SeatingArea(
                2L,
                "אזור ישיבה",
                new Pair<>(30, 12),
                new Pair<>(10, 5),
                10,
                20
        ));

        event.getMap().addElement(new StandingArea(
                3L,
                "אזור עמידה",
                new Pair<>(25, 19),
                new Pair<>(20, 10),
                500
        ));

        event.getMap().addElement(new Element(
                4L,
                "בר",
                new Pair<>(44, 30),
                new Pair<>(5, 4)
        ));

        event.getMap().addElement(new Element(
                5L,
                "יציאת חירום",
                new Pair<>(15, 11),
                new Pair<>(4, 3)
        ));

        event.getMap().addElement(new Element(
                6L,
                "עזרה ראשונה",
                new Pair<>(38, 30),
                new Pair<>(5, 4)
        ));
    }

    private void addDefaultCatalogMap(Event event) {
        event.getMap().addElement(new Element(
                1L,
                "במה מרכזית",
                new Pair<>(8, 2),
                new Pair<>(8, 3)
        ));

        event.getMap().addElement(new SeatingArea(
                2L,
                "יציע ישיבה",
                new Pair<>(4, 7),
                new Pair<>(10, 6),
                6,
                10
        ));

        event.getMap().addElement(new StandingArea(
                3L,
                "רחבת עמידה",
                new Pair<>(16, 7),
                new Pair<>(8, 8),
                120
        ));

        event.getMap().addElement(new Element(
                4L,
                "כניסה",
                new Pair<>(1, 1),
                new Pair<>(3, 3)
        ));
    }
}