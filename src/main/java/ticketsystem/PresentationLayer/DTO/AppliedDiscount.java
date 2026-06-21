package ticketsystem.PresentationLayer.DTO;

import java.math.BigDecimal;

import ticketsystem.DomainLayer.discount.DiscountKind;

/**
 * Presentation DTO describing a discount displayed in order-related views.
 *
 * <p>This object is intended for UI presentation only. It does not represent
 * the full domain discount policy, but only a discount that was actually
 * applied during the current pricing calculation.</p>
 *
 * <p>The discount kind allows the views to distinguish coupon discounts from
 * visible or conditional discounts without relying on the discount name or
 * description.</p>
 *
 * @param name        discount display name
 * @param kind        business type of the applied discount
 * @param description discount display description
 * @param amount      discount amount to display
 */
public record AppliedDiscount(
        String name,
        DiscountKind kind,
        String description,
        BigDecimal amount
) {
}
