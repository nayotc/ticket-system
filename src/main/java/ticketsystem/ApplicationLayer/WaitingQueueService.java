package ticketsystem.ApplicationLayer;

import java.util.List;

import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.IRepository.IWaitingQueueRepository;
import ticketsystem.DomainLayer.event.Event;

public class WaitingQueueService {

    private final IEventRepository eventRepository;
    private final IWaitingQueueRepository queueRepository;
    private final NotificationsService notificationsService;

    public WaitingQueueService(IEventRepository eventRepository,
            IWaitingQueueRepository queueRepository,
            NotificationsService notificationsService) {
        this.eventRepository = eventRepository;
        this.queueRepository = queueRepository;
        this.notificationsService = notificationsService;
    }

    public String tryReserve(int eventId, String sessionId) {
        Event event = (Event) eventRepository.getEventById(eventId);
        if (event == null) {
            return "ERROR: Event not found";
        }

        if (!event.isOverloaded()) {
            event.incrementActiveReservations();
            System.out.println("User with session id" + sessionId + " APPROVED to enter checkout for Event " + eventId);
            return "APPROVED";
        } else {
            queueRepository.enqueueUser(eventId, sessionId);
            int position = queueRepository.getQueueSize(eventId);
            System.out.println("Event is full. User " + sessionId + " moved to QUEUE. Position: " + position);
            return "QUEUED";
        }
    }

    public void processQueue(int eventId) { //called when a spot is released to fill the next batch in the queue
        Event event = (Event) eventRepository.getEventById(eventId);
        if (event == null) {
            return;
        }

        long availableSpots = event.getTrafficThreshold() - event.getActiveReservationsCount();

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
