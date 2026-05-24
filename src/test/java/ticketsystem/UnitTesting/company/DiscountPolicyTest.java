package ticketsystem.UnitTesting.company;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.DomainLayer.company.DiscountPolicy;
import ticketsystem.DomainLayer.company.VisibleDiscount;
import ticketsystem.DomainLayer.company.CouponDiscount;
import ticketsystem.DomainLayer.company.DiscountPolicy.DiscountCompositionType;

class DiscountPolicyTest {

        private DiscountPolicy discountPolicy;

        @BeforeEach
        void setUp() {
                discountPolicy = new DiscountPolicy(DiscountCompositionType.MAX);
        }

        @Test
        void GivenEmptyDiscountPolicy_WhenCalculateDiscount_ThenReturnZero() {
                BigDecimal result = discountPolicy.calculateDiscount(
                                BigDecimal.valueOf(100),
                                1,
                                null);

                assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }

        @Test
        void GivenNullDiscount_WhenAddDiscount_ThenThrowException() {
                Exception exception = assertThrows(IllegalArgumentException.class,
                                () -> discountPolicy.addDiscount(null));

                assertTrue(exception.getMessage().contains("Discount cannot be null"));
        }

        @Test
        void GivenDiscount_WhenAddDiscount_ThenDiscountIsAdded() {
                VisibleDiscount discount = new VisibleDiscount(
                                "Visible Discount",
                                1L,
                                BigDecimal.valueOf(10)

                );

                discountPolicy.addDiscount(discount);

                assertEquals(1, discountPolicy.getDiscounts().size());
                assertSame(discount, discountPolicy.getDiscounts().get(0));
        }

        @Test
        void GivenMaxComposition_WhenCalculateDiscount_ThenReturnHighestDiscountAmount() {
                discountPolicy.addDiscount(new VisibleDiscount(
                                "10 Percent",
                                1L,
                                BigDecimal.valueOf(10)

                ));

                discountPolicy.addDiscount(new VisibleDiscount(
                                "20 Percent",
                                2L,
                                BigDecimal.valueOf(20)

                ));

                BigDecimal result = discountPolicy.calculateDiscount(
                                BigDecimal.valueOf(100),
                                1,
                                null);

                assertEquals(0, BigDecimal.valueOf(20).compareTo(result));
        }

        @Test
        void GivenSumComposition_WhenCalculateDiscount_ThenReturnTieredTotalDiscountAmount() {
                DiscountPolicy sumPolicy = new DiscountPolicy(DiscountCompositionType.SUM);

                sumPolicy.addDiscount(new VisibleDiscount(
                                "10 Percent",
                                1L,
                                BigDecimal.valueOf(10)

                ));

                sumPolicy.addDiscount(new VisibleDiscount(
                                "20 Percent",
                                2L,
                                BigDecimal.valueOf(20)

                ));

                BigDecimal result = sumPolicy.calculateDiscount(
                                BigDecimal.valueOf(100),
                                1,
                                null);

                assertEquals(0, BigDecimal.valueOf(28).compareTo(result));
        }

        @Test
        void GivenCouponDiscountWithCorrectCoupon_WhenCalculateDiscount_ThenCouponIsApplied() {
                discountPolicy.addDiscount(new CouponDiscount(
                                "Coupon",
                                1L,
                                "BGU10",
                                BigDecimal.valueOf(10)));

                BigDecimal result = discountPolicy.calculateDiscount(
                                BigDecimal.valueOf(100),
                                1,
                                "BGU10");

                assertEquals(0, BigDecimal.valueOf(10).compareTo(result));
        }

        @Test
        void GivenCompositionTypeChanged_WhenGetCompositionType_ThenReturnUpdatedType() {
                discountPolicy.setDiscountCompositionType(DiscountCompositionType.SUM);

                assertEquals(
                                DiscountCompositionType.SUM,
                                discountPolicy.getDiscountCompositionType());
        }

        @Test
        void GivenExistingDiscount_WhenRemoveDiscountFromCompany_ThenDiscountIsRemoved() {
                // Arrange
                VisibleDiscount discount = new VisibleDiscount(
                                "Visible Discount",
                                1L,
                                BigDecimal.valueOf(10));

                discountPolicy.addDiscount(discount);

                assertEquals(1, discountPolicy.getDiscounts().size());

                // Act
                discountPolicy.removeDiscountFromCompany(1L);

                // Assert
                assertTrue(discountPolicy.getDiscounts().isEmpty());
        }

        @Test
        void GivenMultipleDiscounts_WhenRemoveOneDiscountFromCompany_ThenOnlyRequestedDiscountIsRemoved() {
                // Arrange
                VisibleDiscount discount1 = new VisibleDiscount(
                                "10 Percent",
                                1L,
                                BigDecimal.valueOf(10));

                VisibleDiscount discount2 = new VisibleDiscount(
                                "20 Percent",
                                2L,
                                BigDecimal.valueOf(20));

                discountPolicy.addDiscount(discount1);
                discountPolicy.addDiscount(discount2);

                assertEquals(2, discountPolicy.getDiscounts().size());

                // Act
                discountPolicy.removeDiscountFromCompany(1L);

                // Assert
                assertEquals(1, discountPolicy.getDiscounts().size());
                assertSame(discount2, discountPolicy.getDiscounts().get(0));
        }

        @Test
        void GivenNonExistingDiscountId_WhenRemoveDiscountFromCompany_ThenThrowException() {
                // Arrange
                VisibleDiscount discount = new VisibleDiscount(
                                "Visible Discount",
                                1L,
                                BigDecimal.valueOf(10));

                discountPolicy.addDiscount(discount);

                // Act + Assert
                Exception exception = assertThrows(IllegalArgumentException.class,
                                () -> discountPolicy.removeDiscountFromCompany(999L));

                assertTrue(exception.getMessage().contains("Discount not found"));
                assertEquals(1, discountPolicy.getDiscounts().size());
        }

        @Test
        void GivenEmptyDiscountPolicy_WhenRemoveDiscountFromCompany_ThenThrowException() {
                // Act + Assert
                Exception exception = assertThrows(IllegalArgumentException.class,
                                () -> discountPolicy.removeDiscountFromCompany(1L));

                assertTrue(exception.getMessage().contains("Discount not found"));
        }
}