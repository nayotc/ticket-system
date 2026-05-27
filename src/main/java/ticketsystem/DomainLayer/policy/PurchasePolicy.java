package ticketsystem.DomainLayer.policy;

public class PurchasePolicy {
    private PurchaseRule rootRule;

    public PurchasePolicy(PurchaseRule rootRule)
    {
        if(rootRule == null) {
            throw new IllegalArgumentException("Root rule cannot be null");
        }
        this.rootRule = rootRule;
    }

    public PolicyResult validate(int quantity, int age) {
        return rootRule.isValid(quantity, age);
    }

    public PurchaseRule getRootRule() {
        return rootRule;
    }

    public static PurchasePolicy noRestrictions() {
        return new PurchasePolicy(new AlwaysAllowRule());
    }
}