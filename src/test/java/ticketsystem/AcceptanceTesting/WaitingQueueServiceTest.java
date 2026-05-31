package ticketsystem.AcceptanceTesting;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.ApplicationLayer.INotifier;
import ticketsystem.ApplicationLayer.ITokenService;
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

    // private FakeEventRepository fakeEventRepo;
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
        tokenService = new TokenService("manual_test_secret_32_chars_long", tokenRepository, logger);
        logger = new LogbackSystemLogger();
        waitingQueueService = new WaitingQueueService(EventRepo, realQueueRepo, fakeNotifications, tokenService,
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
        assertTrue(fakeNotifications.wasNotified(validToken2),
                "The queued user should have received a notification.");
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
        assertEquals(0,
                fakeNotifications.notificationCount(validToken1)
                + fakeNotifications.notificationCount(validToken2),
                "No notifications should be sent since the queue is empty.");
    }

    @Test
    public void givenInvalidToken_whenTryReserve_thenUserIsRejected() {
        // Arrange
        Event event = new Event(5L, LocalDateTime.now().plusDays(1), "Secret Show", 1L, 1L, EventLocation.NEW_YORK,
                100L, EventCategory.CONCERT, "Artist Name", BigDecimal.valueOf(100), new Pair<>(10, 10));
        EventRepo.addEvent(event);

        // Act
        assertThrows(IllegalArgumentException.class, () -> waitingQueueService.tryReserve(5, "invalid-token"),
                "An invalid token should throw an exception.");

        // Assert
        Event savedEvent = EventRepo.getEventById(event.getId());
        assertEquals(0, savedEvent.getActiveReservationsCount(), "Active reservations should remain 0.");
        assertEquals(0, realQueueRepo.getQueueSize(5), "Queue should remain empty.");
    }

    private static class FakeNotificationsService implements INotifier {

        private final Map<String, List<String>> messagesBySession = new HashMap<>();
        private final Map<Long, List<String>> messagesByMember = new HashMap<>();
        private final List<String> allMessages = new ArrayList<>();

        @Override
        public void notifyGuest(String sessionId, String message) {
            messagesBySession
                    .computeIfAbsent(sessionId, key -> new ArrayList<>())
                    .add(message);

            allMessages.add(message);
        }

        @Override
        public void notifyMember(Long memberId, String message) {
            messagesByMember
                    .computeIfAbsent(memberId, key -> new ArrayList<>())
                    .add(message);

            allMessages.add(message);
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

        boolean wasNotified(String sessionId) {
            return messagesBySession.containsKey(sessionId)
                    && !messagesBySession.get(sessionId).isEmpty();
        }

        int notificationCount(String sessionId) {
            return messagesBySession
                    .getOrDefault(sessionId, List.of())
                    .size();
        }

        String lastMessageFor(String sessionId) {
            List<String> messages = messagesBySession.getOrDefault(sessionId, List.of());

            if (messages.isEmpty()) {
                return "";
            }

            return messages.get(messages.size() - 1);
        }

        boolean wasMemberNotified(Long memberId) {
            return messagesByMember.containsKey(memberId)
                    && !messagesByMember.get(memberId).isEmpty();
        }

        int memberNotificationCount(Long memberId) {
            return messagesByMember
                    .getOrDefault(memberId, List.of())
                    .size();
        }

        String lastMessageForMember(Long memberId) {
            List<String> messages = messagesByMember.getOrDefault(memberId, List.of());

            if (messages.isEmpty()) {
                return "";
            }

            return messages.get(messages.size() - 1);
        }

        boolean containsMessage(String text) {
            return allMessages.stream()
                    .anyMatch(message -> message.contains(text));
        }
    }

    @Test
    public void givenNonExistingEvent_whenTryReserve_thenReturnEventNotFound() {
        // Arrange
        String validToken = tokenService.addActiveSession(new Guest());

        // Act
        String result = waitingQueueService.tryReserve(999L, validToken);

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

        // ממלאים את המקום היחיד
        assertEquals("APPROVED", waitingQueueService.tryReserve(6L, firstToken));

        // Act
        String result = waitingQueueService.tryReserve(6L, secondToken);

        // Assert
        assertEquals("QUEUED", result);
        assertTrue(fakeNotifications.wasNotified(secondToken));
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

        assertTrue(fakeNotifications.wasNotified(token));

        assertTrue(
                fakeNotifications.lastMessageFor(token)
                        .contains("expired"));
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

        assertTrue(fakeNotifications.wasNotified(token2));
        assertTrue(fakeNotifications.wasNotified(token3));

        assertTrue(fakeNotifications.containsMessage("sold out"));
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
                        "0500000000"));

        // Act
        String result = waitingQueueService.tryReserve(10L, memberToken);

        // Assert
        assertEquals("QUEUED", result);

        assertTrue(fakeNotifications.wasMemberNotified(100L));
    }

}
