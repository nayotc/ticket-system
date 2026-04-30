package ticketsystem.AcceptanceTesting;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.ApplicationLayer.NotificationsService;
import ticketsystem.ApplicationLayer.TokenService;
import ticketsystem.ApplicationLayer.WaitingQueueService;
import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.event.DiscountPolicy;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.event.EventCategory;
import ticketsystem.DomainLayer.event.PurchasePolicy;
import ticketsystem.InfrastructureLayer.WaitingQueueRepository;

public class WaitingQueueServiceTest {

    private FakeEventRepository fakeEventRepo;
    private WaitingQueueRepository realQueueRepo;
    private FakeNotificationsService fakeNotifications;
    private TokenService TokenService;
    private WaitingQueueService waitingQueueService;

    @BeforeEach
    public void setUp() {
        fakeEventRepo = new FakeEventRepository();
        realQueueRepo = new WaitingQueueRepository();
        fakeNotifications = new FakeNotificationsService();
        TokenService = new TokenService();
        waitingQueueService = new WaitingQueueService(fakeEventRepo, realQueueRepo, fakeNotifications, TokenService);
    }

    @Test
    public void givenEventHasCapacity_whenTryReserve_thenUserIsApproved() {
        // Arrange
        Event event = new Event(1L, "Music Festival", LocalDateTime.now(), "Central Park", 100L, EventCategory.CONCERT, null, new PurchasePolicy("Default"), new DiscountPolicy());
        fakeEventRepo.addEvent(event);
        String validToken = TokenService.generateNewGuestToken();

        // Act
        String result = waitingQueueService.tryReserve(1, validToken);

        // Assert
        assertEquals("APPROVED", result, "User should be approved instantly.");
        assertEquals(1, event.getActiveReservationsCount(), "Active reservations should be 1.");
        assertEquals(0, realQueueRepo.getQueueSize(1), "Queue should be completely empty.");
    }

    @Test
    public void givenEventIsFull_whenTryReserve_thenUserIsQueued() {
        // Arrange
        Event event = new Event(2L, "Art Expo", LocalDateTime.now(), "Central Park", 1L, EventCategory.EXHIBITION, null, new PurchasePolicy("Default"), new DiscountPolicy());
        fakeEventRepo.addEvent(event);
        String validToken = TokenService.generateNewGuestToken();

        waitingQueueService.tryReserve(2, validToken);

        // Act 
        String result = waitingQueueService.tryReserve(2, validToken);

        // Assert
        assertEquals("QUEUED", result, "User should be queued because event is full.");
        assertEquals(1, realQueueRepo.getQueueSize(2), "Queue size should be exactly 1.");
        assertEquals(1, event.getActiveReservationsCount(), "Active reservations should not exceed the maximum capacity.");
    }

    @Test
    public void givenUserInQueue_whenSpotReleased_thenNextUserIsProcessedAndNotified() {
        // Arrange 
        Event event = new Event(3L, "Rock Concert", LocalDateTime.now(), "Central Park", 1L, EventCategory.CONCERT, null, new PurchasePolicy("Default"), new DiscountPolicy());
        fakeEventRepo.addEvent(event);
        String validToken1 = TokenService.generateNewGuestToken();
        String validToken2 = TokenService.generateNewGuestToken();

        waitingQueueService.tryReserve(3, validToken1);
        waitingQueueService.tryReserve(3, validToken2);

        // Act 
        waitingQueueService.releaseSpot(3, validToken1);

        // Assert
        assertTrue(fakeNotifications.notifiedUsers.contains(validToken2), "The queued user should have received a notification.");
        assertEquals(1, event.getActiveReservationsCount(), "Capacity should be full again because the queued user took the spot.");
        assertEquals(0, realQueueRepo.getQueueSize(3), "Queue should be empty after the user was dequeued.");
    }

    @Test
    public void givenEmptyQueue_whenSpotReleased_thenCapacityDrops() {
        // Arrange
        Event event = new Event(4L, "Jazz Night", LocalDateTime.now(), "Central Park", 2L, EventCategory.CONCERT, null, new PurchasePolicy("Default"), new DiscountPolicy());
        fakeEventRepo.addEvent(event);

        String validToken1 = TokenService.generateNewGuestToken();
        String validToken2 = TokenService.generateNewGuestToken();

        waitingQueueService.tryReserve(4, validToken1);
        waitingQueueService.tryReserve(4, validToken2);

        // Act 
        waitingQueueService.releaseSpot(4, validToken2);

        // Assert
        assertEquals(1, event.getActiveReservationsCount(), "Capacity should drop to 1.");
        assertTrue(fakeNotifications.notifiedUsers.isEmpty(), "No notifications should be sent since the queue is empty.");
    }

    @Test
    public void givenInvalidToken_whenTryReserve_thenUserIsRejected() {
        // Arrange
        Event event = new Event(5L, "Secret Show", LocalDateTime.now(), "Central Park", 100L, EventCategory.CONCERT, null, new PurchasePolicy("Default"), new DiscountPolicy());
        fakeEventRepo.addEvent(event);

        // Act
        String result = waitingQueueService.tryReserve(5, "invalid-session");

        // Assert
        assertEquals("ERROR: Invalid token", result, "User with invalid token should be rejected.");
        assertEquals(0, event.getActiveReservationsCount(), "Active reservations should remain 0.");
        assertEquals(0, realQueueRepo.getQueueSize(5), "Queue should remain empty.");
    }

    // Fake Implementations for Acceptance Testing
    private class FakeEventRepository implements IEventRepository {

        private Map<Long, Event> events = new HashMap<>();

        @Override
        public void addEvent(Event event) {
            events.put(event.getId(), event);
        }

        @Override
        public Event getEventById(long id) {
            return events.get(id);
        }

        @Override
        public void deleteEvent(long eventId) {
            events.remove(eventId);
        }

        @Override
        public void updateEvent(Event event) {
            events.put(event.getId(), event);
        }
    }

    private class FakeNotificationsService implements NotificationsService {

        public List<String> notifiedUsers = new ArrayList<>();

        @Override
        public void notifyUser(String sessionId, String message) {
            notifiedUsers.add(sessionId);
        }
    }

}
