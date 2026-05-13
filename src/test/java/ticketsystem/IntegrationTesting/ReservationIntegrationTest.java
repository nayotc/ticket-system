import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.DTO.seatPositionDTO;
import ticketsystem.DomainLayer.Reservation;
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
import ticketsystem.DomainLayer.order.Ticket;

public class ReservationIntegrationTest {

    private Reservation reservation;
    private ActiveOrder order;
    private Event event;

    @BeforeEach
    void setUp() {
        reservation = new Reservation();

        order = new ActiveOrder(
                1L,
                "member-token-1",
                1L,
                100L
        );

        event = createActiveEventWithSeatingAndStandingAreas(100L);
    }

    //after rotem g. upload her code.
    
    // public void testSelectSeatTicket_WhenSeatAvailable_ThenTicketAddedToOrderAndSeatStatusBecomesReserved() {
    //     // Arrange
    //     Long eventId = 100L;
    //     Long areaId = 1L;

    //     Reservation reservation = new Reservation();
    //     Event event = createActiveEventWithSeatingAndStandingAreas(eventId);

    //     ActiveOrder order = new ActiveOrder(
    //             1L,
    //             "token-1",
    //             1L,
    //             eventId
    //     );

    //     seatPositionDTO position = new seatPositionDTO(1, 1);

    //     // Act
    //     reservation.selectSeatTicket(order, event, areaId, position);

    //     // Assert - Order
    //     assertEquals(1, order.getTickets().size());

    //     Ticket ticket = order.getTickets().get(0);
    //     assertEquals(eventId, ticket.getEventId());
    //     assertEquals(areaId, ticket.getAreaId());
    //     assertEquals(1, ticket.getRow());
    //     assertEquals(1, ticket.getChair());

    //     // Assert - Event
    //     assertEquals(
    //             SeatStatus.RESERVED,
    //             event.getSeatStatus(areaId, new SeatPosition(1, 1))
    //     );
    // }
    // @Test
    // public void testCompleteCheckout_WhenSeatReserved_ThenSeatStatusBecomesSold() {
    //     // Arrange
    //     Long eventId = 100L;
    //     Long areaId = 1L;

    //     Reservation reservation = new Reservation();
    //     Event event = createActiveEventWithSeatingAndStandingAreas(eventId);

    //     ActiveOrder order = new ActiveOrder(1L, "token-1", 1L, eventId);

    //     reservation.selectSeatTicket(
    //             order,
    //             event,
    //             areaId,
    //             new seatPositionDTO(1, 1)
    //     );

    //     // Act
    //     reservation.completeCheckout(order, event);

    //     // Assert
    //     assertEquals(ActiveOrder.OrderStatus.COMPLETED, order.getStatus());

    //     assertEquals(
    //             SeatStatus.SOLD,
    //             event.getSeatStatus(areaId, new SeatPosition(1, 1))
    //     );
    // }

    // @Test
    // public void testRemoveSeatTicket_WhenSeatWasReserved_ThenTicketRemovedAndSeatStatusBecomesAvailable() {
    //     // Arrange
    //     Long eventId = 100L;
    //     Long areaId = 1L;

    //     Reservation reservation = new Reservation();
    //     Event event = createActiveEventWithSeatingAndStandingAreas(eventId);

    //     ActiveOrder order = new ActiveOrder(1L, "token-1", 1L, eventId);

    //     reservation.selectSeatTicket(
    //             order,
    //             event,
    //             areaId,
    //             new seatPositionDTO(1, 1)
    //     );

    //     Long ticketId = order.getTickets().get(0).getTicketId();

    //     // Act
    //     reservation.removeTicketFromActiveOrder(order, event, ticketId);

    //     // Assert
    //     assertTrue(order.getTickets().isEmpty());

    //     assertEquals(
    //             SeatStatus.AVAILABLE,
    //             event.getSeatStatus(areaId, new SeatPosition(1, 1))
    //     );
    // }

   private Event createActiveEventWithSeatingAndStandingAreas(Long eventId) {
        Event event = new Event(
                eventId,
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

        EventMap map = new EventMap(new Pair<>(10, 10));

        SeatingArea seatingArea = new SeatingArea(
                1L,
                "Main Seating Area",
                new Pair<>(0, 0),
                new Pair<>(5, 5),
                5,
                5
        );

        StandingArea standingArea = new StandingArea(
                2L,
                "Main Standing Area",
                new Pair<>(5, 0),
                new Pair<>(5, 5),
                100
        );

        map.addElement(seatingArea);
        map.addElement(standingArea);

        event.setMap(map);

        return event;
    }
}