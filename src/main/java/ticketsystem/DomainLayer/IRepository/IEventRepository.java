package ticketsystem.DomainLayer.IRepository;

import java.util.List;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.SearchCriteria;
import ticketsystem.DomainLayer.event.EventSearchResultView;

public interface IEventRepository {
    void addEvent(Event event);
    Event getEventById(Long eventId);
    void updateEvent(Event event);
    void deleteEvent(Long eventId, long expectedVersion);
    List<Event> getEventsByCompanyId(Long companyId);
    List<Event> getAllEvents();
    List<EventSearchResultView> searchEvents(SearchCriteria criteria, List<Long> companyIds);
    List<EventSearchResultView> getFeaturedEvents(int limit);
}
