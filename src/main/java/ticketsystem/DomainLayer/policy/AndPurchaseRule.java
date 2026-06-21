package ticketsystem.DomainLayer.policy;

import java.util.List;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("AND")
public class AndPurchaseRule extends CompositePurchaseRule {

    protected AndPurchaseRule() {
    }

    public AndPurchaseRule(List<PurchaseRule> rules) {
        super(rules);
    }

    @Override
    public PolicyResult isValid(int quantity, int age) {
        for (PurchaseRule rule : rules) {
            PolicyResult result = rule.isValid(quantity, age);

            if (!result.isAllowed()) {
                return result;
            }
        }

        return PolicyResult.allowed();
    }
}