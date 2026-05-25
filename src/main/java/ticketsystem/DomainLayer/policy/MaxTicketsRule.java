package ticketsystem.DomainLayer.policy;

public class MaxTicketsRule implements PurchaseRule {
    private final int maxTickets;

    public MaxTicketsRule(int maxTickets) {
        this.maxTickets = maxTickets;
    }

    public PolicyResult isValid(int quantity, int age) {
        if (quantity > maxTickets) {
            return PolicyResult.denied("Cannot purchase more than " + maxTickets + " tickets.");
        }
        return PolicyResult.allowed();
    }
    
}
