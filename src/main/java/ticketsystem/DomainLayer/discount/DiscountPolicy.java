package ticketsystem.DomainLayer.discount;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import ticketsystem.DomainLayer.discount.DiscountCompositionType;
import ticketsystem.DomainLayer.discount.DiscountTypes;

public class DiscountPolicy {

    private List<DiscountTypes> discounts = new ArrayList<>();
    private DiscountCompositionType compositionType;

    public DiscountPolicy(DiscountCompositionType compositionType) {
        if (compositionType == null) {
             throw new IllegalArgumentException("Discount composition type cannot be null");
        }
             this.compositionType = compositionType;
    }

    public void addDiscount(DiscountTypes discount) {
        if (discount == null) {
            throw new IllegalArgumentException("Discount cannot be null");
        }
        discounts.add(discount);
    }

    public void setDiscountCompositionType(DiscountCompositionType compositionType) {
        if (compositionType == null) {
            throw new IllegalArgumentException("Discount composition type cannot be null");
        }
        this.compositionType = compositionType;
    }

    public DiscountCompositionType getDiscountCompositionType() {
        return compositionType;
    }

    public void removeDiscount(Long discountId) {
        boolean removed = discounts.removeIf(discount -> discount.getDiscountId().equals(discountId));

        if (!removed) {
            throw new IllegalArgumentException("Discount not found");
        }
    }

    /**
     * Calculates the total discount amount that should be deducted
     * from the original price according to the discount composition policy.
     *
     * SUM -> all applicable discounts are accumulated together.
     * MAX -> only the highest applicable discount is applied.
     *
     * The method returns the discount amount itself,
     * not the final price after discount.
     */
    public BigDecimal calculateDiscount(BigDecimal totalPrice, int ticketCount, String couponCode) {

        if (discounts.isEmpty()) {
            return BigDecimal.ZERO;
        }

        if (compositionType == DiscountCompositionType.SUM) {

            BigDecimal currentPrice = totalPrice;

            for (DiscountTypes discount : discounts) {

                BigDecimal discountAmount = discount.calculateDiscount(
                        currentPrice,
                        ticketCount,
                        couponCode);

                currentPrice = currentPrice.subtract(discountAmount);

                if (currentPrice.compareTo(BigDecimal.ZERO) < 0) {
                    currentPrice = BigDecimal.ZERO;
                    break;
                }
            }

            return totalPrice.subtract(currentPrice);
        }

        return discounts.stream()
                .map(d -> d.calculateDiscount(totalPrice, ticketCount, couponCode))
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    public List<DiscountTypes> getDiscounts() {
        return discounts;
    }

}