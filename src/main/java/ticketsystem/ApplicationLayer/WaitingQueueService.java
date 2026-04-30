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
        //Lock by eventId to ensure atomic checks and increments
        synchronized (getEventLock(eventId)) {
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
        }
    }

    //called when a spot is released to fill the next batch in the queue
    public void processQueue(long eventId) {
        //Lock to prevent exceeding spots when pulling from the queue
        synchronized (getEventLock(eventId)) {
            Event event = (Event) eventRepository.getEventById(eventId);
            if (event == null) {
                return;
            }

            long availableSpots = event.getTrafficThreshold() - event.getActiveReservationsCount();

            //if there are available spots, dequeue the next batch of users and approve them
            if (availableSpots > 0) {
                List<String> approvedUsers = queueRepository.dequeueBatch(eventId, availableSpots);

                if (!approvedUsers.isEmpty()) {
                    for (String sessionId : approvedUsers) {
                        event.incrementActiveReservations();
                        notificationsService.notifyUser(sessionId, "It's your turn! You can now purchase tickets for Event " + eventId);
                    }
                }
            }
        }
    }

    public void releaseSpot(long eventId, String sessionId) {
        // Lock to ensure decrementing doesn't clash with processQueue or tryReserve
        synchronized (getEventLock(eventId)) {
            Event event = (Event) eventRepository.getEventById(eventId);
            if (event != null) {
                event.decrementActiveReservations();
                processQueue(eventId); //call batch processing to fill the new available spot
            }
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
