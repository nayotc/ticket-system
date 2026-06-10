package ticketsystem.DomainLayer.discount;

import java.math.BigDecimal;
import java.util.List;

/**
 * Represents the result of applying a discount policy.
 *
 * The discount policy may include several discount rules and a composition
 * strategy such as MAX or SUM. This result object keeps both the total discount
 * amount and the specific discounts that were actually applied after the
 * composition strategy was resolved.
 *
 * This object is intentionally part of the domain layer because deciding
 * which discounts apply is business logic. The UI should only receive a mapped
 * presentation DTO later.
 *
 * @param discountTotal the total monetary amount deducted by the policy
 * @param appliedDiscounts the discounts that were actually applied
 */
public record DiscountCalculationResult(
        BigDecimal discountTotal,
        List<AppliedDiscountResult> appliedDiscounts
) {
    public DiscountCalculationResult {
        if (discountTotal == null) {
            throw new IllegalArgumentException("Discount total cannot be null");
        }
        if (discountTotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Discount total cannot be negative");
        }
        if (appliedDiscounts == null) {
            appliedDiscounts = List.of();
        }
        appliedDiscounts = List.copyOf(appliedDiscounts);
    }

    /**
     * Creates an empty discount calculation result for cases where no discount
     * was applied.
     *
     * @return a result with zero discount and no applied discounts
     */
    public static DiscountCalculationResult none() {
        return new DiscountCalculationResult(BigDecimal.ZERO, List.of());
    }
}