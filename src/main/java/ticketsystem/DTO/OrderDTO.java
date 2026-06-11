package ticketsystem.DTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class OrderDTO {
    
    private Long purchaseId;
    private List<PurchaseDTO> tickets;
    private String eventName;
    private String location;
    private Long memberId;
    private Long companyId;
    private Long managedByMemberId;
    private Long eventId;
    private BigDecimal totalPrice;
    private Integer transactionId ;


    //for json 
    public OrderDTO() {
    }

    public OrderDTO(Long purchaseId, List<PurchaseDTO> tickets, String eventName, 
                    String location, Long memberId, Long companyId, Long managedByMemberId, Long eventId,BigDecimal total, Integer transactionId) {
        this.purchaseId = purchaseId;
        this.tickets = tickets;
        this.eventName = eventName;
        this.location = location;
        this.memberId = memberId;
        this.companyId = companyId;
        this.managedByMemberId = managedByMemberId;
        this.eventId = eventId;
        this.totalPrice=total;
        this.transactionId = transactionId;
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
    public Long getEventId(){
        return eventId;
    }

    public void setManagedByMemberId(Long managedByMemberId) {
        this.managedByMemberId = managedByMemberId;
    }
    public BigDecimal getTotalPrice(){
        return totalPrice;
    }
    public void setTotalPrice(BigDecimal totalPrice){
        this.totalPrice=totalPrice;
    }
    public Integer getTransactionId() {
        return transactionId;
    }
    public void setPaymentDetails(Integer transactionId) {
        this.transactionId = transactionId;
    }

}
