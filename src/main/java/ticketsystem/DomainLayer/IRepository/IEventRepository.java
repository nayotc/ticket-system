package ticketsystem.DomainLayer.IRepository;

import java.util.List;
import ticketsystem.DomainLayer.event.Event;

public interface IEventRepository {
    void addEvent(Event event);
    Event getEventById(Long eventId);
    void updateEvent(Event event);
    void deleteEvent(Long eventId, long expectedVersion);
    List<Event> getEventsByCompanyId(Long companyId);
    List<Event> getAllEvents();
}
