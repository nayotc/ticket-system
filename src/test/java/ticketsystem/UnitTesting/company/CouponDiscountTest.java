package ticketsystem.UnitTesting.company;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import ticketsystem.DomainLayer.company.CouponDiscount;

class CouponDiscountTest {

    @Test
    void GivenCorrectCouponCode_WhenCalculateDiscount_ThenReturnDiscountAmount() {
        CouponDiscount discount = new CouponDiscount(
                "Coupon BGU10",
                1L,
                "BGU10",
                BigDecimal.valueOf(10)
        );

        BigDecimal result = discount.calculateDiscount(
                BigDecimal.valueOf(200),
                1,
                "BGU10"
        );

        assertEquals(0, BigDecimal.valueOf(20).compareTo(result));
    }

    @Test
    void GivenIncorrectCouponCode_WhenCalculateDiscount_ThenThrowException() {
        CouponDiscount discount = new CouponDiscount(
                "Coupon BGU10",
                1L,
                "BGU10",
                BigDecimal.valueOf(10)
        );

        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                discount.calculateDiscount(
                        BigDecimal.valueOf(200),
                        1,
                        "WRONG"
                )
        );

        assertTrue(exception.getMessage().contains("Incorrect coupon"));
    }

    @Test
    void GivenCouponDiscount_WhenGettersCalled_ThenReturnCorrectValues() {
        CouponDiscount discount = new CouponDiscount(
                "Coupon BGU10",
                1L,
                "BGU10",
                BigDecimal.valueOf(10)
        );

        assertEquals("BGU10", discount.getCouponCode());
        assertEquals(0, BigDecimal.valueOf(10).compareTo(discount.getPercentage()));
    }
}