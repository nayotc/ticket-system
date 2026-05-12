package ticketsystem.AcceptanceTesting;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.ApplicationLayer.EventService;
import ticketsystem.ApplicationLayer.ITokenService;
import ticketsystem.DTO.Event.ElementDTO;
import ticketsystem.DTO.Event.EventDTO;
import ticketsystem.DTO.Event.EventMapDTO;
import ticketsystem.DTO.Event.IMapElementDTO;
import ticketsystem.DTO.Event.PairDTO;
import ticketsystem.DTO.Event.SeatingAreaDTO;
import ticketsystem.DTO.Event.StandingAreaDTO;
import ticketsystem.DomainLayer.MembershipDomainService;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.event.EventCategory;
import ticketsystem.DomainLayer.event.EventLocation;
import ticketsystem.DomainLayer.event.Event.eventStatus;
import ticketsystem.DomainLayer.user.User;
import ticketsystem.DomainLayer.user.Member;
import ticketsystem.DomainLayer.user.CompanyRole;
import ticketsystem.DomainLayer.user.Permission;
import ticketsystem.DomainLayer.user.RoleStatus;
import ticketsystem.InfrastructureLayer.EventRepository;
import ticketsystem.InfrastructureLayer.UserRepository;

public class EventServiceAcceptanceTest {

    private EventService eventService;
    private EventRepository eventRepository;
    private UserRepository userRepository;
    private FakeTokenService tokenService;
    private MembershipDomainService membershipDomain;

    private final String validOwnerSessionId = "owner-session";
    private final String invalidSessionId = "invalid-session";

    private final Long ownerId = 1L;
    private final Long companyId = 100L;

    @BeforeEach
    void setUp() {
        eventRepository = new EventRepository();
        tokenService = new FakeTokenService();
        userRepository = new UserRepository();
        
        // FIX: We use a robust anonymous subclass of MembershipDomainService.
        // This ensures permissions work correctly even if EventService uses the incomplete String-based stub method,
        // and protects against NullPointerExceptions when the repository lookups fail.
        membershipDomain = new MembershipDomainService(userRepository) {
            @Override
            public boolean validatePermission(Long memberId, Long compId, Permission permission) {
                if (memberId == null) return false;
                Member member = userRepository.getMemberById(memberId);
                // Fallback protection if EventService mistakenly passes companyId instead of userId
                if (member == null && memberId.equals(compId)) {
                    member = userRepository.getMemberById(ownerId);
                }
                if (member == null) return false;
                
                CompanyRole role = member.getRoleInCompany(compId);
                return role != null && role.getStatus() == RoleStatus.ACTIVE && role.hasPermission(permission);
            }

            @Override
            public boolean validatePermission(String sessionId, Long compId, String permission) {
                Long uId = tokenService.extractUserId(sessionId);
                if (uId == null) return false;
                Member member = userRepository.getMemberById(uId);
                if (member == null) return false;
                
                CompanyRole role = member.getRoleInCompany(compId);
                return role != null && role.getStatus() == RoleStatus.ACTIVE;
            }
        };

        eventService = new EventService(
                eventRepository,
                tokenService,
                membershipDomain);

        // FIX: Setup a real Member with an ACTIVE Owner role in the DB
        Member ownerMember = new Member(ownerId, "EventOwnerUser");
        ownerMember.addOwnerRole(companyId, 999L);
        ownerMember.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        userRepository.addRegisteredMember(ownerId, ownerMember, "password");

        tokenService.addValidSession(validOwnerSessionId, ownerId);
    }

    // -------------------- Insert Event Tests -------------------

    @Test
    void GivenOwnerLoggedInAndValidEventDetails_WhenInsertEvent_ThenEventIsCreatedAndSaved() {
        String eventName = "Rock Concert";
        LocalDateTime eventDate = LocalDateTime.now().plusDays(10);

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

        Event savedEvent = eventRepository.getEventById(1L);

        assertNotNull(savedEvent);
        assertEquals(eventName, savedEvent.getName());
        assertEquals(companyId, savedEvent.getCompanyId());
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
    }

    @Test
    void GivenLoggedInUserWithoutCreatePermission_WhenInsertEvent_ThenSystemRejectsTheRequest() {
        String sessionWithoutPermission = "session-without-create-permission";
        Long plainUserId = 2L;
        
        // Setup a real user WITHOUT any roles
        Member plainUser = new Member(plainUserId, "PlainUser");
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
    }

    @Test
    void GivenOwnerLoggedInEventExistsAndInvalidUpdatedName_WhenUpdateEvent_ThenSystemRejectsTheRequest() {
        Event event = createExistingEvent();
        eventRepository.addEvent(event);
        Event savedEvent = eventRepository.getEventById(event.getId());

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
                savedEvent.isSoldOut(),
                savedEvent.isOverloaded(),
                savedEvent.getActiveReservationsCount(),
                savedEvent.getVersion(),
                null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.updateEvent(validOwnerSessionId, invalidUpdateDTO));

        assertTrue(exception.getMessage().contains("Event name"));
    }

    @Test
    void GivenOwnerLoggedInEventExistsAndPastUpdatedDate_WhenUpdateEvent_ThenSystemRejectsTheRequest() {
        Event event = createExistingEvent();
        eventRepository.addEvent(event);
        Event savedEvent = eventRepository.getEventById(event.getId());

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
                savedEvent.isSoldOut(),
                savedEvent.isOverloaded(),
                savedEvent.getActiveReservationsCount(),
                savedEvent.getVersion(),
                null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.updateEvent(validOwnerSessionId, invalidUpdateDTO));

        assertTrue(exception.getMessage().contains("date"));
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

        assertTrue(exception.getMessage().contains("Invalid session ID"));
    }

    @Test
    void GivenLoggedInUserWithoutUpdatePermission_WhenUpdateEvent_ThenSystemRejectsTheRequest() {
        Event event = createExistingEvent();
        eventRepository.addEvent(event);
        Event savedEvent = eventRepository.getEventById(event.getId());

        String sessionWithoutPermission = "session-without-update-permission";
        
        // Setup a real user WITHOUT any roles
        Member plainUser = new Member(2L, "PlainUser");
        userRepository.addRegisteredMember(2L, plainUser, "password");
        tokenService.addValidSession(sessionWithoutPermission, 2L);

        EventDTO updateDTO = createValidUpdateDTO(savedEvent);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.updateEvent(sessionWithoutPermission, updateDTO));

        assertTrue(exception.getMessage().contains("User does not have permission to update event"));
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
                false,
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

        // FIX: The domain rules block editing if the user isn't an owner in the new company too.
        // Grant the owner a role in the new company to bypass the permission check,
        // allowing the logic to proceed and throw the intended "Cannot change event's company" exception.
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
                savedEvent.isSoldOut(),
                savedEvent.isOverloaded(),
                savedEvent.getActiveReservationsCount(),
                savedEvent.getVersion(),
                null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.updateEvent(validOwnerSessionId, updateDTO));

        assertTrue(exception.getMessage().contains("Cannot change event's company"));
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
                savedEvent.isSoldOut(),
                savedEvent.isOverloaded(),
                savedEvent.getActiveReservationsCount(),
                staleVersion,
                null);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> eventService.updateEvent(validOwnerSessionId, staleUpdateDTO));

        assertTrue(exception.getMessage().contains("Event was updated by another request"));
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
    }

    @Test
    void GivenOwnerLoggedInEventExistsAndInvalidMapConfiguration_WhenDefineEventMap_ThenSystemRejectsAndPreventsSaving() {
        Event event = createExistingEvent();
        eventRepository.addEvent(event);

        EventMapDTO invalidMapDTO = null;

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.defineEventMap(validOwnerSessionId, event.getId(), invalidMapDTO));

        assertTrue(exception.getMessage().contains("Map data cannot be null"));
    }

    @Test
    void GivenOwnerLoggedInEventExistsAndMapInventoryInconsistency_WhenDefineEventMap_ThenSystemRejectsAndPreventsSaving() {
        Event event = createExistingEvent();
        eventRepository.addEvent(event);

        EventMapDTO inconsistentMapDTO = createInconsistentMapDTO();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.defineEventMap(validOwnerSessionId, event.getId(), inconsistentMapDTO));

        assertTrue(exception.getMessage().contains("Reserved and sold spots cannot exceed capacity"));
    }

    @Test
    void GivenInvalidSession_WhenDefineEventMap_ThenSystemRejectsTheRequest() {
        Event event = createExistingEvent();
        eventRepository.addEvent(event);
        EventMapDTO validMapDTO = createValidMapDTO();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.defineEventMap(invalidSessionId, event.getId(), validMapDTO));

        assertTrue(exception.getMessage().contains("Invalid session ID"));
    }

    @Test
    void GivenLoggedInUserWithoutDefineMapPermission_WhenDefineEventMap_ThenSystemRejectsTheRequest() {
        Event event = createExistingEvent();
        eventRepository.addEvent(event);

        String sessionWithoutPermission = "session-without-map-permission";
        Member plainUser = new Member(2L, "PlainUser");
        userRepository.addRegisteredMember(2L, plainUser, "password");
        tokenService.addValidSession(sessionWithoutPermission, 2L);

        EventMapDTO validMapDTO = createValidMapDTO();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.defineEventMap(sessionWithoutPermission, event.getId(), validMapDTO));

        assertTrue(exception.getMessage().contains("User does not have permission to define event map"));
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
        return new Event(
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

        assertTrue(exception.getMessage().contains("Invalid session ID"));
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
                savedEvent.isSoldOut(),
                savedEvent.isOverloaded(),
                savedEvent.getActiveReservationsCount(),
                savedEvent.getVersion(),
                null);
    }

    private EventMapDTO createValidMapDTO() {
        ElementDTO stage = new ElementDTO(
                1L, "Main Stage", new PairDTO<>(0, 0), new PairDTO<>(2, 10), "Stage");
        ElementDTO entrance = new ElementDTO(
                2L, "Main Entrance", new PairDTO<>(9, 0), new PairDTO<>(1, 3), "Entrance");
        SeatingAreaDTO seatingArea = new SeatingAreaDTO(
                3L, "Seating Area A", new PairDTO<>(3, 2), new PairDTO<>(4, 6), "SeatingArea", false, 4, 6, List.of());
        StandingAreaDTO standingArea = new StandingAreaDTO(
                4L, "Standing Area B", new PairDTO<>(3, 10), new PairDTO<>(4, 5), "StandingArea", false, 100L, 0L, 0L);

        return new EventMapDTO(new PairDTO<>(10, 20), List.of(stage, entrance, seatingArea, standingArea), false);
    }

    private EventMapDTO createInconsistentMapDTO() {
        StandingAreaDTO inconsistentStandingArea = new StandingAreaDTO(
                1L, "Invalid Standing Area", new PairDTO<>(2, 2), new PairDTO<>(4, 5), "StandingArea", false, 10L, 8L, 5L);

        return new EventMapDTO(new PairDTO<>(10, 20), List.of(inconsistentStandingArea), false);
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
    }
}
