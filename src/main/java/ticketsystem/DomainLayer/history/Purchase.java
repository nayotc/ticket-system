package ticketsystem.DomainLayer.history;

import java.util.ArrayList;
import java.util.List;


public class Purchase {
    private long purchaseId;
    private List<PurchasedTicket> tickets;
    private String eventName;
    private String location;
    private long memberId;
    private long companyId;

    public Purchase() {
    }
    public Purchase(long purchaseId, List<PurchasedTicket> tickets, String eventName, String location, long memberId, long companyId) {
        this.purchaseId = purchaseId;
        this.tickets = new ArrayList<>(tickets);
        this.eventName = eventName;
        this.location = location;
        this.memberId = memberId;
        this.companyId = companyId;
    }
    public long getPurchaseId() {
        return purchaseId;
    }
    public void setPurchaseId(long purchaseId) {
        this.purchaseId = purchaseId;
    }
    public long getMemberId() {
        return memberId;
    }
    public long getCompanyId() {
        return companyId;
    }
    public String getEventName() {
        return eventName;
    }
    public String getLocation() {
        return location;
    }
    public List<PurchasedTicket> getTickets() {
        return new ArrayList<>(tickets);
    }
    
    

    
}
