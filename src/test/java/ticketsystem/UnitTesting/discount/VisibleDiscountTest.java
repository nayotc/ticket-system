package ticketsystem.UnitTesting.discount;


import org.junit.jupiter.api.Test;

import ticketsystem.DomainLayer.discount.VisibleDiscount;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class VisibleDiscountTest {

    @Test
    void GivenValidDiscount_WhenCalculateDiscount_ThenReturnDiscountAmount() {
        VisibleDiscount discount =
                new VisibleDiscount("Visible discount", BigDecimal.valueOf(10));

        BigDecimal result =
                discount.calculateDiscount(BigDecimal.valueOf(200), 2, null);

        assertEquals(0, BigDecimal.valueOf(20).compareTo(result));
    }

    @Test
    void GivenNullName_WhenCreateDiscount_ThenThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> new VisibleDiscount(null, BigDecimal.TEN));
    }

    @Test
    void GivenBlankName_WhenCreateDiscount_ThenThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> new VisibleDiscount("   ", BigDecimal.TEN));
    }

    @Test
    void GivenNullPercentage_WhenCreateDiscount_ThenThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> new VisibleDiscount("Discount", null));
    }

    @Test
    void GivenNegativePercentage_WhenCreateDiscount_ThenThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> new VisibleDiscount("Discount", BigDecimal.valueOf(-1)));
    }

    @Test
    void GivenPercentageGreaterThan100_WhenCreateDiscount_ThenThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> new VisibleDiscount("Discount", BigDecimal.valueOf(101)));
    }

    @Test
    void GivenValidPercentage_WhenSetPercentage_ThenUpdatePercentage() {
        VisibleDiscount discount =
                new VisibleDiscount("Discount", BigDecimal.TEN);

        discount.setPercentage(BigDecimal.valueOf(25));

        assertEquals(0, BigDecimal.valueOf(25).compareTo(discount.getPercentage()));
    }
}