package ticketsystem.DomainLayer.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.text.Normalizer;
import java.util.concurrent.atomic.AtomicInteger;

import ticketsystem.DomainLayer.event.EventCategory;
import ticketsystem.DomainLayer.event.EventMap;
import ticketsystem.DomainLayer.event.PurchasePolicy;
import ticketsystem.DomainLayer.SearchCriteria;
import ticketsystem.DomainLayer.event.DiscountPolicy;

public class Event {

    public enum eventStatus {
        DRAFT, ACTIVE, INACTIVE, CANCELLED
    };

    private final Long id;
    private String name;
    private Long companyId;
    private Long openedBy; // userId of the creator
    private LocalDateTime Date;
    private EventLocation location;
    private Long trafficThreshold;
    private eventStatus status;
    private EventCategory category;
    private String artistName; 
    private EventMap map;
    private BigDecimal TicketPrice;
    private Double rate = 0.0; // for search and filtering
    private Double totalRating = 0.0; // for calculating average rating
    private Integer ratingCount = 0; // for calculating average rating
    private PurchasePolicy purchasePolicy;
    private DiscountPolicy discountPolicy;
    private AtomicInteger activeReservationsCount = new AtomicInteger(0); // for load management and virtual queue
    private int version = 0;
    // waiting queue
    
    public Event(Long id,LocalDateTime date,String name, Long companyId, Long openedBy, EventLocation location, Long trafficThreshold, EventCategory category,String artistName, BigDecimal ticketPrice, Pair<Integer, Integer> mapSize) {
        this.id = id;
        this.name = name;
        this.Date = date;
        this.companyId = companyId;
        this.openedBy = openedBy;
        this.artistName = artistName;
        this.location = location;
        this.trafficThreshold = trafficThreshold;
        this.status = eventStatus.DRAFT; // Default status until the map is set and the event is activated
        this.category = category;
        this.TicketPrice = ticketPrice;
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
    
    public EventLocation getLocation() {
        return location;
    }

    public void setLocation(EventLocation location) {
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

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public EventMap getMap() {
        return map;
    }

    public void setMap(EventMap map) {
        this.map = map;
    }

    public BigDecimal getTicketPrice() {
        return TicketPrice;
    }

    public void setTicketPrice(BigDecimal ticketPrice) {
        this.TicketPrice = ticketPrice;
    }

    public Double getRate() {
        return rate;
    }

    public void setRate(Double rate) {
        this.totalRating += rate;
        this.ratingCount++;
        this.rate = this.totalRating / this.ratingCount;
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

    // use case: search and filtering 

    public boolean matchesSearchCriteria(SearchCriteria criteria) {
        if (criteria == null) {
            throw new IllegalArgumentException("Search criteria cannot be null");
        }

        return matchesSearchTerm(criteria.getSearchTerm()) && 
               matchesCategory(criteria.getCategory()) &&
               matchesDateRange(criteria.getFromDate(), criteria.getToDate()) &&
               matchesLocation(criteria.getLocation()) &&
               matchesPriceRange(criteria.getMinPrice(), criteria.getMaxPrice()) &&
               matchesRate(criteria.getEventRate()) &&
               matchesArtist(criteria.getArtist());
               
    }

    private boolean matchesSearchTerm(String searchTerm) {
        if (searchTerm == null || searchTerm.isBlank()) {
            return true;
        }

        String term = normalize(searchTerm);

        return containsNormalized(name, term)
                || containsNormalized(location.name(), term)
                || containsNormalized(category.name(), term)
                || containsNormalized(artistName, term);
    }

    private boolean matchesCategory(EventCategory requestedCategory) {
        if (requestedCategory == null) {
            return true; // event matches any category if no specific category is requested
        }

        return this.category == requestedCategory;
    }

    private boolean matchesDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate != null && Date.isBefore(startDate)) {
            return false;
        }
        if (endDate != null && Date.isAfter(endDate)) {
            return false;
        }
        return true;
    }

    private boolean matchesLocation(EventLocation requestedLocation) {
        if (requestedLocation == null) {
            return true;   // event matches any location if no specific location is requested
        }
        return requestedLocation == this.location;
    }

    private boolean matchesPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice == null && maxPrice == null) {
            return true;  // event matches any price if no specific price range is requested
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("Minimum price cannot be greater than maximum price");
        }
        if (minPrice != null && this.TicketPrice.compareTo(minPrice) < 0) {  // event price is less than the minimum price
            return false;
        }
        if (maxPrice != null && this.TicketPrice.compareTo(maxPrice) > 0) {  // event price is greater than the maximum price
            return false;
        }
        return true; // the event price is within the specified range
    }

    private boolean matchesRate(Double requestedRate){
        if(requestedRate == null){
            return true;  // event matches any rate if no specific rate is requested
        }
        return this.rate >= requestedRate;
    }

    private boolean matchesArtist(String requestedArtist) {
        if (requestedArtist == null || requestedArtist.isEmpty()) {
            return true;  // event matches any artist if no specific artist is requested
        }
        return containsNormalized(this.artistName, normalize(requestedArtist));
    }

    private boolean containsNormalized(String source, String normalizedTerm) {
        if (source == null) {
            return false;
        }
        return normalize(source).contains(normalizedTerm);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")              // removes accents
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");          // removes space, _, -, /, ., comma, etc.
    }


<<<<<<< HEAD
    public int getVersion() {
        return version;
    }

    public void incrementVersion() {
        this.version++;
    }
=======
>>>>>>> f4fd19f (fix comments)

}
