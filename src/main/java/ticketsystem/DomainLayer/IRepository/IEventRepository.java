package ticketsystem.DomainLayer.IRepository;

import ticketsystem.DomainLayer.event.Event;

public interface IEventRepository {

    Event getEventById(long eventId);

    void addEvent(Event event);

    void updateEvent(Event event);

    void deleteEvent(long eventId);
}
