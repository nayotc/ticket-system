package ticketsystem.UnitTesting;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import ticketsystem.DomainLayer.policy.*;

class CompositePurchaseRulesTest {

    @Test
    void GivenNullRules_WhenCreateAndRule_ThenThrowException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AndPurchaseRule(null)
        );
    }

    @Test
    void GivenEmptyRules_WhenCreateAndRule_ThenThrowException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AndPurchaseRule(List.of())
        );
    }

    @Test
    void GivenNullRules_WhenCreateOrRule_ThenThrowException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new OrPurchaseRule(null)
        );
    }

    @Test
    void GivenEmptyRules_WhenCreateOrRule_ThenThrowException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new OrPurchaseRule(List.of())
        );
    }


    @Test
    void GivenAllRulesAllowed_WhenValidateAndRule_ThenAllowed() {
        AndPurchaseRule rule = new AndPurchaseRule(List.of(
                new MinAgeRule(18),
                new MinTicketsRule(2),
                new MaxTicketsRule(5)
        ));

        PolicyResult result = rule.isValid(3, 20);

        assertTrue(result.isAllowed());
    }

    @Test
    void GivenOneRuleDenied_WhenValidateAndRule_ThenDeniedWithOriginalMessage() {
        AndPurchaseRule rule = new AndPurchaseRule(List.of(
                new MinAgeRule(18),
                new MaxTicketsRule(5)
        ));

        PolicyResult result = rule.isValid(6, 20);

        assertFalse(result.isAllowed());
        assertEquals(
                "Cannot purchase more than 5 tickets.",
                result.getMessage()
        );
    }

    @Test
    void GivenFirstRuleAllowed_WhenValidateOrRule_ThenAllowed() {
        OrPurchaseRule rule = new OrPurchaseRule(List.of(
                new MaxTicketsRule(2),
                new MinTicketsRule(100)
        ));

        PolicyResult result = rule.isValid(2, 20);

        assertTrue(result.isAllowed());
    }

    @Test
    void GivenSecondRuleAllowed_WhenValidateOrRule_ThenAllowed() {
        OrPurchaseRule rule = new OrPurchaseRule(List.of(
                new MaxTicketsRule(2),
                new MinTicketsRule(100)
        ));

        PolicyResult result = rule.isValid(100, 20);

        assertTrue(result.isAllowed());
    }

    @Test
    void GivenAllRulesDenied_WhenValidateOrRule_ThenDeniedWithGenericMessage() {
        OrPurchaseRule rule = new OrPurchaseRule(List.of(
                new MaxTicketsRule(2),
                new MinTicketsRule(100)
        ));

        PolicyResult result = rule.isValid(50, 20);

        assertFalse(result.isAllowed());
        assertEquals(
                "יש לעמוד לפחות באחד מתנאי מדיניות הרכישה",
                result.getMessage()
        );
    }

    @Test
    void GivenNestedCompositeRule_WhenPurchaseMatchesFirstOrCondition_ThenAllowed() {
        PurchaseRule rule = new AndPurchaseRule(List.of(
                new MinAgeRule(18),
                new OrPurchaseRule(List.of(
                        new MaxTicketsRule(2),
                        new MinTicketsRule(100)
                ))
        ));

        PolicyResult result = rule.isValid(2, 20);

        assertTrue(result.isAllowed());
    }

    @Test
    void GivenNestedCompositeRule_WhenPurchaseMatchesSecondOrCondition_ThenAllowed() {
        PurchaseRule rule = new AndPurchaseRule(List.of(
                new MinAgeRule(18),
                new OrPurchaseRule(List.of(
                        new MaxTicketsRule(2),
                        new MinTicketsRule(100)
                ))
        ));

        PolicyResult result = rule.isValid(100, 20);

        assertTrue(result.isAllowed());
    }

    @Test
    void GivenNestedCompositeRule_WhenPurchaseMatchesNoOrCondition_ThenDenied() {
        PurchaseRule rule = new AndPurchaseRule(List.of(
                new MinAgeRule(18),
                new OrPurchaseRule(List.of(
                        new MaxTicketsRule(2),
                        new MinTicketsRule(100)
                ))
        ));

        PolicyResult result = rule.isValid(50, 20);

        assertFalse(result.isAllowed());
        assertEquals(
                "יש לעמוד לפחות באחד מתנאי מדיניות הרכישה",
                result.getMessage()
        );
    }
}