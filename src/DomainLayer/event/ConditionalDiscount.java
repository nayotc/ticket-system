package DomainLayer.event;

import java.time.LocalDateTime;

public class ConditionalDiscount extends VisibleDiscount{
        private String condition;
        public ConditionalDiscount(String name,
                               LocalDateTime startTime,
                               LocalDateTime endTime,
                               double percentage,
                               String targetTicketType,
                               String targetEventName,
                               String condition) {
        
        super(name, startTime, endTime, percentage, targetTicketType, targetEventName);
        
        this.condition = condition;
    }
    public String getCondition() {
        return condition;
    }
    public void setCondition(String condition) {
        this.condition = condition;
    }
}