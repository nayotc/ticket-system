package ticketsystem.DomainLayer.discount;

import java.math.BigDecimal;

public class ConditionalDiscount extends VisibleDiscount {

    private DiscountCondition condition;

    public ConditionalDiscount(String name,
                               
                               BigDecimal percentage,
                               DiscountCondition condition) {

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