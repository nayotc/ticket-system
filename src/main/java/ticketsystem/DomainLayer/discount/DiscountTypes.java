package ticketsystem.DomainLayer.discount;

import java.math.BigDecimal;

public abstract class DiscountTypes {
    protected String name;
    private Long discountId;
    public DiscountTypes(Long discountId,String name) {
        this.discountId=discountId;
        this.name = name;

    }

    public Long getDiscountId(){
        return discountId;
    }
    public String getName() {
        return name;
    }

    public abstract BigDecimal calculateDiscount(BigDecimal totalPrice,int ticketCount,String couponCode);
}
