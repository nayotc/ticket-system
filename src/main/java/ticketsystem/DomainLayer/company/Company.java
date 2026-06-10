package ticketsystem.DomainLayer.company;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

import ticketsystem.DomainLayer.discount.ConditionalDiscount;
import ticketsystem.DomainLayer.discount.CouponDiscount;
import ticketsystem.DomainLayer.discount.DiscountCalculationResult;
import ticketsystem.DomainLayer.discount.DiscountCompositionType;
import ticketsystem.DomainLayer.discount.DiscountCondition;
import ticketsystem.DomainLayer.discount.DiscountPolicy;
import ticketsystem.DomainLayer.discount.DiscountTypes;
import ticketsystem.DomainLayer.discount.VisibleDiscount;
import ticketsystem.DomainLayer.policy.PolicyResult;
import ticketsystem.DomainLayer.policy.PurchasePolicy;

@Entity
@Table(name = "companies")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "founder_id", nullable = false, updatable = false)
    private Long founderId;

    @Column(name = "active", nullable = false)
    private boolean active;

    /*
     * Temporary for the Company DB branch:
     * purchasePolicy and discountPolicy persistence are handled separately.
     */
    @Transient
    private PurchasePolicy purchasePolicy;

    @Transient
    private DiscountPolicy discountPolicy;

    @Column(name = "rate", nullable = false)
    private Double rate = 0.0;

    @Column(name = "total_rating", nullable = false)
    private Double totalRating = 0.0;

    @Column(name = "rating_count", nullable = false)
    private Integer ratingCount = 0;

    @Column(name = "discount_id_counter", nullable = false)
    private long discountIdCounter = 0L;

    @Version
    @Column(name = "version")
    private long version;

    protected Company() {
        // Required by JPA
    }

    public Company(String name, long founderId, PurchasePolicy purchasePolicy, DiscountPolicy discountPolicy) {
        this.name = name;
        this.founderId = founderId;
        this.active = true;
        this.purchasePolicy = purchasePolicy;
        this.discountPolicy = discountPolicy;
        this.rate = 0.0;
        this.totalRating = 0.0;
        this.ratingCount = 0;
        this.discountIdCounter = 0L;
        this.version = 0L;
        ensureDefaultPolicies();
    }

    public Company(Company other) {
        this.id = other.id;
        this.name = other.name;
        this.founderId = other.founderId;
        this.active = other.active;
        this.purchasePolicy = other.purchasePolicy;
        this.discountPolicy = other.discountPolicy;
        this.rate = other.rate;
        this.totalRating = other.totalRating;
        this.ratingCount = other.ratingCount;
        this.discountIdCounter = other.discountIdCounter;
        this.version = other.version;
        ensureDefaultPolicies();
    }

    /**
     * Ensures that policies are initialized after Hibernate loads the company.
     */
    @PostLoad
    private void onPostLoad() {
        ensureDefaultPolicies();
    }

    /**
     * Restores the default company policies when no policy is currently set.
     */
    private void ensureDefaultPolicies() {
        if (this.purchasePolicy == null) {
            this.purchasePolicy = PurchasePolicy.noRestrictions();
        }

        if (this.discountPolicy == null) {
            this.discountPolicy =
                    new DiscountPolicy(DiscountCompositionType.MAX);
        }
    }

    public long getId() {
        return id == null ? 0L : id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Long getJpaId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getFounderUsername() {
        return founderId == null ? 0L : founderId;
    }

    public boolean isActive() {
        return active;
    }

    public PurchasePolicy getPurchasePolicy() {
        ensureDefaultPolicies();
        return purchasePolicy;
    }

    public void setPurchasePolicy(PurchasePolicy purchasePolicy) {
        if (purchasePolicy == null) {
            throw new IllegalArgumentException("Purchase policy cannot be null");
        }
        this.purchasePolicy = purchasePolicy;
    }

    public DiscountPolicy getDiscountPolicy() {
        ensureDefaultPolicies();
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
        return founderId == null ? 0L : founderId;
    }

    public Double getRate() {
        return rate;
    }

    public void setRate(Double rate) {
        if (rate == null) {
            throw new IllegalArgumentException("Rate cannot be null");
        }

        this.totalRating += rate;
        this.ratingCount++;
        this.rate = this.totalRating / this.ratingCount;
    }

    public void inactivate() {
        this.active = false;
    }

    public void closeOrSuspend() throws Exception {
        if (!this.active) {
            throw new Exception("Company is already inactive.");
        }

        this.active = false;
    }

    public void reopenCompany() throws Exception {
        if (this.active) {
            throw new Exception("The company is already Active. No action needed.");
        }

        this.active = true;
    }

    public void closeBySystemAdmin() throws Exception {
        if (!this.active) {
            throw new Exception("Company is already inactive.");
        }

        this.active = false;
    }

    public void canPurchase(int quantity, int age) {
        PolicyResult result = getPurchasePolicy().validate(quantity, age);
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

    public void setDiscountCompositionType(DiscountCompositionType compositionType) {
        getDiscountPolicy().setDiscountCompositionType(compositionType);
    }

    public DiscountCompositionType getDiscountCompositionType() {
        return getDiscountPolicy().getDiscountCompositionType();
    }

    /**
     * Generates the next internal identifier for a company discount.
     *
     * @return the next discount identifier
     */
    public synchronized Long getNextId() {
        discountIdCounter++;
        return discountIdCounter;
    }

    // Visible discounts
    public void addVisibleDiscountToCompany(String name, BigDecimal percentage) {
        DiscountTypes discount = new VisibleDiscount(
                name,
                percentage
        );

        getDiscountPolicy().addDiscount(discount);
    }

    public void addConditionalDiscountToCompany(String name,
                                                BigDecimal percentage,
                                                DiscountCondition condition) {
        DiscountTypes discount = new ConditionalDiscount(
                name,
                percentage,
                condition
        );

        getDiscountPolicy().addDiscount(discount);
    }

    public void addCouponDiscountToCompany(String name,
                                           String couponCode,
                                           BigDecimal percentage,
                                           LocalDateTime endTime) {
        DiscountTypes discount = new CouponDiscount(
                name,
                couponCode,
                percentage,
                endTime
        );

        getDiscountPolicy().addDiscount(discount);
    }

    public BigDecimal calculateDiscountCompany(BigDecimal totalPrice, int ticketCount, String couponCode) {
        return getDiscountPolicy().calculateDiscount(totalPrice, ticketCount, couponCode);
    }

    public DiscountCalculationResult calculateDiscountCompanyDetails(BigDecimal totalPrice,
                                                                     int ticketCount,
                                                                     String couponCode) {
        return getDiscountPolicy().calculateDiscountDetails(totalPrice, ticketCount, couponCode);
    }
}