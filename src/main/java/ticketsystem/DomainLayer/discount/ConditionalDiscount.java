package ticketsystem.DomainLayer.discount;

import java.math.BigDecimal;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

@Entity
@DiscriminatorValue("CONDITIONAL")
public class ConditionalDiscount extends VisibleDiscount {

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "condition_id", unique = true)
    private DiscountCondition condition;

    protected ConditionalDiscount() {
    }

    public ConditionalDiscount(
            String name,
            BigDecimal percentage,
            DiscountCondition condition
    ) {
        super(name, percentage);

        if (condition == null) {
            throw new IllegalArgumentException(
                    "Condition cannot be null");
        }

        this.condition = condition;
    }

    public DiscountCondition getCondition() {
        return condition;
    }

    public void setCondition(DiscountCondition condition) {
        if (condition == null) {
            throw new IllegalArgumentException(
                    "Condition cannot be null");
        }

        this.condition = condition;
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal totalPrice,
                                        int ticketCount,
                                        String couponCode) {

        DiscountConditionContext context =
                new DiscountConditionContext(ticketCount);

        if (!condition.isSatisfied(context)) {
            return BigDecimal.ZERO;
        }

        return super.calculateDiscount(
                totalPrice,
                ticketCount,
                couponCode);
    }

    /**
     * Identifies this rule as a conditional discount.
     *
     * <p>This override is required because ConditionalDiscount inherits from
     * VisibleDiscount, but represents a different business discount type.</p>
     *
     * @return {@link DiscountKind#CONDITIONAL}
     */
    @Override
    public DiscountKind getKind() {
        return DiscountKind.CONDITIONAL;
    }

    
}