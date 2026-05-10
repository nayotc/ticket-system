package ticketsystem.DomainLayer.order;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import ticketsystem.DTO.OrderDTO;
import ticketsystem.DTO.TicketDTO;

public class ActiveOrder {

    private Long orderId;
    private Long userId;
    private String sessionToken;    
    private Long eventId;
    private List<Ticket> tickets;
    private OrderStatus status;
    private final LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);
    private boolean timerStopped= false;
    


    public ActiveOrder(Long orderId, String sessionToken, Long userId, Long eventId) {
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

    public Ticket deleteTicket(Long ticketId) {

        Ticket ticketToRemove = tickets.stream()
                .filter(ticket -> ticket.getTicketId() == ticketId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));
        tickets.remove(ticketToRemove);
        return ticketToRemove;
    
    }

    public void cancelOrder() {
        this.status = OrderStatus.CANCELLED;
    }

    public List<Ticket> getTickets() {
        return this.tickets;
    }

    public Long getOrderId() {
        return this.orderId;
    }

    
    public Long getUserId() {
        return this.userId;
    }

    public Long getEventId() {
        return this.eventId;
    }

    public OrderStatus getStatus() {
        return this.status;
    }

    public String getSessionToken() {
        return this.sessionToken;
    }
 
  
    public void stopTimer() {
        this.timerStopped = true;
    }

    public boolean isStopped() {
        return timerStopped;
    }

     public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public enum OrderStatus {
    ACTIVE,
    PENDING_CHECKOUT,
    PAYMENT_FAILED,
    COMPLETED,
    CANCELLED
}


public OrderDTO toDTO() {
    List<TicketDTO> ticketDTOs = new ArrayList<>();

    for (Ticket ticket : tickets) {
        ticketDTOs.add(new TicketDTO(ticket.getTicketId(), ticket.getEventId(), ticket.getRow(), ticket.getChair(), ticket.getPrice(), ""));
    }
    String nameEvent=""; //TODO get event name
    String location=""; //TODO get event location
    int companyId=0; //TODO get company id

    return new OrderDTO(0,ticketDTOs,nameEvent,location ,userId,companyId);

    }
}