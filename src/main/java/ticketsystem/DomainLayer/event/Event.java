package ticketsystem.DomainLayer.event;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.persistence.*;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import ticketsystem.ApplicationLayer.ISystemLogger;
import ticketsystem.DTO.Event.EventMapDTO;
import ticketsystem.DTO.Event.IAreaDTO;
import ticketsystem.DomainLayer.SearchCriteria;
import ticketsystem.DomainLayer.discount.CouponDiscount;
import ticketsystem.DomainLayer.discount.DiscountCompositionType;
import ticketsystem.DomainLayer.discount.DiscountPolicy;
import ticketsystem.DomainLayer.discount.DiscountTypes;
import ticketsystem.DomainLayer.discount.VisibleDiscount;
import ticketsystem.DomainLayer.discount.DiscountCalculationResult;
import ticketsystem.DomainLayer.event.Seat.SeatStatus;
import ticketsystem.DomainLayer.policy.PolicyResult;
import ticketsystem.DomainLayer.policy.PurchasePolicy;

@Entity
@Table(name = "events")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Event {

    public enum eventStatus {
        DRAFT, ACTIVE, INACTIVE, CANCELLED,CANCELLATION_PENDING,CANCELLATION_FAILED
    };

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "opened_by")
    private Long openedBy; // userId of the creator

    @Column(name = "event_date")
    private LocalDateTime date;

    @Enumerated(EnumType.STRING)
    @Column(name = "location")
    private EventLocation location;

    @Column(name = "traffic_threshold")
    private Long trafficThreshold;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private eventStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private EventCategory category;

    @Column(name = "artist_name")
    private String artistName;

    @Embedded
    private EventMap map;

    @Column(name = "ticket_price", precision = 12, scale = 2)
    private BigDecimal ticketPrice;

    @Column(name = "rate")
    private Double rate = 0.0; // for search and filtering

    @Column(name = "total_rating")
    private Double totalRating = 0.0; // for calculating average rating

    @Column(name = "rating_count")
    private Integer ratingCount = 0; // for calculating average rating

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "purchase_policy_id", nullable = false, unique = true)
    private PurchasePolicy purchasePolicy;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "discount_policy_id", nullable = false, unique = true)
    private DiscountPolicy discountPolicy;

    @Transient
    private AtomicInteger activeReservationsCount = new AtomicInteger(0); // for load management and virtual queue

    @Version
    @Column(name = "version")
    private int version;

    // waiting queue
    @Enumerated(EnumType.STRING)
    @Column(name = "sale_status")
    private SaleStatus saleStatus = SaleStatus.NOT_STARTED;

    protected Event() {}

    public Event(LocalDateTime date, String name, Long companyId, Long openedBy, EventLocation location, Long trafficThreshold, EventCategory category, String artistName, BigDecimal ticketPrice, Pair<Integer, Integer> mapSize) {
        this.name = name;
        this.date = date;
        this.companyId = companyId;
        this.openedBy = openedBy;
        this.artistName = artistName;
        this.location = location;
        this.trafficThreshold = trafficThreshold;
        this.status = eventStatus.DRAFT; // Default status until the map is set and the event is activated
        this.category = category;
        this.ticketPrice = ticketPrice;
        this.map = new EventMap(mapSize);
        this.purchasePolicy = PurchasePolicy.noRestrictions();
        this.discountPolicy =new DiscountPolicy(DiscountCompositionType.MAX);//defult
        this.version = 0;
    }

    // copy constructor
    public Event(Event other) {
        this.id = other.id;
        this.name = other.name;
        this.date = other.date;
        this.companyId = other.companyId;
        this.openedBy = other.openedBy;
        this.location = other.location;
        this.trafficThreshold = other.trafficThreshold;
        this.category = other.category;
        this.artistName = other.artistName;
        this.ticketPrice = other.ticketPrice;
        this.map = other.map == null ? null : new EventMap(other.map); // Deep copy of the map
        this.status = other.status;
        this.saleStatus = other.saleStatus;
        this.rate = other.rate;
        this.totalRating = other.totalRating;
        this.ratingCount = other.ratingCount;
        this.purchasePolicy = other.purchasePolicy;
        this.discountPolicy = other.discountPolicy;
        this.activeReservationsCount = new AtomicInteger(other.getActiveReservationsCount());
        this.version = other.version;
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
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
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

    public BigDecimal getMinimalTicketPrice() {
        if (map != null) {
            BigDecimal minimumAreaPrice = map.getMinimumAreaPrice();

            if (minimumAreaPrice != null) {
                return minimumAreaPrice;
            }
        }

        return ticketPrice;
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
       if (discountPolicy == null) {
            throw new IllegalArgumentException("Discount policy cannot be null");
        }
        this.discountPolicy = discountPolicy;
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

    public void defineMap(EventMap newMap) {
        if (newMap == null) {
            throw new IllegalArgumentException("Event map cannot be null");
        }

        if (status != eventStatus.DRAFT) {
            throw new IllegalStateException("Event map can only be defined for a draft event");
        }

        if (map != null && map.getElements() != null && !map.getElements().isEmpty()) {
            throw new IllegalStateException("Event map has already been defined");
        }

        newMap.validateForActivation();

        this.map = newMap;
        this.status = eventStatus.ACTIVE;
    }

    public void updateDraftMap(EventMap updatedMap) {
        if (updatedMap == null) {
            throw new IllegalArgumentException("Event map cannot be null");
        }

        if (status != eventStatus.DRAFT) {
            throw new IllegalStateException("The complete map can only be replaced for a draft event");
        }

        this.map = updatedMap;
    }

    public void updateActiveMap(List<Area> newAreas, Map<Long, Area> updatedAreas) {
        if (status != eventStatus.ACTIVE) {
            throw new IllegalStateException("This map operation is only allowed for an active event");
        }
        this.map.updateActiveAreas(newAreas, updatedAreas);
    }

    private void increaseAvailableTickets(long quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException(
                    "Added ticket quantity cannot be negative"
            );
        }

        if (quantity == 0) {
            return;
        }

        // TODO: if implement remainingTickets, uncomment the following lines
//        this.remainingTickets = Math.addExact(
//                this.remainingTickets,
//                Math.toIntExact(quantity)
//        );

        if (this.saleStatus == SaleStatus.SOLD_OUT) {
            this.saleStatus = SaleStatus.ONGOING;
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

    public void restoreActiveReservationsCount(int count) {
        if (count < 0) {
            throw new IllegalArgumentException(
                    "Active reservations count cannot be negative"
            );
        }

        this.activeReservationsCount.set(count);
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
    public void markCancellationPending() {
        if (status == eventStatus.CANCELLED) {
            throw new IllegalStateException("Event is already canceled");
        }
        this.status = eventStatus.CANCELLATION_PENDING;
        this.saleStatus=SaleStatus.ENDED;
    }

    public void markCancellationFailed() {
        if (status != eventStatus.CANCELLATION_PENDING) {
            throw new IllegalStateException("Event cancellation is not pending");
        }
        this.status = eventStatus.CANCELLATION_FAILED;
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
        if (startDate != null && date.isBefore(startDate)) {
            return false;
        }
        if (endDate != null && date.isAfter(endDate)) {
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

    public boolean matchesPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        if (map != null && map.getMinimumAreaPrice() != null) {
            return map.hasAreaInPriceRange(minPrice, maxPrice);
        }

        if (minPrice != null
                && maxPrice != null
                && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException(
                    "Minimum price cannot be greater than maximum price"
            );
        }

        return ticketPrice != null
                && (minPrice == null || ticketPrice.compareTo(minPrice) >= 0)
                && (maxPrice == null || ticketPrice.compareTo(maxPrice) <= 0);
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
            this.date = date;
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
            this.ticketPrice = ticketPrice;
        }
    }

    public void cancel() {
    if (this.status == eventStatus.CANCELLED) {
        throw new IllegalStateException("Event is already cancelled");
    }
    if (this.date.isBefore(LocalDateTime.now())) {
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
        DiscountTypes discount = new VisibleDiscount(name,  percentage);
        discountPolicy.addDiscount(discount);
    }

    public void addCouponDiscountToEvent(String name, String couponCode,
            BigDecimal percentage, LocalDateTime endTime) {
        DiscountTypes discount = new CouponDiscount(
                name,  couponCode, percentage, endTime);
        discountPolicy.addDiscount(discount);
    }

    public int getSoldTicketsCount() {
        AtomicInteger sold = new AtomicInteger(0);

        map.getElements().forEach(element -> {
            if (element instanceof StandingArea standingArea) {
                sold.addAndGet((int) standingArea.getSold());
            }

            if (element instanceof SeatingArea seatingArea) {
                sold.addAndGet((int) seatingArea.getSeats()
                        .values()
                        .stream()
                        .filter(seat -> seat.getStatus() == SeatStatus.SOLD)
                        .count());
            }
        });

        return sold.get();
    }
    public int getCapacity() {
        AtomicInteger capacity = new AtomicInteger(0);

        map.getElements().forEach(element -> {
            if (element instanceof StandingArea standingArea) {
                capacity.addAndGet((int) standingArea.getCapacity());
            }

            if (element instanceof SeatingArea seatingArea) {
                capacity.addAndGet(seatingArea.getSeats().size());
            }
        });

        return capacity.get();
    }


    public void setDiscountCompositionType(DiscountCompositionType compositionType) {
        discountPolicy.setDiscountCompositionType(compositionType);
    }

    public BigDecimal calculateDiscountEvent(BigDecimal totalPrice, int ticketCount, String couponCode) {
        return discountPolicy.calculateDiscount(totalPrice, ticketCount, couponCode);
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

    public String getAreaName(Long areaId){
        return map.getAreaName(areaId);
    }

    public BigDecimal getAreaPrice(Long areaId) {
        BigDecimal areaPrice = map == null ? null : map.getAreaPrice(areaId);
        return areaPrice != null ? areaPrice : ticketPrice;
    }

    @Override
    public String toString() {
        return "Event{"
                + "id=" + id
                + ", name='" + name + '\''
                + ", companyId=" + companyId
                + ", openedBy=" + openedBy
                + ", Date=" + date
                + ", location=" + location
                + ", trafficThreshold=" + trafficThreshold
                + ", status=" + status
                + ", category=" + category
                + ", artistName='" + artistName + '\''
                + ", TicketPrice=" + ticketPrice
                + ", rate=" + rate
                + ", activeReservationsCount=" + activeReservationsCount
                + '}';
    }
}

