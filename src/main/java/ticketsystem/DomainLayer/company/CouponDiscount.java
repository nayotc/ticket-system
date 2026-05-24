package ticketsystem.DomainLayer.company;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CouponDiscount extends DiscountTypes{
    private String couponCode;
    private BigDecimal percentage;
    

    public CouponDiscount(String name,Long id,
                          String couponCode,
                          BigDecimal percentage
                          ) {
        super(id,name);

        this.couponCode = couponCode;
        this.percentage = percentage;
    }
    public String getCouponCode() {
        return couponCode;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }


    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }

    public void setPercentage(BigDecimal percentage) {
        this.percentage = percentage;
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal totalPrice, int ticketCount, String couponCode) {
        if (couponCode == null || !couponCode.equals(this.couponCode)) {
            return BigDecimal.ZERO;
        }

        return totalPrice
                .multiply(percentage)
                .divide(BigDecimal.valueOf(100));
    }
}
