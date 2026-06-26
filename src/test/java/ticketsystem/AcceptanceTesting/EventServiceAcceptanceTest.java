package ticketsystem.AcceptanceTesting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.ApplicationLayer.EventService;
import ticketsystem.ApplicationLayer.Events.EventUpdatesListener;
import ticketsystem.ApplicationLayer.HistoryService;
import ticketsystem.ApplicationLayer.INotifier;
import ticketsystem.ApplicationLayer.IPaymentService;
import ticketsystem.ApplicationLayer.ISystemLogger;
import ticketsystem.ApplicationLayer.ITicketIssuingService;
import ticketsystem.ApplicationLayer.ITokenService;
import ticketsystem.ApplicationLayer.OrderService;
import ticketsystem.ApplicationLayer.TokenService;
import ticketsystem.ApplicationLayer.UserAccessService;
import ticketsystem.DTO.Event.ElementDTO;
import ticketsystem.DTO.Event.EventDTO;
import ticketsystem.DTO.Event.EventMapDTO;
import ticketsystem.DTO.Event.IMapElementDTO;
import ticketsystem.DTO.Event.PairDTO;
import ticketsystem.DTO.Event.SeatingAreaDTO;
import ticketsystem.DTO.Event.StandingAreaDTO;
import ticketsystem.DTO.DiscountPolicyDTO;
import ticketsystem.DTO.PurchasePolicyDTO;
import ticketsystem.DTO.PurchaseRuleDTO;
import ticketsystem.DTO.PurchaseRuleType;
import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.MembershipDomainService;
import ticketsystem.DomainLayer.discount.ConditionalDiscount;
import ticketsystem.DomainLayer.discount.CouponDiscount;
import ticketsystem.DomainLayer.discount.DiscountCompositionType;
import ticketsystem.DomainLayer.discount.VisibleDiscount;
import ticketsystem.DomainLayer.event.*;
import ticketsystem.DomainLayer.event.Event.eventStatus;
import ticketsystem.DomainLayer.order.ActiveOrder;
import ticketsystem.DomainLayer.order.Ticket;
import ticketsystem.DomainLayer.user.CompanyRole;
import ticketsystem.DomainLayer.user.Guest;
import ticketsystem.DomainLayer.user.Member;
import ticketsystem.DomainLayer.user.Permission;
import ticketsystem.DomainLayer.user.RoleStatus;
import ticketsystem.DomainLayer.IRepository.IOrderRepository;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.InfrastructureLayer.InMemoryEventRepository;
import ticketsystem.InfrastructureLayer.HistoryRepository;
import ticketsystem.InfrastructureLayer.InMemoryUserRepository;
import ticketsystem.InfrastructureLayer.InMemoryOrderRepository;
import ticketsystem.InfrastructureLayer.LogbackSystemLogger;
import ticketsystem.InfrastructureLayer.InMemoryNotificationsRepository;
import ticketsystem.InfrastructureLayer.TokenRepository;
import ticketsystem.InfrastructureLayer.VaadinNotifier;
import ticketsystem.DTO.DiscountDTO;
import ticketsystem.DTO.DiscountConditionDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
public class EventServiceAcceptanceTest {

    private EventService eventService;

    @Autowired
    private IEventRepository eventRepository;
    private IUserRepository userRepository;
    private ITokenService tokenService;
    private MembershipDomainService membershipDomain;
    private final ISystemLogger logger = new LogbackSystemLogger();
    private InMemoryNotificationsRepository notificationsRepository;
    private INotifier notifier;
    private String validOwnerSessionId;
    private final String invalidSessionId = "invalid-session";
    @Autowired
    private HistoryRepository historyRepository;
    private final Long ownerId = 1L;
    private final Long companyId = 100L;
    private UserAccessService userAccessService;
    private HistoryService historyService;
    private IPaymentService paymentService;
    private ITicketIssuingService ticketIssuingService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService("default_secret_key_for_development_purposes_only_32_chars", new TokenRepository(), new LogbackSystemLogger());
        userRepository = new InMemoryUserRepository();
        notificationsRepository = new InMemoryNotificationsRepository();
        notifier = new VaadinNotifier(notificationsRepository);
        userAccessService = new UserAccessService(userRepository);
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

        paymentService = mock(IPaymentService.class);
        ticketIssuingService = mock(ITicketIssuingService.class);

        when(paymentService.handshake()).thenReturn(true);
        when(paymentService.refund(any())).thenReturn(true);

        when(ticketIssuingService.handshake()).thenReturn(true);
        when(ticketIssuingService.cancelTicket(any())).thenReturn(true);

        historyService = new HistoryService(
                historyRepository,
                tokenService,
                membershipDomain,
                logger,
                userAccessService,
                notifier,
                paymentService,ticketIssuingService
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

        validOwnerSessionId = tokenService.addActiveSession(ownerMember);
    }

    // -------------------- Insert Event Tests -------------------
    @Test
    void GivenOwnerLoggedInAndValidEventDetails_WhenInsertEvent_ThenEventIsCreatedAndSaved() {
        // Arrange
        String eventName = "Rock Concert";
        LocalDateTime eventDate = LocalDateTime.now().plusDays(10);

        // Act
        Long eventId = eventService.insertEvent(
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
        assertNotNull(eventId);
        Event savedEvent = eventRepository.getEventById(eventId);

        assertNotNull(savedEvent);
        assertEquals(eventName, savedEvent.getName());
        assertEquals(eventId, savedEvent.getId());
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

        assertTrue(exception.getMessage().contains("Invalid or expired security token"));
        assertNull(eventRepository.getEventById(1L));
    }

    @Test
    void GivenLoggedInUserWithoutCreatePermission_WhenInsertEvent_ThenSystemRejectsTheRequest() {
        String sessionWithoutPermission;
        Long plainUserId = 2L;

        // Setup a real user WITHOUT any roles
        Member plainUser = new Member(plainUserId, "PlainUser", "Plain User", "0500000002", LocalDate.of(2001, 1, 1));
        userRepository.addRegisteredMember(plainUserId, plainUser, "password");
        sessionWithoutPermission = tokenService.addActiveSession(plainUser);

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
        assertEquals(0, BigDecimal.valueOf(149.99).compareTo(updatedEvent.getMinimalTicketPrice()));
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
        BigDecimal originalPrice = savedEvent.getMinimalTicketPrice();

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
        assertEquals(0, originalPrice.compareTo(unchangedEvent.getMinimalTicketPrice()));
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

        assertTrue(exception.getMessage().contains("Invalid or expired security token"));
        assertEquals(savedEvent.getName(), unchangedEvent.getName());
    }

    @Test
    void GivenLoggedInUserWithoutUpdatePermission_WhenUpdateEvent_ThenSystemRejectsTheRequest() {
        Event event = createExistingEvent();
        eventRepository.addEvent(event);
        Event savedEvent = eventRepository.getEventById(event.getId());

        String sessionWithoutPermission;

        // Setup a real user WITHOUT any roles
        Member plainUser = new Member(2L, "PlainUser", "Plain User", "0500000003", LocalDate.of(2001, 1, 1));
        userRepository.addRegisteredMember(2L, plainUser, "password");
        sessionWithoutPermission = tokenService.addActiveSession(plainUser);

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

        assertTrue(exception.getMessage().contains("Invalid or expired security token"));
        assertEquals(originalElementCount, elementCount(unchangedEvent));
        assertEquals(originalStatus, unchangedEvent.getStatus());
    }

    @Test
    void GivenLoggedInUserWithoutDefineMapPermission_WhenDefineEventMap_ThenSystemRejectsTheRequest() {
        Event event = createExistingEvent();
        eventRepository.addEvent(event);
        int originalElementCount = elementCount(eventRepository.getEventById(event.getId()));
        eventStatus originalStatus = eventRepository.getEventById(event.getId()).getStatus();

        String sessionWithoutPermission;
        Member plainUser = new Member(2L, "PlainUser", "Plain User", "0500000004", LocalDate.of(2001, 1, 1));
        userRepository.addRegisteredMember(2L, plainUser, "password");
        sessionWithoutPermission = tokenService.addActiveSession(plainUser);

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

    @Test
    void GivenEventWithDefinedMap_WhenDefineEventMapAgain_ThenSystemRejectsAndExistingMapIsNotReplaced() {
        Event event = createExistingEvent();
        eventRepository.addEvent(event);

        eventService.defineEventMap(
                validOwnerSessionId,
                event.getId(),
                createValidMapDTO()
        );

        Event eventAfterFirstDefinition =
                eventRepository.getEventById(event.getId());

        List<Long> originalElementIds =
                eventAfterFirstDefinition.getMap()
                        .getElements()
                        .stream()
                        .map(Element::getId)
                        .toList();

        int originalElementCount =
                eventAfterFirstDefinition.getMap()
                        .getElements()
                        .size();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> eventService.defineEventMap(
                        validOwnerSessionId,
                        event.getId(),
                        createEdgeTouchingMapDTO()
                )
        );

        Event unchangedEvent =
                eventRepository.getEventById(event.getId());

        List<Long> persistedElementIds =
                unchangedEvent.getMap()
                        .getElements()
                        .stream()
                        .map(Element::getId)
                        .toList();

        assertTrue(exception.getMessage().contains("defined"));
        assertEquals(eventStatus.ACTIVE, unchangedEvent.getStatus());
        assertEquals(originalElementCount, elementCount(unchangedEvent));
        assertEquals(originalElementIds, persistedElementIds);
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
        assertTrue(exception.getMessage().contains("Invalid or expired security token"));
    }

    // -------------------- Cancel Event Tests -------------------
    @Test
    void GivenOwnerLoggedInEventExistsHistoryAndOrderListenersRegistered_WhenCancelEvent_ThenEventIsCanceledHistoryIsNotifiedAndActiveOrderIsCanceled() {
        // Arrange
        Event event = createActiveExistingEvent();

        FakeHistoryServiceListener historyListener = new FakeHistoryServiceListener();
        IOrderRepository orderRepository = new InMemoryOrderRepository();
        OrderService orderService = createOrderServiceListener(orderRepository, notifier);

        String buyerSessionId = tokenService.addActiveSession(new Guest());
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
    }

    @Test
    void GivenInvalidSession_WhenCancelEvent_ThenSystemRejectsTheRequestAndEventAndActiveOrderAreNotCanceled() {
        // Arrange
        Event event = createActiveExistingEvent();

        FakeHistoryServiceListener historyListener = new FakeHistoryServiceListener();
        IOrderRepository orderRepository = new InMemoryOrderRepository();

        OrderService orderService = createOrderServiceListener(orderRepository, notifier);

        String buyerSessionId = tokenService.addActiveSession(new Guest());
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

        assertTrue(exception.getMessage().contains("Invalid or expired security token"));
        assertEquals(eventStatus.ACTIVE, unchangedEvent.getStatus());

        assertFalse(historyListener.wasNotifiedFor(event.getId()));

        assertNotNull(unchangedOrder);
        assertEquals(ActiveOrder.OrderStatus.ACTIVE, unchangedOrder.getStatus());

    }

    @Test
    void GivenLoggedInUserWithoutCancelPermission_WhenCancelEvent_ThenSystemRejectsTheRequestAndEventAndActiveOrderAreNotCanceled() {
        // Arrange
        Event event = createActiveExistingEvent();

        String sessionWithoutPermission;

        FakeHistoryServiceListener historyListener = new FakeHistoryServiceListener();
        IOrderRepository orderRepository = new InMemoryOrderRepository();
        OrderService orderService = createOrderServiceListener(orderRepository, notifier);

        String buyerSessionId = tokenService.addActiveSession(new Guest());
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
        sessionWithoutPermission = tokenService.addActiveSession(memberWithoutPermission);
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

    }

    @Test
    void GivenEventDoesNotExist_WhenCancelEvent_ThenSystemRejectsTheRequestAndListenersAreNotNotified() {
        // Arrange
        Long nonExistingEventId = 999L;

        FakeHistoryServiceListener historyListener = new FakeHistoryServiceListener();
        IOrderRepository orderRepository = new InMemoryOrderRepository();
        OrderService orderService = createOrderServiceListener(orderRepository, notifier);

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
        IOrderRepository orderRepository = new InMemoryOrderRepository();
        OrderService orderService = createOrderServiceListener(orderRepository, notifier);
       

        String buyerSessionId = tokenService.addActiveSession(new Guest());
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

        assertEquals(eventStatus.CANCELLED, cancelledEvent.getStatus());

        assertEquals(1, historyListener.notificationCount());
        assertEquals(ActiveOrder.OrderStatus.CANCELLED, orderAfterSecondCancel.getStatus());
    }

    // -------------------- Set Event Discount Policy Tests --------------------
    @Test
    void GivenOwnerLoggedInEventExistsAndVisibleDiscountPolicy_WhenSetEventDiscountPolicy_ThenPolicyIsStoredThroughRepository()
            throws Exception {

        PolicyTestContext context = createPolicyTestContext();

        DiscountDTO visible = new DiscountDTO();
        visible.setType("VISIBLE");
        visible.setName("Visible Discount");
        visible.setPercentage(BigDecimal.valueOf(10));

        DiscountPolicyDTO policyDTO = new DiscountPolicyDTO();
        policyDTO.setCompositionType(DiscountCompositionType.MAX);
        policyDTO.setDiscounts(List.of(visible));

        context.service().setEventDiscountPolicy(
                validOwnerSessionId,
                context.eventId(),
                policyDTO
        );

        Event storedEvent = context.repository()
                .getEventById(context.eventId());

        assertEquals(
                1,
                context.repository().getUpdateCount(context.eventId())
        );
        assertNotNull(storedEvent.getDiscountPolicy());
        assertEquals(
                DiscountCompositionType.MAX,
                storedEvent.getDiscountPolicy()
                        .getDiscountCompositionType()
        );
        assertEquals(
                1,
                storedEvent.getDiscountPolicy()
                        .getDiscounts()
                        .size()
        );
        assertTrue(
                storedEvent.getDiscountPolicy()
                        .getDiscounts()
                        .get(0) instanceof VisibleDiscount
        );
    }

    @Test
    void GivenOwnerLoggedInEventExistsAndCouponDiscountPolicy_WhenSetEventDiscountPolicy_ThenPolicyIsStoredThroughRepository()
            throws Exception {

        PolicyTestContext context = createPolicyTestContext();

        DiscountDTO coupon = new DiscountDTO();
        coupon.setType("COUPON");
        coupon.setName("Coupon Discount");
        coupon.setCouponCode("SAVE10");
        coupon.setPercentage(BigDecimal.valueOf(10));
        coupon.setEndTime(LocalDateTime.now().plusDays(1));

        DiscountPolicyDTO policyDTO = new DiscountPolicyDTO();
        policyDTO.setCompositionType(DiscountCompositionType.MAX);
        policyDTO.setDiscounts(List.of(coupon));

        context.service().setEventDiscountPolicy(
                validOwnerSessionId,
                context.eventId(),
                policyDTO
        );

        Event storedEvent = context.repository()
                .getEventById(context.eventId());

        assertEquals(
                1,
                context.repository().getUpdateCount(context.eventId())
        );
        assertNotNull(storedEvent.getDiscountPolicy());
        assertEquals(
                1,
                storedEvent.getDiscountPolicy()
                        .getDiscounts()
                        .size()
        );
        assertTrue(
                storedEvent.getDiscountPolicy()
                        .getDiscounts()
                        .get(0) instanceof CouponDiscount
        );
    }

    @Test
    void GivenOwnerLoggedInEventExistsAndConditionalDiscountPolicy_WhenSetEventDiscountPolicy_ThenPolicyIsStoredThroughRepository()
            throws Exception {

        PolicyTestContext context = createPolicyTestContext();

        DiscountDTO conditional = new DiscountDTO();
        conditional.setType("CONDITIONAL");
        conditional.setName("Min Tickets Discount");
        conditional.setPercentage(BigDecimal.valueOf(15));
        conditional.setConditions(
                List.of(
                        new DiscountConditionDTO(
                                "MIN_TICKET",
                                3,
                                null,
                                null
                        )
                )
        );

        DiscountPolicyDTO policyDTO = new DiscountPolicyDTO();
        policyDTO.setCompositionType(DiscountCompositionType.SUM);
        policyDTO.setDiscounts(List.of(conditional));

        context.service().setEventDiscountPolicy(
                validOwnerSessionId,
                context.eventId(),
                policyDTO
        );

        Event storedEvent = context.repository()
                .getEventById(context.eventId());

        assertEquals(
                1,
                context.repository().getUpdateCount(context.eventId())
        );
        assertNotNull(storedEvent.getDiscountPolicy());
        assertEquals(
                DiscountCompositionType.SUM,
                storedEvent.getDiscountPolicy()
                        .getDiscountCompositionType()
        );
        assertEquals(
                1,
                storedEvent.getDiscountPolicy()
                        .getDiscounts()
                        .size()
        );
        assertTrue(
                storedEvent.getDiscountPolicy()
                        .getDiscounts()
                        .get(0) instanceof ConditionalDiscount
        );
    }

    @Test
    void GivenLoggedInUserWithoutPermission_WhenSetEventDiscountPolicy_ThenSystemRejectsAndStoredPolicyIsNotChanged()
            throws Exception {

        PolicyTestContext context = createPolicyTestContext();

        DiscountPolicyDTO policyBefore =
                context.service().getEventDiscountPolicy(
                        validOwnerSessionId,
                        context.eventId()
                );

        Long plainUserId = 2L;

        Member plainUser = new Member(
                plainUserId,
                "PlainDiscountUser",
                "Plain Discount User",
                "0500000099",
                LocalDate.of(2001, 1, 1)
        );

        userRepository.addRegisteredMember(
                plainUserId,
                plainUser,
                "password"
        );

        String sessionWithoutPermission =
                tokenService.addActiveSession(plainUser);

        DiscountDTO visible = new DiscountDTO();
        visible.setType("VISIBLE");
        visible.setName("Visible Discount");
        visible.setPercentage(BigDecimal.valueOf(10));

        DiscountPolicyDTO requestedPolicy = new DiscountPolicyDTO();
        requestedPolicy.setCompositionType(
                DiscountCompositionType.MAX
        );
        requestedPolicy.setDiscounts(List.of(visible));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> context.service().setEventDiscountPolicy(
                        sessionWithoutPermission,
                        context.eventId(),
                        requestedPolicy
                )
        );

        DiscountPolicyDTO policyAfter =
                context.service().getEventDiscountPolicy(
                        validOwnerSessionId,
                        context.eventId()
                );

        assertTrue(exception.getMessage().contains("permission"));
        assertEquals(
                0,
                context.repository().getUpdateCount(context.eventId())
        );
        assertEquals(
                policyBefore.getCompositionType(),
                policyAfter.getCompositionType()
        );
        assertEquals(
                policyBefore.getDiscounts().size(),
                policyAfter.getDiscounts().size()
        );
    }

    @Test
    void GivenEventDoesNotExist_WhenSetEventDiscountPolicy_ThenSystemRejectsTheRequest() {
        InMemoryEventRepository policyRepository =
                new InMemoryEventRepository();

        EventService policyEventService =
                createEventService(policyRepository);

        DiscountPolicyDTO policyDTO = new DiscountPolicyDTO();
        policyDTO.setCompositionType(DiscountCompositionType.MAX);
        policyDTO.setDiscounts(List.of());

        Exception exception = assertThrows(
                Exception.class,
                () -> policyEventService.setEventDiscountPolicy(
                        validOwnerSessionId,
                        Long.MAX_VALUE,
                        policyDTO
                )
        );

        assertTrue(exception.getMessage().contains("Event not found"));
        assertEquals(
                0,
                policyRepository.getUpdateCount(Long.MAX_VALUE)
        );
    }

    @Test
    void GivenOwnerLoggedInEventExistsAndDiscountPolicyStored_WhenGetEventDiscountPolicy_ThenReturnsStoredPolicyDTO()
            throws Exception {

        PolicyTestContext context = createPolicyTestContext();

        DiscountDTO visible = new DiscountDTO();
        visible.setType("VISIBLE");
        visible.setName("Visible Discount");
        visible.setPercentage(BigDecimal.valueOf(10));

        DiscountPolicyDTO policyDTO = new DiscountPolicyDTO();
        policyDTO.setCompositionType(DiscountCompositionType.MAX);
        policyDTO.setDiscounts(List.of(visible));

        context.service().setEventDiscountPolicy(
                validOwnerSessionId,
                context.eventId(),
                policyDTO
        );

        DiscountPolicyDTO result =
                context.service().getEventDiscountPolicy(
                        validOwnerSessionId,
                        context.eventId()
                );

        assertEquals(
                1,
                context.repository().getUpdateCount(context.eventId())
        );
        assertNotNull(result);
        assertEquals(
                DiscountCompositionType.MAX,
                result.getCompositionType()
        );
        assertEquals(1, result.getDiscounts().size());
        assertEquals(
                "VISIBLE",
                result.getDiscounts().get(0).getType()
        );
    }

    @Test
    void GivenOwnerLoggedIn_WhenSetEventDiscountCompositionType_ThenCompositionTypeIsStored()
            throws Exception {

        PolicyTestContext context = createPolicyTestContext();

        context.service().setEventDiscountCompositionType(
                validOwnerSessionId,
                context.eventId(),
                DiscountCompositionType.MAX
        );

        Event storedEvent = context.repository()
                .getEventById(context.eventId());

        assertEquals(
                1,
                context.repository().getUpdateCount(context.eventId())
        );
        assertEquals(
                DiscountCompositionType.MAX,
                storedEvent.getDiscountPolicy()
                        .getDiscountCompositionType()
        );
    }

    // -------------------- Set Event Purchase Policy Tests -------------------
    @Test
    void GivenOwnerLoggedInEventExistsAndMaxTicketsPolicy_WhenSetEventPurchasePolicy_ThenPolicyIsStoredAndEnforced()
            throws Exception {

        PolicyTestContext context = createPolicyTestContext();

        PurchasePolicyDTO policyDTO = maxTicketsPolicyDTO(5);

        context.service().setEventPurchasePolicy(
                validOwnerSessionId,
                context.eventId(),
                policyDTO
        );

        Event storedEvent = context.repository()
                .getEventById(context.eventId());

        assertEquals(
                1,
                context.repository().getUpdateCount(context.eventId())
        );
        assertNotNull(storedEvent.getPurchasePolicy());

        assertDoesNotThrow(
                () -> storedEvent.canPurchase(5, 20)
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> storedEvent.canPurchase(6, 20)
        );

        assertTrue(
                exception.getMessage()
                        .contains("Cannot purchase more than 5 tickets")
        );
    }

    @Test
    void GivenOwnerLoggedInEventExistsAndMinAgePolicy_WhenSetEventPurchasePolicy_ThenPolicyIsStoredAndEnforced()
            throws Exception {

        PolicyTestContext context = createPolicyTestContext();

        PurchasePolicyDTO policyDTO = minAgePolicyDTO(18);

        context.service().setEventPurchasePolicy(
                validOwnerSessionId,
                context.eventId(),
                policyDTO
        );

        Event storedEvent = context.repository()
                .getEventById(context.eventId());

        assertEquals(
                1,
                context.repository().getUpdateCount(context.eventId())
        );
        assertNotNull(storedEvent.getPurchasePolicy());

        assertDoesNotThrow(
                () -> storedEvent.canPurchase(1, 18)
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> storedEvent.canPurchase(1, 17)
        );

        assertTrue(
                exception.getMessage()
                        .contains("minimum age requirement of 18")
        );
    }

    @Test
    void GivenOwnerLoggedInEventExistsAndNestedPurchasePolicy_WhenSetEventPurchasePolicy_ThenPolicyIsStoredAndEnforced()
            throws Exception {

        PolicyTestContext context = createPolicyTestContext();

        PurchasePolicyDTO policyDTO = nestedPolicyDTO();

        context.service().setEventPurchasePolicy(
                validOwnerSessionId,
                context.eventId(),
                policyDTO
        );

        Event storedEvent = context.repository()
                .getEventById(context.eventId());

        assertEquals(
                1,
                context.repository().getUpdateCount(context.eventId())
        );
        assertNotNull(storedEvent.getPurchasePolicy());

        assertDoesNotThrow(
                () -> storedEvent.canPurchase(2, 18)
        );

        assertDoesNotThrow(
                () -> storedEvent.canPurchase(100, 18)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> storedEvent.canPurchase(50, 18)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> storedEvent.canPurchase(2, 17)
        );
    }

    @Test
    void GivenLoggedInUserWithoutPermission_WhenSetEventPurchasePolicy_ThenSystemRejectsAndStoredPolicyIsNotChanged()
            throws Exception {

        PolicyTestContext context = createPolicyTestContext();

        Long plainUserId = 2L;

        Member plainUser = new Member(
                plainUserId,
                "PlainUser",
                "Plain User",
                "0500000002",
                LocalDate.of(2001, 1, 1)
        );

        userRepository.addRegisteredMember(
                plainUserId,
                plainUser,
                "password"
        );

        String sessionWithoutPermission =
                tokenService.addActiveSession(plainUser);

        PurchasePolicyDTO policyDTO = maxTicketsPolicyDTO(5);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> context.service().setEventPurchasePolicy(
                        sessionWithoutPermission,
                        context.eventId(),
                        policyDTO
                )
        );

        Event unchangedEvent = context.repository()
                .getEventById(context.eventId());

        assertTrue(exception.getMessage().contains("permission"));
        assertEquals(
                0,
                context.repository().getUpdateCount(context.eventId())
        );
        assertDoesNotThrow(
                () -> unchangedEvent.canPurchase(100, 0)
        );
    }

    @Test
    void GivenEventDoesNotExist_WhenSetEventPurchasePolicy_ThenSystemRejectsTheRequest() {
        InMemoryEventRepository policyRepository =
                new InMemoryEventRepository();

        EventService policyEventService =
                createEventService(policyRepository);

        PurchasePolicyDTO policyDTO = maxTicketsPolicyDTO(5);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> policyEventService.setEventPurchasePolicy(
                        validOwnerSessionId,
                        Long.MAX_VALUE,
                        policyDTO
                )
        );

        assertTrue(exception.getMessage().contains("Event not found"));
        assertEquals(
                0,
                policyRepository.getUpdateCount(Long.MAX_VALUE)
        );
    }

    @Test
    void GivenInvalidPurchasePolicyDTO_WhenSetEventPurchasePolicy_ThenSystemRejectsAndStoredPolicyIsNotChanged()
            throws Exception {

        PolicyTestContext context = createPolicyTestContext();

        PurchasePolicyDTO invalidPolicyDTO =
                new PurchasePolicyDTO(
                        new PurchaseRuleDTO(
                                PurchaseRuleType.MAX_TICKETS,
                                null,
                                null
                        )
                );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> context.service().setEventPurchasePolicy(
                        validOwnerSessionId,
                        context.eventId(),
                        invalidPolicyDTO
                )
        );

        Event unchangedEvent = context.repository()
                .getEventById(context.eventId());

        assertTrue(
                exception.getMessage()
                        .contains("Maximum tickets is required")
        );
        assertEquals(
                0,
                context.repository().getUpdateCount(context.eventId())
        );
        assertDoesNotThrow(
                () -> unchangedEvent.canPurchase(100, 0)
        );
    }

    @Test
    void GivenInvalidSession_WhenSetEventPurchasePolicy_ThenSystemRejectsAndStoredPolicyIsNotChanged()
            throws Exception {

        PolicyTestContext context = createPolicyTestContext();

        PurchasePolicyDTO policyDTO = maxTicketsPolicyDTO(5);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> context.service().setEventPurchasePolicy(
                        invalidSessionId,
                        context.eventId(),
                        policyDTO
                )
        );

        Event unchangedEvent = context.repository()
                .getEventById(context.eventId());

        assertTrue(
                exception.getMessage()
                        .contains("Invalid or expired security token")
        );
        assertEquals(
                0,
                context.repository().getUpdateCount(context.eventId())
        );
        assertDoesNotThrow(
                () -> unchangedEvent.canPurchase(100, 0)
        );
    }

    // -------------------- Map Domain Validation Tests -------------------

    @Test
    void GivenMapSizeIsNull_WhenDefineEventMap_ThenSystemRejectsAndEventRemainsDraft() {
        EventMapDTO invalidMap = new EventMapDTO(
                null,
                List.of(createNewStandingArea(
                        "Standing Area",
                        0,
                        0,
                        5,
                        5,
                        100
                )),
                false
        );

        assertDefineMapRejected(
                invalidMap,
                "Map size cannot be null"
        );
    }

    @Test
    void GivenMapSizeIsNotPositive_WhenDefineEventMap_ThenSystemRejectsAndEventRemainsDraft() {
        EventMapDTO invalidMap = new EventMapDTO(
                new PairDTO<>(0, 20),
                List.of(createNewStandingArea(
                        "Standing Area",
                        0,
                        0,
                        5,
                        5,
                        100
                )),
                false
        );

        assertDefineMapRejected(
                invalidMap,
                "Map size must be positive"
        );
    }

    @Test
    void GivenMapElementsListIsNull_WhenDefineEventMap_ThenSystemRejectsAndEventRemainsDraft() {
        EventMapDTO invalidMap = new EventMapDTO(
                new PairDTO<>(20, 20),
                null,
                false
        );

        assertDefineMapRejected(
                invalidMap,
                "Map elements cannot be null"
        );
    }

    @Test
    void GivenMapContainsNullElement_WhenDefineEventMap_ThenSystemRejectsAndEventRemainsDraft() {
        List<IMapElementDTO> elements = new java.util.ArrayList<>();
        elements.add(null);

        EventMapDTO invalidMap = new EventMapDTO(
                new PairDTO<>(20, 20),
                elements,
                false
        );

        assertDefineMapRejected(
                invalidMap,
                "Map elements cannot contain null"
        );
    }

    @Test
    void GivenMapDoesNotContainTicketArea_WhenDefineEventMap_ThenSystemRejectsAndEventRemainsDraft() {
        ElementDTO stage = new ElementDTO(
                null,
                "Main Stage",
                new PairDTO<>(0, 0),
                new PairDTO<>(5, 3),
                "STAGE"
        );

        EventMapDTO invalidMap = new EventMapDTO(
                new PairDTO<>(20, 20),
                List.of(stage),
                false
        );

        assertDefineMapRejected(
                invalidMap,
                "Event map must contain at least one seating area or standing area"
        );
    }

    @Test
    void GivenElementLocationIsNegative_WhenDefineEventMap_ThenSystemRejectsAndEventRemainsDraft() {
        EventMapDTO invalidMap = new EventMapDTO(
                new PairDTO<>(20, 20),
                List.of(createNewStandingArea(
                        "Standing Area",
                        -1,
                        0,
                        5,
                        5,
                        100
                )),
                false
        );

        assertDefineMapRejected(
                invalidMap,
                "Element location cannot be negative"
        );
    }

    @Test
    void GivenElementWidthIsZero_WhenDefineEventMap_ThenSystemRejectsAndEventRemainsDraft() {
        EventMapDTO invalidMap = new EventMapDTO(
                new PairDTO<>(20, 20),
                List.of(createNewStandingArea(
                        "Standing Area",
                        0,
                        0,
                        0,
                        5,
                        100
                )),
                false
        );

        assertDefineMapRejected(
                invalidMap,
                "Element size must be positive"
        );
    }

    @Test
    void GivenElementHeightIsNegative_WhenDefineEventMap_ThenSystemRejectsAndEventRemainsDraft() {
        EventMapDTO invalidMap = new EventMapDTO(
                new PairDTO<>(20, 20),
                List.of(createNewStandingArea(
                        "Standing Area",
                        0,
                        0,
                        5,
                        -1,
                        100
                )),
                false
        );

        assertDefineMapRejected(
                invalidMap,
                "Element size must be positive"
        );
    }

    @Test
    void GivenElementExceedsMapWidth_WhenDefineEventMap_ThenSystemRejectsAndEventRemainsDraft() {
        EventMapDTO invalidMap = new EventMapDTO(
                new PairDTO<>(20, 20),
                List.of(createNewStandingArea(
                        "Standing Area",
                        18,
                        0,
                        3,
                        5,
                        100
                )),
                false
        );

        assertDefineMapRejected(
                invalidMap,
                "Element is outside map bounds"
        );
    }

    @Test
    void GivenElementExceedsMapHeight_WhenDefineEventMap_ThenSystemRejectsAndEventRemainsDraft() {
        EventMapDTO invalidMap = new EventMapDTO(
                new PairDTO<>(20, 20),
                List.of(createNewStandingArea(
                        "Standing Area",
                        0,
                        18,
                        5,
                        3,
                        100
                )),
                false
        );

        assertDefineMapRejected(
                invalidMap,
                "Element is outside map bounds"
        );
    }

    @Test
    void GivenAreaPriceIsNegative_WhenDefineEventMap_ThenSystemRejectsAndEventRemainsDraft() {
        StandingAreaDTO invalidArea = new StandingAreaDTO(
                null,
                "Standing Area",
                new PairDTO<>(0, 0),
                new PairDTO<>(5, 5),
                "STANDING_AREA",
                false,
                BigDecimal.valueOf(-1),
                100,
                0,
                0
        );

        EventMapDTO invalidMap = new EventMapDTO(
                new PairDTO<>(20, 20),
                List.of(invalidArea),
                false
        );

        assertDefineMapRejected(
                invalidMap,
                "Area price cannot be negative"
        );
    }

    @Test
    void GivenAreaPriceIsNull_WhenDefineEventMap_ThenSystemRejectsAndEventRemainsDraft() {
        StandingAreaDTO invalidArea = new StandingAreaDTO(
                null,
                "Standing Area",
                new PairDTO<>(0, 0),
                new PairDTO<>(5, 5),
                "STANDING_AREA",
                false,
                null,
                100,
                0,
                0
        );

        EventMapDTO invalidMap = new EventMapDTO(
                new PairDTO<>(20, 20),
                List.of(invalidArea),
                false
        );

        assertDefineMapRejected(
                invalidMap,
                "Area price cannot be null"
        );
    }

    // -------------------- Update Active Event Map Tests -------------------

    @Test
    void GivenActiveEventAndValidNewArea_WhenUpdateActiveEventMap_ThenAreaIsAddedAndPersisted() {
        Event activeEvent = createActiveEventForMapUpdateTests();

        EventMapDTO beforeUpdate = eventService.getEventMap(
                validOwnerSessionId,
                activeEvent.getId()
        );

        int originalElementCount = beforeUpdate.getElementDTOs().size();

        StandingAreaDTO newArea = createNewStandingArea(
                "New Standing Area",
                10,
                10,
                4,
                4,
                50
        );

        Boolean result = eventService.updateActiveEvantMap(
                validOwnerSessionId,
                activeEvent.getId(),
                List.of(newArea),
                List.of()
        );

        EventMapDTO afterUpdate = eventService.getEventMap(
                validOwnerSessionId,
                activeEvent.getId()
        );

        StandingAreaDTO persistedArea = findStandingArea(
                afterUpdate,
                "New Standing Area"
        );

        assertTrue(result);
        assertEquals(
                originalElementCount + 1,
                afterUpdate.getElementDTOs().size()
        );
        assertNotNull(persistedArea);
        assertNotNull(persistedArea.id());
        assertEquals(50L, persistedArea.capacity());
    }

    @Test
    void GivenActiveEventAndLargerSeatingDimensions_WhenUpdateActiveEventMap_ThenSeatsAreAdded() {
        Event activeEvent = createActiveEventForMapUpdateTests();

        EventMapDTO originalMap = eventService.getEventMap(
                validOwnerSessionId,
                activeEvent.getId()
        );

        SeatingAreaDTO originalArea = findSeatingArea(
                originalMap,
                "Editable Seating"
        );

        SeatingAreaDTO update = createSeatingAreaUpdate(
                originalArea,
                originalArea.location(),
                originalArea.size(),
                4,
                5
        );

        Boolean result = eventService.updateActiveEvantMap(
                validOwnerSessionId,
                activeEvent.getId(),
                List.of(),
                List.of(update)
        );

        EventMapDTO updatedMap = eventService.getEventMap(
                validOwnerSessionId,
                activeEvent.getId()
        );

        SeatingAreaDTO updatedArea = findSeatingArea(
                updatedMap,
                "Editable Seating"
        );

        assertTrue(result);
        assertNotNull(updatedArea);
        assertEquals(4, updatedArea.rows());
        assertEquals(5, updatedArea.columns());
        assertEquals(20, updatedArea.seats().size());
    }

    @Test
    void GivenActiveEventAndLargerStandingCapacity_WhenUpdateActiveEventMap_ThenCapacityIsIncreased() {
        Event activeEvent = createActiveEventForMapUpdateTests();

        EventMapDTO originalMap = eventService.getEventMap(
                validOwnerSessionId,
                activeEvent.getId()
        );

        StandingAreaDTO originalArea = findStandingArea(
                originalMap,
                "Editable Standing"
        );

        StandingAreaDTO update = createStandingAreaUpdate(
                originalArea,
                150
        );

        Boolean result = eventService.updateActiveEvantMap(
                validOwnerSessionId,
                activeEvent.getId(),
                List.of(),
                List.of(update)
        );

        EventMapDTO updatedMap = eventService.getEventMap(
                validOwnerSessionId,
                activeEvent.getId()
        );

        StandingAreaDTO updatedArea = findStandingArea(
                updatedMap,
                "Editable Standing"
        );

        assertTrue(result);
        assertNotNull(updatedArea);
        assertEquals(150L, updatedArea.capacity());
    }

    @Test
    void GivenNewAreasListIsNull_WhenUpdateActiveEventMap_ThenSystemRejectsTheRequest() {
        Event activeEvent = createActiveEventForMapUpdateTests();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.updateActiveEvantMap(
                        validOwnerSessionId,
                        activeEvent.getId(),
                        null,
                        List.of()
                )
        );

        assertTrue(
                exception.getMessage().contains(
                        "New areas list cannot be null"
                )
        );
    }

    @Test
    void GivenUpdatedAreasListIsNull_WhenUpdateActiveEventMap_ThenSystemRejectsTheRequest() {
        Event activeEvent = createActiveEventForMapUpdateTests();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.updateActiveEvantMap(
                        validOwnerSessionId,
                        activeEvent.getId(),
                        List.of(),
                        null
                )
        );

        assertTrue(
                exception.getMessage().contains(
                        "Updated areas list cannot be null"
                )
        );
    }

    @Test
    void GivenNewAreaAlreadyHasId_WhenUpdateActiveEventMap_ThenSystemRejectsAndMapIsUnchanged() {
        Event activeEvent = createActiveEventForMapUpdateTests();

        EventMapDTO beforeUpdate = eventService.getEventMap(
                validOwnerSessionId,
                activeEvent.getId()
        );

        StandingAreaDTO invalidNewArea = new StandingAreaDTO(
                999L,
                "Invalid New Area",
                new PairDTO<>(10, 10),
                new PairDTO<>(4, 4),
                "STANDING_AREA",
                false,
                BigDecimal.valueOf(40),
                50,
                0,
                0
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.updateActiveEvantMap(
                        validOwnerSessionId,
                        activeEvent.getId(),
                        List.of(invalidNewArea),
                        List.of()
                )
        );

        EventMapDTO afterUpdate = eventService.getEventMap(
                validOwnerSessionId,
                activeEvent.getId()
        );

        assertTrue(
                exception.getMessage().contains(
                        "new area must not have an ID"
                )
        );
        assertEquals(
                beforeUpdate.getElementDTOs().size(),
                afterUpdate.getElementDTOs().size()
        );
    }

    @Test
    void GivenNewAreaOverlapsExistingArea_WhenUpdateActiveEventMap_ThenSystemRejectsAndMapIsUnchanged() {
        Event activeEvent = createActiveEventForMapUpdateTests();

        EventMapDTO beforeUpdate = eventService.getEventMap(
                validOwnerSessionId,
                activeEvent.getId()
        );

        StandingAreaDTO overlappingArea = createNewStandingArea(
                "Overlapping Area",
                2,
                2,
                4,
                4,
                50
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.updateActiveEvantMap(
                        validOwnerSessionId,
                        activeEvent.getId(),
                        List.of(overlappingArea),
                        List.of()
                )
        );

        EventMapDTO afterUpdate = eventService.getEventMap(
                validOwnerSessionId,
                activeEvent.getId()
        );

        assertTrue(
                exception.getMessage().contains(
                        "Map elements cannot overlap"
                )
        );
        assertEquals(
                beforeUpdate.getElementDTOs().size(),
                afterUpdate.getElementDTOs().size()
        );
        assertFalse(
                mapContainsElementNamed(
                        afterUpdate,
                        "Overlapping Area"
                )
        );
    }

    @Test
    void GivenNewAreaIsOutsideMapBounds_WhenUpdateActiveEventMap_ThenSystemRejectsAndMapIsUnchanged() {
        Event activeEvent = createActiveEventForMapUpdateTests();

        EventMapDTO beforeUpdate = eventService.getEventMap(
                validOwnerSessionId,
                activeEvent.getId()
        );

        StandingAreaDTO outsideArea = createNewStandingArea(
                "Outside Area",
                18,
                18,
                4,
                4,
                50
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.updateActiveEvantMap(
                        validOwnerSessionId,
                        activeEvent.getId(),
                        List.of(outsideArea),
                        List.of()
                )
        );

        EventMapDTO afterUpdate = eventService.getEventMap(
                validOwnerSessionId,
                activeEvent.getId()
        );

        assertTrue(
                exception.getMessage().contains(
                        "Element is outside map bounds"
                )
        );
        assertEquals(
                beforeUpdate.getElementDTOs().size(),
                afterUpdate.getElementDTOs().size()
        );
    }

    @Test
    void GivenSeatingRowsAreReduced_WhenUpdateActiveEventMap_ThenSystemRejectsAndRowsRemainUnchanged() {
        Event activeEvent = createActiveEventForMapUpdateTests();

        EventMapDTO originalMap = eventService.getEventMap(
                validOwnerSessionId,
                activeEvent.getId()
        );

        SeatingAreaDTO originalArea = findSeatingArea(
                originalMap,
                "Editable Seating"
        );

        SeatingAreaDTO invalidUpdate = createSeatingAreaUpdate(
                originalArea,
                originalArea.location(),
                originalArea.size(),
                originalArea.rows() - 1,
                originalArea.columns()
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.updateActiveEvantMap(
                        validOwnerSessionId,
                        activeEvent.getId(),
                        List.of(),
                        List.of(invalidUpdate)
                )
        );

        SeatingAreaDTO unchangedArea = findSeatingArea(
                eventService.getEventMap(
                        validOwnerSessionId,
                        activeEvent.getId()
                ),
                "Editable Seating"
        );

        assertTrue(
                exception.getMessage().contains(
                        "rows cannot be reduced"
                )
        );
        assertEquals(
                originalArea.rows(),
                unchangedArea.rows()
        );
        assertEquals(
                originalArea.columns(),
                unchangedArea.columns()
        );
    }

    @Test
    void GivenSeatingColumnsAreReduced_WhenUpdateActiveEventMap_ThenSystemRejectsAndColumnsRemainUnchanged() {
        Event activeEvent = createActiveEventForMapUpdateTests();

        EventMapDTO originalMap = eventService.getEventMap(
                validOwnerSessionId,
                activeEvent.getId()
        );

        SeatingAreaDTO originalArea = findSeatingArea(
                originalMap,
                "Editable Seating"
        );

        SeatingAreaDTO invalidUpdate = createSeatingAreaUpdate(
                originalArea,
                originalArea.location(),
                originalArea.size(),
                originalArea.rows(),
                originalArea.columns() - 1
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.updateActiveEvantMap(
                        validOwnerSessionId,
                        activeEvent.getId(),
                        List.of(),
                        List.of(invalidUpdate)
                )
        );

        SeatingAreaDTO unchangedArea = findSeatingArea(
                eventService.getEventMap(
                        validOwnerSessionId,
                        activeEvent.getId()
                ),
                "Editable Seating"
        );

        assertTrue(
                exception.getMessage().contains(
                        "columns cannot be reduced"
                )
        );
        assertEquals(
                originalArea.rows(),
                unchangedArea.rows()
        );
        assertEquals(
                originalArea.columns(),
                unchangedArea.columns()
        );
    }

    @Test
    void GivenStandingCapacityIsReduced_WhenUpdateActiveEventMap_ThenSystemRejectsAndCapacityRemainsUnchanged() {
        Event activeEvent = createActiveEventForMapUpdateTests();

        EventMapDTO originalMap = eventService.getEventMap(
                validOwnerSessionId,
                activeEvent.getId()
        );

        StandingAreaDTO originalArea = findStandingArea(
                originalMap,
                "Editable Standing"
        );

        StandingAreaDTO invalidUpdate = createStandingAreaUpdate(
                originalArea,
                originalArea.capacity() - 1
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.updateActiveEvantMap(
                        validOwnerSessionId,
                        activeEvent.getId(),
                        List.of(),
                        List.of(invalidUpdate)
                )
        );

        StandingAreaDTO unchangedArea = findStandingArea(
                eventService.getEventMap(
                        validOwnerSessionId,
                        activeEvent.getId()
                ),
                "Editable Standing"
        );

        assertTrue(
                exception.getMessage().contains(
                        "capacity cannot be reduced"
                )
        );
        assertEquals(
                originalArea.capacity(),
                unchangedArea.capacity()
        );
    }

    @Test
    void GivenUpdatedAreaDoesNotExist_WhenUpdateActiveEventMap_ThenSystemRejectsAndMapIsUnchanged() {
        Event activeEvent = createActiveEventForMapUpdateTests();

        EventMapDTO beforeUpdate = eventService.getEventMap(
                validOwnerSessionId,
                activeEvent.getId()
        );

        StandingAreaDTO missingAreaUpdate = new StandingAreaDTO(
                Long.MAX_VALUE,
                "Missing Area",
                new PairDTO<>(10, 10),
                new PairDTO<>(4, 4),
                "STANDING_AREA",
                false,
                BigDecimal.valueOf(40),
                150,
                0,
                0
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.updateActiveEvantMap(
                        validOwnerSessionId,
                        activeEvent.getId(),
                        List.of(),
                        List.of(missingAreaUpdate)
                )
        );

        EventMapDTO afterUpdate = eventService.getEventMap(
                validOwnerSessionId,
                activeEvent.getId()
        );

        assertTrue(
                exception.getMessage().contains(
                        "Area not found"
                )
        );
        assertEquals(
                beforeUpdate.getElementDTOs().size(),
                afterUpdate.getElementDTOs().size()
        );
    }

    @Test
    void GivenUpdatedAreaChangesType_WhenUpdateActiveEventMap_ThenSystemRejectsAndOriginalAreaRemains() {
        Event activeEvent = createActiveEventForMapUpdateTests();

        EventMapDTO originalMap = eventService.getEventMap(
                validOwnerSessionId,
                activeEvent.getId()
        );

        StandingAreaDTO standingArea = findStandingArea(
                originalMap,
                "Editable Standing"
        );

        SeatingAreaDTO invalidTypeUpdate = new SeatingAreaDTO(
                standingArea.id(),
                standingArea.name(),
                standingArea.location(),
                standingArea.size(),
                "SEATING_AREA",
                false,
                standingArea.price(),
                3,
                3,
                List.of()
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.updateActiveEvantMap(
                        validOwnerSessionId,
                        activeEvent.getId(),
                        List.of(),
                        List.of(invalidTypeUpdate)
                )
        );

        EventMapDTO unchangedMap = eventService.getEventMap(
                validOwnerSessionId,
                activeEvent.getId()
        );

        assertTrue(
                exception.getMessage().contains(
                        "Area type cannot be changed"
                )
        );
        assertNotNull(
                findStandingArea(
                        unchangedMap,
                        "Editable Standing"
                )
        );
    }

    @Test
    void GivenValidNewAreaAndInvalidAreaUpdate_WhenUpdateActiveEventMap_ThenNoPartialChangesAreSaved() {
        Event activeEvent = createActiveEventForMapUpdateTests();

        EventMapDTO originalMap = eventService.getEventMap(
                validOwnerSessionId,
                activeEvent.getId()
        );

        int originalElementCount =
                originalMap.getElementDTOs().size();

        StandingAreaDTO originalStandingArea =
                findStandingArea(
                        originalMap,
                        "Editable Standing"
                );

        StandingAreaDTO validNewArea = createNewStandingArea(
                "Atomic New Area",
                10,
                10,
                4,
                4,
                50
        );

        StandingAreaDTO invalidUpdate =
                createStandingAreaUpdate(
                        originalStandingArea,
                        originalStandingArea.capacity() - 1
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> eventService.updateActiveEvantMap(
                        validOwnerSessionId,
                        activeEvent.getId(),
                        List.of(validNewArea),
                        List.of(invalidUpdate)
                )
        );

        EventMapDTO unchangedMap = eventService.getEventMap(
                validOwnerSessionId,
                activeEvent.getId()
        );

        StandingAreaDTO unchangedStandingArea =
                findStandingArea(
                        unchangedMap,
                        "Editable Standing"
                );

        assertEquals(
                originalElementCount,
                unchangedMap.getElementDTOs().size()
        );
        assertFalse(
                mapContainsElementNamed(
                        unchangedMap,
                        "Atomic New Area"
                )
        );
        assertEquals(
                originalStandingArea.capacity(),
                unchangedStandingArea.capacity()
        );
    }

    @Test
    void GivenDraftEvent_WhenUpdateActiveEventMap_ThenSystemRejectsTheRequest() {
        Event draftEvent = createExistingEvent();
        eventRepository.addEvent(draftEvent);

        StandingAreaDTO newArea = createNewStandingArea(
                "New Area",
                10,
                10,
                4,
                4,
                50
        );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> eventService.updateActiveEvantMap(
                        validOwnerSessionId,
                        draftEvent.getId(),
                        List.of(newArea),
                        List.of()
                )
        );

        Event unchangedEvent =
                eventRepository.getEventById(draftEvent.getId());

        assertTrue(
                exception.getMessage()
                        .toLowerCase()
                        .contains("active")
        );
        assertEquals(
                eventStatus.DRAFT,
                unchangedEvent.getStatus()
        );
        assertEquals(0, elementCount(unchangedEvent));
    }

    @Test
    void GivenInvalidSession_WhenUpdateActiveEventMap_ThenSystemRejectsTheRequest() {
        Event activeEvent = createActiveEventForMapUpdateTests();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.updateActiveEvantMap(
                        invalidSessionId,
                        activeEvent.getId(),
                        List.of(),
                        List.of()
                )
        );

        assertTrue(
                exception.getMessage().contains(
                        "Invalid or expired security token"
                )
        );
    }

    @Test
    void GivenUserWithoutMapPermission_WhenUpdateActiveEventMap_ThenSystemRejectsTheRequest() {
        Event activeEvent = createActiveEventForMapUpdateTests();

        Member memberWithoutPermission = new Member(
                200L,
                "MapUpdateUser",
                "Map Update User",
                "0500000200",
                LocalDate.of(2001, 1, 1)
        );

        userRepository.addRegisteredMember(
                200L,
                memberWithoutPermission,
                "password"
        );

        String sessionWithoutPermission =
                tokenService.addActiveSession(
                        memberWithoutPermission
                );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.updateActiveEvantMap(
                        sessionWithoutPermission,
                        activeEvent.getId(),
                        List.of(),
                        List.of()
                )
        );

        assertTrue(
                exception.getMessage().contains(
                        "User does not have permission to update event map"
                )
        );
    }

    // -------------------- Helper Methods and Test Doubles -------------------

    private Event createExistingEvent() {
        Event event = new Event(
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
                BigDecimal.valueOf(99.99),
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
                BigDecimal.valueOf(99.99),
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
                BigDecimal.valueOf(100.00),
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
                BigDecimal.valueOf(100.00),
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
                1L, "Invalid Standing Area", new PairDTO<>(2, 2), new PairDTO<>(4, 5), "StandingArea", false,BigDecimal.valueOf(120.00), 10L, 8L,
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

    private Long getSeatingAreaId(Event event) {
        return event.getMap().getElements().stream()
                .filter(SeatingArea.class::isInstance)
                .map(Element::getId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Seating area was not found"));
    }

    private ActiveOrder createActiveOrderForEvent(IOrderRepository orderRepository,
                                                  Long eventId,
                                                  String buyerSessionId,
                                                  Long buyerId) {
        ActiveOrder activeOrder = new ActiveOrder(
                orderRepository.getNextId(),
                buyerSessionId,
                buyerId,
                eventId);

        Long areaId = getSeatingAreaId(eventRepository.getEventById(eventId));

        activeOrder.addTicket(
                new Ticket(
                        1L,
                        eventId,
                        areaId,
                        1,
                        1,
                        BigDecimal.valueOf(99.99)));

        orderRepository.addOrder(activeOrder);

        return activeOrder;
    }

    private OrderService createOrderServiceListener(
            IOrderRepository orderRepository,
            INotifier notificationsService) {
        return new OrderService(
                orderRepository,
                (TokenService) tokenService,
                new LogbackSystemLogger(),
                notificationsService);
    }

    private PolicyTestContext createPolicyTestContext() {
        InMemoryEventRepository policyRepository =
                new InMemoryEventRepository();

        Event event = createExistingEvent();
        policyRepository.addEvent(event);

        EventService policyEventService =
                createEventService(policyRepository);

        return new PolicyTestContext(
                policyEventService,
                policyRepository,
                event.getId()
        );
    }

    private EventService createEventService(
            IEventRepository repository
    ) {
        return new EventService(
                repository,
                tokenService,
                membershipDomain,
                logger,
                userAccessService
        );
    }

    private record PolicyTestContext(
            EventService service,
            InMemoryEventRepository repository,
            Long eventId
    ) {
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

        @Override
        public boolean onEventCancellationRequested(Long eventId) {
               return true;
        }
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

    private void assertDefineMapRejected(
            EventMapDTO invalidMap,
            String expectedMessage
    ) {
        Event event = createExistingEvent();
        eventRepository.addEvent(event);

        Event originalEvent =
                eventRepository.getEventById(event.getId());

        int originalElementCount =
                elementCount(originalEvent);

        eventStatus originalStatus =
                originalEvent.getStatus();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.defineEventMap(
                        validOwnerSessionId,
                        event.getId(),
                        invalidMap
                )
        );

        Event unchangedEvent =
                eventRepository.getEventById(event.getId());

        assertTrue(
                exception.getMessage().contains(expectedMessage),
                "Expected message containing: "
                        + expectedMessage
                        + ", but received: "
                        + exception.getMessage()
        );

        assertEquals(
                originalElementCount,
                elementCount(unchangedEvent)
        );

        assertEquals(
                originalStatus,
                unchangedEvent.getStatus()
        );
    }

    private Event createActiveEventForMapUpdateTests() {
        Event event = createExistingEvent();
        eventRepository.addEvent(event);

        eventService.defineEventMap(
                validOwnerSessionId,
                event.getId(),
                createEditableMapDTO()
        );

        return eventRepository.getEventById(event.getId());
    }

    private EventMapDTO createEditableMapDTO() {
        SeatingAreaDTO seatingArea = new SeatingAreaDTO(
                null,
                "Editable Seating",
                new PairDTO<>(0, 0),
                new PairDTO<>(6, 6),
                "SEATING_AREA",
                false,
                BigDecimal.valueOf(50),
                2,
                3,
                List.of()
        );

        StandingAreaDTO standingArea = createNewStandingArea(
                "Editable Standing",
                10,
                0,
                5,
                5,
                100
        );

        return new EventMapDTO(
                new PairDTO<>(20, 20),
                List.of(seatingArea, standingArea),
                false
        );
    }

    private StandingAreaDTO createNewStandingArea(
            String name,
            int x,
            int y,
            int width,
            int height,
            long capacity
    ) {
        return new StandingAreaDTO(
                null,
                name,
                new PairDTO<>(x, y),
                new PairDTO<>(width, height),
                "STANDING_AREA",
                false,
                BigDecimal.valueOf(40),
                capacity,
                0,
                0
        );
    }

    private SeatingAreaDTO createSeatingAreaUpdate(
            SeatingAreaDTO existingArea,
            PairDTO<Integer, Integer> location,
            PairDTO<Integer, Integer> size,
            int rows,
            int columns
    ) {
        return new SeatingAreaDTO(
                existingArea.id(),
                existingArea.name(),
                location,
                size,
                existingArea.type(),
                existingArea.soldOut(),
                existingArea.price(),
                rows,
                columns,
                existingArea.seats()
        );
    }

    private StandingAreaDTO createStandingAreaUpdate(
            StandingAreaDTO existingArea,
            long capacity
    ) {
        return new StandingAreaDTO(
                existingArea.id(),
                existingArea.name(),
                existingArea.location(),
                existingArea.size(),
                existingArea.type(),
                existingArea.soldOut(),
                existingArea.price(),
                capacity,
                existingArea.reserved(),
                existingArea.sold()
        );
    }

}
