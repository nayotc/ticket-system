package ticketsystem.DomainLayer.order;

import java.util.ArrayList;
import java.util.List;

public class ActiveOrder {

    private int orderId;
    private Integer userId;
    private String sessionToken;
    private int eventId;
    private List<Ticket> tickets;
    private OrderStatus status;

    public ActiveOrder(int orderId, int userId,String sessionToken, int eventId) {
        this.orderId = orderId;
        this.userId = userId;
        this.sessionToken = sessionToken;
        this.eventId = eventId;
        this.tickets = new ArrayList<>();
        this.status = OrderStatus.ACTIVE;
    }


    public void addTicket(Ticket ticket) {
        this.tickets.add(ticket);

    }

    public Ticket deleteTicket(int ticketId) {
      
        for(Ticket ticket : tickets) {
            if (ticket.getTicketId() == ticketId) {
                tickets.remove(ticket);
                return ticket;
            }
        }
           
            throw new IllegalArgumentException("Ticket not found");
    
    }

    public List<Ticket> getTickets() {
        return this.tickets;
    }

    public int getOrderId() {
        return this.orderId;
    }

    public void completeOrder() {
        this.status = OrderStatus.COMPLETED;
    }
    
    public int getUserId() {
        return this.userId;
    }

    public int getEventId() {
        return this.eventId;
    }

    public OrderStatus getStatus() {
        return this.status;
    }

    public String getSessionToken() {
        return this.sessionToken;
    }

    public void submitForCheckout() {
       
        status = OrderStatus.PENDING_CHECKOUT;
    }

    public boolean isPendingCheckout() {
        return status == OrderStatus.PENDING_CHECKOUT;
    }

    public void validateTicketLimit() {
    int maxTickets = 10;

    if (tickets.size() > maxTickets) {
        throw new IllegalStateException("Ticket quantity exceeds limit");
    }

    }

    public void validateCanBeSubmittedBy() {
        if (userId == null) {
                throw new IllegalArgumentException(
                        "Guests cannot submit orders for checkout, please log in or register"
                );
            }

            validateHasTickets();
            validateTicketLimit();

            if (status != OrderStatus.ACTIVE) {
                throw new IllegalStateException("Order is not active");
            }
    }

    public void validateHasTickets() {
        if (tickets == null || tickets.isEmpty()) {
            throw new IllegalStateException("No active order with tickets");
        }
    }

    public int calculateTotalPrice() {
        int total = 0;
        for (Ticket ticket : tickets) {
            total += ticket.getPrice();
        }
        return total;
    }
    public void cancelOrder() {
        this.status = OrderStatus.CANCELLED;
    }

    public void paymentFailed() {
        
        this.status = OrderStatus.PAYMENT_FAILED;
    }


    public enum OrderStatus {
    ACTIVE,
    PENDING_CHECKOUT,
    PAYMENT_FAILED,
    COMPLETED,
    CANCELLED
}
}
