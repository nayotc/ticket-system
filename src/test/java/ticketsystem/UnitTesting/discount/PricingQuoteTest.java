package ticketsystem.UnitTesting.discount;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import ticketsystem.DomainLayer.discount.AppliedDiscountResult;
import ticketsystem.DomainLayer.discount.PricingQuote;
import ticketsystem.DomainLayer.discount.DiscountKind;

class PricingQuoteTest {

    @Test
    void GivenNullSubtotal_WhenCreatePricingQuote_ThenThrowException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new PricingQuote(null, BigDecimal.ZERO, BigDecimal.ZERO, List.of())
        );

        assertEquals("Subtotal cannot be null", ex.getMessage());
    }

    @Test
    void GivenNullDiscountTotal_WhenCreatePricingQuote_ThenThrowException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new PricingQuote(BigDecimal.TEN, null, BigDecimal.TEN, List.of())
        );

        assertEquals("Discount total cannot be null", ex.getMessage());
    }

    @Test
    void GivenNullTotal_WhenCreatePricingQuote_ThenThrowException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new PricingQuote(BigDecimal.TEN, BigDecimal.ZERO, null, List.of())
        );

        assertEquals("Total cannot be null", ex.getMessage());
    }

    @Test
    void GivenNegativeSubtotal_WhenCreatePricingQuote_ThenThrowException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new PricingQuote(BigDecimal.valueOf(-1), BigDecimal.ZERO, BigDecimal.ZERO, List.of())
        );

        assertEquals("Subtotal cannot be negative", ex.getMessage());
    }

    @Test
    void GivenNegativeDiscountTotal_WhenCreatePricingQuote_ThenThrowException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new PricingQuote(BigDecimal.TEN, BigDecimal.valueOf(-1), BigDecimal.TEN, List.of())
        );

        assertEquals("Discount total cannot be negative", ex.getMessage());
    }

    @Test
    void GivenNegativeTotal_WhenCreatePricingQuote_ThenThrowException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new PricingQuote(BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.valueOf(-1), List.of())
        );

        assertEquals("Total cannot be negative", ex.getMessage());
    }

    @Test
    void GivenNullAppliedDiscounts_WhenCreatePricingQuote_ThenUseEmptyList() {
        PricingQuote quote = new PricingQuote(
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(80),
                null
        );

        assertEquals(BigDecimal.valueOf(100), quote.subtotal());
        assertEquals(BigDecimal.valueOf(20), quote.discountTotal());
        assertEquals(BigDecimal.valueOf(80), quote.total());
        assertNotNull(quote.appliedDiscounts());
        assertTrue(quote.appliedDiscounts().isEmpty());
    }

    @Test
    void GivenValidValues_WhenCreatePricingQuote_ThenFieldsAreSet() {
        AppliedDiscountResult discount = createAppliedDiscount();

        PricingQuote quote = new PricingQuote(
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(25),
                BigDecimal.valueOf(75),
                List.of(discount)
        );

        assertEquals(BigDecimal.valueOf(100), quote.subtotal());
        assertEquals(BigDecimal.valueOf(25), quote.discountTotal());
        assertEquals(BigDecimal.valueOf(75), quote.total());
        assertEquals(1, quote.appliedDiscounts().size());
        assertSame(discount, quote.appliedDiscounts().get(0));
    }

    @Test
    void GivenAppliedDiscounts_WhenCreatePricingQuote_ThenAppliedDiscountsListIsImmutable() {
        AppliedDiscountResult discount = createAppliedDiscount();

        PricingQuote quote = new PricingQuote(
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(90),
                List.of(discount)
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> quote.appliedDiscounts().clear()
        );
    }

    @Test
    void GivenOriginalAppliedDiscountsListChangedAfterCreation_WhenReadPricingQuote_ThenInternalListIsNotChanged() {
        AppliedDiscountResult discount1 = createAppliedDiscount();
        AppliedDiscountResult discount2 = createAppliedDiscount();

        List<AppliedDiscountResult> originalList = new ArrayList<>();
        originalList.add(discount1);

        PricingQuote quote = new PricingQuote(
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(90),
                originalList
        );

        originalList.add(discount2);

        assertEquals(1, quote.appliedDiscounts().size());
        assertSame(discount1, quote.appliedDiscounts().get(0));
    }

    @Test
    void GivenSubtotal_WhenCreateWithoutDiscounts_ThenTotalEqualsSubtotalAndDiscountIsZero() {
        PricingQuote quote = PricingQuote.withoutDiscounts(BigDecimal.valueOf(120));

        assertEquals(BigDecimal.valueOf(120), quote.subtotal());
        assertEquals(BigDecimal.ZERO, quote.discountTotal());
        assertEquals(BigDecimal.valueOf(120), quote.total());
        assertNotNull(quote.appliedDiscounts());
        assertTrue(quote.appliedDiscounts().isEmpty());
    }

    @Test
    void GivenNullSubtotal_WhenCreateWithoutDiscounts_ThenThrowException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> PricingQuote.withoutDiscounts(null)
        );

        assertEquals("Subtotal cannot be null", ex.getMessage());
    }

    /**
     * Verifies that an applied discount preserves its business discount kind.
     */
    @Test
    void GivenAppliedDiscount_WhenAccessKind_ThenReturnExpectedKind() {
        AppliedDiscountResult discount = createAppliedDiscount();

        assertEquals(DiscountKind.VISIBLE, discount.kind());
    }


    /**
     * Creates a valid applied discount used by the pricing quote unit tests.
     *
     * @return a visible applied discount with fixed test values
     */
    private AppliedDiscountResult createAppliedDiscount() {
        return new AppliedDiscountResult(
                "Test Discount",
                DiscountKind.VISIBLE,
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(20)
        );
    }
}
