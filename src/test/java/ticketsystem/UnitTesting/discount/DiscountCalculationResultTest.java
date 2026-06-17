package ticketsystem.UnitTesting.discount;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import ticketsystem.DomainLayer.discount.AppliedDiscountResult;
import ticketsystem.DomainLayer.discount.DiscountCalculationResult;

class DiscountCalculationResultTest {

    @Test
    void GivenNullDiscountTotal_WhenCreateDiscountCalculationResult_ThenThrowException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new DiscountCalculationResult(null, List.of())
        );

        assertEquals("Discount total cannot be null", ex.getMessage());
    }

    @Test
    void GivenNegativeDiscountTotal_WhenCreateDiscountCalculationResult_ThenThrowException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new DiscountCalculationResult(BigDecimal.valueOf(-1), List.of())
        );

        assertEquals("Discount total cannot be negative", ex.getMessage());
    }

    @Test
    void GivenNullAppliedDiscounts_WhenCreateDiscountCalculationResult_ThenUseEmptyList() {
        DiscountCalculationResult result =
                new DiscountCalculationResult(BigDecimal.TEN, null);

        assertEquals(BigDecimal.TEN, result.discountTotal());
        assertNotNull(result.appliedDiscounts());
        assertTrue(result.appliedDiscounts().isEmpty());
    }

    @Test
    void GivenValidValues_WhenCreateDiscountCalculationResult_ThenFieldsAreSet() {
        AppliedDiscountResult appliedDiscount = mock(AppliedDiscountResult.class);

        DiscountCalculationResult result =
                new DiscountCalculationResult(
                        BigDecimal.valueOf(25),
                        List.of(appliedDiscount)
                );

        assertEquals(BigDecimal.valueOf(25), result.discountTotal());
        assertEquals(1, result.appliedDiscounts().size());
        assertSame(appliedDiscount, result.appliedDiscounts().get(0));
    }

    @Test
    void GivenAppliedDiscounts_WhenCreateDiscountCalculationResult_ThenListIsImmutable() {
        AppliedDiscountResult appliedDiscount = mock(AppliedDiscountResult.class);

        DiscountCalculationResult result =
                new DiscountCalculationResult(
                        BigDecimal.valueOf(10),
                        List.of(appliedDiscount)
                );

        assertThrows(
                UnsupportedOperationException.class,
                () -> result.appliedDiscounts().clear()
        );
    }

    @Test
    void GivenOriginalAppliedDiscountsListChangedAfterCreation_WhenReadResult_ThenResultListIsNotChanged() {
        AppliedDiscountResult appliedDiscount1 = mock(AppliedDiscountResult.class);
        AppliedDiscountResult appliedDiscount2 = mock(AppliedDiscountResult.class);

        List<AppliedDiscountResult> originalList =
                new ArrayList<>(List.of(appliedDiscount1));

        DiscountCalculationResult result =
                new DiscountCalculationResult(BigDecimal.valueOf(10), originalList);

        originalList.add(appliedDiscount2);

        assertEquals(1, result.appliedDiscounts().size());
        assertSame(appliedDiscount1, result.appliedDiscounts().get(0));
    }

    @Test
    void GivenNone_WhenCreateNoneResult_ThenReturnZeroDiscountAndEmptyAppliedDiscounts() {
        DiscountCalculationResult result = DiscountCalculationResult.none();

        assertEquals(BigDecimal.ZERO, result.discountTotal());
        assertNotNull(result.appliedDiscounts());
        assertTrue(result.appliedDiscounts().isEmpty());
    }
}