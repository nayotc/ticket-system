package ticketsystem.UnitTesting.discount;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import ticketsystem.DomainLayer.discount.ConditionalDiscount;
import ticketsystem.DomainLayer.discount.MaxTicketsCondition;
import ticketsystem.DomainLayer.discount.MinTicketsCondition;


class ConditionalDiscountTest {

    @Test
    void GivenNullCondition_WhenCreateConditionalDiscount_ThenThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> new ConditionalDiscount("Conditional", BigDecimal.TEN, null));
    }

    @Test
    void GivenSatisfiedCondition_WhenCalculateDiscount_ThenReturnDiscountAmount() {
        ConditionalDiscount discount =
                new ConditionalDiscount(
                        "Min tickets discount",
                        BigDecimal.valueOf(10),
                        new MinTicketsCondition(2)
                );

        BigDecimal result =
                discount.calculateDiscount(BigDecimal.valueOf(100), 2, null);

        assertEquals(0, BigDecimal.valueOf(10).compareTo(result));
    }

    @Test
    void GivenUnsatisfiedCondition_WhenCalculateDiscount_ThenReturnZero() {
        ConditionalDiscount discount =
                new ConditionalDiscount(
                        "Min tickets discount",
                        BigDecimal.valueOf(10),
                        new MinTicketsCondition(3)
                );

        BigDecimal result =
                discount.calculateDiscount(BigDecimal.valueOf(100), 2, null);

        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    @Test
    void GivenValidCondition_WhenSetCondition_ThenConditionIsUpdated() {
        ConditionalDiscount discount =
                new ConditionalDiscount(
                        "Conditional",
                        BigDecimal.TEN,
                        new MinTicketsCondition(2)
                );

        MaxTicketsCondition newCondition = new MaxTicketsCondition(5);

        discount.setCondition(newCondition);

        assertSame(newCondition, discount.getCondition());
    }

    @Test
    void GivenNullCondition_WhenSetCondition_ThenThrowException() {
        ConditionalDiscount discount =
                new ConditionalDiscount(
                        "Conditional",
                        BigDecimal.TEN,
                        new MinTicketsCondition(2)
                );

        assertThrows(IllegalArgumentException.class,
                () -> discount.setCondition(null));
    }
}