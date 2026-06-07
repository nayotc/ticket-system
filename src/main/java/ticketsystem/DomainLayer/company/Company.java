package ticketsystem.DomainLayer.company;

import ticketsystem.DomainLayer.policy.PolicyResult;
import ticketsystem.DomainLayer.policy.PurchasePolicy;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

import ticketsystem.DomainLayer.discount.DiscountPolicy;
import ticketsystem.DomainLayer.discount.DiscountCompositionType;
import ticketsystem.DomainLayer.discount.DiscountCondition;
import ticketsystem.DomainLayer.discount.ConditionalDiscount;
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
    private DiscountPolicy discountPolicy;
    private Double rate = 0.0; // for search and filtering
    private Double totalRating = 0.0; // for calculating average rating
    private Integer ratingCount = 0; // for calculating average rating
    private AtomicLong discountId=new AtomicLong(0L);

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

        DiscountTypes discount = new VisibleDiscount(
                name,
                percentage
        );

        discountPolicy.addDiscount(discount);
    }


    // conditional discount
  public void addConditionalDiscountToCompany(String name,
                                            BigDecimal percentage,
                                            DiscountCondition condition) {
    DiscountTypes discount = new ConditionalDiscount(
            name,
            percentage,
            condition
    );

    discountPolicy.addDiscount(discount);
}


    // coupon discount
    public void addCouponDiscountToCompany(
            String name,
            String couponCode,
            BigDecimal percentage,LocalDateTime endTime
    ) {

        DiscountTypes discount = new CouponDiscount(
                name,
                couponCode,
                percentage,endTime
        );

        discountPolicy.addDiscount(discount);
    }
    
   

    public BigDecimal calculateDiscountCompany(BigDecimal totalPrice, int ticketCount, String couponCode){
        return discountPolicy.calculateDiscount(totalPrice, ticketCount, couponCode);
    }

    // public void removeDiscountFromCompany(Long discountId) {
    //     discountPolicy.removeDiscount(discountId);
    // }
   
    
}