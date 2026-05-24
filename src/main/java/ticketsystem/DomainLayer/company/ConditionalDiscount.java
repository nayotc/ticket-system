package ticketsystem.DomainLayer.company;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ConditionalDiscount extends VisibleDiscount{
        private Condition condition;
        private Integer ticketThreshold;

        public ConditionalDiscount(String name,
                               LocalDateTime startTime,
                               LocalDateTime endTime,
                               double percentage,
                               String targetTicketType,
                               Condition condition, Integer ticketThreshold) {
        
        super(name,percentage, targetTicketType);
        
        this.condition = condition;
        this.ticketThreshold = ticketThreshold;
    }
    public Condition getCondition() {
        return condition;
    }

    public void setCondition(Condition condition) {
        this.condition = condition;
    }

   public Integer getTicketThreshold() {
        return ticketThreshold;
    }

    public void setTicketThreshold(Integer ticketThreshold) {
        this.ticketThreshold = ticketThreshold;
    }
    
    public boolean isConditionSatisfied(int ticketCount) {
        switch (condition) {
            case MIN_TICKET:
                return ticketThreshold != null && ticketCount >= ticketThreshold;

            case MAX_TICKET:
                return ticketThreshold != null && ticketCount <= ticketThreshold;

            case DATE:
                return isActiveNow();
            default:
                return false;
        }
    }

    protected boolean isActiveNow() {
        LocalDateTime now = LocalDateTime.now();

        if (startTime != null && now.isBefore(startTime)) {
            return false;
        }

        if (endTime != null && now.isAfter(endTime)) {
            return false;
        }

        return true;
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal totalPrice, int ticketCount, String couponCode) {
        if (!isConditionSatisfied(ticketCount)) {
            return BigDecimal.ZERO;
        }

        return super.calculateDiscount(totalPrice, ticketCount, couponCode);
    }


    public enum Condition {
    MIN_TICKET,
    MAX_TICKET,
    DATE
    }
}
