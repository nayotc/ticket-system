package ticketsystem.DomainLayer.order;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ActiveOrder {

    private int orderId;
    private Long userId;
    private String sessionToken;
    private int eventId;
    private List<Ticket> tickets;
    private final LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);

    public ActiveOrder(int orderId, Long userId,String sessionToken, int eventId) {
        this.orderId = orderId;
        this.userId = userId;
        this.sessionToken = sessionToken;
        this.eventId = eventId;
        this.tickets = new ArrayList<>();
    }


    public void addTicket(Ticket ticket) {
        this.tickets.add(ticket);

    }

    public Ticket deleteTicket(int ticketId) {

        Ticket ticketToRemove = tickets.stream()
                .filter(ticket -> ticket.getTicketId() == ticketId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));
        tickets.remove(ticketToRemove);
        return ticketToRemove;
    
    }

    public List<Ticket> getTickets() {
        return this.tickets;
    }

    public int getOrderId() {
        return this.orderId;
    }

    public void completeOrder() {
        
    }
    
    public Long getUserId() {
        return this.userId;
    }

    public int getEventId() {
        return this.eventId;
    }

    public String getSessionToken() {
        return this.sessionToken;
    }

     public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }


}
