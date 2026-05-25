package ticketsystem.DomainLayer.policy;

public class AlwaysAllowRule implements PurchaseRule {

    @Override
    public PolicyResult isValid( int quantity, int age) {
        return PolicyResult.allowed();
    }
}
