package ticketsystem.IntegrationTesting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ticketsystem.DTO.PaymentDetails;
import ticketsystem.DTO.TicketIssueRequest;
import ticketsystem.InfrastructureLayer.ExternalPaymentService;
import ticketsystem.InfrastructureLayer.ExternalTicketIssuingService;

@Tag("integration")
class ExternalServiceResponseTest {

    private static final String EXTERNAL_URL = "http://external.test";
    private static final String HANDSHAKE_OK = "OK";
    private static final String VALID_TRANSACTION_ID = "50000";
    private static final String REFUND_SUCCESS = "1";
    private static final String FAILURE_RESPONSE = "-1";
    private static final String VALID_TICKET_CODE = "TIX-1a2b-3456";

    private ExternalPaymentService paymentService;
    private ExternalTicketIssuingService ticketIssuingService;
    private MockRestServiceServer paymentServer;
    private MockRestServiceServer ticketServer;

    @BeforeEach
    void setUp() {
        paymentService = new ExternalPaymentService();
        ticketIssuingService = new ExternalTicketIssuingService();

        ReflectionTestUtils.setField(paymentService, "externalSystemUrl", EXTERNAL_URL);
        ReflectionTestUtils.setField(ticketIssuingService, "externalSystemUrl", EXTERNAL_URL);

        RestTemplate paymentRestTemplate =
                (RestTemplate) ReflectionTestUtils.getField(paymentService, "restTemplate");
        RestTemplate ticketRestTemplate =
                (RestTemplate) ReflectionTestUtils.getField(ticketIssuingService, "restTemplate");

        paymentServer = MockRestServiceServer.createServer(paymentRestTemplate);
        ticketServer = MockRestServiceServer.createServer(ticketRestTemplate);
    }

    @Test
    void GivenOkHandshakeResponse_WhenHandshake_ThenReturnsTrue() {
        paymentServer.expect(requestTo(EXTERNAL_URL))
                .andRespond(withSuccess(HANDSHAKE_OK, MediaType.TEXT_PLAIN));

        assertTrue(paymentService.handshake());
    }

    @Test
    void GivenMalformedHandshakeResponse_WhenHandshake_ThenReturnsFalse() {
        paymentServer.expect(requestTo(EXTERNAL_URL))
                .andRespond(withSuccess("DOWN", MediaType.TEXT_PLAIN));

        assertFalse(paymentService.handshake());
    }

    @Test
    void GivenValidPayResponse_WhenPay_ThenReturnsTransactionIdInApiRange() {
        paymentServer.expect(requestTo(EXTERNAL_URL))
                .andRespond(withSuccess(VALID_TRANSACTION_ID, MediaType.TEXT_PLAIN));

        Integer transactionId = paymentService.pay(
                BigDecimal.valueOf(1000),
                createValidPaymentDetails()
        );

        assertEquals(50_000, transactionId);
    }

    @Test
    void GivenMalformedPayResponse_WhenPay_ThenReturnsMinusOne() {
        paymentServer.expect(requestTo(EXTERNAL_URL))
                .andRespond(withSuccess("NOT_A_TRANSACTION_ID", MediaType.TEXT_PLAIN));

        Integer transactionId = paymentService.pay(
                BigDecimal.valueOf(1000),
                createValidPaymentDetails()
        );

        assertEquals(-1, transactionId);
    }

    @Test
    void GivenOutOfRangePayResponse_WhenPay_ThenReturnsMinusOne() {
        paymentServer.expect(requestTo(EXTERNAL_URL))
                .andRespond(withSuccess("42", MediaType.TEXT_PLAIN));

        Integer transactionId = paymentService.pay(
                BigDecimal.valueOf(1000),
                createValidPaymentDetails()
        );

        assertEquals(-1, transactionId);
    }

    @Test
    void GivenExplicitRejectPayResponse_WhenPay_ThenReturnsMinusOne() {
        paymentServer.expect(requestTo(EXTERNAL_URL))
                .andRespond(withSuccess(FAILURE_RESPONSE, MediaType.TEXT_PLAIN));

        Integer transactionId = paymentService.pay(
                BigDecimal.valueOf(1000),
                createValidPaymentDetails()
        );

        assertEquals(-1, transactionId);
    }

    @Test
    void GivenSuccessfulRefundResponse_WhenRefund_ThenReturnsTrue() {
        paymentServer.expect(requestTo(EXTERNAL_URL))
                .andRespond(withSuccess(REFUND_SUCCESS, MediaType.TEXT_PLAIN));

        assertTrue(paymentService.refund(50_000));
    }

    @Test
    void GivenExplicitRejectRefundResponse_WhenRefund_ThenReturnsFalse() {
        paymentServer.expect(requestTo(EXTERNAL_URL))
                .andRespond(withSuccess(FAILURE_RESPONSE, MediaType.TEXT_PLAIN));

        assertFalse(paymentService.refund(50_000));
    }

    @Test
    void GivenMalformedRefundResponse_WhenRefund_ThenReturnsFalse() {
        paymentServer.expect(requestTo(EXTERNAL_URL))
                .andRespond(withSuccess("0", MediaType.TEXT_PLAIN));

        assertFalse(paymentService.refund(50_000));
    }

    @Test
    void GivenSuccessfulIssuingResponse_WhenIssueTicket_ThenReturnsTicketCode() {
        ticketServer.expect(requestTo(EXTERNAL_URL))
                .andRespond(withSuccess(VALID_TICKET_CODE, MediaType.TEXT_PLAIN));

        String ticketId = ticketIssuingService.issueTicket(createValidIssueRequest());

        assertEquals(VALID_TICKET_CODE, ticketId);
    }

    @Test
    void GivenExplicitRejectIssuingResponse_WhenIssueTicket_ThenReturnsMinusOne() {
        ticketServer.expect(requestTo(EXTERNAL_URL))
                .andRespond(withSuccess(FAILURE_RESPONSE, MediaType.TEXT_PLAIN));

        String ticketId = ticketIssuingService.issueTicket(createValidIssueRequest());

        assertEquals(FAILURE_RESPONSE, ticketId);
    }

    private PaymentDetails createValidPaymentDetails() {
        return new PaymentDetails(
                "CREDIT_CARD",
                "Israel Israelovice",
                LocalDate.of(2000, 1, 1),
                "2222333344445555",
                4,
                2021,
                "262",
                "20444444",
                "USD"
        );
    }

    private TicketIssueRequest createValidIssueRequest() {
        return new TicketIssueRequest(
                "849302",
                "EVT-9923",
                TicketIssueRequest.TicketZoneType.STANDING,
                2,
                false,
                null
        );
    }
}
