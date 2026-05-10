package ticketsystem.DTO;

import java.util.List;

public class OrderDTO {
    
    private int purchaseId;
    private List<PurchaseDTO> tickets;
    private String eventName;
    private String location;
    private long memberId;
    private Long companyId;

    //for json 
    public OrderDTO() {
    }

    public OrderDTO(int purchaseId, List<PurchaseDTO> tickets, String eventName, 
                    String location, long memberId, Long companyId) {
        this.purchaseId = purchaseId;
        this.tickets = tickets;
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

    public List<PurchaseDTO> getTickets() {
        return tickets;
    }

    public void setTickets(List<PurchaseDTO> tickets) {
        this.tickets = tickets;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public long getMemberId() {
        return memberId;
    }

    public void setMemberId(long memberId) {
        this.memberId = memberId;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }
}
