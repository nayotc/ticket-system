package ticketsystem.DomainLayer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import ticketsystem.DTO.TicketDTO;
import ticketsystem.DTO.seatPositionDTO;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.event.Pair;
import ticketsystem.DomainLayer.event.Seat;
import ticketsystem.DomainLayer.event.SeatPosition;
import ticketsystem.DomainLayer.order.ActiveOrder;
import ticketsystem.DomainLayer.order.Ticket;

public class Reservation {
    private static int ticketIdCounter = 0;

    public Reservation() {

    }

    public void selectSeatTicket(ActiveOrder order, Event event,Long areaId, seatPositionDTO position) {
       validateActive(order,  event);
      SeatPosition seatPosition = new SeatPosition(position.getRow(), position.getChair());
       event.reserveSeat(areaId, seatPosition);
       Long eventId = event.getId();
       Ticket ticket = new Ticket(generateTicketId(), eventId.intValue(), position.getRow(), position.getChair(), /* event.getPrice() */0.0);
        order.addTicket(ticket);
    } 

    public void selectStandingTicket(ActiveOrder order, Event event,Long areaId, int quantity) {
      validateActive(order,  event);
      //event.reserveStanding(areaId, quantity);
        for(int i=0; i<quantity; i++) {
            Long eventId = event.getId();
            Ticket ticket = new Ticket(generateTicketId(), eventId.intValue(), 0, 0, /* event.getPrice() */0.0);
            order.addTicket(ticket);
      }
    }
  
   
    public void expire(Event event , ActiveOrder order) {
        //todo: release seats in event
    }

    

    public void validateActive(ActiveOrder order, Event event) {
        if (order.isExpired()) {
            expire(event, order);
            throw new IllegalStateException("Reservation expired");
        }
    }

    public int generateTicketId() {
        return ++ticketIdCounter;
    }

}