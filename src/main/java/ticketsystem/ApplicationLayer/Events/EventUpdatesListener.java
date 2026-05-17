package ticketsystem.ApplicationLayer.Events;

import java.time.LocalDateTime;

public interface EventUpdatesListener {

    void onEventCanceled(Long eventId);
    // called when an event is canceled and should trigger order cancellations and user notifications for all affected orders
    void onEventUpdated(Long eventId, LocalDateTime date, String location, String updateMessage);
    // called when an event is updated (e.g., time change, venue change) and should trigger user notifications for all affected orders with the provided update message
    
}
