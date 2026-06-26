package ticketsystem.ApplicationLayer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import ticketsystem.ApplicationLayer.Events.EventUpdatesListener;
import ticketsystem.ApplicationLayer.ISystemLogger.LogLevel;
import ticketsystem.DTO.Event.*;
import ticketsystem.DTO.DiscountPolicyDTO;
import ticketsystem.DTO.PurchasePolicyDTO;
import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.MembershipDomainService;
import ticketsystem.DomainLayer.discount.DiscountCompositionType;
import ticketsystem.DomainLayer.discount.DiscountPolicy;
import ticketsystem.DomainLayer.event.*;
import ticketsystem.DomainLayer.event.Event.eventStatus;
import ticketsystem.DomainLayer.policy.PurchasePolicy;
import ticketsystem.DomainLayer.user.Permission;

import org.springframework.beans.factory.annotation.Autowired;

@Service
public class EventService {

    private final IEventRepository eventRepository;
    private final ITokenService tokenService;
    private final MembershipDomainService membershipDomain;
    private final List<EventUpdatesListener> eventUpdatesListeners = new ArrayList<>();
    private final ISystemLogger logger;
    private final PurchasePolicyMapper mapper = new PurchasePolicyMapper();
    private final DiscountPolicyMapper discountMapper = new DiscountPolicyMapper();
    private final UserAccessService userAccessService;

    @Autowired
    public EventService(IEventRepository eventRepository, ITokenService tokenService,
                        MembershipDomainService membershipDomain, ISystemLogger logger,
                        UserAccessService userAccessService) {
        this.eventRepository = eventRepository;
        this.tokenService = tokenService;
        this.membershipDomain = membershipDomain;
        this.logger = logger;
        this.userAccessService = userAccessService;
    }

    public Long insertEvent(String sessionId, String eventName, Long companyId, LocalDateTime date,
                            EventLocation location, Long trafficThreshold, EventCategory category, String artist, BigDecimal price,
                            Integer mapHigh, Integer mapWidth) {

        String context = "SessionId=" + sessionId
                + ", companyId=" + companyId
                + ", eventName=" + eventName
                + ", date=" + date
                + ", location=" + location
                + ", category=" + category
                + ", mapSize=" + mapHigh + "x" + mapWidth;

        logger.logEvent("Started - insertEvent. " + context, LogLevel.INFO);

        try {
            // precondition: user logged in
            if (!tokenService.validateToken(sessionId)) {
                throw new IllegalArgumentException("Invalid session ID");
            }
            Long userId = tokenService.extractUserId(sessionId);
            logger.logEvent("Authenticated actor - insertEvent. userId=" + userId + ", companyId=" + companyId, LogLevel.DEBUG);
            // precondition: user has permission to create an event
            userAccessService.validateCanPerformNonViewAction(userId);
            if (!membershipDomain.validatePermission(userId, companyId, Permission.MANAGE_EVENT_INVENTORY)) {
                throw new IllegalArgumentException("User does not have permission to create an event");
            }
            logger.logEvent("Checked permissions - insertEvent. userId=" + userId + ", companyId=" + companyId + "permission=" + Permission.MANAGE_EVENT_INVENTORY, LogLevel.DEBUG);

            // main scenario: validate input
            validateEventDetails(eventName, date, location, trafficThreshold, category, artist, price);
            if (mapHigh == null || mapHigh <= 0) {
                throw new IllegalArgumentException("Map size must be positive");
            }
            if (mapWidth == null || mapWidth <= 0) {
                throw new IllegalArgumentException("Map size must be positive");
            }
            // main scenario: create and add event

            Event event = new Event(date, eventName, companyId, userId, location, trafficThreshold, category,
                    artist, price, new Pair<>(mapHigh, mapWidth));
            eventRepository.addEvent(event);
            logger.logEvent("Completed - insertEvent. eventId=" + event.getId() + ", companyId=" + companyId + ", " + event.toString(), LogLevel.INFO);
            return event.getId();
        } catch (IllegalArgumentException e) {
            logger.logEvent("Failed - insertEvent. " + context + ". Error: " + e.getMessage(), LogLevel.WARN);
            throw e;
        } catch (Exception e) {
            logger.logError("Failed - insertEvent. " + context + ". Unexpected error: " + e.getMessage(), e);
            throw e;
        }

    }

    public Boolean updateEvent(String SessionId, EventDTO eventDTO) {
        String name;
        LocalDateTime date;
        EventLocation location;
        BigDecimal ticketPrice;
        boolean notificateUsers = false;
        String context = eventDTOContext(eventDTO);
        logger.logEvent("Use-case started: updateEvent. " + context, LogLevel.INFO);

        try {
            // precondition: user logged in
            if (!tokenService.validateToken(SessionId)) {
                throw new IllegalArgumentException("Invalid session ID");
            }
            logger.logEvent("Authenticated actor - updateEvent. " + context, LogLevel.DEBUG);
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
            logger.logEvent("Validated input - updateEvent. " + context, LogLevel.DEBUG);
            // precondition: user has permission to update event
            Long userId = tokenService.extractUserId(SessionId);
            userAccessService.validateCanPerformNonViewAction(userId);
            if (!membershipDomain.validatePermission(userId, eventDTO.companyId(), Permission.MANAGE_EVENT_INVENTORY)) {
                throw new IllegalArgumentException("User does not have permission to update event");
            }
            logger.logEvent("Checked permissions - updateEvent. userId=" + userId + ", companyId=" + eventDTO.companyId() + "permission=" + Permission.MANAGE_EVENT_INVENTORY, LogLevel.DEBUG);
            // precondition: event exists
            Long eventId = eventDTO.id();
            if (eventId == null) {
                throw new IllegalArgumentException("Event ID cannot be null");
            }
            Event existingEvent = eventRepository.getEventById(eventId);
            if (existingEvent == null) {
                throw new IllegalArgumentException("Event not found");
            }
            logger.logEvent("Found existing event - updateEvent. eventId=" + existingEvent.getId(), LogLevel.DEBUG);
            // validate that the event belongs to the same company and version matches
            if (!existingEvent.getCompanyId().equals(eventDTO.companyId())) {
                throw new IllegalArgumentException("Cannot change event's company");
            }
            if (eventDTO.version() != existingEvent.getVersion()) {
                throw new IllegalStateException("Event was updated by another request");
            }

            // main scenario: update event details and notify users if needed
            String message = "Event " + existingEvent.getName() + " has been updated. New details: /n";
            if (eventDTO.name() != null) {
                if (existingEvent.getStatus() == eventStatus.ACTIVE) {
                    throw new IllegalStateException("Cannot change name of an active event");
                }
                name = eventDTO.name();
            } else {
                name = existingEvent.getName();
            }
            if (eventDTO.date() != null) {
                date = eventDTO.date();
                message += "New Date: " + eventDTO.date().toString() + "/n";
                notificateUsers = true;
            } else {
                date = existingEvent.getDate();
            }
            if (eventDTO.location() != null) {
                location = EventMapper.toEventLocation(eventDTO.location());
                message += "New Location: " + eventDTO.location().toString() + "/n";
                notificateUsers = true;
            } else {
                location = existingEvent.getLocation();
            }

            if (eventDTO.ticketPrice() != null) {
                if (existingEvent.getStatus() == eventStatus.ACTIVE) {
                    throw new IllegalStateException("Cannot change ticket price of an active event");
                }
                ticketPrice = eventDTO.ticketPrice();
            } else {
                ticketPrice = existingEvent.getMinimalTicketPrice();
            }
            Long trafficThreshold = eventDTO.trafficThreshold() != null ? eventDTO.trafficThreshold() : existingEvent.getTrafficThreshold();
            EventCategory category = eventDTO.category() != null ? EventMapper.toEventCategory(eventDTO.category()) : existingEvent.getCategory();
            String artistName = eventDTO.artistName() != null ? eventDTO.artistName() : existingEvent.getArtistName();
            logger.logEvent("update values - updateEvent. " + context, LogLevel.DEBUG);
            validateEventDetails(name, date, location, trafficThreshold, category, artistName, ticketPrice);
            logger.logEvent("Validated details - updateEvent. " + context, LogLevel.DEBUG);
            if (notificateUsers) {
                notifyEventUpdatedListeners(existingEvent.getId(), eventDTO.date(), eventDTO.location(), message);
                logger.logEvent("Notified event update listeners - updateEvent. " + context, LogLevel.DEBUG);
            }
            existingEvent.updateDetails(name, date, location, trafficThreshold, category, artistName, ticketPrice);
            eventRepository.updateEvent(existingEvent);
            logger.logEvent("Completed - updateEvent. " + context, LogLevel.INFO);
            return true;
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.logEvent("Failed - updateEvent. " + context + ". Error: " + e.getMessage(), LogLevel.WARN);
            throw e;
        } catch (Exception e) {
            logger.logError("Failed - updateEvent. " + context + ". Unexpected error: " + e.getMessage(), e);
            throw e;
        }
    }

    public Boolean defineEventMap(String sessionId, Long eventId, EventMapDTO mapDTO) {
        String context = "eventId=" + eventId + ", mapProvided=" + (mapDTO != null);
        logger.logEvent("Started - defineEventMap. " + context, LogLevel.INFO);
        try {
            if (!tokenService.validateToken(sessionId)) {
                throw new IllegalArgumentException("Invalid session ID");
            }
            logger.logEvent("Authenticated actor - defineEventMap. " + context, LogLevel.DEBUG);

            Event event = eventRepository.getEventById(eventId);
            if (event == null) {
                throw new IllegalArgumentException("Event not found");
            }
            logger.logEvent("Found event - defineEventMap. " + context, LogLevel.DEBUG);
            Long userId = tokenService.extractUserId(sessionId);
            userAccessService.validateCanPerformNonViewAction(userId);
            if (!membershipDomain.validatePermission(userId, event.getCompanyId(), Permission.CONFIGURE_HALL_AND_MAP)) {
                throw new IllegalArgumentException("User does not have permission to define event map");
            }

            logger.logEvent(
                    "Validated permission - defineEventMap. userId=" + userId
                            + ", eventId=" + eventId
                            + ", companyId=" + event.getCompanyId()
                            + ", permission=" + Permission.CONFIGURE_HALL_AND_MAP,
                    LogLevel.DEBUG
            );

            if (mapDTO == null) {
                throw new IllegalArgumentException("Map data cannot be null");
            }

            logger.logEvent(
                    "Map DTO received - defineEventMap. " + mapDTOLogContext(mapDTO),
                    LogLevel.DEBUG
            );

            EventMap map = EventMapper.toDomain(mapDTO);
            event.defineMap(map);
            eventRepository.updateEvent(event);
            logger.logEvent("Completed - defineEventMap. " + context, LogLevel.INFO);
            return true;
        } catch (IllegalArgumentException e) {
            logger.logEvent("Failed - defineEventMap. " + context + ". Error: " + e.getMessage(), LogLevel.WARN);
            throw e;

        } catch (Exception e) {
            logger.logError(
                    "Failed - defineEventMap. " + context
                            + ", mapSnapshot={" + mapDTOLogContext(mapDTO) + "}"
                            + ". Unexpected error: " + e.getMessage(),
                    e
            );
            throw e;
        }
    }

    public Boolean UpdateActiveEvantMap(String sessionId, Long eventId, List<IAreaDTO> newAreasDTO, List<IAreaDTO> updatedAreasDTO) {
        String context = "eventId=" + eventId + ", newAreasCount=" + (newAreasDTO != null ? newAreasDTO.size() : 0) + ", updatedAreasCount=" + (updatedAreasDTO != null ? updatedAreasDTO.size() : 0);
        logger.logEvent("Started - UpdateEventMap. " + context, LogLevel.INFO);
        try {
            if (!tokenService.validateToken(sessionId)) {
                throw new IllegalArgumentException("Invalid session ID");
            }
            logger.logEvent("Authenticated actor - UpdateEventMap. " + context, LogLevel.DEBUG);

            Event event = eventRepository.getEventById(eventId);
            if (event == null) {
                throw new IllegalArgumentException("Event not found");
            }
            logger.logEvent("Found event - UpdateEventMap. " + context, LogLevel.DEBUG);
            Long userId = tokenService.extractUserId(sessionId);
            userAccessService.validateCanPerformNonViewAction(userId);
            if (!membershipDomain.validatePermission(userId, event.getCompanyId(), Permission.CONFIGURE_HALL_AND_MAP)) {
                throw new IllegalArgumentException("User does not have permission to update event map");
            }
            if (newAreasDTO == null) {
                throw new IllegalArgumentException("New areas list cannot be null");
            }
            if (updatedAreasDTO == null) {
                throw new IllegalArgumentException("Updated areas list cannot be null");
            }

            logger.logEvent("Validated permission - UpdateEventMap. userId=" + userId + ", eventId=" + eventId + ", companyId=" + event.getCompanyId() + ", permission=" + Permission.CONFIGURE_HALL_AND_MAP, LogLevel.DEBUG);
            List<Area> newAreas = new ArrayList<>();
            for (IAreaDTO areaDTO : newAreasDTO) {
                Area area = EventMapper.toNewArea(areaDTO);
                newAreas.add(area);
            }
            Map<Long, Area> updatedAreas = new HashMap<>();

            for (IAreaDTO dto : updatedAreasDTO) {
                if (dto == null || dto.id() == null) {
                    throw new IllegalArgumentException(
                            "An updated area must have an ID"
                    );
                }

                Area previous = updatedAreas.put(
                        dto.id(),
                        EventMapper.toAreaUpdate(dto)
                );

                if (previous != null) {
                    throw new IllegalArgumentException(
                            "Duplicate updated area ID: " + dto.id()
                    );
                }
            }

            event.updateActiveMap(newAreas, updatedAreas);
            eventRepository.updateEvent(event);
            return true;
        }
        catch (IllegalArgumentException | IllegalStateException e) {
            logger.logEvent("Failed - updateActiveEventMap. " + context + ". Error: " + e.getMessage(), LogLevel.WARN);
            throw e;
        } catch (Exception e) {
            logger.logError("Failed - updateActiveEventMap. " + context + ". Unexpected error: " + e.getMessage(), e);
            throw e;
        }
    }



    public Boolean updateEventSaleStatus(String sessionId, Long eventId, SaleStatus targetStatus) {
        String context = "eventId=" + eventId + ", targetSaleStatus=" + targetStatus;
        logger.logEvent("Started - updateEventSaleStatus. " + context, LogLevel.INFO);

        try {
            if (!tokenService.validateToken(sessionId)) {
                throw new IllegalArgumentException("Invalid session ID");
            }

            if (eventId == null) {
                throw new IllegalArgumentException("Event ID cannot be null");
            }

            if (targetStatus == null) {
                throw new IllegalArgumentException("Sale status cannot be null");
            }

            Event event = eventRepository.getEventById(eventId);
            if (event == null) {
                throw new IllegalArgumentException("Event not found");
            }

            Long userId = tokenService.extractUserId(sessionId);
            userAccessService.validateCanPerformNonViewAction(userId);

            if (!membershipDomain.validatePermission(userId, event.getCompanyId(), Permission.MANAGE_EVENT_INVENTORY)) {
                throw new IllegalArgumentException("User does not have permission to update event sale status");
            }

            validateSaleStatusTransition(event.getSaleStatus(), targetStatus);

            event.setSaleStatus(targetStatus);
            eventRepository.updateEvent(event);

            logger.logEvent("Completed - updateEventSaleStatus. " + context, LogLevel.INFO);
            return true;

        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.logEvent("Failed - updateEventSaleStatus. " + context + ". Error: " + e.getMessage(), LogLevel.WARN);
            throw e;
        } catch (Exception e) {
            logger.logError("Failed - updateEventSaleStatus. " + context + ". Unexpected error: " + e.getMessage(), e);
            throw e;
        }
    }

    private void validateSaleStatusTransition(SaleStatus currentStatus, SaleStatus targetStatus) {
        SaleStatus current = currentStatus == null ? SaleStatus.NOT_STARTED : currentStatus;

        if (current == targetStatus) {
            return;
        }

        if (targetStatus == SaleStatus.PRE_SALE) {
            if (current != SaleStatus.NOT_STARTED) {
                throw new IllegalStateException("Cannot move to pre-sale from current sale status");
            }
            return;
        }

        if (targetStatus == SaleStatus.ONGOING) {
            if (current != SaleStatus.NOT_STARTED && current != SaleStatus.PRE_SALE) {
                throw new IllegalStateException("Cannot open regular sale from current sale status");
            }
            return;
        }

        if (targetStatus == SaleStatus.NOT_STARTED) {
            if (current != SaleStatus.NOT_STARTED) {
                throw new IllegalStateException("Cannot return sale status to not started");
            }
            return;
        }

        if (targetStatus == SaleStatus.SOLD_OUT || targetStatus == SaleStatus.ENDED) {
            throw new IllegalStateException("Sale status should move to sold out or ended only by the relevant business flow");
        }
    }

    public Boolean deleteEvent(String sessionId, Long eventId) {
        String context = "eventId=" + eventId;
        logger.logEvent("Started - deleteEvent. " + context, LogLevel.INFO);

        try {
            // precondition: user logged in
            if (!tokenService.validateToken(sessionId)) {
                throw new IllegalArgumentException("Invalid session ID");
            }
            logger.logEvent("Authenticated actor - deleteEvent. " + context, LogLevel.DEBUG);
            Event event = eventRepository.getEventById(eventId);
            if (event == null) {
                throw new IllegalArgumentException("Event does not exist");
            }
            logger.logEvent("Found event - deleteEvent. " + context, LogLevel.DEBUG);
            Long companyId = event.getCompanyId();
            // precondition: user has permission to remove an event
            Long userId = tokenService.extractUserId(sessionId);
            userAccessService.validateCanPerformNonViewAction(userId);
            if (!membershipDomain.validatePermission(userId, companyId, Permission.MANAGE_EVENT_INVENTORY)) {
                throw new IllegalArgumentException("User does not have permission to remove an event");
            }
            logger.logEvent("Validated permission - deleteEvent. " + context, LogLevel.DEBUG);
            eventStatus status = event.getStatus();
            if (status == eventStatus.ACTIVE || status == eventStatus.DRAFT) {
                throw new IllegalArgumentException("Only inactive or cancelled events can be removed");
            }
            long expectedVersion = eventRepository.getEventById(eventId).getVersion();
            // main scenario: remove event
            eventRepository.deleteEvent(eventId, expectedVersion);
            logger.logEvent("Completed - deleteEvent. " + context, LogLevel.INFO);
            return true;
        } catch (IllegalArgumentException e) {
            logger.logEvent("Failed - deleteEvent. " + context + ". Error: " + e.getMessage(), LogLevel.WARN);
            throw e;
        } catch (Exception e) {
            logger.logError("Failed - deleteEvent. " + context + ". Unexpected error: " + e.getMessage(), e);
            throw e;
        }
    }

    public EventDTO getEvent(String sessionId, Long eventId) {
        String context = "eventId=" + eventId;
        logger.logEvent("Started - getEvent. " + context, LogLevel.INFO);
        try {
            // precondition: user logged in
            if (!tokenService.validateToken(sessionId)) {
                throw new IllegalArgumentException("Invalid session ID");
            }
            logger.logEvent("Authenticated actor - getEvent. " + context, LogLevel.DEBUG);
            // precondition: user has permission to view event details
            Event event = eventRepository.getEventById(eventId);
            if (event == null) {
                throw new IllegalArgumentException("Event not found");
            }
            logger.logEvent("Found event - getEvent. " + context, LogLevel.DEBUG);

            // main scenario: return event details
            logger.logEvent("Completed - getEvent. " + event.toString(), LogLevel.DEBUG);
            return EventDTO.from(event);
        } catch (IllegalArgumentException e) {
            logger.logEvent("Failed - getEvent. " + context + ". Error: " + e.getMessage(), LogLevel.WARN);
            throw e;
        } catch (Exception e) {
            logger.logError("Failed - getEvent. " + context + ". Unexpected error: " + e.getMessage(), e);
            throw e;
        }
    }

    public EventMapDTO getEventMap(String sessionId, Long eventId) {
        String context = "eventId=" + eventId;
        logger.logEvent("Use-case started: getEventMap. " + context, LogLevel.INFO);
        try {
            // precondition: user logged in
            if (!tokenService.validateToken(sessionId)) {
                throw new IllegalArgumentException("Invalid session ID");
            }
            logger.logEvent("Authenticated actor - getEventMap. " + context, LogLevel.DEBUG);
            // precondition: user has permission to view event map
            Event event = eventRepository.getEventById(eventId);
            if (event == null) {
                throw new IllegalArgumentException("Event not found");
            }
            logger.logEvent("Found event - getEventMap. " + context, LogLevel.DEBUG);

            // main scenario: return map
            EventMap map = event.getMap();
            logger.logEvent("Completed - getEventMap. " + context, LogLevel.DEBUG);
            return EventMapDTO.from(map);
        } catch (IllegalArgumentException e) {
            logger.logEvent("Failed - getEventMap. " + context + ". Error: " + e.getMessage(), LogLevel.WARN);
            throw e;
        } catch (Exception e) {
            logger.logError("Failed - getEventMap. " + context + ". Unexpected error: " + e.getMessage(), e);
            throw e;
        }
    }

    public Boolean cancelEvent(String sessionId, Long eventId) {
        String context = "eventId=" + eventId;
        logger.logEvent("Started - cancelEvent. " + context, LogLevel.INFO);
        try {
            // precondition: user logged in
            if (!tokenService.validateToken(sessionId)) {
                throw new IllegalArgumentException("Invalid session ID");
            }
            logger.logEvent("Authenticated actor - cancelEvent. " + context, LogLevel.DEBUG);
            Event event = eventRepository.getEventById(eventId);
            if (event == null) {
                throw new IllegalArgumentException("Event does not exist");
            }
            Long companyId = event.getCompanyId();
            // precondition: user has permission to cancel an event
            Long userId = tokenService.extractUserId(sessionId);
            userAccessService.validateCanPerformNonViewAction(userId);
            if (!membershipDomain.validatePermission(userId, companyId, Permission.MANAGE_EVENT_INVENTORY)) {
                throw new IllegalArgumentException("User does not have permission to cancel an event");
            }
            logger.logEvent("Validated permission - cancelEvent. " + context, LogLevel.DEBUG);
            if (event.getStatus() == eventStatus.CANCELLED) {
                throw new IllegalStateException("Event is already canceled");
            }
         
            event.markCancellationPending();
            eventRepository.updateEvent(event);

            boolean success = notifyEventCancellationRequestedListeners(eventId);
            event = eventRepository.getEventById(eventId);
            if (success) {
                event.cancel();
                eventRepository.updateEvent(event);
                notifyEventCanceledListeners(eventId);
                return true;
            } else {
                event.markCancellationFailed();
                eventRepository.updateEvent(event);
                throw new IllegalArgumentException("Event cancellation failed. Please try again later to complete the cancellation process.");
            }

        } catch (IllegalArgumentException e) {
            logger.logEvent("Failed - cancelEvent. " + context + ". Error: " + e.getMessage(), LogLevel.WARN);
            throw e;
        } catch (Exception e) {
            logger.logError("Failed - cancelEvent. " + context + ". Unexpected error: " + e.getMessage(), e);
            throw e;
        }
    }

    public void addEventUpdatesListener(EventUpdatesListener listener) {
        eventUpdatesListeners.add(listener);
        logger.logEvent("Added event updates listener. Listener class: " + listener.getClass().getName(), LogLevel.DEBUG);
    }
    public int getEventCapacity(String sessionId, Long eventId) {
    String context = "eventId=" + eventId;
    logger.logEvent("Started - getEventCapacity. " + context, LogLevel.INFO);

    try {
        if (!tokenService.validateToken(sessionId)) {
            throw new IllegalArgumentException("Invalid session ID");
        }

        Event event = eventRepository.getEventById(eventId);
        if (event == null) {
            throw new IllegalArgumentException("Event not found");
        }

        int capacity = 0;

        for (IMapElementDTO element : EventMapDTO.from(event.getMap()).elements()) {
            if (element instanceof SeatingAreaDTO seatingArea) {
                capacity += seatingArea.seats().size();
            }

            if (element instanceof StandingAreaDTO standingArea) {
                capacity += (int) standingArea.capacity();
            }
        }

        logger.logEvent(
                "Completed - getEventCapacity. eventId=" + eventId + ", capacity=" + capacity,
                LogLevel.DEBUG
        );

        return capacity;

    } catch (IllegalArgumentException e) {
        logger.logEvent(
                "Failed - getEventCapacity. " + context + ". Error: " + e.getMessage(),
                LogLevel.WARN
        );
        throw e;
    } catch (Exception e) {
        logger.logError(
                "Failed - getEventCapacity. " + context + ". Unexpected error: " + e.getMessage(),
                e
        );
        throw e;
    }
}

public int getSoldTicketsCount(String sessionId, Long eventId) {
    String context = "eventId=" + eventId;
    logger.logEvent("Started - getSoldTicketsCount. " + context, LogLevel.INFO);

    try {
        if (!tokenService.validateToken(sessionId)) {
            throw new IllegalArgumentException("Invalid session ID");
        }

        Event event = eventRepository.getEventById(eventId);
        if (event == null) {
            throw new IllegalArgumentException("Event not found");
        }

       int sold=event.getSoldTicketsCount();

        logger.logEvent(
                "Completed - getSoldTicketsCount. eventId=" + eventId + ", sold=" + sold,
                LogLevel.DEBUG
        );

        return sold;

    } catch (IllegalArgumentException e) {
        logger.logEvent(
                "Failed - getSoldTicketsCount. " + context + ". Error: " + e.getMessage(),
                LogLevel.WARN
        );
        throw e;
    } catch (Exception e) {
        logger.logError(
                "Failed - getSoldTicketsCount. " + context + ". Unexpected error: " + e.getMessage(),
                e
        );
        throw e;
    }
}

    private void notifyEventCanceledListeners(Long eventId) {
        for (EventUpdatesListener listener : eventUpdatesListeners) {
            listener.onEventCanceled(eventId);
            logger.logEvent("Notified event canceled listener. Listener class: " + listener.getClass().getName(), LogLevel.DEBUG);
        }
    }

    private void notifyEventUpdatedListeners(Long eventId, LocalDateTime date, String Location, String updateMessage) {
        for (EventUpdatesListener listener : eventUpdatesListeners) {
            listener.onEventUpdated(eventId, date, Location, updateMessage);
            logger.logEvent("Notified event updated listener. Listener class: " + listener.getClass().getName(), LogLevel.DEBUG);
        }
    }

    
    private boolean notifyEventCancellationRequestedListeners(Long eventId) {
            for (EventUpdatesListener listener : eventUpdatesListeners) {
                boolean success = listener.onEventCancellationRequested(eventId);

                logger.logEvent(
                        "Notified event cancellation requested listener. Listener class: "
                                + listener.getClass().getName()
                                + ", success=" + success,
                        LogLevel.DEBUG
                );

                if (!success) {
                    return false;
                }
            }

            return true;
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
            logger.logEvent("Validation failed - event name is null or empty", LogLevel.DEBUG);
            throw new IllegalArgumentException("Event name cannot be null or empty");
        }

        if (date == null || date.isBefore(LocalDateTime.now())) {
            logger.logEvent("Validation failed - event date is null or in the past", LogLevel.DEBUG);
            throw new IllegalArgumentException("Event date must be in the future");
        }

        if (location == null) {
            logger.logEvent("Validation failed - event location is null", LogLevel.DEBUG);
            throw new IllegalArgumentException("Event location cannot be null");
        }

        if (trafficThreshold == null || trafficThreshold <= 0) {
            logger.logEvent("Validation failed - traffic threshold is null or not positive", LogLevel.DEBUG);
            throw new IllegalArgumentException("Traffic threshold must be a positive number");
        }

        if (category == null) {
            logger.logEvent("Validation failed - event category is null", LogLevel.DEBUG);
            throw new IllegalArgumentException("Event category cannot be null");
        }

        if (artist == null || artist.isBlank()) {
            logger.logEvent("Validation failed - artist name is null or empty", LogLevel.DEBUG);
            throw new IllegalArgumentException("Artist name cannot be null or empty");
        }

        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            logger.logEvent("Validation failed - price is null or negative", LogLevel.DEBUG);
            throw new IllegalArgumentException("Price must be a non-negative number");
        }
        logger.logEvent("Validated event details successfully - validateEventDetails", LogLevel.DEBUG);
    }

    private String eventDTOContext(EventDTO eventDTO) {
        if (eventDTO == null) {
            return "eventDTO=null";
        }

        return "eventId=" + eventDTO.id()
                + ", companyId=" + eventDTO.companyId()
                + ", version=" + eventDTO.version();
    }

  public void setEventDiscountPolicy(String token,
                                   Long eventId,
                                   DiscountPolicyDTO policyDTO) throws Exception {
    try {
        Event event = canEditEventDiscount(token, eventId);

        DiscountPolicy policy = discountMapper.toDomain(policyDTO);
        event.setDiscountPolicy(policy);
        eventRepository.updateEvent(event);


    } catch (Exception e) {
        logger.logEvent(
                "Failed to set discount policy for event, id: " + eventId
                        + ", reason=" + e.getMessage(),
                ISystemLogger.LogLevel.WARN
        );
        throw e;
    }
}

public DiscountPolicyDTO getEventDiscountPolicy(String token, Long eventId) throws Exception {
    try {
        Event event = eventRepository.getEventById(eventId);
        if (event == null) {
            throw new IllegalArgumentException("Event not found");
        }
        tokenService.validateToken(token);
        Long memberId = tokenService.extractUserId(token);
        userAccessService.validateCanPerformNonViewAction(memberId);
        if (!membershipDomain.validatePermission(memberId, event.getCompanyId(), Permission.SET_DISCOUNT_POLICY)) {
            throw new IllegalArgumentException(
                    "User does not have permission to view event discount policy");
        }

        DiscountPolicy policy = event.getDiscountPolicy();
        return discountMapper.toDTO(policy);

    } catch (Exception e) {
        logger.logEvent(
                "Failed to get discount policy for event, id: " + eventId
                        + ", reason=" + e.getMessage(),
                ISystemLogger.LogLevel.WARN
        );
        throw e;
    }
}

    public void setEventPurchasePolicy(String token, Long eventId, PurchasePolicyDTO policyDTO) throws Exception {
        try {
            Event event = canEditPurchasePolicy(token, eventId);

            PurchasePolicy policy = mapper.toDomain(policyDTO);


            event.setPurchasePolicy(policy);

            eventRepository.updateEvent(event);

        } catch (Exception e) {
            logger.logEvent(
                    "Failed to set purchase policy for event, id: " + eventId,
                    ISystemLogger.LogLevel.WARN
            );
            throw e;
        }
    }

    private Event canEditPurchasePolicy(String token, Long eventId) throws Exception {
        tokenService.validateToken(token);

        Long memberId = tokenService.extractUserId(token);
        userAccessService.validateCanPerformNonViewAction(memberId);
        Event event = eventRepository.getEventById(eventId);
        if (event == null) {
            throw new IllegalArgumentException("Event not found");
        }

        if (!membershipDomain.validatePermission(memberId, event.getCompanyId(), Permission.SET_PURCHASING_POLICY)) {
            throw new IllegalArgumentException(
                    "User does not have permission to manage event purchasing policy");
        }
        return event;
    }


    // set event discount composition type
    public void setEventDiscountCompositionType(String token, Long eventId,
                                                DiscountCompositionType compositionType) throws Exception {

        try {
            Event event = canEditEventDiscount(token, eventId);

            event.setDiscountCompositionType(compositionType);

            eventRepository.updateEvent(event);

            logger.logEvent(
                    "Discount composition type updated successfully for event id: "
                            + eventId,
                    ISystemLogger.LogLevel.INFO
            );

        } catch (Exception e) {
            logger.logEvent("Failed to set composition type discount to event",
                    ISystemLogger.LogLevel.WARN);
            throw e;
        }
    }

    private Event canEditEventDiscount(String token, Long eventId) throws Exception {
        tokenService.validateToken(token);

        Long memberId = tokenService.extractUserId(token);
        userAccessService.validateCanPerformNonViewAction(memberId);
        Event event = eventRepository.getEventById(eventId);

        if (event == null) {
            throw new Exception("Error: Event not found.");
        }

        Long companyId = event.getCompanyId();

        if (!membershipDomain.validatePermission(
                memberId,
                companyId,
                Permission.SET_DISCOUNT_POLICY)) {

            throw new IllegalArgumentException(
                    "User does not have permission to manage event discount policy");
        }

        return event;
    }

    public Boolean rollbackCreatedEvent(String sessionId, Long eventId) {
        String context = "eventId=" + eventId;

        logger.logEvent("Started - rollbackCreatedEvent. " + context, LogLevel.WARN);

        try {
            if (!tokenService.validateToken(sessionId)) {
                throw new IllegalArgumentException("Invalid session ID");
            }

            if (eventId == null) {
                throw new IllegalArgumentException("Event ID cannot be null");
            }

            Event event = eventRepository.getEventById(eventId);

            if (event == null) {
                logger.logEvent(
                        "Rollback skipped - event already does not exist. " + context,
                        LogLevel.WARN
                );
                return true;
            }

            Long userId = tokenService.extractUserId(sessionId);
            userAccessService.validateCanPerformNonViewAction(userId);

            if (!membershipDomain.validatePermission(
                    userId,
                    event.getCompanyId(),
                    Permission.MANAGE_EVENT_INVENTORY
            )) {
                throw new IllegalArgumentException("User does not have permission to rollback event creation");
            }

            /*
             * Safety check:
             * rollback is allowed only for a fresh event creation failure.
             * Do not use this method as a regular delete action.
             */
            if (event.getStatus() != eventStatus.DRAFT) {
                throw new IllegalStateException("Only draft events can be rolled back after creation failure");
            }

            long expectedVersion = event.getVersion();
            eventRepository.deleteEvent(eventId, expectedVersion);

            logger.logEvent(
                    "Completed - rollbackCreatedEvent. eventId=" + eventId,
                    LogLevel.WARN
            );
            return true;

        } catch (Exception e) {
            logger.logError(
                    "Failed - rollbackCreatedEvent. " + context + ". Error: " + e.getMessage(),
                    e
            );
            throw e;
        }
    }

    /**
     * Retrieves all events associated with a specific company.
     * Used for company management dashboards.
     */
    public List<EventDTO> getEventsByCompany(String token, Long companyId) throws Exception {
        try {
            // 1. Authenticate user
            tokenService.validateToken(token);
            Long memberId = tokenService.extractUserId(token);
            userAccessService.validateCanPerformNonViewAction(memberId);

            // 2. Fetch events from the database
            List<Event> events = eventRepository.getEventsByCompanyId(companyId);

            // 3. Map to DTO and return (Using EventDTO::from instead of EventDTO::new)
            return events.stream()
                    .map(EventDTO::from)
                    .collect(java.util.stream.Collectors.toList());

        } catch (Exception e) {
            logger.logError("Failed to fetch events for companyId=" + companyId, e);
            throw e;
        }
    }

    private String mapDTOLogContext(EventMapDTO mapDTO) {
        if (mapDTO == null) {
            return "mapDTO=null";
        }

        PairDTO<Integer, Integer> size = mapDTO.size();
        List<IMapElementDTO> elements = mapDTO.getElementDTOs();

        int elementCount = elements == null ? 0 : elements.size();
        int seatingAreas = 0;
        int standingAreas = 0;
        int regularElements = 0;
        int totalSeats = 0;
        long totalStandingCapacity = 0;

        StringBuilder elementsText = new StringBuilder();

        if (elements != null) {
            for (int i = 0; i < elements.size(); i++) {
                IMapElementDTO element = elements.get(i);

                if (element instanceof SeatingAreaDTO seatingArea) {
                    seatingAreas++;
                    totalSeats += safeMultiply(seatingArea.rows(), seatingArea.columns());
                } else if (element instanceof StandingAreaDTO standingArea) {
                    standingAreas++;
                    totalStandingCapacity += standingArea.capacity();
                } else if (element instanceof ElementDTO) {
                    regularElements++;
                }

                if (i > 0) {
                    elementsText.append(" | ");
                }

                elementsText.append("#")
                        .append(i + 1)
                        .append(" ")
                        .append(mapElementDTOLogContext(element));
            }
        }

        return "mapSize=" + pairToText(size)
                + ", width=" + pairFirst(size)
                + ", height=" + pairSecond(size)
                + ", elementCount=" + elementCount
                + ", seatingAreas=" + seatingAreas
                + ", standingAreas=" + standingAreas
                + ", regularElements=" + regularElements
                + ", totalSeats=" + totalSeats
                + ", totalStandingCapacity=" + totalStandingCapacity
                + ", elements=[" + elementsText + "]";
    }

    private String mapElementDTOLogContext(IMapElementDTO element) {
        if (element == null) {
            return "element=null";
        }

        if (element instanceof SeatingAreaDTO seatingArea) {
            return "SeatingArea{"
                    + "id=" + seatingArea.id()
                    + ", name=" + seatingArea.name()
                    + ", type=" + seatingArea.type()
                    + ", location=" + pairToText(seatingArea.location())
                    + ", size=" + pairToText(seatingArea.size())
                    + ", rows=" + seatingArea.rows()
                    + ", columns=" + seatingArea.columns()
                    + ", seats=" + (seatingArea.seats() == null ? 0 : seatingArea.seats().size())
                    + ", soldOut=" + seatingArea.soldOut()
                    + "}";
        }

        if (element instanceof StandingAreaDTO standingArea) {
            return "StandingArea{"
                    + "id=" + standingArea.id()
                    + ", name=" + standingArea.name()
                    + ", type=" + standingArea.type()
                    + ", location=" + pairToText(standingArea.location())
                    + ", size=" + pairToText(standingArea.size())
                    + ", capacity=" + standingArea.capacity()
                    + ", reserved=" + standingArea.reserved()
                    + ", sold=" + standingArea.sold()
                    + ", soldOut=" + standingArea.soldOut()
                    + "}";
        }

        if (element instanceof ElementDTO regularElement) {
            return "Element{"
                    + "id=" + regularElement.id()
                    + ", name=" + regularElement.name()
                    + ", type=" + regularElement.type()
                    + ", location=" + pairToText(regularElement.location())
                    + ", size=" + pairToText(regularElement.size())
                    + "}";
        }

        return "UnsupportedElement{class=" + element.getClass().getSimpleName() + "}";
    }

    private String pairToText(PairDTO<Integer, Integer> pair) {
        if (pair == null) {
            return "null";
        }

        return "(" + pair.first() + "," + pair.second() + ")";
    }

    private Integer pairFirst(PairDTO<Integer, Integer> pair) {
        return pair == null ? null : pair.first();
    }

    private Integer pairSecond(PairDTO<Integer, Integer> pair) {
        return pair == null ? null : pair.second();
    }

    private int safeMultiply(Integer first, Integer second) {
        if (first == null || second == null) {
            return 0;
        }

        return first * second;
    }

}
