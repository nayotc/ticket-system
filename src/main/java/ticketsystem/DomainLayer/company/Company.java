package ticketsystem.DomainLayer.company;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

import ticketsystem.DomainLayer.company.DiscountCompanyPolicy;
import ticketsystem.DomainLayer.discount.DiscountCompositionType;
import ticketsystem.DomainLayer.discount.ConditionalDiscount;
import ticketsystem.DomainLayer.discount.ConditionalDiscount.Condition;
import ticketsystem.DomainLayer.discount.CouponDiscount;
import ticketsystem.DomainLayer.discount.DiscountTypes;
import ticketsystem.DomainLayer.discount.VisibleDiscount;

public class Company {
    private static long idCounter = 1;

    private long id;
    private String name;
    private final long founderId;
    private boolean isActive;
    private PurchasePolicy purchasePolicy;
    private DiscountCompanyPolicy discountPolicy;
    private Double rate = 0.0; // for search and filtering
    private Double totalRating = 0.0; // for calculating average rating
    private Integer ratingCount = 0; // for calculating average rating
    private AtomicLong discountId=new AtomicLong(0L);

    // Version field for Optimistic Locking
    private long version;

    public Company(String name, long founderId, PurchasePolicy purchasePolicy, DiscountCompanyPolicy discountPolicy) {
        this.id = idCounter++;

        this.name = name;
        this.founderId = founderId;
        this.isActive = true;
        this.purchasePolicy = purchasePolicy;
        this.discountPolicy = discountPolicy;
        this.version = 0; // Initialize version

    }

    // Copy Constructor
    public Company(Company other) {
        this.id = other.id;
        this.name = other.name;
        this.founderId = other.founderId;
        this.isActive = other.isActive;
        this.version = other.version;
        this.rate = other.rate;
        this.totalRating = other.totalRating;
        this.ratingCount = other.ratingCount;
        this.purchasePolicy = other.purchasePolicy;
        this.discountPolicy = other.discountPolicy;
        this.discountId = new AtomicLong(other.discountId.get());
    }
    // --- Getters & Setters ---

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getFounderUsername() {
        return founderId;
    }

    public boolean isActive() {
        return isActive;
    }

    public PurchasePolicy getPurchasePolicy() {
        return purchasePolicy;
    }

    public void setPurchasePolicy(PurchasePolicy purchasePolicy) {
        this.purchasePolicy = purchasePolicy;
    }

    public DiscountCompanyPolicy getDiscountPolicy() {
        return discountPolicy;
    }

    public void setDiscountPolicy(DiscountCompanyPolicy discountPolicy) {
        this.discountPolicy = discountPolicy;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public long getFounderId() {
        return this.founderId;
    }

    public Double getRate() {
        return this.rate;
    }

    public void setRate(Double rate) {
        this.totalRating += rate;
        this.ratingCount++;
        this.rate = this.totalRating / this.ratingCount;
    }

    public void inactivate() {
        this.isActive = false;
    }

    // --- Use Cases Logic ---

    public void closeOrSuspend() throws Exception {
        if (!this.isActive) {
            throw new Exception("Company is already inactive.");
        }

        this.isActive = false;
    }

    public void reopenCompany() throws Exception {
        if (this.isActive) {
            throw new Exception("The company is already Active. No action needed.");
        }

        this.isActive = true;
    }

    public void closeBySystemAdmin() throws Exception {
        if (!this.isActive) {
            throw new Exception("Company is already inactive.");
        }

        this.isActive = false;
    }
    public void setDiscountCompositionType(DiscountCompositionType compositionType){
        getDiscountPolicy().setDiscountCompositionType(compositionType);

    }
    public DiscountCompositionType getDiscountCompositionType(){
        return getDiscountPolicy().getDiscountCompositionType();

    }

    public Long getNextId() {
        return discountId.incrementAndGet();
    }
// visible discount
    public void addVisibleDiscountToCompany(String name, BigDecimal percentage) {
        validateDiscountName(name);
        validatePercentage(percentage);

        DiscountTypes discount = new VisibleDiscount(
                name,
                getNextId(),
                percentage
        );

        discountPolicy.addDiscount(discount);
    }


    // conditional discount
    public void addConditionalDiscountToCompany(String name,
            LocalDateTime startTime, LocalDateTime endTime,
            BigDecimal percentage, Condition condition,
            Integer ticketThreshold) {

        validateDiscountName(name);
        validatePercentage(percentage);

        if (condition == null) {
            throw new IllegalArgumentException("Discount condition cannot be null");
        }

        switch (condition) {
            case MIN_TICKET:
            case MAX_TICKET:
                validateTicketThreshold(ticketThreshold);
                break;

            case DATE:
                validateDateRange(startTime, endTime);
                break;

            default:
                throw new IllegalArgumentException("Unsupported discount condition");
        }

        DiscountTypes discount = new ConditionalDiscount(
                name,
                getNextId(),
                startTime,
                endTime,
                percentage,
                condition,
                ticketThreshold
        );

        discountPolicy.addDiscount(discount);
    }


    // coupon discount
    public void addCouponDiscountToCompany(
            String name,
            String couponCode,
            BigDecimal percentage,LocalDateTime endTime
    ) {
        validateDiscountName(name);
        validatePercentage(percentage);

        if (couponCode == null || couponCode.isBlank()) {
            throw new IllegalArgumentException("Coupon code cannot be empty");
        }

        DiscountTypes discount = new CouponDiscount(
                name,
                getNextId(),
                couponCode,
                percentage,endTime
        );

        discountPolicy.addDiscount(discount);
    }
    
   

    public BigDecimal calculateDiscountCompany(BigDecimal totalPrice, int ticketCount, String couponCode){
        return discountPolicy.calculateDiscount(totalPrice, ticketCount, couponCode);
    }

    public void removeDiscountFromCompany(Long discountId) {
        discountPolicy.removeDiscountFromCompany(discountId);
    }
    //validation function
    private void validateDiscountName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Discount name cannot be empty");
        }
    }

    private void validatePercentage(BigDecimal percentage) {
        if (percentage == null) {
            throw new IllegalArgumentException("Discount percentage cannot be null");
        }

        if (percentage.compareTo(BigDecimal.ZERO) < 0 ||
                percentage.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException(
                    "Discount percentage must be between 0 and 100");
        }
    }

    private void validateTicketThreshold(Integer ticketThreshold) {
        if (ticketThreshold == null || ticketThreshold <= 0) {
            throw new IllegalArgumentException(
                    "Ticket threshold must be positive");
        }
    }

    private void validateDateRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException(
                    "Discount dates cannot be null for date condition");
        }

        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException(
                    "End time cannot be before start time");
        }
    }
    
}