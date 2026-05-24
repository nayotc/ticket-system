package ticketsystem.DomainLayer.company;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ConditionalDiscount extends VisibleDiscount{
        private Condition condition;
        private Integer ticketThreshold;
        private LocalDateTime dateThreshold;

        public ConditionalDiscount(String name,
                               LocalDateTime startTime,
                               LocalDateTime endTime,
                               double percentage,
                               String targetTicketType,
                               Condition condition, Integer ticketThreshold,
                               LocalDateTime dateThreshold) {
        
        super(name, startTime, endTime, percentage, targetTicketType);
        
        this.condition = condition;
        this.ticketThreshold = ticketThreshold;
        this.dateThreshold = dateThreshold;
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

    public LocalDateTime getDateThreshold() {
        return dateThreshold;
    }

    public void setDateThreshold(LocalDateTime dateThreshold) {
        this.dateThreshold = dateThreshold;
    }
    
    public boolean isConditionSatisfied(int ticketCount) {
        switch (condition) {
            case MIN_TICKET:
                return ticketThreshold != null && ticketCount >= ticketThreshold;

            case MAX_TICKET:
                return ticketThreshold != null && ticketCount <= ticketThreshold;

            case DATE:
                return dateThreshold != null && LocalDateTime.now().isBefore(dateThreshold);

            default:
                return false;
        }
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
