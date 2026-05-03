package ticketsystem.DomainLayer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.event.Seat;
import ticketsystem.DomainLayer.order.ActiveOrder;
import ticketsystem.DomainLayer.order.Ticket;

public class Reservation {

    private final int reservationId;
    private final ActiveOrder order;
    private final Event event;
    private final LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);
    private int ticketIdCounter = 0;

    public Reservation(int reservationId, ActiveOrder order, Event event) {
        this.reservationId = reservationId;
        this.order = order;
        this.event = event;
    }


 public void selectSeatTicket(int eventId,int row, int chair) {

        validateActive();
         Seat seat = event.getSeatByLocation(row, chair);
        if (seat.getStatus() != Seat.SeatStatus.AVAILABLE) {
            throw new IllegalStateException("Seat is not available");
        }
        /*לבדוק שלא מוגרל */

        Ticket ticket=new Ticket(generateTicketId(), eventId, seat.getPosition().row(), seat.getPosition().number(), seat.getPrice());
        seat.setStatus(Seat.SeatStatus.RESERVED);
        order.addTicket(ticket);
    }


    public void selectStandingTicket(int eventId, double price, int quantity) {
        validateActive();
        if (event.getStandingArea().getAvailableSpots() < quantity) {

            throw new IllegalStateException("not available");
        }
        /*לבדוק שלא מוגרל */
        if (event.getStandingArea().hasAvailableSpots()) {
            for(int i=0; i<quantity; i++) {
                 Ticket ticket = new Ticket(generateTicketId(), eventId, 0, 0, price);
                event.getStandingArea().reserveStandingSpot();
                order.addTicket(ticket);
            }
        } else {
            throw new IllegalStateException("No standing spots available");
        }
    }
   
    public int getReservationId() {
        return reservationId;
    }

    public int getOrderId() {
    return order.getOrderId();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public void expire() {
    for (Ticket ticket : new ArrayList<>(order.getTickets())) {

        if (ticket.getRow() == 0 && ticket.getChair() == 0) {
            event.getStandingArea().releaseStandingSpot();
        } else {
            event.getSeatByLocation(ticket.getRow(), ticket.getChair())
                    .setStatus(Seat.SeatStatus.AVAILABLE);
        }

        order.deleteTicket(ticket.getTicketId());
        }
    }
    
    //mybe move to order
    public int generateTicketId() {
        return ++ticketIdCounter;
    }

    private void validateActive() {
    if (isExpired()) {
        expire();
        throw new IllegalStateException("Reservation expired");
    }
}
}