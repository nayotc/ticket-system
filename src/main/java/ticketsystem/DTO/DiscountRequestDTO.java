package ticketsystem.DTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import ticketsystem.DomainLayer.company.ConditionalDiscount.Condition;
import ticketsystem.DomainLayer.company.DiscountPolicy.DiscountKind;

public class DiscountRequestDTO {

    private String name;
    private DiscountKind discountType;
    private BigDecimal percentage;    
    // for condition
    private Condition condition;
    private Integer ticketThreshold;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // for coupun
    private String couponCode;

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

public BigDecimal getPercentage() {
    return percentage;
}

public Condition getCondition() {
    return condition;
}

public String getCouponCode() {
    return couponCode;
}

public Integer getTicketThreshold() {
    return ticketThreshold;
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

public void setPercentage(BigDecimal percentage) {
    this.percentage = percentage;
}

public void setCondition(Condition condition) {
    this.condition = condition;
}

public void setCouponCode(String couponCode) {
    this.couponCode = couponCode;
}

public void setTicketThreshold(Integer ticketThreshold) {
    this.ticketThreshold = ticketThreshold;
}

}