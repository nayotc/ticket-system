package ticketsystem.DomainLayer.policy;

public class MinAgeRule implements PurchaseRule {

    private final int minAge;

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