package ticketsystem.DomainLayer.discount;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;

@Entity
@DiscriminatorValue("AND")
public class AndDiscountCondition extends DiscountCondition {

    @OneToMany(
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @JoinColumn(name = "parent_condition_id")
    @OrderColumn(name = "condition_order")
    private List<DiscountCondition> conditions =
            new ArrayList<>();

    protected AndDiscountCondition() {
    }

    public AndDiscountCondition(List<DiscountCondition> conditions) {

        if (conditions == null || conditions.isEmpty()) {
            throw new IllegalArgumentException(
                    "Conditions cannot be empty"
            );
        }

        if (conditions.stream()
                .anyMatch(condition -> condition == null)) {
            throw new IllegalArgumentException(
                    "Conditions cannot contain null"
            );
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
