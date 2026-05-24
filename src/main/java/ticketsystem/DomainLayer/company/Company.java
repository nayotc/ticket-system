package ticketsystem.DomainLayer.company;

import java.time.LocalDateTime;

import ticketsystem.DTO.DiscountRequestDTO;

public class Company {
    private static long idCounter = 1;

    private long id;
    private String name;
    private final long founderId;
    private boolean isActive;
    private PurchasePolicy purchasePolicy;
    private DiscountPolicy discountPolicy;
    private Double rate = 0.0; // for search and filtering
    private Double totalRating = 0.0; // for calculating average rating
    private Integer ratingCount = 0; // for calculating average rating

    // Version field for Optimistic Locking
    private long version;

    public Company(String name, long founderId, PurchasePolicy purchasePolicy, DiscountPolicy discountPolicy) {
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

    public DiscountPolicy getDiscountPolicy() {
        return discountPolicy;
    }

    public void setDiscountPolicy(DiscountPolicy discountPolicy) {
        this.discountPolicy = discountPolicy;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    private boolean isFounder(long memberId) {
        return this.founderId == memberId;
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

    public boolean addDiscountToCompany(DiscountRequestDTO discountDTO) {

    DiscountTypes discount;

    switch (discountDTO.getDiscountKind()) {

        case VISIBLE:
            discount = new VisibleDiscount(
                    discountDTO.getName(),
                    discountDTO.getStartTime(),
                    discountDTO.getEndTime(),
                    discountDTO.getPercentage(),
                    discountDTO.getTargetTicketType()
            );
            break;

        case CONDITIONAL:
            discount = new ConditionalDiscount(
                    discountDTO.getName(),
                    discountDTO.getStartTime(),
                    discountDTO.getEndTime(),
                    discountDTO.getPercentage(),
                    discountDTO.getTargetTicketType(),
                    discountDTO.getCondition()
            );
            break;

        case COUPON:
            discount = new CouponDiscount(
                    discountDTO.getName(),
                    discountDTO.getStartTime(),
                    discountDTO.getEndTime(),
                    discountDTO.getCouponCode(),
                    discountDTO.getPercentage(),
                    discountDTO.getFixedAmount()
                    
            );
           break;

        default:
            throw new IllegalArgumentException("Unsupported discount type");
    }
    getDiscountPolicy().addDiscount(discount);
    return true;
}
}