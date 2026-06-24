package ticketsystem.IntegrationTesting;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.DTO.seatPositionDTO;
import ticketsystem.DomainLayer.Reservation;
import ticketsystem.DomainLayer.event.Element;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.event.EventCategory;
import ticketsystem.DomainLayer.event.EventLocation;
import ticketsystem.DomainLayer.event.EventMap;
import ticketsystem.DomainLayer.event.Pair;
import ticketsystem.DomainLayer.event.Seat.SeatStatus;
import ticketsystem.DomainLayer.event.SeatPosition;
import ticketsystem.DomainLayer.event.SeatingArea;
import ticketsystem.DomainLayer.event.StandingArea;
import ticketsystem.DomainLayer.order.ActiveOrder;
import ticketsystem.DomainLayer.order.ActiveOrder.OrderStatus;
import ticketsystem.DomainLayer.order.Ticket;

public class ReservationIntegrationTest {

    private Reservation reservation;
    private ActiveOrder order;
    private Event event;
    private Long eventId;
    private Long seatingAreaId;
    private Long standingAreaId;
    private StandingArea standingArea;

    @BeforeEach
    void setUp() {
        reservation = new Reservation();
        event = createActiveEventWithSeatingAndStandingAreas();
        eventId = event.getId();
        SeatingArea seatingArea = getSeatingArea(event);
        standingArea = getStandingArea(event);
        seatingAreaId = seatingArea.getId();
        standingAreaId = standingArea.getId();
        order = new ActiveOrder(1L, "member-token-1", 1L, eventId);
    }

    @Test
    public void GivenSeatAvailable_WhenSelectSeatTicket_ThenTicketAddedToOrderAndSeatBecomesReserved() {
        seatPositionDTO position = new seatPositionDTO(1, 1);

        reservation.selectSeatTicket(order, event, seatingAreaId, position);

        assertEquals(1, order.getTickets().size());
        Ticket ticket = order.getTickets().get(0);
        assertEquals(eventId, ticket.getEventId());
        assertEquals(seatingAreaId, ticket.getAreaId());
        assertEquals(1, ticket.getRow());
        assertEquals(1, ticket.getChair());
        assertEquals(SeatStatus.RESERVED, event.getSeatStatus(seatingAreaId, new SeatPosition(1, 1)));
    }

    @Test
    public void selectStandingTicket_WhenQuantityTooLarge_ThenThrowsAndOrderAndEventUnchanged() {
        assertThrows(
                IllegalStateException.class,
                () -> reservation.selectStandingTicket(order, event, standingAreaId, 200)
        );

        assertTrue(order.getTickets().isEmpty());
        assertEquals(0, standingArea.getReserved());
        assertEquals(0, standingArea.getSold());
    }

    @Test
    public void removeSeatTicket_WhenTicketDoesNotExist_ThenThrowsAndSeatRemainsReserved() {
        reservation.selectSeatTicket(order, event, seatingAreaId, new seatPositionDTO(1, 1));

        assertThrows(
                IllegalArgumentException.class,
                () -> reservation.removeTicketFromActiveOrder(order, event, 999L)
        );

        assertEquals(1, order.getTickets().size());
        assertEquals(SeatStatus.RESERVED, event.getSeatStatus(seatingAreaId, new SeatPosition(1, 1)));
    }

    @Test
    public void GivenReservedSeatTicket_WhenRemoveSeatTicket_ThenTicketRemovedAndSeatBecomesAvailable() {
        reservation.selectSeatTicket(order, event, seatingAreaId, new seatPositionDTO(1, 1));
        Long ticketId = order.getTickets().get(0).getTicketId();

        reservation.removeTicketFromActiveOrder(order, event, ticketId);

        assertTrue(order.getTickets().isEmpty());
        assertEquals(SeatStatus.AVAILABLE, event.getSeatStatus(seatingAreaId, new SeatPosition(1, 1)));
    }

    @Test
    public void removeStandingTickets_WhenNotEnoughTickets_ThenThrowsAndNothingChanges() {
        reservation.selectStandingTicket(order, event, standingAreaId, 2);

        assertThrows(
                IllegalArgumentException.class,
                () -> reservation.removeStandingTicketsFromActiveOrder(order, event, standingAreaId, 3)
        );

        assertEquals(2, order.getTickets().size());
        assertEquals(2, standingArea.getReserved());
        assertEquals(0, standingArea.getSold());
    }

    @Test
    public void GivenSeatAlreadyReserved_WhenSelectSameSeatAgain_ThenSecondSelectionFailsAndSeatRemainsReserved() {
        ActiveOrder secondOrder = new ActiveOrder(2L, "token-2", 2L, eventId);
        reservation.selectSeatTicket(order, event, seatingAreaId, new seatPositionDTO(1, 1));

        assertThrows(
                IllegalStateException.class,
                () -> reservation.selectSeatTicket(
                        secondOrder,
                        event,
                        seatingAreaId,
                        new seatPositionDTO(1, 1)
                )
        );

        assertEquals(1, order.getTickets().size());
        assertTrue(secondOrder.getTickets().isEmpty());
        assertEquals(SeatStatus.RESERVED, event.getSeatStatus(seatingAreaId, new SeatPosition(1, 1)));
    }

    @Test
    public void expire_WhenStandingTicketsReserved_ThenOrderCancelledTicketsRemovedAndSpotsReleased() {
        reservation.selectStandingTicket(order, event, standingAreaId, 5);

        reservation.expire(event, order);

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertTrue(order.getTickets().isEmpty());
        assertEquals(0, standingArea.getReserved());
        assertEquals(0, standingArea.getSold());
    }

    @Test
    public void GivenReservedSeat_WhenExpireOrder_ThenSeatBecomesAvailableAndOrderCancelled() {
        reservation.selectSeatTicket(order, event, seatingAreaId, new seatPositionDTO(1, 1));

        reservation.expire(event, order);

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertTrue(order.getTickets().isEmpty());
        assertEquals(SeatStatus.AVAILABLE, event.getSeatStatus(seatingAreaId, new SeatPosition(1, 1)));
    }

    @Test
    public void GivenStandingTicketsAvailable_WhenSelectStandingTickets_ThenReservedCountIncreases() {
        reservation.selectStandingTicket(order, event, standingAreaId, 5);

        assertEquals(5, order.getTickets().size());
        assertEquals(5, standingArea.getReserved());
    }

    @Test
    public void GivenStandingTicketsExist_WhenRemoveStandingTickets_ThenReservedCountDecreases() {
        reservation.selectStandingTicket(order, event, standingAreaId, 5);

        reservation.removeStandingTicketsFromActiveOrder(order, event, standingAreaId, 3);

        assertEquals(2, order.getTickets().size());
        assertEquals(2, standingArea.getReserved());
    }

    @Test
    public void GivenReservedSeat_WhenCompleteCheckout_ThenSeatStatusBecomesSold() {
        reservation.selectSeatTicket(order, event, seatingAreaId, new seatPositionDTO(1, 1));

        reservation.submitActiveOrderForCheckout(order, event);
        reservation.completeCheckout(order, event);

        assertEquals(OrderStatus.COMPLETED, order.getStatus());
        assertEquals(SeatStatus.SOLD, event.getSeatStatus(seatingAreaId, new SeatPosition(1, 1)));
    }

    @Test
    public void GivenSeatAlreadySold_WhenSelectingSameSeat_ThenThrowsAndKeepsConsistentState() {
        reservation.selectSeatTicket(order, event, seatingAreaId, new seatPositionDTO(1, 1));
        reservation.submitActiveOrderForCheckout(order, event);
        reservation.completeCheckout(order, event);
        ActiveOrder secondOrder = new ActiveOrder(2L, "token-2", 2L, eventId);

        assertThrows(
                IllegalStateException.class,
                () -> reservation.selectSeatTicket(
                        secondOrder,
                        event,
                        seatingAreaId,
                        new seatPositionDTO(1, 1)
                )
        );

        assertEquals(OrderStatus.COMPLETED, order.getStatus());
        assertTrue(secondOrder.getTickets().isEmpty());
        assertEquals(SeatStatus.SOLD, event.getSeatStatus(seatingAreaId, new SeatPosition(1, 1)));
    }

    @Test
    public void GivenTicketNotReserved_WhenCompleteCheckout_ThenThrowsExceptionAndOrderRemainsUnchanged() {
        reservation.selectSeatTicket(order, event, seatingAreaId, new seatPositionDTO(1, 1));
        reservation.removeTicketFromActiveOrder(order, event, order.getTickets().get(0).getTicketId());

        assertThrows(
                IllegalStateException.class,
                () -> reservation.completeCheckout(order, event)
        );

        assertEquals(OrderStatus.ACTIVE, order.getStatus());
        assertTrue(order.getTickets().isEmpty());
        assertEquals(SeatStatus.AVAILABLE, event.getSeatStatus(seatingAreaId, new SeatPosition(1, 1)));
    }

    @Test
    public void GivenValidOrder_WhenSubmitActiveOrderForCheckout_ThenStatusBecomesPendingCheckoutAndEventRemainsUnchanged() {
        reservation.selectSeatTicket(order, event, seatingAreaId, new seatPositionDTO(1, 1));

        BigDecimal total = reservation.submitActiveOrderForCheckout(order, event);

        assertEquals(OrderStatus.PENDING_CHECKOUT, order.getStatus());
        assertEquals(new BigDecimal("100.00"), total);
        assertEquals(SeatStatus.RESERVED, event.getSeatStatus(seatingAreaId, new SeatPosition(1, 1)));
    }

    @Test
    public void GivenReservedStandingTickets_WhenCompleteCheckout_ThenOrderCompletedAndStandingStateUpdated() {
        reservation.selectStandingTicket(order, event, standingAreaId, 4);
        reservation.submitActiveOrderForCheckout(order, event);

        reservation.completeCheckout(order, event);

        assertEquals(OrderStatus.COMPLETED, order.getStatus());
        assertEquals(4, order.getTickets().size());
        assertEquals(0, standingArea.getReserved());
        assertEquals(4, standingArea.getSold());
    }

    @Test
    public void GivenOrderWithoutTickets_WhenSubmitActiveOrderForCheckout_ThenThrowsException() {
        assertThrows(
                IllegalStateException.class,
                () -> reservation.submitActiveOrderForCheckout(order, event)
        );
    }

    @Test
    public void GivenPaymentFails_WhenPaymentFailedCalled_ThenOrderStatusBecomesPaymentFailed() {
        order.paymentFailed();

        assertEquals(OrderStatus.PAYMENT_FAILED, order.getStatus());
    }

    @Test
    public void GivenReservedSeat_WhenPaymentFails_ThenOrderBecomesPaymentFailedAndSeatRemainsReserved() {
        reservation.selectSeatTicket(order, event, seatingAreaId, new seatPositionDTO(1, 1));

        order.paymentFailed();

        assertEquals(OrderStatus.PAYMENT_FAILED, order.getStatus());
        assertEquals(SeatStatus.RESERVED, event.getSeatStatus(seatingAreaId, new SeatPosition(1, 1)));
        assertEquals(1, order.getTickets().size());
    }

    @Test
    public void GivenPaymentFailedAfterReservation_WhenBarcodeFails_ThenTicketsNotSoldAndSeatRemainsReserved() {
        reservation.selectSeatTicket(order, event, seatingAreaId, new seatPositionDTO(1, 1));
        reservation.submitActiveOrderForCheckout(order, event);

        order.paymentFailed();

        assertEquals(OrderStatus.PAYMENT_FAILED, order.getStatus());
        assertEquals(SeatStatus.RESERVED, event.getSeatStatus(seatingAreaId, new SeatPosition(1, 1)));
        assertEquals(1, order.getTickets().size());
    }

    @Test
    public void GivenOrderIsActive_WhenCompleteCheckout_ThenThrowsAndDoesNotSellTickets() {
        SeatPosition seatPosition = new SeatPosition(1, 1);
        reservation.selectSeatTicket(order, event, seatingAreaId, new seatPositionDTO(1, 1));

        assertThrows(
                IllegalStateException.class,
                () -> reservation.completeCheckout(order, event)
        );

        assertEquals(OrderStatus.ACTIVE, order.getStatus());
        assertEquals(SeatStatus.RESERVED, event.getSeatStatus(seatingAreaId, seatPosition));
    }

    @Test
    public void GivenReservedSeatWasReleased_WhenCompleteCheckout_ThenThrowsAndOrderRemainsNotCompleted() {
        SeatPosition seatPosition = new SeatPosition(1, 1);
        reservation.selectSeatTicket(order, event, seatingAreaId, new seatPositionDTO(1, 1));
        reservation.submitActiveOrderForCheckout(order, event);
        event.releaseSeat(seatingAreaId, seatPosition);

        assertThrows(
                IllegalStateException.class,
                () -> reservation.completeCheckout(order, event)
        );

        assertEquals(OrderStatus.PENDING_CHECKOUT, order.getStatus());
        assertEquals(SeatStatus.AVAILABLE, event.getSeatStatus(seatingAreaId, seatPosition));
    }

    @Test
    public void GivenLotteryIsNull_WhenCheckLottery_ThenOrderAndEventRemainUnchanged() {
        assertDoesNotThrow(() -> reservation.checkLottery(null, order.getUserId(), null));

        assertEquals(OrderStatus.ACTIVE, order.getStatus());
        assertTrue(order.getTickets().isEmpty());
    }

    private SeatingArea getSeatingArea(Event event) {
        return event.getMap().getElements().stream()
                .filter(SeatingArea.class::isInstance)
                .map(SeatingArea.class::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Seating area was not found"));
    }

    private StandingArea getStandingArea(Event event) {
        return event.getMap().getElements().stream()
                .filter(StandingArea.class::isInstance)
                .map(StandingArea.class::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Standing area was not found"));
    }

    private void setEventId(Event event, Long id) {
        try {
            Field idField = Event.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(event, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to set generated event ID in test", exception);
        }
    }

    private void setElementId(Element element, Long id) {
        try {
            Field idField = Element.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(element, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to set generated element ID in test", exception);
        }
    }

    private Event createActiveEventWithSeatingAndStandingAreas() {
        Event event = new Event(
                LocalDateTime.now().plusDays(10),
                "Reservation Integration Test Event",
                1L,
                1L,
                EventLocation.TEL_AVIV,
                100L,
                EventCategory.CONCERT,
                "Test Artist",
                new BigDecimal("100.00"),
                new Pair<>(10, 10)
        );
        event.setStatus(Event.eventStatus.ACTIVE);
        setEventId(event, 100L);

        EventMap map = new EventMap(new Pair<>(10, 10));

        SeatingArea seatingArea = new SeatingArea(
                "Main Seating Area",
                new Pair<>(0, 0),
                new Pair<>(5, 5),
                5,
                5,
                new BigDecimal("100.00")
        );

        StandingArea standingArea = new StandingArea(
                "Main Standing Area",
                new Pair<>(5, 0),
                new Pair<>(5, 5),
                100,
                new BigDecimal("80.00")
        );

        setElementId(seatingArea, 1L);
        setElementId(standingArea, 2L);

        map.addElement(seatingArea);
        map.addElement(standingArea);
        event.setMap(map);

        return event;
    }
}
