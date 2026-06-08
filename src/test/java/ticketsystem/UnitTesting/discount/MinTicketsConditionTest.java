package ticketsystem.UnitTesting.discount;

import org.junit.jupiter.api.Test;

import ticketsystem.DomainLayer.discount.DiscountConditionContext;
import ticketsystem.DomainLayer.discount.MinTicketsCondition;

import static org.junit.jupiter.api.Assertions.*;

class MinTicketsConditionTest {

    @Test
    void GivenTicketCountEqualToMinimum_WhenIsSatisfied_ThenReturnTrue() {
        MinTicketsCondition condition = new MinTicketsCondition(3);

        assertTrue(condition.isSatisfied(new DiscountConditionContext(3)));
    }

    @Test
    void GivenTicketCountGreaterThanMinimum_WhenIsSatisfied_ThenReturnTrue() {
        MinTicketsCondition condition = new MinTicketsCondition(3);

        assertTrue(condition.isSatisfied(new DiscountConditionContext(5)));
    }

    @Test
    void GivenTicketCountLessThanMinimum_WhenIsSatisfied_ThenReturnFalse() {
        MinTicketsCondition condition = new MinTicketsCondition(3);

        assertFalse(condition.isSatisfied(new DiscountConditionContext(2)));
    }

    @Test
    void GivenZeroMinimum_WhenCreateCondition_ThenThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> new MinTicketsCondition(0));
    }

    @Test
    void GivenNegativeMinimum_WhenCreateCondition_ThenThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> new MinTicketsCondition(-1));
    }
}