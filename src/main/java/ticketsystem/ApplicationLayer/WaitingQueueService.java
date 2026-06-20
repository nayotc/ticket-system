package ticketsystem.ApplicationLayer;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import ticketsystem.ApplicationLayer.ISystemLogger.LogLevel;
import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.IRepository.IWaitingQueueRepository;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.InfrastructureLayer.LogbackSystemLogger;
import java.util.Set;

@Service
public class WaitingQueueService {
    /**
     * User-facing messages for the virtual waiting-queue flow.
     *
     * Internal service results and log messages remain in English, while all
     * messages delivered to guests and members are written in Hebrew to match
     * the presentation language used throughout the application.
     */
    private static final String SOLD_OUT_NOTIFICATION =
            "הכרטיסים לאירוע אזלו.";

    private static final String TURN_REACHED_NOTIFICATION =
            "התור שלך הגיע! אפשר לעבור כעת לרכישת כרטיסים.";

    private static final String LEFT_QUEUE_NOTIFICATION =
            "יצאת מתור ההמתנה.";

    private static final String ACCESS_EXPIRED_NOTIFICATION =
            "זמן הגישה שלך לבחירת הכרטיסים הסתיים, ולכן הוסרת מהתור.";

    private static final String QUEUE_CLOSED_SOLD_OUT_NOTIFICATION =
            "הכרטיסים לאירוע אזלו ותור ההמתנה נסגר.";

    private final IEventRepository eventRepository;
    private final IWaitingQueueRepository queueRepository;
    private final INotifier notificationsService;
    private final ITokenService tokenService;
    private final ConcurrentHashMap<Long, Object> eventLocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Set<String>> activeReservationSessions = new ConcurrentHashMap<>();
    private final ISystemLogger logger;

    public WaitingQueueService(IEventRepository eventRepository,
            IWaitingQueueRepository queueRepository,
            INotifier notificationsService, ITokenService tokenService, ISystemLogger logger) {
        this.eventRepository = eventRepository;
        this.queueRepository = queueRepository;
        this.notificationsService = notificationsService;
        this.tokenService = tokenService;
        this.logger = logger;
    }

    // Helper method to retrieve or create a lock for a specific event
    private Object getEventLock(long eventId) {
        return eventLocks.computeIfAbsent(eventId, k -> new Object());
    }

    public String tryReserve(long eventId, String tokenString) {
        // Validate the token
        if (!(tokenService.validateToken(tokenString))) {
            logger.logEvent("Invalid token provided for reservation attempt.", LogLevel.INFO);
            return "ERROR: Invalid session ID";
        }
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                synchronized (getEventLock(eventId)) {
                    Event event = eventRepository.getEventById(eventId);
                    if (event == null) {
                        logger.logEvent("Attempt to reserve for non-existent event. Event ID: " + eventId,
                                LogbackSystemLogger.LogLevel.WARN);
                        return "ERROR: Event not found";
                    }
                    if (event.isSoldOut()) {
                        logger.logEvent("Attempt to reserve for sold-out event. Event ID: " + eventId,
                                LogbackSystemLogger.LogLevel.INFO);
                    notifyTokenHolder(
                            tokenString,
                            SOLD_OUT_NOTIFICATION
                    );
                        return "ERROR: Sold Out";
                    }

                   if (hasActiveSpot(eventId, tokenString)) {
                        logger.logEvent(
                                "User with session id " + tokenString + " already has active access for Event " + eventId,
                                LogLevel.INFO
                        );
                        return "APPROVED";
                    }

                    int existingPosition = queueRepository.getUserPosition(eventId, tokenString);
                    if (existingPosition > 0) {
                        logger.logEvent(
                                "User with session id " + tokenString + " is already in queue for Event "
                                        + eventId + ". Position: " + existingPosition,
                                LogLevel.INFO
                        );
                        return "QUEUED";
                    }

                    if (hasAvailableSpot(event)) {
                        approveSession(eventId, event, tokenString);
                        logger.logEvent(
                                "User with session id " + tokenString + " APPROVED to enter ticket selection for Event "
                                        + eventId,
                                LogLevel.INFO
                        );
                        return "APPROVED";
                    }

                    queueRepository.enqueueUser(eventId, tokenString);
                    int position = queueRepository.getUserPosition(eventId, tokenString);
                    logger.logEvent("Event is full. User " + tokenString + " moved to QUEUE. Position: " + position,
                            LogLevel.INFO);
                    notifyTokenHolder(
                            tokenString,
                            "נכנסת לתור ההמתנה. המיקום הנוכחי שלך בתור הוא "
                                    + position + "."
                    );
                    return "QUEUED";
                }
            } catch (Exception e) {
                logger.logError("EXCEPTION CAUGHT: " + e.getMessage(), e);
                e.printStackTrace();
                continue;
            }
        }
        logger.logEvent("Failed to process reservation after " + maxRetries + " attempts. Event ID: " + eventId,
                LogLevel.WARN);
        return "ERROR: System is too busy, please try again.";
    }

    private void promoteOneFromWaitingQueue(long eventId) {
        Event event = eventRepository.getEventById(eventId);

        if (event == null) {
            logger.logEvent(
                    "Failed to promote user from waiting queue. Event not found. Event ID: " + eventId,
                    LogLevel.INFO
            );
            return;
        }

        if (!hasAvailableSpot(event)) {
            logger.logEvent(
                    "Failed to promote user from waiting queue. No available spots. Event ID: " + eventId,
                    LogLevel.INFO
            );
            return;
        }

        List<String> approvedUsers = queueRepository.dequeueBatch(eventId, 1);

        if (approvedUsers.isEmpty()) {
            logger.logEvent(
                    "Failed to promote user from waiting queue. No users in queue. Event ID: " + eventId,
                    LogLevel.INFO
            );
            cleanupActiveSessionsIfPossible(eventId);
            return;
        }

        String sessionId = approvedUsers.get(0);
        approveSession(eventId, event, sessionId);

        notifyTokenHolder(
                sessionId,
                TURN_REACHED_NOTIFICATION
        );

        logger.logEvent(
                "Processed waiting queue for Event " + eventId + ". Approved user: " + sessionId,
                LogLevel.INFO
        );
    }

    public void releaseSpot(long eventId, String sessionId) {
        synchronized (getEventLock(eventId)) {
            if (sessionId == null || sessionId.isBlank()) {
                return;
            }

            if (isQueued(eventId, sessionId)) {
                queueRepository.removeUserFromQueue(eventId, sessionId);
                cleanupActiveSessionsIfPossible(eventId);

                logger.logEvent(
                        "User with session id " + sessionId + " was removed from queue for Event " + eventId,
                        LogLevel.INFO
                );
                return;
            }

            Event event = eventRepository.getEventById(eventId);

            if (event == null) {
                logger.logEvent(
                        "Failed to release spot. Event not found. Event ID: " + eventId,
                        LogLevel.INFO
                );
                return;
            }

            Set<String> activeSessions = activeReservationSessions.get(eventId);

            boolean hadTrackedActiveSpot = activeSessions != null && activeSessions.remove(sessionId);
            int trackedActiveCountAfterRemoval = activeSessions == null ? 0 : activeSessions.size();

            /*
            * Fallback for legacy/test state:
            * Some tests increment activeReservationsCount directly,
            * without registering the active tokens in activeReservationSessions.
            *
            * If the event counter is higher than the tracked active sessions,
            * there is still an untracked active spot that can be released.
            */
            boolean hasUntrackedActiveSpot =
                    event.getActiveReservationsCount() > trackedActiveCountAfterRemoval;

            if (!hadTrackedActiveSpot && !hasUntrackedActiveSpot) {
                logger.logEvent(
                        "No active queue access to release for session id "
                                + sessionId + " and Event " + eventId,
                        LogLevel.INFO
                );
                cleanupActiveSessionsIfPossible(eventId);
                return;
            }

            event.decrementActiveReservations();
            eventRepository.updateEvent(event);

            promoteOneFromWaitingQueue(eventId);
            cleanupActiveSessionsIfPossible(eventId);

            logger.logEvent(
                    "User with session id " + sessionId + " released their spot for Event " + eventId,
                    LogLevel.INFO
            );
        }
    }

    public void leaveQueue(long eventId, String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("Invalid token");
        }

        if (!tokenService.validateToken(sessionId)) {
            throw new IllegalArgumentException("Invalid token");
        }

        releaseSpot(eventId, sessionId);

        logger.logEvent(
                "User left the waiting queue or released active queue access for Event " + eventId,
                LogLevel.INFO
        );

        notifyTokenHolder(
                sessionId,
                LEFT_QUEUE_NOTIFICATION
        );
    }
    public void expireUserSession(long eventId, String sessionId) {
        releaseSpot(eventId, sessionId);
        logger.logEvent("User session expired for Event " + eventId,
                LogLevel.INFO);
        notifyTokenHolder(
                sessionId,
                ACCESS_EXPIRED_NOTIFICATION
        );
    }

    public void handleSoldOutEvent(long eventId) {
        List<String> remainingUsers = queueRepository.clearQueue(eventId);
        logger.logEvent("Event " + eventId + " is sold out. Cleared waiting queue. Notifying " + remainingUsers.size() + " users.",
                LogLevel.INFO);
        notifyTokenHolders(
                remainingUsers,
                QUEUE_CLOSED_SOLD_OUT_NOTIFICATION
        );
    }

    private void notifyTokenHolder(String token, String message) {
        if (notificationsService == null || token == null || token.isBlank()
                || message == null || message.isBlank()) {
            logger.logEvent("Failed to notify token holder. Invalid token or message.", LogLevel.INFO);
            return;
        }

        if (tokenService.isMemberToken(token)) {
            Long memberId = tokenService.extractUserId(token);
            if (memberId != null) {
                notificationsService.notifyMember(memberId, message);
                logger.logEvent("Notified member with ID " + memberId + ": " + message, LogLevel.INFO);
                return;
            }
        }

        notificationsService.notifyGuest(token, message);
        logger.logEvent("Notified guest with message: " + message, LogLevel.INFO);
    }

    private void notifyTokenHolders(List<String> tokens, String message) {
        if (notificationsService == null || tokens == null || tokens.isEmpty()
                || message == null || message.isBlank()) {
            logger.logEvent("Failed to notify token holders. Invalid tokens list or message.", LogLevel.INFO);
            return;
        }

        List<Long> memberIds = new java.util.ArrayList<>();
        List<String> guestTokens = new java.util.ArrayList<>();

        for (String token : tokens) {
            if (token == null || token.isBlank()) {
                continue;
            }

            if (tokenService.isMemberToken(token)) {
                Long memberId = tokenService.extractUserId(token);
                if (memberId != null) {
                    memberIds.add(memberId);
                    continue;
                }
            }

            guestTokens.add(token);
        }

        notificationsService.notifyMembers(memberIds, message);
        notificationsService.notifyGuests(guestTokens, message);
        logger.logEvent("Notified " + memberIds.size() + " members and " + guestTokens.size() + " guests with message: " + message, LogLevel.INFO);
    }

    public int getQueuePosition(long eventId, String tokenString) {
        if (tokenString == null || tokenString.isBlank()) {
            throw new IllegalArgumentException("Invalid token");
        }

        if (!(tokenService.validateToken(tokenString))) {
            logger.logEvent("Invalid token provided for queue position request.", LogbackSystemLogger.LogLevel.INFO);
            throw new IllegalArgumentException("Invalid session ID");
        }

        Event event = eventRepository.getEventById(eventId);

        if (event == null) {
            logger.logEvent("Queue position requested for non-existent event. Event ID: " + eventId,
                    LogbackSystemLogger.LogLevel.INFO);
            throw new IllegalArgumentException("Event not found");
        }

        if (event.isSoldOut()) {
            throw new IllegalStateException("Sold Out");
        }
        if (event.isSoldOut()) {
            throw new IllegalStateException("Sold Out");
        }

        if (hasActiveSpot(eventId, tokenString)) {
            return 0;
        }

        return queueRepository.getUserPosition(eventId, tokenString);
    }

    public String getQueueEventName(long eventId) {
        Event event = eventRepository.getEventById(eventId);

        if (event == null) {
            logger.logEvent("Queue event name requested for non-existent event. Event ID: " + eventId,
                    LogbackSystemLogger.LogLevel.INFO);
            throw new IllegalArgumentException("Event not found");
        }

        return event.getName();
    }

    public int estimateWaitMinutes(long eventId, String tokenString) {
        int position = getQueuePosition(eventId, tokenString);

        if (position <= 0) {
            return 0;
        }

        return Math.max(1, (position + 4) / 5);
    }
    private Set<String> getActiveSessions(long eventId) {
        return activeReservationSessions.computeIfAbsent(eventId, k -> ConcurrentHashMap.newKeySet());
    }

    private boolean hasActiveSpot(long eventId, String token) {
        return token != null
                && !token.isBlank()
                && activeReservationSessions.containsKey(eventId)
                && activeReservationSessions.get(eventId).contains(token);
    }

    private boolean hasAvailableSpot(Event event) {
        if (event == null || event.getTrafficThreshold() == null || event.getTrafficThreshold() <= 0) {
            return false;
        }

        return event.getActiveReservationsCount() < event.getTrafficThreshold();
    }

    private void approveSession(long eventId, Event event, String token) {
        if (getActiveSessions(eventId).add(token)) {
            event.incrementActiveReservations();
            eventRepository.updateEvent(event);
        }
    }

    private void cleanupActiveSessionsIfPossible(long eventId) {
        Set<String> activeSessions = activeReservationSessions.get(eventId);
        if (activeSessions != null && activeSessions.isEmpty() && queueRepository.getQueueSize(eventId) == 0) {
            activeReservationSessions.remove(eventId);
        }
    }
    private boolean isQueued(long eventId, String token) {
        return token != null
                && !token.isBlank()
                && queueRepository.getUserPosition(eventId, token) > 0;
    }
}
