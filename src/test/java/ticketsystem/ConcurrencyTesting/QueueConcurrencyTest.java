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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import ticketsystem.ApplicationLayer.NotificationsService;
import ticketsystem.ApplicationLayer.TokenService;
import ticketsystem.ApplicationLayer.WaitingQueueService;
import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.event.DiscountPolicy;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.event.EventCategory;
import ticketsystem.DomainLayer.event.EventLocation;
import ticketsystem.DomainLayer.event.Pair;
import ticketsystem.DomainLayer.event.PurchasePolicy;
import ticketsystem.InfrastructureLayer.TokenRepository;
import ticketsystem.InfrastructureLayer.WaitingQueueRepository;

public class QueueConcurrencyTest {

    @Test
    public void testHighConcurrencyLoadOnQueue() throws InterruptedException {
        // fake implementations for testing until we have real ones
        IEventRepository fakeEventRepo = createFakeEventRepo();
        NotificationsService fakeNotifications = createFakeNotifications();
        TokenService fakeTokenService = createFakeTokenService();
        WaitingQueueRepository queueRepo = new WaitingQueueRepository();

        WaitingQueueService queueService = new WaitingQueueService(fakeEventRepo, queueRepo, fakeNotifications, fakeTokenService);
        var event = new Event(1L,LocalDateTime.now().plusDays(1),"Music Festival", 1L,1L,EventLocation.NEW_YORK, 100L, EventCategory.CONCERT,"Artist Name",BigDecimal.valueOf(100),new Pair<>(10, 10));
        fakeEventRepo.addEvent(event);

        int numberOfUsers = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfUsers);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(numberOfUsers);

        // Added list to track futures
        List<Future<?>> futures = new ArrayList<>();

        AtomicInteger approvedCount = new AtomicInteger(0);
        AtomicInteger queuedCount = new AtomicInteger(0);

        // create 1000 tasks simulating users trying to reserve at the same time
        for (int i = 0; i < numberOfUsers; i++) {
            final String sessionId = "User-" + i;
            futures.add(executor.submit(() -> {
                try {
                    // Thread will wait maximum 5 seconds to start
                    if (!latch.await(5, TimeUnit.SECONDS)) {
                        throw new RuntimeException("Timeout waiting for start signal");
                    }

                    String result = queueService.tryReserve(99, sessionId);

                    if ("APPROVED".equals(result)) {
                        approvedCount.incrementAndGet();
                    } else if ("QUEUED".equals(result)) {
                        queuedCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completionLatch.countDown();
                }
            }));
        }

        latch.countDown(); //release all 1000 threads to start processing

        boolean completed = completionLatch.await(10, TimeUnit.SECONDS); //untill all threads finish or timeout
        assertTrue(completed, "Test timed out! One or more threads got stuck and did not finish.");
        executor.shutdown();

        // Check for any exceptions that occurred inside the threads
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        System.out.println("Approved: " + approvedCount.get());
        System.out.println("Queued: " + queuedCount.get());

        assertEquals(100, approvedCount.get(), "Exactly 100 users should be approved.");
        assertEquals(100, event.getActiveReservationsCount(), "Event should be at exact maximum capacity.");
        assertEquals(900, queuedCount.get(), "Exactly 900 users should be queued.");
        assertEquals(900, queueRepo.getQueueSize(99), "Queue repository should hold exactly 900 users.");
    }

    // test to ensure that duplicate requests from the same sessionId are handled correctly
    @Test
    public void testConcurrentDuplicateRequests_ShouldOnlyQueueOnce() throws InterruptedException {
        IEventRepository fakeEventRepo = createFakeEventRepo();
        NotificationsService fakeNotifications = createFakeNotifications();
        TokenService fakeTokenService = createFakeTokenService();
        WaitingQueueRepository queueRepo = new WaitingQueueRepository();

        WaitingQueueService queueService = new WaitingQueueService(fakeEventRepo, queueRepo, fakeNotifications, fakeTokenService);

        // create an event that is already at full capacity
        var event = new Event(1L,LocalDateTime.now().plusDays(1),"Music Festival2", 1L,1L,EventLocation.NEW_YORK, 100L, EventCategory.CONCERT,"Artist Name",BigDecimal.valueOf(100),new Pair<>(10, 10));
        for (int i = 0; i < 100; i++) {
            event.incrementActiveReservations();
        }
        fakeEventRepo.addEvent(event);

        // simulate 100 concurrent requests from the same sessionId
        int numberOfDuplicates = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfDuplicates);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(numberOfDuplicates);

        List<Future<?>> futures = new ArrayList<>();
        final String duplicateSession = "Sneaky-User";

        for (int i = 0; i < numberOfDuplicates; i++) {
            futures.add(executor.submit(() -> {
                try {
                    // Thread will wait maximum 5 seconds to start
                    if (!latch.await(5, TimeUnit.SECONDS)) {
                        throw new RuntimeException("Timeout waiting for start signal");
                    }
                    queueService.tryReserve(99, duplicateSession);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completionLatch.countDown();
                }
            }));
        }

        latch.countDown();
        boolean completed = completionLatch.await(10, TimeUnit.SECONDS);
        assertTrue(completed, "Test timed out! One or more threads got stuck and did not finish.");
        executor.shutdown();

        // Check for any exceptions that occurred inside the threads
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        // ensure that only 1 entry for this sessionId is in the queue, not 100
        assertEquals(1, queueRepo.getQueueSize(99), "Duplicate requests should result in exactly 1 entry in the queue.");
    }

    // @Test
    // public void testConcurrentReleases_ShouldProcessBatchesCorrectly() throws InterruptedException {
    //     IEventRepository fakeEventRepo = createFakeEventRepo();
    //     NotificationsService fakeNotifications = createFakeNotifications();
    //     TokenService fakeTokenService = createFakeTokenService();
    //     WaitingQueueRepository queueRepo = new WaitingQueueRepository();

    //     WaitingQueueService queueService = new WaitingQueueService(fakeEventRepo, queueRepo, fakeNotifications, fakeTokenService);

    //     // full event with 100 active reservations and 200 people in the queue
    //     var event = new Event(1L,LocalDateTime.now().plusDays(1),"Music Festival3", 1L,1L,EventLocation.NEW_YORK, 100L, EventCategory.CONCERT,"Artist Name",BigDecimal.valueOf(100),new Pair<>(10, 10));
    //     for (int i = 0; i < 100; i++) {
    //         event.incrementActiveReservations();
    //     }
    //     fakeEventRepo.addEvent(event);

    //     // put 200 users in the queue
    //     for (int i = 0; i < 200; i++) {
    //         queueRepo.enqueueUser(99, "Waiting-User-" + i);
    //     }

    //     // simulate 50 concurrent spots being released
    //     int numberOfReleases = 50;
    //     ExecutorService executor = Executors.newFixedThreadPool(numberOfReleases);
    //     CountDownLatch latch = new CountDownLatch(1);
    //     CountDownLatch completionLatch = new CountDownLatch(numberOfReleases);

    //     List<Future<?>> futures = new ArrayList<>();

    //     for (int i = 0; i < numberOfReleases; i++) {
    //         final String finishingUser = "Active-User-" + i;
    //         futures.add(executor.submit(() -> {
    //             try {
    //                 // Thread will wait maximum 5 seconds to start
    //                 if (!latch.await(5, TimeUnit.SECONDS)) {
    //                     throw new RuntimeException("Timeout waiting for start signal");
    //                 }
    //                 // every time a spot is released, the next user in the queue should be approved and take that spot, so the event should remain at full capacity
    //                 queueService.releaseSpot(99, finishingUser);
    //             } catch (InterruptedException e) {
    //                 Thread.currentThread().interrupt();
    //             } finally {
    //                 completionLatch.countDown();
    //             }
    //         }));
    //     }

    //     latch.countDown();
    //     boolean completed = completionLatch.await(10, TimeUnit.SECONDS);
    //     assertTrue(completed, "Test timed out! One or more threads got stuck and did not finish.");
    //     executor.shutdown();

    //     // Check for any exceptions that occurred inside the threads
    //     for (Future<?> future : futures) {
    //         try {
    //             future.get();
    //         } catch (ExecutionException e) {
    //             throw new RuntimeException(e);
    //         }
    //     }

    //     assertEquals(100, event.getActiveReservationsCount(), "Event should be instantly refilled to max capacity.");

    //     // originally there were 200 users in the queue, 50 should have been approved and removed, leaving 150 still waiting
    //     assertEquals(150, queueRepo.getQueueSize(99), "Queue should have exactly 150 users left.");
    // }

    // Helper methods for fakes (mocks)
    private IEventRepository createFakeEventRepo() {
        return new IEventRepository() {
            private Event savedEvent;

            public void addEvent(Event event) {
                this.savedEvent = event;
            }

            public Event getEventById(long id) {
                return savedEvent;
            }

            public void deleteEvent(long eventId) {
                /* no-op */
            }

            public void updateEvent(Event event) {
                this.savedEvent = event;
            }

            public long getNextId() {
                return 1L;
            }

            public List<Event> getEventsByCompanyId(long companyId) {
                return List.of(savedEvent);
            }

            public List<Event> getAllEvents() {
                return List.of(savedEvent);
            }
        };
    }

    private NotificationsService createFakeNotifications() {
        return (sessionId, message) -> {
            // Do nothing
        };
    }

    private TokenService createFakeTokenService() {
        return new TokenService("manual_test_secret_32_chars_long", new TokenRepository()) {
            @Override
            public boolean validateToken(String sessionId) {
                return true; // All sessions valid for testing
            }
        };
    }
}
