package ticketsystem.PresentationLayer.DTO;

import java.math.BigDecimal;
import java.util.List;

/**
 * Presentation DTO with pricing information for order-related views.
 *
 * This object contains the pricing summary displayed to the user. At this
 * stage it may represent a preview price, while the final validated amount
 * is calculated during checkout.
 *
 * @param subtotal order price before discounts
 * @param discountTotal total discount amount
 * @param total final displayed total
 * @param appliedDiscounts discounts displayed to the user
 * @param policyMessages pricing or policy messages displayed to the user
 */
public record OrderPricing(
        BigDecimal subtotal,
        BigDecimal discountTotal,
        BigDecimal total,
        List<AppliedDiscount> appliedDiscounts,
        List<String> policyMessages
) {
}
