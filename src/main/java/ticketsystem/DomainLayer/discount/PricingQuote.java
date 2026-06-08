package ticketsystem.DomainLayer.discount;

import java.math.BigDecimal;
import java.util.List;

/**
 * Represents the full pricing result after applying discount policies.
 *
 * This is a domain-level calculation result. It contains the original price,
 * the total discount amount, the final price after discounts, and the list of
 * discounts that were actually applied.
 *
 * The UI should not calculate these values by itself. Instead, the domain
 * calculates the pricing quote, and the presentation layer can later map it
 * into its own display DTO.
 *
 * @param subtotal the original price before discounts
 * @param discountTotal the total amount deducted by all applied discounts
 * @param total the final price after discounts
 * @param appliedDiscounts the discounts that were actually applied
 */
public record PricingQuote(
        BigDecimal subtotal,
        BigDecimal discountTotal,
        BigDecimal total,
        List<AppliedDiscountResult> appliedDiscounts
) {
    public PricingQuote {
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
     * Creates a pricing quote with no applied discounts.
     *
     * @param subtotal the original price before discounts
     * @return a pricing quote where the final total equals the subtotal
     */
    public static PricingQuote withoutDiscounts(BigDecimal subtotal) {
        return new PricingQuote(
                subtotal,
                BigDecimal.ZERO,
                subtotal,
                List.of()
        );
    }
}