package ticketsystem.ApplicationLayer;

import java.util.List;

import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.IRepository.IWaitingQueueRepository;
import ticketsystem.DomainLayer.event.Event;

public class WaitingQueueService {

    private final IEventRepository eventRepository;
    private final IWaitingQueueRepository queueRepository;
    private final NotificationsService notificationsService;
    private final TokenService tokenService;
    private final ISystemLogger logger;

    public WaitingQueueService(IEventRepository eventRepository,
            IWaitingQueueRepository queueRepository,
            NotificationsService notificationsService, TokenService tokenService, ISystemLogger logger) {
        this.eventRepository = eventRepository;
        this.queueRepository = queueRepository;
        this.notificationsService = notificationsService;
        this.tokenService = tokenService;
        this.logger = logger;
    }

    public String tryReserve(int eventId, String tokenString) {
        logger.logEvent("tryReserve", "Invoked with parameters - EventID: " + eventId + ", Token: " + tokenString);
        // Validate the token
        if (!(tokenService.validate(tokenString))) {
            logger.logError("tryReserve", "Negative Scenario: Invalid token provided. Token: " + tokenString, null);
            return "ERROR: Invalid token";
        }

        Event event = (Event) eventRepository.getEventById(eventId);
        if (event == null) {
            logger.logError("tryReserve", "Negative Scenario: Attempt to reserve for non-existent event. EventID: " + eventId, null);
            return "ERROR: Event not found";
        }

        if (!event.isOverloaded()) { //if event is not overloaded, approve the user immediately
            event.incrementActiveReservations();
            logger.logEvent("tryReserve", "User with session id" + tokenString + " APPROVED to enter checkout for Event " + eventId);
            return "APPROVED";
        } else { //enqueue the user and return their position in the queue
            queueRepository.enqueueUser(eventId, tokenString);
            int position = queueRepository.getQueueSize(eventId);
            logger.logError("tryReserve", " Event is full. User " + tokenString + " moved to QUEUE. Position: " + position, null);
            return "QUEUED";
        }
    }

    //called when a spot is released to fill the next batch in the queue
    public void processQueue(int eventId) {
        logger.logEvent("processQueue", "Processing queue for EventID: " + eventId);
        Event event = (Event) eventRepository.getEventById(eventId);
        if (event == null) {
            logger.logError("processQueue", "Negative Scenario: Attempt to process queue for non-existent event. EventID: " + eventId, null);
            return;
        }

        long availableSpots = event.getTrafficThreshold() - event.getActiveReservationsCount();

        //if there are available spots, dequeue the next batch of users and approve them
        if (availableSpots > 0) {
            logger.logEvent("processQueue", "Available spots: " + availableSpots + ". Attempting to approve next batch in queue for EventID: " + eventId);
            List<String> approvedUsers = queueRepository.dequeueBatch(eventId, availableSpots);

            if (!approvedUsers.isEmpty()) {
                logger.logEvent("processQueue", "Found approved users for EventID: " + eventId);
                for (String sessionId : approvedUsers) {
                    event.incrementActiveReservations();
                    notificationsService.notifyUser(sessionId, "It's your turn! You can now purchase tickets for Event " + eventId);
                }
            }
        }
    }

    public void releaseSpot(int eventId, String sessionId) {
        logger.logEvent("releaseSpot", "Releasing spot for EventID: " + eventId + " by user with session id: " + sessionId);
        Event event = (Event) eventRepository.getEventById(eventId);
        if (event != null) {
            logger.logError("releaseSpot", "Spot released for EventID: " + eventId + " by user with session id: " + sessionId, null);
            event.decrementActiveReservations();
            processQueue(eventId); //call batch processing to fill the new available spot
        }
    }
}
