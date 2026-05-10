package ticketsystem.ApplicationLayer;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.IRepository.IWaitingQueueRepository;
import ticketsystem.DomainLayer.event.Event;

public class WaitingQueueService {

    private final IEventRepository eventRepository;
    private final IWaitingQueueRepository queueRepository;
    private final NotificationsService notificationsService;
    private final ITokenService tokenService;
    private final ConcurrentHashMap<Long, Object> eventLocks = new ConcurrentHashMap<>();

    public WaitingQueueService(IEventRepository eventRepository,
            IWaitingQueueRepository queueRepository,
            NotificationsService notificationsService, ITokenService tokenService) {
        this.eventRepository = eventRepository;
        this.queueRepository = queueRepository;
        this.notificationsService = notificationsService;
        this.tokenService = tokenService;
    }

    // Helper method to retrieve or create a lock for a specific event
    private Object getEventLock(long eventId) {
        return eventLocks.computeIfAbsent(eventId, k -> new Object());
    }

    public String tryReserve(long eventId, String tokenString) {
        // Validate the token
        if (!(tokenService.validateToken(tokenString))) {
            return "ERROR: Invalid token";
        }
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                Event event = (Event) eventRepository.getEventById(eventId);
                if (event == null) {
                    return "ERROR: Event not found";
                }
                // if (event.isSoldOut()) { //      will add when we have event logic pushed!
                //     return new QueueResponse("ERROR: Sold Out");
                // }

                if (!event.isOverloaded()) { //if event is not overloaded, approve the user immediately
                    event.incrementActiveReservations();
                    System.out.println("User with session id" + tokenString + " APPROVED to enter checkout for Event " + eventId);
                    return "APPROVED";
                } else { //enqueue the user and return their position in the queue
                    queueRepository.enqueueUser(eventId, tokenString);
                    int position = queueRepository.getQueueSize(eventId);
                    System.out.println("Event is full. User " + tokenString + " moved to QUEUE. Position: " + position);
                    return "QUEUED";
                }
            } catch (Exception e) { //optimistic locking failure or other concurrency issue, retry the operation a few times before giving up
                continue;
            }
        }
        return "ERROR: System is too busy, please try again.";
    }

    //called when a spot is released to fill the next batch in the queue
    public void processQueue(long eventId) {
        Event tempEvent = (Event) eventRepository.getEventById(eventId);
        if (tempEvent == null) {
            return;
        }
        long availableSpots = tempEvent.getTrafficThreshold() - tempEvent.getActiveReservationsCount();
        if (availableSpots <= 0) {
            return;
        }

        List<String> approvedUsers = queueRepository.dequeueBatch(eventId, availableSpots);
        if (approvedUsers.isEmpty()) {
            return;
        }

        // retry loop to handle optimistic locking when updating the event's active reservations count
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                // to get most updated version of the event before we update the active reservations count
                Event eventToUpdate = (Event) eventRepository.getEventById(eventId);
                if (eventToUpdate == null) {
                    return;
                }

                // increment the active reservations count by the num we approved from the queue
                for (int j = 0; j < approvedUsers.size(); j++) {
                    eventToUpdate.incrementActiveReservations();
                }

                // if version has changed since we read it, exception wiil be thrown
                eventRepository.updateEvent(eventToUpdate);

                for (String sessionId : approvedUsers) {
                    notificationsService.notifyUser(sessionId, "It's your turn! You can now purchase tickets for Event " + eventId);
                }
                return;

            } catch (Exception e) {
                continue;
            }
        }
    }

    public void releaseSpot(long eventId, String sessionId) {
        int maxRetries = 3;
        boolean updateSuccessful = false;
        for (int i = 0; i < maxRetries; i++) {
            try {
                Event event = (Event) eventRepository.getEventById(eventId);
                if (event != null) {
                    event.decrementActiveReservations();
                    eventRepository.updateEvent(event); // try to save the updated event
                }
                updateSuccessful = true;
                break;
            } catch (Exception e) {
                continue;
            }
        }
        if (updateSuccessful) {
            processQueue(eventId); //call batch processing to fill the new available spot
        }
    }

    public void leaveQueue(long eventId, String sessionId) {
        queueRepository.removeUserFromQueue(eventId, sessionId);
    }

    public void expireUserSession(long eventId, String sessionId) {
        releaseSpot(eventId, sessionId);
    }

    public void handleSoldOutEvent(long eventId) {
        List<String> remainingUsers = queueRepository.clearQueue(eventId);
    }

}
