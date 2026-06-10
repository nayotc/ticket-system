package ticketsystem.DTO;

import java.math.BigDecimal;

/**
 * Application-layer DTO that represents a discount applied during pricing
 * calculation.
 *
 * This DTO describes a discount that was actually applied to a specific order
 * pricing calculation. It is different from DiscountDTO, which describes a
 * discount rule that exists in a discount policy.
 *
 * Application services should return this DTO instead of exposing domain-layer
 * discount result objects to upper layers.
 *
 * @param name the applied discount name
 * @param percentage the discount percentage that was applied
 * @param amount the monetary amount deducted by this discount
 */
public record AppliedDiscountDTO(
        String name,
        BigDecimal percentage,
        BigDecimal amount
) {
    public AppliedDiscountDTO {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Applied discount name cannot be empty");
        }
        if (percentage == null) {
            throw new IllegalArgumentException("Applied discount percentage cannot be null");
        }
        if (percentage.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Applied discount percentage cannot be negative");
        }
        if (amount == null) {
            throw new IllegalArgumentException("Applied discount amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Applied discount amount cannot be negative");
        }
    }
}