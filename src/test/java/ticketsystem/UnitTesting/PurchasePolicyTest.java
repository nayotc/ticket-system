package ticketsystem.UnitTesting;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import ticketsystem.DomainLayer.policy.*;

class PurchasePolicyTest {

    @Test
    void GivenNoRestrictionsPolicy_WhenValidateAnyPurchase_ThenAllowed() {
        PurchasePolicy policy = PurchasePolicy.noRestrictions();

        PolicyResult result = policy.validate(100, 0);

        assertTrue(result.isAllowed());
        assertNull(result.getMessage());
    }

    @Test
    void GivenAllowedRootRule_WhenValidatePolicy_ThenAllowed() {
        PurchasePolicy policy = new PurchasePolicy(new AlwaysAllowRule());

        PolicyResult result = policy.validate(1, 20);

        assertTrue(result.isAllowed());
    }

    @Test
    void GivenDeniedRootRule_WhenValidatePolicy_ThenDenied() {
        PurchasePolicy policy = new PurchasePolicy(new MaxTicketsRule(5));

        PolicyResult result = policy.validate(6, 20);

        assertFalse(result.isAllowed());
        assertEquals(
                "Cannot purchase more than 5 tickets.",
                result.getMessage()
        );
    }

    @Test
    void GivenPolicyCreatedWithRootRule_WhenGetRootRule_ThenReturnSameRule() {
        PurchaseRule rootRule = new MaxTicketsRule(5);

        PurchasePolicy policy = new PurchasePolicy(rootRule);

        assertSame(rootRule, policy.getRootRule());
    }
}