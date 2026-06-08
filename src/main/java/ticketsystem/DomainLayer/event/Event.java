package ticketsystem.DomainLayer.event;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import ticketsystem.DomainLayer.SearchCriteria;
import ticketsystem.DomainLayer.discount.ConditionalDiscount;
import ticketsystem.DomainLayer.discount.ConditionalDiscount.Condition;
import ticketsystem.DomainLayer.discount.CouponDiscount;
import ticketsystem.DomainLayer.discount.DiscountCompositionType;
import ticketsystem.DomainLayer.discount.DiscountPolicy;
import ticketsystem.DomainLayer.discount.DiscountTypes;
import ticketsystem.DomainLayer.discount.VisibleDiscount;
import ticketsystem.DomainLayer.discount.DiscountCalculationResult;
import ticketsystem.DomainLayer.event.Seat.SeatStatus;
import ticketsystem.DomainLayer.policy.PolicyResult;
import ticketsystem.DomainLayer.policy.PurchasePolicy;



public class Event {

    public enum eventStatus {
        DRAFT, ACTIVE, INACTIVE, CANCELLED
    };

    private final Long id;
    private String name;
    private final Long companyId;
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
    private int version;
    private AtomicLong discountId=new AtomicLong(0L);
    // waiting queue
    private SaleStatus saleStatus = SaleStatus.NOT_STARTED;

    public Event(Long id, LocalDateTime date, String name, Long companyId, Long openedBy, EventLocation location, Long trafficThreshold, EventCategory category, String artistName, BigDecimal ticketPrice, Pair<Integer, Integer> mapSize) {
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
        this.purchasePolicy = PurchasePolicy.noRestrictions();
        this.discountPolicy =new DiscountPolicy(DiscountCompositionType.MAX);//defult
        this.version = 0;
    }

    // copy constructor
    public Event(Event other) {
        this.id = other.id;
        this.name = other.name;
        this.Date = other.Date;
        this.companyId = other.companyId;
        this.openedBy = other.openedBy;
        this.location = other.location;
        this.trafficThreshold = other.trafficThreshold;
        this.category = other.category;
        this.artistName = other.artistName;
        this.TicketPrice = other.TicketPrice;
        this.map = new EventMap(other.map); // Deep copy of the map
        this.status = other.status;
        this.saleStatus = other.saleStatus;
        this.rate = other.rate;
        this.totalRating = other.totalRating;
        this.ratingCount = other.ratingCount;
        this.purchasePolicy = other.purchasePolicy;
        this.discountPolicy = other.discountPolicy;
        this.activeReservationsCount = new AtomicInteger(other.activeReservationsCount.get());
        this.version = other.version;
        this.discountId = new AtomicLong(other.discountId.get());
        
    }

    public Event copy() {
        return new Event(this);
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

    public boolean isActive() {
        return this.status == eventStatus.ACTIVE;
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
        if (purchasePolicy == null) {
            throw new IllegalArgumentException("Purchase policy cannot be null");
        }
        this.purchasePolicy = purchasePolicy;
    }

    public DiscountPolicy getDiscountPolicy() {
        return discountPolicy;
    }
    public void setDiscountPolicy(DiscountPolicy discountPolicy) {
        this.discountPolicy = discountPolicy;
    }
    private Long getNextDiscountId() {
        return discountId.incrementAndGet();
    }

    public boolean isSoldOut() {
        return map.isSoldOut();
    }

    public SaleStatus getSaleStatus() {
        return saleStatus;
    }

    public  void setSaleStatus(SaleStatus saleStatus) {
        this.saleStatus = saleStatus;
    }

    public void SoldOut() {
        if (this.map != null && this.map.isSoldOut()) {
            this.saleStatus = SaleStatus.SOLD_OUT;
        }
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

    public void reserveSpot(Long areaId, int quantity) {
        this.map.reserveSpot(areaId, quantity);
    }

    public void releaseSpot(Long areaId, int quantity) {
        this.map.releaseSpot(areaId, quantity);
    }

    public void sellSpot(Long areaId, int quantity) {
        this.map.sellSpot(areaId, quantity);
    }

    public SeatStatus getSeatStatus(Long areaId, SeatPosition position) {
        return this.map.isSeatAvailable(areaId, position);
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

        return matchesSearchTerm(criteria.getSearchTerm())
                && matchesCategory(criteria.getCategory())
                && matchesDateRange(criteria.getFromDate(), criteria.getToDate())
                && matchesLocation(criteria.getLocation())
                && matchesPriceRange(criteria.getMinPrice(), criteria.getMaxPrice())
                && matchesRate(criteria.getEventRate())
                && matchesArtist(criteria.getArtist());

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

    private boolean matchesRate(Double requestedRate) {
        if (requestedRate == null) {
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
                .replaceAll("\\p{M}", "") // removes accents
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");          // removes space, _, -, /, ., comma, etc.
    }

    public int getVersion() {
        return version;
    }

    public void incrementVersion() {
        this.version++;
    }

    public void updateDetails(String name, LocalDateTime date, EventLocation location, Long trafficThreshold, EventCategory category, String artistName, BigDecimal ticketPrice) {
        if (name != null && !name.isEmpty()) {
            this.name = name;
        }
        if (date != null && date.isAfter(LocalDateTime.now())) {
            this.Date = date;
        }
        if (location != null) {
            this.location = location;
        }
        if (trafficThreshold != null && trafficThreshold > 0) {
            this.trafficThreshold = trafficThreshold;
        }
        if (category != null) {
            this.category = category;
        }
        if (artistName != null && !artistName.isEmpty()) {
            this.artistName = artistName;
        }
        if (ticketPrice != null && ticketPrice.compareTo(BigDecimal.ZERO) >= 0) {
            this.TicketPrice = ticketPrice;
        }
    }

    public void cancel() {
    if (this.status == eventStatus.CANCELLED) {
        throw new IllegalStateException("Event is already cancelled");
    }
    if (this.Date.isBefore(LocalDateTime.now())) {
        throw new IllegalStateException("Cannot cancel an event that has already occurred");
    }
    this.status = eventStatus.CANCELLED;
    }

    public void canPurchase(int quantity, int age) {
        PolicyResult result = this.purchasePolicy.validate(quantity, age);
        if (result == null) {
            throw new IllegalStateException("Purchase policy validation failed");
        }

        if (!result.isAllowed()) {
            String message = result.getMessage();

            if (message == null || message.isBlank()) {
                message = "User does not satisfy the purchase policy";
            }

            throw new IllegalArgumentException(message);
        }
    }

    // discount related methods
    public void addVisibleDiscountToEvent(String name, BigDecimal percentage) {
        DiscountTypes discount = new VisibleDiscount(name, getNextDiscountId(), percentage);
        discountPolicy.addDiscount(discount);
    }

    public void addCouponDiscountToEvent(String name, String couponCode,
            BigDecimal percentage, LocalDateTime endTime) {
        DiscountTypes discount = new CouponDiscount(
                name, getNextDiscountId(), couponCode, percentage, endTime);
        discountPolicy.addDiscount(discount);
    }

    public void addConditionalDiscountToEvent(String name,
            LocalDateTime startTime, LocalDateTime endTime,
            BigDecimal percentage, Condition condition,
            Integer ticketThreshold) {

        DiscountTypes discount = new ConditionalDiscount(
                name, getNextDiscountId(), startTime, endTime,
                percentage, condition, ticketThreshold);
        discountPolicy.addDiscount(discount);
    }

    public void setDiscountCompositionType(DiscountCompositionType compositionType) {
        discountPolicy.setDiscountCompositionType(compositionType);
    }

    public BigDecimal calculateDiscountEvent(BigDecimal totalPrice, int ticketCount, String couponCode) {
        return discountPolicy.calculateDiscount(totalPrice, ticketCount, couponCode);
    }

    public void removeDiscountFromEvent(Long discountId) {
        discountPolicy.removeDiscount(discountId);
    }

    /**
     * Calculates the event's discount policy and returns a detailed domain result.
     *
     * This method is used when callers need to know not only the total discount
     * amount, but also which event-level discounts were actually applied.
     *
     * The existing calculateDiscountEvent(...) method is kept unchanged for callers
     * that only need the numeric discount amount.
     *
     * @param totalPrice the price before applying event-level discounts
     * @param ticketCount the number of tickets in the order
     * @param couponCode the coupon code entered by the user, if any
     * @return detailed result of the event-level discount calculation
     */
    public DiscountCalculationResult calculateDiscountEventDetails(BigDecimal totalPrice, int ticketCount, String couponCode) {
        return discountPolicy.calculateDiscountDetails(totalPrice, ticketCount, couponCode);
    }

    public List<DiscountTypes> getDiscounts() {
        return discountPolicy.getDiscounts();
    }

    public String toString() {
        return "Event{" +
                "id=" + id.toString() +
                ", name='" + name + '\'' +
                ", companyId=" + companyId.toString() +
                ", openedBy=" + openedBy.toString() +
                ", Date=" + Date.toString() +
                ", location=" + location.toString() +
                ", trafficThreshold=" + trafficThreshold.toString() +
                ", status=" + status.toString() +
                ", category=" + category.toString() +
                ", artistName='" + artistName + '\'' +
                ", TicketPrice=" + TicketPrice.toString() +
                ", rate=" + rate.toString() +
                ", activeReservationsCount=" + activeReservationsCount.toString() +
                '}';
    }
}

