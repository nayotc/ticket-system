package ticketsystem.DomainLayer.discount;

public interface DiscountCondition {
    boolean isSatisfied(DiscountConditionContext context);
}