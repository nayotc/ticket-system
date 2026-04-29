package ticketsystem.UnitTesting;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

import ticketsystem.ApplicationLayer.EventService;
import ticketsystem.ApplicationLayer.ITokenService;
import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.event.EventCategory;
import ticketsystem.DomainLayer.event.Pair;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EventServiceTest {
    private IEventRepository mockEventRepository;
    private EventService mockEventService;
    private ITokenService mockTokenService;
    private MembershipService mockMembershipService;

    @Test
    void TestInsertEvent_returnValidTokenAndAddOnce() {
        // Arrange: set up the mock behavior
        // Create a mock instance of the repository to isolate the service logic
        mockEventRepository = mock(IEventRepository.class);
        mockTokenService = mock(ITokenService.class);
        mockEventService = new EventService(mockEventRepository, mockTokenService);
        mockMembershipService = mock(MembershipService.class);
        when(mockMembershipService.validatePermission(anyString(), any(Long.class), anyString())).thenReturn(true);
        when(mockTokenService.validateToken(anyString())).thenReturn(true);
        when(mockTokenService.extractSubject(anyString())).thenReturn(String.valueOf(1L));  // TODO: remove casting
        when(mockEventRepository.getMaxId()).thenReturn(1L);
        EventCategory category = EventCategory.CONCERT;
        // Act: invoke the method under test
        mockEventService.insertEvent("validSessionId", "Test Event", 1L, null, "Test Location", 100L, category, new Pair<>(10, 10));

        // Assert:
        verify(mockEventRepository, times(1)).addEvent(any());
    }

    
}
