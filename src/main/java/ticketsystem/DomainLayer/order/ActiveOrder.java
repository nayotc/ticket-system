package ticketsystem.DomainLayer.order;

import java.util.ArrayList;
import java.util.List;

public class ActiveOrder {

    private int orderId;
    private Integer userId;
    private String sessionToken;
    private int eventId;
    private List<Ticket> tickets;

    public ActiveOrder(int orderId, int userId,String sessionToken, int eventId) {
        this.orderId = orderId;
        this.userId = userId;
        this.sessionToken = sessionToken;
        this.eventId = eventId;
        this.tickets = new ArrayList<>();
    }


    public void addTicket(Ticket ticket) {
        this.tickets.add(ticket);

    }

    public void deleteTicket(int ticketId) {
        this.tickets.removeIf(ticket -> ticket.getTicketId() == ticketId);
    }

    public List<Ticket> getTickets() {
        return this.tickets;
    }

    public int getOrderId() {
        return this.orderId;
    }

    public void completeOrder() {
        
    }
    
    public int getUserId() {
        return this.userId;
    }

    public int getEventId() {
        return this.eventId;
    }

    public String getSessionToken() {
        return this.sessionToken;
    }
}
