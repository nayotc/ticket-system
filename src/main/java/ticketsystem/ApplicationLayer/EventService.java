package ticketsystem.ApplicationLayer;

import java.time.LocalDateTime;
import java.util.List;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.event.EventCategory;
import ticketsystem.DomainLayer.event.EventMap;
import ticketsystem.DomainLayer.event.Pair;
import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.event.PurchasePolicy;
import ticketsystem.DomainLayer.event.DiscountPolicy;



public class EventService {
    
    private final IEventRepository eventRepository;
    private final ITokenService tokenService;

    public EventService(IEventRepository eventRepository, ITokenService tokenService) {
        this.eventRepository = eventRepository;
        this.tokenService = tokenService;
    }

    public void insertEvent(String sessionId, String eventName, Long companyId, LocalDateTime date, String location, Long trafficThreshold, EventCategory category, Pair<Integer, Integer> mapSize) {
        try {
            if (!tokenService.validateToken(sessionId)) {
                throw new IllegalArgumentException("Invalid session ID");
            }
            if (eventName == null || eventName.isEmpty()) {
                throw new IllegalArgumentException("Event name cannot be null or empty");
            }
            if (date == null || date.isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("Event date must be in the future");
            }
            if (location == null || location.isEmpty()) {
                throw new IllegalArgumentException("Event location cannot be null or empty");
            }
            if (trafficThreshold == null || trafficThreshold <= 0) {
                throw new IllegalArgumentException("Traffic threshold must be a positive number");
            }
            if (category == null) {
                throw new IllegalArgumentException("Event category cannot be null");
            }
            if (mapSize == null || mapSize.getFirst() <= 0 || mapSize.getSecond() <= 0) {
                throw new IllegalArgumentException("Map size must be positive");
            }
            if (!MembershipService.validatePermission(sessionId, companyId, "CREATE_EVENT")) {
                throw new IllegalArgumentException("User does not have permission to create an event");
            }
            Long userId = Long.valueOf(tokenService.extractSubject(sessionId));  // TODO: remove casting
            Long eventId = eventRepository.getMaxId();
            Event event = new Event(eventId, date, eventName, companyId, userId, location, trafficThreshold, category, mapSize);
            eventRepository.addEvent(event);
        } catch (Exception e) {
            // TODO: handle exception
        }
        
    }

    
}
