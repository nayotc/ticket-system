package ticketsystem.DomainLayer.discount;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("MIN_TICKETS")
public class MinTicketsCondition extends DiscountCondition {

    @Column(name = "min_tickets")
    private int minTickets;

    protected MinTicketsCondition() {
    }

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