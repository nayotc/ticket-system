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
    public Ticket createTicket(int eventId, int row, int chair, double price) {
        return new Ticket(ticketIdCounter++, eventId, row, chair, price);
    }


    public void reserveSeatTicket(int eventId,int row, int chair) {
         Seat seat = event.getSeatByLocation(row, chair);
        if (seat.getStatus() != Seat.SeatStatus.AVAILABLE) {
            throw new IllegalStateException("Seat is not available");
        }
        Ticket ticket=createTicket(eventId, seat.getPosition().row(), seat.getPosition().number(), seat.getPrice());
        seat.setStatus(Seat.SeatStatus.RESERVED);
        order.addTicket(ticket);
    }

    public void reserveStandingTicket(int eventId, double price) {
        if (event.getStandingArea().getAvailableSpots() <= 0) {
            throw new IllegalStateException("not available");
        }
        if (event.getStandingArea().hasAvailableSpots()) {
            Ticket ticket = createTicket(eventId, 0, 0, price);
            event.getStandingArea().reserveStandingSpot();
            order.addTicket(ticket);
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

}