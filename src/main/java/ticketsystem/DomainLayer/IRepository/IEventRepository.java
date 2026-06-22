package ticketsystem.DomainLayer.IRepository;

import java.util.List;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.SearchCriteria;
import ticketsystem.DomainLayer.event.EventSearchResultView;
import ticketsystem.DomainLayer.event.Seat.SeatStatus;

public interface IEventRepository {
    void addEvent(Event event);
    Event getEventById(Long eventId);
    void updateEvent(Event event);
    void deleteEvent(Long eventId, long expectedVersion);
    List<Event> getEventsByCompanyId(Long companyId);
    List<Event> getAllEvents();
    List<EventSearchResultView> searchEvents(SearchCriteria criteria, List<Long> companyIds);
    List<EventSearchResultView> getFeaturedEvents(int limit);
    void updateSeatStatus(Long eventId, Long areaId, int row, int number, SeatStatus newStatus); 
    void updateStandingAreaReservedCount(Long eventId, Long areaId, int reservedDelta); 
    //void markStandingTicketsAsSold(Long eventId, Long areaId, int quantity);
}
