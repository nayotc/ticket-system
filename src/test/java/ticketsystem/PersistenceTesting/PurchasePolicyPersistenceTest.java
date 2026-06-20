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
import java.math.BigDecimal;
import java.time.LocalDateTime;

import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.event.EventCategory;
import ticketsystem.DomainLayer.event.EventLocation;
import ticketsystem.DomainLayer.event.Pair;

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

    @Test
    void GivenEventWithPurchasePolicy_WhenPolicyIsReplaced_ThenOldPolicyAndRuleTreeAreDeleted() {
        Event event = createEvent();
        PurchasePolicy oldPolicy = createNestedPolicy();

        event.setPurchasePolicy(oldPolicy);
        entityManager.persistAndFlush(event);

        Long eventId = event.getId();
        Long oldPolicyId = oldPolicy.getId();
        List<Long> oldRuleIds = collectRuleIds(oldPolicy.getRootRule());

        assertNotNull(eventId);
        assertNotNull(oldPolicyId);
        assertEquals(5, oldRuleIds.size());
        assertTrue(oldRuleIds.stream().allMatch(id -> id != null));

        entityManager.clear();

        Event managedEvent = entityManager.find(Event.class, eventId);

        assertNotNull(managedEvent);

        PurchasePolicy replacementPolicy =
                new PurchasePolicy(new MinAgeRule(21));

        managedEvent.setPurchasePolicy(replacementPolicy);

        entityManager.flush();

        Long replacementPolicyId = replacementPolicy.getId();
        assertNotNull(replacementPolicyId);

        entityManager.clear();

        Event reloadedEvent = entityManager.find(Event.class, eventId);

        assertNotNull(reloadedEvent);
        assertNotNull(reloadedEvent.getPurchasePolicy());
        assertEquals(
                replacementPolicyId,
                reloadedEvent.getPurchasePolicy().getId()
        );
        assertTrue(
                reloadedEvent.getPurchasePolicy()
                        .validate(1, 21)
                        .isAllowed()
        );
        assertFalse(
                reloadedEvent.getPurchasePolicy()
                        .validate(1, 20)
                        .isAllowed()
        );

        assertNull(
                entityManager.find(
                        PurchasePolicy.class,
                        oldPolicyId
                )
        );

        for (Long ruleId : oldRuleIds) {
            assertNull(
                    entityManager.find(
                            PurchaseRule.class,
                            ruleId
                    )
            );
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

    private Event createEvent() {
        return new Event(
                LocalDateTime.now().plusDays(30),
                "Purchase Policy Owner Event",
                1L,
                10L,
                EventLocation.TEL_AVIV,
                100L,
                EventCategory.CONCERT,
                "Test Artist",
                new BigDecimal("100.00"),
                new Pair<>(20, 15)
        );
    }
}
