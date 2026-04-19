package DomainLayer.event;

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
                          Double fixedAmount,
                          String targetEventName) {
        super(name, startTime, endTime);

        this.couponCode = couponCode;
        this.percentage = percentage;
        this.fixedAmount = fixedAmount;
        this.targetEventName = targetEventName;
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
}