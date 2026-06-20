package ticketsystem.DTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DiscountDTO {

    private String name;
    private BigDecimal percentage;
    private String type; // Expected values: "VISIBLE", "COUPON", "CONDITIONAL"
  
    
    // Fields for specific discount types
    private String couponCode;
    private LocalDateTime endTime;
    private List<DiscountConditionDTO> conditions = new ArrayList<>();

    private String conditionText;


    public DiscountDTO() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public void setPercentage(BigDecimal percentage) {
        this.percentage = percentage;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public List<DiscountConditionDTO> getConditions() {
        return conditions;
    }
    public void setConditions(List<DiscountConditionDTO> conditions) {
        this.conditions = conditions;
    }
  
      public String getConditionText() {
        return conditionText;
    }
        public void setConditionText(String conditionText) {
        this.conditionText = conditionText;
    }
}