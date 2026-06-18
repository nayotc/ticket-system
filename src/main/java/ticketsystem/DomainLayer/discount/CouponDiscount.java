package ticketsystem.DomainLayer.discount;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CouponDiscount extends DiscountTypes{
    private String couponCode;
    private LocalDateTime endTime;
    

    public CouponDiscount(String name,
                          String couponCode,
                          BigDecimal percentage,LocalDateTime endTime
                          ) {
        super(name,percentage);
        validateCouponCode(couponCode);
        validateEndTime(endTime);
        this.couponCode = couponCode;
        this.endTime=endTime;
    }
    public String getCouponCode() {
        return couponCode;
    }

    public LocalDateTime getEndTime(){
        return endTime;
    }
    public void setEndTime(LocalDateTime endTime){
        this.endTime=endTime;
    }

    public void setCouponCode(String couponCode) {
        validateCouponCode(couponCode);
        this.couponCode = couponCode;
    }
    private void validateCouponCode(String couponCode) {
    if (couponCode == null || couponCode.isBlank()) {
        throw new IllegalArgumentException("Coupon code cannot be empty");
    }
}

    private void validateEndTime(LocalDateTime endTime) {
        if(endTime != null && LocalDateTime.now().isAfter(endTime)) {
            throw new IllegalArgumentException("End time cannot be in the past");
        }
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
                .multiply(getPercentage())
                .divide(BigDecimal.valueOf(100));
    }


    /**
     * Identifies this rule as a coupon-based discount.
     *
     * @return {@link DiscountKind#COUPON}
     */
    @Override
    public DiscountKind getKind() {
        return DiscountKind.COUPON;
    }
}
