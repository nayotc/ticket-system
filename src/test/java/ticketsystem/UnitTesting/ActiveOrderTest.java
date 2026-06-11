package ticketsystem.UnitTesting;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.DomainLayer.order.ActiveOrder;
import ticketsystem.DomainLayer.order.ActiveOrder.OrderStatus;
import ticketsystem.DomainLayer.order.Ticket;

public class ActiveOrderTest {

    private ActiveOrder order;

    private final Long orderId = 1L;
    private final Long userId = 10L;
    private final String sessionToken = "session-token";
    private final Long eventId = 100L;

    @BeforeEach
    void setUp() {
        order = new ActiveOrder(orderId, sessionToken, userId, eventId);
    }

    private Ticket createMockTicket(Long ticketId, BigDecimal price) {
        Ticket ticket = mock(Ticket.class);

        when(ticket.getTicketId()).thenReturn(ticketId);
        when(ticket.getEventId()).thenReturn(eventId);
        when(ticket.getRow()).thenReturn(1);
        when(ticket.getChair()).thenReturn(2);
        when(ticket.getPrice()).thenReturn(price);

        return ticket;
    }

    @Test
    void givenValidDetails_whenCreateActiveOrder_thenFieldsAreInitializedCorrectly() {
        assertEquals(orderId, order.getOrderId());
        assertEquals(userId, order.getUserId());
        assertEquals(sessionToken, order.getSessionToken());
        assertEquals(eventId, order.getEventId());
        assertEquals(OrderStatus.ACTIVE, order.getStatus());
        assertNotNull(order.getTickets());
        assertTrue(order.getTickets().isEmpty());
        // assertFalse(order.isStopped());
    }

    @Test
    void givenTicket_whenAddTicket_thenTicketIsAddedToOrder() {
        Ticket ticket = createMockTicket(1L, new BigDecimal(50));

        order.addTicket(ticket);

        assertEquals(1, order.getTickets().size());
        assertSame(ticket, order.getTickets().get(0));
    }

    @Test
    void givenExistingTicket_whenDeleteTicket_thenTicketIsRemovedAndReturned() {
        Ticket ticket = createMockTicket(1L, new BigDecimal(50));
        order.addTicket(ticket);

        Ticket removedTicket = order.deleteTicket(1L);

        assertSame(ticket, removedTicket);
        assertTrue(order.getTickets().isEmpty());
    }

    @Test
    void givenNonExistingTicket_whenDeleteTicket_thenThrowException() {
        Ticket ticket = createMockTicket(1L, new BigDecimal(50));
        order.addTicket(ticket);

        assertThrows(IllegalArgumentException.class, () -> order.deleteTicket(2L));
    }

    @Test
    void givenActiveOrder_whenCancelOrder_thenStatusIsCancelled() {
        order.cancelOrder();

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    void givenActiveOrder_whenCompleteOrder_thenStatusIsCompleted() {
        order.completeOrder();

        assertEquals(OrderStatus.COMPLETED, order.getStatus());
    }

    @Test
    void givenActiveOrder_whenSubmitForCheckout_thenStatusIsPendingCheckoutAndTimerStopped() {
        order.submitForCheckout();

        assertEquals(OrderStatus.PENDING_CHECKOUT, order.getStatus());
        assertTrue(order.isPendingCheckout());
        // assertTrue(order.isStopped());
    }

    @Test
    void givenActiveOrder_whenPaymentFailed_thenStatusIsPaymentFailed() {
        order.paymentFailed();

        assertEquals(OrderStatus.PAYMENT_FAILED, order.getStatus());
    }

    @Test
    void givenOrderWithNoTickets_whenValidateHasTickets_thenThrowException() {
        assertThrows(IllegalStateException.class, () -> order.validateHasTickets());
    }

    @Test
    void givenOrderWithTickets_whenValidateHasTickets_thenDoesNotThrowException() {
        order.addTicket(createMockTicket(1L, new BigDecimal(50)));

        order.validateHasTickets();

        assertEquals(1, order.getTickets().size());
    }



    @Test
    void givenOrderWithoutTickets_whenValidateCanBeSubmittedBy_thenThrowException() {
        assertThrows(IllegalStateException.class, () -> order.validateCanBeSubmittedBy());
    }

    @Test
    void givenCancelledOrder_whenValidateCanBeSubmittedBy_thenThrowException() {
        order.addTicket(createMockTicket(1L, new BigDecimal(50)));
        order.cancelOrder();

        assertThrows(IllegalStateException.class, () -> order.validateCanBeSubmittedBy());
    }

    @Test
    void givenValidActiveOrder_whenValidateCanBeSubmittedBy_thenDoesNotThrowException() {
        order.addTicket(createMockTicket(1L, new BigDecimal(50)));

        order.validateCanBeSubmittedBy();

        assertEquals(OrderStatus.ACTIVE, order.getStatus());
    }

    @Test
    void givenTickets_whenCalculateTotalPrice_thenReturnSumOfTicketPrices() {
        order.addTicket(createMockTicket(1L, new BigDecimal(50)));
        order.addTicket(createMockTicket(2L, new BigDecimal(70)));
        order.addTicket(createMockTicket(3L, new BigDecimal(30)));

        BigDecimal total = order.calculateTotalPrice();

        assertEquals(new BigDecimal(150), total);
    }

    @Test
    void givenOrderWithoutTickets_whenCalculateTotalPrice_thenReturnZero() {
        BigDecimal total = order.calculateTotalPrice();

        assertEquals(new BigDecimal(0), total);
    }

    @Test
    void givenNewActiveOrder_whenCheckExpiredImmediately_thenReturnFalse() {
        assertFalse(order.isExpired());
    }

    @Test
    void givenTicketFromDifferentEvent_whenAddTicket_thenThrowException() {
        // Arrange
        Ticket ticket = mock(Ticket.class);

        when(ticket.getTicketId()).thenReturn(1L);
        when(ticket.getEventId()).thenReturn(999L);

        // Act & Assert
        assertThrows(IllegalStateException.class,
                () -> order.addTicket(ticket));
    }

    @Test
    void givenOrderInPaymentFailedStatus_whenValidateCanBeSubmittedBy_thenDoesNotThrow() {
        // Arrange
        order.addTicket(createMockTicket(1L, new BigDecimal(50)));
        order.paymentFailed();

        // Act & Assert
        assertDoesNotThrow(() -> order.validateCanBeSubmittedBy());
    }

    @Test
    void givenOrderCloseToExpiration_whenIsAboutToExpire_thenReturnTrue() {
        // Arrange
        ActiveOrder expiringOrder = new ActiveOrder(
                orderId,
                sessionToken,
                userId,
                eventId,
                java.time.LocalDateTime.now().plusMinutes(1));

        // Act & Assert
        assertTrue(expiringOrder.isAboutToExpire());
    }

    @Test
    void givenOrderFarFromExpiration_whenIsAboutToExpire_thenReturnFalse() {
        // Arrange
        ActiveOrder nonExpiringOrder = new ActiveOrder(
                orderId,
                sessionToken,
                userId,
                eventId,
                java.time.LocalDateTime.now().plusMinutes(10));

        // Act & Assert
        assertFalse(nonExpiringOrder.isAboutToExpire());
    }

    @Test
    void givenExpiredOrder_whenIsAboutToExpire_thenReturnFalse() {
        // Arrange
        ActiveOrder expiredOrder = new ActiveOrder(
                orderId,
                sessionToken,
                userId,
                eventId,
                java.time.LocalDateTime.now().minusMinutes(1));

        // Act & Assert
        assertFalse(expiredOrder.isAboutToExpire());
    }

    @Test
    void givenExpiredOrder_whenIsExpired_thenReturnTrue() {
        // Arrange
        ActiveOrder expiredOrder = new ActiveOrder(
                orderId,
                sessionToken,
                userId,
                eventId,
                java.time.LocalDateTime.now().minusMinutes(1));

        // Act & Assert
        assertTrue(expiredOrder.isExpired());
    }

    @Test
    void givenPendingCheckoutOrder_whenCheckPendingCheckout_thenReturnTrue() {
        // Arrange
        order.submitForCheckout();

        // Act & Assert
        assertTrue(order.isPendingCheckout());
    }

    @Test
    void givenActiveOrder_whenCheckPendingCheckout_thenReturnFalse() {
        // Act & Assert
        assertFalse(order.isPendingCheckout());
    }

    @Test
    void givenOrder_whenActivateOrder_thenStatusBecomesActive() {
        // Arrange
        order.cancelOrder();

        // Act
        order.activeOrder();

        // Assert
        assertEquals(OrderStatus.ACTIVE, order.getStatus());
    }

    @Test
    void givenOrder_whenIncrementVersion_thenVersionIncreases() {
        // Arrange
        int initialVersion = order.getVersion() == null ? 0 : order.getVersion();

        // Act
        order.incrementVersion();

        // Assert
        assertEquals(initialVersion + 1, order.getVersion());
    }

    @Test
    void givenOrderWithTickets_whenCopyConstructor_thenCreatesCopy() {
        // Arrange
        Ticket ticket = createMockTicket(1L, new BigDecimal(50));
        Ticket copiedTicket = createMockTicket(1L, new BigDecimal(50));

        when(ticket.copy()).thenReturn(copiedTicket);

        order.addTicket(ticket);

        // Act
        ActiveOrder copiedOrder = new ActiveOrder(order);

        // Assert
        assertEquals(order.getOrderId(), copiedOrder.getOrderId());
        assertEquals(order.getUserId(), copiedOrder.getUserId());
        assertEquals(order.getSessionToken(), copiedOrder.getSessionToken());
        assertEquals(order.getEventId(), copiedOrder.getEventId());
        assertEquals(order.getStatus(), copiedOrder.getStatus());
        assertEquals(1, copiedOrder.getTickets().size());
        assertSame(copiedTicket, copiedOrder.getTickets().get(0));
    }

    @Test
    void givenOrder_whenSetUserIdAndSessionToken_thenValuesUpdated() {
        // Act
        order.setUserId(999L);
        order.setSessionToken("new-token");

        // Assert
        assertEquals(999L, order.getUserId());
        assertEquals("new-token", order.getSessionToken());
    }
}