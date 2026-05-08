package ticketsystem.UnitTesting;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ticketsystem.ApplicationLayer.EventService;
import ticketsystem.ApplicationLayer.ITokenService;
import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.event.EventCategory;
import ticketsystem.DomainLayer.event.Pair;
import ticketsystem.DomainLayer.MembershipDomainService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

public class EventServiceTest {

    @Mock
    private IEventRepository mockEventRepository;
    @Mock
    private ITokenService mockTokenService;
    @Mock
    private MembershipDomainService mockMembershipDomainService;

    private EventService EventService;
    private final String validSessionId = "valid-session";
    private final Long validCompanyId = 1L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        EventService = new EventService(
            mockEventRepository,
            mockTokenService,
            mockMembershipDomainService);
        when(mockTokenService.validateToken(validSessionId)).thenReturn(true);
        when(mockMembershipDomainService.validatePermission(
            validSessionId,
            validCompanyId,
            "event:create")).thenReturn(true);
    }

    @Test
    void GivenValidTokenAndPermission_WhenInsertEvent_ThenAddEventOnce() {
        when(mockTokenService.extractUserId(validSessionId)).thenReturn(1L);
        when(mockEventRepository.getNextId()).thenReturn(1L);

        LocalDateTime futureDate = LocalDateTime.now().plusDays(1);

        // Act + Assert
        assertDoesNotThrow(() -> EventService.insertEvent(
            validSessionId,
            "Test Event",
            validCompanyId,
            futureDate,
            "Test Location",
            100L,
            EventCategory.CONCERT,
            new Pair<>(10, 10)));
        verify(mockEventRepository, times(1)).addEvent(any());
    }

    @Test
    void GivenInvalidToken_WhenInsertEvent_ThenThrowException() {
        // Arrange
        String sessionId = "invalidSessionId";

        when(mockTokenService.validateToken(sessionId)).thenReturn(false);
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> EventService.insertEvent(
                sessionId,
                "Test Event",
                validCompanyId,
                LocalDateTime.now().plusDays(1),
                "Test Location",
                100L,
                EventCategory.CONCERT,
                new Pair<>(10, 10)));
        assertEquals("Invalid session ID", exception.getMessage());
        verify(mockEventRepository, times(0)).addEvent(any());
    }

    @Test
    void GivenInvalidPermission_WhenInsertEvent_ThenThrowException() {
        // Arrange
        when(mockMembershipDomainService.validatePermission(
            validSessionId,
            validCompanyId,
            "event:create")).thenReturn(false);
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> EventService.insertEvent(
                validSessionId,
                "Test Event",
                validCompanyId,
                LocalDateTime.now().plusDays(1),
                "Test Location",
                100L,
                EventCategory.CONCERT,
                new Pair<>(10, 10)));
        assertEquals("User does not have permission to create an event", exception.getMessage());
        verify(mockEventRepository, times(0)).addEvent(any());
    }

    @Test
    void GivenInvalidEventName_WhenInsertEvent_ThenThrowException() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> EventService.insertEvent(
                validSessionId,
                "",
                validCompanyId,
                LocalDateTime.now().plusDays(1),
                "Test Location",
                100L,
                EventCategory.CONCERT,
                new Pair<>(10, 10)));
        assertNotNull(exception);
        assertEquals("Event name cannot be null or empty", exception.getMessage());
        verify(mockEventRepository, times(0)).addEvent(any());
    }

    @Test
    void GivenInvalidEventDate_WhenInsertEvent_ThenThrowException() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> EventService.insertEvent(
                validSessionId,
                "Test Event",
                validCompanyId,
                LocalDateTime.now().minusDays(1),
                "Test Location",
                100L,
                EventCategory.CONCERT,
                new Pair<>(10, 10)));
        assertEquals("Event date must be in the future", exception.getMessage());
        verify(mockEventRepository, times(0)).addEvent(any());
    }

    @Test
    void GivenInvalidEventLocation_WhenInsertEvent_ThenThrowException() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> EventService.insertEvent(
                validSessionId,
                "Test Event",
                validCompanyId,
                LocalDateTime.now().plusDays(1),
                "",
                100L,
                EventCategory.CONCERT,
                new Pair<>(10, 10)));
        assertEquals("Event location cannot be null or empty", exception.getMessage());
        verify(mockEventRepository, times(0)).addEvent(any());
    }

    @Test
    void GivenInvalidTrafficThreshold_WhenInsertEvent_ThenThrowException() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> EventService.insertEvent(
                validSessionId,
                "Test Event",
                validCompanyId,
                LocalDateTime.now().plusDays(1),
                "Test Location",
                -1L,
                EventCategory.CONCERT,
                new Pair<>(10, 10)));
        assertEquals("Traffic threshold must be a positive number", exception.getMessage());
        verify(mockEventRepository, times(0)).addEvent(any());
    }

    @Test
    void GivenInvalidEventCategory_WhenInsertEvent_ThenThrowException() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> EventService.insertEvent(
                validSessionId,
                "Test Event",
                validCompanyId,
                LocalDateTime.now().plusDays(1),
                "Test Location",
                100L,
                null,
                new Pair<>(10, 10)));
        assertEquals("Event category cannot be null", exception.getMessage());
        verify(mockEventRepository, times(0)).addEvent(any());
    }

    @Test
    void GivenInvalidMapSize_WhenInsertEvent_ThenThrowException() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> EventService.insertEvent(
                validSessionId,
                "Test Event",
                validCompanyId,
                LocalDateTime.now().plusDays(1),
                "Test Location",
                100L,
                EventCategory.CONCERT,
                new Pair<>(-1, -1)));
        assertEquals("Map size must be positive", exception.getMessage());
        verify(mockEventRepository, times(0)).addEvent(any());
    }








}


