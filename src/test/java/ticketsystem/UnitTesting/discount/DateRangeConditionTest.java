package ticketsystem.UnitTesting.discount;

import org.junit.jupiter.api.Test;

import ticketsystem.DomainLayer.discount.DateRangeCondition;
import ticketsystem.DomainLayer.discount.DiscountConditionContext;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DateRangeConditionTest {

    @Test
    void GivenCurrentTimeInsideRange_WhenIsSatisfied_ThenReturnTrue() {
        DateRangeCondition condition =
                new DateRangeCondition(
                        LocalDateTime.now().minusMinutes(1),
                        LocalDateTime.now().plusMinutes(1)
                );

        assertTrue(condition.isSatisfied(new DiscountConditionContext(1)));
    }

    @Test
    void GivenCurrentTimeBeforeRange_WhenIsSatisfied_ThenReturnFalse() {
        DateRangeCondition condition =
                new DateRangeCondition(
                        LocalDateTime.now().plusMinutes(1),
                        LocalDateTime.now().plusMinutes(2)
                );

        assertFalse(condition.isSatisfied(new DiscountConditionContext(1)));
    }

    @Test
    void GivenCurrentTimeAfterRange_WhenIsSatisfied_ThenReturnFalse() {
        DateRangeCondition condition =
                new DateRangeCondition(
                        LocalDateTime.now().minusMinutes(2),
                        LocalDateTime.now().minusMinutes(1)
                );

        assertFalse(condition.isSatisfied(new DiscountConditionContext(1)));
    }

    @Test
    void GivenNullStartTime_WhenCreateCondition_ThenThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> new DateRangeCondition(null, LocalDateTime.now()));
    }

    @Test
    void GivenNullEndTime_WhenCreateCondition_ThenThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> new DateRangeCondition(LocalDateTime.now(), null));
    }

    @Test
    void GivenEndTimeBeforeStartTime_WhenCreateCondition_ThenThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> new DateRangeCondition(
                        LocalDateTime.now().plusDays(1),
                        LocalDateTime.now()
                ));
    }
}