package ticketsystem.DomainLayer.company;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class VisibleDiscount extends DiscountTypes {
    private double percentage;
    private String targetTicketType; 
    private String targetEventName;
    

    public VisibleDiscount(String name,
                           LocalDateTime startTime,
                           LocalDateTime endTime,
                           double percentage,
                           String targetTicketType
                          ) {
        super(name, startTime, endTime);
        this.percentage = percentage;
        this.targetTicketType = targetTicketType;
    }
public double getPercentage() {
        return percentage;
    }

    public String getTargetTicketType() {
        return targetTicketType;
    }

    public String getTargetEventName() {
        return targetEventName;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public void setTargetTicketType(String targetTicketType) {
        this.targetTicketType = targetTicketType;
    }

    public void setTargetEventName(String targetEventName) {
        this.targetEventName = targetEventName;
    }
    @Override
    public BigDecimal calculateDiscount(BigDecimal totalPrice, int ticketCount, String couponCode) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'calculateDiscount'");
    }
}
