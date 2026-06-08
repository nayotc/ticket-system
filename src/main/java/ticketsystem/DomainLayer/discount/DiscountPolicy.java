package ticketsystem.DomainLayer.discount;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Comparator;

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

    /**
     * Calculates this discount policy and returns a detailed domain result.
     *
     * This method does not replace the existing calculateDiscount(...) method.
     * It adds a richer calculation result that includes both the total discount
     * amount and the discounts that were actually applied.
     *
     * For SUM composition, discounts are applied one after another on the current
     * remaining price. Every discount that deducts a positive amount is included
     * in the applied discounts list.
     *
     * For MAX composition, all discounts are evaluated against the original price,
     * but only the discount with the highest amount is considered applied.
     *
     * @param totalPrice the original price before applying this discount policy
     * @param ticketCount the number of tickets in the order
     * @param couponCode the coupon code entered by the user, if any
     * @return detailed discount calculation result after applying the policy
     */
    public DiscountCalculationResult calculateDiscountDetails(
            BigDecimal totalPrice,
            int ticketCount,
            String couponCode
    ) {
        if (discounts.isEmpty()) {
            return DiscountCalculationResult.none();
        }

        if (compositionType == DiscountCompositionType.SUM) {
            BigDecimal currentPrice = totalPrice;
            List<AppliedDiscountResult> appliedDiscounts = new ArrayList<>();

            for (DiscountTypes discount : discounts) {
                DiscountCalculationResult result = discount.calculateDiscountDetails(
                        currentPrice,
                        ticketCount,
                        couponCode
                );

                if (result.discountTotal().compareTo(BigDecimal.ZERO) > 0) {
                    appliedDiscounts.addAll(result.appliedDiscounts());
                    currentPrice = currentPrice.subtract(result.discountTotal());

                    if (currentPrice.compareTo(BigDecimal.ZERO) < 0) {
                        currentPrice = BigDecimal.ZERO;
                        break;
                    }
                }
            }

            BigDecimal discountTotal = totalPrice.subtract(currentPrice);
            return new DiscountCalculationResult(discountTotal, appliedDiscounts);
        }

        return discounts.stream()
                .map(discount -> discount.calculateDiscountDetails(totalPrice, ticketCount, couponCode))
                .filter(result -> result.discountTotal().compareTo(BigDecimal.ZERO) > 0)
                .max(Comparator.comparing(DiscountCalculationResult::discountTotal))
                .orElseGet(DiscountCalculationResult::none);
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