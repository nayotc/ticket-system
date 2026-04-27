package ticketsystem.ApplicationLayer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ticketsystem.DTO.OrderDTO;
import ticketsystem.DomainLayer.IRepository.IHistoryRepository;
import ticketsystem.DomainLayer.order.Purchase;
import ticketsystem.DomainLayer.order.Ticket;


public class HistoryServiceTest {

    @Mock
    private IHistoryRepository historyRepository;
    
    private HistoryService historyService;

    // Simulation of a logged-in user
    private final int user1Id = 100;

    @BeforeEach
    void setUp() {
        // Initializes the mocks and the service before each test
        MockitoAnnotations.openMocks(this);
        historyService = new HistoryService(historyRepository);
    }

    /**
     * 3.5 View personal purchase history - Successful Scenario
     */
    @Test
    void viewHistory_Success_ReturnsHistory() {
        // --- Given ---
        // 1. user1 is created (via user1Id)
        // 2. user1 is logged in (simulated)
        // 3. purchase history exists for user1
        List<Purchase> fakeHistory = new ArrayList<>();
        List<Ticket> tickets = new ArrayList<>(); 
        
        // Creating a purchase using the 6-parameter constructor:
        // (orderId, tickets, eventName, location, memberId, companyId)
        Purchase p = new Purchase(1, tickets, "Taylor Swift Tour", "HaYarkon Park", user1Id, 50);
        fakeHistory.add(p);
        
        // Mocking the repository to return the fake history
        when(historyRepository.getPurchasesByMemberId(user1Id)).thenReturn(fakeHistory);

        // --- When ---
        // user1 requests to view his purchase history
        List<OrderDTO> result = historyService.getHistoryForUser(user1Id);

        // --- Then ---
        // 1. purchase history is presented to user1
        assertNotNull(result, "The result should not be null");
        assertFalse(result.isEmpty(), "The history list should not be empty");
        assertEquals(1, result.size(), "User should have exactly 1 purchase record");
        assertEquals("Taylor Swift Tour", result.get(0).getEventName(), "Event name should match");
        
        // Verify repository interaction
        verify(historyRepository, times(1)).getPurchasesByMemberId(user1Id);
    }

    /**
     * 3.5 View personal purchase history - Failure (Empty) Scenario
     */
    @Test
    void viewHistory_Failure_EmptyHistoryNotification() {
        // --- Given ---
        // 1. user1 is created
        // 2. user1 is logged in
        // 3. purchase history doesn't exist for this user
        when(historyRepository.getPurchasesByMemberId(user1Id)).thenReturn(new ArrayList<>());

        // --- When ---
        // user1 requests to view his purchase history
        List<OrderDTO> result = historyService.getHistoryForUser(user1Id);

        // --- Then ---
        // 1. purchase history is presented to user1 (as an empty list)
        // 2. user1 is notified that no purchase history is available (via empty list logic)
        assertNotNull(result, "Result should be an empty list, not null");
        assertTrue(result.isEmpty(), "History should be empty when user has no purchases");
        
        // Verify repository interaction
        verify(historyRepository, times(1)).getPurchasesByMemberId(user1Id);
    }
}