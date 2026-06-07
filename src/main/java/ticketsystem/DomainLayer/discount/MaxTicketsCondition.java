package ticketsystem.DomainLayer.discount;

public class MaxTicketsCondition implements DiscountCondition {

    private final int maxTickets;

    public MaxTicketsCondition(int maxTickets) {
        if (maxTickets <= 0) {
            throw new IllegalArgumentException(
                    "Maximum tickets must be positive");
        }

        this.maxTickets = maxTickets;
    }
    public int getMaxTickets() {
        return maxTickets;
    }

    @Override
    public boolean isSatisfied(DiscountConditionContext context) {
        return context.getTicketCount() <= maxTickets;
    }
}