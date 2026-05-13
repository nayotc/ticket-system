package ticketsystem.DTO;

import java.util.List;

public class OrderDTO {
    
    private Long purchaseId;
    private List<PurchaseDTO> tickets;
    private String eventName;
    private String location;
    private Long memberId;
    private Long companyId;
    private Long managedByMemberId;

    //for json 
    public OrderDTO() {
    }

    public OrderDTO(Long purchaseId, List<PurchaseDTO> tickets, String eventName, 
                    String location, Long memberId, Long companyId, Long managedByMemberId) {
        this.purchaseId = purchaseId;
        this.tickets = tickets;
        this.eventName = eventName;
        this.location = location;
        this.memberId = memberId;
        this.companyId = companyId;
        this.managedByMemberId = managedByMemberId;
    }

    public Long getPurchaseId() {
        return purchaseId;
    }

    public void setPurchaseId(Long purchaseId) {
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

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public Long getManagedByMemberId() {
        return managedByMemberId;
    }

    public void setManagedByMemberId(Long managedByMemberId) {
        this.managedByMemberId = managedByMemberId;
    }
}
