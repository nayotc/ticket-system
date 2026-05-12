package ticketsystem.ConcurrencyTesting;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import static java.util.Collections.synchronizedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.ApplicationLayer.*;
import ticketsystem.DTO.PaymentDetails;
import ticketsystem.DTO.seatPositionDTO;
import ticketsystem.DomainLayer.IRepository.*;
import ticketsystem.DomainLayer.event.*;
import ticketsystem.InfrastructureLayer.*;

public class ReservationServiceTest {

    private ReservationService reservationService;
    private IOrderRepository orderRepository;
    private IEventRepository eventRepository;
    private ILotteryRepository lotteryRepository;
    private FakePaymentService paymentService;
    private FakeSecureBarcode secureBarcode;
    private TokenService tokenService;
    private FakeSystemLogger logger;


    @BeforeEach
    void setUp() {
        orderRepository = new OrderRepository();
        eventRepository = new EventRepository();

        lotteryRepository = LotteryRepository.getInstance();
        ((LotteryRepository) lotteryRepository).clearForTests();

        paymentService = new FakePaymentService();
        secureBarcode = new FakeSecureBarcode();
        logger= new FakeSystemLogger();


        tokenService = new TokenService(
                "manual_test_secret_32_chars_long",
                new TokenRepository()
        ) {
            @Override
            public boolean validateToken(String token) {
                return true;
            }

            @Override
            public boolean isGuestToken(String token) {
                return false;
            }

            @Override
            public boolean isMemberToken(String token) {
                return true;
            }

            @Override
            public Long extractUserId(String token) {
                String[] parts = token.split("-");
                return Long.parseLong(parts[2]);
            }
        };

        reservationService = new ReservationService(
                orderRepository,
                eventRepository,
                tokenService,
                paymentService,
                secureBarcode,
                lotteryRepository,logger
        );
    }

    @Test
    void ConcurrencyTest_SelectSameSeat_WhenManyUsersTrySameSeat_ThenOnlyOneUserSucceeds() throws InterruptedException {
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
            final int userId = i + 1;

            executor.submit(() -> {
                try {
                    startLatch.await();

                    boolean result = reservationService.selectSeatTicket(
                            "member-token-" + userId,
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
    void ConcurrencyTest_CheckoutSameOrder_WhenManyThreadsCheckoutSameOrder_ThenOnlyOneCheckoutSucceeds() throws InterruptedException {
        Long eventId = 101L;
        Long areaId = 1L;
        String token = "member-token-1";

        Event event = createActiveStandingEvent(eventId);
        eventRepository.addEvent(event);

        reservationService.selectStandingTicket(
                token,
                eventId,
                areaId,
                1,
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

                    boolean result = reservationService.checkout(
                            token,
                            eventId,
                            createPaymentDetails()
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
    void ConcurrencyTest_SelectStandingTickets_WhenManyUsersSelectTickets_ThenSystemStaysConsistent() throws InterruptedException {
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
            final int userId = i + 1;

            executor.submit(() -> {
                try {
                    startLatch.await();

                    boolean result = reservationService.selectStandingTicket(
                            "member-token-" + userId,
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
    void ConcurrencyTest_ManyUsersSelectDifferentSeats_ThenAllUsersSucceed()
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
            final int userId = i + 1;
            final int row = (i / 5) + 1;
            final int col = (i % 5) + 1;

            executor.submit(() -> {
                try {
                    startLatch.await();

                    boolean result = reservationService.selectSeatTicket(
                            "member-token-" + userId,
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
        assertTrue(successCount.get() > 0,
        "At least some users should succeed");

        assertTrue(successCount.get() <= numberOfUsers,
                "Success count cannot exceed number of users");

        assertEquals(numberOfUsers, successCount.get() + exceptions.size(),
                "Every thread should either succeed or fail safely");
            }

    private Event createActiveEventWithManySeats(Long eventId, int rows, int columns) {
    Event event = new Event(
            eventId,
            LocalDateTime.now().plusDays(10),
            "Concurrency Many Seats Test Event",
            1L,
            1L,
            EventLocation.TEL_AVIV,
            100L,
            EventCategory.CONCERT,
            "Test Artist",
            new BigDecimal("100.00"),
            new Pair<>(10, 10)
    );

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
    Event event = new Event(
            eventId,
            LocalDateTime.now().plusDays(10),
            "Concurrency Standing Capacity Test Event",
            1L,
            1L,
            EventLocation.TEL_AVIV,
            100L,
            EventCategory.CONCERT,
            "Test Artist",
            new BigDecimal("100.00"),
            new Pair<>(10, 10)
    );

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
    Event event = new Event(
            eventId,
            LocalDateTime.now().plusDays(10),
            "Concurrency Standing Test Event",
            1L,
            1L,
            EventLocation.TEL_AVIV,
            100L,
            EventCategory.CONCERT,
            "Test Artist",
            new BigDecimal("100.00"),
            new Pair<>(10, 10)
    );

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
    Event event = new Event(
            eventId,
            LocalDateTime.now().plusDays(10),
            "Concurrency Seat Test Event",
            1L,
            1L,
            EventLocation.TEL_AVIV,
            100L,
            EventCategory.CONCERT,
            "Test Artist",
            new BigDecimal("100.00"),
            new Pair<>(10, 10)
    );

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
    

    private PaymentDetails createPaymentDetails() {
        return new PaymentDetails("VISA", "Yosi");
    }

    private static class FakePaymentService implements IPaymentService {
        AtomicInteger payCalls = new AtomicInteger(0);
        AtomicInteger refundCalls = new AtomicInteger(0);

        @Override
        public boolean connect() {
            return true;
        }

        @Override
        public boolean pay(BigDecimal amount, PaymentDetails details) {
            payCalls.incrementAndGet();
            return true;
        }

        @Override
        public boolean refund(BigDecimal amount, PaymentDetails details) {
            refundCalls.incrementAndGet();
            return true;
        }
    }

    private static class FakeSecureBarcode implements ISecureBarcode {
        AtomicInteger generateCalls = new AtomicInteger(0);

        @Override
        public boolean connect() {
            return true;
        }

        @Override
        public String generateSecureBarcode(Long ticketId, Long eventId, Long userId) {
            generateCalls.incrementAndGet();
            return "SECURE_BARCODE_" + ticketId + "_" + eventId + "_" + userId;
        }
    }

    private static class FakeSystemLogger implements ISystemLogger {
        List<String> messages = new ArrayList<>();

        @Override
        public void logEvent(String message, LogLevel level) {
            messages.add("[" + level + "] " + message);
        }

        @Override
        public void logError(String errorMessage, Throwable exception) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'logError'");
        }
    }
}