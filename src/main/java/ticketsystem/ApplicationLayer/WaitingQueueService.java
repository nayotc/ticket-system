package ticketsystem.ApplicationLayer;

import java.util.List;

import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.IRepository.IWaitingQueueRepository;
import ticketsystem.DomainLayer.event.Event;

public class WaitingQueueService {

    private final IEventRepository eventRepository;
    private final IWaitingQueueRepository queueRepository;
    private final NotificationsService notificationsService;
    private final ITokenService tokenService;

    public WaitingQueueService(IEventRepository eventRepository,
            IWaitingQueueRepository queueRepository,
            NotificationsService notificationsService, ITokenService tokenService) {
        this.eventRepository = eventRepository;
        this.queueRepository = queueRepository;
        this.notificationsService = notificationsService;
        this.tokenService = tokenService;
    }

    public String tryReserve(int eventId, String tokenString) {
        // Validate the token
        if (!(tokenService.validateToken(tokenString))) {
            return "ERROR: Invalid token";
        }

        Event event = (Event) eventRepository.getEventById(eventId);
        if (event == null) {
            return "ERROR: Event not found";
        }

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

    //called when a spot is released to fill the next batch in the queue
    public void processQueue(int eventId) {
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

    public void releaseSpot(int eventId, String sessionId) {
        Event event = (Event) eventRepository.getEventById(eventId);
        if (event != null) {
            event.decrementActiveReservations();
            processQueue(eventId); //call batch processing to fill the new available spot
        }
    }
}
