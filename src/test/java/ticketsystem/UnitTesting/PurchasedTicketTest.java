package ticketsystem.UnitTesting;


import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import ticketsystem.DomainLayer.history.PurchasedTicket;
import ticketsystem.DomainLayer.history.TicketStatus;

public class PurchasedTicketTest {

    @Test
    public void GivenNewTicket_WhenCreated_ThenStatusIsActive() {
        // Arrange & Act (Setup & Invocation)
        PurchasedTicket ticket =
        new PurchasedTicket(
                1L,
                1,
                1,
                new BigDecimal("150.00"),
                "SECURE-BARCODE-123"
        );;

        // Assert (Assessment)
        assertEquals(TicketStatus.ACTIVE, ticket.getStatus(), "New ticket status should be ACTIVE by default");
    }
}