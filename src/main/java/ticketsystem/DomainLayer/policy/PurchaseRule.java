package ticketsystem.DomainLayer.policy;

public interface PurchaseRule {
    PolicyResult isValid(int quantity, int age);
}