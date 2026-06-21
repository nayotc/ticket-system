package ticketsystem.DomainLayer.discount;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("MAX_TICKETS")
public class MaxTicketsCondition extends DiscountCondition {

    @Column(name = "max_tickets")
    private int maxTickets;

    protected MaxTicketsCondition() {
    }

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