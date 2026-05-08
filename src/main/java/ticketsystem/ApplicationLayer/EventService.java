package ticketsystem.ApplicationLayer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.event.Event.eventStatus;
import ticketsystem.DomainLayer.event.EventCategory;
import ticketsystem.DomainLayer.event.EventLocation;
import ticketsystem.DomainLayer.event.EventMap;
import ticketsystem.DomainLayer.event.Pair;
import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.event.Event.eventStatus;
import ticketsystem.DTO.Event.EventDTO;
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

    public void insertEvent(String sessionId, String eventName, Long companyId, LocalDateTime date,
            EventLocation location, Long trafficThreshold, EventCategory category, String artist, BigDecimal price,
            Integer mapHigh, Integer mapWidth) {
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
            validateEventDetails(eventName, date, location, trafficThreshold, category, artist, price);
            if (mapHigh == null || mapHigh <= 0) {
                throw new IllegalArgumentException("Map size must be positive");
            }
            if (mapWidth == null || mapWidth <= 0) {
                throw new IllegalArgumentException("Map size must be positive");
            }
            // main scenario: create and add event
            Long userId = tokenService.extractUserId(sessionId);
            Long eventId = eventRepository.getNextId();

            Event event = new Event(eventId, date, eventName, companyId, userId, location, trafficThreshold, category,
                    artist, price, new Pair<>(mapHigh, mapWidth));
            eventRepository.addEvent(event);
            // logger.servere("Event created successfully: " + event.getName());
        } catch (Exception e) {
            // TODO: handle exception
            // logger.servere("create event failed: " + e.getMessage());
            throw e;
        }

    }

    public Boolean updateEvent(String SessionId, EventDTO eventDTO) {
        try {
            // precondition: user logged in
            if (!tokenService.validateToken(SessionId)) {
                throw new IllegalArgumentException("Invalid session ID");
            }
            // precondition: given data is not null and valid
            if (eventDTO == null) {
                throw new IllegalArgumentException("Event data cannot be null");
            }
            if (eventDTO.id() == null) {
                throw new IllegalArgumentException("Event ID cannot be null");
            }
            if (eventDTO.companyId() == null) {
                throw new IllegalArgumentException("Company ID cannot be null");
            }
            // precondition: user has permission to update event
            if (!membershipDomain.validatePermission(SessionId, eventDTO.companyId(), "event:update")) {
                throw new IllegalArgumentException("User does not have permission to update event");
            }
            // precondition: event exists
            Event existingEvent = eventRepository.getEventById(eventDTO.id());
            if (existingEvent == null) {
                throw new IllegalArgumentException("Event not found");
            }
            if (!existingEvent.getCompanyId().equals(eventDTO.companyId())) {
                throw new IllegalArgumentException("Cannot change event's company");
            }
            if (eventDTO.version() != existingEvent.getVersion()) {
                throw new IllegalStateException("Event was updated by another request");
            }
            String name = eventDTO.name() != null ? eventDTO.name() : existingEvent.getName();
            LocalDateTime date = eventDTO.date() != null ? eventDTO.date() : existingEvent.getDate();
            EventLocation location = eventDTO.location() != null ? EventMapper.toEventLocation(eventDTO.location()): existingEvent.getLocation();
            Long trafficThreshold = eventDTO.trafficThreshold() != null ? eventDTO.trafficThreshold(): existingEvent.getTrafficThreshold();
            EventCategory category = eventDTO.category() != null ? EventMapper.toEventCategory(eventDTO.category()): existingEvent.getCategory();
            String artistName = eventDTO.artistName() != null ? eventDTO.artistName() : existingEvent.getArtistName();
            BigDecimal ticketPrice = eventDTO.ticketPrice() != null ? eventDTO.ticketPrice(): existingEvent.getTicketPrice();
            validateEventDetails(name, date, location, trafficThreshold, category, artistName, ticketPrice);
            existingEvent.updateDetails(name, date, location, trafficThreshold, category, artistName, ticketPrice);
            eventRepository.updateEvent(existingEvent);
            return true;
        } catch (Exception e) {
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

    
    public Boolean deleteEvent(String sessionId, Long eventId) {
        try {
            // precondition: user logged in
            if (!tokenService.validateToken(sessionId)) {
                throw new IllegalArgumentException("Invalid session ID");
            }
            Event event = eventRepository.getEventById(eventId);
            if (event == null) {
                throw new IllegalArgumentException("Event does not exist");
            }
            Long companyId = event.getCompanyId();
            // precondition: user has permission to remove an event
            if (!membershipDomain.validatePermission(sessionId, companyId, "event:remove")) {
                throw new IllegalArgumentException("User does not have permission to remove an event");
            }
            eventStatus status = event.getStatus();
            if (status == eventStatus.ACTIVE || status == eventStatus.DRAFT) {
                throw new IllegalArgumentException("Only inactive or cancelled events can be removed");
            }
            long expectedVersion = eventRepository.getEventById(eventId).getVersion();
            // main scenario: remove event
            eventRepository.deleteEvent(eventId, expectedVersion);
            // logger.servere("Event removed successfully: " + eventId);
            return true;
        } catch (Exception e) {
            //logger.servere("remove event failed: " + e.getMessage());
            throw e;
        }
    }

    public EventMapDTO getEventMap(String sessionId, Long eventId) {
        try {
            // precondition: user logged in
            if (!tokenService.validateToken(sessionId)) {
                throw new IllegalArgumentException("Invalid session ID");
            }
            // precondition: user has permission to view event map
            Event event = eventRepository.getEventById(eventId);
            if (event == null) {
                throw new IllegalArgumentException("Event not found");
            }

            // main scenario: return map
            EventMap map = event.getMap();
            return EventMapDTO.from(map);
        } catch (Exception e) {
            throw e;
        }
    }

    private void validateEventDetails(
            String eventName,
            LocalDateTime date,
            EventLocation location,
            Long trafficThreshold,
            EventCategory category,
            String artist,
            BigDecimal price) {
        if (eventName == null || eventName.isBlank()) {
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

        if (artist == null || artist.isBlank()) {
            throw new IllegalArgumentException("Artist name cannot be null or empty");
        }

        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price must be a non-negative number");
        }
    }


}
