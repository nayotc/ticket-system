package ticketsystem.DomainLayer.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.time.ZoneId;

import ticketsystem.DTO.ActiveOrderDTO;
import ticketsystem.DTO.OrderDTO;
import ticketsystem.DTO.PurchaseDTO;
import ticketsystem.DTO.TicketDTO;

public class ActiveOrder {

    private Long orderId;
    private Long userId;
    private String sessionToken;    
    private Long eventId;
    private List<Ticket> tickets;
    private OrderStatus status;
    private LocalDateTime expiresAt;
    private int version;
    private static final long EXPIRATION_WARNING_BEFORE_MINUTES = 2;

    public ActiveOrder(Long orderId, String sessionToken, Long userId, Long eventId) {
        this.orderId = orderId;
        this.userId = userId;
        this.sessionToken = sessionToken;
        this.eventId = eventId;
        this.tickets = new ArrayList<>();
        this.status = OrderStatus.ACTIVE;
        this.expiresAt = LocalDateTime.now().plusMinutes(15);
        this.version = 0;
    }

    //copy constructor
    public ActiveOrder(ActiveOrder other){
        this.orderId = other.orderId;
        this.userId = other.userId;
        this.sessionToken = other.sessionToken;
        this.eventId = other.eventId;
        this.tickets = other.tickets.stream()
        .map(Ticket::copy)
        .collect(Collectors.toList());
        this.status = other.status;
        this.expiresAt = other.expiresAt;
        this.version = other.version;

    }

     public ActiveOrder copy() {
        return new ActiveOrder(this);
    }

    public void addTicket(Ticket ticket) {
        if(!ticket.getEventId().equals(eventId))
            throw new IllegalStateException("Ticket event ID does not match order event ID");

        this.tickets.add(ticket);
    }

    public Ticket deleteTicket(Long ticketId) {

        Ticket ticketToRemove = tickets.stream()
                .filter(ticket -> ticket.getTicketId().equals(ticketId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));
        tickets.remove(ticketToRemove);
        return ticketToRemove;

    }

    public void cancelOrder() {
        this.status = OrderStatus.CANCELLED;
    }

    public List<Ticket> getTickets() {
        return List.copyOf(tickets);
    }

    public Long getOrderId() {
        return this.orderId;
    }

    public void completeOrder() {
        this.status = OrderStatus.COMPLETED;
    }

    
    public Long getUserId() {
        return this.userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public Long getEventId() {
        return this.eventId;
    }

    public OrderStatus getStatus() {
        return this.status;
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
      
            validateHasTickets();
            validateTicketLimit();

            if (status != OrderStatus.ACTIVE && status != OrderStatus.PAYMENT_FAILED) {
                throw new IllegalStateException("Order is not active");
            }
    }

    public void validateHasTickets() {
        if (tickets == null || tickets.isEmpty()) {
            throw new IllegalStateException("No active order with tickets");
        }
    }

    public BigDecimal calculateTotalPrice() {
        BigDecimal total = BigDecimal.ZERO;
        for (Ticket ticket : tickets) {
            total = total.add(ticket.getPrice());
        }
        return total;
    }

    public void paymentFailed() {

        this.status = OrderStatus.PAYMENT_FAILED;
    }

    public String getSessionToken() {
        return this.sessionToken;

    }
 

     public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public long getExpiresAtEpochMillis() {
        return expiresAt
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
    }

    public boolean isAboutToExpire() {
        LocalDateTime now = LocalDateTime.now();

        return !isExpired()
                && !now.isBefore(expiresAt.minusMinutes(EXPIRATION_WARNING_BEFORE_MINUTES));
    }
    public enum OrderStatus {
    ACTIVE,
    PENDING_CHECKOUT,
    PAYMENT_FAILED,
    COMPLETED,
    CANCELLED
    }

    public void activeOrder(){
        this.status=OrderStatus.ACTIVE;
    }
    public int getVersion() {
        return version;
    }

    public void incrementVersion() {
        this.version++;
    }

    public OrderDTO toDTO(String eventName,String location, Long companyId, Long managedByMemberId, Long eventId,BigDecimal total) {
        List<PurchaseDTO> ticketDTOs = new ArrayList<>();

        for (Ticket ticket : tickets) {
            ticketDTOs.add(new PurchaseDTO(
                    ticket.getTicketId(),
                    ticket.getRow(),
                    ticket.getChair(),
                    ticket.getPrice(),
                    "ACTIVE",
                    ""
            ));
        }

        if(userId!=null)
            return new OrderDTO(0L,ticketDTOs,eventName,location ,userId,companyId, managedByMemberId, eventId,total);
        else
            return new OrderDTO(0L,ticketDTOs,eventName,location ,null ,companyId, managedByMemberId, eventId,total);
    }

     public ActiveOrderDTO toDTO() {
        List<TicketDTO> ticketDTOs = new ArrayList<>();

        for (Ticket ticket : tickets) {
            ticketDTOs.add(new TicketDTO(ticket.getTicketId(), ticket.getEventId(), ticket.getRow(), ticket.getChair(), ticket.getPrice()));
        }
            return new ActiveOrderDTO(orderId, userId, eventId, ticketDTOs, getExpiresAtEpochMillis());
        }

        //for testing purposes only
        public ActiveOrder(Long orderId, String sessionToken, Long userId, Long eventId, LocalDateTime expiresAt) {
            this.orderId = orderId;
            this.userId = userId;
            this.sessionToken = sessionToken;
            this.eventId = eventId;
            this.tickets = new ArrayList<>();
            this.status = OrderStatus.ACTIVE;
            this.expiresAt = expiresAt;
        }
        //only for testing purposes
        public void setExpiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
        }

    }