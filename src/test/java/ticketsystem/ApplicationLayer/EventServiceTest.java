package ticketsystem.ApplicationLayer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ticketsystem.ApplicationLayer.Events.EventUpdatesListener;
import ticketsystem.DTO.Event.ElementDTO;
import ticketsystem.DTO.Event.EventDTO;
import ticketsystem.DTO.Event.EventMapDTO;
import ticketsystem.DTO.Event.IMapElementDTO;
import ticketsystem.DTO.Event.PairDTO;
import ticketsystem.DTO.Event.StandingAreaDTO;
import ticketsystem.DomainLayer.MembershipDomainService;
import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.event.*;
import ticketsystem.DomainLayer.user.Permission;
import ticketsystem.DomainLayer.IRepository.IHistoryRepository;

public class EventServiceTest {

    @Mock
    private IEventRepository mockEventRepository;

    @Mock
    private ITokenService mockTokenService;

    @Mock
    private MembershipDomainService mockMembershipDomainService;

    @Mock
    private EventUpdatesListener mockEventUpdatesListener;

    @Mock
    private ISystemLogger logger;

    @Mock
    private UserAccessService mockUserAccessService;
    private EventService eventService;
    private final String validSessionId = "valid-session";
    private final Long validUserId = 1L;
    private final Long validCompanyId = 1L;
    private final Long validEventId = 1L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        //notification
        eventService = new EventService(
                mockEventRepository,
                mockTokenService,
                mockMembershipDomainService,
                logger,
                mockUserAccessService
        );
        when(mockTokenService.validateToken(validSessionId)).thenReturn(true);
        when(mockTokenService.extractUserId(validSessionId)).thenReturn(validUserId);

        when(mockMembershipDomainService.validatePermission(
                validUserId,
                validCompanyId,
                Permission.MANAGE_EVENT_INVENTORY
        )).thenReturn(true);

        when(mockMembershipDomainService.validatePermission(
                validUserId,
                validCompanyId,
                Permission.CONFIGURE_HALL_AND_MAP
        )).thenReturn(true);
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
                null,
                0.0,
                SaleStatus.ONGOING.name(),
                false,
                0,
                version,
                null
        );
    }

    private EventMapDTO createMapDTO() {
        return new EventMapDTO(
                new PairDTO<>(10, 10),
                List.<IMapElementDTO>of(
                        new StandingAreaDTO(
                                1L,
                                "Standing Area A",
                                new PairDTO<>(1, 1),
                                new PairDTO<>(3, 3),
                                "StandingArea",
                                false,
                                100L,
                                0L,
                                0L
                        )
                ),
                false
        );
    }

    private EventMapDTO createMapWithoutTicketAreaDTO() {
        return new EventMapDTO(
                new PairDTO<>(10, 10),
                List.<IMapElementDTO>of(
                        new ElementDTO(
                                1L,
                                "Stage",
                                new PairDTO<>(1, 1),
                                new PairDTO<>(3, 2),
                                "Stage"
                        )
                ),
                false
        );
    }

    // ------------------ Insert Event Tests ------------------

    @Test
    void GivenValidTokenAndPermission_WhenInsertEvent_ThenAddEventOnce() {
        when(mockEventRepository.getNextId()).thenReturn(validEventId);

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
        verify(mockEventRepository).addEvent(eventCaptor.capture());

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
        verify(mockTokenService).extractUserId(validSessionId);
        verify(mockMembershipDomainService).validatePermission(
                validUserId,
                validCompanyId,
                Permission.MANAGE_EVENT_INVENTORY
        );
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
        verify(mockTokenService, never()).extractUserId(anyString());
        verify(mockMembershipDomainService, never()).validatePermission(
                anyLong(),
                anyLong(),
                any(Permission.class)
        );
        verify(mockEventRepository, never()).addEvent(any(Event.class));

        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenInvalidPermission_WhenInsertEvent_ThenThrowException() {
        when(mockMembershipDomainService.validatePermission(
                validUserId,
                validCompanyId,
                Permission.MANAGE_EVENT_INVENTORY
        )).thenReturn(false);

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
        verify(mockTokenService).extractUserId(validSessionId);
        verify(mockMembershipDomainService).validatePermission(
                validUserId,
                validCompanyId,
                Permission.MANAGE_EVENT_INVENTORY
        );
        verify(mockEventRepository, never()).addEvent(any(Event.class));

        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenEmptyEventName_WhenInsertEvent_ThenThrowException() {
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
        verify(mockTokenService).extractUserId(validSessionId);
        verify(mockMembershipDomainService).validatePermission(
                validUserId,
                validCompanyId,
                Permission.MANAGE_EVENT_INVENTORY
        );
        verify(mockEventRepository, never()).addEvent(any(Event.class));

        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenPastDate_WhenInsertEvent_ThenThrowException() {
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
        verify(mockTokenService).extractUserId(validSessionId);
        verify(mockMembershipDomainService).validatePermission(
                validUserId,
                validCompanyId,
                Permission.MANAGE_EVENT_INVENTORY
        );
        verify(mockEventRepository, never()).addEvent(any(Event.class));

        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenNullLocation_WhenInsertEvent_ThenThrowException() {
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
        verify(mockTokenService).extractUserId(validSessionId);
        verify(mockMembershipDomainService).validatePermission(
                validUserId,
                validCompanyId,
                Permission.MANAGE_EVENT_INVENTORY
        );
        verify(mockEventRepository, never()).addEvent(any(Event.class));

        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenInvalidTrafficThreshold_WhenInsertEvent_ThenThrowException() {
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
        verify(mockTokenService).extractUserId(validSessionId);
        verify(mockMembershipDomainService).validatePermission(
                validUserId,
                validCompanyId,
                Permission.MANAGE_EVENT_INVENTORY
        );
        verify(mockEventRepository, never()).addEvent(any(Event.class));

        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenNullCategory_WhenInsertEvent_ThenThrowException() {
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
        verify(mockTokenService).extractUserId(validSessionId);
        verify(mockMembershipDomainService).validatePermission(
                validUserId,
                validCompanyId,
                Permission.MANAGE_EVENT_INVENTORY
        );
        verify(mockEventRepository, never()).addEvent(any(Event.class));

        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenEmptyArtist_WhenInsertEvent_ThenThrowException() {
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
        verify(mockTokenService).extractUserId(validSessionId);
        verify(mockMembershipDomainService).validatePermission(
                validUserId,
                validCompanyId,
                Permission.MANAGE_EVENT_INVENTORY
        );
        verify(mockEventRepository, never()).addEvent(any(Event.class));

        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenNegativePrice_WhenInsertEvent_ThenThrowException() {
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
        verify(mockTokenService).extractUserId(validSessionId);
        verify(mockMembershipDomainService).validatePermission(
                validUserId,
                validCompanyId,
                Permission.MANAGE_EVENT_INVENTORY
        );
        verify(mockEventRepository, never()).addEvent(any(Event.class));

        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenInvalidMapHeight_WhenInsertEvent_ThenThrowException() {
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
        verify(mockTokenService).extractUserId(validSessionId);
        verify(mockMembershipDomainService).validatePermission(
                validUserId,
                validCompanyId,
                Permission.MANAGE_EVENT_INVENTORY
        );
        verify(mockEventRepository, never()).addEvent(any(Event.class));

        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenInvalidMapWidth_WhenInsertEvent_ThenThrowException() {
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
        verify(mockTokenService).extractUserId(validSessionId);
        verify(mockMembershipDomainService).validatePermission(
                validUserId,
                validCompanyId,
                Permission.MANAGE_EVENT_INVENTORY
        );
        verify(mockEventRepository, never()).addEvent(any(Event.class));

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
        verify(mockTokenService).extractUserId(validSessionId);
        verify(mockMembershipDomainService).validatePermission(
                validUserId,
                validCompanyId,
                Permission.MANAGE_EVENT_INVENTORY
        );
        verify(mockEventRepository).getEventById(validEventId);

        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenSuccessfulUpdateWithNameDateLocation_WhenUpdateEvent_ThenNotifyListeners() {
        Event existingEvent = createEvent(Event.eventStatus.DRAFT);
        eventService.addEventUpdatesListener(mockEventUpdatesListener);

        LocalDateTime updatedDate = LocalDateTime.now().plusDays(20);

        EventDTO eventDTO = createEventDTO(
                validEventId,
                "Updated Event",
                validCompanyId,
                updatedDate,
                EventLocation.NEW_YORK,
                null,
                null,
                null,
                null,
                existingEvent.getVersion()
        );

        when(mockEventRepository.getEventById(validEventId)).thenReturn(existingEvent);

        Boolean result = eventService.updateEvent(validSessionId, eventDTO);

        assertTrue(result);

        verify(mockEventUpdatesListener).onEventUpdated(
                eq(validEventId),
                eq(updatedDate),
                eq(EventLocation.NEW_YORK.name()),
                anyString());
        verify(mockEventRepository).updateEvent(existingEvent);
    }

    @Test
    void GivenUpdateWithoutNameDateLocation_WhenUpdateEvent_ThenDoNotNotifyListeners() {
        Event existingEvent = createEvent(Event.eventStatus.DRAFT);
        eventService.addEventUpdatesListener(mockEventUpdatesListener);

        EventDTO eventDTO = createEventDTO(
                validEventId,
                null,
                validCompanyId,
                null,
                null,
                300L,
                EventCategory.CONCERT,
                "Changed Artist",
                BigDecimal.valueOf(90),
                existingEvent.getVersion()
        );

        when(mockEventRepository.getEventById(validEventId)).thenReturn(existingEvent);

        Boolean result = eventService.updateEvent(validSessionId, eventDTO);

        assertTrue(result);

        verify(mockEventUpdatesListener, never()).onEventUpdated(anyLong(), any(), anyString(), anyString());
        verify(mockEventRepository).updateEvent(existingEvent);
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
        verify(mockTokenService, never()).extractUserId(anyString());
        verify(mockMembershipDomainService, never()).validatePermission(
                anyLong(),
                anyLong(),
                any(Permission.class)
        );
        verify(mockEventRepository, never()).updateEvent(any(Event.class));

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
        verify(mockTokenService, never()).extractUserId(anyString());
        verify(mockMembershipDomainService, never()).validatePermission(
                anyLong(),
                anyLong(),
                any(Permission.class)
        );
        verify(mockEventRepository, never()).updateEvent(any(Event.class));

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
        verify(mockTokenService, never()).extractUserId(anyString());
        verify(mockMembershipDomainService, never()).validatePermission(
                anyLong(),
                anyLong(),
                any(Permission.class)
        );
        verify(mockEventRepository, never()).updateEvent(any(Event.class));

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
        verify(mockTokenService, never()).extractUserId(anyString());
        verify(mockMembershipDomainService, never()).validatePermission(
                anyLong(),
                anyLong(),
                any(Permission.class)
        );
        verify(mockEventRepository, never()).updateEvent(any(Event.class));

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
                validUserId,
                validCompanyId,
                Permission.MANAGE_EVENT_INVENTORY
        )).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.updateEvent(validSessionId, eventDTO)
        );

        assertEquals("User does not have permission to update event", exception.getMessage());

        verify(mockTokenService).validateToken(validSessionId);
        verify(mockTokenService).extractUserId(validSessionId);
        verify(mockMembershipDomainService).validatePermission(
                validUserId,
                validCompanyId,
                Permission.MANAGE_EVENT_INVENTORY
        );
        verify(mockEventRepository, never()).updateEvent(any(Event.class));

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

        when(mockEventRepository.getEventById(validEventId)).thenReturn(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.updateEvent(validSessionId, eventDTO)
        );

        assertEquals("Event not found", exception.getMessage());

        verify(mockTokenService).validateToken(validSessionId);
        verify(mockTokenService).extractUserId(validSessionId);
        verify(mockMembershipDomainService).validatePermission(
                validUserId,
                validCompanyId,
                Permission.MANAGE_EVENT_INVENTORY
        );
        verify(mockEventRepository).getEventById(validEventId);
        verify(mockEventRepository, never()).updateEvent(any(Event.class));

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
                existingEvent.getVersion()
        );

        when(mockMembershipDomainService.validatePermission(
                validUserId,
                differentCompanyId,
                Permission.MANAGE_EVENT_INVENTORY
        )).thenReturn(true);

        when(mockEventRepository.getEventById(validEventId)).thenReturn(existingEvent);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.updateEvent(validSessionId, eventDTO)
        );

        assertEquals("Cannot change event's company", exception.getMessage());

        verify(mockTokenService).validateToken(validSessionId);
        verify(mockTokenService).extractUserId(validSessionId);
        verify(mockMembershipDomainService).validatePermission(
                validUserId,
                differentCompanyId,
                Permission.MANAGE_EVENT_INVENTORY
        );
        verify(mockEventRepository).getEventById(validEventId);
        verify(mockEventRepository, never()).updateEvent(any(Event.class));

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

        when(mockEventRepository.getEventById(validEventId)).thenReturn(existingEvent);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> eventService.updateEvent(validSessionId, eventDTO)
        );

        assertEquals("Event was updated by another request", exception.getMessage());

        verify(mockTokenService).validateToken(validSessionId);
        verify(mockTokenService).extractUserId(validSessionId);
        verify(mockMembershipDomainService).validatePermission(
                validUserId,
                validCompanyId,
                Permission.MANAGE_EVENT_INVENTORY
        );
        verify(mockEventRepository).getEventById(validEventId);
        verify(mockEventRepository, never()).updateEvent(any(Event.class));

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

        when(mockEventRepository.getEventById(validEventId)).thenReturn(existingEvent);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.updateEvent(validSessionId, eventDTO)
        );

        assertEquals("Event date must be in the future", exception.getMessage());

        verify(mockTokenService).validateToken(validSessionId);
        verify(mockTokenService).extractUserId(validSessionId);
        verify(mockMembershipDomainService).validatePermission(
                validUserId,
                validCompanyId,
                Permission.MANAGE_EVENT_INVENTORY
        );
        verify(mockEventRepository).getEventById(validEventId);
        verify(mockEventRepository, never()).updateEvent(any(Event.class));

        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenEmptyName_WhenUpdateEvent_ThenThrowException() {
        Event existingEvent = createEvent(Event.eventStatus.DRAFT);

        EventDTO eventDTO = createEventDTO(
                validEventId,
                " ",
                validCompanyId,
                null,
                null,
                null,
                null,
                null,
                null,
                existingEvent.getVersion()
        );

        when(mockEventRepository.getEventById(validEventId)).thenReturn(existingEvent);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.updateEvent(validSessionId, eventDTO)
        );

        assertEquals("Event name cannot be null or empty", exception.getMessage());

        verify(mockEventRepository, never()).updateEvent(any(Event.class));
    }

    @Test
    void GivenInvalidTrafficThreshold_WhenUpdateEvent_ThenThrowException() {
        Event existingEvent = createEvent(Event.eventStatus.DRAFT);

        EventDTO eventDTO = createEventDTO(
                validEventId,
                null,
                validCompanyId,
                null,
                null,
                0L,
                null,
                null,
                null,
                existingEvent.getVersion()
        );

        when(mockEventRepository.getEventById(validEventId)).thenReturn(existingEvent);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.updateEvent(validSessionId, eventDTO)
        );

        assertEquals("Traffic threshold must be a positive number", exception.getMessage());

        verify(mockEventRepository, never()).updateEvent(any(Event.class));
    }

    @Test
    void GivenEmptyArtist_WhenUpdateEvent_ThenThrowException() {
        Event existingEvent = createEvent(Event.eventStatus.DRAFT);

        EventDTO eventDTO = createEventDTO(
                validEventId,
                null,
                validCompanyId,
                null,
                null,
                null,
                null,
                " ",
                null,
                existingEvent.getVersion()
        );

        when(mockEventRepository.getEventById(validEventId)).thenReturn(existingEvent);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.updateEvent(validSessionId, eventDTO)
        );

        assertEquals("Artist name cannot be null or empty", exception.getMessage());

        verify(mockEventRepository, never()).updateEvent(any(Event.class));
    }

    @Test
    void GivenNegativePrice_WhenUpdateEvent_ThenThrowException() {
        Event existingEvent = createEvent(Event.eventStatus.DRAFT);

        EventDTO eventDTO = createEventDTO(
                validEventId,
                null,
                validCompanyId,
                null,
                null,
                null,
                null,
                null,
                BigDecimal.valueOf(-1),
                existingEvent.getVersion()
        );

        when(mockEventRepository.getEventById(validEventId)).thenReturn(existingEvent);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.updateEvent(validSessionId, eventDTO)
        );

        assertEquals("Price must be a non-negative number", exception.getMessage());

        verify(mockEventRepository, never()).updateEvent(any(Event.class));
    }

    @Test
    void GivenActiveEventAndNewTicketPrice_WhenUpdateEvent_ThenThrowException() {
        Event existingEvent = createEvent(Event.eventStatus.ACTIVE);

        EventDTO eventDTO = createEventDTO(
                validEventId,
                null,
                validCompanyId,
                null,
                null,
                null,
                null,
                null,
                BigDecimal.valueOf(90),
                existingEvent.getVersion()
        );

        when(mockEventRepository.getEventById(validEventId)).thenReturn(existingEvent);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> eventService.updateEvent(validSessionId, eventDTO)
        );

        assertEquals("Cannot change ticket price of an active event", exception.getMessage());

        verify(mockTokenService).validateToken(validSessionId);
        verify(mockTokenService).extractUserId(validSessionId);
        verify(mockMembershipDomainService).validatePermission(
                validUserId,
                validCompanyId,
                Permission.MANAGE_EVENT_INVENTORY
        );
        verify(mockEventRepository).getEventById(validEventId);
        verify(mockEventRepository, never()).updateEvent(any(Event.class));

        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    // ------------------ Define Event Map Tests ------------------

    @Test
    void GivenValidMap_WhenDefineEventMap_ThenMapIsSavedAndEventBecomesActive() {
        Event event = createEvent(Event.eventStatus.DRAFT);
        EventMapDTO mapDTO = createMapDTO();

        when(mockEventRepository.getEventById(validEventId)).thenReturn(event);

        Boolean result = eventService.defineEventMap(validSessionId, validEventId, mapDTO);

        assertTrue(result);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(mockEventRepository).updateEvent(eventCaptor.capture());

        Event updatedEvent = eventCaptor.getValue();

        assertEquals(Event.eventStatus.ACTIVE, updatedEvent.getStatus());
        assertNotNull(updatedEvent.getMap());

        verify(mockTokenService).validateToken(validSessionId);
        verify(mockEventRepository).getEventById(validEventId);
        verify(mockTokenService).extractUserId(validSessionId);
        verify(mockMembershipDomainService).validatePermission(
                validUserId,
                validCompanyId,
                Permission.CONFIGURE_HALL_AND_MAP
        );

        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenMapWithoutSeatingOrStandingArea_WhenDefineEventMap_ThenThrowException() {
        Event event = createEvent(Event.eventStatus.DRAFT);

        when(mockEventRepository.getEventById(validEventId)).thenReturn(event);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.defineEventMap(
                        validSessionId,
                        validEventId,
                        createMapWithoutTicketAreaDTO()
                )
        );

        assertEquals(
                "Event map must contain at least one seating area or standing area",
                exception.getMessage()
        );

        verify(mockTokenService).validateToken(validSessionId);
        verify(mockEventRepository).getEventById(validEventId);
        verify(mockTokenService).extractUserId(validSessionId);
        verify(mockMembershipDomainService).validatePermission(
                validUserId,
                validCompanyId,
                Permission.CONFIGURE_HALL_AND_MAP
        );
        verify(mockEventRepository, never()).updateEvent(any(Event.class));

        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenInvalidToken_WhenDefineEventMap_ThenThrowException() {
        String sessionId = "invalid-session";
        when(mockTokenService.validateToken(sessionId)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.defineEventMap(sessionId, validEventId, createMapDTO())
        );

        assertEquals("Invalid session ID", exception.getMessage());

        verify(mockTokenService).validateToken(sessionId);
        verify(mockEventRepository, never()).getEventById(anyLong());
        verify(mockTokenService, never()).extractUserId(anyString());
        verify(mockMembershipDomainService, never()).validatePermission(
                anyLong(),
                anyLong(),
                any(Permission.class)
        );
        verify(mockEventRepository, never()).updateEvent(any(Event.class));

        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenEventNotFound_WhenDefineEventMap_ThenThrowException() {
        when(mockEventRepository.getEventById(validEventId)).thenReturn(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.defineEventMap(validSessionId, validEventId, createMapDTO())
        );

        assertEquals("Event not found", exception.getMessage());

        verify(mockTokenService).validateToken(validSessionId);
        verify(mockEventRepository).getEventById(validEventId);
        verify(mockTokenService, never()).extractUserId(anyString());
        verify(mockMembershipDomainService, never()).validatePermission(
                anyLong(),
                anyLong(),
                any(Permission.class)
        );
        verify(mockEventRepository, never()).updateEvent(any(Event.class));

        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenNoPermission_WhenDefineEventMap_ThenThrowException() {
        Event event = createEvent(Event.eventStatus.DRAFT);

        when(mockEventRepository.getEventById(validEventId)).thenReturn(event);
        when(mockMembershipDomainService.validatePermission(
                validUserId,
                validCompanyId,
                Permission.CONFIGURE_HALL_AND_MAP
        )).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.defineEventMap(validSessionId, validEventId, createMapDTO())
        );

        assertEquals("User does not have permission to define event map", exception.getMessage());

        verify(mockTokenService).validateToken(validSessionId);
        verify(mockEventRepository).getEventById(validEventId);
        verify(mockTokenService).extractUserId(validSessionId);
        verify(mockMembershipDomainService).validatePermission(
                validUserId,
                validCompanyId,
                Permission.CONFIGURE_HALL_AND_MAP
        );
        verify(mockEventRepository, never()).updateEvent(any(Event.class));

        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenNullMapDTO_WhenDefineEventMap_ThenThrowException() {
        Event event = createEvent(Event.eventStatus.DRAFT);

        when(mockEventRepository.getEventById(validEventId)).thenReturn(event);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.defineEventMap(validSessionId, validEventId, null)
        );

        assertEquals("Map data cannot be null", exception.getMessage());

        verify(mockTokenService).validateToken(validSessionId);
        verify(mockEventRepository).getEventById(validEventId);
        verify(mockTokenService).extractUserId(validSessionId);
        verify(mockMembershipDomainService).validatePermission(
                validUserId,
                validCompanyId,
                Permission.CONFIGURE_HALL_AND_MAP
        );
        verify(mockEventRepository, never()).updateEvent(any(Event.class));

        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    // ------------------ Get Event Map Tests ------------------

    @Test
    void GivenValidEvent_WhenGetEventMap_ThenReturnMapDTO() {
        Event event = createEvent(Event.eventStatus.ACTIVE);
        when(mockEventRepository.getEventById(validEventId)).thenReturn(event);

        EventMapDTO result = eventService.getEventMap(validSessionId, validEventId);

        assertNotNull(result);

        verify(mockTokenService).validateToken(validSessionId);
        verify(mockEventRepository).getEventById(validEventId);

        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenInvalidToken_WhenGetEventMap_ThenThrowException() {
        String sessionId = "invalid-session";
        when(mockTokenService.validateToken(sessionId)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.getEventMap(sessionId, validEventId)
        );

        assertEquals("Invalid session ID", exception.getMessage());

        verify(mockTokenService).validateToken(sessionId);
        verify(mockEventRepository, never()).getEventById(anyLong());

        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenEventNotFound_WhenGetEventMap_ThenThrowException() {
        when(mockEventRepository.getEventById(validEventId)).thenReturn(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.getEventMap(validSessionId, validEventId)
        );

        assertEquals("Event not found", exception.getMessage());

        verify(mockTokenService).validateToken(validSessionId);
        verify(mockEventRepository).getEventById(validEventId);

        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    // ------------------ Delete Event Tests ------------------

    @Test
    void GivenInactiveEvent_WhenDeleteEvent_ThenEventIsDeleted() {
        Event event = createEvent(Event.eventStatus.INACTIVE);

        when(mockEventRepository.getEventById(validEventId)).thenReturn(event);

        Boolean result = eventService.deleteEvent(validSessionId, validEventId);

        assertTrue(result);

        verify(mockTokenService).validateToken(validSessionId);
        verify(mockTokenService).extractUserId(validSessionId);
        verify(mockEventRepository, times(2)).getEventById(validEventId);
        verify(mockMembershipDomainService).validatePermission(
                validUserId,
                validCompanyId,
                Permission.MANAGE_EVENT_INVENTORY
        );
        verify(mockEventRepository).deleteEvent(validEventId, event.getVersion());

        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenCancelledEvent_WhenDeleteEvent_ThenEventIsDeleted() {
        Event event = createEvent(Event.eventStatus.CANCELLED);

        when(mockEventRepository.getEventById(validEventId)).thenReturn(event);

        Boolean result = eventService.deleteEvent(validSessionId, validEventId);

        assertTrue(result);

        verify(mockTokenService).validateToken(validSessionId);
        verify(mockTokenService).extractUserId(validSessionId);
        verify(mockEventRepository, times(2)).getEventById(validEventId);
        verify(mockMembershipDomainService).validatePermission(
                validUserId,
                validCompanyId,
                Permission.MANAGE_EVENT_INVENTORY
        );
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
        verify(mockTokenService, never()).extractUserId(anyString());
        verify(mockEventRepository, never()).getEventById(anyLong());
        verify(mockEventRepository, never()).deleteEvent(anyLong(), anyLong());
        verify(mockMembershipDomainService, never()).validatePermission(
                anyLong(),
                anyLong(),
                any(Permission.class)
        );

        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenEventDoesNotExist_WhenDeleteEvent_ThenThrowException() {
        when(mockEventRepository.getEventById(validEventId)).thenReturn(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.deleteEvent(validSessionId, validEventId)
        );

        assertEquals("Event does not exist", exception.getMessage());

        verify(mockTokenService).validateToken(validSessionId);
        verify(mockEventRepository).getEventById(validEventId);
        verify(mockTokenService, never()).extractUserId(anyString());
        verify(mockMembershipDomainService, never()).validatePermission(
                anyLong(),
                anyLong(),
                any(Permission.class)
        );
        verify(mockEventRepository, never()).deleteEvent(anyLong(), anyLong());

        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenNoPermission_WhenDeleteEvent_ThenThrowException() {
        Event event = createEvent(Event.eventStatus.INACTIVE);

        when(mockEventRepository.getEventById(validEventId)).thenReturn(event);
        when(mockMembershipDomainService.validatePermission(
                validUserId,
                validCompanyId,
                Permission.MANAGE_EVENT_INVENTORY
        )).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.deleteEvent(validSessionId, validEventId)
        );

        assertEquals("User does not have permission to remove an event", exception.getMessage());

        verify(mockTokenService).validateToken(validSessionId);
        verify(mockTokenService).extractUserId(validSessionId);
        verify(mockEventRepository).getEventById(validEventId);
        verify(mockMembershipDomainService).validatePermission(
                validUserId,
                validCompanyId,
                Permission.MANAGE_EVENT_INVENTORY
        );
        verify(mockEventRepository, never()).deleteEvent(anyLong(), anyLong());

        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenActiveEvent_WhenDeleteEvent_ThenThrowException() {
        Event event = createEvent(Event.eventStatus.ACTIVE);

        when(mockEventRepository.getEventById(validEventId)).thenReturn(event);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.deleteEvent(validSessionId, validEventId)
        );

        assertEquals("Only inactive or cancelled events can be removed", exception.getMessage());

        verify(mockTokenService).validateToken(validSessionId);
        verify(mockTokenService).extractUserId(validSessionId);
        verify(mockEventRepository).getEventById(validEventId);
        verify(mockMembershipDomainService).validatePermission(
                validUserId,
                validCompanyId,
                Permission.MANAGE_EVENT_INVENTORY
        );
        verify(mockEventRepository, never()).deleteEvent(anyLong(), anyLong());

        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenDraftEvent_WhenDeleteEvent_ThenThrowException() {
        Event event = createEvent(Event.eventStatus.DRAFT);

        when(mockEventRepository.getEventById(validEventId)).thenReturn(event);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.deleteEvent(validSessionId, validEventId)
        );

        assertEquals("Only inactive or cancelled events can be removed", exception.getMessage());

        verify(mockTokenService).validateToken(validSessionId);
        verify(mockTokenService).extractUserId(validSessionId);
        verify(mockEventRepository).getEventById(validEventId);
        verify(mockMembershipDomainService).validatePermission(
                validUserId,
                validCompanyId,
                Permission.MANAGE_EVENT_INVENTORY
        );
        verify(mockEventRepository, never()).deleteEvent(anyLong(), anyLong());

        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    // ------------------ Cancel Event Tests ------------------

   @Test
void GivenActiveEvent_WhenCancelEvent_ThenEventIsCancelledAndUpdated() {
    Event event = createEvent(Event.eventStatus.ACTIVE);

    when(mockEventRepository.getEventById(validEventId))
            .thenReturn(event);

    when(mockEventUpdatesListener.onEventCancellationRequested(validEventId))
            .thenReturn(true);

    eventService.addEventUpdatesListener(mockEventUpdatesListener);

    Boolean result = eventService.cancelEvent(validSessionId, validEventId);

    assertTrue(result);
    assertEquals(Event.eventStatus.CANCELLED, event.getStatus());

    verify(mockTokenService).validateToken(validSessionId);
    verify(mockTokenService).extractUserId(validSessionId);

    verify(mockEventRepository, times(2)).getEventById(validEventId);

    verify(mockMembershipDomainService).validatePermission(
            validUserId,
            validCompanyId,
            Permission.MANAGE_EVENT_INVENTORY
    );

    verify(mockEventRepository, times(2)).updateEvent(event);

    verify(mockEventUpdatesListener).onEventCancellationRequested(validEventId);
    verify(mockEventUpdatesListener).onEventCanceled(validEventId);

    verifyNoMoreInteractions(
            mockEventRepository,
            mockTokenService,
            mockMembershipDomainService,
            mockEventUpdatesListener
    );
}

    @Test
    void GivenActiveEventAndNoListener_WhenCancelEvent_ThenEventIsCancelledAndUpdated() {
        Event event = createEvent(Event.eventStatus.ACTIVE);
        when(mockEventRepository.getEventById(validEventId)).thenReturn(event);

        Boolean result = eventService.cancelEvent(validSessionId, validEventId);

        assertTrue(result);
        assertEquals(Event.eventStatus.CANCELLED, event.getStatus());

        verify(mockEventRepository, times(2)).updateEvent(event);
    }

    @Test
    void GivenInvalidToken_WhenCancelEvent_ThenThrowException() {
        String sessionId = "invalid-session";
        when(mockTokenService.validateToken(sessionId)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.cancelEvent(sessionId, validEventId)
        );

        assertEquals("Invalid session ID", exception.getMessage());

        verify(mockTokenService).validateToken(sessionId);
        verify(mockEventRepository, never()).getEventById(anyLong());
        verify(mockTokenService, never()).extractUserId(anyString());
        verify(mockMembershipDomainService, never()).validatePermission(
                anyLong(),
                anyLong(),
                any(Permission.class)
        );
        verify(mockEventRepository, never()).updateEvent(any(Event.class));

        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenEventDoesNotExist_WhenCancelEvent_ThenThrowException() {
        when(mockEventRepository.getEventById(validEventId)).thenReturn(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.cancelEvent(validSessionId, validEventId)
        );

        assertEquals("Event does not exist", exception.getMessage());

        verify(mockTokenService).validateToken(validSessionId);
        verify(mockEventRepository).getEventById(validEventId);
        verify(mockTokenService, never()).extractUserId(anyString());
        verify(mockMembershipDomainService, never()).validatePermission(
                anyLong(),
                anyLong(),
                any(Permission.class)
        );
        verify(mockEventRepository, never()).updateEvent(any(Event.class));

        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenNoPermission_WhenCancelEvent_ThenThrowException() {
        Event event = createEvent(Event.eventStatus.ACTIVE);

        when(mockEventRepository.getEventById(validEventId)).thenReturn(event);
        when(mockMembershipDomainService.validatePermission(
                validUserId,
                validCompanyId,
                Permission.MANAGE_EVENT_INVENTORY
        )).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.cancelEvent(validSessionId, validEventId)
        );

        assertEquals("User does not have permission to cancel an event", exception.getMessage());

        verify(mockTokenService).validateToken(validSessionId);
        verify(mockTokenService).extractUserId(validSessionId);
        verify(mockEventRepository).getEventById(validEventId);
        verify(mockMembershipDomainService).validatePermission(
                validUserId,
                validCompanyId,
                Permission.MANAGE_EVENT_INVENTORY
        );
        verify(mockEventRepository, never()).updateEvent(any(Event.class));

        verifyNoMoreInteractions(mockEventRepository, mockTokenService, mockMembershipDomainService);
    }

    @Test
    void GivenAlreadyCancelledEvent_WhenCancelEvent_ThenThrowException() {
        Event event = createEvent(Event.eventStatus.CANCELLED);
        when(mockEventRepository.getEventById(validEventId)).thenReturn(event);

        eventService.addEventUpdatesListener(mockEventUpdatesListener);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> eventService.cancelEvent(validSessionId, validEventId)
        );

        assertEquals("Event is already canceled", exception.getMessage());

        verify(mockTokenService).validateToken(validSessionId);
        verify(mockTokenService).extractUserId(validSessionId);
        verify(mockEventRepository).getEventById(validEventId);
        verify(mockMembershipDomainService).validatePermission(
                validUserId,
                validCompanyId,
                Permission.MANAGE_EVENT_INVENTORY
        );
        verify(mockEventRepository, never()).updateEvent(any(Event.class));
        verify(mockEventUpdatesListener, never()).onEventCanceled(anyLong());

        verifyNoMoreInteractions(
                mockEventRepository,
                mockTokenService,
                mockMembershipDomainService,
                mockEventUpdatesListener
        );
    }
}