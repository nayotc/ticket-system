package ticketsystem.DomainLayer.company;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class VisibleDiscount extends DiscountTypes {
    private double percentage;
    private String targetTicketType; 
    

    public VisibleDiscount(String name,Long id,
                           double percentage,
                           String targetTicketType
                          ) {
        super(id,name);
        this.percentage = percentage;
        this.targetTicketType = targetTicketType;
    }
public double getPercentage() {
        return percentage;
    }

    public String getTargetTicketType() {
        return targetTicketType;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public void setTargetTicketType(String targetTicketType) {
        this.targetTicketType = targetTicketType;
    }

 
    @Override
    public BigDecimal calculateDiscount(BigDecimal totalPrice, int ticketCount, String couponCode) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'calculateDiscount'");
    }
}
