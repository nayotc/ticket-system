package ticketsystem.IntegrationTesting;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ticketsystem.DTO.PaymentDetails;
import ticketsystem.DTO.TicketIssueRequest;
import ticketsystem.InfrastructureLayer.ExternalPaymentService;
import ticketsystem.InfrastructureLayer.ExternalTicketIssuingService;

@SpringBootTest
@Tag("integration")
public class ExternalSystemsIntegrationTest {

    @Autowired
    private ExternalPaymentService paymentService;

    @Autowired
    private ExternalTicketIssuingService ticketService;

    @Test
    void GivenExternalSystem_WhenHandshake_ThenReturnsTrue() {
        boolean result = paymentService.handshake();

        assertTrue(result);
    }

    @Test
    void GivenValidPaymentDetails_WhenPay_ThenReturnsTransactionId() {
        PaymentDetails details = createValidPaymentDetails();

        Integer transactionId =
                paymentService.pay(BigDecimal.valueOf(1000), details);

        assertNotNull(transactionId);
        assertTrue(transactionId > 0);
    }

    @Test
    void GivenValidTransaction_WhenRefund_ThenReturnsTrue() {
        PaymentDetails details = createValidPaymentDetails();

        Integer transactionId =
                paymentService.pay(BigDecimal.valueOf(1000), details);

        assertNotNull(transactionId);
        assertTrue(transactionId > 0);

        boolean refunded = paymentService.refund(transactionId);

        assertTrue(refunded);
    }

    @Test
    void GivenStandingTicketRequest_WhenIssueTicket_ThenReturnsTicketCode() {
        TicketIssueRequest request = new TicketIssueRequest(
                "849302",
                "EVT-9923",
                TicketIssueRequest.TicketZoneType.STANDING,
                2,
                false,
                null
        );

        String ticketId = ticketService.issueTicket(request);

        assertNotNull(ticketId);
        assertFalse(ticketId.isBlank());
        assertNotEquals("-1", ticketId);
    }

        @Test
    void GivenIssuedTicket_WhenCancelTicket_ThenReturnsTrue() {

        TicketIssueRequest request = new TicketIssueRequest(
                "849302",
                "EVT-9923",
                TicketIssueRequest.TicketZoneType.STANDING,
                1,
                false,
                null
        );

        String ticketId = ticketService.issueTicket(request);

        assertNotNull(ticketId);
        assertNotEquals("-1", ticketId);

        boolean cancelled = ticketService.cancelTicket(ticketId);

        assertTrue(cancelled);
    }


    @Test
    void GivenNullPaymentDetails_WhenPay_ThenReturnsMinusOne() {
        Integer transactionId =
                paymentService.pay(BigDecimal.valueOf(1000), null);

        assertEquals(-1, transactionId);
    }
    @Test
    void GivenNullTransactionId_WhenRefund_ThenReturnsFalse() {
        boolean refunded =
                paymentService.refund(null);

        assertFalse(refunded);
    }

    @Test
    void GivenInvalidStandingTicketRequest_WhenIssueTicket_ThenReturnsMinusOne() {
        TicketIssueRequest request = new TicketIssueRequest(
                "849302",
                "EVT-9923",
                TicketIssueRequest.TicketZoneType.STANDING,
                0,
                false,
                null
        );

        String ticketId = ticketService.issueTicket(request);

        assertEquals("-1", ticketId);
    }

    @Test
    void GivenNullTicketRequest_WhenIssueTicket_ThenReturnsMinusOne() {
        String ticketId =
                ticketService.issueTicket(null);

        assertEquals("-1", ticketId);
    }

    @Test
    void GivenInvalidSeatingTicketRequest_WhenIssueTicket_ThenReturnsMinusOne() {
        TicketIssueRequest request = new TicketIssueRequest(
                "849302",
                "EVT-9923",
                TicketIssueRequest.TicketZoneType.SEATING,
                null,
                true,
                null
        );

        String ticketId = ticketService.issueTicket(request);

        assertEquals("-1", ticketId);
    }



    private PaymentDetails createValidPaymentDetails() {
        return new PaymentDetails(
                "CREDIT_CARD",
                "Israel Israelovice",
                LocalDate.of(2000, 1, 1),
                "2222333344445555",
                4,
                2028,
                "262",
                "20444444",
                "USD"
        );
    }


}