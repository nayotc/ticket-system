package ticketsystem.DomainLayer.policy;

public class MinTicketsRule implements PurchaseRule {

    private final int minTickets;

    public MinTicketsRule(int minTickets) {
        if (minTickets < 0) {
            throw new IllegalArgumentException("Minimum tickets cannot be negative");
        }
        this.minTickets = minTickets;
    }

    @Override
    public PolicyResult isValid(int quantity, int age) {
        if (quantity >= minTickets) {
            return PolicyResult.allowed();
        } else {
            return PolicyResult.denied("Insufficient tickets purchased, minimum required: " + minTickets);
        }
    }

    public int getMinTickets() {
        return minTickets;
    }
    
}
