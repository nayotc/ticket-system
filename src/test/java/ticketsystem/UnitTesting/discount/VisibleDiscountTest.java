package ticketsystem.UnitTesting.discount;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import ticketsystem.DomainLayer.discount.VisibleDiscount;

class VisibleDiscountTest {

    @Test
    void GivenVisibleDiscount10Percent_WhenCalculateDiscount_ThenReturn10PercentOfTotalPrice() {
        VisibleDiscount discount = new VisibleDiscount(
                "Summer Sale",
                1L,
                BigDecimal.valueOf(10)
        );

        BigDecimal result = discount.calculateDiscount(
                BigDecimal.valueOf(200),
                2,
                null
        );

        assertEquals(0, BigDecimal.valueOf(20).compareTo(result));
    }

    @Test
    void GivenVisibleDiscount_WhenGettersCalled_ThenReturnCorrectValues() {
        VisibleDiscount discount = new VisibleDiscount(
                "Student Discount",
                1L,
                BigDecimal.valueOf(15)
                
        );

        assertEquals("Student Discount", discount.getName());
        assertEquals(0, BigDecimal.valueOf(15).compareTo(discount.getPercentage()));
    }
}