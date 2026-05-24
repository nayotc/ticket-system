package ticketsystem.DomainLayer.company;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DiscountPolicy {

    private List<DiscountTypes> discounts = new ArrayList<>();
    private DiscountCompositionType compositionType;

    public DiscountPolicy(DiscountCompositionType compositionType) {
        this.compositionType = compositionType;
    }

    public void addDiscount(DiscountTypes discount) {
        if (discount == null) {
            throw new IllegalArgumentException("Discount cannot be null");
        }
        discounts.add(discount);
    }

    public BigDecimal calculateDiscount(BigDecimal totalPrice, int ticketCount, String couponCode) {
        if(compositionType==DiscountCompositionType.SUM){
                
            return discounts.stream()
                    .map(d -> d.calculateDiscount(totalPrice, ticketCount, couponCode))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        }
        
        // if (discounts.isEmpty()) {
        //     return BigDecimal.ZERO;
        // }

        // if (compositionType == DiscountCompositionType.SUM) {
        //     return discounts.stream()
        //             .map(d -> d.calculateDiscount(totalPrice, ticketCount, eventName, couponCode))
        //             .reduce(BigDecimal.ZERO, BigDecimal::add);
        // }

        // return discounts.stream()
        //         .map(d -> d.calculateDiscount(totalPrice, ticketCount, eventName, couponCode))
        //         .max(BigDecimal::compareTo)
        //         .orElse(BigDecimal.ZERO);
    //}

    public enum DiscountCompositionType {
        SUM,
        MAX
    }

    public enum DiscountKind {
        VISIBLE,
        CONDITIONAL,
        COUPON
    }

}