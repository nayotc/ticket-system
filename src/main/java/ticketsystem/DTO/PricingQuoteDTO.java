package ticketsystem.DTO;

import java.math.BigDecimal;
import java.util.List;

/**
 * Application-layer DTO that represents the pricing result of an active order.
 *
 * This DTO is returned by application services instead of exposing domain-layer
 * pricing result objects to the presentation layer.
 *
 * It contains the original subtotal, the total discount amount, the final total
 * after discounts, and the discounts that were actually applied during the
 * pricing calculation.
 *
 * @param subtotal the original order price before discounts
 * @param discountTotal the total amount deducted by all applied discounts
 * @param total the final order price after discounts
 * @param appliedDiscounts the discounts applied during the pricing calculation
 */
public record PricingQuoteDTO(
        BigDecimal subtotal,
        BigDecimal discountTotal,
        BigDecimal total,
        List<AppliedDiscountDTO> appliedDiscounts
) {
    public PricingQuoteDTO {
        if (subtotal == null) {
            throw new IllegalArgumentException("Subtotal cannot be null");
        }
        if (discountTotal == null) {
            throw new IllegalArgumentException("Discount total cannot be null");
        }
        if (total == null) {
            throw new IllegalArgumentException("Total cannot be null");
        }
        if (subtotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Subtotal cannot be negative");
        }
        if (discountTotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Discount total cannot be negative");
        }
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Total cannot be negative");
        }
        if (appliedDiscounts == null) {
            appliedDiscounts = List.of();
        }
        appliedDiscounts = List.copyOf(appliedDiscounts);
    }

    /**
     * Creates a pricing DTO with no applied discounts.
     *
     * @param subtotal the original order price before discounts
     * @return a pricing DTO where the final total equals the subtotal
     */
    public static PricingQuoteDTO withoutDiscounts(BigDecimal subtotal) {
        return new PricingQuoteDTO(
                subtotal,
                BigDecimal.ZERO,
                subtotal,
                List.of()
        );
    }
}