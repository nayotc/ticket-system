package ticketsystem.ApplicationLayer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ticketsystem.DTO.Event.EventDTO;
import ticketsystem.DomainLayer.MembershipDomainService;
import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.event.EventCategory;
import ticketsystem.DomainLayer.event.EventLocation;
import ticketsystem.DomainLayer.event.Pair;
import ticketsystem.DomainLayer.user.Permission;
import ticketsystem.DomainLayer.MembershipDomainService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class EventServiceTest {

    @Mock
    private IEventRepository mockEventRepository;

    @Mock
    private ITokenService mockTokenService;

    @Mock
    private MembershipDomainService mockMembershipDomainService;

    private EventService eventService;

    private final String validSessionId = "valid-session";
    private final Long validUserId = 1L;
    private final Long validCompanyId = 1L;
    private final Long validEventId = 1L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        eventService = new EventService(
                mockEventRepository,
                mockTokenService,
                mockMembershipDomainService
        );
            

        when(mockTokenService.validateToken(validSessionId)).thenReturn(true);
        when(mockTokenService.validateToken(validSessionId)).thenReturn(true);
        // FIX: Extracting the user ID must be mocked in setUp so all validation tests know who the user is!
        when(mockTokenService.extractUserId(validSessionId)).thenReturn(validUserId);
        
        when(mockMembershipDomainService.validatePermission(
            validUserId,
            validCompanyId,
            Permission.MANAGE_EVENT_INVENTORY)).thenReturn(true);
    }

    private Event createEvent(Event.eventStatus status) {
        Event event = new Event(
                validEventId,
                LocalDateTime.now().plusDays(10),
                "Test Event",
                validCompanyId,
                validUserId,
                EventLocation.NEW_YORK,
                100L,
                EventCategory.CONCERT,
                "Test Artist",
                BigDecimal.valueOf(50),
                new Pair<>(10, 10)
        );

        event.setStatus(status);
        return event;
    }

    private EventDTO createEventDTO(
        Long id,
        String name,
        Long companyId,
        LocalDateTime date,
        EventLocation location,
        Long trafficThreshold,
        EventCategory category,
        String artistName,
        BigDecimal ticketPrice,
        int version
) {
    return new EventDTO(
            id,
            name,
            companyId,
            validUserId,
            date,
            location == null ? null : location.name(),
            trafficThreshold,
            Event.eventStatus.DRAFT.name(),
            category == null ? null : category.name(),
            artistName,
            ticketPrice,
            null,       // mapSize
            0.0,        // rate
            false,      // soldOut
            false,      // overloaded
            0,          // activeReservationsCount
            version,
            null        // map
    );

        
    }

    @Test
    void GivenValidTokenAndPermission_WhenInsertEvent_ThenAddEventOnce() {
        when(mockTokenService.extractUserId(validSessionId)).thenReturn(validUserId);
        when(mockEventRepository.getNextId()).thenReturn(1L);

        LocalDateTime futureDate = LocalDateTime.now().plusDays(1);

        assertDoesNotThrow(() -> eventService.insertEvent(
                validSessionId,
                "Test Event",
                validCompanyId,
                futureDate,
                EventLocation.NEW_YORK,
                100L,
                EventCategory.CONCERT,
                "Test Artist",
                BigDecimal.valueOf(50),
                10,
                10
        ));

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(mockEventRepository, times(1)).addEvent(eventCaptor.capture());

        Event createdEvent = eventCaptor.getValue();

        assertEquals(validEventId, createdEvent.getId());
        assertEquals("Test Event", createdEvent.getName());
        assertEquals(validCompanyId, createdEvent.getCompanyId());
        assertEquals(validUserId, createdEvent.getOpenedBy());
        assertEquals(EventLocation.NEW_YORK, createdEvent.getLocation());
        assertEquals(EventCategory.CONCERT, createdEvent.getCategory());
        assertEquals("Test Artist", createdEvent.getArtistName());
        assertEquals(0, BigDecimal.valueOf(50).compareTo(createdEvent.getTicketPrice()));
        assertEquals(Event.eventStatus.DRAFT, createdEvent.getStatus());

        verify(mockTokenService).validateToken(validSessionId);
        verify(mockMembershipDomainService).validatePermission(validSessionId, validCompanyId, "event:create");
        verify(mockTokenService).extractUserId(validSessionId);
        verify(mockEventRepository).getNextId();
        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenInvalidToken_WhenInsertEvent_ThenThrowException() {
        String sessionId = "invalid-session";
        when(mockTokenService.validateToken(sessionId)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.insertEvent(
                        sessionId,
                        "Test Event",
                        validCompanyId,
                        LocalDateTime.now().plusDays(1),
                        EventLocation.NEW_YORK,
                        100L,
                        EventCategory.CONCERT,
                        "Test Artist",
                        BigDecimal.valueOf(50),
                        10,
                        10
                )
        );

        assertEquals("Invalid session ID", exception.getMessage());

        verify(mockTokenService).validateToken(sessionId);
        verify(mockMembershipDomainService, never()).validatePermission(any(), any(), any());
        verify(mockEventRepository, never()).addEvent(any());
        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenInvalidPermission_WhenInsertEvent_ThenThrowException() {
        when(mockMembershipDomainService.validatePermission(
            validUserId,
            validCompanyId,
            Permission.MANAGE_EVENT_INVENTORY)).thenReturn(false);
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.insertEvent(
                        validSessionId,
                        "Test Event",
                        validCompanyId,
                        LocalDateTime.now().plusDays(1),
                        EventLocation.NEW_YORK,
                        100L,
                        EventCategory.CONCERT,
                        "Test Artist",
                        BigDecimal.valueOf(50),
                        10,
                        10
                )
        );

        assertEquals("User does not have permission to create an event", exception.getMessage());

        verify(mockTokenService).validateToken(validSessionId);
        verify(mockMembershipDomainService).validatePermission(validSessionId, validCompanyId, "event:create");
        verify(mockEventRepository, never()).addEvent(any());
        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenEmptyEventName_WhenInsertEvent_ThenThrowException() {
        when(mockMembershipDomainService.validatePermission(
                validSessionId,
                validCompanyId,
                "event:create"
        )).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.insertEvent(
                        validSessionId,
                        "",
                        validCompanyId,
                        LocalDateTime.now().plusDays(1),
                        EventLocation.NEW_YORK,
                        100L,
                        EventCategory.CONCERT,
                        "Test Artist",
                        BigDecimal.valueOf(50),
                        10,
                        10
                )
        );

        assertEquals("Event name cannot be null or empty", exception.getMessage());

        verify(mockTokenService).validateToken(validSessionId);
        verify(mockMembershipDomainService).validatePermission(validSessionId, validCompanyId, "event:create");
        verify(mockEventRepository, never()).addEvent(any());
        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenPastDate_WhenInsertEvent_ThenThrowException() {
        when(mockMembershipDomainService.validatePermission(
                validSessionId,
                validCompanyId,
                "event:create"
        )).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.insertEvent(
                        validSessionId,
                        "Test Event",
                        validCompanyId,
                        LocalDateTime.now().minusDays(1),
                        EventLocation.NEW_YORK,
                        100L,
                        EventCategory.CONCERT,
                        "Test Artist",
                        BigDecimal.valueOf(50),
                        10,
                        10
                )
        );

        assertEquals("Event date must be in the future", exception.getMessage());

        verify(mockTokenService).validateToken(validSessionId);
        verify(mockMembershipDomainService).validatePermission(validSessionId, validCompanyId, "event:create");
        verify(mockEventRepository, never()).addEvent(any());
        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenNullLocation_WhenInsertEvent_ThenThrowException() {
        when(mockMembershipDomainService.validatePermission(
                validSessionId,
                validCompanyId,
                "event:create"
        )).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.insertEvent(
                        validSessionId,
                        "Test Event",
                        validCompanyId,
                        LocalDateTime.now().plusDays(1),
                        null,
                        100L,
                        EventCategory.CONCERT,
                        "Test Artist",
                        BigDecimal.valueOf(50),
                        10,
                        10
                )
        );

        assertEquals("Event location cannot be null", exception.getMessage());

        verify(mockTokenService).validateToken(validSessionId);
        verify(mockMembershipDomainService).validatePermission(validSessionId, validCompanyId, "event:create");
        verify(mockEventRepository, never()).addEvent(any());
        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenInvalidTrafficThreshold_WhenInsertEvent_ThenThrowException() {
        when(mockMembershipDomainService.validatePermission(
                validSessionId,
                validCompanyId,
                "event:create"
        )).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.insertEvent(
                        validSessionId,
                        "Test Event",
                        validCompanyId,
                        LocalDateTime.now().plusDays(1),
                        EventLocation.NEW_YORK,
                        0L,
                        EventCategory.CONCERT,
                        "Test Artist",
                        BigDecimal.valueOf(50),
                        10,
                        10
                )
        );

        assertEquals("Traffic threshold must be a positive number", exception.getMessage());

        verify(mockTokenService).validateToken(validSessionId);
        verify(mockMembershipDomainService).validatePermission(validSessionId, validCompanyId, "event:create");
        verify(mockEventRepository, never()).addEvent(any());
        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenNullCategory_WhenInsertEvent_ThenThrowException() {
        when(mockMembershipDomainService.validatePermission(
                validSessionId,
                validCompanyId,
                "event:create"
        )).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.insertEvent(
                        validSessionId,
                        "Test Event",
                        validCompanyId,
                        LocalDateTime.now().plusDays(1),
                        EventLocation.NEW_YORK,
                        100L,
                        null,
                        "Test Artist",
                        BigDecimal.valueOf(50),
                        10,
                        10
                )
        );

        assertEquals("Event category cannot be null", exception.getMessage());

        verify(mockTokenService).validateToken(validSessionId);
        verify(mockMembershipDomainService).validatePermission(validSessionId, validCompanyId, "event:create");
        verify(mockEventRepository, never()).addEvent(any());
        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenEmptyArtist_WhenInsertEvent_ThenThrowException() {
        when(mockMembershipDomainService.validatePermission(
                validSessionId,
                validCompanyId,
                "event:create"
        )).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.insertEvent(
                        validSessionId,
                        "Test Event",
                        validCompanyId,
                        LocalDateTime.now().plusDays(1),
                        EventLocation.NEW_YORK,
                        100L,
                        EventCategory.CONCERT,
                        " ",
                        BigDecimal.valueOf(50),
                        10,
                        10
                )
        );

        assertEquals("Artist name cannot be null or empty", exception.getMessage());

        verify(mockTokenService).validateToken(validSessionId);
        verify(mockMembershipDomainService).validatePermission(validSessionId, validCompanyId, "event:create");
        verify(mockEventRepository, never()).addEvent(any());
        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenNegativePrice_WhenInsertEvent_ThenThrowException() {
        when(mockMembershipDomainService.validatePermission(
                validSessionId,
                validCompanyId,
                "event:create"
        )).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.insertEvent(
                        validSessionId,
                        "Test Event",
                        validCompanyId,
                        LocalDateTime.now().plusDays(1),
                        EventLocation.NEW_YORK,
                        100L,
                        EventCategory.CONCERT,
                        "Test Artist",
                        BigDecimal.valueOf(-1),
                        10,
                        10
                )
        );

        assertEquals("Price must be a non-negative number", exception.getMessage());

        verify(mockTokenService).validateToken(validSessionId);
        verify(mockMembershipDomainService).validatePermission(validSessionId, validCompanyId, "event:create");
        verify(mockEventRepository, never()).addEvent(any());
        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenInvalidMapHeight_WhenInsertEvent_ThenThrowException() {
        when(mockMembershipDomainService.validatePermission(
                validSessionId,
                validCompanyId,
                "event:create"
        )).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.insertEvent(
                        validSessionId,
                        "Test Event",
                        validCompanyId,
                        LocalDateTime.now().plusDays(1),
                        EventLocation.NEW_YORK,
                        100L,
                        EventCategory.CONCERT,
                        "Test Artist",
                        BigDecimal.valueOf(50),
                        0,
                        10
                )
        );

        assertEquals("Map size must be positive", exception.getMessage());

        verify(mockTokenService).validateToken(validSessionId);
        verify(mockMembershipDomainService).validatePermission(validSessionId, validCompanyId, "event:create");
        verify(mockEventRepository, never()).addEvent(any());
        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenInvalidMapWidth_WhenInsertEvent_ThenThrowException() {
        when(mockMembershipDomainService.validatePermission(
                validSessionId,
                validCompanyId,
                "event:create"
        )).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.insertEvent(
                        validSessionId,
                        "Test Event",
                        validCompanyId,
                        LocalDateTime.now().plusDays(1),
                        EventLocation.NEW_YORK,
                        100L,
                        EventCategory.CONCERT,
                        "Test Artist",
                        BigDecimal.valueOf(50),
                        10,
                        0
                )
        );

        assertEquals("Map size must be positive", exception.getMessage());

        verify(mockTokenService).validateToken(validSessionId);
        verify(mockMembershipDomainService).validatePermission(validSessionId, validCompanyId, "event:create");
        verify(mockEventRepository, never()).addEvent(any());
        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    // ------------------ Update Event Tests ------------------

    @Test
    void GivenValidDetails_WhenUpdateEvent_ThenEventIsUpdated() {
        Event existingEvent = createEvent(Event.eventStatus.DRAFT);

        EventDTO eventDTO = createEventDTO(
                validEventId,
                "Updated Event",
                validCompanyId,
                LocalDateTime.now().plusDays(20),
                EventLocation.NEW_YORK,
                200L,
                EventCategory.CONCERT,
                "Updated Artist",
                BigDecimal.valueOf(80),
                existingEvent.getVersion()
        );

        when(mockMembershipDomainService.validatePermission(
                validSessionId,
                validCompanyId,
                "event:update"
        )).thenReturn(true);

        when(mockEventRepository.getEventById(validEventId)).thenReturn(existingEvent);

        Boolean result = eventService.updateEvent(validSessionId, eventDTO);

        assertTrue(result);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(mockEventRepository).updateEvent(eventCaptor.capture());

        Event updatedEvent = eventCaptor.getValue();

        assertEquals("Updated Event", updatedEvent.getName());
        assertEquals(EventLocation.NEW_YORK, updatedEvent.getLocation());
        assertEquals(200L, updatedEvent.getTrafficThreshold());
        assertEquals(EventCategory.CONCERT, updatedEvent.getCategory());
        assertEquals("Updated Artist", updatedEvent.getArtistName());
        assertEquals(0, BigDecimal.valueOf(80).compareTo(updatedEvent.getTicketPrice()));

        verify(mockTokenService).validateToken(validSessionId);
        verify(mockMembershipDomainService).validatePermission(validSessionId, validCompanyId, "event:update");
        verify(mockEventRepository).getEventById(validEventId);
        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenInvalidToken_WhenUpdateEvent_ThenThrowException() {
        String sessionId = "invalid-session";
        when(mockTokenService.validateToken(sessionId)).thenReturn(false);

        EventDTO eventDTO = createEventDTO(
                validEventId,
                "Updated Event",
                validCompanyId,
                LocalDateTime.now().plusDays(20),
                EventLocation.NEW_YORK,
                200L,
                EventCategory.CONCERT,
                "Updated Artist",
                BigDecimal.valueOf(80),
                0
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.updateEvent(sessionId, eventDTO)
        );

        assertEquals("Invalid session ID", exception.getMessage());

        verify(mockTokenService).validateToken(sessionId);
        verify(mockMembershipDomainService, never()).validatePermission(any(), any(), any());
        verify(mockEventRepository, never()).updateEvent(any());
        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenNullEventDTO_WhenUpdateEvent_ThenThrowException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.updateEvent(validSessionId, null)
        );

        assertEquals("Event data cannot be null", exception.getMessage());

        verify(mockTokenService).validateToken(validSessionId);
        verify(mockMembershipDomainService, never()).validatePermission(any(), any(), any());
        verify(mockEventRepository, never()).updateEvent(any());
        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenNullEventId_WhenUpdateEvent_ThenThrowException() {
        EventDTO eventDTO = createEventDTO(
                null,
                "Updated Event",
                validCompanyId,
                LocalDateTime.now().plusDays(20),
                EventLocation.NEW_YORK,
                200L,
                EventCategory.CONCERT,
                "Updated Artist",
                BigDecimal.valueOf(80),
                0
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.updateEvent(validSessionId, eventDTO)
        );

        assertEquals("Event ID cannot be null", exception.getMessage());

        verify(mockTokenService).validateToken(validSessionId);
        verify(mockMembershipDomainService, never()).validatePermission(any(), any(), any());
        verify(mockEventRepository, never()).updateEvent(any());
        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenNullCompanyId_WhenUpdateEvent_ThenThrowException() {
        EventDTO eventDTO = createEventDTO(
                validEventId,
                "Updated Event",
                null,
                LocalDateTime.now().plusDays(20),
                EventLocation.NEW_YORK,
                200L,
                EventCategory.CONCERT,
                "Updated Artist",
                BigDecimal.valueOf(80),
                0
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.updateEvent(validSessionId, eventDTO)
        );

        assertEquals("Company ID cannot be null", exception.getMessage());

        verify(mockTokenService).validateToken(validSessionId);
        verify(mockMembershipDomainService, never()).validatePermission(any(), any(), any());
        verify(mockEventRepository, never()).updateEvent(any());
        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenNoPermission_WhenUpdateEvent_ThenThrowException() {
        EventDTO eventDTO = createEventDTO(
                validEventId,
                "Updated Event",
                validCompanyId,
                LocalDateTime.now().plusDays(20),
                EventLocation.NEW_YORK,
                200L,
                EventCategory.CONCERT,
                "Updated Artist",
                BigDecimal.valueOf(80),
                0
        );

        when(mockMembershipDomainService.validatePermission(
                validSessionId,
                validCompanyId,
                "event:update"
        )).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.updateEvent(validSessionId, eventDTO)
        );

        assertEquals("User does not have permission to update event", exception.getMessage());

        verify(mockTokenService).validateToken(validSessionId);
        verify(mockMembershipDomainService).validatePermission(validSessionId, validCompanyId, "event:update");
        verify(mockEventRepository, never()).updateEvent(any());
        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenEventNotFound_WhenUpdateEvent_ThenThrowException() {
        EventDTO eventDTO = createEventDTO(
                validEventId,
                "Updated Event",
                validCompanyId,
                LocalDateTime.now().plusDays(20),
                EventLocation.NEW_YORK,
                200L,
                EventCategory.CONCERT,
                "Updated Artist",
                BigDecimal.valueOf(80),
                0
        );

        when(mockMembershipDomainService.validatePermission(
                validSessionId,
                validCompanyId,
                "event:update"
        )).thenReturn(true);

        when(mockEventRepository.getEventById(validEventId)).thenReturn(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.updateEvent(validSessionId, eventDTO)
        );

        assertEquals("Event not found", exception.getMessage());

        verify(mockTokenService).validateToken(validSessionId);
        verify(mockMembershipDomainService).validatePermission(validSessionId, validCompanyId, "event:update");
        verify(mockEventRepository).getEventById(validEventId);
        verify(mockEventRepository, never()).updateEvent(any());
        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenDifferentCompany_WhenUpdateEvent_ThenThrowException() {
        Event existingEvent = createEvent(Event.eventStatus.DRAFT);
        Long differentCompanyId = 2L;

        EventDTO eventDTO = createEventDTO(
                validEventId,
                "Updated Event",
                differentCompanyId,
                LocalDateTime.now().plusDays(20),
                EventLocation.NEW_YORK,
                200L,
                EventCategory.CONCERT,
                "Updated Artist",
                BigDecimal.valueOf(80),
                0
        );

        when(mockMembershipDomainService.validatePermission(
                validSessionId,
                differentCompanyId,
                "event:update"
        )).thenReturn(true);

        when(mockEventRepository.getEventById(validEventId)).thenReturn(existingEvent);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.updateEvent(validSessionId, eventDTO)
        );

        assertEquals("Cannot change event's company", exception.getMessage());

        verify(mockTokenService).validateToken(validSessionId);
        verify(mockMembershipDomainService).validatePermission(validSessionId, differentCompanyId, "event:update");
        verify(mockEventRepository).getEventById(validEventId);
        verify(mockEventRepository, never()).updateEvent(any());
        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenStaleVersion_WhenUpdateEvent_ThenThrowException() {
        Event existingEvent = createEvent(Event.eventStatus.DRAFT);
        existingEvent.incrementVersion();

        EventDTO eventDTO = createEventDTO(
                validEventId,
                "Updated Event",
                validCompanyId,
                LocalDateTime.now().plusDays(20),
                EventLocation.NEW_YORK,
                200L,
                EventCategory.CONCERT,
                "Updated Artist",
                BigDecimal.valueOf(80),
                0
        );

        when(mockMembershipDomainService.validatePermission(
                validSessionId,
                validCompanyId,
                "event:update"
        )).thenReturn(true);

        when(mockEventRepository.getEventById(validEventId)).thenReturn(existingEvent);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> eventService.updateEvent(validSessionId, eventDTO)
        );

        assertEquals("Event was updated by another request", exception.getMessage());

        verify(mockTokenService).validateToken(validSessionId);
        verify(mockMembershipDomainService).validatePermission(validSessionId, validCompanyId, "event:update");
        verify(mockEventRepository).getEventById(validEventId);
        verify(mockEventRepository, never()).updateEvent(any());
        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenInvalidDate_WhenUpdateEvent_ThenThrowException() {
        Event existingEvent = createEvent(Event.eventStatus.DRAFT);

        EventDTO eventDTO = createEventDTO(
                validEventId,
                "Updated Event",
                validCompanyId,
                LocalDateTime.now().minusDays(1),
                EventLocation.NEW_YORK,
                200L,
                EventCategory.CONCERT,
                "Updated Artist",
                BigDecimal.valueOf(80),
                existingEvent.getVersion()
        );

        when(mockMembershipDomainService.validatePermission(
                validSessionId,
                validCompanyId,
                "event:update"
        )).thenReturn(true);

        when(mockEventRepository.getEventById(validEventId)).thenReturn(existingEvent);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.updateEvent(validSessionId, eventDTO)
        );

        assertEquals("Event date must be in the future", exception.getMessage());

        verify(mockTokenService).validateToken(validSessionId);
        verify(mockMembershipDomainService).validatePermission(validSessionId, validCompanyId, "event:update");
        verify(mockEventRepository).getEventById(validEventId);
        verify(mockEventRepository, never()).updateEvent(any());
        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    // ------------------ Delete Event Tests ------------------

    @Test
    void GivenInactiveEvent_WhenDeleteEvent_ThenEventIsDeleted() {
        Event event = createEvent(Event.eventStatus.INACTIVE);

        when(mockEventRepository.getEventById(validEventId)).thenReturn(event);
        when(mockMembershipDomainService.validatePermission(
                validSessionId,
                validCompanyId,
                "event:remove"
        )).thenReturn(true);

        Boolean result = eventService.deleteEvent(validSessionId, validEventId);

        assertTrue(result);

        verify(mockTokenService).validateToken(validSessionId);
        verify(mockEventRepository, times(2)).getEventById(validEventId);
        verify(mockMembershipDomainService).validatePermission(validSessionId, validCompanyId, "event:remove");
        verify(mockEventRepository).deleteEvent(validEventId, event.getVersion());
        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenCancelledEvent_WhenDeleteEvent_ThenEventIsDeleted() {
        Event event = createEvent(Event.eventStatus.CANCELLED);

        when(mockEventRepository.getEventById(validEventId)).thenReturn(event);
        when(mockMembershipDomainService.validatePermission(
                validSessionId,
                validCompanyId,
                "event:remove"
        )).thenReturn(true);

        Boolean result = eventService.deleteEvent(validSessionId, validEventId);

        assertTrue(result);

        verify(mockTokenService).validateToken(validSessionId);
        verify(mockEventRepository, times(2)).getEventById(validEventId);
        verify(mockMembershipDomainService).validatePermission(validSessionId, validCompanyId, "event:remove");
        verify(mockEventRepository).deleteEvent(validEventId, event.getVersion());
        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenInvalidToken_WhenDeleteEvent_ThenThrowException() {
        String sessionId = "invalid-session";
        when(mockTokenService.validateToken(sessionId)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.deleteEvent(sessionId, validEventId)
        );

        assertEquals("Invalid session ID", exception.getMessage());

        verify(mockTokenService).validateToken(sessionId);
        verify(mockEventRepository, never()).getEventById(any());
        verify(mockEventRepository, never()).deleteEvent(any(), anyLong());
        verify(mockMembershipDomainService, never()).validatePermission(any(), any(), any());
        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenNoPermission_WhenDeleteEvent_ThenThrowException() {
        Event event = createEvent(Event.eventStatus.INACTIVE);

        when(mockEventRepository.getEventById(validEventId)).thenReturn(event);
        when(mockMembershipDomainService.validatePermission(
                validSessionId,
                validCompanyId,
                "event:remove"
        )).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.deleteEvent(validSessionId, validEventId)
        );

        assertEquals("User does not have permission to remove an event", exception.getMessage());

        verify(mockTokenService).validateToken(validSessionId);
        verify(mockEventRepository).getEventById(validEventId);
        verify(mockMembershipDomainService).validatePermission(validSessionId, validCompanyId, "event:remove");
        verify(mockEventRepository, never()).deleteEvent(any(), anyLong());
        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenActiveEvent_WhenDeleteEvent_ThenThrowException() {
        Event event = createEvent(Event.eventStatus.ACTIVE);

        when(mockEventRepository.getEventById(validEventId)).thenReturn(event);
        when(mockMembershipDomainService.validatePermission(
                validSessionId,
                validCompanyId,
                "event:remove"
        )).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.deleteEvent(validSessionId, validEventId)
        );

        assertEquals("Only inactive or cancelled events can be removed", exception.getMessage());

        verify(mockTokenService).validateToken(validSessionId);
        verify(mockEventRepository, times(1)).getEventById(validEventId);
        verify(mockMembershipDomainService).validatePermission(validSessionId, validCompanyId, "event:remove");
        verify(mockEventRepository, never()).deleteEvent(any(), anyLong());
        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenDraftEvent_WhenDeleteEvent_ThenThrowException() {
        Event event = createEvent(Event.eventStatus.DRAFT);

        when(mockEventRepository.getEventById(validEventId)).thenReturn(event);
        when(mockMembershipDomainService.validatePermission(
                validSessionId,
                validCompanyId,
                "event:remove"
        )).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.deleteEvent(validSessionId, validEventId)
        );

        assertEquals("Only inactive or cancelled events can be removed", exception.getMessage());

        verify(mockTokenService).validateToken(validSessionId);
        verify(mockEventRepository, times(1)).getEventById(validEventId);
        verify(mockMembershipDomainService).validatePermission(validSessionId, validCompanyId, "event:remove");
        verify(mockEventRepository, never()).deleteEvent(any(), anyLong());
        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }
}