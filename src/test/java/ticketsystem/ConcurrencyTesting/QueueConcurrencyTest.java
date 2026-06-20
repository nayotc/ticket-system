package ticketsystem.ConcurrencyTesting;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ticketsystem.ApplicationLayer.INotifier;
import ticketsystem.ApplicationLayer.TokenService;
import ticketsystem.ApplicationLayer.WaitingQueueService;
import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.event.EventCategory;
import ticketsystem.DomainLayer.event.EventLocation;
import ticketsystem.DomainLayer.event.Pair;
import ticketsystem.DomainLayer.user.Guest;
import ticketsystem.InfrastructureLayer.LogbackSystemLogger;
import ticketsystem.InfrastructureLayer.TokenRepository;
import ticketsystem.InfrastructureLayer.WaitingQueueRepository;

@SpringBootTest
public class QueueConcurrencyTest {

    private static final String TEST_SECRET =
            "manual_test_secret_key_for_jwt_must_be_at_least_32_bytes_long";

    private static final int START_TIMEOUT_SECONDS = 10;
    private static final int COMPLETION_TIMEOUT_SECONDS = 60;
    private static final int TERMINATION_TIMEOUT_SECONDS = 5;

    @Autowired
    private IEventRepository eventRepository;

    private final List<Long> createdEventIds = new ArrayList<>();

    private LogbackSystemLogger logger;
    private INotifier notifier;
    private TokenService tokenService;
    private WaitingQueueRepository queueRepository;
    private WaitingQueueService waitingQueueService;

    @BeforeEach
    void setUp() {
        logger = new LogbackSystemLogger();
        notifier = mock(INotifier.class);

        TokenRepository tokenRepository =
                new TokenRepository();

        tokenService = new TokenService(
                TEST_SECRET,
                tokenRepository,
                logger
        );

        queueRepository =
                new WaitingQueueRepository();

        waitingQueueService =
                new WaitingQueueService(
                        eventRepository,
                        queueRepository,
                        notifier,
                        tokenService,
                        logger
                );
    }

    @AfterEach
    void cleanUpPersistedEvents() {
        for (Long eventId : createdEventIds) {
            try {
                Event persistedEvent =
                        eventRepository.getEventById(eventId);

                if (persistedEvent != null) {
                    eventRepository.deleteEvent(
                            eventId,
                            persistedEvent.getVersion()
                    );
                }
            } catch (Exception exception) {
                /*
                 * Continue cleaning the remaining events.
                 * Cleanup failure should not prevent other events
                 * from being removed.
                 */
                logger.logError(
                        "Failed to clean up event "
                                + eventId
                                + ": "
                                + exception.getMessage(), exception
                );
            }
        }

        createdEventIds.clear();
    }

    @Test
    void Given1000UsersAndCapacity100_WhenTryReserveConcurrently_Then100ApprovedAnd900Queued()
            throws InterruptedException {

        // Arrange
        Event event = createEvent(
                "Music Festival",
                100L
        );

        saveEvent(event);

        Long eventId = event.getId();
        int numberOfUsers = 1000;

        List<String> validTokens =
                createGuestTokens(numberOfUsers);

        AtomicInteger approvedCount =
                new AtomicInteger();

        AtomicInteger queuedCount =
                new AtomicInteger();

        // Act
        runConcurrently(
                numberOfUsers,
                index -> {
                    String result =
                            waitingQueueService.tryReserve(
                                    eventId,
                                    validTokens.get(index)
                            );

                    if ("APPROVED".equals(result)) {
                        approvedCount.incrementAndGet();
                    } else if ("QUEUED".equals(result)) {
                        queuedCount.incrementAndGet();
                    }
                }
        );

        // Assert
        Event updatedEvent =
                eventRepository.getEventById(eventId);

        assertNotNull(updatedEvent);

        assertEquals(
                100,
                approvedCount.get(),
                "Exactly 100 users should be approved."
        );

        assertEquals(
                900,
                queuedCount.get(),
                "Exactly 900 users should be queued."
        );

        assertEquals(
                numberOfUsers,
                approvedCount.get() + queuedCount.get(),
                "Every request should result in either APPROVED or QUEUED."
        );

        assertEquals(
                100,
                updatedEvent.getActiveReservationsCount(),
                "Event should be at its exact maximum capacity."
        );

        assertEquals(
                900,
                queueRepository.getQueueSize(eventId),
                "Queue repository should contain exactly 900 users."
        );
    }

    @Test
    void GivenFullEvent_WhenSameSessionRequestsConcurrently_ThenSessionIsQueuedOnlyOnce()
            throws InterruptedException {

        // Arrange
        Event event = createEvent(
                "Music Festival 2",
                100L
        );

        fillEventToCapacity(event, 100);
        saveEvent(event);

        Long eventId = event.getId();
        int numberOfDuplicateRequests = 100;

        String duplicateSession =
                tokenService.addActiveSession(
                        new Guest()
                );

        // Act
        runConcurrently(
                numberOfDuplicateRequests,
                ignored -> waitingQueueService.tryReserve(
                        eventId,
                        duplicateSession
                )
        );

        // Assert
        Event updatedEvent =
                eventRepository.getEventById(eventId);

        assertNotNull(updatedEvent);

        assertEquals(
                100,
                updatedEvent.getActiveReservationsCount(),
                "Duplicate requests must not increase active reservations."
        );

        assertEquals(
                1,
                queueRepository.getQueueSize(eventId),
                "Concurrent duplicate requests from one session should create only one queue entry."
        );
    }

    @Test
    void GivenFullEventAnd200QueuedUsers_When50SpotsReleasedConcurrently_ThenEventIsRefilledAnd150RemainQueued()
            throws InterruptedException {

        // Arrange
        Event event = createEvent(
                "Music Festival 3",
                100L
        );

        fillEventToCapacity(event, 100);
        saveEvent(event);

        Long eventId = event.getId();

        List<String> queuedTokens =
                createGuestTokens(200);

        for (String queuedToken : queuedTokens) {
            queueRepository.enqueueUser(
                    eventId,
                    queuedToken
            );
        }

        assertEquals(
                200,
                queueRepository.getQueueSize(eventId),
                "The test must begin with exactly 200 queued users."
        );

        int numberOfReleases = 50;

        List<String> releasingUserTokens =
                createGuestTokens(numberOfReleases);

        // Act
        runConcurrently(
                numberOfReleases,
                index -> waitingQueueService.releaseSpot(
                        eventId,
                        releasingUserTokens.get(index)
                )
        );

        // Assert
        Event updatedEvent =
                eventRepository.getEventById(eventId);

        assertNotNull(updatedEvent);

        assertEquals(
                100,
                updatedEvent.getActiveReservationsCount(),
                "Each released spot should immediately be assigned to the next queued user."
        );

        assertEquals(
                150,
                queueRepository.getQueueSize(eventId),
                "Fifty queued users should be approved, leaving exactly 150 users waiting."
        );
    }

    // TODO : fix the implementation to pass this test.
//    @Test
//    void GivenEventHasCapacity_WhenSameSessionRequestsConcurrently_ThenOnlyOneReservationIsApproved()
//            throws InterruptedException {
//
//        // Arrange
//        Event event = createEvent(
//                "Duplicate Approval Event",
//                100L
//        );
//
//        saveEvent(event);
//
//        Long eventId = event.getId();
//
//        String duplicateSession =
//                tokenService.addActiveSession(
//                        new Guest()
//                );
//
//        int numberOfDuplicateRequests = 100;
//
//        AtomicInteger approvedCount =
//                new AtomicInteger();
//
//        AtomicInteger queuedCount =
//                new AtomicInteger();
//
//        // Act
//        runConcurrently(
//                numberOfDuplicateRequests,
//                ignored -> {
//                    String result =
//                            waitingQueueService.tryReserve(
//                                    eventId,
//                                    duplicateSession
//                            );
//
//                    if ("APPROVED".equals(result)) {
//                        approvedCount.incrementAndGet();
//                    } else if ("QUEUED".equals(result)) {
//                        queuedCount.incrementAndGet();
//                    }
//                }
//        );
//
//        // Assert
//        Event updatedEvent =
//                eventRepository.getEventById(eventId);
//
//        assertNotNull(updatedEvent);
//
//        assertEquals(
//                1,
//                updatedEvent.getActiveReservationsCount(),
//                "The same session must occupy only one active reservation."
//        );
//
//        assertEquals(
//                0,
//                queueRepository.getQueueSize(eventId),
//                "The already-approved session should not also enter the queue."
//        );
//
//        assertEquals(
//                1,
//                approvedCount.get(),
//                "Only one concurrent request should approve the session."
//        );
//
//        assertEquals(
//                0,
//                queuedCount.get(),
//                "Duplicate requests from an approved session should not be queued."
//        );
//    }

    private Event createEvent(
            String name,
            long trafficThreshold
    ) {
        return new Event(
                LocalDateTime.now().plusDays(1),
                name,
                1L,
                1L,
                EventLocation.NEW_YORK,
                trafficThreshold,
                EventCategory.CONCERT,
                "Artist Name",
                BigDecimal.valueOf(100),
                new Pair<>(10, 10)
        );
    }

    private void saveEvent(Event event) {
        eventRepository.addEvent(event);

        assertNotNull(
                event.getId(),
                "The database should generate an event ID."
        );

        createdEventIds.add(event.getId());
    }

    private void fillEventToCapacity(
            Event event,
            int capacity
    ) {
        for (int i = 0; i < capacity; i++) {
            event.incrementActiveReservations();
        }
    }

    private List<String> createGuestTokens(int amount) {
        List<String> tokens =
                new ArrayList<>(amount);

        for (int i = 0; i < amount; i++) {
            tokens.add(
                    tokenService.addActiveSession(
                            new Guest()
                    )
            );
        }

        return tokens;
    }

    private void runConcurrently(
            int numberOfTasks,
            IntConsumer task
    ) throws InterruptedException {

        ExecutorService executor =
                Executors.newFixedThreadPool(numberOfTasks);

        CountDownLatch startLatch =
                new CountDownLatch(1);

        CountDownLatch completionLatch =
                new CountDownLatch(numberOfTasks);

        List<Future<?>> futures =
                new ArrayList<>(numberOfTasks);

        try {
            for (int i = 0; i < numberOfTasks; i++) {
                final int taskIndex = i;

                futures.add(
                        executor.submit(() -> {
                            try {
                                boolean started =
                                        startLatch.await(
                                                START_TIMEOUT_SECONDS,
                                                TimeUnit.SECONDS
                                        );

                                if (!started) {
                                    throw new IllegalStateException(
                                            "Timed out while waiting for the concurrent start signal."
                                    );
                                }

                                task.accept(taskIndex);
                            } catch (InterruptedException exception) {
                                Thread.currentThread().interrupt();

                                throw new IllegalStateException(
                                        "Concurrent test task was interrupted.",
                                        exception
                                );
                            } finally {
                                completionLatch.countDown();
                            }
                        })
                );
            }

            startLatch.countDown();

            boolean completed =
                    completionLatch.await(
                            COMPLETION_TIMEOUT_SECONDS,
                            TimeUnit.SECONDS
                    );

            if (!completed) {
                throw new AssertionError(
                        "Concurrent test timed out because one or more tasks did not finish."
                );
            }

            assertTasksCompletedSuccessfully(futures);
        } finally {
            executor.shutdownNow();

            executor.awaitTermination(
                    TERMINATION_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS
            );
        }
    }

    private void assertTasksCompletedSuccessfully(
            List<Future<?>> futures
    ) throws InterruptedException {

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (ExecutionException exception) {
                Throwable cause = exception.getCause();

                throw new AssertionError(
                        "A concurrent task failed.",
                        cause
                );
            }
        }
    }
}

