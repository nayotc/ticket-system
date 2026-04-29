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
import ticketsystem.DTO.TicketDTO;
import ticketsystem.DomainLayer.IRepository.IHistoryRepository;
import ticketsystem.DomainLayer.history.Purchase;
import ticketsystem.DomainLayer.history.PurchasedTicket;

public class HistoryServiceTest {

    @Mock
    private IHistoryRepository historyRepository;
    
    @Mock
    private TokenService tokenService;
    
    private HistoryService historyService;

    // Simulation of a logged-in user and tokens
    private final int user1Id = 100;
    private final String validToken = "valid-token-123";
    private final String invalidToken = "invalid-token-456";

    @BeforeEach
    void setUp() {
        // Initializes the mocks and the service before each test
        MockitoAnnotations.openMocks(this);
        // Injecting both mocks into the service
        historyService = new HistoryService(historyRepository, tokenService);
    }

    /**
     * 3.5 View personal purchase history - Successful Scenario
     */
    @Test
    void GivenValidTokenAndExistingHistory_WhenGetHistoryForUser_ThenReturnsHistory() {
        // --- Given (Arrange) ---
        // 1. Valid token is provided
        when(tokenService.validateToken(validToken)).thenReturn(true);
        
        // 2. Purchase history exists for user1
        List<Purchase> fakeHistory = new ArrayList<>();
        List<PurchasedTicket> tickets = new ArrayList<>(); 
        tickets.add(new PurchasedTicket(10, 20, 1, 1, 150.0)); // adding a fake ticket
        
        Purchase p = new Purchase(1, tickets, "Taylor Swift Tour", "HaYarkon Park", user1Id, 50);
        fakeHistory.add(p);
        
        // Mocking the repository to return the fake history
        when(historyRepository.getPurchasesByMemberId(user1Id)).thenReturn(fakeHistory);

        // --- When (Act) ---
        List<OrderDTO> result = historyService.getHistoryForUser(user1Id, validToken);

        // --- Then (Assert) ---
        assertNotNull(result, "The result should not be null");
        assertFalse(result.isEmpty(), "The history list should not be empty");
        assertEquals(1, result.size(), "User should have exactly 1 purchase record");
        assertEquals("Taylor Swift Tour", result.get(0).getEventName(), "Event name should match");
        assertEquals(1, result.get(0).getTickets().size(), "Should have exactly 1 ticket in the DTO");
        
        // Verify interactions
        verify(tokenService, times(1)).validateToken(validToken);
        verify(historyRepository, times(1)).getPurchasesByMemberId(user1Id);
    }

    /**
     * 3.5 View personal purchase history - Failure (Empty) Scenario
     */
    @Test
    void GivenValidTokenAndNoHistory_WhenGetHistoryForUser_ThenReturnsEmptyList() {
        // --- Given (Arrange) ---
        when(tokenService.validateToken(validToken)).thenReturn(true);
        when(historyRepository.getPurchasesByMemberId(user1Id)).thenReturn(new ArrayList<>());

        // --- When (Act) ---
        List<OrderDTO> result = historyService.getHistoryForUser(user1Id, validToken);

        // --- Then (Assert) ---
        assertNotNull(result, "Result should be an empty list, not null");
        assertTrue(result.isEmpty(), "History should be empty when user has no purchases");
        
        // Verify interactions
        verify(tokenService, times(1)).validateToken(validToken);
        verify(historyRepository, times(1)).getPurchasesByMemberId(user1Id);
    }

    /**
     * 3.5 View personal purchase history - Unauthorized Scenario
     */
    @Test
    void GivenInvalidToken_WhenGetHistoryForUser_ThenThrowsIllegalArgumentException() {
        // --- Given (Arrange) ---
        when(tokenService.validateToken(invalidToken)).thenReturn(false);

        // --- When & Then (Act & Assert) ---
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            historyService.getHistoryForUser(user1Id, invalidToken);
        });
        
        assertEquals("Invalid or expired token", exception.getMessage());
        
        // Verify that the repository was NEVER called because the token was invalid
        verify(historyRepository, never()).getPurchasesByMemberId(anyInt());
    }

    /**
     * Add Purchase - Successful Scenario
     */
    @Test
    void GivenValidTokenAndOrderDto_WhenAddPurchase_ThenRepositoryAddPurchaseIsCalled() {
        // --- Given (Arrange) ---
        when(tokenService.validateToken(validToken)).thenReturn(true);
        
        List<TicketDTO> ticketDTOs = new ArrayList<>();
        ticketDTOs.add(new TicketDTO(10, 20, 1, 1, 150.0, "ACTIVE"));
        OrderDTO orderDto = new OrderDTO(1, ticketDTOs, "Rock Concert", "Barby", user1Id, 5);

        // --- When (Act) ---
        historyService.addPurchase(orderDto, validToken);

        // --- Then (Assert) ---
        verify(tokenService, times(1)).validateToken(validToken);
        // Verifying that the repository method was called exactly once with any Purchase object
        verify(historyRepository, times(1)).addPurchase(any(Purchase.class));
    }

    /**
     * Add Purchase - Unauthorized Scenario
     */
    @Test
    void GivenInvalidToken_WhenAddPurchase_ThenThrowsIllegalArgumentException() {
        // --- Given (Arrange) ---
        when(tokenService.validateToken(invalidToken)).thenReturn(false);
        OrderDTO orderDto = new OrderDTO(1, new ArrayList<>(), "Rock Concert", "Barby", user1Id, 5);

        // --- When & Then (Act & Assert) ---
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            historyService.addPurchase(orderDto, invalidToken);
        });
        
        assertEquals("Invalid or expired token", exception.getMessage());
        
        // Verify that the repository was NEVER called because the token was invalid
        verify(historyRepository, never()).addPurchase(any(Purchase.class));
    }
}