package ticketsystem.DTO;

import java.util.List;

import ticketsystem.DomainLayer.order.Ticket;

public class ActiveOrderDTO {
    private Long orderId;
    private Long userId;  
    private Long eventId;
    private List<TicketDTO> tickets;
    private Long expiresAtEpochMillis;

    public ActiveOrderDTO(Long orderId, Long userId, Long eventId, List<TicketDTO> tickets, Long expiresAtEpochMillis) {
        this.orderId = orderId;
        this.userId = userId;
        this.eventId = eventId;
        this.tickets = tickets;
        this.expiresAtEpochMillis = expiresAtEpochMillis;
    }
    
    public Long getOrderId() {
        return orderId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getEventId() {
        return eventId;
    }

    public List<TicketDTO> getTickets() {
        return tickets;
    }

    public Long getExpiresAtEpochMillis() {
        return expiresAtEpochMillis;
    }

}