package ticketsystem.DomainLayer.order;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import ticketsystem.DTO.ActiveOrderDTO;
import ticketsystem.DTO.OrderDTO;
import ticketsystem.DTO.PaymentDetails;
import ticketsystem.DTO.PurchaseDTO;
import ticketsystem.DTO.TicketDTO;

@Entity
@Table(name = "active_orders")
public class ActiveOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @Column(name = "session_token", nullable = false, length = 512)
    private String sessionToken;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "user_id")
    private Long userId;

    @Version
    @Column(name = "version")
    private Integer version;

    @OneToMany(mappedBy = "activeOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Ticket> tickets = new ArrayList<>();

    private static final long EXPIRATION_WARNING_BEFORE_MINUTES = 2;

    protected ActiveOrder() {
    }

    public ActiveOrder(Long orderId, String sessionToken, Long userId, Long eventId) {
        if (orderId != null) {
            this.orderId = orderId;
        }
        this.sessionToken = sessionToken;
        this.userId = userId;
        this.eventId = eventId;
        this.status = OrderStatus.ACTIVE;
        this.expiresAt = LocalDateTime.now().plusMinutes(15);
    }

    public ActiveOrder(ActiveOrder other) {
        this.orderId = other.orderId;
        this.sessionToken = other.sessionToken;
        this.userId = other.userId;
        this.eventId = other.eventId;
        this.tickets = new ArrayList<>();
        for (Ticket originalTicket : other.tickets) {
            Ticket copiedTicket = originalTicket.copy();
            copiedTicket.setActiveOrder(this);
            this.tickets.add(copiedTicket);
        }
        this.status = other.status;
        this.expiresAt = other.expiresAt;
        this.version = other.version;
    }

    public ActiveOrder copy() {
        return new ActiveOrder(this);
    }

    public void addTicket(Ticket ticket) {
        if (!ticket.getEventId().equals(eventId)) {
            throw new IllegalStateException("Ticket event ID does not match order event ID");
        }
        ticket.setActiveOrder(this);
        this.tickets.add(ticket);
    }

    public Ticket deleteTicket(Long ticketId) {
        Ticket ticketToRemove = tickets.stream()
                .filter(ticket -> Objects.equals(ticket.getTicketId(), ticketId))
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

    public void assignMissingTicketIds(LongSupplier ticketIdSupplier) {
        for (Ticket ticket : tickets) {
            if (ticket.getTicketId() == null) {
                ticket.setTicketId(ticketIdSupplier.getAsLong());
            }
        }
    }

    public Long getOrderId() {
        return this.orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public void completeOrder() {
        this.status = OrderStatus.COMPLETED;
    }

    public Long getUserId() {
        return userId;
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


    public void validateCanBeSubmittedBy() {
        validateHasTickets();
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

    public void activeOrder() {
        this.status = OrderStatus.ACTIVE;
    }

    public Integer getVersion() {
        return version;
    }

    public void incrementVersion() {
        this.version = (version == null ? 0 : version) + 1;
    }

    public OrderDTO toDTO(String eventName, String location, Long companyId, Long managedByMemberId,
                          Long eventId, BigDecimal total, Integer transactionId) {
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
        if (getUserId() != null) {
            return new OrderDTO(0L, ticketDTOs, eventName, location, getUserId(), companyId,
                    managedByMemberId, eventId, total,transactionId,false);
        }
        return new OrderDTO(0L, ticketDTOs, eventName, location, null, companyId,
                managedByMemberId, eventId, total, transactionId,false);
    }

    public ActiveOrderDTO toDTO() {
        List<TicketDTO> ticketDTOs = new ArrayList<>();
        for (Ticket ticket : tickets) {
            ticketDTOs.add(new TicketDTO(ticket.getTicketId(), ticket.getEventId(),
                    ticket.getRow(), ticket.getChair(), ticket.getPrice()));
        }
        return new ActiveOrderDTO(orderId, getUserId(), eventId, ticketDTOs, getExpiresAtEpochMillis());
    }

    public ActiveOrder(Long orderId, String sessionToken, Long userId, Long eventId, LocalDateTime expiresAt) {
        if (orderId != null) {
            this.orderId = orderId;
        }
        this.sessionToken = sessionToken;
        this.userId = userId;
        this.eventId = eventId;
        this.tickets = new ArrayList<>();
        this.status = OrderStatus.ACTIVE;
        this.expiresAt = expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
