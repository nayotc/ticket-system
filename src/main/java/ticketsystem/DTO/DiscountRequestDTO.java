package ticketsystem.DTO;

import java.time.LocalDateTime;

import ticketsystem.DomainLayer.company.DiscountPolicy.DiscountKind;

public class DiscountRequestDTO {

    private String name;
    private DiscountKind discountType;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private Double percentage;

    private String targetTicketType;
    private String targetEventName;

    // for condition
    private String condition;

    // for coupun
    private String couponCode;
    private Double fixedAmount;

    public DiscountKind  getDiscountKind(){
        return discountType;
    }
    public String getName(){
        return name;
    }
    public LocalDateTime getStartTime() {
    return startTime;
}

public LocalDateTime getEndTime() {
    return endTime;
}

public Double getPercentage() {
    return percentage;
}

public String getTargetTicketType() {
    return targetTicketType;
}

public String getTargetEventName() {
    return targetEventName;
}

public String getCondition() {
    return condition;
}

public String getCouponCode() {
    return couponCode;
}

public Double getFixedAmount() {
    return fixedAmount;
}
}