package ticketsystem.DomainLayer.event;

import java.time.LocalDateTime;

public class Event {
    private final Long id;
    private String name;
    private Long companyId;
    // member
    private LocalDateTime Date;
    private String location;
    private Long trafficThreshold;
    private enum eventStatus {ACTIVE, INACTIVE, CANCELLED};
    private eventStatus status;
    private EventCategory category;
    private EventMap map;
    private PurchasePolicy purchasePolicy;
    private DiscountPolicy discountPolicy;
    // waiting queue
    
    public Event(Long id, String name, Long companyId, LocalDateTime date, String location, Long trafficThreshold, EventCategory category, EventMap map, PurchasePolicy purchasePolicy, DiscountPolicy discountPolicy) {
        this.id = id;
        this.name = name;
        this.companyId = companyId;
        this.Date = date;
        this.location = location;
        this.trafficThreshold = trafficThreshold;
        this.status = eventStatus.ACTIVE; // Default status
        this.category = category;
        this.map = map;
        this.purchasePolicy = purchasePolicy;
        this.discountPolicy = discountPolicy;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public LocalDateTime getDate() {
        return Date;
    }

    public void setDate(LocalDateTime date) {
        Date = date;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Long getTrafficThreshold() {
        return trafficThreshold;
    }

    public void setTrafficThreshold(Long trafficThreshold) {
        this.trafficThreshold = trafficThreshold;
    }

    public eventStatus getStatus() {
        return status;
    }

    public void setStatus(eventStatus status) {
        this.status = status;
    }

    public EventCategory getCategory() {
        return category;
    }

    public void setCategory(EventCategory category) {
        this.category = category;
    }

    public EventMap getMap() {
        return map;
    }

    public void setMap(EventMap map) {
        this.map = map;
    }

    public PurchasePolicy getPurchasePolicy() {
        return purchasePolicy;
    }

    public void setPurchasePolicy(PurchasePolicy purchasePolicy) {
        this.purchasePolicy = purchasePolicy;
    }

    public DiscountPolicy getDiscountPolicy() {
        return discountPolicy;
    }

    public void setDiscountPolicy(DiscountPolicy discountPolicy) {
        this.discountPolicy = discountPolicy;
    }

}
