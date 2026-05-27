package ticketsystem.IntegrationTesting;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import ticketsystem.DomainLayer.event.Seat;
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

        @BeforeEach
        void setUp() {
                reservation = new Reservation();

                order = new ActiveOrder(
                                1L,
                                "member-token-1",
                                1L,
                                100L);

                event = createActiveEventWithSeatingAndStandingAreas(100L);
                
        }

        @Test
        public void GivenSeatAvailable_WhenSelectSeatTicket_ThenTicketAddedToOrderAndSeatBecomesReserved() {
                // Arrange
                Long eventId = 100L;
                Long areaId = 1L;

                seatPositionDTO position = new seatPositionDTO(1, 1);

                // Act
                reservation.selectSeatTicket(order, event, areaId, position);

                // Assert - Order
                assertEquals(1, order.getTickets().size());

                Ticket ticket = order.getTickets().get(0);
                assertEquals(eventId, ticket.getEventId());
                assertEquals(areaId, ticket.getAreaId());
                assertEquals(1, ticket.getRow());
                assertEquals(1, ticket.getChair());

                // Assert - Event
                assertEquals(
                                SeatStatus.RESERVED,
                                event.getSeatStatus(areaId, new SeatPosition(1, 1)));
        }

        @Test
        public void selectStandingTicket_WhenQuantityTooLarge_ThenThrowsAndOrderAndEventUnchanged() {
                Long areaId = 2L;

                StandingArea standingArea = getStandingArea(event, areaId);

                assertThrows(IllegalStateException.class, () -> {
                        reservation.selectStandingTicket(order, event, areaId, 200);
                });

                assertTrue(order.getTickets().isEmpty());
                assertEquals(0, standingArea.getReserved());
                assertEquals(0, standingArea.getSold());
        }

        @Test
        public void removeSeatTicket_WhenTicketDoesNotExist_ThenThrowsAndSeatRemainsReserved() {
                Long areaId = 1L;

                reservation.selectSeatTicket(order, event, areaId, new seatPositionDTO(1, 1));

                assertThrows(Exception.class, () -> {
                        reservation.removeTicketFromActiveOrder(order, event, 999L);
                });

                assertEquals(1, order.getTickets().size());
                assertEquals(
                                SeatStatus.RESERVED,
                                event.getSeatStatus(areaId, new SeatPosition(1, 1)));
        }


        @Test
        public void GivenReservedSeatTicket_WhenRemoveSeatTicket_ThenTicketRemovedAndSeatBecomesAvailable() {
                // Arrange
                Long eventId = 100L;
                Long areaId = 1L;

                Event event = createActiveEventWithSeatingAndStandingAreas(eventId);

                ActiveOrder order = new ActiveOrder(1L, "token-1", 1L, eventId);

                reservation.selectSeatTicket(
                                order,
                                event,
                                areaId,
                                new seatPositionDTO(1, 1));

                Long ticketId = order.getTickets().get(0).getTicketId();

                // Act
                reservation.removeTicketFromActiveOrder(order, event, ticketId);

                // Assert
                assertTrue(order.getTickets().isEmpty());

                assertEquals(
                                SeatStatus.AVAILABLE,
                                event.getSeatStatus(areaId, new SeatPosition(1, 1)));
        }

        @Test
        public void removeStandingTickets_WhenNotEnoughTickets_ThenThrowsAndNothingChanges() {
                Long areaId = 2L;

                StandingArea standingArea = getStandingArea(event, areaId);

                reservation.selectStandingTicket(order, event, areaId, 2);

                assertThrows(IllegalArgumentException.class, () -> {
                        reservation.removeStandingTicketsFromActiveOrder(order, event, areaId, 3);
                });

                assertEquals(2, order.getTickets().size());
                assertEquals(2, standingArea.getReserved());
                assertEquals(0, standingArea.getSold());
        }

        @Test
        public void GivenSeatAlreadyReserved_WhenSelectSameSeatAgain_ThenSecondSelectionFailsAndSeatRemainsReserved() {
                // Arrange
                Long eventId = 100L;
                Long areaId = 1L;

                Event event = createActiveEventWithSeatingAndStandingAreas(eventId);

                ActiveOrder firstOrder = new ActiveOrder(1L, "token-1", 1L, eventId);
                ActiveOrder secondOrder = new ActiveOrder(2L, "token-2", 2L, eventId);

                reservation.selectSeatTicket(
                                firstOrder,
                                event,
                                areaId,
                                new seatPositionDTO(1, 1));

                // Act + Assert
                assertThrows(
                                IllegalStateException.class,
                                () -> reservation.selectSeatTicket(
                                                secondOrder,
                                                event,
                                                areaId,
                                                new seatPositionDTO(1, 1)));

                assertEquals(1, firstOrder.getTickets().size());
                assertTrue(secondOrder.getTickets().isEmpty());

                assertEquals(
                                SeatStatus.RESERVED,
                                event.getSeatStatus(areaId, new SeatPosition(1, 1)));
        }

        @Test
        public void expire_WhenStandingTicketsReserved_ThenOrderCancelledTicketsRemovedAndSpotsReleased() {
                Long areaId = 2L;

                StandingArea standingArea = getStandingArea(event, areaId);

                reservation.selectStandingTicket(order, event, areaId, 5);

                reservation.expire(event, order);

                assertEquals(OrderStatus.CANCELLED, order.getStatus());
                assertTrue(order.getTickets().isEmpty());

                assertEquals(0, standingArea.getReserved());
                assertEquals(0, standingArea.getSold());
        }

        @Test
        public void GivenReservedSeat_WhenExpireOrder_ThenSeatBecomesAvailableAndOrderCancelled() {
                // Arrange
                Long eventId = 100L;
                Long areaId = 1L;

                Event event = createActiveEventWithSeatingAndStandingAreas(eventId);

                ActiveOrder order = new ActiveOrder(1L, "token-1", 1L, eventId);

                reservation.selectSeatTicket(
                                order,
                                event,
                                areaId,
                                new seatPositionDTO(1, 1));

                // Act
                reservation.expire(event, order);

                // Assert
                assertEquals(
                                ActiveOrder.OrderStatus.CANCELLED,
                                order.getStatus());

                assertTrue(order.getTickets().isEmpty());

                assertEquals(
                                SeatStatus.AVAILABLE,
                                event.getSeatStatus(areaId, new SeatPosition(1, 1)));
        }

        @Test
        public void GivenStandingTicketsAvailable_WhenSelectStandingTickets_ThenReservedCountIncreases() {
                // Arrange
                Long eventId = 100L;
                Long areaId = 2L;

                Event event = createActiveEventWithSeatingAndStandingAreas(eventId);

                ActiveOrder order = new ActiveOrder(1L, "token-1", 1L, eventId);

                StandingArea standingArea = getStandingArea(event, areaId);

                // Act
                reservation.selectStandingTicket(order, event, areaId, 5);

                // Assert
                assertEquals(5, order.getTickets().size());
                assertEquals(5, standingArea.getReserved());
        }

        @Test
        public void GivenStandingTicketsExist_WhenRemoveStandingTickets_ThenReservedCountDecreases() {
                // Arrange
                Long eventId = 100L;
                Long areaId = 2L;

                Event event = createActiveEventWithSeatingAndStandingAreas(eventId);

                ActiveOrder order = new ActiveOrder(1L, "token-1", 1L, eventId);

                StandingArea standingArea = getStandingArea(event, areaId);

                reservation.selectStandingTicket(order, event, areaId, 5);

                // Act
                reservation.removeStandingTicketsFromActiveOrder(
                                order,
                                event,
                                areaId,
                                3);

                // Assert
                assertEquals(2, order.getTickets().size());
                assertEquals(2, standingArea.getReserved());

        }
         @Test
        public void GivenReservedSeat_WhenCompleteCheckout_ThenSeatStatusBecomesSold() {
                // Arrange
                Long eventId = 100L;
                Long areaId = 1L;

                Event event = createActiveEventWithSeatingAndStandingAreas(eventId);

                ActiveOrder order = new ActiveOrder(1L, "token-1", 1L, eventId);

                reservation.selectSeatTicket(
                                order,
                                event,
                                areaId,
                                new seatPositionDTO(1, 1));

                // Act
                reservation.submitActiveOrderForCheckout(order, event);
                reservation.completeCheckout(order, event);

                // Assert
                assertEquals(ActiveOrder.OrderStatus.COMPLETED, order.getStatus());

                assertEquals(
                                SeatStatus.SOLD,
                                event.getSeatStatus(areaId, new SeatPosition(1, 1)));
        }

        @Test
        public void GivenSeatAlreadySold_WhenSelectingSameSeat_ThenThrowsAndKeepsConsistentState() {
                Long areaId = 1L;

                reservation.selectSeatTicket(order, event, areaId, new seatPositionDTO(1, 1));
                reservation.submitActiveOrderForCheckout(order, event);
                reservation.completeCheckout(order, event);

                ActiveOrder secondOrder = new ActiveOrder(2L, "token-2", 2L, 100L);

                assertThrows(Exception.class, () -> {
                        reservation.selectSeatTicket(secondOrder, event, areaId, new seatPositionDTO(1, 1));
                });

                assertEquals(OrderStatus.COMPLETED, order.getStatus());
                assertTrue(secondOrder.getTickets().isEmpty());

                assertEquals(
                                SeatStatus.SOLD,
                                event.getSeatStatus(areaId, new SeatPosition(1, 1)));
        }
        @Test
        public void GivenTicketNotReserved_WhenCompleteCheckout_ThenThrowsExceptionAndOrderRemainsUnchanged() {
                Long areaId = 1L;

                reservation.selectSeatTicket(order, event, areaId, new seatPositionDTO(1, 1));
                reservation.removeTicketFromActiveOrder(order, event, order.getTickets().get(0).getTicketId());

                assertThrows(Exception.class, () -> {
                        reservation.completeCheckout(order, event);
                });

                assertEquals(OrderStatus.ACTIVE, order.getStatus());
                assertTrue(order.getTickets().isEmpty());

                assertEquals(
                                SeatStatus.AVAILABLE,
                                event.getSeatStatus(areaId, new SeatPosition(1, 1)));
        }

        @Test
        public void GivenValidOrder_WhenSubmitActiveOrderForCheckout_ThenStatusBecomesPendingCheckoutAndEventRemainsUnchanged() {
                Long areaId = 1L;

                reservation.selectSeatTicket(order, event, areaId, new seatPositionDTO(1, 1));

                BigDecimal total = reservation.submitActiveOrderForCheckout(order, event);

                assertEquals(OrderStatus.PENDING_CHECKOUT, order.getStatus());
                assertEquals(new BigDecimal("100.00"), total);

                assertEquals(
                                SeatStatus.RESERVED,
                                event.getSeatStatus(areaId, new SeatPosition(1, 1)));
        }

        @Test
        public void GivenReservedStandingTickets_WhenCompleteCheckout_ThenOrderCompletedAndStandingStateUpdated() {
                Long areaId = 2L;

                StandingArea standingArea = getStandingArea(event, areaId);

                reservation.selectStandingTicket(order, event, areaId, 4);
                reservation.submitActiveOrderForCheckout(order, event);

                reservation.completeCheckout(order, event);

                assertEquals(OrderStatus.COMPLETED, order.getStatus());
                assertEquals(4, order.getTickets().size());

                assertEquals(0, standingArea.getReserved());
                assertEquals(4, standingArea.getSold());
        }

        @Test
        public void GivenOrderWithoutTickets_WhenSubmitActiveOrderForCheckout_ThenThrowsException() {
                // Arrange

                Event event = createActiveEventWithSeatingAndStandingAreas(100L);

                ActiveOrder order = new ActiveOrder(
                                1L,
                                "token-1",
                                10L,
                                100L);

                // Act + Assert
                assertThrows(IllegalStateException.class, () -> {
                        reservation.submitActiveOrderForCheckout(order, event);
                });
        }

        @Test
        public void GivenPaymentFails_WhenPaymentFailedCalled_ThenOrderStatusBecomesPaymentFailed() {

                ActiveOrder order = new ActiveOrder(1L, "token-1", 10L, 100L);

                order.paymentFailed();

                assertEquals(OrderStatus.PAYMENT_FAILED, order.getStatus());
        }

        @Test
        public void GivenReservedSeat_WhenPaymentFails_ThenOrderBecomesPaymentFailedAndSeatRemainsReserved() {
                Long areaId = 1L;

                reservation.selectSeatTicket(order, event, areaId, new seatPositionDTO(1, 1));

                order.paymentFailed();

                assertEquals(OrderStatus.PAYMENT_FAILED, order.getStatus());

                assertEquals(
                                SeatStatus.RESERVED,
                                event.getSeatStatus(areaId, new SeatPosition(1, 1)));

                assertEquals(1, order.getTickets().size());
        }

        @Test
        public void GivenPaymentFailedAfterReservation_WhenBarcodeFails_ThenTicketsNotSoldAndSeatRemainsReserved() {
                Long areaId = 1L;

                reservation.selectSeatTicket(order, event, areaId, new seatPositionDTO(1, 1));
                reservation.submitActiveOrderForCheckout(order, event);

                order.paymentFailed();

                assertEquals(OrderStatus.PAYMENT_FAILED, order.getStatus());

                assertEquals(
                                SeatStatus.RESERVED,
                                event.getSeatStatus(areaId, new SeatPosition(1, 1)));

                assertEquals(1, order.getTickets().size());
        }

        @Test
        public void GivenOrderIsActive_WhenCompleteCheckout_ThenThrowsAndDoesNotSellTickets() {
                Long areaId = 1L;
                SeatPosition seatPosition = new SeatPosition(1, 1);

                reservation.selectSeatTicket(order, event, areaId, new seatPositionDTO(1, 1));

                assertThrows(IllegalStateException.class, () -> {
                        reservation.completeCheckout(order, event);
                });

                assertEquals(OrderStatus.ACTIVE, order.getStatus());

                assertEquals(
                                SeatStatus.RESERVED,
                                event.getSeatStatus(areaId, seatPosition));
        }
        @Test
        public void GivenReservedSeatWasReleased_WhenCompleteCheckout_ThenThrowsAndOrderRemainsNotCompleted() {
                Long areaId = 1L;
                SeatPosition seatPosition = new SeatPosition(1, 1);

                reservation.selectSeatTicket(order, event, areaId, new seatPositionDTO(1, 1));
                reservation.submitActiveOrderForCheckout(order, event);

                event.releaseSeat(areaId, seatPosition);

                assertThrows(Exception.class, () -> {
                        reservation.completeCheckout(order, event);
                });

                assertEquals(OrderStatus.PENDING_CHECKOUT, order.getStatus());
                assertEquals(
                                SeatStatus.AVAILABLE,
                                event.getSeatStatus(areaId, seatPosition));
        }

        @Test
        public void GivenLotteryIsNull_WhenCheckLottery_ThenOrderAndEventRemainUnchanged() {
                assertDoesNotThrow(() -> {
                        reservation.checkLottery(null, order.getUserId(), null);
                });

                assertEquals(OrderStatus.ACTIVE, order.getStatus());
                assertTrue(order.getTickets().isEmpty());
        }

        private Seat getSeat(Event event, Long areaId, SeatPosition position) {
                for (Element element : event.getMap().getElements()) {
                        if (element instanceof SeatingArea
                                        && element.getId() == areaId) {
                                return ((SeatingArea) element).getSeats().get(position);
                        }
                }
                return null;
        }

        
        private StandingArea getStandingArea(Event event, Long areaId) {
                for (Element element : event.getMap().getElements()) {
                        if (element instanceof StandingArea
                                        && element.getId() == areaId) {
                                return (StandingArea) element;
                        }
                }

                throw new IllegalArgumentException("Standing area not found");
        }

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
                                new Pair<>(10, 10));

                event.setStatus(Event.eventStatus.ACTIVE);

                EventMap map = new EventMap(new Pair<>(10, 10));

                SeatingArea seatingArea = new SeatingArea(
                                1L,
                                "Main Seating Area",
                                new Pair<>(0, 0),
                                new Pair<>(5, 5),
                                5,
                                5);

                StandingArea standingArea = new StandingArea(
                                2L,
                                "Main Standing Area",
                                new Pair<>(5, 0),
                                new Pair<>(5, 5),
                                100);

                map.addElement(seatingArea);
                map.addElement(standingArea);

                event.setMap(map);

                return event;
        }
}