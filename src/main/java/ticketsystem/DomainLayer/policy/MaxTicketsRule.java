package ticketsystem.DomainLayer.policy;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("MAX_TICKETS")
public class MaxTicketsRule extends PurchaseRule {

    @Column(name = "max_tickets")
    private int maxTickets;

    protected MaxTicketsRule() {
    }

    public MaxTicketsRule(int maxTickets) {
        if(maxTickets <= 0) {
            throw new IllegalArgumentException("Max tickets must be greater than zero");
        }
        this.maxTickets = maxTickets;
    }

    @Override
    public PolicyResult isValid(int quantity, int age) {
        if (quantity > maxTickets) {
            return PolicyResult.denied("Cannot purchase more than " + maxTickets + " tickets.");
        }
        return PolicyResult.allowed();
    }

    public int getMaxTickets() {
        return maxTickets;
    }
    
}
