package ticketsystem.UnitTesting;


import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import ticketsystem.DomainLayer.history.PurchasedTicket;
import ticketsystem.DomainLayer.history.TicketStatus;

public class PurchasedTicketTest {

    @Test
    public void GivenNewTicket_WhenCreated_ThenStatusIsActive() {
        // Arrange & Act (Setup & Invocation)
        PurchasedTicket ticket = new PurchasedTicket(1, 101, 1, 1, 150.0, "SECURE-BARCODE-123");

        // Assert (Assessment)
        assertEquals(TicketStatus.ACTIVE, ticket.getStatus(), "New ticket status should be ACTIVE by default");
    }
}