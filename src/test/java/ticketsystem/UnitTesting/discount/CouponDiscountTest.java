package ticketsystem.UnitTesting.discount;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import ticketsystem.DomainLayer.discount.CouponDiscount;

class CouponDiscountTest {

    @Test
    void GivenCorrectCouponCode_WhenCalculateDiscount_ThenReturnDiscountAmount() {
        CouponDiscount discount = new CouponDiscount(
                "Coupon BGU10",
                1L,
                "BGU10",
                BigDecimal.valueOf(10),
                LocalDateTime.now().plusDays(7)
        );

        BigDecimal result = discount.calculateDiscount(
                BigDecimal.valueOf(200),
                1,
                "BGU10"
        );

        assertEquals(0, BigDecimal.valueOf(20).compareTo(result));
    }

    @Test
    void GivenIncorrectCouponCode_WhenCalculateDiscount_ThenReturnZero() {
        CouponDiscount discount = new CouponDiscount(
                "Coupon BGU10",
                1L,
                "BGU10",
                BigDecimal.valueOf(10),
                LocalDateTime.now().plusDays(7)
        );

        BigDecimal result = discount.calculateDiscount(
                BigDecimal.valueOf(200),
                1,
                "WRONG"
        );

        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    @Test
    void GivenNullCouponCode_WhenCalculateDiscount_ThenReturnZero() {
        CouponDiscount discount = new CouponDiscount(
                "Coupon BGU10",
                1L,
                "BGU10",
                BigDecimal.valueOf(10),
                LocalDateTime.now().plusDays(7)
        );

        BigDecimal result = discount.calculateDiscount(
                BigDecimal.valueOf(200),
                1,
                null
        );

        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    @Test
    void GivenExpiredCoupon_WhenCalculateDiscount_ThenReturnZero() {
        CouponDiscount discount = new CouponDiscount(
                "Coupon BGU10",
                1L,
                "BGU10",
                BigDecimal.valueOf(10),
                LocalDateTime.now().minusDays(1)
        );

        BigDecimal result = discount.calculateDiscount(
                BigDecimal.valueOf(200),
                1,
                "BGU10"
        );

        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    @Test
    void GivenCouponDiscount_WhenGettersCalled_ThenReturnCorrectValues() {
        LocalDateTime endTime = LocalDateTime.now().plusDays(7);

        CouponDiscount discount = new CouponDiscount(
                "Coupon BGU10",
                1L,
                "BGU10",
                BigDecimal.valueOf(10),
                endTime
        );

        assertEquals("BGU10", discount.getCouponCode());
        assertEquals(0, BigDecimal.valueOf(10).compareTo(discount.getPercentage()));
        assertEquals(endTime, discount.getEndTime());
    }
}