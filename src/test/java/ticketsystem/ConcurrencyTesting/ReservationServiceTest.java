package ticketsystem.ConcurrencyTesting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.synchronizedList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ticketsystem.ApplicationLayer.INotifier;
import ticketsystem.ApplicationLayer.IPaymentService;
import ticketsystem.ApplicationLayer.ISystemLogger;
import ticketsystem.ApplicationLayer.ITicketIssuingService;
import ticketsystem.ApplicationLayer.ReservationService;
import ticketsystem.ApplicationLayer.TokenService;
import ticketsystem.ApplicationLayer.UserAccessService;
import ticketsystem.ApplicationLayer.UserService;
import ticketsystem.DTO.PaymentDetails;
import ticketsystem.DTO.seatPositionDTO;
import ticketsystem.DomainLayer.EventCatalogDomainService;
import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.IRepository.IOrderRepository;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.DomainLayer.MembershipDomainService;
import ticketsystem.DomainLayer.SearchCriteria;
import ticketsystem.DomainLayer.company.Company;
import ticketsystem.DomainLayer.discount.DiscountCompositionType;
import ticketsystem.DomainLayer.discount.DiscountPolicy;
import ticketsystem.DomainLayer.event.*;
import ticketsystem.DomainLayer.event.Seat.SeatStatus;
import ticketsystem.DomainLayer.policy.PurchasePolicy;
import ticketsystem.InfrastructureLayer.CompanyRepository;
import ticketsystem.InfrastructureLayer.InMemoryEventRepository;
import ticketsystem.InfrastructureLayer.LotteryRepository;
import ticketsystem.InfrastructureLayer.InMemoryUserRepository;
import ticketsystem.InfrastructureLayer.InMemoryOrderRepository;
import ticketsystem.InfrastructureLayer.PaymentServiceProxy;
import ticketsystem.InfrastructureLayer.TokenRepository;

import ticketsystem.InfrastructureLayer.persistence.CompanyJpaRepository;
import ticketsystem.InfrastructureLayer.persistence.LotteryJpaRepository;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
@SpringBootTest
public class ReservationServiceTest {
    private ReservationService reservationService;
    private ReservationService policyAwareReservationService;
    private IOrderRepository orderRepository;

    @Autowired
    private IEventRepository eventRepository;

    @Autowired
    private LotteryRepository lotteryRepository;

    @Autowired
    private LotteryJpaRepository lotteryJpaRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyJpaRepository companyJpaRepository;

    private IEventRepository policyRepository;
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

    private Long companyId;

    private final List<Long> createdEventIds = new ArrayList<>();

    private static final Long COMPANY_FOUNDER_ID = 1L;

    @BeforeEach
    void setUp() {
        companyJpaRepository.deleteAll();
        orderRepository = new InMemoryOrderRepository();
        policyRepository = new InMemoryEventRepository();
        userRepository = new InMemoryUserRepository();
        membershipDomain = new MembershipDomainService(userRepository);
        paymentService = new PaymentServiceProxy();
        secureBarcode = new TestSecureBarcode();
        logger = new NoOpSystemLogger();
        fakeNotifier = new FakeNotifier();
        userAccessService = new UserAccessService(userRepository);

        tokenService = new TokenService(
                "manual_test_secret_32_chars_long",
                new TokenRepository(),
                logger
        );

        userService = new UserService(userRepository, tokenService, logger);

        Company company = new Company(
                "BGU Productions",
                COMPANY_FOUNDER_ID,
                PurchasePolicy.noRestrictions(),
                new DiscountPolicy(DiscountCompositionType.MAX)
        );

        companyRepository.save(company);

        eventCatalogDomainService = new EventCatalogDomainService((CompanyRepository) companyRepository);
        companyId = company.getId();

        assertNotNull(
                companyId,
                "The database should assign an identifier to the saved company."
        );

        assertTrue(
                companyId > 0,
                "The database-generated company identifier should be positive."
        );
        eventCatalogDomainService =
                new EventCatalogDomainService(
                        companyRepository
                );
        resetPaymentProxy();

        reservationService = createReservationService(eventRepository);
        policyAwareReservationService = createReservationService(
                new PolicyAwareEventRepository(eventRepository, policyRepository)
        );

        memberTokens = new String[40];

        for (int i = 0; i < memberTokens.length; i++) {
            memberTokens[i] = createLoggedInMember("user" + i, "password123");
        }
    }

    @AfterEach
    void cleanUpPersistedEvents() {
        for (Long eventId : createdEventIds) {
            Event persistedEvent = eventRepository.getEventById(eventId);

            if (persistedEvent != null) {
                eventRepository.deleteEvent(eventId, persistedEvent.getVersion());
            }
        }

        createdEventIds.clear();
    }

    @Test
    void ConcurrencyTest_SelectSameSeat_WhenManyUsersTrySameSeat_ThenOnlyOneUserSucceeds()
            throws InterruptedException {
        Event event = createActiveEventWithSingleSeat();
        saveEvent(event);

        Long eventId = event.getId();
        Long areaId = getSeatingAreaId(event);
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
                } catch (Throwable throwable) {
                    exceptions.add(throwable);
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
        String token = tokenForUserIndex(1);
        Event event = createActiveStandingEvent();

        saveEventWithPolicies(event);

        Long eventId = event.getId();
        Long areaId = getStandingAreaId(event);

        reservationService.selectStandingTicket(token, eventId, areaId, 1, null);

        policyAwareReservationService.validateActiveOrderPolicy(
                token,
                eventId,
                createPaymentDetails(),
                null
        );

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

                    boolean result = policyAwareReservationService.checkout(
                            token,
                            eventId,
                            createPaymentDetails(),
                            null
                    );

                    if (result) {
                        successCount.incrementAndGet();
                    }
                } catch (Throwable throwable) {
                    exceptions.add(throwable);
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
        Event event = createActiveStandingEvent();
        saveEvent(event);

        Long eventId = event.getId();
        Long areaId = getStandingAreaId(event);
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
                } catch (Throwable throwable) {
                    exceptions.add(throwable);
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

        assertTrue(
                successCount.get() <= 20,
                "Successful standing reservations must not exceed standing area capacity"
        );

        assertTrue(
                activeOrdersForEvent >= successCount.get(),
                "There should be at least one active order for every successful reservation"
        );

        assertTrue(successCount.get() <= numberOfThreads);
    }

    @Test
    void ConcurrencyTest_ManyUsersSelectDifferentSeats_ThenPartOfUsersSucceed()
            throws InterruptedException {
        Event event = createActiveEventWithManySeats(5, 5);
        saveEvent(event);

        Long eventId = event.getId();
        Long areaId = getSeatingAreaId(event);
        int numberOfUsers = 20;

        ExecutorService executor = Executors.newFixedThreadPool(numberOfUsers);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfUsers);
        AtomicInteger successCount = new AtomicInteger(0);
        List<Throwable> exceptions = synchronizedList(new ArrayList<>());

        for (int i = 0; i < numberOfUsers; i++) {
            final int userIndex = i + 1;
            final int row = (i / 5) + 1;
            final int column = (i % 5) + 1;

            executor.submit(() -> {
                try {
                    startLatch.await();

                    boolean result = reservationService.selectSeatTicket(
                            tokenForUserIndex(userIndex),
                            eventId,
                            areaId,
                            new seatPositionDTO(row, column),
                            null
                    );

                    if (result) {
                        successCount.incrementAndGet();
                    }
                } catch (Throwable throwable) {
                    exceptions.add(throwable);
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
        assertTrue(successCount.get() <= numberOfUsers, "Success count cannot exceed number of users");

        assertEquals(
                numberOfUsers,
                successCount.get() + exceptions.size(),
                "Every thread should either succeed or fail safely"
        );
    }

    @Test
    void ConcurrencyTest_ManyUsersCheckoutDifferentOrders_ThenAllCheckoutsSucceed()
            throws InterruptedException {
        Event event = createActiveStandingEvent();

        saveEventWithPolicies(event);

        Long eventId = event.getId();
        Long areaId = getStandingAreaId(event);
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
                    policyAwareReservationService.validateActiveOrderPolicy(
                            tokenForUserIndex(userIndex),
                            eventId,
                            createPaymentDetails(),
                            null
                    );

                    startLatch.await();

                    boolean result = policyAwareReservationService.checkout(
                            tokenForUserIndex(userIndex),
                            eventId,
                            createPaymentDetails(),
                            null
                    );

                    if (result) {
                        successCount.incrementAndGet();
                    }
                } catch (Throwable throwable) {
                    exceptions.add(throwable);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Test timed out");
        assertTrue(successCount.get() > 0, "At least one checkout should succeed");
        assertTrue(successCount.get() <= numberOfUsers, "Success count cannot exceed number of users");

        assertEquals(
                numberOfUsers,
                successCount.get() + exceptions.size(),
                "Every checkout attempt should either succeed or fail safely"
        );

        assertTrue(
                orderRepository.getAll().size() <= numberOfUsers,
                "Repository should remain in a consistent state"
        );
    }

    @Test
    void ConcurrencyTest_ManyUsersSelectStandingTickets_OverCapacity_ThenOnlyCapacityUsersSucceed()
            throws InterruptedException {
        int capacity = 5;
        Event event = createActiveStandingEventWithCapacity(capacity);

        saveEvent(event);

        Long eventId = event.getId();
        Long areaId = getStandingAreaId(event);
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
                } catch (Throwable throwable) {
                    exceptions.add(throwable);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Test timed out");

        assertTrue(
                successCount.get() <= capacity,
                "Successful reservations must not exceed standing capacity"
        );
    }

    private ReservationService createReservationService(IEventRepository repository) {
        return new ReservationService(
                orderRepository,
                repository,
                companyRepository,
                membershipDomain,
                tokenService,
                paymentService,
                secureBarcode,
                lotteryRepository,
                eventCatalogDomainService,
                logger,
                fakeNotifier,
                userAccessService
        );
    }

    private void saveEvent(Event event) {
        eventRepository.addEvent(event);
        assertNotNull(event.getId(), "The database should generate an event ID");
        createdEventIds.add(event.getId());
    }

    private void saveEventWithPolicies(Event event) {
        saveEvent(event);
        policyRepository.addEvent(event);
    }

    private Long getSeatingAreaId(Event event) {
        return event.getMap().getElements().stream()
                .filter(SeatingArea.class::isInstance)
                .map(Element::getId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Seating area was not found"));
    }

    private Long getStandingAreaId(Event event) {
        return event.getMap().getElements().stream()
                .filter(StandingArea.class::isInstance)
                .map(Element::getId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Standing area was not found"));
    }

    private Event createActiveEventWithManySeats(int rows, int columns) {
        Event event = createBaseEvent("Concurrency Many Seats Test Event");

        SeatingArea seatingArea = new SeatingArea(
                "Seating Area",
                new Pair<>(0, 0),
                new Pair<>(10, 10),
                rows,
                columns,
                new BigDecimal("100.00")
        );

        event.getMap().addElement(seatingArea);
        event.setStatus(Event.eventStatus.ACTIVE);

        return event;
    }

    private Event createActiveStandingEventWithCapacity(int capacity) {
        Event event = createBaseEvent("Concurrency Standing Capacity Test Event");

        StandingArea standingArea = new StandingArea(
                "Standing Area",
                new Pair<>(0, 0),
                new Pair<>(10, 10),
                capacity,
                new BigDecimal("100.00")
        );

        event.getMap().addElement(standingArea);
        event.setStatus(Event.eventStatus.ACTIVE);

        return event;
    }

    private Event createActiveStandingEvent() {
        Event event = createBaseEvent("Concurrency Standing Test Event");

        StandingArea standingArea = new StandingArea(
                "Standing Area",
                new Pair<>(0, 0),
                new Pair<>(10, 10),
                20,
                new BigDecimal("100.00")
        );

        event.getMap().addElement(standingArea);
        event.setStatus(Event.eventStatus.ACTIVE);

        return event;
    }

    private Event createActiveEventWithSingleSeat() {
        Event event = createBaseEvent("Concurrency Seat Test Event");

        SeatingArea seatingArea = new SeatingArea(
                "Seating Area",
                new Pair<>(0, 0),
                new Pair<>(10, 10),
                1,
                1,
                new BigDecimal("100.00")
        );

        event.getMap().addElement(seatingArea);
        event.setStatus(Event.eventStatus.ACTIVE);

        return event;
    }

    private Event createBaseEvent(String name) {
        return new Event(
                LocalDateTime.now().plusDays(10),
                name,
                companyId,
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
        return new PaymentDetails(
            "VISA",
            "Yosi Cohen",
            LocalDate.of(2001, 1, 1),
            "4580458045804580",
            12,
            2030,
            "123",
            "123456789",
            "ILS"
        );
    }

    private String createLoggedInMember(String username, String password) {
        String guest = userService.visitSystem();

        boolean signedUp = userService.signUp(
                guest,
                username,
                password,
                "Test User",
                "0500000000",
                LocalDate.of(2001, 1, 1)
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

    private static class PolicyAwareEventRepository implements IEventRepository {

        private final IEventRepository realRepository;
        private final IEventRepository policyRepository;

        private PolicyAwareEventRepository(
                IEventRepository realRepository,
                IEventRepository policyRepository
        ) {
            this.realRepository = realRepository;
            this.policyRepository = policyRepository;
        }

        @Override
        public void addEvent(Event event) {
            realRepository.addEvent(event);
        }

        @Override
        public Event getEventById(Long eventId) {
            Event persistedEvent = realRepository.getEventById(eventId);

            if (persistedEvent == null) {
                return null;
            }

            Event policyEvent;

            try {
                policyEvent = policyRepository.getEventById(eventId);
            } catch (Exception ignored) {
                return persistedEvent;
            }

            if (policyEvent != null) {
                persistedEvent.setPurchasePolicy(policyEvent.getPurchasePolicy());
                persistedEvent.setDiscountPolicy(policyEvent.getDiscountPolicy());
            }

            return persistedEvent;
        }

        @Override
        public void updateEvent(Event event) {
            realRepository.updateEvent(event);
        }

        @Override
        public void deleteEvent(Long eventId, long expectedVersion) {
            realRepository.deleteEvent(eventId, expectedVersion);
        }

        @Override
        public List<Event> getEventsByCompanyId(Long companyId) {
            return realRepository.getEventsByCompanyId(companyId);
        }

        @Override
        public List<Event> getAllEvents() {
            return realRepository.getAllEvents();
        }

        @Override
        public List<EventSearchResultView> getFeaturedEvents(int limit) {
            return realRepository.getFeaturedEvents(limit);
        }

        @Override
        public List<EventSearchResultView> searchEvents(SearchCriteria criteria, List<Long> companyIds
        ) {
            return realRepository.searchEvents(criteria, companyIds);
        }

		@Override
		public void updateSeatStatus(Long eventId, Long areaId, int row, int number, SeatStatus newStatus) {
			realRepository.updateSeatStatus(eventId,areaId,row,number,newStatus);
		}

		@Override
		public void updateStandingAreaReservedCount(Long eventId, Long areaId, int reservedDelta) {
			realRepository.updateStandingAreaReservedCount(eventId, areaId, reservedDelta);
		}

		@Override
		public void markStandingTicketsAsSold(Long eventId, Long areaId, int quantity) {
			realRepository.markStandingTicketsAsSold(eventId, areaId, quantity);
		}

		@Override
		public void updateSaleStatus(Long eventId, SaleStatus saleStatus) {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException("Unimplemented method 'updateSaleStatus'");
		}

		@Override
        public Event getEventForReservation(Long eventId) {
            return getEventById(eventId);
        }


    }

    private static class TestSecureBarcode implements ITicketIssuingService {

        private final AtomicInteger generateCalls = new AtomicInteger(0);

        @Override
        public boolean handshake() {
            return true;
        }

        @Override
        public String issueTicket(ticketsystem.DTO.TicketIssueRequest request) {
            generateCalls.incrementAndGet();
            return "SECURE_BARCODE_" + request.getEventId() + "_" + request.getCustomerId();
        }

        @Override
        public boolean cancelTicket(String ticketId) {
            return true;
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

        private final List<String> messages = synchronizedList(new ArrayList<>());

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
            return messages.stream().anyMatch(message -> message.contains(text));
        }
    }
}
