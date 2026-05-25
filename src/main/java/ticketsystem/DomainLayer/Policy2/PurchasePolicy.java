package ticketsystem.DomainLayer.policy;

public class PurchasePolicy {
    private PurchaseRule rootRule;

    public PurchasePolicy(PurchaseRule rootRule)
    {
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