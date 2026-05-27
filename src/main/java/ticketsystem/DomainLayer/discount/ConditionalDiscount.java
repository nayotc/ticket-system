package ticketsystem.DomainLayer.discount;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ConditionalDiscount extends VisibleDiscount{
        private Condition condition;
        private Integer ticketThreshold;
        private LocalDateTime startTime;
        private LocalDateTime endTime;

        public ConditionalDiscount(String name,Long id,
                               LocalDateTime startTime,
                               LocalDateTime endTime,
                               BigDecimal percentage,
                               Condition condition, Integer ticketThreshold) {
        
        super(name,id,percentage);
        validateCondition(condition, ticketThreshold, startTime, endTime);

        this.condition = condition;
        this.ticketThreshold = ticketThreshold;
        this.startTime=startTime;
        this.endTime=endTime;
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

  private void validateCondition(Condition condition, Integer ticketThreshold,
            LocalDateTime startTime, LocalDateTime endTime) {

        if (condition == null) {
            throw new IllegalArgumentException("Condition cannot be null");
        }

        switch (condition) {
            case MIN_TICKET:
            case MAX_TICKET:
                if (ticketThreshold == null || ticketThreshold <= 0) {
                    throw new IllegalArgumentException("Ticket threshold must be positive");
                }
                break;

            case DATE:
                if (startTime == null || endTime == null) {
                    throw new IllegalArgumentException(
                            "Start time and end time are required for date condition");
                }

                if (endTime.isBefore(startTime)) {
                    throw new IllegalArgumentException(
                            "End time cannot be before start time");
                }
                break;

            default:
                throw new IllegalArgumentException("Unsupported condition type");
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
