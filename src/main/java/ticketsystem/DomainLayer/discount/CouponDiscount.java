package ticketsystem.DomainLayer.discount;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CouponDiscount extends DiscountTypes{
    private String couponCode;
    private BigDecimal percentage;
    private LocalDateTime endTime;
    

    public CouponDiscount(String name,Long id,
                          String couponCode,
                          BigDecimal percentage,LocalDateTime endTime
                          ) {
        super(id,name);

        this.couponCode = couponCode;
        this.percentage = percentage;
        this.endTime=endTime;
    }
    public String getCouponCode() {
        return couponCode;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }
    public LocalDateTime getEndTime(){
        return endTime;
    }
    public void setEndTime(LocalDateTime endTime){
        this.endTime=endTime;
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
        if (endTime != null && LocalDateTime.now().isAfter(endTime)) {
         return BigDecimal.ZERO;
        }

        return totalPrice
                .multiply(percentage)
                .divide(BigDecimal.valueOf(100));
    }
}
