package ticketsystem.AcceptanceTesting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.ApplicationLayer.INotifier;
import ticketsystem.ApplicationLayer.ITokenService;
import ticketsystem.ApplicationLayer.NotificationService;
import ticketsystem.ApplicationLayer.TokenService;
import ticketsystem.ApplicationLayer.WaitingQueueService;
import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.IRepository.INotificationsRepository;
import ticketsystem.DomainLayer.IRepository.ITokenRepository;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.event.EventCategory;
import ticketsystem.DomainLayer.event.EventLocation;
import ticketsystem.DomainLayer.event.Pair;
import ticketsystem.DomainLayer.user.Guest;
import ticketsystem.DomainLayer.event.EventMap;
import ticketsystem.DomainLayer.event.StandingArea;
import ticketsystem.InfrastructureLayer.EventRepository;
import ticketsystem.InfrastructureLayer.LogbackSystemLogger;
import ticketsystem.InfrastructureLayer.InMemoryNotificationsRepository;
import ticketsystem.InfrastructureLayer.TokenRepository;
import ticketsystem.InfrastructureLayer.WaitingQueueRepository;
import ticketsystem.testutil.RecordingNotifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;


@SpringBootTest
@Transactional
public class WaitingQueueServiceTest {

    @Autowired
    private IEventRepository EventRepo;
    private WaitingQueueRepository realQueueRepo;
    private NotificationService Notifications;
    private RecordingNotifier recordingNotifier;
    private INotifier notifier;
    private ITokenService tokenService;
    private ITokenRepository tokenRepository;
    private WaitingQueueService waitingQueueService;
    private LogbackSystemLogger logger;
    private INotificationsRepository notificationRepository;

    @BeforeEach
    public void setUp() {
        realQueueRepo = new WaitingQueueRepository();
        notificationRepository = new InMemoryNotificationsRepository();
        Notifications = new NotificationService(notificationRepository);
        recordingNotifier = new RecordingNotifier();
        notifier = recordingNotifier;
        tokenRepository = new TokenRepository();
        logger = new LogbackSystemLogger();
        tokenService = new TokenService("manual_test_secret_32_chars_long", tokenRepository, logger);
        waitingQueueService = new WaitingQueueService(EventRepo, realQueueRepo, notifier, tokenService,
                logger);
    }

    @Test
    public void givenEventHasCapacity_whenTryReserve_thenUserIsApproved() {
        // Arrange
        Event event = new Event(LocalDateTime.now().plusDays(1), "Music Festival", 1L, 1L, EventLocation.NEW_YORK,
                100L, EventCategory.CONCERT, "Michel Jackson", BigDecimal.valueOf(300), new Pair<>(10, 10));
        EventRepo.addEvent(event);
        Long eventId = event.getId();
        String validToken = tokenService.addActiveSession(new Guest());

        // Act
        String result = waitingQueueService.tryReserve(eventId, validToken);

        // Assert
        assertEquals("APPROVED", result, "User should be approved instantly.");
        Event savedEvent = EventRepo.getEventById(eventId);
        assertEquals(1, savedEvent.getActiveReservationsCount(), "Active reservations should be 1.");
        assertEquals(0, realQueueRepo.getQueueSize(eventId), "Queue should be completely empty.");
    }

   @Test
    public void givenEventIsFull_whenTryReserve_thenUserIsQueued() {
        // Arrange
        Event event = new Event(LocalDateTime.now().plusDays(1), "Art Expo", 1L, 1L, EventLocation.NEW_YORK, 1L,
                EventCategory.EXHIBITION, "Artist Name", BigDecimal.valueOf(100), new Pair<>(10, 10));
        EventRepo.addEvent(event);
        Long eventId = event.getId();
        String approvedToken = tokenService.addActiveSession(new Guest());

        String queuedToken = tokenService.addActiveSession(new Guest());

        assertEquals("APPROVED",waitingQueueService.tryReserve(eventId,approvedToken));

        // Act
        String result = waitingQueueService.tryReserve(eventId,queuedToken);

        // Assert
        assertEquals("QUEUED", result, "User should be queued because event is full.");
        assertEquals(1, realQueueRepo.getQueueSize(eventId), "Queue size should be exactly 1.");
        Event savedEvent = EventRepo.getEventById(eventId);
        assertEquals(1, savedEvent.getActiveReservationsCount(), "Active reservations should be 1.");
        recordingNotifier.assertNotifiedGuest(queuedToken, "נכנסת לתור ההמתנה");
        recordingNotifier.assertNotificationCount(1);
    }

    @Test
    public void givenUserInQueue_whenSpotReleased_thenNextUserIsProcessedAndNotified() {
        // Arrange
        Event event = new Event( LocalDateTime.now().plusDays(1), "Rock Concert", 1L, 1L, EventLocation.NEW_YORK, 1L,
                EventCategory.CONCERT, "Artist Name", BigDecimal.valueOf(100), new Pair<>(10, 10));
        EventRepo.addEvent(event);
        Long eventId = event.getId();

        String validToken1 = tokenService.addActiveSession(new Guest());
        String validToken2 = tokenService.addActiveSession(new Guest());

        String result1 = waitingQueueService.tryReserve(eventId, validToken1);
        String result2 = waitingQueueService.tryReserve(eventId, validToken2);

        assertEquals("APPROVED", result1);
        assertEquals("QUEUED", result2);

        // Act
        waitingQueueService.releaseSpot(eventId, validToken1);

        // Assert
        Event savedEvent = EventRepo.getEventById(event.getId());
        assertEquals(1, savedEvent.getActiveReservationsCount(), "Active reservations should be 1.");
        assertEquals(0, realQueueRepo.getQueueSize(eventId), "Queue should be empty after the user was dequeued.");
        recordingNotifier.assertNotifiedGuest(validToken2, "נכנסת לתור ההמתנה");
        recordingNotifier.assertNotifiedGuest(validToken2, "התור שלך הגיע");
        recordingNotifier.assertNotificationCount(2);
    }

    @Test
    public void givenEmptyQueue_whenSpotReleased_thenCapacityDrops() {
        // Arrange
        Event event = new Event( LocalDateTime.now().plusDays(1), "Jazz Night", 1L, 1L, EventLocation.NEW_YORK, 2L,
                EventCategory.CONCERT, "Artist Name", BigDecimal.valueOf(100), new Pair<>(10, 10));
        EventRepo.addEvent(event);
        Long eventId = event.getId();

        String validToken1 = tokenService.addActiveSession(new Guest());
        String validToken2 = tokenService.addActiveSession(new Guest());

        String result1 = waitingQueueService.tryReserve(eventId, validToken1);
        String result2 = waitingQueueService.tryReserve(eventId, validToken2);

        assertEquals("APPROVED", result1);
        assertEquals("APPROVED", result2);

        // Act
        waitingQueueService.releaseSpot(eventId, validToken2);

        // Assert
        Event savedEvent = EventRepo.getEventById(event.getId());
        assertEquals(1, savedEvent.getActiveReservationsCount(), "Capacity should drop to 1.");
    }

    /**
     * Verifies that an invalid or expired security token is rejected without
     * changing either the event capacity or the waiting queue.
     */
    @Test
    public void givenInvalidToken_whenTryReserve_thenUserIsRejected() {
        // Arrange
        Event event = new Event( LocalDateTime.now().plusDays(1), "Secret Show", 1L, 1L, EventLocation.NEW_YORK,
                100L, EventCategory.CONCERT, "Artist Name", BigDecimal.valueOf(100), new Pair<>(10, 10));
        EventRepo.addEvent(event);
        Long eventId = event.getId();
        FailureStateSnapshot beforeState = captureStateSnapshot(eventId);

        // Act
        assertThrows(IllegalArgumentException.class, () -> waitingQueueService.tryReserve(eventId, "invalid-token"),
                "An invalid token should throw an exception.");
        FailureStateSnapshot afterState = captureStateSnapshot(eventId);

        // Assert
        Event savedEvent = EventRepo.getEventById(event.getId());
        assertEquals(0, savedEvent.getActiveReservationsCount(), "Active reservations should remain 0.");
        assertEquals(0, realQueueRepo.getQueueSize(eventId), "Queue should remain empty.");
        assertStateUnchanged(beforeState, afterState, "Invalid token reservation attempt");
    }

    @Test
    public void givenNonExistingEvent_whenTryReserve_thenReturnEventNotFound() {
        // Arrange
        String validToken = tokenService.addActiveSession(new Guest());
        long nonexistentEventId = Long.MAX_VALUE;
        FailureStateSnapshot beforeState = captureStateSnapshot(nonexistentEventId);
        // Act
        String result = waitingQueueService.tryReserve(nonexistentEventId, validToken);
        FailureStateSnapshot afterState = captureStateSnapshot(nonexistentEventId);
        assertStateUnchanged(beforeState, afterState, "Try reserve for non-existing event");

        // Assert
        assertEquals("ERROR: Event not found", result);
        assertEquals(0, realQueueRepo.getQueueSize(nonexistentEventId));
    }

    @Test
    public void givenSoldOutEvent_whenTryReserve_thenUserGetsSoldOutNotification() {
        // Arrange
        Event event = new Event(
                LocalDateTime.now().plusDays(1),
                "Sold Out Concert",
                1L,
                1L,
                EventLocation.NEW_YORK,
                100L,
                EventCategory.CONCERT,
                "Artist",
                BigDecimal.valueOf(100),
                new Pair<>(10, 10)
        );

        event.setStatus(Event.eventStatus.ACTIVE);

        EventMap map = new EventMap(new Pair<>(10, 10));

        StandingArea standingArea = new StandingArea( "Sold Out Standing Area", new Pair<>(0, 0), new Pair<>(5, 5), 1, BigDecimal.valueOf(100));

        map.addElement(standingArea);
        event.setMap(map);

        EventRepo.addEvent(event);

        Long eventId = event.getId();

        Event storedEvent = EventRepo.getEventById(eventId);

        Long areaId = storedEvent.getMap()
                .getElements()
                .stream()
                .filter(StandingArea.class::isInstance)
                .map(StandingArea.class::cast)
                .map(StandingArea::getId)
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException(
                                "Standing area was not found"
                        )
                );

        /*
         * A standing ticket must first be reserved and then sold.
         * Selling the only ticket makes the event genuinely sold out.
         */
        storedEvent.reserveSpot(areaId, 1);
        storedEvent.sellSpot(areaId, 1);
        storedEvent.SoldOut();

        EventRepo.updateEvent(storedEvent);

        Event persistedSoldOutEvent = EventRepo.getEventById(eventId);

        assertTrue(persistedSoldOutEvent.isSoldOut(),"The test event must be sold out before trying to reserve");

        String token = tokenService.addActiveSession(new Guest());

        // Act
        String result = waitingQueueService.tryReserve(eventId, token);

        // Assert
        assertEquals("ERROR: Sold Out", result);
        assertEquals(0, persistedSoldOutEvent.getActiveReservationsCount(),"A sold-out event must not approve a reservation");
        assertEquals(0,realQueueRepo.getQueueSize(eventId),"A user must not enter the queue for a sold-out event");
        recordingNotifier.assertNotifiedGuest(token, "הכרטיסים לאירוע אזלו");
        recordingNotifier.assertNotificationCount(1);
    }

    @Test
    public void givenQueuedUser_whenLeaveQueue_thenUserRemovedFromQueue() {
        // Arrange
        Event event = new Event(
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
        Long eventId = event.getId();

        String token1 = tokenService.addActiveSession(new Guest());
        String token2 = tokenService.addActiveSession(new Guest());

        waitingQueueService.tryReserve(eventId, token1);
        waitingQueueService.tryReserve(eventId, token2);

        assertEquals(1, realQueueRepo.getQueueSize(eventId));

        // Act
        waitingQueueService.leaveQueue(eventId, token2);

        // Assert
        assertEquals(0, realQueueRepo.getQueueSize(eventId));
        Event storedEvent = EventRepo.getEventById(eventId);
        assertEquals(1,storedEvent.getActiveReservationsCount());
    }

    @Test
    public void givenApprovedUser_whenExpireSession_thenSpotReleasedAndNotificationSent() {
        // Arrange
        Event event = new Event(
                LocalDateTime.now().plusDays(1),
                "Expire Event",
                1L,
                1L,
                EventLocation.NEW_YORK,
                1L,
                EventCategory.CONCERT,
                "Artist",
                BigDecimal.valueOf(100),
                new Pair<>(10, 10)
        );
        EventRepo.addEvent(event);
        Long eventId = event.getId();
        String token = tokenService.addActiveSession(new Guest());
        String reservationResult = waitingQueueService.tryReserve(eventId, token);
        assertEquals("APPROVED", reservationResult);
        Event eventBeforeExpiration = EventRepo.getEventById(eventId);
        assertEquals(1, eventBeforeExpiration.getActiveReservationsCount(),"The user should occupy one active reservation before expiration");
        recordingNotifier.clear();

        // Act
        waitingQueueService.expireUserSession(eventId,token);

        // Assert
        Event updatedEvent =EventRepo.getEventById(eventId);
        assertEquals(0,updatedEvent.getActiveReservationsCount(),"The expired user's reservation spot should be released");
        assertEquals( 0,realQueueRepo.getQueueSize(eventId),"The queue should remain empty");
        recordingNotifier.assertNotifiedGuest(token, "זמן הגישה שלך לבחירת הכרטיסים הסתיים");
        recordingNotifier.assertNotificationCount(1);
    }

    @Test
    public void givenQueuedUsers_whenEventSoldOut_thenQueueClearedAndUsersNotified() {
        // Arrange
        Event event = new Event(
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
        Long eventId = event.getId();

        String approvedToken = tokenService.addActiveSession(new Guest());
        String firstQueuedToken = tokenService.addActiveSession(new Guest());
        String secondQueuedToken = tokenService.addActiveSession(new Guest());

        assertEquals( "APPROVED", waitingQueueService.tryReserve(eventId,approvedToken));
        assertEquals("QUEUED",waitingQueueService.tryReserve(eventId,firstQueuedToken));
        assertEquals("QUEUED",waitingQueueService.tryReserve(eventId,secondQueuedToken));
        assertEquals(2,realQueueRepo.getQueueSize(eventId),"Two users should be waiting before the event becomes sold out");
        recordingNotifier.clear();

        // Act
        waitingQueueService.handleSoldOutEvent(eventId);

        // Assert
        assertEquals(0, realQueueRepo.getQueueSize(eventId),"The waiting queue should be cleared");
        recordingNotifier.assertNotifiedGuest(firstQueuedToken, "תור ההמתנה נסגר");
        recordingNotifier.assertNotifiedGuest(secondQueuedToken, "תור ההמתנה נסגר");
        recordingNotifier.assertNotificationCount(2);
    }

    @Test
    public void givenMemberToken_whenQueued_thenMemberNotificationIsSent() {
        // Arrange
        Event event = new Event(
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
        Long eventId = event.getId();

        String guestToken = tokenService.addActiveSession(new Guest());

        waitingQueueService.tryReserve(eventId, guestToken);

        String memberToken = tokenService.addActiveSession(
                new ticketsystem.DomainLayer.user.Member(
                        100L,
                        "member",
                        "Member User",
                        "0500000000", LocalDate.of(2001, 1, 1)));

        // Act
        String result = waitingQueueService.tryReserve(eventId, memberToken);

        // Assert
        assertEquals("QUEUED", result);
        recordingNotifier.assertNotifiedMember(100L, "נכנסת לתור ההמתנה");
        recordingNotifier.assertNotificationCount(1);
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

    /**
     * Verifies that a user who is approved directly because there is available
     * capacity does not receive a waiting-queue selection-access deadline.
     *
     * Direct approval means the user did not wait in the virtual queue, so the
     * queue access countdown should not be created for that user.
     */
    @Test
    public void givenUserApprovedDirectly_whenCheckingSelectionAccess_thenNoQueueDeadlineExists() {
        // Arrange
        Event event = new Event(
                LocalDateTime.now().plusDays(1),
                "Direct Access Event",
                1L,
                1L,
                EventLocation.NEW_YORK,
                2L,
                EventCategory.CONCERT,
                "Artist Name",
                BigDecimal.valueOf(100),
                new Pair<>(10, 10)
        );
        EventRepo.addEvent(event);

        Long eventId = event.getId();
        String directToken = tokenService.addActiveSession(new Guest());

        // Act
        String result = waitingQueueService.tryReserve(eventId, directToken);

        // Assert
        assertEquals("APPROVED", result, "Direct user should be approved while capacity is available.");
        assertEquals(
                0,
                waitingQueueService.getSelectionAccessSecondsLeft(eventId, directToken),
                "Directly approved user should not receive a waiting-queue selection-access deadline."
        );
    }

    /**
     * Verifies that a user who was waiting in the queue and then got promoted does
     * receive a selection-access deadline.
     *
     * This deadline represents the limited access window granted after the user's
     * turn in the queue arrives.
     */
    @Test
    public void givenQueuedUserPromoted_whenCheckingSelectionAccess_thenQueueDeadlineExists() {
        // Arrange
        Event event = new Event(
                LocalDateTime.now().plusDays(1),
                "Queued Access Event",
                1L,
                1L,
                EventLocation.NEW_YORK,
                1L,
                EventCategory.CONCERT,
                "Artist Name",
                BigDecimal.valueOf(100),
                new Pair<>(10, 10)
        );
        EventRepo.addEvent(event);

        Long eventId = event.getId();
        String directToken = tokenService.addActiveSession(new Guest());
        String queuedToken = tokenService.addActiveSession(new Guest());

        assertEquals("APPROVED", waitingQueueService.tryReserve(eventId, directToken));
        assertEquals("QUEUED", waitingQueueService.tryReserve(eventId, queuedToken));

        assertEquals(
                0,
                waitingQueueService.getSelectionAccessSecondsLeft(eventId, queuedToken),
                "Queued user should not have a selection-access deadline before being promoted."
        );

        // Act
        waitingQueueService.releaseSpot(eventId, directToken);

        // Assert
        assertEquals(0, realQueueRepo.getQueueSize(eventId), "Queue should be empty after promotion.");
        assertTrue(
                waitingQueueService.getSelectionAccessSecondsLeft(eventId, queuedToken) > 0,
                "Promoted queued user should receive a positive selection-access deadline."
        );
    }

    /**
     * Verifies that checking expiration for a directly approved user does not create
     * a queue deadline and does not release the user's active purchasing slot.
     *
     * A missing queue deadline means the user did not enter through the waiting
     * queue, not that their queue access expired.
     */
    @Test
    public void givenDirectlyApprovedUserWithoutQueueDeadline_whenExpireSelectionAccessChecked_thenStateUnchanged() {
        // Arrange
        Event event = new Event(
                LocalDateTime.now().plusDays(1),
                "Direct Expiration Check Event",
                1L,
                1L,
                EventLocation.NEW_YORK,
                1L,
                EventCategory.CONCERT,
                "Artist Name",
                BigDecimal.valueOf(100),
                new Pair<>(10, 10)
        );
        EventRepo.addEvent(event);

        Long eventId = event.getId();
        String directToken = tokenService.addActiveSession(new Guest());
        String queuedToken = tokenService.addActiveSession(new Guest());

        assertEquals("APPROVED", waitingQueueService.tryReserve(eventId, directToken));
        assertEquals("QUEUED", waitingQueueService.tryReserve(eventId, queuedToken));

        recordingNotifier.clear();

        // Act
        boolean expired = waitingQueueService.expireSelectionAccessIfNeeded(eventId, directToken);

        // Assert
        assertFalse(expired, "Directly approved user should not be expired by the queue access timer.");
        assertEquals(
                0,
                waitingQueueService.getSelectionAccessSecondsLeft(eventId, directToken),
                "Expiration check must not create a queue deadline for a directly approved user."
        );

        Event savedEvent = EventRepo.getEventById(eventId);
        assertEquals(
                1,
                savedEvent.getActiveReservationsCount(),
                "Direct user's active purchasing slot should remain active."
        );
        assertEquals(
                1,
                realQueueRepo.getQueueSize(eventId),
                "Queued user should remain waiting because the direct user was not expired."
        );
        assertEquals(
                1,
                realQueueRepo.getUserPosition(eventId, queuedToken),
                "Queued user should remain first in line."
        );
        recordingNotifier.assertNotificationCount(0);
    }

}
