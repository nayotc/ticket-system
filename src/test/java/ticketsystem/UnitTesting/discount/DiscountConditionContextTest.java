package ticketsystem.UnitTesting.discount;

import org.junit.jupiter.api.Test;

import ticketsystem.DomainLayer.discount.DiscountConditionContext;

import static org.junit.jupiter.api.Assertions.*;

class DiscountConditionContextTest {

    @Test
    void GivenTicketCount_WhenCreateContext_ThenStoreTicketCount() {
        DiscountConditionContext context = new DiscountConditionContext(4);

        assertEquals(4, context.getTicketCount());
    }

    @Test
    void GivenContext_WhenCreateContext_ThenCurrentTimeIsNotNull() {
        DiscountConditionContext context = new DiscountConditionContext(4);

        assertNotNull(context.getCurrentTime());
    }
}