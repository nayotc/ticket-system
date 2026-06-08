package ticketsystem.DomainLayer.discount;

import java.util.ArrayList;
import java.util.List;

public class AndDiscountCondition implements DiscountCondition {

    private final List<DiscountCondition> conditions;

    public AndDiscountCondition(List<DiscountCondition> conditions) {

        if (conditions == null || conditions.isEmpty()) {
            throw new IllegalArgumentException(
                    "Conditions cannot be empty");
        }

        this.conditions = new ArrayList<>(conditions);
    }
    public List<DiscountCondition> getConditions() {
        return new ArrayList<>(conditions);
    }

    @Override
    public boolean isSatisfied(DiscountConditionContext context) {

        for (DiscountCondition condition : conditions) {

            if (!condition.isSatisfied(context)) {
                return false;
            }
        }

        return true;
    }
}
