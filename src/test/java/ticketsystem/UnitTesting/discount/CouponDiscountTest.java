package ticketsystem.UnitTesting.discount;

import org.junit.jupiter.api.Test;

import ticketsystem.DomainLayer.discount.CouponDiscount;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CouponDiscountTest {

    @Test
    void GivenCorrectCouponCode_WhenCalculateDiscount_ThenReturnDiscountAmount() {
        CouponDiscount discount =
                new CouponDiscount(
                        "Coupon",
                        "ABC123",
                        BigDecimal.valueOf(20),
                        LocalDateTime.now().plusDays(1)
                );

        BigDecimal result =
                discount.calculateDiscount(BigDecimal.valueOf(100), 1, "ABC123");

        assertEquals(0, BigDecimal.valueOf(20).compareTo(result));
    }

    @Test
    void GivenWrongCouponCode_WhenCalculateDiscount_ThenReturnZero() {
        CouponDiscount discount =
                new CouponDiscount("Coupon", "ABC123", BigDecimal.TEN, null);

        BigDecimal result =
                discount.calculateDiscount(BigDecimal.valueOf(100), 1, "WRONG");

        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    @Test
    void GivenNullCouponCode_WhenCalculateDiscount_ThenReturnZero() {
        CouponDiscount discount =
                new CouponDiscount("Coupon", "ABC123", BigDecimal.TEN, null);

        BigDecimal result =
                discount.calculateDiscount(BigDecimal.valueOf(100), 1, null);

        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    @Test
    void GivenExpiredCoupon_WhenCalculateDiscount_ThenReturnZero() {
        CouponDiscount discount =
                new CouponDiscount(
                        "Coupon",
                        "ABC123",
                        BigDecimal.TEN,
                        LocalDateTime.now().plusDays(1)
                );

        discount.setEndTime(LocalDateTime.now().minusDays(1));

        BigDecimal result =
                discount.calculateDiscount(BigDecimal.valueOf(100), 1, "ABC123");

        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    @Test
    void GivenNullCouponCode_WhenCreateCouponDiscount_ThenThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> new CouponDiscount("Coupon", null, BigDecimal.TEN, null));
    }

    @Test
    void GivenBlankCouponCode_WhenCreateCouponDiscount_ThenThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> new CouponDiscount("Coupon", "   ", BigDecimal.TEN, null));
    }

    @Test
    void GivenPastEndTime_WhenCreateCouponDiscount_ThenThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> new CouponDiscount(
                        "Coupon",
                        "ABC123",
                        BigDecimal.TEN,
                        LocalDateTime.now().minusDays(1)
                ));
    }

    @Test
    void GivenValidCouponCode_WhenSetCouponCode_ThenUpdateCouponCode() {
        CouponDiscount discount =
                new CouponDiscount("Coupon", "OLD", BigDecimal.TEN, null);

        discount.setCouponCode("NEW");

        assertEquals("NEW", discount.getCouponCode());
    }
}