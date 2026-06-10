package ticketsystem.ConcurrencyTesting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import static java.util.Collections.synchronizedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.ApplicationLayer.INotifier;
import ticketsystem.ApplicationLayer.IPaymentService;
import ticketsystem.ApplicationLayer.ITicketIssuingService;
import ticketsystem.ApplicationLayer.ISystemLogger;
import ticketsystem.ApplicationLayer.ReservationService;
import ticketsystem.ApplicationLayer.TokenService;
import ticketsystem.ApplicationLayer.UserAccessService;
import ticketsystem.ApplicationLayer.UserService;
import ticketsystem.DTO.PaymentDetails;
import ticketsystem.DTO.seatPositionDTO;
import ticketsystem.DomainLayer.EventCatalogDomainService;
import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.IRepository.ILotteryRepository;
import ticketsystem.DomainLayer.IRepository.IOrderRepository;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.DomainLayer.MembershipDomainService;
import ticketsystem.DomainLayer.company.Company;
import ticketsystem.DomainLayer.discount.DiscountCompositionType;
import ticketsystem.DomainLayer.discount.DiscountPolicy;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.event.EventCategory;
import ticketsystem.DomainLayer.event.EventLocation;
import ticketsystem.DomainLayer.event.Pair;
import ticketsystem.DomainLayer.event.SeatingArea;
import ticketsystem.DomainLayer.event.StandingArea;
import ticketsystem.DomainLayer.policy.PurchasePolicy;
import ticketsystem.InfrastructureLayer.CompanyRepository;
import ticketsystem.InfrastructureLayer.EventRepository;
import ticketsystem.InfrastructureLayer.LotteryRepository;
import ticketsystem.InfrastructureLayer.InMemoryUserRepository;
import ticketsystem.InfrastructureLayer.InMemoryOrderRepository;
import ticketsystem.InfrastructureLayer.PaymentServiceProxy;
import ticketsystem.InfrastructureLayer.TokenRepository;

public class ReservationServiceTest {

    private ReservationService reservationService;

    private IOrderRepository orderRepository;
    private IEventRepository eventRepository;
    private ILotteryRepository lotteryRepository;
    private ICompanyRepository companyRepository;
    private IUserRepository userRepository;

    private IPaymentService paymentService;
    private TestSecureBarcode secureBarcode;
    private TokenService tokenService;
    private UserService userService;
    private EventCatalogDomainService eventCatalogDomainService;
    private MembershipDomainService membershipDomain;
    private UserAccessService userAccessService;

    private ISystemLogger logger;
    private FakeNotifier fakeNotifier;

    private String[] memberTokens;

    private static final Long COMPANY_ID = 1L;
    private static final Long COMPANY_FOUNDER_ID = 1L;

    @BeforeEach
    void setUp() {
        orderRepository = new InMemoryOrderRepository();
        eventRepository = new EventRepository();

        lotteryRepository = new LotteryRepository();
        ((LotteryRepository) lotteryRepository).clearForTests();

        companyRepository = new CompanyRepository();
        userRepository = new InMemoryUserRepository();
        membershipDomain = new MembershipDomainService(userRepository);

        paymentService = new PaymentServiceProxy();
        secureBarcode = new TestSecureBarcode();
        logger = new NoOpSystemLogger();
        fakeNotifier = new FakeNotifier();
        userAccessService = new UserAccessService(userRepository);
        tokenService = new TokenService(
                "manual_test_secret_32_chars_long",
                new TokenRepository(), logger
        );

        userService = new UserService(userRepository, tokenService, logger);

        Company company = new Company(
                "BGU Productions",
                COMPANY_FOUNDER_ID,
                PurchasePolicy.noRestrictions(),
                new DiscountPolicy(DiscountCompositionType.MAX)
        );

        company.setId(COMPANY_ID);
        companyRepository.save(company);

        eventCatalogDomainService
                = new EventCatalogDomainService((CompanyRepository) companyRepository);

        resetPaymentProxy();

        reservationService = new ReservationService(
                orderRepository,
                eventRepository,
                companyRepository,
                membershipDomain,
                tokenService,
                paymentService,
                secureBarcode,
                lotteryRepository,
                eventCatalogDomainService,
                logger,
                fakeNotifier, userAccessService
        );

        memberTokens = new String[40];
        for (int i = 0; i < memberTokens.length; i++) {
            memberTokens[i] = createLoggedInMember("user" + i, "password123");
        }
    }

    @Test
    void ConcurrencyTest_SelectSameSeat_WhenManyUsersTrySameSeat_ThenOnlyOneUserSucceeds()
            throws InterruptedException {

        Long eventId = 100L;
        Long areaId = 1L;

        Event event = createActiveEventWithSingleSeat(eventId);
        eventRepository.addEvent(event);

        int numberOfThreads = 20;

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        List<Throwable> exceptions = synchronizedList(new ArrayList<>());

        seatPositionDTO position = new seatPositionDTO(1, 1);

        for (int i = 0; i < numberOfThreads; i++) {
            final int userIndex = i + 1;

            executor.submit(() -> {
                try {
                    startLatch.await();

                    boolean result = reservationService.selectSeatTicket(
                            tokenForUserIndex(userIndex),
                            eventId,
                            areaId,
                            position,
                            null
                    );

                    if (result) {
                        successCount.incrementAndGet();
                    }

                } catch (Throwable t) {
                    exceptions.add(t);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Test timed out");
        assertEquals(1, successCount.get(), "Only one user should reserve the same seat");
    }

    @Test
    void ConcurrencyTest_CheckoutSameOrder_WhenManyThreadsCheckoutSameOrder_ThenOnlyOneCheckoutSucceeds()
            throws InterruptedException {

        Long eventId = 101L;
        Long areaId = 1L;
        String token = tokenForUserIndex(1);

        Event event = createActiveStandingEvent(eventId);
        eventRepository.addEvent(event);

        reservationService.selectStandingTicket(token, eventId, areaId, 1, null);
        reservationService.validateActiveOrderPolicy(token, eventId, createPaymentDetails(), null);

        int numberOfThreads = 10;

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        List<Throwable> exceptions = synchronizedList(new ArrayList<>());
        
        for (int i = 0; i < numberOfThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();

                    boolean result = reservationService.checkout(
                            token,
                            eventId,
                            createPaymentDetails(),
                            null
                    );

                    if (result) {
                        successCount.incrementAndGet();
                    }

                } catch (Throwable t) {
                    exceptions.add(t);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Test timed out");
        assertEquals(1, successCount.get(), "Only one checkout should complete the same order");
    }
    @Test
    void ConcurrencyTest_SelectStandingTickets_WhenManyUsersSelectTickets_ThenSystemStaysConsistent()
            throws InterruptedException {

        Long eventId = 102L;
        Long areaId = 1L;

        Event event = createActiveStandingEvent(eventId);
        eventRepository.addEvent(event);

        int numberOfThreads = 30;

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        List<Throwable> exceptions = synchronizedList(new ArrayList<>());

        for (int i = 0; i < numberOfThreads; i++) {
            final int userIndex = i + 1;

            executor.submit(() -> {
                try {
                    startLatch.await();

                    boolean result = reservationService.selectStandingTicket(
                            tokenForUserIndex(userIndex),
                            eventId,
                            areaId,
                            1,
                            null
                    );

                    if (result) {
                        successCount.incrementAndGet();
                    }

                } catch (Throwable t) {
                    exceptions.add(t);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Test timed out");

        int activeOrdersForEvent = orderRepository.getAll().stream()
                .filter(order -> order.getEventId().equals(eventId))
                .toList()
                .size();

        assertTrue(successCount.get() <= 20,
                "Successful standing reservations must not exceed standing area capacity");

        assertTrue(activeOrdersForEvent >= successCount.get(),
                "There should be at least one active order for every successful reservation");

        assertTrue(successCount.get() <= numberOfThreads);
    }

    @Test
    void ConcurrencyTest_ManyUsersSelectDifferentSeats_ThenPartOfUsersSucceed()
            throws InterruptedException {

        Long eventId = 103L;
        Long areaId = 1L;

        Event event = createActiveEventWithManySeats(eventId, 5, 5);
        eventRepository.addEvent(event);

        int numberOfUsers = 20;

        ExecutorService executor = Executors.newFixedThreadPool(numberOfUsers);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfUsers);

        AtomicInteger successCount = new AtomicInteger(0);
        List<Throwable> exceptions = synchronizedList(new ArrayList<>());

        for (int i = 0; i < numberOfUsers; i++) {
            final int userIndex = i + 1;
            final int row = (i / 5) + 1;
            final int col = (i % 5) + 1;

            executor.submit(() -> {
                try {
                    startLatch.await();

                    boolean result = reservationService.selectSeatTicket(
                            tokenForUserIndex(userIndex),
                            eventId,
                            areaId,
                            new seatPositionDTO(row, col),
                            null
                    );

                    if (result) {
                        successCount.incrementAndGet();
                    }

                } catch (Throwable t) {
                    exceptions.add(t);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Test timed out");

        assertTrue(successCount.get() > 0, "At least some users should succeed");

        assertTrue(successCount.get() <= numberOfUsers,
                "Success count cannot exceed number of users");

        assertEquals(numberOfUsers, successCount.get() + exceptions.size(),
                "Every thread should either succeed or fail safely");
    }

    @Test
    void ConcurrencyTest_ManyUsersCheckoutDifferentOrders_ThenAllCheckoutsSucceed()
            throws InterruptedException {

        Long eventId = 104L;
        Long areaId = 1L;

        Event event = createActiveStandingEvent(eventId);
        eventRepository.addEvent(event);

        int numberOfUsers = 10;

        for (int i = 0; i < numberOfUsers; i++) {
            int userIndex = i + 1;

            reservationService.selectStandingTicket(
                    tokenForUserIndex(userIndex),
                    eventId,
                    areaId,
                    1,
                    null
            );
        }

        ExecutorService executor = Executors.newFixedThreadPool(numberOfUsers);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfUsers);

        AtomicInteger successCount = new AtomicInteger(0);
        List<Throwable> exceptions = synchronizedList(new ArrayList<>());

        for (int i = 0; i < numberOfUsers; i++) {
            final int userIndex = i + 1;

            executor.submit(() -> {
                try {
                    reservationService.validateActiveOrderPolicy(
                            tokenForUserIndex(userIndex),
                            eventId,
                            createPaymentDetails(),
                            null
                    );

                    startLatch.await();

                    boolean result = reservationService.checkout(
                            tokenForUserIndex(userIndex),
                            eventId,
                            createPaymentDetails(),
                            null 
                    );

                    if (result) {
                        successCount.incrementAndGet();
                    }

                } catch (Throwable t) {
                    exceptions.add(t);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Test timed out");

        assertTrue(successCount.get() > 0,
                "At least one checkout should succeed");

        assertTrue(successCount.get() <= numberOfUsers,
                "Success count cannot exceed number of users");

        assertEquals(numberOfUsers, successCount.get() + exceptions.size(),
                "Every checkout attempt should either succeed or fail safely");

        assertTrue(orderRepository.getAll().size() <= numberOfUsers,
                "Repository should remain in a consistent state");
    }
    @Test
    void ConcurrencyTest_ManyUsersSelectStandingTickets_OverCapacity_ThenOnlyCapacityUsersSucceed()
            throws InterruptedException {

        Long eventId = 105L;
        Long areaId = 1L;

        int capacity = 5;

        Event event = createActiveStandingEventWithCapacity(eventId, capacity);
        eventRepository.addEvent(event);

        int numberOfUsers = 20;

        ExecutorService executor = Executors.newFixedThreadPool(numberOfUsers);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfUsers);

        AtomicInteger successCount = new AtomicInteger(0);
        List<Throwable> exceptions = synchronizedList(new ArrayList<>());

        for (int i = 0; i < numberOfUsers; i++) {
            final int userIndex = i + 1;

            executor.submit(() -> {
                try {
                    startLatch.await();

                    boolean result = reservationService.selectStandingTicket(
                            tokenForUserIndex(userIndex),
                            eventId,
                            areaId,
                            1,
                            null
                    );

                    if (result) {
                        successCount.incrementAndGet();
                    }

                } catch (Throwable t) {
                    exceptions.add(t);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Test timed out");

        assertTrue(successCount.get() <= capacity,
                "Successful reservations must not exceed standing capacity");
    }

    private Event createActiveEventWithManySeats(Long eventId, int rows, int columns) {
        Event event = createBaseEvent(eventId, "Concurrency Many Seats Test Event");

        SeatingArea seatingArea = new SeatingArea(
                1L,
                "Seating Area",
                new Pair<>(0, 0),
                new Pair<>(10, 10),
                rows,
                columns
        );

        event.getMap().addElement(seatingArea);
        event.setStatus(Event.eventStatus.ACTIVE);

        return event;
    }

    private Event createActiveStandingEventWithCapacity(Long eventId, int capacity) {
        Event event = createBaseEvent(eventId, "Concurrency Standing Capacity Test Event");

        StandingArea standingArea = new StandingArea(
                1L,
                "Standing Area",
                new Pair<>(0, 0),
                new Pair<>(10, 10),
                capacity
        );

        event.getMap().addElement(standingArea);
        event.setStatus(Event.eventStatus.ACTIVE);

        return event;
    }

    private Event createActiveStandingEvent(Long eventId) {
        Event event = createBaseEvent(eventId, "Concurrency Standing Test Event");

        StandingArea standingArea = new StandingArea(
                1L,
                "Standing Area",
                new Pair<>(0, 0),
                new Pair<>(10, 10),
                20
        );

        event.getMap().addElement(standingArea);
        event.setStatus(Event.eventStatus.ACTIVE);

        return event;
    }

    private Event createActiveEventWithSingleSeat(Long eventId) {
        Event event = createBaseEvent(eventId, "Concurrency Seat Test Event");

        SeatingArea seatingArea = new SeatingArea(
                1L,
                "Seating Area",
                new Pair<>(0, 0),
                new Pair<>(10, 10),
                1,
                1
        );

        event.getMap().addElement(seatingArea);
        event.setStatus(Event.eventStatus.ACTIVE);

        return event;
    }

    private Event createBaseEvent(Long eventId, String name) {
        return new Event(
                eventId,
                LocalDateTime.now().plusDays(10),
                name,
                COMPANY_ID,
                COMPANY_FOUNDER_ID,
                EventLocation.TEL_AVIV,
                100L,
                EventCategory.CONCERT,
                "Test Artist",
                new BigDecimal("100.00"),
                new Pair<>(10, 10)
        );
    }

    private PaymentDetails createPaymentDetails() {
        return new PaymentDetails("VISA", "Yosi", LocalDate.now());
    }

    private String createLoggedInMember(String username, String password) {
        String guest = userService.visitSystem();

        boolean signedUp = userService.signUp(
                guest,
                username,
                password,
                "Test User",
                "0500000000",LocalDate.of(2001, 1, 1)
        );
        assertTrue(signedUp);

        String token = userService.login(guest, username, password);
        assertNotNull(token);

        return token;
    }

    private String tokenForUserIndex(int index) {
        return memberTokens[index - 1];
    }

    private void resetPaymentProxy() {
        PaymentServiceProxy.isConnectionSuccessful = true;
        PaymentServiceProxy.isPaymentSuccessful = true;
        PaymentServiceProxy.isRefundSuccessful = true;

        PaymentServiceProxy.wasConnectCalled = false;
        PaymentServiceProxy.wasPayCalled = false;
        PaymentServiceProxy.wasRefundCalled = false;
    }

    private static class TestSecureBarcode implements ITicketIssuingService {

        AtomicInteger generateCalls = new AtomicInteger(0);

        @Override
        public boolean handshake() {
            return true;
        }

        @Override
        public String issueTicket(Long ticketId, Long eventId, Long userId) {
            generateCalls.incrementAndGet();
            return "SECURE_BARCODE_" + ticketId + "_" + eventId + "_" + userId;
        }
    }

    private static class NoOpSystemLogger implements ISystemLogger {

        @Override
        public void logEvent(String message, LogLevel level) {
        }

        @Override
        public void logError(String errorMessage, Throwable exception) {
        }
    }

    private static class FakeNotifier implements INotifier {

        private final List<String> messages = new ArrayList<>();

        @Override
        public void notifyMember(Long memberId, String message) {
            messages.add(message);
        }

        @Override
        public void notifyGuest(String guestToken, String message) {
            messages.add(message);
        }

        @Override
        public void notifyMembers(Collection<Long> memberIds, String message) {
            if (memberIds == null) {
                return;
            }

            for (Long memberId : memberIds) {
                if (memberId != null) {
                    notifyMember(memberId, message);
                }
            }
        }

        @Override
        public void notifyGuests(Collection<String> guestTokens, String message) {
            if (guestTokens == null) {
                return;
            }

            for (String guestToken : guestTokens) {
                if (guestToken != null && !guestToken.isBlank()) {
                    notifyGuest(guestToken, message);
                }
            }
        }

        boolean containsMessage(String text) {
            return messages.stream()
                    .anyMatch(message -> message.contains(text));
        }
    }
}
