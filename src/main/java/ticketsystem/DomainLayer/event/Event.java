package ticketsystem.DomainLayer.event;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

public class Event {

    private final long id;
    private String name;
    private LocalDateTime Date;
    private String location;
    private long trafficThreshold;

    private enum status {
        ACTIVE, INACTIVE, CANCELLED
    };
    private EventCategory category;
    private EventMap map;
    private PurchasePolicy purchasePolicy;
    private DiscountPolicy discountPolicy;
    private AtomicInteger activeReservationsCount = new AtomicInteger(0); // for load management and virtual queue
    // waiting queue
    private int version = 0;

    public Event(Long id, LocalDateTime date, String name, Long companyId, Long openedBy, String location, Long trafficThreshold, EventCategory category, Pair<Integer, Integer> mapSize) {
        this.id = id;
        this.name = name;
        this.Date = date;
        this.location = location;
        this.trafficThreshold = trafficThreshold;
        this.category = category;
        this.map = map;
        this.purchasePolicy = purchasePolicy;
        this.discountPolicy = discountPolicy;
    }

    public long getId() {
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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setTrafficThreshold(long trafficThreshold) {
        this.trafficThreshold = trafficThreshold;
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

    public long getTrafficThreshold() {
        return trafficThreshold;
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
