package ticketsystem.ApplicationLayer;

import java.time.LocalDateTime;
import java.util.List;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.event.EventMap;
import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.event.PurchasePolicy;
import ticketsystem.DomainLayer.event.DiscountPolicy;

public class EventService {
    
    private final IEventRepository eventRepository;

    public EventService(IEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public void insertEvent(String sessionId, String eventName, Long companyId, LocalDateTime date, String location, Long trafficThreshold, EventCategory category, EventMap map, PurchasePolicy purchasePolicy, DiscountPolicy discountPolicy) {
        Long eventId = eventRepository.getMaxId();
        Event event = new Event(eventId, eventName, companyId, date, location, trafficThreshold, category, map, purchasePolicy, discountPolicy);
        eventRepository.addEvent(event);
    }

    
}
