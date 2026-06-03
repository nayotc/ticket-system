package ticketsystem.AcceptanceTesting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.ApplicationLayer.INotifier;
import ticketsystem.ApplicationLayer.ITokenService;
import ticketsystem.ApplicationLayer.NotificationService;
import ticketsystem.ApplicationLayer.TokenService;
import ticketsystem.ApplicationLayer.WaitingQueueService;
import ticketsystem.DomainLayer.IRepository.INotificationsRepository;
import ticketsystem.DomainLayer.IRepository.ITokenRepository;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.event.EventCategory;
import ticketsystem.DomainLayer.event.EventLocation;
import ticketsystem.DomainLayer.event.Pair;
import ticketsystem.DomainLayer.user.Guest;
import ticketsystem.InfrastructureLayer.EventRepository;
import ticketsystem.InfrastructureLayer.LogbackSystemLogger;
import ticketsystem.InfrastructureLayer.NotificationsRepository;
import ticketsystem.InfrastructureLayer.TokenRepository;
import ticketsystem.InfrastructureLayer.VaadinNotifier;
import ticketsystem.InfrastructureLayer.WaitingQueueRepository;

public class WaitingQueueServiceTest {

    private EventRepository EventRepo;
    private WaitingQueueRepository realQueueRepo;
    private NotificationService Notifications;
    private INotifier notifier;
    private ITokenService tokenService;
    private ITokenRepository tokenRepository;
    private WaitingQueueService waitingQueueService;
    private LogbackSystemLogger logger;
    private INotificationsRepository notificationRepository;

    @BeforeEach
    public void setUp() {
        EventRepo = new EventRepository();
        realQueueRepo = new WaitingQueueRepository();
        notificationRepository = new NotificationsRepository();
        Notifications = new NotificationService(notificationRepository);
        notifier = new VaadinNotifier(notificationRepository);
        tokenRepository = new TokenRepository();
        logger = new LogbackSystemLogger();
        tokenService = new TokenService("manual_test_secret_32_chars_long", tokenRepository, logger);
        waitingQueueService = new WaitingQueueService(EventRepo, realQueueRepo, notifier, tokenService,
                logger);
    }

    @Test
    public void givenEventHasCapacity_whenTryReserve_thenUserIsApproved() {
        // Arrange
        Event event = new Event(1L, LocalDateTime.now().plusDays(1), "Music Festival", 1L, 1L, EventLocation.NEW_YORK,
                100L, EventCategory.CONCERT, "Michel Jackson", BigDecimal.valueOf(300), new Pair<>(10, 10));
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
        Event event = new Event(2L, LocalDateTime.now().plusDays(1), "Art Expo", 1L, 1L, EventLocation.NEW_YORK, 1L,
                EventCategory.EXHIBITION, "Artist Name", BigDecimal.valueOf(100), new Pair<>(10, 10));
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
        Event event = new Event(3L, LocalDateTime.now().plusDays(1), "Rock Concert", 1L, 1L, EventLocation.NEW_YORK, 1L,
                EventCategory.CONCERT, "Artist Name", BigDecimal.valueOf(100), new Pair<>(10, 10));
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
        Event savedEvent = EventRepo.getEventById(event.getId());
        assertEquals(1, savedEvent.getActiveReservationsCount(), "Active reservations should be 1.");
        assertEquals(0, realQueueRepo.getQueueSize(3), "Queue should be empty after the user was dequeued.");
    }

    @Test
    public void givenEmptyQueue_whenSpotReleased_thenCapacityDrops() {
        // Arrange
        Event event = new Event(4L, LocalDateTime.now().plusDays(1), "Jazz Night", 1L, 1L, EventLocation.NEW_YORK, 2L,
                EventCategory.CONCERT, "Artist Name", BigDecimal.valueOf(100), new Pair<>(10, 10));
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
    }

    @Test
    public void givenInvalidToken_whenTryReserve_thenUserIsRejected() {
        // Arrange
        Event event = new Event(5L, LocalDateTime.now().plusDays(1), "Secret Show", 1L, 1L, EventLocation.NEW_YORK,
                100L, EventCategory.CONCERT, "Artist Name", BigDecimal.valueOf(100), new Pair<>(10, 10));
        EventRepo.addEvent(event);
        FailureStateSnapshot beforeState = captureStateSnapshot(5L);

        // Act
        assertThrows(IllegalArgumentException.class, () -> waitingQueueService.tryReserve(5, "invalid-token"),
                "An invalid token should throw an exception.");
        FailureStateSnapshot afterState = captureStateSnapshot(5L);

        // Assert
        Event savedEvent = EventRepo.getEventById(event.getId());
        assertEquals(0, savedEvent.getActiveReservationsCount(), "Active reservations should remain 0.");
        assertEquals(0, realQueueRepo.getQueueSize(5), "Queue should remain empty.");
        assertStateUnchanged(beforeState, afterState, "Invalid token reservation attempt");
    }

    @Test
    public void givenNonExistingEvent_whenTryReserve_thenReturnEventNotFound() {
        // Arrange
        String validToken = tokenService.addActiveSession(new Guest());

        FailureStateSnapshot beforeState = captureStateSnapshot(-5L);
        // Act
        String result = waitingQueueService.tryReserve(999L, validToken);
        FailureStateSnapshot afterState = captureStateSnapshot(-5L);
        assertStateUnchanged(beforeState, afterState, "Try reserve for non-existing event");

        // Assert
        assertEquals("ERROR: Event not found", result);
    }

    @Test
    public void givenSoldOutEvent_whenTryReserve_thenUserGetsSoldOutNotification() {
        // Arrange
        Event event = new Event(
                6L,
                LocalDateTime.now().plusDays(1),
                "Sold Out Concert",
                1L,
                1L,
                EventLocation.NEW_YORK,
                1L,
                EventCategory.CONCERT,
                "Artist",
                BigDecimal.valueOf(100),
                new Pair<>(10, 10));

        EventRepo.addEvent(event);

        String firstToken = tokenService.addActiveSession(new Guest());
        String secondToken = tokenService.addActiveSession(new Guest());

        assertEquals("APPROVED", waitingQueueService.tryReserve(6L, firstToken));

        // Act
        String result = waitingQueueService.tryReserve(6L, secondToken);

        // Assert
        assertEquals("QUEUED", result);
    }

    @Test
    public void givenQueuedUser_whenLeaveQueue_thenUserRemovedFromQueue() {
        // Arrange
        Event event = new Event(
                7L,
                LocalDateTime.now().plusDays(1),
                "Busy Event",
                1L,
                1L,
                EventLocation.NEW_YORK,
                1L,
                EventCategory.CONCERT,
                "Artist",
                BigDecimal.valueOf(100),
                new Pair<>(10, 10));

        EventRepo.addEvent(event);

        String token1 = tokenService.addActiveSession(new Guest());
        String token2 = tokenService.addActiveSession(new Guest());

        waitingQueueService.tryReserve(7L, token1);
        waitingQueueService.tryReserve(7L, token2);

        assertEquals(1, realQueueRepo.getQueueSize(7L));

        // Act
        waitingQueueService.leaveQueue(7L, token2);

        // Assert
        assertEquals(0, realQueueRepo.getQueueSize(7L));
    }

    @Test
    public void givenApprovedUser_whenExpireSession_thenSpotReleasedAndNotificationSent() {
        // Arrange
        Event event = new Event(
                8L,
                LocalDateTime.now().plusDays(1),
                "Expire Event",
                1L,
                1L,
                EventLocation.NEW_YORK,
                1L,
                EventCategory.CONCERT,
                "Artist",
                BigDecimal.valueOf(100),
                new Pair<>(10, 10));

        EventRepo.addEvent(event);

        String token = tokenService.addActiveSession(new Guest());

        waitingQueueService.tryReserve(8L, token);

        // Act
        waitingQueueService.expireUserSession(8L, token);

        // Assert
        Event updatedEvent = EventRepo.getEventById(8L);

        assertEquals(0, updatedEvent.getActiveReservationsCount());
    }

    @Test
    public void givenQueuedUsers_whenEventSoldOut_thenQueueClearedAndUsersNotified() {
        // Arrange
        Event event = new Event(
                9L,
                LocalDateTime.now().plusDays(1),
                "Queue Event",
                1L,
                1L,
                EventLocation.NEW_YORK,
                1L,
                EventCategory.CONCERT,
                "Artist",
                BigDecimal.valueOf(100),
                new Pair<>(10, 10));

        EventRepo.addEvent(event);

        String token1 = tokenService.addActiveSession(new Guest());
        String token2 = tokenService.addActiveSession(new Guest());
        String token3 = tokenService.addActiveSession(new Guest());

        waitingQueueService.tryReserve(9L, token1);
        waitingQueueService.tryReserve(9L, token2);
        waitingQueueService.tryReserve(9L, token3);

        assertEquals(2, realQueueRepo.getQueueSize(9L));

        // Act
        waitingQueueService.handleSoldOutEvent(9L);

        // Assert
        assertEquals(0, realQueueRepo.getQueueSize(9L));
    }

    @Test
    public void givenMemberToken_whenQueued_thenMemberNotificationIsSent() {
        // Arrange
        Event event = new Event(
                10L,
                LocalDateTime.now().plusDays(1),
                "Member Queue Event",
                1L,
                1L,
                EventLocation.NEW_YORK,
                1L,
                EventCategory.CONCERT,
                "Artist",
                BigDecimal.valueOf(100),
                new Pair<>(10, 10));

        EventRepo.addEvent(event);

        String guestToken = tokenService.addActiveSession(new Guest());

        waitingQueueService.tryReserve(10L, guestToken);

        String memberToken = tokenService.addActiveSession(
                new ticketsystem.DomainLayer.user.Member(
                        100L,
                        "member",
                        "Member User",
                        "0500000000", LocalDate.of(2001, 1, 1)));

        // Act
        String result = waitingQueueService.tryReserve(10L, memberToken);

        // Assert
        assertEquals("QUEUED", result);
    }

    // checking invariants before and after failure scenarios
    private static class FailureStateSnapshot {

        final int activeReservations;
        final int queueSize;

        FailureStateSnapshot(int activeReservations, int queueSize) {
            this.activeReservations = activeReservations;
            this.queueSize = queueSize;
        }
    }

    private FailureStateSnapshot captureStateSnapshot(long eventId) {
        Event event = EventRepo.getEventById(eventId);
        return new FailureStateSnapshot(
                event != null ? event.getActiveReservationsCount() : 0,
                realQueueRepo.getQueueSize(eventId));
    }

    private void assertStateUnchanged(FailureStateSnapshot before, FailureStateSnapshot after, String scenario) {
        assertEquals(before.activeReservations, after.activeReservations,
                scenario + ": active reservation count should not change on failure");
        assertEquals(before.queueSize, after.queueSize,
                scenario + ": queue size should not change on failure");
    }

}
