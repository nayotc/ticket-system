package ticketsystem.DomainLayer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import ticketsystem.DTO.TicketDTO;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.event.Seat;
import ticketsystem.DomainLayer.order.ActiveOrder;
import ticketsystem.DomainLayer.order.Ticket;

public class Reservation {
    private static int ticketIdCounter = 0;

    public Reservation() {

    }

    public void selectSeatTicket(ActiveOrder order, Event event, int row, int chair) {
       validateActive(order,  event);
       event.reserveSeat(row, chair);
       Ticket ticket = new Ticket(generateTicketId(), event.getEventId(), row, chair, event.getPrice());
        order.addTicket(ticket);
    }

    public void selectStandingTicket(ActiveOrder order, Event event, int quantity) {
      validateActive(order,  event);
  
        for(int i=0; i<quantity; i++) {
            Ticket ticket = new Ticket(generateTicketId(), event.getEventId(), 0, 0, event.getPrice());
            order.addTicket(ticket);
      }
    }

    public void viewActiveOrder(ActiveOrder order) {
        validateActive(order,  event);
    }

    public void removeTicketFromActiveOrder(ActiveOrder order, Event event,int ticketId) {
        validateActive(order,  event);
        Ticket ticket= order.deleteTicket(ticketId);
        releaseTicket(ticket, event);
    }
   
   
    public void expire(Event event , ActiveOrder order) {
    for (Ticket ticket : new ArrayList<>(order.getTickets())) {
        releaseTicket(ticket, event);
        order.deleteTicket(ticket.getTicketId());
        }
    }

    public void validateActive(ActiveOrder order, Event event) {
        if (order.isExpired()) {
            expire(event, order);
            throw new IllegalStateException("Reservation expired");
        }
    }

public void releaseTicket(Ticket ticket, Event event) {
     if(ticket.getRow()==0 && ticket.getChair()==0) {
            event.releaseStandingTicket(ticket);
        } else {
            event.releaseSeatTicket(ticket);
        }
}
    public int generateTicketId() {
        return ++ticketIdCounter;
    }

}