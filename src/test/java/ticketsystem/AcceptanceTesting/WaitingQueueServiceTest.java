package ticketsystem.AcceptanceTesting;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.ApplicationLayer.ITokenService;
import ticketsystem.ApplicationLayer.INotifier;
import ticketsystem.ApplicationLayer.TokenService;
import ticketsystem.ApplicationLayer.WaitingQueueService;
import ticketsystem.DomainLayer.IRepository.ITokenRepository;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.event.EventCategory;
import ticketsystem.DomainLayer.event.EventLocation;
import ticketsystem.DomainLayer.event.Pair;
import ticketsystem.DomainLayer.user.Guest;
import ticketsystem.InfrastructureLayer.EventRepository;
import ticketsystem.InfrastructureLayer.LogbackSystemLogger;
import ticketsystem.InfrastructureLayer.TokenRepository;
import ticketsystem.InfrastructureLayer.WaitingQueueRepository;

public class WaitingQueueServiceTest {

    //private FakeEventRepository fakeEventRepo;
    private EventRepository EventRepo;
    private WaitingQueueRepository realQueueRepo;
    private FakeNotificationsService fakeNotifications;
    private ITokenService tokenService;
    private ITokenRepository tokenRepository;
    private WaitingQueueService waitingQueueService;
    private LogbackSystemLogger logger;

    @BeforeEach
    public void setUp() {
        EventRepo = new EventRepository();
        realQueueRepo = new WaitingQueueRepository();
        fakeNotifications = new FakeNotificationsService();
        tokenRepository = new TokenRepository();
        tokenService = new TokenService("manual_test_secret_32_chars_long", tokenRepository);
        logger = new LogbackSystemLogger();
        waitingQueueService = new WaitingQueueService(EventRepo, realQueueRepo, fakeNotifications, tokenService, logger);
    }

    @Test
    public void givenEventHasCapacity_whenTryReserve_thenUserIsApproved() {
        // Arrange
        Event event = new Event(1L, LocalDateTime.now().plusDays(1), "Music Festival", 1L, 1L, EventLocation.NEW_YORK, 100L, EventCategory.CONCERT, "Michel Jackson", BigDecimal.valueOf(300), new Pair<>(10, 10));
        EventRepo.addEvent(event);
        String validToken = tokenService.addActiveSession(new Guest());

        // Act
        String result = waitingQueueService.tryReserve(1, validToken);

        // Assert
        assertEquals("APPROVED", result, "User should be approved instantly.");
        Event savedEvent = EventRepo.getEventById(event.getId());
        assertEquals(1, savedEvent.getActiveReservationsCount(), "Active reservations should be 1.");
        assertEquals(0, realQueueRepo.getQueueSize(1), "Queue should be completely empty.");
    }

    @Test
    public void givenEventIsFull_whenTryReserve_thenUserIsQueued() {
        // Arrange
        Event event = new Event(2L, LocalDateTime.now().plusDays(1), "Art Expo", 1L, 1L, EventLocation.NEW_YORK, 1L, EventCategory.EXHIBITION, "Artist Name", BigDecimal.valueOf(100), new Pair<>(10, 10));
        EventRepo.addEvent(event);
        String validToken = tokenService.addActiveSession(new Guest());

        waitingQueueService.tryReserve(2, validToken);

        // Act 
        String result = waitingQueueService.tryReserve(2, validToken);

        // Assert
        assertEquals("QUEUED", result, "User should be queued because event is full.");
        assertEquals(1, realQueueRepo.getQueueSize(2), "Queue size should be exactly 1.");
        Event savedEvent = EventRepo.getEventById(event.getId());
        assertEquals(1, savedEvent.getActiveReservationsCount(), "Active reservations should be 1.");
    }

    @Test
    public void givenUserInQueue_whenSpotReleased_thenNextUserIsProcessedAndNotified() {
        // Arrange 
        Event event = new Event(3L, LocalDateTime.now().plusDays(1), "Rock Concert", 1L, 1L, EventLocation.NEW_YORK, 1L, EventCategory.CONCERT, "Artist Name", BigDecimal.valueOf(100), new Pair<>(10, 10));
        EventRepo.addEvent(event);

        String validToken1 = tokenService.addActiveSession(new Guest());
        String validToken2 = tokenService.addActiveSession(new Guest());

        String result1 = waitingQueueService.tryReserve(3, validToken1);
        String result2 = waitingQueueService.tryReserve(3, validToken2);

        assertEquals("APPROVED", result1);
        assertEquals("QUEUED", result2);

        // Act 
        waitingQueueService.releaseSpot(3, validToken1);

        // Assert
        assertTrue(fakeNotifications.notifiedUsers.contains(validToken2), "The queued user should have received a notification.");
        Event savedEvent = EventRepo.getEventById(event.getId());
        assertEquals(1, savedEvent.getActiveReservationsCount(), "Active reservations should be 1.");
        assertEquals(0, realQueueRepo.getQueueSize(3), "Queue should be empty after the user was dequeued.");
    }

    @Test
    public void givenEmptyQueue_whenSpotReleased_thenCapacityDrops() {
        // Arrange
        Event event = new Event(4L, LocalDateTime.now().plusDays(1), "Jazz Night", 1L, 1L, EventLocation.NEW_YORK, 2L, EventCategory.CONCERT, "Artist Name", BigDecimal.valueOf(100), new Pair<>(10, 10));
        EventRepo.addEvent(event);

        String validToken1 = tokenService.addActiveSession(new Guest());
        String validToken2 = tokenService.addActiveSession(new Guest());

        String result1 = waitingQueueService.tryReserve(4, validToken1);
        String result2 = waitingQueueService.tryReserve(4, validToken2);

        assertEquals("APPROVED", result1);
        assertEquals("APPROVED", result2);

        // Act 
        waitingQueueService.releaseSpot(4, validToken2);

        // Assert
        Event savedEvent = EventRepo.getEventById(event.getId());
        assertEquals(1, savedEvent.getActiveReservationsCount(), "Capacity should drop to 1.");
        assertTrue(fakeNotifications.notifiedUsers.isEmpty(), "No notifications should be sent since the queue is empty.");
    }

    @Test
    public void givenInvalidToken_whenTryReserve_thenUserIsRejected() {
        // Arrange
        Event event = new Event(5L, LocalDateTime.now().plusDays(1), "Secret Show", 1L, 1L, EventLocation.NEW_YORK, 100L, EventCategory.CONCERT, "Artist Name", BigDecimal.valueOf(100), new Pair<>(10, 10));
        EventRepo.addEvent(event);

        // Act
        assertThrows(IllegalArgumentException.class, () -> waitingQueueService.tryReserve(5, "invalid-token"), "An invalid token should throw an exception.");

        // Assert
        Event savedEvent = EventRepo.getEventById(event.getId());
        assertEquals(0, savedEvent.getActiveReservationsCount(), "Active reservations should remain 0.");
        assertEquals(0, realQueueRepo.getQueueSize(5), "Queue should remain empty.");
    }

    private class FakeNotificationsService implements INotifier {

        public List<String> notifiedUsers = new ArrayList<>();

        @Override
        public void notifyUser(String sessionId, String message) {
            notifiedUsers.add(sessionId);
        }
    }

}
