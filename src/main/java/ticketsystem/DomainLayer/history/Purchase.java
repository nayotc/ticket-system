package ticketsystem.DomainLayer.history;

import java.util.ArrayList;
import java.util.List;


public class Purchase {
    private int purchaseId;
    private List<PurchasedTicket> tickets;
    private String eventName;
    private String location;
    private int memberId;
    private int companyId;

    public Purchase(int purchaseId, List<PurchasedTicket> tickets, String eventName, String location, int memberId, int companyId) {
        this.purchaseId = purchaseId;
        this.tickets = new ArrayList<>(tickets);
        this.eventName = eventName;
        this.location = location;
        this.memberId = memberId;
        this.companyId = companyId;
    }
    public int getId() {
        return purchaseId;
    }
    public void setId(int purchaseId) {
        this.purchaseId = purchaseId;
    }
    public Integer getMemberId() {
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
