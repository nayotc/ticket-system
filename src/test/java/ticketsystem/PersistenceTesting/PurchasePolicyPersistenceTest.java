package ticketsystem.PersistenceTesting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import ticketsystem.DomainLayer.policy.AndPurchaseRule;
import ticketsystem.DomainLayer.policy.CompositePurchaseRule;
import ticketsystem.DomainLayer.policy.MaxTicketsRule;
import ticketsystem.DomainLayer.policy.MinAgeRule;
import ticketsystem.DomainLayer.policy.MinTicketsRule;
import ticketsystem.DomainLayer.policy.OrPurchaseRule;
import ticketsystem.DomainLayer.policy.PurchasePolicy;
import ticketsystem.DomainLayer.policy.PurchaseRule;

@DataJpaTest
class PurchasePolicyPersistenceTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void GivenNestedPurchasePolicy_WhenPersistedAndReloaded_ThenCompleteRuleTreeIsRestored() {
        PurchasePolicy policy = createNestedPolicy();

        entityManager.persistAndFlush(policy);

        Long policyId = policy.getId();
        assertNotNull(policyId);

        entityManager.clear();

        PurchasePolicy loadedPolicy =
                entityManager.find(PurchasePolicy.class, policyId);

        assertNotNull(loadedPolicy);
        assertInstanceOf(
                AndPurchaseRule.class,
                loadedPolicy.getRootRule()
        );

        AndPurchaseRule rootRule =
                (AndPurchaseRule) loadedPolicy.getRootRule();

        assertEquals(2, rootRule.getRules().size());
        assertInstanceOf(
                MinAgeRule.class,
                rootRule.getRules().get(0)
        );
        assertInstanceOf(
                OrPurchaseRule.class,
                rootRule.getRules().get(1)
        );

        entityManager.clear();

        assertTrue(loadedPolicy.validate(2, 18).isAllowed());
        assertFalse(loadedPolicy.validate(3, 18).isAllowed());
        assertTrue(loadedPolicy.validate(100, 18).isAllowed());
        assertFalse(loadedPolicy.validate(2, 17).isAllowed());
    }

    @Test
    void GivenPersistedPurchasePolicy_WhenDeleted_ThenCompleteRuleTreeIsDeleted() {
        PurchasePolicy policy = createNestedPolicy();

        entityManager.persistAndFlush(policy);

        Long policyId = policy.getId();
        List<Long> ruleIds = collectRuleIds(policy.getRootRule());

        assertNotNull(policyId);
        assertEquals(5, ruleIds.size());

        entityManager.remove(policy);
        entityManager.flush();
        entityManager.clear();

        assertNull(entityManager.find(PurchasePolicy.class, policyId));

        for (Long ruleId : ruleIds) {
            assertNull(entityManager.find(PurchaseRule.class, ruleId));
        }
    }

    private PurchasePolicy createNestedPolicy() {
        return new PurchasePolicy(
                new AndPurchaseRule(
                        List.of(
                                new MinAgeRule(18),
                                new OrPurchaseRule(
                                        List.of(
                                                new MaxTicketsRule(2),
                                                new MinTicketsRule(100)
                                        )
                                )
                        )
                )
        );
    }

    private List<Long> collectRuleIds(PurchaseRule rule) {
        List<Long> ids = new ArrayList<>();
        ids.add(rule.getId());

        if (rule instanceof CompositePurchaseRule compositeRule) {
            for (PurchaseRule child : compositeRule.getRules()) {
                ids.addAll(collectRuleIds(child));
            }
        }

        return ids;
    }
}
