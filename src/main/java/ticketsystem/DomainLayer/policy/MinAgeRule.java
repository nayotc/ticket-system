package ticketsystem.DomainLayer.policy;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("MIN_AGE")
public class MinAgeRule extends PurchaseRule {

    @Column(name = "min_age")
    private int minAge;

    protected MinAgeRule() {
    }

    public MinAgeRule(int minAge) {
        if (minAge < 0) {
            throw new IllegalArgumentException("Minimum age cannot be negative");
        }
        this.minAge = minAge;
    }

    @Override
    public PolicyResult isValid(int quantity, int age) {
        if (age < minAge) {
            return PolicyResult.denied("Customer does not meet the minimum age requirement of " + minAge);
        }
        return PolicyResult.allowed();
    }

    public int getMinAge() {
        return minAge;
    }

}