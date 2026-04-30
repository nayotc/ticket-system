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
import ticketsystem.DomainLayer.MembershipDomainService;




public class EventService {
    
    private final IEventRepository eventRepository;
    private final ITokenService tokenService;
    private final MembershipDomainService membershipDomain;

    public EventService(IEventRepository eventRepository, ITokenService tokenService, MembershipDomainService membershipDomain) {
        this.eventRepository = eventRepository;
        this.tokenService = tokenService;
        this.membershipDomain = membershipDomain;
    }

    public void insertEvent(String sessionId, String eventName, Long companyId, LocalDateTime date, String location, Long trafficThreshold, EventCategory category, Pair<Integer, Integer> mapSize) {
        try {
            // precondition: user logged in
            if (!tokenService.validateToken(sessionId)) {
                throw new IllegalArgumentException("Invalid session ID");
            }
            // precondition: user has permission to create an event
            if (!membershipDomain.validatePermission(sessionId, companyId, "event:create")) {
                throw new IllegalArgumentException("User does not have permission to create an event");
            }

            // main scenario: validate input
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
            // main scenario: create and add event
            Long userId = Long.valueOf(tokenService.extractSubject(sessionId));  // TODO: remove casting
            Long eventId = eventRepository.getMaxId();
            Event event = new Event(eventId, date, eventName, companyId, userId, location, trafficThreshold, category, mapSize);
            eventRepository.addEvent(event);
            // logger.servere("Event created successfully: " + event.getName());
        } catch (Exception e) {
            // TODO: handle exception
            //logger.servere("create event failed: " + e.getMessage());
            throw e;
        }
        
    }

    
}
