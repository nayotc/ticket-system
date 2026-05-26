package ticketsystem.DomainLayer.discount;

import java.math.BigDecimal;

public abstract class DiscountTypes {
    protected String name;
    private Long discountId;
    private BigDecimal percentage;

    public DiscountTypes(Long discountId,String name,BigDecimal percentage) {
        validateDiscountId(discountId);
        validateName(name);
        validatePercentage(percentage);
        this.discountId=discountId;
        this.name = name;
        this.percentage = percentage;

    }

    public Long getDiscountId(){
        return discountId;
    }
    public String getName() {
        return name;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }
 
    public void setPercentage(BigDecimal percentage) {
        validatePercentage(percentage);
        this.percentage = percentage;
    }
    protected void validateName(String name) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Discount name cannot be empty");
        }
    }

    protected void validateDiscountId(Long discountId) {

        if (discountId == null ) {
            throw new IllegalArgumentException(
                    "Discount id must be positive");
        }
    }
    protected void validatePercentage(BigDecimal percentage) {

        if (percentage == null) {
            throw new IllegalArgumentException(
                    "Discount percentage cannot be null");
        }

        if (percentage.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Discount percentage cannot be negative");
        }

        if (percentage.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException(
                    "Discount percentage cannot be greater than 100");
        }
    }

    public abstract BigDecimal calculateDiscount(BigDecimal totalPrice,int ticketCount,String couponCode);
}
