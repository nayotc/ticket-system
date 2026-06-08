package ticketsystem.DomainLayer.discount;

public class MinTicketsCondition implements DiscountCondition {

    private final int minTickets;

    public MinTicketsCondition(int minTickets) {
        if (minTickets <= 0) {
            throw new IllegalArgumentException(
                    "Minimum tickets must be positive");
        }

        this.minTickets = minTickets;
    }

    public int getMinTickets() {
        return minTickets;
    }
    
    @Override
    public boolean isSatisfied(DiscountConditionContext context) {
        return context.getTicketCount() >= minTickets;
    }
}