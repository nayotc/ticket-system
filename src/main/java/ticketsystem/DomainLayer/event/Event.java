package ticketsystem.DomainLayer.event;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

public class Event {

    public enum eventStatus {
        DRAFT, ACTIVE, INACTIVE, CANCELLED
    };

    private final Long id;
    private String name;
    private Long companyId;
    private Long openedBy; // userId of the creator
    private LocalDateTime Date;
    private String location;
    private Long trafficThreshold;
    private eventStatus status;
    private EventCategory category;
    private EventMap map;
    private PurchasePolicy purchasePolicy;
    private DiscountPolicy discountPolicy;
    private AtomicInteger activeReservationsCount = new AtomicInteger(0); // for load management and virtual queue
    private int version = 0;
    // waiting queue

    public Event(Long id, LocalDateTime date, String name, Long companyId, Long openedBy, String location, Long trafficThreshold, EventCategory category, Pair<Integer, Integer> mapSize) {
        this.id = id;
        this.name = name;
        this.Date = date;
        this.companyId = companyId;
        this.openedBy = openedBy;
        this.location = location;
        this.trafficThreshold = trafficThreshold;
        this.status = eventStatus.DRAFT; // Default status until the map is set and the event is activated
        this.category = category;
        this.map = new EventMap(mapSize);
        this.purchasePolicy = new PurchasePolicy("Default purchase policy");
        this.discountPolicy = new DiscountPolicy();
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

    public LocalDateTime getDate() {
        return Date;
    }

    public void setDate(LocalDateTime date) {
        Date = date;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public Long getOpenedBy() {
        return openedBy;
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

    public boolean isSoldOut() {
        return map.isSoldOut();
    }

    // use case: ticket reservation 
    public void reserveSeat(Long areaId, SeatPosition position) {
        this.map.reserveSeat(areaId, position);
    }

    public void releaseSeat(Long areaId, SeatPosition position) {
        this.map.releaseSeat(areaId, position);
    }

    public void sellSeat(Long areaId, SeatPosition position) {
        this.map.sellSeat(areaId, position);
    }

    public void reserveSpot(Long areaId) {
        this.map.reserveSpot(areaId);
    }

    public void releaseSpot(Long areaId) {
        this.map.releaseSpot(areaId);
    }

    public void sellSpot(Long areaId) {
        this.map.sellSpot(areaId);
    }

    // use case: virtual queue and load management
    public boolean isOverloaded() {
        return this.activeReservationsCount.get() >= this.trafficThreshold;
    }

    public void incrementActiveReservations() {
        this.activeReservationsCount.incrementAndGet();
    }

    public void decrementActiveReservations() {
        if (this.activeReservationsCount.get() > 0) {
            this.activeReservationsCount.decrementAndGet();
        }
    }

    public int getActiveReservationsCount() {
        return activeReservationsCount.get();
    }

    public int getVersion() {
        return version;
    }

    public void incrementVersion() {
        this.version++;
    }

}
