package ticketsystem.ApplicationLayer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.event.EventCategory;
import ticketsystem.DomainLayer.event.EventLocation;
import ticketsystem.DomainLayer.event.EventMap;
import ticketsystem.DomainLayer.event.Pair;
import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.event.PurchasePolicy;
import ticketsystem.DomainLayer.event.Event.eventStatus;
import ticketsystem.DomainLayer.event.DiscountPolicy;
import ticketsystem.DTO.Event.EventMapDTO;
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

    public void insertEvent(String sessionId, String eventName, Long companyId, LocalDateTime date, EventLocation location, Long trafficThreshold, EventCategory category, String artist, BigDecimal price, Integer mapHigh, Integer mapWidth) {
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
            if (location == null) {
                throw new IllegalArgumentException("Event location cannot be null");
            }
            if (trafficThreshold == null || trafficThreshold <= 0) {
                throw new IllegalArgumentException("Traffic threshold must be a positive number");
            }
            if (category == null) {
                throw new IllegalArgumentException("Event category cannot be null");
            }
            if (artist == null || artist.isEmpty()) {
                throw new IllegalArgumentException("Artist name cannot be null or empty");
            }
            if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Price must be a non-negative number");
            }
            if (mapHigh == null || mapHigh <= 0) {
                throw new IllegalArgumentException("Map size must be positive");
            }
            if (mapWidth == null || mapWidth <= 0) {
                throw new IllegalArgumentException("Map size must be positive");
            }
            // main scenario: create and add event
            Long userId = tokenService.extractUserId(sessionId);  // TODO: remove casting
            Long eventId = eventRepository.getNextId();

            Event event = new Event(eventId, date, eventName, companyId, userId, location, trafficThreshold, category,artist,price,new Pair<>(mapHigh, mapWidth));
            eventRepository.addEvent(event);
            // logger.servere("Event created successfully: " + event.getName());
        } catch (Exception e) {
            // TODO: handle exception
            //logger.servere("create event failed: " + e.getMessage());
            throw e;
        }
        
    }

    public Boolean defineEventMap(String sessionId, Long eventId, EventMapDTO mapDTO) {
        try {
            // precondition: user logged in
            if (!tokenService.validateToken(sessionId)) {
                throw new IllegalArgumentException("Invalid session ID");
            }
            // precondition: user has permission to define event map
            Event event = eventRepository.getEventById(eventId);
            if (event == null) {
                throw new IllegalArgumentException("Event not found");
            }
            if (!membershipDomain.validatePermission(sessionId, event.getCompanyId(), "event:defineMap")) {
                throw new IllegalArgumentException("User does not have permission to define event map");
            }

            // main scenario: create map
            if (mapDTO == null) {
                throw new IllegalArgumentException("Map data cannot be null");
            }
            EventMap map = EventMapper.toDomain(mapDTO);
            event.setMap(map);
            event.setStatus(eventStatus.ACTIVE);
            eventRepository.updateEvent(event);
            return true;
        } catch (Exception e) {
            throw e;
        }
    }
}
