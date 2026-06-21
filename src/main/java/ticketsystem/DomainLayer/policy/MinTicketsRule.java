package ticketsystem.DomainLayer.policy;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("MIN_TICKETS")
public class MinTicketsRule extends PurchaseRule {

    @Column(name = "min_tickets")
    private int minTickets;

    protected MinTicketsRule() {
    }

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
        }

        return PolicyResult.denied(
                "Insufficient tickets purchased, minimum required: "
                        + minTickets
        );
    }

    public int getMinTickets() {
        return minTickets;
    }
    
}
