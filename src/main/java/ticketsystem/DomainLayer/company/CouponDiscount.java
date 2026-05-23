package ticketsystem.DomainLayer.company;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CouponDiscount extends DiscountTypes{
    private String couponCode;
    private Double percentage;
    private Double fixedAmount;
    private String targetEventName;

    public CouponDiscount(String name,
                          LocalDateTime startTime,
                          LocalDateTime endTime,
                          String couponCode,
                          Double percentage,
                          Double fixedAmount
                          ) {
        super(name, startTime, endTime);

        this.couponCode = couponCode;
        this.percentage = percentage;
        this.fixedAmount = fixedAmount;
    }
    public String getCouponCode() {
        return couponCode;
    }

    public Double getPercentage() {
        return percentage;
    }

    public Double getFixedAmount() {
        return fixedAmount;
    }

    public String getTargetEventName() {
        return targetEventName;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }

    public void setPercentage(Double percentage) {
        this.percentage = percentage;
    }

    public void setFixedAmount(Double fixedAmount) {
        this.fixedAmount = fixedAmount;
    }

    public void setTargetEventName(String targetEventName) {
        this.targetEventName = targetEventName;
    }
    @Override
    public BigDecimal calculateDiscount(BigDecimal totalPrice, int ticketCount, String eventName, String couponCode) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'calculateDiscount'");
    }
}
