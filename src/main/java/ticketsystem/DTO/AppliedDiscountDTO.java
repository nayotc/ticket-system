package ticketsystem.DTO;

import java.math.BigDecimal;

import ticketsystem.DomainLayer.discount.DiscountKind;

/**
 * Application-layer DTO that represents a discount applied during a pricing
 * calculation.
 *
 * <p>This DTO describes a discount that was actually applied to a specific
 * order pricing calculation. It is different from {@link DiscountDTO}, which
 * describes a discount rule that exists in a discount policy.</p>
 *
 * <p>The discount kind allows upper layers to distinguish between visible,
 * conditional, and coupon discounts without exposing concrete domain
 * implementation classes.</p>
 *
 * @param name       the applied discount name
 * @param kind       the business type of the applied discount
 * @param percentage the discount percentage that was applied
 * @param amount     the monetary amount deducted by this discount
 */
public record AppliedDiscountDTO(
        String name,
        DiscountKind kind,
        BigDecimal percentage,
        BigDecimal amount
) {

    public AppliedDiscountDTO {
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

        if (percentage.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Applied discount percentage cannot be negative"
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
