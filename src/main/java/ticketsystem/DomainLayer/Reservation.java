package ticketsystem.DomainLayer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import ticketsystem.DTO.PurchaseDTO;
import ticketsystem.DTO.seatPositionDTO;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.event.SeatPosition;
import ticketsystem.DomainLayer.order.ActiveOrder;
import ticketsystem.DomainLayer.order.Ticket;

public class Reservation {
    private final AtomicLong ticketIdCounter = new AtomicLong(0);
    public Reservation() {

    }

    // UC 2.4, 2.5
    public void selectSeatTicket(ActiveOrder order, Event event,Long areaId, seatPositionDTO position) {
      
        SeatPosition seatPosition = new SeatPosition(position.getRow(), position.getChair());
       event.reserveSeat(areaId, seatPosition);
       Ticket ticket = new Ticket(generateTicketId(), event.getId(), areaId, position.getRow(), position.getChair(), event.getTicktPrice());
        order.addTicket(ticket);
    } 

    public void selectStandingTicket(ActiveOrder order, Event event,Long areaId, int quantity) {
      event.reserveSpot(areaId, quantity);
        for(int i=0; i<quantity; i++) {
            Ticket ticket = new Ticket(generateTicketId(),event.getId(), areaId, 0, 0, event.getTicktPrice());
            order.addTicket(ticket);
      }
    }

    //UC 2.7
    public void removeTicketFromActiveOrder(ActiveOrder order, Event event,Long ticketId) {
        Ticket ticket= order.deleteTicket(ticketId);
        releaseTicket(ticket, event);
        
    }


    public void submitActiveOrderForCheckout(ActiveOrder order, Event event) {
        order.validateCanBeSubmittedBy();
        order.submitForCheckout();
        
    }

    
    public void completeCheckout(ActiveOrder order, Event event) {
        order.completeOrder();       
        for (Ticket ticket : new ArrayList<>(order.getTickets())) {

            if(ticket.getRow()==0 && ticket.getChair()==0) {
                event.sellSpot(ticket.getAreaId());
            } else {
                event.sellSeat(ticket.getAreaId(),new SeatPosition(ticket.getRow(), ticket.getChair()));
            }
        }

    }
    
    public BigDecimal calculateTotalPrice(ActiveOrder order, Event event) {
        BigDecimal total = order.calculateTotalPrice();
        //EVENT?
        return total;
    }

    public void expire(Event event , ActiveOrder order) {
    for (Ticket ticket : new ArrayList<>(order.getTickets())) {
        releaseTicket(ticket, event);
        order.deleteTicket(ticket.getTicketId());
        }
        order.cancelOrder();
    }


    public void releaseTicket(Ticket ticket, Event event) {
     if(ticket.getRow()==0 && ticket.getChair()==0) {
           event. releaseSpot(ticket.getAreaId());
        } else {
            SeatPosition position = new SeatPosition(ticket.getRow(), ticket.getChair());   
            event.releaseSeat(ticket.getAreaId(), position);
        }

}

    public Long generateTicketId() {
        return ticketIdCounter.incrementAndGet();
    }

}