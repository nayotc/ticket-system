package ticketsystem.DomainLayer.discount;

import java.time.LocalDateTime;

public class DiscountConditionContext {

    private final int ticketCount;
    private final LocalDateTime currentTime;

    public DiscountConditionContext(int ticketCount) {
        this.ticketCount = ticketCount;
        this.currentTime = LocalDateTime.now();
    }

    public int getTicketCount() {
        return ticketCount;
    }

    public LocalDateTime getCurrentTime() {
        return currentTime;
    }
}