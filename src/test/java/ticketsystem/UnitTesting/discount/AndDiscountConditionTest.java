package ticketsystem.UnitTesting.discount;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import ticketsystem.DomainLayer.discount.AndDiscountCondition;
import ticketsystem.DomainLayer.discount.DiscountCondition;
import ticketsystem.DomainLayer.discount.DiscountConditionContext;

class AndDiscountConditionTest {

    @Test
    void GivenNullConditions_WhenCreateAndDiscountCondition_ThenThrowException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AndDiscountCondition(null)
        );
    }

    @Test
    void GivenEmptyConditions_WhenCreateAndDiscountCondition_ThenThrowException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AndDiscountCondition(List.of())
        );
    }

    @Test
    void GivenAllConditionsSatisfied_WhenIsSatisfied_ThenReturnTrue() {
        FakeDiscountCondition condition1 = new FakeDiscountCondition(true);
        FakeDiscountCondition condition2 = new FakeDiscountCondition(true);

        AndDiscountCondition andCondition =
                new AndDiscountCondition(List.of(condition1, condition2));

        assertTrue(andCondition.isSatisfied(null));
    }

    @Test
    void GivenOneConditionNotSatisfied_WhenIsSatisfied_ThenReturnFalse() {
        FakeDiscountCondition condition1 = new FakeDiscountCondition(true);
        FakeDiscountCondition condition2 = new FakeDiscountCondition(false);

        AndDiscountCondition andCondition =
                new AndDiscountCondition(List.of(condition1, condition2));

        assertFalse(andCondition.isSatisfied(null));
    }

    @Test
    void GivenFirstConditionNotSatisfied_WhenIsSatisfied_ThenStopCheckingNextConditions() {
        FakeDiscountCondition condition1 = new FakeDiscountCondition(false);
        FakeDiscountCondition condition2 = new FakeDiscountCondition(true);

        AndDiscountCondition andCondition =
                new AndDiscountCondition(List.of(condition1, condition2));

        assertFalse(andCondition.isSatisfied(null));

        assertEquals(1, condition1.getCallCount());
        assertEquals(0, condition2.getCallCount());
    }

    @Test
    void GivenReturnedConditionsListChanged_WhenGetConditions_ThenOriginalConditionListNotChanged() {
        FakeDiscountCondition condition = new FakeDiscountCondition(true);

        AndDiscountCondition andCondition =
                new AndDiscountCondition(List.of(condition));

        List<DiscountCondition> returnedConditions = andCondition.getConditions();
        returnedConditions.clear();

        assertEquals(1, andCondition.getConditions().size());
    }

    @Test
    void GivenOriginalListChangedAfterCreation_WhenGetConditions_ThenInternalListNotChanged() {
        FakeDiscountCondition condition1 = new FakeDiscountCondition(true);
        FakeDiscountCondition condition2 = new FakeDiscountCondition(true);

        List<DiscountCondition> originalList = new ArrayList<>();
        originalList.add(condition1);

        AndDiscountCondition andCondition =
                new AndDiscountCondition(originalList);

        originalList.add(condition2);

        assertEquals(1, andCondition.getConditions().size());
    }

    private static class FakeDiscountCondition implements DiscountCondition {
        private final boolean satisfied;
        private int callCount = 0;

        private FakeDiscountCondition(boolean satisfied) {
            this.satisfied = satisfied;
        }

        @Override
        public boolean isSatisfied(DiscountConditionContext context) {
            callCount++;
            return satisfied;
        }

        private int getCallCount() {
            return callCount;
        }
    }
}