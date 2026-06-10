package ticketsystem.DomainLayer.discount;

import java.math.BigDecimal;

/**
 * Represents a single discount that was actually applied during a discount
 * calculation.
 *
 * This is a domain-level result object. It describes the business result
 * of applying one discount rule: the discount name, its percentage, and the
 * monetary amount deducted from the current price.
 *
 * This object does not belong to the UI layer. Presentation objects such as
 * OrderPricing or AppliedDiscount should be created later by mapping this
 * domain result in the presenter layer.
 *
 * @param name the display/business name of the applied discount
 * @param percentage the discount percentage that was applied
 * @param amount the monetary amount deducted by this discount
 */
public record AppliedDiscountResult(
        String name,
        BigDecimal percentage,
        BigDecimal amount
) {
    public AppliedDiscountResult {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Applied discount name cannot be empty");
        }
        if (percentage == null) {
            throw new IllegalArgumentException("Applied discount percentage cannot be null");
        }
        if (amount == null) {
            throw new IllegalArgumentException("Applied discount amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Applied discount amount cannot be negative");
        }
    }
}