package ticketsystem.DomainLayer.history;

import java.util.ArrayList;
import java.util.List;


public class Purchase {
    private int purchaseId;
    private List<PurchasedTicket> tickets;
    private String eventName;
    private String location;
    private long memberId;
    private int companyId;

    public Purchase() {
    }
    public Purchase(int purchaseId, List<PurchasedTicket> tickets, String eventName, String location, long memberId, int companyId) {
        this.purchaseId = purchaseId;
        this.tickets = new ArrayList<>(tickets);
        this.eventName = eventName;
        this.location = location;
        this.memberId = memberId;
        this.companyId = companyId;
    }
    public int getPurchaseId() {
        return purchaseId;
    }
    public void setPurchaseId(int purchaseId) {
        this.purchaseId = purchaseId;
    }
    public long getMemberId() {
        return memberId;
    }
    public Integer getCompanyId() {
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
