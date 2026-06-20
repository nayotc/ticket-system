package ticketsystem.DomainLayer.policy;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("ALWAYS_ALLOW")
public class AlwaysAllowRule extends PurchaseRule {

    public AlwaysAllowRule() {
    }

    @Override
    public PolicyResult isValid(int quantity, int age) {
        return PolicyResult.allowed();
    }
}