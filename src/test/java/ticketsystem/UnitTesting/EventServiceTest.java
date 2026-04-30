package ticketsystem.UnitTesting;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    private IEventRepository mockEventRepository;
    private EventService EventService;
    private ITokenService mockTokenService;
    private MembershipDomainService mockMembershipDomainService;

    @BeforeEach
    void setUp() {
        mockEventRepository = mock(IEventRepository.class);
        mockTokenService = mock(ITokenService.class);
        mockMembershipDomainService = mock(MembershipDomainService.class);

        EventService = new EventService(
                mockEventRepository,
                mockTokenService,
                mockMembershipDomainService
        );
    }

    @Test
    void GivenValidTokenAndPermission_WhenInsertEvent_ThenAddEventOnce() {
        // Arrange
        String sessionId = "validSessionId";
        Long companyId = 1L;

        when(mockTokenService.validateToken(sessionId)).thenReturn(true);
        when(mockTokenService.extractSubject(sessionId)).thenReturn("1");

        when(mockMembershipDomainService.validatePermission(
                sessionId,
                companyId,
                "event:create"
        )).thenReturn(true);

        when(mockEventRepository.getNextId()).thenReturn(1L);

        LocalDateTime futureDate = LocalDateTime.now().plusDays(1);

        // Act + Assert
        assertDoesNotThrow(() ->
                EventService.insertEvent(
                        sessionId,
                        "Test Event",
                        companyId,
                        futureDate,
                        "Test Location",
                        100L,
                        EventCategory.CONCERT,
                        new Pair<>(10, 10)
                )
        );

        verify(mockEventRepository, times(1)).addEvent(any());
    }

    
}
