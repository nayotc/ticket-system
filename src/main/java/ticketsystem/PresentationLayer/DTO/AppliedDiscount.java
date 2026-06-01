package ticketsystem.PresentationLayer.DTO;

import java.math.BigDecimal;

/**
 * Presentation DTO describing a discount displayed in order-related views.
 *
 * This object is intended for UI presentation only. It does not represent
 * the full domain discount policy, but only the discount information that
 * should be shown to the user.
 *
 * @param name discount display name
 * @param description discount display description
 * @param amount discount amount to display
 */
public record AppliedDiscount(
        String name,
        String description,
        BigDecimal amount
) {
}
