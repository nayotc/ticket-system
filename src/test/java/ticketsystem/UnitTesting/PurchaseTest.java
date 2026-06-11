package ticketsystem.UnitTesting;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import ticketsystem.DomainLayer.history.PaymentDetails;
import ticketsystem.DomainLayer.history.Purchase;
import ticketsystem.DomainLayer.history.PurchasedTicket;

import static org.junit.jupiter.api.Assertions.*; 

public class PurchaseTest {

    @Test
    public void GivenExternalList_WhenPurchaseCreated_ThenInternalListNotModifiedByExternalChanges() {
        // Arrange (Setup)
        List<PurchasedTicket> originalList = new ArrayList<>();
        PurchasedTicket ticket = new PurchasedTicket(1, 1, 1, 150.0,"SECURE-BARCODE-123");
        originalList.add(ticket);
        
        Purchase purchase = new Purchase(1L, originalList, "Rock Concert", "Tel Aviv", 10L, 5L, 1L, 101L,new BigDecimal(100), 111111);

        // Act (Invocation)

        originalList.clear(); 

        // Assert (Assessment)
        assertFalse(purchase.getTickets().isEmpty(), "Internal list should not be empty"); 
        assertEquals(1, purchase.getTickets().size(), "Internal list should still have 1 ticket"); 
    }

    @Test
    public void GivenPurchase_WhenGetTicketsCalled_ThenReturnedListModificationsDoNotAffectPurchase() {
        // Arrange (Setup)
        List<PurchasedTicket> originalList = new ArrayList<>();
        originalList.add(new PurchasedTicket(1, 1, 1, 150.0, "SECURE-BARCODE-123"));
        Purchase purchase = new Purchase(1L, originalList, "Rock Concert", "Tel Aviv", 10L, 5L, 1L, 101L,new BigDecimal(100), 111111);

        // Act (Invocation)
        List<PurchasedTicket> retrievedList = purchase.getTickets();
        retrievedList.clear(); 

        // Assert (Assessment)
        assertFalse(purchase.getTickets().isEmpty(), "Original purchase tickets should not be affected by getter modifications");
    }
}