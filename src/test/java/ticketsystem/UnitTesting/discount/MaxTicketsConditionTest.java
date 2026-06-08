package ticketsystem.UnitTesting.discount;

import org.junit.jupiter.api.Test;

import ticketsystem.DomainLayer.discount.DiscountConditionContext;
import ticketsystem.DomainLayer.discount.MaxTicketsCondition;

import static org.junit.jupiter.api.Assertions.*;

class MaxTicketsConditionTest {

    @Test
    void GivenTicketCountEqualToMaximum_WhenIsSatisfied_ThenReturnTrue() {
        MaxTicketsCondition condition = new MaxTicketsCondition(5);

        assertTrue(condition.isSatisfied(new DiscountConditionContext(5)));
    }

    @Test
    void GivenTicketCountLessThanMaximum_WhenIsSatisfied_ThenReturnTrue() {
        MaxTicketsCondition condition = new MaxTicketsCondition(5);

        assertTrue(condition.isSatisfied(new DiscountConditionContext(3)));
    }

    @Test
    void GivenTicketCountGreaterThanMaximum_WhenIsSatisfied_ThenReturnFalse() {
        MaxTicketsCondition condition = new MaxTicketsCondition(5);

        assertFalse(condition.isSatisfied(new DiscountConditionContext(6)));
    }

    @Test
    void GivenZeroMaximum_WhenCreateCondition_ThenThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> new MaxTicketsCondition(0));
    }

    @Test
    void GivenNegativeMaximum_WhenCreateCondition_ThenThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> new MaxTicketsCondition(-1));
    }
}