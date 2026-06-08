package ticketsystem.UnitTesting.discount;

import org.junit.jupiter.api.Test;

import ticketsystem.DomainLayer.discount.ConditionalDiscount;
import ticketsystem.DomainLayer.discount.CouponDiscount;
import ticketsystem.DomainLayer.discount.DiscountCompositionType;
import ticketsystem.DomainLayer.discount.DiscountPolicy;
import ticketsystem.DomainLayer.discount.MinTicketsCondition;
import ticketsystem.DomainLayer.discount.VisibleDiscount;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class DiscountPolicyTest {

    @Test
    void GivenNullCompositionType_WhenCreatePolicy_ThenThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> new DiscountPolicy(null));
    }

    @Test
    void GivenEmptyPolicy_WhenCalculateDiscount_ThenReturnZero() {
        DiscountPolicy policy = new DiscountPolicy(DiscountCompositionType.SUM);

        BigDecimal result =
                policy.calculateDiscount(BigDecimal.valueOf(100), 1, null);

        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    @Test
    void GivenNullDiscount_WhenAddDiscount_ThenThrowException() {
        DiscountPolicy policy = new DiscountPolicy(DiscountCompositionType.SUM);

        assertThrows(IllegalArgumentException.class,
                () -> policy.addDiscount(null));
    }

    @Test
    void GivenSumComposition_WhenCalculateDiscount_ThenApplyDiscountsSequentially() {
        DiscountPolicy policy = new DiscountPolicy(DiscountCompositionType.SUM);

        policy.addDiscount(new VisibleDiscount("10 percent", BigDecimal.valueOf(10)));
        policy.addDiscount(new VisibleDiscount("20 percent", BigDecimal.valueOf(20)));

        BigDecimal result =
                policy.calculateDiscount(BigDecimal.valueOf(100), 1, null);

        assertEquals(0, BigDecimal.valueOf(28).compareTo(result));
    }

    @Test
    void GivenMaxComposition_WhenCalculateDiscount_ThenReturnHighestDiscountOnly() {
        DiscountPolicy policy = new DiscountPolicy(DiscountCompositionType.MAX);

        policy.addDiscount(new VisibleDiscount("10 percent", BigDecimal.valueOf(10)));
        policy.addDiscount(new VisibleDiscount("30 percent", BigDecimal.valueOf(30)));
        policy.addDiscount(new VisibleDiscount("20 percent", BigDecimal.valueOf(20)));

        BigDecimal result =
                policy.calculateDiscount(BigDecimal.valueOf(100), 1, null);

        assertEquals(0, BigDecimal.valueOf(30).compareTo(result));
    }

    @Test
    void GivenConditionalDiscountNotSatisfied_WhenCalculateDiscount_ThenIgnoreIt() {
        DiscountPolicy policy = new DiscountPolicy(DiscountCompositionType.SUM);

        policy.addDiscount(
                new ConditionalDiscount(
                        "Min 3 tickets",
                        BigDecimal.valueOf(50),
                        new MinTicketsCondition(3)
                )
        );

        BigDecimal result =
                policy.calculateDiscount(BigDecimal.valueOf(100), 2, null);

        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    @Test
    void GivenCouponDiscountWithWrongCode_WhenCalculateDiscount_ThenIgnoreIt() {
        DiscountPolicy policy = new DiscountPolicy(DiscountCompositionType.SUM);

        policy.addDiscount(
                new CouponDiscount(
                        "Coupon",
                        "ABC123",
                        BigDecimal.valueOf(50),
                        null
                )
        );

        BigDecimal result =
                policy.calculateDiscount(BigDecimal.valueOf(100), 1, "WRONG");

        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    @Test
    void GivenValidCompositionType_WhenSetCompositionType_ThenUpdateCompositionType() {
        DiscountPolicy policy = new DiscountPolicy(DiscountCompositionType.SUM);

        policy.setDiscountCompositionType(DiscountCompositionType.MAX);

        assertEquals(DiscountCompositionType.MAX, policy.getDiscountCompositionType());
    }

    @Test
    void GivenNullCompositionType_WhenSetCompositionType_ThenThrowException() {
        DiscountPolicy policy = new DiscountPolicy(DiscountCompositionType.SUM);

        assertThrows(IllegalArgumentException.class,
                () -> policy.setDiscountCompositionType(null));
    }
}