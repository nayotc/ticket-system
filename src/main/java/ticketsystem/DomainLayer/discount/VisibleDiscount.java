package ticketsystem.DomainLayer.discount;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class VisibleDiscount extends DiscountTypes {
    

    public VisibleDiscount(String name,Long id,
                           BigDecimal percentage
                          ) {
        super(id,name,percentage);
    }
 
    @Override
    public BigDecimal calculateDiscount(BigDecimal totalPrice, int ticketCount, String couponCode) {
         return totalPrice
            .multiply(getPercentage())
            .divide(BigDecimal.valueOf(100));
    }
}
