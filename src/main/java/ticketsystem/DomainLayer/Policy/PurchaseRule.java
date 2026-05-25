package ticketsystem.DomainLayer.policy;

interface PurchaseRule {
    PolicyResult isValid(int quantity, int age);
}