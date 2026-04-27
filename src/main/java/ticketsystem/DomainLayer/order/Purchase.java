package ticketsystem.DomainLayer.order;

import java.util.ArrayList;
import java.util.List;

public class Purchase extends Order {
    private String eventName;
    private String location;
    private int memberId;
    private int companyId;

    public Purchase(int orderId, List<Ticket> tickets, String eventName, String location, int memberId, int companyId) {
        super(orderId);
        this.tickets = new ArrayList<>(tickets);
        this.eventName = eventName;
        this.location = location;
        this.memberId = memberId;
        this.companyId = companyId;
    }
    public void setId(int orderId) {
        this.orderId = orderId;
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
    public List<Ticket> getTickets() {
        return new ArrayList<>(tickets);
    }
    
    

    
}
