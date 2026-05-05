package ticketsystem.InfrastructureLayer;

import ticketsystem.DomainLayer.event.Event;

public interface EventRepository {

    public void addEvent(Event event);

    Event getEventById(long eventId);

    void updateEvent(Event event);

    void deleteEvent(long eventId);

}
