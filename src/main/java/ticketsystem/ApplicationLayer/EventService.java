package ticketsystem.ApplicationLayer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import ticketsystem.ApplicationLayer.Events.EventUpdatesListener;
import ticketsystem.ApplicationLayer.ISystemLogger.LogLevel;
import ticketsystem.DTO.Event.ElementDTO;
import ticketsystem.DTO.Event.EventDTO;
import ticketsystem.DTO.Event.EventMapDTO;
import ticketsystem.DTO.Event.IMapElementDTO;
import ticketsystem.DTO.Event.PairDTO;
import ticketsystem.DTO.Event.SeatingAreaDTO;
import ticketsystem.DTO.Event.StandingAreaDTO;
import ticketsystem.DTO.PurchasePolicyDTO;
import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.MembershipDomainService;
import ticketsystem.DomainLayer.discount.ConditionalDiscount.Condition;
import ticketsystem.DomainLayer.discount.DiscountCompositionType;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.event.Event.eventStatus;
import ticketsystem.DomainLayer.event.EventCategory;
import ticketsystem.DomainLayer.event.EventLocation;
import ticketsystem.DomainLayer.event.EventMap;
import ticketsystem.DomainLayer.event.Pair;
import ticketsystem.DomainLayer.event.SaleStatus;
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
            Long eventId = eventRepository.getNextId();

            Event event = new Event(eventId, date, eventName, companyId, userId, location, trafficThreshold, category,
                    artist, price, new Pair<>(mapHigh, mapWidth));
            eventRepository.addEvent(event);
            logger.logEvent("Completed - insertEvent. eventId=" + eventId + ", companyId=" + companyId + ", " + event.toString(), LogLevel.INFO);
            return eventId;
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
            Event existingEvent = eventRepository.getEventById(eventDTO.id());
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
                ticketPrice = existingEvent.getTicketPrice();
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

            validateMapHasAtLeastOneTicketArea(mapDTO);
            validateMapElementsInsideMapBounds(mapDTO);

            logger.logEvent(
                    "Map DTO validated - defineEventMap. " + mapDTOLogContext(mapDTO),
                    LogLevel.DEBUG
            );

            EventMap map = EventMapper.toDomain(mapDTO);
            event.setMap(map);
            event.setStatus(eventStatus.ACTIVE);
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
            event.cancel();
            eventRepository.updateEvent(event); // update event status to cancelled
            notifyEventCanceledListeners(eventId);
            logger.logEvent("Completed - cancelEvent. " + context, LogLevel.INFO);
            return true;
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

    private void validateMapHasAtLeastOneTicketArea(EventMapDTO mapDTO) {
        boolean hasTicketArea = mapDTO.getElementDTOs() != null
                && mapDTO.getElementDTOs()
                .stream()
                .anyMatch(this::isTicketArea);
        logger.logEvent("Validating map contains at least one ticket area - validateMapHasAtLeastOneTicketArea. hasTicketArea=" + hasTicketArea, LogLevel.DEBUG);

        if (!hasTicketArea) {
            logger.logEvent("Validation failed - event map does not contain any seating or standing area", LogLevel.DEBUG);
            throw new IllegalArgumentException(
                    "Event map must contain at least one seating area or standing area"
            );
        }
    }

    private boolean isTicketArea(IMapElementDTO element) {
        return element instanceof SeatingAreaDTO
                || element instanceof StandingAreaDTO;
    }

    private void validateMapElementsInsideMapBounds(EventMapDTO mapDTO) {
        if (mapDTO.size() == null
                || mapDTO.size().first() == null
                || mapDTO.size().second() == null) {
            logger.logEvent("Validation failed - map size is null", LogLevel.DEBUG);
            throw new IllegalArgumentException("Map size cannot be null");
        }

        int mapHeight = mapDTO.size().first();
        int mapWidth = mapDTO.size().second();

        if (mapWidth <= 0 || mapHeight <= 0) {
            logger.logEvent("Validation failed - map size is not positive", LogLevel.DEBUG);
            throw new IllegalArgumentException("Map size must be positive");
        }

        if (mapDTO.getElementDTOs() == null) {
            logger.logEvent("Validation failed - map elements list is null", LogLevel.DEBUG);
            throw new IllegalArgumentException("Map elements cannot be null");
        }

        for (IMapElementDTO element : mapDTO.getElementDTOs()) {
            validateSingleElementInsideMapBounds(element, mapWidth, mapHeight);
        }
    }

    private void validateSingleElementInsideMapBounds(IMapElementDTO element, int mapWidth, int mapHeight) {
        if (element == null) {
            logger.logEvent("Validation failed - map element is null", LogLevel.DEBUG);
            throw new IllegalArgumentException("Map elements cannot contain null");
        }

        PairDTO<Integer, Integer> location = getElementLocation(element);
        PairDTO<Integer, Integer> size = getElementSize(element);

        String elementName = getElementName(element);

        if (location == null || size == null) {
            logger.logEvent("Validation failed - element location or size is null: " + elementName, LogLevel.DEBUG);
            throw new IllegalArgumentException("Element location and size cannot be null: " + elementName);
        }

        if (location.first() == null || location.second() == null
                || size.first() == null || size.second() == null) {
            logger.logEvent("Validation failed - element location or size values are null: " + elementName, LogLevel.DEBUG);
            throw new IllegalArgumentException("Element location and size values cannot be null: " + elementName);
        }

        int x = location.first();
        int y = location.second();
        int width = size.first();
        int height = size.second();

        if (x < 0 || y < 0) {
            logger.logEvent("Validation failed - element location cannot be negative: " + elementName, LogLevel.DEBUG);
            throw new IllegalArgumentException("Element location cannot be negative: " + elementName);
        }

        if (width <= 0 || height <= 0) {
            logger.logEvent("Validation failed - element size must be positive: " + elementName, LogLevel.DEBUG);
            throw new IllegalArgumentException("Element size must be positive: " + elementName);
        }

        if (x + width > mapWidth || y + height > mapHeight) {
            logger.logEvent("Validation failed - element is outside map bounds: " + elementName, LogLevel.DEBUG);
            throw new IllegalArgumentException("Element is outside map bounds: " + elementName);
        }
    }

    private PairDTO<Integer, Integer> getElementLocation(IMapElementDTO element) {
        if (element instanceof SeatingAreaDTO seatingArea) {
            return seatingArea.location();
        }

        if (element instanceof StandingAreaDTO standingArea) {
            return standingArea.location();
        }

        if (element instanceof ElementDTO regularElement) {
            return regularElement.location();
        }
        logger.logEvent("Validation failed - unsupported map element type: " + element.getClass().getSimpleName(), LogLevel.DEBUG);
        throw new IllegalArgumentException("Unsupported map element type: " + element.getClass().getSimpleName());
    }

    private PairDTO<Integer, Integer> getElementSize(IMapElementDTO element) {
        if (element instanceof SeatingAreaDTO seatingArea) {
            return seatingArea.size();
        }

        if (element instanceof StandingAreaDTO standingArea) {
            return standingArea.size();
        }

        if (element instanceof ElementDTO regularElement) {
            return regularElement.size();
        }
        logger.logEvent("Validation failed - unsupported map element type: " + element.getClass().getSimpleName(), LogLevel.DEBUG);
        throw new IllegalArgumentException("Unsupported map element type: " + element.getClass().getSimpleName());
    }

    private String getElementName(IMapElementDTO element) {
        if (element instanceof SeatingAreaDTO seatingArea) {
            return seatingArea.name();
        }

        if (element instanceof StandingAreaDTO standingArea) {
            return standingArea.name();
        }

        if (element instanceof ElementDTO regularElement) {
            return regularElement.name();
        }

        return element.getClass().getSimpleName();
    }

    private String eventDTOContext(EventDTO eventDTO) {
        if (eventDTO == null) {
            return "eventDTO=null";
        }

        return "eventId=" + eventDTO.id()
                + ", companyId=" + eventDTO.companyId()
                + ", version=" + eventDTO.version();
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

    // add visible discount to event
    public void addVisibleDiscountToEvent(String token, Long eventId,
                                          String name, BigDecimal percentage) throws Exception {

        try {
            Event event = canEditEventDiscount(token, eventId);

            event.addVisibleDiscountToEvent(name, percentage);

            eventRepository.updateEvent(event);

            logger.logEvent(
                    "Visible discount added successfully to event id: " + eventId,
                    ISystemLogger.LogLevel.INFO
            );

        } catch (Exception e) {
            logger.logEvent("Failed to add visible discount to event",
                    ISystemLogger.LogLevel.WARN);
            throw e;
        }
    }

    // add coupon discount to event
    public void addCouponDiscountToEvent(String token, Long eventId,
                                         String name, String couponCode,
                                         BigDecimal percentage, LocalDateTime endTime) throws Exception {

        try {
            Event event = canEditEventDiscount(token, eventId);

            event.addCouponDiscountToEvent(name, couponCode, percentage, endTime);

            eventRepository.updateEvent(event);

            logger.logEvent(
                    "Coupon discount added successfully to event id: " + eventId,
                    ISystemLogger.LogLevel.INFO
            );

        } catch (Exception e) {
            logger.logEvent("Failed to add coupon discount to event",
                    ISystemLogger.LogLevel.WARN);
            throw e;
        }
    }

    // add conditional discount to event
    public void addConditionalDiscountToEvent(String token, Long eventId,
                                              String name, LocalDateTime startTime,
                                              LocalDateTime endTime, BigDecimal percentage,
                                              Condition condition,
                                              Integer ticketThreshold) throws Exception {

        try {
            Event event = canEditEventDiscount(token, eventId);

            event.addConditionalDiscountToEvent(
                    name,
                    startTime,
                    endTime,
                    percentage,
                    condition,
                    ticketThreshold
            );

            eventRepository.updateEvent(event);

            logger.logEvent(
                    "Conditional discount added successfully to event id: " + eventId,
                    ISystemLogger.LogLevel.INFO
            );

        } catch (Exception e) {
            logger.logEvent("Failed to add conditional discount to event",
                    ISystemLogger.LogLevel.WARN);
            throw e;
        }
    }

    // remove discount from event
    public void removeDiscountFromEvent(String token, Long eventId,
                                        Long discountId) throws Exception {

        try {
            Event event = canEditEventDiscount(token, eventId);

            event.removeDiscountFromEvent(discountId);

            eventRepository.updateEvent(event);

            logger.logEvent(
                    "Discount removed successfully from event id: "
                            + eventId + ", discount id: " + discountId,
                    ISystemLogger.LogLevel.INFO
            );

        } catch (Exception e) {
            logger.logEvent("Failed to remove discount from event, id: " + discountId,
                    ISystemLogger.LogLevel.WARN);
            throw e;
        }
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
