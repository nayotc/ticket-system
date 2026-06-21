package ticketsystem.DomainLayer.discount;

import java.math.BigDecimal;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("VISIBLE")
public class VisibleDiscount extends DiscountTypes {

    protected VisibleDiscount() {
    }

    public VisibleDiscount(String name,
                           BigDecimal percentage
                          ) {
        super(name,percentage);
    }
 
    @Override
    public BigDecimal calculateDiscount(BigDecimal totalPrice, int ticketCount, String couponCode) {
         return totalPrice
            .multiply(getPercentage())
            .divide(BigDecimal.valueOf(100));
    }

    /**
     * Identifies this rule as a visible discount.
     *
     * @return {@link DiscountKind#VISIBLE}
     */
    @Override
    public DiscountKind getKind() {
        return DiscountKind.VISIBLE;
    }
}

