package ticketsystem.DomainLayer.policy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;

@Entity
public abstract class CompositePurchaseRule extends PurchaseRule {

    @OneToMany(
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    @JoinColumn(name = "parent_rule_id")
    @OrderColumn(name = "rule_order")
    protected List<PurchaseRule> rules = new ArrayList<>();

    protected CompositePurchaseRule() {
    }

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