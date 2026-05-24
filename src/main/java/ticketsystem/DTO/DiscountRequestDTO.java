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

//for test
public void setName(String name) {
    this.name = name;
}

public void setDiscountType(DiscountKind discountType) {
    this.discountType = discountType;
}

public void setStartTime(LocalDateTime startTime) {
    this.startTime = startTime;
}

public void setEndTime(LocalDateTime endTime) {
    this.endTime = endTime;
}

public void setPercentage(Double percentage) {
    this.percentage = percentage;
}

public void setTargetTicketType(String targetTicketType) {
    this.targetTicketType = targetTicketType;
}

public void setTargetEventName(String targetEventName) {
    this.targetEventName = targetEventName;
}

public void setCondition(String condition) {
    this.condition = condition;
}

public void setCouponCode(String couponCode) {
    this.couponCode = couponCode;
}

public void setFixedAmount(Double fixedAmount) {
    this.fixedAmount = fixedAmount;
}
}