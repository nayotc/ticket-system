package ticketsystem.AcceptanceTesting;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import ticketsystem.ApplicationLayer.NotificationsService;
import ticketsystem.ApplicationLayer.WaitingQueueService;
import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.InfrastructureLayer.WaitingQueueRepository;

public class QueueConcurrencyTest {

    @Test
    public void testHighConcurrencyLoadOnQueue() throws InterruptedException {
        // fake implementations for testing until we have real ones
        IEventRepository fakeEventRepo = new IEventRepository() {
            private Event savedEvent;

            public void addEvent(Event event) {
                this.savedEvent = event;
            }

            public Event getEventById(long id) {
                return savedEvent;
            }

            public void deleteEvent(long eventId) {
                /* no-op */ }

            public void updateEvent(Event event) {
                this.savedEvent = event;
            }
        };

        NotificationsService fakeNotifications = (sessionId, message) -> {
            // Do nothing
        };

        WaitingQueueRepository queueRepo = new WaitingQueueRepository();

        WaitingQueueService queueService = new WaitingQueueService(fakeEventRepo, queueRepo, fakeNotifications);
        Event event = new Event(99, "Tomorrowland", 100);
        fakeEventRepo.addEvent(event);

        int numberOfUsers = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfUsers);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(numberOfUsers);

        AtomicInteger approvedCount = new AtomicInteger(0);
        AtomicInteger queuedCount = new AtomicInteger(0);

        // create 1000 tasks simulating users trying to reserve at the same time
        for (int i = 0; i < numberOfUsers; i++) {
            final String sessionId = "User-" + i;
            executor.submit(() -> {
                try {
                    latch.await();

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
            });
        }

        latch.countDown(); //release all 1000 threads to start processing

        completionLatch.await(); //until all threads have finished
        executor.shutdown();

        System.out.println("Approved: " + approvedCount.get());
        System.out.println("Queued: " + queuedCount.get());

        assertEquals(100, approvedCount.get(), "Exactly 100 users should be approved.");
        assertEquals(100, event.getActiveReservationsCount(), "Event should be at exact maximum capacity.");
        assertEquals(900, queuedCount.get(), "Exactly 900 users should be queued.");
        assertEquals(900, queueRepo.getQueueSize(99), "Queue repository should hold exactly 900 users.");
    }

}
