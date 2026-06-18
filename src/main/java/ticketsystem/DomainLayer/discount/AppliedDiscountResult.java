package ticketsystem.DomainLayer.discount;

import java.math.BigDecimal;

/**
 * Represents a single discount that was actually applied during a discount
 * calculation.
 *
 * <p>This is a domain-level result object. It describes the business result
 * of applying one discount rule: its name, type, percentage, and the monetary
 * amount deducted from the current price.</p>
 *
 * <p>The discount kind allows upper layers to distinguish between visible,
 * conditional, and coupon discounts without checking concrete domain
 * implementation classes.</p>
 *
 * @param name       the display/business name of the applied discount
 * @param kind       the business type of the applied discount
 * @param percentage the discount percentage that was applied
 * @param amount     the monetary amount deducted by this discount
 */
public record AppliedDiscountResult(
        String name,
        DiscountKind kind,
        BigDecimal percentage,
        BigDecimal amount
) {

    public AppliedDiscountResult {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Applied discount name cannot be empty"
            );
        }

        if (kind == null) {
            throw new IllegalArgumentException(
                    "Applied discount kind cannot be null"
            );
        }

        if (percentage == null) {
            throw new IllegalArgumentException(
                    "Applied discount percentage cannot be null"
            );
        }

        if (amount == null) {
            throw new IllegalArgumentException(
                    "Applied discount amount cannot be null"
            );
        }

        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Applied discount amount cannot be negative"
            );
        }
    }
}
