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
import ticketsystem.InfrastructureLayer.EventRepository;

public class EventServiceAcceptanceTest {

    private EventService eventService;
    private EventRepository eventRepository;
    private FakeTokenService tokenService;
    private FakeMembershipDomainService membershipDomain;

    private final String validOwnerSessionId = "owner-session";
    private final String invalidSessionId = "invalid-session";

    private final Long ownerId = 1L;
    private final Long companyId = 100L;

    @BeforeEach
    void setUp() {
        eventRepository = new EventRepository();
        tokenService = new FakeTokenService();
        membershipDomain = new FakeMembershipDomainService();

        eventService = new EventService(
                eventRepository,
                tokenService,
                membershipDomain);

        tokenService.addValidSession(validOwnerSessionId, ownerId);

        membershipDomain.allow(validOwnerSessionId, companyId, "event:create");
        membershipDomain.allow(validOwnerSessionId, companyId, "event:defineMap");
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
        // Act
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

        // Assert
        assertTrue(exception.getMessage().contains("Event name cannot be null or empty"));
        assertNull(eventRepository.getEventById(1L));
    }

    @Test
    void GivenOwnerLoggedInAndPastEventDate_WhenInsertEvent_ThenSystemRejectsTheRequest() {
        // Act
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

        // Assert
        assertTrue(exception.getMessage().contains("Event date must be in the future"));
        assertNull(eventRepository.getEventById(1L));
    }

    @Test
    void GivenOwnerLoggedInAndInvalidInventoryDetails_WhenInsertEvent_ThenSystemRejectsTheRequest() {
        // Act
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

        // Assert
        assertTrue(exception.getMessage().contains("Traffic threshold must be a positive number"));
        assertNull(eventRepository.getEventById(1L));
    }

    @Test
    void GivenOwnerLoggedInAndInvalidMapSize_WhenInsertEvent_ThenSystemRejectsTheRequest() {
        // Act
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

        // Assert
        assertTrue(exception.getMessage().contains("Map size must be positive"));
        assertNull(eventRepository.getEventById(1L));
    }

    @Test
    void GivenInvalidSession_WhenInsertEvent_ThenSystemRejectsTheRequest() {
        // Act
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

        // Assert
        assertTrue(exception.getMessage().contains("Invalid session ID"));
        assertNull(eventRepository.getEventById(1L));
    }

    @Test
    void GivenLoggedInUserWithoutCreatePermission_WhenInsertEvent_ThenSystemRejectsTheRequest() {
        // Arrange
        String sessionWithoutPermission = "session-without-create-permission";
        tokenService.addValidSession(sessionWithoutPermission, 2L);

        // Act
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

        // Assert
        assertTrue(exception.getMessage().contains("User does not have permission to create an event"));
        assertNull(eventRepository.getEventById(1L));
    }

    // -------------------- Define Event Map Tests -------------------

    @Test
    void GivenOwnerLoggedInEventExistsAndValidMap_WhenDefineEventMap_ThenConfigurationIsSaved() {
        // Arrange
        Event event = createExistingEvent();
        eventRepository.addEvent(event);

        EventMapDTO validMapDTO = createValidMapDTO();

        // Act
        Boolean result = eventService.defineEventMap(
                validOwnerSessionId,
                event.getId(),
                validMapDTO);

        // Assert
        Event updatedEvent = eventRepository.getEventById(event.getId());

        assertTrue(result);
        assertNotNull(updatedEvent);
        assertNotNull(updatedEvent.getMap());
        assertEquals(eventStatus.ACTIVE, updatedEvent.getStatus());
    }

    @Test
    void GivenOwnerLoggedInEventExistsAndInvalidMapConfiguration_WhenDefineEventMap_ThenSystemRejectsAndPreventsSaving() {
        // Arrange
        Event event = createExistingEvent();
        eventRepository.addEvent(event);

        int originalElementCount = elementCount(eventRepository.getEventById(event.getId()));
        eventStatus originalStatus = eventRepository.getEventById(event.getId()).getStatus();

        EventMapDTO invalidMapDTO = null;

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.defineEventMap(
                        validOwnerSessionId,
                        event.getId(),
                        invalidMapDTO));

        // Assert
        Event unchangedEvent = eventRepository.getEventById(event.getId());

        assertTrue(exception.getMessage().contains("Map data cannot be null"));
        assertEquals(originalElementCount, elementCount(unchangedEvent));
        assertEquals(originalStatus, unchangedEvent.getStatus());
    }

    @Test
    void GivenOwnerLoggedInEventExistsAndMapInventoryInconsistency_WhenDefineEventMap_ThenSystemRejectsAndPreventsSaving() {
        // Arrange
        Event event = createExistingEvent();
        eventRepository.addEvent(event);

        int originalElementCount = elementCount(eventRepository.getEventById(event.getId()));
        eventStatus originalStatus = eventRepository.getEventById(event.getId()).getStatus();

        EventMapDTO inconsistentMapDTO = createInconsistentMapDTO();

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.defineEventMap(
                        validOwnerSessionId,
                        event.getId(),
                        inconsistentMapDTO));

        // Assert
        Event unchangedEvent = eventRepository.getEventById(event.getId());

        assertTrue(exception.getMessage().contains("Reserved and sold spots cannot exceed capacity"));
        assertEquals(originalElementCount, elementCount(unchangedEvent));
        assertEquals(originalStatus, unchangedEvent.getStatus());
    }

    @Test
    void GivenInvalidSession_WhenDefineEventMap_ThenSystemRejectsTheRequest() {
        // Arrange
        Event event = createExistingEvent();
        eventRepository.addEvent(event);

        int originalElementCount = elementCount(eventRepository.getEventById(event.getId()));
        eventStatus originalStatus = eventRepository.getEventById(event.getId()).getStatus();

        EventMapDTO validMapDTO = createValidMapDTO();

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.defineEventMap(
                        invalidSessionId,
                        event.getId(),
                        validMapDTO));

        // Assert
        Event unchangedEvent = eventRepository.getEventById(event.getId());

        assertTrue(exception.getMessage().contains("Invalid session ID"));
        assertEquals(originalElementCount, elementCount(unchangedEvent));
        assertEquals(originalStatus, unchangedEvent.getStatus());
    }

    @Test
    void GivenLoggedInUserWithoutDefineMapPermission_WhenDefineEventMap_ThenSystemRejectsTheRequest() {
        // Arrange
        Event event = createExistingEvent();
        eventRepository.addEvent(event);

        int originalElementCount = elementCount(eventRepository.getEventById(event.getId()));
        eventStatus originalStatus = eventRepository.getEventById(event.getId()).getStatus();

        String sessionWithoutPermission = "session-without-map-permission";
        tokenService.addValidSession(sessionWithoutPermission, 2L);

        EventMapDTO validMapDTO = createValidMapDTO();

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.defineEventMap(
                        sessionWithoutPermission,
                        event.getId(),
                        validMapDTO));

        // Assert
        Event unchangedEvent = eventRepository.getEventById(event.getId());

        assertTrue(exception.getMessage().contains("User does not have permission to define event map"));
        assertEquals(originalElementCount, elementCount(unchangedEvent));
        assertEquals(originalStatus, unchangedEvent.getStatus());
    }

    @Test
    void GivenEventDoesNotExist_WhenDefineEventMap_ThenSystemRejectsTheRequest() {
        // Arrange
        EventMapDTO validMapDTO = createValidMapDTO();

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.defineEventMap(
                        validOwnerSessionId,
                        999L,
                        validMapDTO));

        // Assert
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
        // Arrange
        Event event = createExistingEvent();
        eventRepository.addEvent(event);

        EventMapDTO mapLayout = createValidMapDTO();

        eventService.defineEventMap(
                validOwnerSessionId,
                event.getId(),
                mapLayout);

        // Act
        EventMapDTO result = eventService.getEventMap(
                validOwnerSessionId,
                event.getId());

        // Assert
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
        // Arrange
        Long nonExistingEventId = 999L;

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.getEventMap(validOwnerSessionId, nonExistingEventId));

        // Assert
        assertTrue(exception.getMessage().contains("Event not found"));
    }

    @Test
    void GivenInvalidSession_WhenGetEventMap_ThenSystemRejectsTheRequest() {
        // Arrange
        Event event = createExistingEvent();
        eventRepository.addEvent(event);

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.getEventMap(invalidSessionId, event.getId()));

        // Assert
        assertTrue(exception.getMessage().contains("Invalid session ID"));
    }

    // -------------------- Helper Methods and Test Doubles -------------------

    private EventMapDTO createValidMapDTO() {
        ElementDTO stage = new ElementDTO(
                1L,
                "Main Stage",
                new PairDTO<>(0, 0),
                new PairDTO<>(2, 10),
                "Stage");

        ElementDTO entrance = new ElementDTO(
                2L,
                "Main Entrance",
                new PairDTO<>(9, 0),
                new PairDTO<>(1, 3),
                "Entrance");

        SeatingAreaDTO seatingArea = new SeatingAreaDTO(
                3L,
                "Seating Area A",
                new PairDTO<>(3, 2),
                new PairDTO<>(4, 6),
                "SeatingArea",
                false,
                4,
                6,
                List.of());

        StandingAreaDTO standingArea = new StandingAreaDTO(
                4L,
                "Standing Area B",
                new PairDTO<>(3, 10),
                new PairDTO<>(4, 5),
                "StandingArea",
                false,
                100L,
                0L,
                0L);

        return new EventMapDTO(
                new PairDTO<>(10, 20),
                List.of(stage, entrance, seatingArea, standingArea),
                false);
    }

    private EventMapDTO createInconsistentMapDTO() {
        StandingAreaDTO inconsistentStandingArea = new StandingAreaDTO(
                1L,
                "Invalid Standing Area",
                new PairDTO<>(2, 2),
                new PairDTO<>(4, 5),
                "StandingArea",
                false,
                10L,
                8L,
                5L);

        return new EventMapDTO(
                new PairDTO<>(10, 20),
                List.of(inconsistentStandingArea),
                false);
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

    /*
     * This is a test double, not a Mockito mock.
     * If your MembershipDomainService has a constructor with dependencies,
     * adjust the super(...) call according to your real constructor.
     */
    private static class FakeMembershipDomainService extends MembershipDomainService {

        private final Set<String> allowedPermissions = new HashSet<>();

        void allow(String sessionId, Long companyId, String permission) {
            allowedPermissions.add(key(sessionId, companyId, permission));
        }

        @Override
        public boolean validatePermission(String sessionId, Long companyId, String permission) {
            return allowedPermissions.contains(key(sessionId, companyId, permission));
        }

        private String key(String sessionId, Long companyId, String permission) {
            return sessionId + "|" + companyId + "|" + permission;
        }
    }
}