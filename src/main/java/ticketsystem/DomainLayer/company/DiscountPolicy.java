package ticketsystem.DomainLayer.company;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DiscountPolicy extends DiscountTypes {

    private List<DiscountTypes> discounts = new ArrayList<>();
    private DiscountCompositionType compositionType;

    public DiscountPolicy(String name,
                          LocalDateTime startTime,
                          LocalDateTime endTime,
                          DiscountCompositionType compositionType) {
        super(name, startTime, endTime);
        this.compositionType = compositionType;
    }

    public void addDiscount(DiscountTypes discount) {
        if (discount == null) {
            throw new IllegalArgumentException("Discount cannot be null");
        }
        discounts.add(discount);
    }

    @Override
    public BigDecimal calculateDiscount(
            BigDecimal totalPrice,
            int ticketCount,
            String eventName,
            String couponCode
    ) {
        if (!isActiveNow()) {
            return BigDecimal.ZERO;
        }

        if (discounts.isEmpty()) {
            return BigDecimal.ZERO;
        }

        if (compositionType == DiscountCompositionType.SUM) {
            return discounts.stream()
                    .map(d -> d.calculateDiscount(totalPrice, ticketCount, eventName, couponCode))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        return discounts.stream()
                .map(d -> d.calculateDiscount(totalPrice, ticketCount, eventName, couponCode))
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

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