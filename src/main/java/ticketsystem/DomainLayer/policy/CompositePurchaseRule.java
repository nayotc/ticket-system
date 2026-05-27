package ticketsystem.DomainLayer.policy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class CompositePurchaseRule implements PurchaseRule {

    protected final List<PurchaseRule> rules;

    protected CompositePurchaseRule(List<PurchaseRule> rules) {
        if (rules == null || rules.isEmpty()) {
            throw new IllegalArgumentException("Composite rule must contain at least one rule");
        }

        if (rules.stream().anyMatch(rule -> rule == null)) {
            throw new IllegalArgumentException("Composite rule cannot contain null rules");
        }

        this.rules = new ArrayList<>(rules);
    }

    public List<PurchaseRule> getRules() {
        return Collections.unmodifiableList(rules);
    }
}