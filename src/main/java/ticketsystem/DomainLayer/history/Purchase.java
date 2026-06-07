package ticketsystem.DomainLayer.history;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

;

public class Purchase {

    private Long purchaseId;
    private List<PurchasedTicket> tickets;
    private String eventName;
    private String location;
    private Long memberId;
    private Long companyId;
    private Long managedByMemberId;
    private Long eventId;
    private BigDecimal totalPrice;
    private PaymentDetails paymentDetails;

    public Purchase() {
    }

    public Purchase(Long purchaseId, List<PurchasedTicket> tickets, String eventName, String location, Long memberId, Long companyId,
            Long managedByMemberId, Long eventId, BigDecimal totalPrice) {
        this(purchaseId, tickets, eventName, location, memberId, companyId, managedByMemberId, eventId, totalPrice, null);
    }

    public Purchase(Long purchaseId, List<PurchasedTicket> tickets, String eventName, String location, Long memberId, Long companyId,
            Long managedByMemberId, Long eventId, BigDecimal totalPrice, PaymentDetails paymentDetails) {
        this.purchaseId = purchaseId;
        this.tickets = new ArrayList<>(tickets);
        this.eventName = eventName;
        this.location = location;
        this.memberId = memberId;
        this.companyId = companyId;
        this.managedByMemberId = managedByMemberId;
        this.eventId = eventId;
        this.totalPrice = totalPrice;
        this.paymentDetails = paymentDetails;
    }

    public Long getPurchaseId() {
        return purchaseId;
    }

    public void setPurchaseId(Long purchaseId) {
        this.purchaseId = purchaseId;
    }

    public PaymentDetails getPaymentDetails() {
        return paymentDetails;
    }

    public Long getMemberId() {
        return memberId;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public Long getManagedByMemberId() {
        return managedByMemberId;
    }

    public String getEventName() {
        return eventName;
    }

    public String getLocation() {
        return location;
    }

    public Long getEventId() {
        return eventId;
    }

    public List<PurchasedTicket> getTickets() {
        return new ArrayList<>(tickets);
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

}
