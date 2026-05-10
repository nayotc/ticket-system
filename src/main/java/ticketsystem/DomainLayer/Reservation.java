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
    private static Long ticketIdCounter = 0L;

    public Reservation() {

    }

    public void selectSeatTicket(ActiveOrder order, Event event,Long areaId, seatPositionDTO position) {
       validateActive(order,  event);
      SeatPosition seatPosition = new SeatPosition(position.getRow(), position.getChair());
       event.reserveSeat(areaId, seatPosition);
       Ticket ticket = new Ticket(generateTicketId(), event.getId(), position.getRow(), position.getChair(), event.getTicktPrice());
        order.addTicket(ticket);
    } 

    public void selectStandingTicket(ActiveOrder order, Event event,Long areaId, int quantity) {
      validateActive(order,  event);
      event.reserveStanding(areaId, quantity);
        for(int i=0; i<quantity; i++) {
            Ticket ticket = new Ticket(generateTicketId(),event.getId(), 0, 0, event.getTicktPrice());
            order.addTicket(ticket);
      }
    }
  
    public void removeTicketFromActiveOrder(ActiveOrder order, Event event,Long ticketId) {
        validateActive(order,  event);
        Ticket ticket= order.deleteTicket(ticketId);
        releaseTicket(ticket, event);
    }
    public void changeQuantityOfStandingTickets(ActiveOrder order, Event event,Long areaId, int newQuantity) {
        validateActive(order,  event);
        int currentQuantity = order.getTickets().size();
        if (newQuantity > currentQuantity) {
            int toAdd = newQuantity - currentQuantity;
            selectStandingTicket(order, event, areaId, toAdd);
        } else if (newQuantity < currentQuantity) {
            int toRemove = currentQuantity - newQuantity;
            List<Ticket> tickets = order.getTickets();
            for (int i = 0; i < toRemove; i++) {
                Ticket ticket = tickets.get(tickets.size() - 1);
                removeTicketFromActiveOrder(order, event, ticket.getTicketId());
            }
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

    public Long generateTicketId() {
        return ++ticketIdCounter;
    }

}