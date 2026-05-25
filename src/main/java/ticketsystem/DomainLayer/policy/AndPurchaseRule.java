package ticketsystem.DomainLayer.policy;

import java.util.List;

public class AndPurchaseRule extends CompositePurchaseRule {

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
