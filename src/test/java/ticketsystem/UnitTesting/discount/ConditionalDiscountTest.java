package ticketsystem.UnitTesting.discount;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import ticketsystem.DomainLayer.discount.ConditionalDiscount;
import ticketsystem.DomainLayer.discount.ConditionalDiscount.Condition;

class ConditionalDiscountTest {

    @Test
    void GivenMinTicketConditionSatisfied_WhenCalculateDiscount_ThenReturnDiscountAmount() {
        ConditionalDiscount discount = new ConditionalDiscount(
                "Buy 3 Tickets Discount",
                1L,
                null,
                null,
                BigDecimal.valueOf(10),
                Condition.MIN_TICKET,
                3
        );

        BigDecimal result = discount.calculateDiscount(
                BigDecimal.valueOf(300),
                3,
                null
        );

        assertEquals(0, BigDecimal.valueOf(30).compareTo(result));
    }

    @Test
    void GivenMinTicketConditionNotSatisfied_WhenCalculateDiscount_ThenReturnZero() {
        ConditionalDiscount discount = new ConditionalDiscount(
                "Buy 3 Tickets Discount",
                1L,
                null,
                null,
                BigDecimal.valueOf(10),
                Condition.MIN_TICKET,
                3
        );

        BigDecimal result = discount.calculateDiscount(
                BigDecimal.valueOf(300),
                2,
                null
        );

        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    @Test
    void GivenMaxTicketConditionSatisfied_WhenCalculateDiscount_ThenReturnDiscountAmount() {
        ConditionalDiscount discount = new ConditionalDiscount(
                "Max 5 Tickets Discount",
                1L,
                null,
                null,
                BigDecimal.valueOf(20),
                Condition.MAX_TICKET,
                5
        );

        BigDecimal result = discount.calculateDiscount(
                BigDecimal.valueOf(100),
                4,
                null
        );

        assertEquals(0, BigDecimal.valueOf(20).compareTo(result));
    }

    @Test
    void GivenMaxTicketConditionNotSatisfied_WhenCalculateDiscount_ThenReturnZero() {
        ConditionalDiscount discount = new ConditionalDiscount(
                "Max 5 Tickets Discount",
                1L,
                null,
                null,
                BigDecimal.valueOf(20),
                Condition.MAX_TICKET,
                5
        );

        BigDecimal result = discount.calculateDiscount(
                BigDecimal.valueOf(100),
                6,
                null
        );

        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    @Test
    void GivenDateConditionInValidRange_WhenCalculateDiscount_ThenReturnDiscountAmount() {
        ConditionalDiscount discount = new ConditionalDiscount(
                "Early Bird",
                1L,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1),
                BigDecimal.valueOf(15),
                Condition.DATE,
                null
        );

        BigDecimal result = discount.calculateDiscount(
                BigDecimal.valueOf(200),
                1,
                null
        );

        assertEquals(0, BigDecimal.valueOf(30).compareTo(result));
    }

    @Test
    void GivenDateConditionExpired_WhenCalculateDiscount_ThenReturnZero() {
        ConditionalDiscount discount = new ConditionalDiscount(
                "Expired Early Bird",
                1L,
                LocalDateTime.now().minusDays(3),
                LocalDateTime.now().minusDays(1),
                BigDecimal.valueOf(15),
                Condition.DATE,
                null
        );

        BigDecimal result = discount.calculateDiscount(
                BigDecimal.valueOf(200),
                1,
                null
        );

        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }
}