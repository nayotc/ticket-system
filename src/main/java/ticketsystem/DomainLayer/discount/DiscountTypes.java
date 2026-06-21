package ticketsystem.DomainLayer.discount;

import java.math.BigDecimal;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

@Entity
@Table(name = "discount_types")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(
        name = "discount_type",
        discriminatorType = DiscriminatorType.STRING,
        length = 32
)
public abstract class DiscountTypes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    protected String name;

    @Column(
            name = "percentage",
            nullable = false,
            precision = 10,
            scale = 4
    )
    private BigDecimal percentage;

    protected DiscountTypes() {
    }

    public DiscountTypes(String name, BigDecimal percentage) {
        validateName(name);
        validatePercentage(percentage);

        this.name = name;
        this.percentage = percentage;
    }

    public Long getId() {
        return id;
    }

    // public Long getDiscountId(){
    //     return discountId;
    // }
    public String getName() {
        return name;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }
 
    public void setPercentage(BigDecimal percentage) {
        validatePercentage(percentage);
        this.percentage = percentage;
    }
    protected void validateName(String name) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Discount name cannot be empty");
        }
    }

    protected void validateDiscountId(Long discountId) {

        if (discountId == null ) {
            throw new IllegalArgumentException(
                    "Discount id must be positive");
        }
    }
    protected void validatePercentage(BigDecimal percentage) {

        if (percentage == null) {
            throw new IllegalArgumentException(
                    "Discount percentage cannot be null");
        }

        if (percentage.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Discount percentage cannot be negative");
        }

        if (percentage.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException(
                    "Discount percentage cannot be greater than 100");
        }
    }

    public abstract BigDecimal calculateDiscount(BigDecimal totalPrice,int ticketCount,String couponCode);

    /**
     * Returns the business type of this discount rule.
     *
     * @return the discount kind represented by this implementation
     */
    public abstract DiscountKind getKind();

    /**
     * Calculates this discount and returns a detailed domain result.
     *
     * This method keeps the existing discount calculation logic in one place by
     * using calculateDiscount(...), and wraps the calculated amount with the
     * discount metadata needed for later pricing display.
     *
     * If the discount amount is zero or negative, the discount is considered not
     * applied and an empty result is returned.
     *
     * @param totalPrice the price before this discount is applied
     * @param ticketCount the number of tickets in the order
     * @param couponCode the coupon code entered by the user, if any
     * @return detailed discount calculation result for this specific discount
     */
    public DiscountCalculationResult calculateDiscountDetails( BigDecimal totalPrice, int ticketCount, String couponCode) {
        BigDecimal amount = calculateDiscount(totalPrice, ticketCount, couponCode);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return DiscountCalculationResult.none();
        }

        return new DiscountCalculationResult(
                amount,
                List.of(new AppliedDiscountResult(
                        getName(),
                        getKind(),
                        getPercentage(),
                        amount
                ))
        );
    }
}
