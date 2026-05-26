package ticketsystem.UnitTesting;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import ticketsystem.DTO.seatPositionDTO;
import ticketsystem.DomainLayer.Reservation;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.event.Seat.SeatStatus;
import ticketsystem.DomainLayer.event.SeatPosition;
import ticketsystem.DomainLayer.lottery.Lottery;
import ticketsystem.DomainLayer.order.ActiveOrder;
import ticketsystem.DomainLayer.order.Ticket;

public class ReservationTest {

    private Reservation reservation;
    private ActiveOrder order;
    private Event event;

    @BeforeEach
    void setUp() {
        reservation = new Reservation();
        order = mock(ActiveOrder.class);
        event = mock(Event.class);
    }

    @Test
    void GivenSeatTicketDetails_WhenSelectSeatTicket_ThenReserveSeatAndAddTicketToOrder() {
        // Arrange
        Long eventId = 10L;
        Long areaId = 5L;
        BigDecimal price = BigDecimal.valueOf(100);

        seatPositionDTO position = mock(seatPositionDTO.class);

        when(position.getRow()).thenReturn(3);
        when(position.getChair()).thenReturn(7);
        when(event.getId()).thenReturn(eventId);
        when(event.getTicketPrice()).thenReturn(price);

        // Act
        reservation.selectSeatTicket(order, event, areaId, position);

        // Assert
        verify(event).reserveSeat(eq(areaId), any(SeatPosition.class));

        ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
        verify(order).addTicket(ticketCaptor.capture());

        Ticket ticket = ticketCaptor.getValue();

        assertEquals(eventId, ticket.getEventId());
        assertEquals(areaId, ticket.getAreaId());
        assertEquals(3, ticket.getRow());
        assertEquals(7, ticket.getChair());
        assertEquals(price, ticket.getPrice());
    }

    @Test
    void GivenStandingTicketQuantity_WhenSelectStandingTicket_ThenReserveSpotsAndAddTicketsToOrder() {
        // Arrange
        Long eventId = 10L;
        Long areaId = 5L;
        int quantity = 3;
        BigDecimal price = BigDecimal.valueOf(80);

        when(event.getId()).thenReturn(eventId);
        when(event.getTicketPrice()).thenReturn(price);

        // Act
        reservation.selectStandingTicket(order, event, areaId, quantity);

        // Assert
        verify(event).reserveSpot(areaId, quantity);
        verify(order, times(quantity)).addTicket(any(Ticket.class));
    }
    @Test
    void GivenSeatTicketInOrder_WhenRemoveTicketFromActiveOrder_ThenDeleteTicketAndReleaseSeat() {
        // Arrange
        Long ticketId = 1L;
        Long eventId = 10L;
        Long areaId = 5L;

        Ticket ticket = new Ticket(ticketId, eventId, areaId, 2, 4, BigDecimal.valueOf(100));

        when(order.getTickets()).thenReturn(List.of(ticket));

        // Act
        reservation.removeTicketFromActiveOrder(order, event, ticketId);

        // Assert
        verify(event).releaseSeat(eq(areaId), any(SeatPosition.class));
        verify(order).deleteTicket(ticketId);
        verify(event, never()).releaseSpot(anyLong(), anyInt());
    }

    @Test
    void GivenStandingTicketInOrder_WhenRemoveTicketFromActiveOrder_ThenDeleteTicketAndReleaseSpot() {
        // Arrange
        Long ticketId = 1L;
        Long eventId = 10L;
        Long areaId = 5L;

        Ticket ticket = new Ticket(ticketId, eventId, areaId, 0, 0, BigDecimal.valueOf(80));

        when(order.getTickets()).thenReturn(List.of(ticket));

        // Act
        reservation.removeTicketFromActiveOrder(order, event, ticketId);

        // Assert
        verify(event).releaseSpot(areaId, 1);
        verify(order).deleteTicket(ticketId);
        verify(event, never()).releaseSeat(anyLong(), any(SeatPosition.class));
    }


    @Test
    void GivenValidActiveOrder_WhenSubmitActiveOrderForCheckout_ThenValidateAndSubmitOrder() {
        // Act
        reservation.submitActiveOrderForCheckout(order, event, 18);

        // Assert
        InOrder inOrder = inOrder(order);
        inOrder.verify(order).validateCanBeSubmittedBy();
        inOrder.verify(order).submitForCheckout();
    }

    @Test
    void GivenOrderWithSeatAndStandingTickets_WhenCompleteCheckout_ThenCompleteOrderAndSellTickets() {
        // Arrange
        Long eventId = 10L;

        Ticket seatTicket = new Ticket(1L, eventId, 5L, 2, 3, BigDecimal.valueOf(100));
        Ticket standingTicket = new Ticket(2L, eventId, 6L, 0, 0, BigDecimal.valueOf(80));
        when(event.getSeatStatus(eq(5L), any(SeatPosition.class))).thenReturn(SeatStatus.RESERVED);
        when(order.getStatus()).thenReturn(ActiveOrder.OrderStatus.PENDING_CHECKOUT);
        when(order.getTickets()).thenReturn(List.of(seatTicket, standingTicket));

        // Act
        reservation.completeCheckout(order, event);

        // Assert
        verify(order).completeOrder();
        verify(event).sellSeat(eq(5L), any(SeatPosition.class));
        verify(event).sellSpot(6L,1);
    }

    @Test
    void GivenOrder_WhenCalculateTotalPrice_ThenReturnOrderTotalPrice() {
        // Arrange
        BigDecimal total = BigDecimal.valueOf(250);

        when(order.calculateTotalPrice()).thenReturn(total);

        // Act
        BigDecimal result = reservation.calculateTotalPrice(order, event);

        // Assert
        assertEquals(total, result);
        verify(order).calculateTotalPrice();
    }

    @Test
    void GivenOrderWithSeatAndStandingTickets_WhenExpire_ThenReleaseTicketsDeleteThemAndCancelOrder() {
        // Arrange
        Long eventId = 10L;

        Ticket seatTicket = new Ticket(1L, eventId, 5L, 2, 3, BigDecimal.valueOf(100));
        Ticket standingTicket = new Ticket(2L, eventId, 6L, 0, 0, BigDecimal.valueOf(80));

        when(order.getTickets()).thenReturn(List.of(seatTicket, standingTicket));

        // Act
        reservation.expire(event, order);

        // Assert
        verify(event).releaseSeat(eq(5L), any(SeatPosition.class));
        verify(event).releaseSpot(6L,1);

        verify(order).deleteTicket(1L);
        verify(order).deleteTicket(2L);

        verify(order).cancelOrder();
    }

    @Test
    void GivenNullLottery_WhenCheckLottery_ThenDoNothing() {
        // Act & Assert
        assertDoesNotThrow(() -> reservation.checkLottery(null, 1L, "CODE"));
    }

    @Test
    void GivenLotteryAndGuestUser_WhenCheckLottery_ThenThrowException() {
        // Arrange
        Lottery lottery = mock(Lottery.class);

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reservation.checkLottery(lottery, null, "CODE")
        );

        // Assert
        assertEquals("Guests cannot buy tickets for lottery events", exception.getMessage());
    }

    @Test
    void GivenLotteryAndBlankCode_WhenCheckLottery_ThenThrowException() {
        // Arrange
        Lottery lottery = mock(Lottery.class);

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reservation.checkLottery(lottery, 1L, " ")
        );

        // Assert
        assertEquals("Lottery code is required for this event", exception.getMessage());
    }

    @Test
    void GivenUserNotRegisteredToLottery_WhenCheckLottery_ThenThrowException() {
        // Arrange
        Lottery lottery = mock(Lottery.class);

        when(lottery.getRegisteredMemberIds()).thenReturn(List.of(2L, 3L));

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reservation.checkLottery(lottery, 1L, "CODE")
        );

        // Assert
        assertEquals("User is not registered for this lottery", exception.getMessage());
    }

    @Test
    void GivenRegisteredUserButInvalidLotteryCode_WhenCheckLottery_ThenThrowException() {
        // Arrange
        Lottery lottery = mock(Lottery.class);

        when(lottery.getRegisteredMemberIds()).thenReturn(List.of(1L));
        when(lottery.validateWinnerCode(1L, "BAD_CODE")).thenReturn(false);

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reservation.checkLottery(lottery, 1L, "BAD_CODE")
        );

        // Assert
        assertEquals("Invalid lottery code", exception.getMessage());
    }

    @Test
    void GivenRegisteredUserAndValidLotteryCode_WhenCheckLottery_ThenPassSuccessfully() {
        // Arrange
        Lottery lottery = mock(Lottery.class);

        when(lottery.getRegisteredMemberIds()).thenReturn(List.of(1L));
        when(lottery.validateWinnerCode(1L, "VALID_CODE")).thenReturn(true);

        // Act & Assert
        assertDoesNotThrow(() -> reservation.checkLottery(lottery, 1L, "VALID_CODE"));
    }

    @Test
    void GivenReservation_WhenGenerateTicketIdTwice_ThenIdsAreIncremental() {
        // Act
        Long firstId = reservation.generateTicketId();
        Long secondId = reservation.generateTicketId();

        // Assert
        assertEquals(1L, firstId);
        assertEquals(2L, secondId);
    }
    @Test
void GivenEnoughStandingTicketsInOrder_WhenRemoveStandingTicketsFromActiveOrder_ThenDeleteRequestedQuantityAndReleaseSpots() {
    // Arrange
    Long eventId = 10L;
    Long areaId = 5L;

    Ticket ticket1 = new Ticket(1L, eventId, areaId, 0, 0, BigDecimal.valueOf(80));
    Ticket ticket2 = new Ticket(2L, eventId, areaId, 0, 0, BigDecimal.valueOf(80));
    Ticket ticket3 = new Ticket(3L, eventId, areaId, 0, 0, BigDecimal.valueOf(80));

    when(order.getTickets()).thenReturn(List.of(ticket1, ticket2, ticket3));

    // Act
    reservation.removeStandingTicketsFromActiveOrder(order, event, areaId, 2);

    // Assert
    verify(order).deleteTicket(1L);
    verify(order).deleteTicket(2L);
    verify(order, never()).deleteTicket(3L);

    verify(event).releaseSpot(areaId, 2);
}

@Test
void GivenNotEnoughStandingTicketsInOrder_WhenRemoveStandingTicketsFromActiveOrder_ThenThrowExceptionAndDoNotReleaseSpots() {
    // Arrange
    Long eventId = 10L;
    Long areaId = 5L;

    Ticket ticket1 = new Ticket(1L, eventId, areaId, 0, 0, BigDecimal.valueOf(80));

    when(order.getTickets()).thenReturn(List.of(ticket1));

    // Act
    IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> reservation.removeStandingTicketsFromActiveOrder(order, event, areaId, 2)
    );

    // Assert
    assertEquals("Not enough standing tickets in the order to remove", exception.getMessage());

    verify(order, never()).deleteTicket(anyLong());
    verify(event, never()).releaseSpot(anyLong(), anyInt());
}

@Test
void GivenStandingTicketsFromDifferentArea_WhenRemoveStandingTicketsFromActiveOrder_ThenRemoveOnlyFromRequestedArea() {
    // Arrange
    Long eventId = 10L;
    Long requestedAreaId = 5L;
    Long otherAreaId = 6L;

    Ticket ticketFromRequestedArea = new Ticket(1L, eventId, requestedAreaId, 0, 0, BigDecimal.valueOf(80));
    Ticket ticketFromOtherArea = new Ticket(2L, eventId, otherAreaId, 0, 0, BigDecimal.valueOf(80));

    when(order.getTickets()).thenReturn(List.of(ticketFromRequestedArea, ticketFromOtherArea));

    // Act
    reservation.removeStandingTicketsFromActiveOrder(order, event, requestedAreaId, 1);

    // Assert
    verify(order).deleteTicket(1L);
    verify(order, never()).deleteTicket(2L);

    verify(event).releaseSpot(requestedAreaId, 1);
}

@Test
void GivenSeatTicketsInSameArea_WhenRemoveStandingTicketsFromActiveOrder_ThenIgnoreSeatTickets() {
    // Arrange
    Long eventId = 10L;
    Long areaId = 5L;

    Ticket standingTicket = new Ticket(1L, eventId, areaId, 0, 0, BigDecimal.valueOf(80));
    Ticket seatTicket = new Ticket(2L, eventId, areaId, 3, 4, BigDecimal.valueOf(100));

    when(order.getTickets()).thenReturn(List.of(standingTicket, seatTicket));

    // Act
    reservation.removeStandingTicketsFromActiveOrder(order, event, areaId, 1);

    // Assert
    verify(order).deleteTicket(1L);
    verify(order, never()).deleteTicket(2L);

    verify(event).releaseSpot(areaId, 1);

}
    
}