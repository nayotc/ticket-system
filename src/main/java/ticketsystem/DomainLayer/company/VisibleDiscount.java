package ticketsystem.DomainLayer.company;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class VisibleDiscount extends DiscountTypes {
    private BigDecimal percentage;
    private String targetTicketType; 
    

    public VisibleDiscount(String name,Long id,
                           BigDecimal percentage
                          ) {
        super(id,name);
        this.percentage = percentage;
    }
public BigDecimal getPercentage() {
        return percentage;
    }

    public String getTargetTicketType() {
        return targetTicketType;
    }

    public void setPercentage(BigDecimal percentage) {
        this.percentage = percentage;
    }

    public void setTargetTicketType(String targetTicketType) {
        this.targetTicketType = targetTicketType;
    }

 
    @Override
    public BigDecimal calculateDiscount(BigDecimal totalPrice, int ticketCount, String couponCode) {
         return totalPrice
            .multiply(percentage)
            .divide(BigDecimal.valueOf(100));
    }
}
