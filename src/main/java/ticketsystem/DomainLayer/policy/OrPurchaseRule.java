package ticketsystem.DomainLayer.policy;

import java.util.ArrayList;
import java.util.List;

public class OrPurchaseRule extends CompositePurchaseRule {

    public OrPurchaseRule(List<PurchaseRule> rules) {
        super(rules);
    }

    @Override
    public PolicyResult isValid(int quantity, int age) {
        List<String> failureMessages = new ArrayList<>();

        for (PurchaseRule rule : rules) {
            PolicyResult result = rule.isValid(quantity, age);

            if (result.isAllowed()) {
                return PolicyResult.allowed();
            }

            if (result.getMessage() != null) {
                failureMessages.add(result.getMessage());
            }
        }

        return PolicyResult.denied(
                "All rules failed: " + String.join("; ", failureMessages)
        );
    }
}
