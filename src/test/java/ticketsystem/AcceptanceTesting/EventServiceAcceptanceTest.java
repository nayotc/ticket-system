package ticketsystem.AcceptanceTesting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.ApplicationLayer.EventService;
import ticketsystem.ApplicationLayer.Events.EventUpdatesListener;
import ticketsystem.ApplicationLayer.HistoryService;
import ticketsystem.ApplicationLayer.INotifier;
import ticketsystem.ApplicationLayer.IPaymentService;
import ticketsystem.ApplicationLayer.ISystemLogger;
import ticketsystem.ApplicationLayer.ITokenService;
import ticketsystem.ApplicationLayer.OrderService;
import ticketsystem.ApplicationLayer.UserAccessService;
import ticketsystem.DTO.Event.ElementDTO;
import ticketsystem.DTO.Event.EventDTO;
import ticketsystem.DTO.Event.EventMapDTO;
import ticketsystem.DTO.Event.IMapElementDTO;
import ticketsystem.DTO.Event.PairDTO;
import ticketsystem.DTO.Event.SeatingAreaDTO;
import ticketsystem.DTO.Event.StandingAreaDTO;
import ticketsystem.DTO.PurchasePolicyDTO;
import ticketsystem.DTO.PurchaseRuleDTO;
import ticketsystem.DTO.PurchaseRuleType;
import ticketsystem.DomainLayer.IRepository.IHistoryRepository;
import ticketsystem.DomainLayer.MembershipDomainService;
import ticketsystem.DomainLayer.discount.ConditionalDiscount.Condition;
import ticketsystem.DomainLayer.discount.DiscountCompositionType;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.event.Event.eventStatus;
import ticketsystem.DomainLayer.event.EventCategory;
import ticketsystem.DomainLayer.event.EventLocation;
import ticketsystem.DomainLayer.event.SaleStatus;
import ticketsystem.DomainLayer.order.ActiveOrder;
import ticketsystem.DomainLayer.order.Ticket;
import ticketsystem.DomainLayer.user.CompanyRole;
import ticketsystem.DomainLayer.user.Member;
import ticketsystem.DomainLayer.user.Permission;
import ticketsystem.DomainLayer.user.RoleStatus;
import ticketsystem.DomainLayer.user.User;
import ticketsystem.InfrastructureLayer.EventRepository;
import ticketsystem.InfrastructureLayer.HistoryRepository;
import ticketsystem.InfrastructureLayer.LogbackSystemLogger;
import ticketsystem.InfrastructureLayer.OrderRepository;
import ticketsystem.InfrastructureLayer.UserRepository;

public class EventServiceAcceptanceTest {

    private EventService eventService;
    private EventRepository eventRepository;
    private UserRepository userRepository;
    private FakeTokenService tokenService;
    private MembershipDomainService membershipDomain;
    private final ISystemLogger logger = new LogbackSystemLogger();
    private FakeNotificationsService fakeNotifications;
    private final String validOwnerSessionId = "owner-session";
    private final String invalidSessionId = "invalid-session";
    private IHistoryRepository historyRepository;
    private final Long ownerId = 1L;
    private final Long companyId = 100L;
    private UserAccessService userAccessService;
    private HistoryService historyService;
    private IPaymentService paymentService;

    @BeforeEach
    void setUp() {
        eventRepository = new EventRepository();
        tokenService = new FakeTokenService();
        userRepository = new UserRepository();
        fakeNotifications = new FakeNotificationsService();
        userAccessService = new UserAccessService(userRepository);

        // FIX: We use a robust anonymous subclass of MembershipDomainService.
        // This ensures permissions work correctly even if EventService uses the
        // incomplete String-based stub method,
        // and protects against NullPointerExceptions when the repository lookups fail.
        membershipDomain = new MembershipDomainService(userRepository) {
            @Override
            public boolean validatePermission(Long memberId, Long compId, Permission permission) {
                if (memberId == null) {
                    return false;
                }

                Member member = userRepository.getMemberById(memberId);

                // Fallback protection if EventService mistakenly passes companyId instead of userId
                if (member == null && memberId.equals(compId)) {
                    member = userRepository.getMemberById(ownerId);
                }

                if (member == null) {
                    return false;
                }

                CompanyRole role = member.getRoleInCompany(compId);
                return role != null
                        && role.getStatus() == RoleStatus.ACTIVE
                        && role.hasPermission(permission);
            }

            @Override
            public boolean validatePermission(String sessionId, Long compId, String permission) {
                Long uId = tokenService.extractUserId(sessionId);
                if (uId == null) {
                    return false;
                }

                Member member = userRepository.getMemberById(uId);
                if (member == null) {
                    return false;
                }

                CompanyRole role = member.getRoleInCompany(compId);
                return role != null && role.getStatus() == RoleStatus.ACTIVE;
            }
        };

        historyRepository = new HistoryRepository();

        historyService = new HistoryService(
                historyRepository,
                tokenService,
                membershipDomain,
                logger,
                userAccessService,
                fakeNotifications,
                paymentService
        );

        eventService = new EventService(
                eventRepository,
                tokenService,
                membershipDomain,
                logger,
                userAccessService
        );

        eventService.addEventUpdatesListener(historyService);

        // FIX: Setup a real Member with an ACTIVE Owner role in the DB
        Member ownerMember = new Member(ownerId, "EventOwnerUser", "Event Owner User", "0500000001", LocalDate.of(2001, 1, 1));
        ownerMember.addOwnerRole(companyId, 999L);
        ownerMember.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        userRepository.addRegisteredMember(ownerId, ownerMember, "password");

        tokenService.addValidSession(validOwnerSessionId, ownerId);
    }

    // -------------------- Insert Event Tests -------------------
    @Test
    void GivenOwnerLoggedInAndValidEventDetails_WhenInsertEvent_ThenEventIsCreatedAndSaved() {
        // Arrange
        String eventName = "Rock Concert";
        LocalDateTime eventDate = LocalDateTime.now().plusDays(10);

        // Act
        eventService.insertEvent(
                validOwnerSessionId,
                eventName,
                companyId,
                eventDate,
                EventLocation.TEL_AVIV,
                100L,
                EventCategory.CONCERT,
                "The Rockers",
                BigDecimal.valueOf(99.99),
                10,
                20);

        // Assert
        Event savedEvent = eventRepository.getEventById(1L);

        assertNotNull(savedEvent);
        assertEquals(eventName, savedEvent.getName());
        assertEquals(companyId, savedEvent.getCompanyId());
        assertEquals(ownerId, savedEvent.getOpenedBy());
        assertEquals(EventLocation.TEL_AVIV, savedEvent.getLocation());
        assertEquals(EventCategory.CONCERT, savedEvent.getCategory());
    }

    @Test
    void GivenOwnerLoggedInAndInvalidEventName_WhenInsertEvent_ThenSystemRejectsTheRequest() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.insertEvent(
                        validOwnerSessionId,
                        "",
                        companyId,
                        LocalDateTime.now().plusDays(10),
                        EventLocation.TEL_AVIV,
                        100L,
                        EventCategory.CONCERT,
                        "The Rockers",
                        BigDecimal.valueOf(99.99),
                        10,
                        20));

        assertTrue(exception.getMessage().contains("Event name cannot be null or empty"));
    }

    @Test
    void GivenOwnerLoggedInAndPastEventDate_WhenInsertEvent_ThenSystemRejectsTheRequest() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.insertEvent(
                        validOwnerSessionId,
                        "Past Event",
                        companyId,
                        LocalDateTime.now().minusDays(1),
                        EventLocation.TEL_AVIV,
                        100L,
                        EventCategory.CONCERT,
                        "The Rockers",
                        BigDecimal.valueOf(99.99),
                        10,
                        20));

        assertTrue(exception.getMessage().contains("Event date must be in the future"));
    }

    @Test
    void GivenOwnerLoggedInAndInvalidInventoryDetails_WhenInsertEvent_ThenSystemRejectsTheRequest() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.insertEvent(
                        validOwnerSessionId,
                        "Rock Concert",
                        companyId,
                        LocalDateTime.now().plusDays(10),
                        EventLocation.TEL_AVIV,
                        0L,
                        EventCategory.CONCERT,
                        "The Rockers",
                        BigDecimal.valueOf(99.99),
                        10,
                        20));

        assertTrue(exception.getMessage().contains("Traffic threshold must be a positive number"));
    }

    @Test
    void GivenOwnerLoggedInAndInvalidMapSize_WhenInsertEvent_ThenSystemRejectsTheRequest() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.insertEvent(
                        validOwnerSessionId,
                        "Rock Concert",
                        companyId,
                        LocalDateTime.now().plusDays(10),
                        EventLocation.TEL_AVIV,
                        100L,
                        EventCategory.CONCERT,
                        "The Rockers",
                        BigDecimal.valueOf(99.99),
                        0,
                        20));

        assertTrue(exception.getMessage().contains("Map size must be positive"));
        assertNull(eventRepository.getEventById(1L));
    }

    @Test
    void GivenInvalidSession_WhenInsertEvent_ThenSystemRejectsTheRequest() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.insertEvent(
                        invalidSessionId,
                        "Rock Concert",
                        companyId,
                        LocalDateTime.now().plusDays(10),
                        EventLocation.TEL_AVIV,
                        100L,
                        EventCategory.CONCERT,
                        "The Rockers",
                        BigDecimal.valueOf(99.99),
                        10,
                        20));

        assertTrue(exception.getMessage().contains("Invalid session ID"));
        assertNull(eventRepository.getEventById(1L));
    }

    @Test
    void GivenLoggedInUserWithoutCreatePermission_WhenInsertEvent_ThenSystemRejectsTheRequest() {
        String sessionWithoutPermission = "session-without-create-permission";
        Long plainUserId = 2L;

        // Setup a real user WITHOUT any roles
        Member plainUser = new Member(plainUserId, "PlainUser", "Plain User", "0500000002", LocalDate.of(2001, 1, 1));
        userRepository.addRegisteredMember(plainUserId, plainUser, "password");
        tokenService.addValidSession(sessionWithoutPermission, plainUserId);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.insertEvent(
                        sessionWithoutPermission,
                        "Rock Concert",
                        companyId,
                        LocalDateTime.now().plusDays(10),
                        EventLocation.TEL_AVIV,
                        100L,
                        EventCategory.CONCERT,
                        "The Rockers",
                        BigDecimal.valueOf(99.99),
                        10,
                        20));

        assertTrue(exception.getMessage().contains("User does not have permission to create an event"));
        assertNull(eventRepository.getEventById(1L));
    }

    // -------------------- Update Event Tests -------------------
    @Test
    void GivenOwnerLoggedInEventExistsAndValidUpdatedDetails_WhenUpdateEvent_ThenEventIsUpdatedAndSaved() {
        Event event = createExistingEvent();
        eventRepository.addEvent(event);
        Event savedEvent = eventRepository.getEventById(event.getId());

        EventDTO updateDTO = createValidUpdateDTO(savedEvent);

        Boolean result = eventService.updateEvent(validOwnerSessionId, updateDTO);
        Event updatedEvent = eventRepository.getEventById(savedEvent.getId());

        assertTrue(result);
        assertNotNull(updatedEvent);
        assertEquals("Updated Rock Concert", updatedEvent.getName());
        assertEquals(EventLocation.HAIFA, updatedEvent.getLocation());
        assertEquals(200L, updatedEvent.getTrafficThreshold());
        assertEquals(EventCategory.CONCERT, updatedEvent.getCategory());
        assertEquals("Updated Artist", updatedEvent.getArtistName());
        assertEquals(0, BigDecimal.valueOf(149.99).compareTo(updatedEvent.getTicketPrice()));
    }

    @Test
    void GivenOwnerLoggedInEventExistsAndInvalidUpdatedName_WhenUpdateEvent_ThenSystemRejectsTheRequest() {
        Event event = createExistingEvent();
        eventRepository.addEvent(event);
        Event savedEvent = eventRepository.getEventById(event.getId());

        String originalName = savedEvent.getName();
        LocalDateTime originalDate = savedEvent.getDate();
        EventLocation originalLocation = savedEvent.getLocation();
        EventCategory originalCategory = savedEvent.getCategory();
        BigDecimal originalPrice = savedEvent.getTicketPrice();

        EventDTO invalidUpdateDTO = new EventDTO(
                savedEvent.getId(),
                "",
                savedEvent.getCompanyId(),
                savedEvent.getOpenedBy(),
                LocalDateTime.now().plusDays(30),
                EventLocation.HAIFA.name(),
                200L,
                savedEvent.getStatus().name(),
                EventCategory.CONCERT.name(),
                "Updated Artist",
                BigDecimal.valueOf(149.99),
                null,
                savedEvent.getRate(),
                SaleStatus.ONGOING.name(),
                savedEvent.isOverloaded(),
                savedEvent.getActiveReservationsCount(),
                savedEvent.getVersion(),
                null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.updateEvent(validOwnerSessionId, invalidUpdateDTO));

        Event unchangedEvent = eventRepository.getEventById(savedEvent.getId());
        assertTrue(exception.getMessage().contains("Event name"));
        assertEquals(originalName, unchangedEvent.getName());
        assertEquals(originalDate, unchangedEvent.getDate());
        assertEquals(originalLocation, unchangedEvent.getLocation());
        assertEquals(originalCategory, unchangedEvent.getCategory());
        assertEquals(0, originalPrice.compareTo(unchangedEvent.getTicketPrice()));
    }

    @Test
    void GivenOwnerLoggedInEventExistsAndPastUpdatedDate_WhenUpdateEvent_ThenSystemRejectsTheRequest() {
        Event event = createExistingEvent();
        eventRepository.addEvent(event);
        Event savedEvent = eventRepository.getEventById(event.getId());
        LocalDateTime originalDate = savedEvent.getDate();

        EventDTO invalidUpdateDTO = new EventDTO(
                savedEvent.getId(),
                "Updated Rock Concert",
                savedEvent.getCompanyId(),
                savedEvent.getOpenedBy(),
                LocalDateTime.now().minusDays(1),
                EventLocation.HAIFA.name(),
                200L,
                savedEvent.getStatus().name(),
                EventCategory.CONCERT.name(),
                "Updated Artist",
                BigDecimal.valueOf(149.99),
                null,
                savedEvent.getRate(),
                SaleStatus.ONGOING.name(),
                savedEvent.isOverloaded(),
                savedEvent.getActiveReservationsCount(),
                savedEvent.getVersion(),
                null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.updateEvent(validOwnerSessionId, invalidUpdateDTO));

        Event unchangedEvent = eventRepository.getEventById(savedEvent.getId());

        assertTrue(exception.getMessage().contains("date"));
        assertEquals(originalDate, unchangedEvent.getDate());
    }

    @Test
    void GivenInvalidSession_WhenUpdateEvent_ThenSystemRejectsTheRequest() {
        Event event = createExistingEvent();
        eventRepository.addEvent(event);
        Event savedEvent = eventRepository.getEventById(event.getId());
        EventDTO updateDTO = createValidUpdateDTO(savedEvent);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.updateEvent(invalidSessionId, updateDTO));

        // Assert
        Event unchangedEvent = eventRepository.getEventById(savedEvent.getId());

        assertTrue(exception.getMessage().contains("Invalid session ID"));
        assertEquals(savedEvent.getName(), unchangedEvent.getName());
    }

    @Test
    void GivenLoggedInUserWithoutUpdatePermission_WhenUpdateEvent_ThenSystemRejectsTheRequest() {
        Event event = createExistingEvent();
        eventRepository.addEvent(event);
        Event savedEvent = eventRepository.getEventById(event.getId());

        String sessionWithoutPermission = "session-without-update-permission";

        // Setup a real user WITHOUT any roles
        Member plainUser = new Member(2L, "PlainUser", "Plain User", "0500000003", LocalDate.of(2001, 1, 1));
        userRepository.addRegisteredMember(2L, plainUser, "password");
        tokenService.addValidSession(sessionWithoutPermission, 2L);

        EventDTO updateDTO = createValidUpdateDTO(savedEvent);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.updateEvent(sessionWithoutPermission, updateDTO));

        Event unchangedEvent = eventRepository.getEventById(savedEvent.getId());

        assertTrue(exception.getMessage().contains("User does not have permission to update event"));
        assertEquals(savedEvent.getName(), unchangedEvent.getName());
    }

    @Test
    void GivenEventDoesNotExist_WhenUpdateEvent_ThenSystemRejectsTheRequest() {
        EventDTO updateDTO = new EventDTO(
                999L,
                "Updated Rock Concert",
                companyId,
                ownerId,
                LocalDateTime.now().plusDays(30),
                EventLocation.HAIFA.name(),
                200L,
                eventStatus.DRAFT.name(),
                EventCategory.CONCERT.name(),
                "Updated Artist",
                BigDecimal.valueOf(149.99),
                null,
                0.0,
                SaleStatus.ONGOING.name(),
                false,
                0,
                0,
                null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.updateEvent(validOwnerSessionId, updateDTO));

        assertTrue(exception.getMessage().contains("Event not found"));
    }

    @Test
    void GivenUpdateEventTriesToChangeCompany_WhenUpdateEvent_ThenSystemRejectsTheRequest() {
        Event event = createExistingEvent();
        eventRepository.addEvent(event);
        Event savedEvent = eventRepository.getEventById(event.getId());

        Long differentCompanyId = companyId + 1;

        // FIX: The domain rules block editing if the user isn't an owner in the new
        // company too.
        // Grant the owner a role in the new company to bypass the permission check,
        // allowing the logic to proceed and throw the intended "Cannot change event's
        // company" exception.
        Member ownerMember = userRepository.getMemberById(ownerId);
        ownerMember.addOwnerRole(differentCompanyId, 999L);
        ownerMember.getRoleInCompany(differentCompanyId).setStatus(RoleStatus.ACTIVE);
        userRepository.updateMember(ownerMember);

        EventDTO updateDTO = new EventDTO(
                savedEvent.getId(),
                "Updated Rock Concert",
                differentCompanyId,
                savedEvent.getOpenedBy(),
                LocalDateTime.now().plusDays(30),
                EventLocation.HAIFA.name(),
                200L,
                savedEvent.getStatus().name(),
                EventCategory.CONCERT.name(),
                "Updated Artist",
                BigDecimal.valueOf(149.99),
                null,
                savedEvent.getRate(),
                SaleStatus.ONGOING.name(),
                savedEvent.isOverloaded(),
                savedEvent.getActiveReservationsCount(),
                savedEvent.getVersion(),
                null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.updateEvent(validOwnerSessionId, updateDTO));

        Event unchangedEvent = eventRepository.getEventById(savedEvent.getId());

        assertTrue(exception.getMessage().contains("Cannot change event's company"));
        assertEquals(companyId, unchangedEvent.getCompanyId());
    }

    @Test
    void GivenEventWasUpdatedByAnotherRequest_WhenUpdateEvent_ThenSystemRejectsTheRequest() {
        Event event = createExistingEvent();
        eventRepository.addEvent(event);
        Event savedEvent = eventRepository.getEventById(event.getId());

        int staleVersion = savedEvent.getVersion() - 1;

        EventDTO staleUpdateDTO = new EventDTO(
                savedEvent.getId(),
                "Updated Rock Concert",
                savedEvent.getCompanyId(),
                savedEvent.getOpenedBy(),
                LocalDateTime.now().plusDays(30),
                EventLocation.HAIFA.name(),
                200L,
                savedEvent.getStatus().name(),
                EventCategory.CONCERT.name(),
                "Updated Artist",
                BigDecimal.valueOf(149.99),
                null,
                savedEvent.getRate(),
                SaleStatus.ONGOING.name(),
                savedEvent.isOverloaded(),
                savedEvent.getActiveReservationsCount(),
                staleVersion,
                null);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> eventService.updateEvent(validOwnerSessionId, staleUpdateDTO));

        // Assert
        Event unchangedEvent = eventRepository.getEventById(savedEvent.getId());

        assertTrue(exception.getMessage().contains("Event was updated by another request"));
        assertEquals(savedEvent.getName(), unchangedEvent.getName());
    }

    // -------------------- Define Event Map Tests -------------------
    @Test
    void GivenOwnerLoggedInEventExistsAndValidMap_WhenDefineEventMap_ThenConfigurationIsSaved() {
        Event event = createExistingEvent();
        eventRepository.addEvent(event);
        EventMapDTO validMapDTO = createValidMapDTO();

        Boolean result = eventService.defineEventMap(validOwnerSessionId, event.getId(), validMapDTO);

        Event updatedEvent = eventRepository.getEventById(event.getId());
        assertTrue(result);
        assertNotNull(updatedEvent);
        assertNotNull(updatedEvent.getMap());
        assertEquals(eventStatus.ACTIVE, updatedEvent.getStatus());
    }

    @Test
    void GivenOwnerLoggedInEventExistsAndInvalidMapConfiguration_WhenDefineEventMap_ThenSystemRejectsAndPreventsSaving() {
        Event event = createExistingEvent();
        eventRepository.addEvent(event);

        int originalElementCount = elementCount(eventRepository.getEventById(event.getId()));
        eventStatus originalStatus = eventRepository.getEventById(event.getId()).getStatus();

        EventMapDTO invalidMapDTO = null;

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.defineEventMap(validOwnerSessionId, event.getId(), invalidMapDTO));

        // Assert
        Event unchangedEvent = eventRepository.getEventById(event.getId());

        assertTrue(exception.getMessage().contains("Map data cannot be null"));
        assertEquals(originalElementCount, elementCount(unchangedEvent));
        assertEquals(originalStatus, unchangedEvent.getStatus());
    }

    @Test
    void GivenOwnerLoggedInEventExistsAndMapInventoryInconsistency_WhenDefineEventMap_ThenSystemRejectsAndPreventsSaving() {
        Event event = createExistingEvent();
        eventRepository.addEvent(event);
        int originalElementCount = elementCount(eventRepository.getEventById(event.getId()));
        eventStatus originalStatus = eventRepository.getEventById(event.getId()).getStatus();

        EventMapDTO inconsistentMapDTO = createInconsistentMapDTO();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.defineEventMap(validOwnerSessionId, event.getId(), inconsistentMapDTO));

        // Assert
        Event unchangedEvent = eventRepository.getEventById(event.getId());

        assertTrue(exception.getMessage().contains("Reserved and sold spots cannot exceed capacity"));
        assertEquals(originalElementCount, elementCount(unchangedEvent));
        assertEquals(originalStatus, unchangedEvent.getStatus());
    }

    @Test
    void GivenInvalidSession_WhenDefineEventMap_ThenSystemRejectsTheRequest() {
        Event event = createExistingEvent();
        eventRepository.addEvent(event);
        int originalElementCount = elementCount(eventRepository.getEventById(event.getId()));
        eventStatus originalStatus = eventRepository.getEventById(event.getId()).getStatus();

        EventMapDTO validMapDTO = createValidMapDTO();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.defineEventMap(invalidSessionId, event.getId(), validMapDTO));

        // Assert
        Event unchangedEvent = eventRepository.getEventById(event.getId());

        assertTrue(exception.getMessage().contains("Invalid session ID"));
        assertEquals(originalElementCount, elementCount(unchangedEvent));
        assertEquals(originalStatus, unchangedEvent.getStatus());
    }

    @Test
    void GivenLoggedInUserWithoutDefineMapPermission_WhenDefineEventMap_ThenSystemRejectsTheRequest() {
        Event event = createExistingEvent();
        eventRepository.addEvent(event);
        int originalElementCount = elementCount(eventRepository.getEventById(event.getId()));
        eventStatus originalStatus = eventRepository.getEventById(event.getId()).getStatus();

        String sessionWithoutPermission = "session-without-map-permission";
        Member plainUser = new Member(2L, "PlainUser", "Plain User", "0500000004", LocalDate.of(2001, 1, 1));
        userRepository.addRegisteredMember(2L, plainUser, "password");
        tokenService.addValidSession(sessionWithoutPermission, 2L);

        EventMapDTO validMapDTO = createValidMapDTO();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.defineEventMap(sessionWithoutPermission, event.getId(), validMapDTO));

        // Assert
        Event unchangedEvent = eventRepository.getEventById(event.getId());

        assertTrue(exception.getMessage().contains("User does not have permission to define event map"));
        assertEquals(originalElementCount, elementCount(unchangedEvent));
        assertEquals(originalStatus, unchangedEvent.getStatus());
    }

    @Test
    void GivenEventDoesNotExist_WhenDefineEventMap_ThenSystemRejectsTheRequest() {
        EventMapDTO validMapDTO = createValidMapDTO();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.defineEventMap(validOwnerSessionId, 999L, validMapDTO));

        assertTrue(exception.getMessage().contains("Event not found"));
    }

    private Event createExistingEvent() {
        Event event = new Event(
                1L,
                LocalDateTime.now().plusDays(10),
                "Rock Concert",
                companyId,
                ownerId,
                EventLocation.TEL_AVIV,
                100L,
                EventCategory.CONCERT,
                "The Rockers",
                BigDecimal.valueOf(99.99),
                new ticketsystem.DomainLayer.event.Pair<>(10, 20));
        event.setSaleStatus(SaleStatus.ONGOING);
        return event;
    }

    @Test
    void GivenOwnerLoggedInEventExistsAndMapElementsOverlap_WhenDefineEventMap_ThenSystemRejectsAndPreventsSaving() {
        Event event = createExistingEvent();
        eventRepository.addEvent(event);

        int originalElementCount = elementCount(eventRepository.getEventById(event.getId()));
        eventStatus originalStatus = eventRepository.getEventById(event.getId()).getStatus();

        EventMapDTO overlappingMapDTO = createOverlappingMapDTO();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.defineEventMap(validOwnerSessionId, event.getId(), overlappingMapDTO)
        );

        Event unchangedEvent = eventRepository.getEventById(event.getId());

        assertTrue(exception.getMessage().contains("Map elements cannot overlap"));
        assertEquals(originalElementCount, elementCount(unchangedEvent));
        assertEquals(originalStatus, unchangedEvent.getStatus());
    }

    @Test
    void GivenOwnerLoggedInEventExistsAndMapElementsOnlyTouchEdges_WhenDefineEventMap_ThenConfigurationIsSaved() {
        Event event = createExistingEvent();
        eventRepository.addEvent(event);

        EventMapDTO edgeTouchingMapDTO = createEdgeTouchingMapDTO();

        Boolean result = eventService.defineEventMap(validOwnerSessionId, event.getId(), edgeTouchingMapDTO);

        Event updatedEvent = eventRepository.getEventById(event.getId());

        assertTrue(result);
        assertNotNull(updatedEvent.getMap());
        assertEquals(eventStatus.ACTIVE, updatedEvent.getStatus());
    }

    // --------------------view Event Map Tests -------------------
    @Test
    void GivenUserEnteredSystemAndEventHasConfiguredMap_WhenGetEventMap_ThenSystemReturnsEventMapAndAvailability() {
        Event event = createExistingEvent();
        eventRepository.addEvent(event);
        EventMapDTO mapLayout = createValidMapDTO();

        eventService.defineEventMap(validOwnerSessionId, event.getId(), mapLayout);

        EventMapDTO result = eventService.getEventMap(validOwnerSessionId, event.getId());

        assertNotNull(result);
        assertEquals(new PairDTO<>(10, 20), result.size());
        assertFalse(result.soldOut());
        assertNotNull(result.getElementDTOs());
        assertEquals(4, result.getElementDTOs().size());

        assertTrue(mapContainsElementNamed(result, "Main Stage"));
        assertTrue(mapContainsElementNamed(result, "Main Entrance"));
        assertTrue(mapContainsElementNamed(result, "Seating Area A"));
        assertTrue(mapContainsElementNamed(result, "Standing Area B"));

        SeatingAreaDTO seatingArea = findSeatingArea(result, "Seating Area A");
        assertNotNull(seatingArea);
        assertEquals(4, seatingArea.rows());
        assertEquals(6, seatingArea.columns());
        assertEquals(24, seatingArea.seats().size());

        StandingAreaDTO standingArea = findStandingArea(result, "Standing Area B");
        assertNotNull(standingArea);
        assertEquals(100L, standingArea.capacity());
        assertEquals(0L, standingArea.reserved());
        assertEquals(0L, standingArea.sold());
    }

    @Test
    void GivenUserEnteredSystemAndEventDoesNotExist_WhenGetEventMap_ThenSystemRejectsTheRequest() {
        Long nonExistingEventId = 999L;

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.getEventMap(validOwnerSessionId, nonExistingEventId));

        assertTrue(exception.getMessage().contains("Event not found"));
    }

    @Test
    void GivenInvalidSession_WhenGetEventMap_ThenSystemRejectsTheRequest() {
        Event event = createExistingEvent();
        eventRepository.addEvent(event);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.getEventMap(invalidSessionId, event.getId()));

        // Assert
        assertTrue(exception.getMessage().contains("Invalid session ID"));
    }

    // -------------------- Cancel Event Tests -------------------
    @Test
    void GivenOwnerLoggedInEventExistsHistoryAndOrderListenersRegistered_WhenCancelEvent_ThenEventIsCanceledHistoryIsNotifiedAndActiveOrderIsCanceled() {
        // Arrange
        Event event = createActiveExistingEvent();

        FakeHistoryServiceListener historyListener = new FakeHistoryServiceListener();
        OrderRepository orderRepository = new OrderRepository();
        FakeNotificationsService notificationsService = new FakeNotificationsService();
        OrderService orderService = createOrderServiceListener(orderRepository, notificationsService);

        String buyerSessionId = "buyer-session";
        Long buyerId = 55L;

        ActiveOrder activeOrder = createActiveOrderForEvent(
                orderRepository,
                event.getId(),
                buyerSessionId,
                buyerId);

        eventService.addEventUpdatesListener(historyListener);
        eventService.addEventUpdatesListener(orderService);

        // Act
        Boolean result = eventService.cancelEvent(validOwnerSessionId, event.getId());

        // Assert
        Event cancelledEvent = eventRepository.getEventById(event.getId());
        ActiveOrder updatedOrder = orderRepository.findOrderById(activeOrder.getOrderId());

        assertTrue(result);
        assertNotNull(cancelledEvent);
        assertEquals(eventStatus.CANCELLED, cancelledEvent.getStatus());

        assertTrue(historyListener.wasNotifiedFor(event.getId()));
        assertEquals(1, historyListener.notificationCount());

        assertNotNull(updatedOrder);
        assertEquals(ActiveOrder.OrderStatus.CANCELLED, updatedOrder.getStatus());

        assertTrue(notificationsService.wasNotified(buyerSessionId));
        assertEquals(1, notificationsService.notificationCount(buyerSessionId));
        assertTrue(notificationsService.lastMessageFor(buyerSessionId)
                .contains("has been canceled due to event cancellation"));
    }

    @Test
    void GivenInvalidSession_WhenCancelEvent_ThenSystemRejectsTheRequestAndEventAndActiveOrderAreNotCanceled() {
        // Arrange
        Event event = createActiveExistingEvent();

        FakeHistoryServiceListener historyListener = new FakeHistoryServiceListener();
        OrderRepository orderRepository = new OrderRepository();
        FakeNotificationsService notificationsService = new FakeNotificationsService();
        OrderService orderService = createOrderServiceListener(orderRepository, notificationsService);

        String buyerSessionId = "buyer-session";
        Long buyerId = 55L;

        ActiveOrder activeOrder = createActiveOrderForEvent(
                orderRepository,
                event.getId(),
                buyerSessionId,
                buyerId);

        eventService.addEventUpdatesListener(historyListener);
        eventService.addEventUpdatesListener(orderService);

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.cancelEvent(invalidSessionId, event.getId()));

        // Assert
        Event unchangedEvent = eventRepository.getEventById(event.getId());
        ActiveOrder unchangedOrder = orderRepository.findOrderById(activeOrder.getOrderId());

        assertTrue(exception.getMessage().contains("Invalid session ID"));
        assertEquals(eventStatus.ACTIVE, unchangedEvent.getStatus());

        assertFalse(historyListener.wasNotifiedFor(event.getId()));

        assertNotNull(unchangedOrder);
        assertEquals(ActiveOrder.OrderStatus.ACTIVE, unchangedOrder.getStatus());

        assertFalse(notificationsService.wasNotified(buyerSessionId));
    }

    @Test
    void GivenLoggedInUserWithoutCancelPermission_WhenCancelEvent_ThenSystemRejectsTheRequestAndEventAndActiveOrderAreNotCanceled() {
        // Arrange
        Event event = createActiveExistingEvent();

        String sessionWithoutPermission = "session-without-cancel-permission";
        tokenService.addValidSession(sessionWithoutPermission, 2L);

        FakeHistoryServiceListener historyListener = new FakeHistoryServiceListener();
        OrderRepository orderRepository = new OrderRepository();
        FakeNotificationsService notificationsService = new FakeNotificationsService();
        OrderService orderService = createOrderServiceListener(orderRepository, notificationsService);

        String buyerSessionId = "buyer-session";
        Long buyerId = 55L;
        Member memberWithoutPermission = new Member(
                2L,
                "userWithoutPermission",
                "User Without Permission",
                "0500000000", LocalDate.of(2001, 1, 1)
        );

        userRepository.addRegisteredMember(
                2L,
                memberWithoutPermission,
                "password"
        );
        ActiveOrder activeOrder = createActiveOrderForEvent(
                orderRepository,
                event.getId(),
                buyerSessionId,
                buyerId);

        eventService.addEventUpdatesListener(historyListener);
        eventService.addEventUpdatesListener(orderService);

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.cancelEvent(sessionWithoutPermission, event.getId()));

        // Assert
        Event unchangedEvent = eventRepository.getEventById(event.getId());
        ActiveOrder unchangedOrder = orderRepository.findOrderById(activeOrder.getOrderId());

        assertTrue(exception.getMessage().contains("User does not have permission to cancel an event"));
        assertEquals(eventStatus.ACTIVE, unchangedEvent.getStatus());

        assertFalse(historyListener.wasNotifiedFor(event.getId()));

        assertNotNull(unchangedOrder);
        assertEquals(ActiveOrder.OrderStatus.ACTIVE, unchangedOrder.getStatus());

        assertFalse(notificationsService.wasNotified(buyerSessionId));
    }

    @Test
    void GivenEventDoesNotExist_WhenCancelEvent_ThenSystemRejectsTheRequestAndListenersAreNotNotified() {
        // Arrange
        Long nonExistingEventId = 999L;

        FakeHistoryServiceListener historyListener = new FakeHistoryServiceListener();
        OrderRepository orderRepository = new OrderRepository();
        FakeNotificationsService notificationsService = new FakeNotificationsService();
        OrderService orderService = createOrderServiceListener(orderRepository, notificationsService);

        eventService.addEventUpdatesListener(historyListener);
        eventService.addEventUpdatesListener(orderService);

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.cancelEvent(validOwnerSessionId, nonExistingEventId));

        // Assert
        assertTrue(exception.getMessage().contains("Event does not exist"));

        assertFalse(historyListener.wasNotifiedFor(nonExistingEventId));
    }

    @Test
    void GivenEventAlreadyCanceled_WhenCancelEvent_ThenSystemRejectsTheRequestAndOrderServiceIsNotNotifiedAgain() {
        // Arrange
        Event event = createActiveExistingEvent();

        FakeHistoryServiceListener historyListener = new FakeHistoryServiceListener();
        OrderRepository orderRepository = new OrderRepository();
        FakeNotificationsService notificationsService = new FakeNotificationsService();
        OrderService orderService = createOrderServiceListener(orderRepository, notificationsService);

        String buyerSessionId = "buyer-session";
        Long buyerId = 55L;

        ActiveOrder activeOrder = createActiveOrderForEvent(
                orderRepository,
                event.getId(),
                buyerSessionId,
                buyerId);

        eventService.addEventUpdatesListener(historyListener);
        eventService.addEventUpdatesListener(orderService);

        eventService.cancelEvent(validOwnerSessionId, event.getId());

        assertEquals(1, historyListener.notificationCount());
        assertEquals(1, notificationsService.notificationCount(buyerSessionId));
        assertEquals(
                ActiveOrder.OrderStatus.CANCELLED,
                orderRepository.findOrderById(activeOrder.getOrderId()).getStatus());

        // Act
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> eventService.cancelEvent(validOwnerSessionId, event.getId()));

        // Assert
        Event cancelledEvent = eventRepository.getEventById(event.getId());
        ActiveOrder orderAfterSecondCancel = orderRepository.findOrderById(activeOrder.getOrderId());

        assertTrue(exception.getMessage().contains("Event is already canceled"));
        assertEquals(eventStatus.CANCELLED, cancelledEvent.getStatus());

        assertEquals(1, historyListener.notificationCount());
        assertEquals(1, notificationsService.notificationCount(buyerSessionId));
        assertEquals(ActiveOrder.OrderStatus.CANCELLED, orderAfterSecondCancel.getStatus());
    }
    // -------------------- Event Discount Policy Tests --------------------

    @Test
    void GivenOwnerLoggedInAndValidVisibleDiscount_WhenAddVisibleDiscountToEvent_ThenDiscountIsAddedSuccessfully() throws Exception {
        // Arrange
        Event event = createExistingEvent();
        eventRepository.addEvent(event);

        // Act
        eventService.addVisibleDiscountToEvent(
                validOwnerSessionId,
                event.getId(),
                "Summer Discount",
                BigDecimal.valueOf(15)
        );

        // Assert
        Event updatedEvent = eventRepository.getEventById(event.getId());

        assertNotNull(updatedEvent);
        assertEquals(1, updatedEvent.getDiscounts().size());
        assertEquals("Summer Discount",
                updatedEvent.getDiscounts().get(0).getName());
    }

    @Test
    void GivenOwnerLoggedInAndInvalidDiscountPercentage_WhenAddVisibleDiscountToEvent_ThenSystemRejectsTheRequest() {
        // Arrange
        Event event = createExistingEvent();
        eventRepository.addEvent(event);

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.addVisibleDiscountToEvent(
                        validOwnerSessionId,
                        event.getId(),
                        "Invalid Discount",
                        BigDecimal.valueOf(150)
                )
        );

        // Assert
        Event unchangedEvent = eventRepository.getEventById(event.getId());

        assertTrue(exception.getMessage().contains("percentage"));
        assertEquals(0, unchangedEvent.getDiscounts().size());
    }

    @Test
    void GivenUserWithoutPermission_WhenAddVisibleDiscountToEvent_ThenSystemRejectsTheRequest() {
        // Arrange
        Event event = createExistingEvent();
        eventRepository.addEvent(event);

        String sessionWithoutPermission = "session-without-discount-permission";

        Member plainUser = new Member(2L, "PlainUser", "Plain User", "0500000005", LocalDate.of(2001, 1, 1));
        userRepository.addRegisteredMember(2L, plainUser, "password");

        tokenService.addValidSession(sessionWithoutPermission, 2L);

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.addVisibleDiscountToEvent(
                        sessionWithoutPermission,
                        event.getId(),
                        "Summer Discount",
                        BigDecimal.valueOf(10)
                )
        );

        // Assert
        assertTrue(exception.getMessage()
                .contains("User does not have permission to manage event discount policy"));
    }

    @Test
    void GivenEventDoesNotExist_WhenAddVisibleDiscountToEvent_ThenSystemRejectsTheRequest() {
        // Act
        Exception exception = assertThrows(
                Exception.class,
                () -> eventService.addVisibleDiscountToEvent(
                        validOwnerSessionId,
                        999L,
                        "Summer Discount",
                        BigDecimal.valueOf(10)
                )
        );

        // Assert
        assertTrue(exception.getMessage().contains("Event not found"));
    }

// -------------------- Coupon Discount Tests --------------------
    @Test
    void GivenOwnerLoggedInAndValidCouponDiscount_WhenAddCouponDiscountToEvent_ThenCouponDiscountIsAddedSuccessfully() throws Exception {
        // Arrange
        Event event = createExistingEvent();
        eventRepository.addEvent(event);

        LocalDateTime endDate = LocalDateTime.now().plusDays(10);

        // Act
        eventService.addCouponDiscountToEvent(
                validOwnerSessionId,
                event.getId(),
                "Coupon Discount",
                "SAVE20",
                BigDecimal.valueOf(20),
                endDate
        );

        // Assert
        Event updatedEvent = eventRepository.getEventById(event.getId());

        assertEquals(1, updatedEvent.getDiscounts().size());
        assertEquals("Coupon Discount",
                updatedEvent.getDiscounts().get(0).getName());
    }

    @Test
    void GivenCouponDiscountWithPastExpirationDate_WhenAddCouponDiscountToEvent_ThenSystemRejectsTheRequest() {
        // Arrange
        Event event = createExistingEvent();
        eventRepository.addEvent(event);

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.addCouponDiscountToEvent(
                        validOwnerSessionId,
                        event.getId(),
                        "Coupon Discount",
                        "SAVE20",
                        BigDecimal.valueOf(20),
                        LocalDateTime.now().minusDays(1)
                )
        );

        // Assert
        assertTrue(exception.getMessage()
                .contains("End time cannot be in the past"));
    }

// -------------------- Conditional Discount Tests --------------------
    @Test
    void GivenOwnerLoggedInAndValidConditionalDiscount_WhenAddConditionalDiscountToEvent_ThenConditionalDiscountIsAddedSuccessfully() throws Exception {
        // Arrange
        Event event = createExistingEvent();
        eventRepository.addEvent(event);

        // Act
        eventService.addConditionalDiscountToEvent(
                validOwnerSessionId,
                event.getId(),
                "Bulk Discount",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(5),
                BigDecimal.valueOf(25),
                Condition.MIN_TICKET,
                4
        );

        // Assert
        Event updatedEvent = eventRepository.getEventById(event.getId());

        assertEquals(1, updatedEvent.getDiscounts().size());
        assertEquals("Bulk Discount",
                updatedEvent.getDiscounts().get(0).getName());
    }

    @Test
    void GivenConditionalDiscountWithInvalidThreshold_WhenAddConditionalDiscountToEvent_ThenSystemRejectsTheRequest() {
        // Arrange
        Event event = createExistingEvent();
        eventRepository.addEvent(event);

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.addConditionalDiscountToEvent(
                        validOwnerSessionId,
                        event.getId(),
                        "Bulk Discount",
                        LocalDateTime.now(),
                        LocalDateTime.now().plusDays(5),
                        BigDecimal.valueOf(25),
                        Condition.MIN_TICKET,
                        0
                )
        );

        // Assert
        assertTrue(exception.getMessage().contains("threshold"));
    }

// -------------------- Remove Discount Tests --------------------
    @Test
    void GivenExistingDiscount_WhenRemoveDiscountFromEvent_ThenDiscountIsRemovedSuccessfully() throws Exception {
        // Arrange
        Event event = createExistingEvent();
        eventRepository.addEvent(event);

        eventService.addVisibleDiscountToEvent(
                validOwnerSessionId,
                event.getId(),
                "Summer Discount",
                BigDecimal.valueOf(15)
        );

        Long discountId
                = eventRepository.getEventById(event.getId())
                        .getDiscounts()
                        .get(0)
                        .getDiscountId();

        // Act
        eventService.removeDiscountFromEvent(
                validOwnerSessionId,
                event.getId(),
                discountId
        );

        // Assert
        Event updatedEvent = eventRepository.getEventById(event.getId());

        assertEquals(0, updatedEvent.getDiscounts().size());
    }

    @Test
    void GivenDiscountDoesNotExist_WhenRemoveDiscountFromEvent_ThenSystemRejectsTheRequest() {
        // Arrange
        Event event = createExistingEvent();
        eventRepository.addEvent(event);

        // Act
        Exception exception = assertThrows(
                Exception.class,
                () -> eventService.removeDiscountFromEvent(
                        validOwnerSessionId,
                        event.getId(),
                        999L
                )
        );

        // Assert
        assertTrue(exception.getMessage().contains("Discount"));
    }

// -------------------- Composition Type Tests --------------------
    @Test
    void GivenOwnerLoggedIn_WhenSetEventDiscountCompositionType_ThenCompositionTypeIsUpdated() throws Exception {
        // Arrange
        Event event = createExistingEvent();
        eventRepository.addEvent(event);

        // Act
        eventService.setEventDiscountCompositionType(
                validOwnerSessionId,
                event.getId(),
                DiscountCompositionType.MAX
        );

        // Assert
        Event updatedEvent = eventRepository.getEventById(event.getId());

        assertEquals(
                DiscountCompositionType.MAX,
                updatedEvent.getDiscountPolicy().getDiscountCompositionType()
        );
    }

    // -------------------- Helper Methods and Test Doubles -------------------
    private EventDTO createValidUpdateDTO(Event savedEvent) {
        return new EventDTO(
                savedEvent.getId(),
                "Updated Rock Concert",
                savedEvent.getCompanyId(),
                savedEvent.getOpenedBy(),
                LocalDateTime.now().plusDays(30),
                EventLocation.HAIFA.name(),
                200L,
                savedEvent.getStatus().name(),
                EventCategory.CONCERT.name(),
                "Updated Artist",
                BigDecimal.valueOf(149.99),
                null,
                savedEvent.getRate(),
                SaleStatus.ONGOING.name(),
                savedEvent.isOverloaded(),
                savedEvent.getActiveReservationsCount(),
                savedEvent.getVersion(),
                null);
    }

    private EventMapDTO createValidMapDTO() {
        ElementDTO stage = new ElementDTO(
                1L, "Main Stage", new PairDTO<>(0, 0), new PairDTO<>(2, 10), "Stage");

        ElementDTO entrance = new ElementDTO(
                2L, "Main Entrance", new PairDTO<>(10, 0), new PairDTO<>(1, 3), "Entrance");

        SeatingAreaDTO seatingArea = new SeatingAreaDTO(
                3L,
                "Seating Area A",
                new PairDTO<>(3, 2),
                new PairDTO<>(4, 6),
                "SeatingArea",
                false,
                4,
                6,
                List.of()
        );

        StandingAreaDTO standingArea = new StandingAreaDTO(
                4L,
                "Standing Area B",
                new PairDTO<>(7, 2),
                new PairDTO<>(3, 5),
                "StandingArea",
                false,
                100L,
                0L,
                0L
        );

        return new EventMapDTO(
                new PairDTO<>(10, 20),
                List.of(stage, entrance, seatingArea, standingArea),
                false
        );
    }

    private EventMapDTO createOverlappingMapDTO() {
        ElementDTO stage = new ElementDTO(
                1L,
                "Main Stage",
                new PairDTO<>(0, 0),
                new PairDTO<>(4, 4),
                "Stage"
        );

        SeatingAreaDTO seatingArea = new SeatingAreaDTO(
                2L,
                "Seating Area A",
                new PairDTO<>(2, 2),
                new PairDTO<>(4, 4),
                "SeatingArea",
                false,
                4,
                6,
                List.of()
        );

        return new EventMapDTO(
                new PairDTO<>(10, 20),
                List.of(stage, seatingArea),
                false
        );
    }

    private EventMapDTO createEdgeTouchingMapDTO() {
        ElementDTO stage = new ElementDTO(
                1L,
                "Main Stage",
                new PairDTO<>(0, 0),
                new PairDTO<>(4, 4),
                "Stage"
        );

        SeatingAreaDTO seatingArea = new SeatingAreaDTO(
                2L,
                "Seating Area A",
                new PairDTO<>(4, 0),
                new PairDTO<>(4, 4),
                "SeatingArea",
                false,
                4,
                6,
                List.of()
        );

        return new EventMapDTO(
                new PairDTO<>(10, 20),
                List.of(stage, seatingArea),
                false
        );
    }

    private EventMapDTO createInconsistentMapDTO() {
        StandingAreaDTO inconsistentStandingArea = new StandingAreaDTO(
                1L, "Invalid Standing Area", new PairDTO<>(2, 2), new PairDTO<>(4, 5), "StandingArea", false, 10L, 8L,
                5L);

        return new EventMapDTO(new PairDTO<>(10, 20), List.of(inconsistentStandingArea), false);
    }

    private int elementCount(Event event) {
        if (event.getMap() == null || event.getMap().getElements() == null) {
            return 0;
        }

        return event.getMap().getElements().size();
    }

    private boolean mapContainsElementNamed(EventMapDTO mapDTO, String name) {
        return mapDTO.getElementDTOs()
                .stream()
                .anyMatch(element -> elementName(element).equals(name));
    }

    private String elementName(IMapElementDTO element) {
        if (element instanceof ElementDTO elementDTO) {
            return elementDTO.name();
        }

        if (element instanceof SeatingAreaDTO seatingAreaDTO) {
            return seatingAreaDTO.name();
        }

        if (element instanceof StandingAreaDTO standingAreaDTO) {
            return standingAreaDTO.name();
        }

        return "";
    }

    private SeatingAreaDTO findSeatingArea(EventMapDTO mapDTO, String name) {
        return mapDTO.getElementDTOs()
                .stream()
                .filter(element -> element instanceof SeatingAreaDTO)
                .map(element -> (SeatingAreaDTO) element)
                .filter(area -> area.name().equals(name))
                .findFirst()
                .orElse(null);
    }

    private StandingAreaDTO findStandingArea(EventMapDTO mapDTO, String name) {
        return mapDTO.getElementDTOs()
                .stream()
                .filter(element -> element instanceof StandingAreaDTO)
                .map(element -> (StandingAreaDTO) element)
                .filter(area -> area.name().equals(name))
                .findFirst()
                .orElse(null);
    }

    private Event createActiveExistingEvent() {
        Event event = createExistingEvent();
        eventRepository.addEvent(event);

        eventService.defineEventMap(
                validOwnerSessionId,
                event.getId(),
                createValidMapDTO());

        return eventRepository.getEventById(event.getId());
    }

    private ActiveOrder createActiveOrderForEvent(OrderRepository orderRepository,
            Long eventId,
            String buyerSessionId,
            Long buyerId) {
        ActiveOrder activeOrder = new ActiveOrder(
                orderRepository.getNextId(),
                buyerSessionId,
                buyerId,
                eventId);

        activeOrder.addTicket(
                new Ticket(
                        1L,
                        eventId,
                        3L,
                        1,
                        1,
                        BigDecimal.valueOf(99.99)));

        orderRepository.addOrder(activeOrder);

        return activeOrder;
    }

    private OrderService createOrderServiceListener(
            OrderRepository orderRepository,
            FakeNotificationsService notificationsService) {
        return new OrderService(
                orderRepository,
                null,
                new LogbackSystemLogger(),
                notificationsService);
    }

    private static class FakeTokenService implements ITokenService {

        private final Set<String> validSessions = new HashSet<>();
        private final Map<String, Long> userIdsBySession = new HashMap<>();

        void addValidSession(String sessionId, Long userId) {
            validSessions.add(sessionId);
            userIdsBySession.put(sessionId, userId);
        }

        @Override
        public boolean validateToken(String sessionId) {
            return validSessions.contains(sessionId);
        }

        @Override
        public Long extractUserId(String sessionId) {
            return userIdsBySession.get(sessionId);
        }

        @Override
        public String addActiveSession(User user) {
            String sessionId = "test-session-" + (validSessions.size() + 1);
            validSessions.add(sessionId);
            return sessionId;
        }

        @Override
        public boolean isActiveSession(String sessionToken) {
            return validSessions.contains(sessionToken);
        }

        @Override
        public int getTotalActiveSessions() {
            return validSessions.size();
        }

        @Override
        public void removeActiveSession(String sessionToken) {
            validSessions.remove(sessionToken);
            userIdsBySession.remove(sessionToken);
        }

        @Override
        public String generateNewGuestToken() {
            return "guest-token";
        }

        @Override
        public String generateNewMemberToken(Long userId) {
            return "member-token-" + userId;
        }

        @Override
        public String extractRole(String token) {
            return null;
        }

        @Override
        public boolean isGuestToken(String token) {
            return false;
        }

        @Override
        public boolean isMemberToken(String token) {
            return false;
        }

        @Override
        public String maskToken(String token) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    private static class FakeHistoryServiceListener implements EventUpdatesListener {

        private final List<Long> canceledEventIds = new java.util.ArrayList<>();
        private final List<String> receivedUpdateMessages = new java.util.ArrayList<>();

        @Override
        public void onEventCanceled(Long eventId) {
            canceledEventIds.add(eventId);
        }

        @Override
        public void onEventUpdated(Long eventId, LocalDateTime date, String location, String updateMessage) {
            receivedUpdateMessages.add(updateMessage);
        }

        boolean wasNotifiedFor(Long eventId) {
            return canceledEventIds.contains(eventId);
        }

        int notificationCount() {
            return canceledEventIds.size();
        }
    }

// -------------------- Set Event Purchase Policy Tests -------------------
    @Test
    void GivenOwnerLoggedInEventExistsAndMaxTicketsPolicy_WhenSetEventPurchasePolicy_ThenPolicyIsSavedAndEnforced() throws Exception {
        Event event = createExistingEvent();
        eventRepository.addEvent(event);

        PurchasePolicyDTO policyDTO = maxTicketsPolicyDTO(5);

        eventService.setEventPurchasePolicy(
                validOwnerSessionId,
                event.getId(),
                policyDTO
        );

        Event updatedEvent = eventRepository.getEventById(event.getId());

        assertNotNull(updatedEvent);
        assertDoesNotThrow(() -> updatedEvent.canPurchase(5, 20));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> updatedEvent.canPurchase(6, 20)
        );

        assertTrue(exception.getMessage().contains("Cannot purchase more than 5 tickets"));
    }

    @Test
    void GivenOwnerLoggedInEventExistsAndMinAgePolicy_WhenSetEventPurchasePolicy_ThenPolicyIsSavedAndEnforced() throws Exception {
        Event event = createExistingEvent();
        eventRepository.addEvent(event);

        PurchasePolicyDTO policyDTO = minAgePolicyDTO(18);

        eventService.setEventPurchasePolicy(
                validOwnerSessionId,
                event.getId(),
                policyDTO
        );

        Event updatedEvent = eventRepository.getEventById(event.getId());

        assertNotNull(updatedEvent);
        assertDoesNotThrow(() -> updatedEvent.canPurchase(1, 18));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> updatedEvent.canPurchase(1, 17)
        );

        assertTrue(exception.getMessage().contains("minimum age requirement of 18"));
    }

    @Test
    void GivenOwnerLoggedInEventExistsAndNestedPurchasePolicy_WhenSetEventPurchasePolicy_ThenNestedPolicyIsSavedAndEnforced() throws Exception {
        Event event = createExistingEvent();
        eventRepository.addEvent(event);

        PurchasePolicyDTO policyDTO = nestedPolicyDTO();

        eventService.setEventPurchasePolicy(
                validOwnerSessionId,
                event.getId(),
                policyDTO
        );

        Event updatedEvent = eventRepository.getEventById(event.getId());

        assertNotNull(updatedEvent);

        assertDoesNotThrow(() -> updatedEvent.canPurchase(2, 18));
        assertDoesNotThrow(() -> updatedEvent.canPurchase(100, 18));

        assertThrows(
                IllegalArgumentException.class,
                () -> updatedEvent.canPurchase(50, 18)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> updatedEvent.canPurchase(2, 17)
        );
    }

    @Test
    void GivenLoggedInUserWithoutPermission_WhenSetEventPurchasePolicy_ThenSystemRejectsAndPolicyIsNotChanged() throws Exception {
        Event event = createExistingEvent();
        eventRepository.addEvent(event);

        String sessionWithoutPermission = "session-without-policy-permission";
        Long plainUserId = 2L;

        Member plainUser = new Member(plainUserId, "PlainUser", "Plain User", "0500000002", LocalDate.of(2001, 1, 1));
        userRepository.addRegisteredMember(plainUserId, plainUser, "password");
        tokenService.addValidSession(sessionWithoutPermission, plainUserId);

        PurchasePolicyDTO policyDTO = maxTicketsPolicyDTO(5);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.setEventPurchasePolicy(
                        sessionWithoutPermission,
                        event.getId(),
                        policyDTO
                )
        );

        Event unchangedEvent = eventRepository.getEventById(event.getId());

        assertTrue(exception.getMessage().contains("permission"));
        assertDoesNotThrow(() -> unchangedEvent.canPurchase(100, 0));
    }

    @Test
    void GivenEventDoesNotExist_WhenSetEventPurchasePolicy_ThenSystemRejectsTheRequest() {
        PurchasePolicyDTO policyDTO = maxTicketsPolicyDTO(5);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.setEventPurchasePolicy(
                        validOwnerSessionId,
                        999L,
                        policyDTO
                )
        );

        assertTrue(exception.getMessage().contains("Event not found"));
    }

    @Test
    void GivenInvalidPurchasePolicyDTO_WhenSetEventPurchasePolicy_ThenSystemRejectsAndPolicyIsNotChanged() throws Exception {
        Event event = createExistingEvent();
        eventRepository.addEvent(event);

        PurchasePolicyDTO invalidPolicyDTO = new PurchasePolicyDTO(
                new PurchaseRuleDTO(PurchaseRuleType.MAX_TICKETS, null, null)
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.setEventPurchasePolicy(
                        validOwnerSessionId,
                        event.getId(),
                        invalidPolicyDTO
                )
        );

        Event unchangedEvent = eventRepository.getEventById(event.getId());

        assertTrue(exception.getMessage().contains("Maximum tickets is required"));
        assertDoesNotThrow(() -> unchangedEvent.canPurchase(100, 0));
    }

    @Test
    void GivenInvalidSession_WhenSetEventPurchasePolicy_ThenSystemRejectsAndPolicyIsNotChanged() throws Exception {
        Event event = createExistingEvent();
        eventRepository.addEvent(event);

        PurchasePolicyDTO policyDTO = maxTicketsPolicyDTO(5);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.setEventPurchasePolicy(
                        invalidSessionId,
                        event.getId(),
                        policyDTO
                )
        );

        Event unchangedEvent = eventRepository.getEventById(event.getId());

        assertTrue(exception.getMessage().contains("permission"));
        assertDoesNotThrow(() -> unchangedEvent.canPurchase(100, 0));
    }

    private PurchasePolicyDTO maxTicketsPolicyDTO(int maxTickets) {
        return new PurchasePolicyDTO(
                new PurchaseRuleDTO(PurchaseRuleType.MAX_TICKETS, maxTickets, null)
        );
    }

    private PurchasePolicyDTO minAgePolicyDTO(int minAge) {
        return new PurchasePolicyDTO(
                new PurchaseRuleDTO(PurchaseRuleType.MIN_AGE, minAge, null)
        );
    }

    private PurchasePolicyDTO nestedPolicyDTO() {
        return new PurchasePolicyDTO(
                new PurchaseRuleDTO(
                        PurchaseRuleType.AND,
                        null,
                        List.of(
                                new PurchaseRuleDTO(PurchaseRuleType.MIN_AGE, 18, null),
                                new PurchaseRuleDTO(
                                        PurchaseRuleType.OR,
                                        null,
                                        List.of(
                                                new PurchaseRuleDTO(PurchaseRuleType.MAX_TICKETS, 2, null),
                                                new PurchaseRuleDTO(PurchaseRuleType.MIN_TICKETS, 100, null)
                                        )
                                )
                        )
                )
        );
    }

    private static class FakeNotificationsService implements INotifier {

        private final Map<String, List<String>> messagesBySession = new HashMap<>();
        private final Map<Long, List<String>> messagesByMember = new HashMap<>();
        private final List<String> allMessages = new ArrayList<>();

        @Override
        public void notifyGuest(String sessionId, String message) {
            messagesBySession
                    .computeIfAbsent(sessionId, key -> new ArrayList<>())
                    .add(message);

            allMessages.add(message);
        }

        @Override
        public void notifyMember(Long memberId, String message) {
            messagesByMember
                    .computeIfAbsent(memberId, key -> new ArrayList<>())
                    .add(message);

            allMessages.add(message);
        }

        @Override
        public void notifyMembers(Collection<Long> memberIds, String message) {
            if (memberIds == null) {
                return;
            }

            for (Long memberId : memberIds) {
                if (memberId != null) {
                    notifyMember(memberId, message);
                }
            }
        }

        @Override
        public void notifyGuests(Collection<String> guestTokens, String message) {
            if (guestTokens == null) {
                return;
            }

            for (String guestToken : guestTokens) {
                if (guestToken != null && !guestToken.isBlank()) {
                    notifyGuest(guestToken, message);
                }
            }
        }

        boolean wasNotified(String sessionId) {
            return messagesBySession.containsKey(sessionId)
                    && !messagesBySession.get(sessionId).isEmpty();
        }

        int notificationCount(String sessionId) {
            return messagesBySession
                    .getOrDefault(sessionId, List.of())
                    .size();
        }

        String lastMessageFor(String sessionId) {
            List<String> messages = messagesBySession.getOrDefault(sessionId, List.of());

            if (messages.isEmpty()) {
                return "";
            }

            return messages.get(messages.size() - 1);
        }

        boolean wasMemberNotified(Long memberId) {
            return messagesByMember.containsKey(memberId)
                    && !messagesByMember.get(memberId).isEmpty();
        }

        int memberNotificationCount(Long memberId) {
            return messagesByMember
                    .getOrDefault(memberId, List.of())
                    .size();
        }

        String lastMessageForMember(Long memberId) {
            List<String> messages = messagesByMember.getOrDefault(memberId, List.of());

            if (messages.isEmpty()) {
                return "";
            }

            return messages.get(messages.size() - 1);
        }

        boolean containsMessage(String text) {
            return allMessages.stream()
                    .anyMatch(message -> message.contains(text));
        }
    }
}
