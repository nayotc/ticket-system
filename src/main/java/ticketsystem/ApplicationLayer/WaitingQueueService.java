package ticketsystem.ApplicationLayer;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.IRepository.IWaitingQueueRepository;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.InfrastructureLayer.LogbackSystemLogger;

public class WaitingQueueService {

    private final IEventRepository eventRepository;
    private final IWaitingQueueRepository queueRepository;
    private final INotifier notificationsService;
    private final ITokenService tokenService;
    private final ConcurrentHashMap<Long, Object> eventLocks = new ConcurrentHashMap<>();
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
            logger.logEvent("Invalid token provided for reservation attempt.", LogbackSystemLogger.LogLevel.INFO);
            return "ERROR: Invalid token";
        }
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                synchronized (getEventLock(eventId)) {
                    Event event = eventRepository.getEventById(eventId);
                    if (event == null) {
                        logger.logEvent("Attempt to reserve for non-existent event. Event ID: " + eventId,
                                LogbackSystemLogger.LogLevel.INFO);
                        return "ERROR: Event not found";
                    }
                    if (event.isSoldOut()) {
                        logger.logEvent("Attempt to reserve for sold-out event. Event ID: " + eventId,
                                LogbackSystemLogger.LogLevel.INFO);
                    notifyTokenHolder(
                    tokenString,
                    "The event is sold out."
                    );
                        return "ERROR: Sold Out";
                    }

                    if (!event.isOverloaded()) {
                        event.incrementActiveReservations();
                        eventRepository.updateEvent(event);
                        logger.logEvent(
                                "User with session id" + tokenString + " APPROVED to enter checkout for Event "
                                        + eventId,
                                LogbackSystemLogger.LogLevel.INFO);
                        return "APPROVED";
                    }
                    queueRepository.enqueueUser(eventId, tokenString);
                    int position = queueRepository.getQueueSize(eventId);
                    logger.logEvent("Event is full. User " + tokenString + " moved to QUEUE. Position: " + position,
                            LogbackSystemLogger.LogLevel.INFO);
                    notifyTokenHolder(
                    tokenString,
                    "You have entered the waiting queue. Your current position is: " + position + "."
                    );
                    return "QUEUED";
                }
            } catch (Exception e) {
                logger.logError("EXCEPTION CAUGHT: " + e.getMessage(), e);
                e.printStackTrace();
                continue;
            }
        }
        return "ERROR: System is too busy, please try again.";
    }

    private void promoteOneFromWaitingQueue(long eventId) {
        Event tempEvent = eventRepository.getEventById(eventId);
        if (tempEvent == null) {
            return;
        }
        long slack = tempEvent.getTrafficThreshold() - tempEvent.getActiveReservationsCount();
        if (slack <= 0) {
            return;
        }

        List<String> approvedUsers = queueRepository.dequeueBatch(eventId, 1);
        if (approvedUsers.isEmpty()) {
            return;
        }

        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                Event eventToUpdate = eventRepository.getEventById(eventId);
                if (eventToUpdate == null) {
                    return;
                }

                eventToUpdate.incrementActiveReservations();
                eventRepository.updateEvent(eventToUpdate);

                String sessionId = approvedUsers.get(0);
                notifyTokenHolder(sessionId,
                        "It's your turn! You can now purchase tickets for Event " + eventId);
                logger.logEvent("Processed waiting queue for Event " + eventId + ". Approved users: 1",
                        LogbackSystemLogger.LogLevel.INFO);
                return;

            } catch (Exception e) {
                continue;
            }
        }
    }

    public void releaseSpot(long eventId, String sessionId) {
        synchronized (getEventLock(eventId)) {
            int maxRetries = 3;
            boolean updateSuccessful = false;
            for (int i = 0; i < maxRetries; i++) {
                try {
                    Event event = eventRepository.getEventById(eventId);
                    if (event == null) {
                        break;
                    }
                    event.decrementActiveReservations();
                    eventRepository.updateEvent(event);
                    updateSuccessful = true;
                    break;
                } catch (Exception e) {
                    continue;
                }
            }
            if (updateSuccessful) {
                promoteOneFromWaitingQueue(eventId);
                logger.logEvent("User with session id " + sessionId + " released their spot for Event " + eventId,
                        LogbackSystemLogger.LogLevel.INFO);
            }
        }
    }

    public void leaveQueue(long eventId, String sessionId) {
        queueRepository.removeUserFromQueue(eventId, sessionId);
    }

    public void expireUserSession(long eventId, String sessionId) {
        releaseSpot(eventId, sessionId);
        notifyTokenHolder(
        sessionId,
        "Your access time for ticket selection has expired. You were removed from the queue."
        );
    }

    public void handleSoldOutEvent(long eventId) {
        List<String> remainingUsers = queueRepository.clearQueue(eventId);

        notifyTokenHolders(
                remainingUsers,
                "The event is sold out. The waiting queue has been closed."
        );
    }

    private void notifyTokenHolder(String token, String message) {
        if (notificationsService == null || token == null || token.isBlank()
                || message == null || message.isBlank()) {
            return;
        }

        if (tokenService.isMemberToken(token)) {
            Long memberId = tokenService.extractUserId(token);
            if (memberId != null) {
                notificationsService.notifyMember(memberId, message);
                return;
            }
        }

        notificationsService.notifyGuest(token, message);
    }

    private void notifyTokenHolders(List<String> tokens, String message) {
        if (notificationsService == null || tokens == null || tokens.isEmpty()
                || message == null || message.isBlank()) {
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
    }

}
