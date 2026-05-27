package ticketsystem.UnitTesting;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import ticketsystem.DomainLayer.policy.*;
class BasicPurchaseRulesTest {

    @Test
    void GivenAlwaysAllowRule_WhenValidateAnyPurchase_ThenAllowed() {
        AlwaysAllowRule rule = new AlwaysAllowRule();

        PolicyResult result = rule.isValid(100, 0);

        assertTrue(result.isAllowed());
        assertNull(result.getMessage());
    }

    @Test
    void GivenAgeEqualsMinimum_WhenValidateMinAgeRule_ThenAllowed() {
        MinAgeRule rule = new MinAgeRule(18);

        PolicyResult result = rule.isValid(1, 18);

        assertTrue(result.isAllowed());
    }

    @Test
    void GivenAgeAboveMinimum_WhenValidateMinAgeRule_ThenAllowed() {
        MinAgeRule rule = new MinAgeRule(18);

        PolicyResult result = rule.isValid(1, 25);

        assertTrue(result.isAllowed());
    }

    @Test
    void GivenAgeBelowMinimum_WhenValidateMinAgeRule_ThenDenied() {
        MinAgeRule rule = new MinAgeRule(18);

        PolicyResult result = rule.isValid(1, 17);

        assertFalse(result.isAllowed());
        assertEquals(
                "Customer does not meet the minimum age requirement of 18",
                result.getMessage()
        );
    }

    @Test
    void GivenNegativeMinimumAge_WhenCreateMinAgeRule_ThenThrowException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new MinAgeRule(-1)
        );
    }

    @Test
    void GivenQuantityEqualsMinimum_WhenValidateMinTicketsRule_ThenAllowed() {
        MinTicketsRule rule = new MinTicketsRule(2);

        PolicyResult result = rule.isValid(2, 20);

        assertTrue(result.isAllowed());
    }

    @Test
    void GivenQuantityAboveMinimum_WhenValidateMinTicketsRule_ThenAllowed() {
        MinTicketsRule rule = new MinTicketsRule(2);

        PolicyResult result = rule.isValid(5, 20);

        assertTrue(result.isAllowed());
    }

    @Test
    void GivenQuantityBelowMinimum_WhenValidateMinTicketsRule_ThenDenied() {
        MinTicketsRule rule = new MinTicketsRule(2);

        PolicyResult result = rule.isValid(1, 20);

        assertFalse(result.isAllowed());
        assertEquals(
                "Insufficient tickets purchased, minimum required: 2",
                result.getMessage()
        );
    }

    @Test
    void GivenNegativeMinimumTickets_WhenCreateMinTicketsRule_ThenThrowException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new MinTicketsRule(-1)
        );
    }

    @Test
    void GivenQuantityBelowMaximum_WhenValidateMaxTicketsRule_ThenAllowed() {
        MaxTicketsRule rule = new MaxTicketsRule(5);

        PolicyResult result = rule.isValid(3, 20);

        assertTrue(result.isAllowed());
    }

    @Test
    void GivenQuantityEqualsMaximum_WhenValidateMaxTicketsRule_ThenAllowed() {
        MaxTicketsRule rule = new MaxTicketsRule(5);

        PolicyResult result = rule.isValid(5, 20);

        assertTrue(result.isAllowed());
    }

    @Test
    void GivenQuantityAboveMaximum_WhenValidateMaxTicketsRule_ThenDenied() {
        MaxTicketsRule rule = new MaxTicketsRule(5);

        PolicyResult result = rule.isValid(6, 20);

        assertFalse(result.isAllowed());
        assertEquals(
                "Cannot purchase more than 5 tickets.",
                result.getMessage()
        );
    }
}
