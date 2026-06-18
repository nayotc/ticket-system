package ticketsystem.PersistenceTesting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.hibernate.Hibernate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import ticketsystem.DomainLayer.discount.AndDiscountCondition;
import ticketsystem.DomainLayer.discount.ConditionalDiscount;
import ticketsystem.DomainLayer.discount.CouponDiscount;
import ticketsystem.DomainLayer.discount.DateRangeCondition;
import ticketsystem.DomainLayer.discount.DiscountCompositionType;
import ticketsystem.DomainLayer.discount.DiscountCondition;
import ticketsystem.DomainLayer.discount.DiscountPolicy;
import ticketsystem.DomainLayer.discount.DiscountTypes;
import ticketsystem.DomainLayer.discount.MaxTicketsCondition;
import ticketsystem.DomainLayer.discount.MinTicketsCondition;
import ticketsystem.DomainLayer.discount.VisibleDiscount;

@DataJpaTest
public class DiscountPolicyPersistenceTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void GivenCompleteDiscountPolicy_WhenPersistedAndReloaded_ThenStructureAndBehaviorArePreserved() {
        DiscountPolicy policy = createDiscountPolicy();

        entityManager.persistAndFlush(policy);

        Long policyId = policy.getId();
        assertNotNull(policyId);

        entityManager.clear();

        DiscountPolicy loadedPolicy = entityManager.find(DiscountPolicy.class, policyId);

        assertNotNull(loadedPolicy);
        assertEquals(DiscountCompositionType.SUM, loadedPolicy.getDiscountCompositionType());
        assertEquals(3, loadedPolicy.getDiscounts().size());

        assertInstanceOf(VisibleDiscount.class, loadedPolicy.getDiscounts().get(0));
        assertInstanceOf(ConditionalDiscount.class, loadedPolicy.getDiscounts().get(1));
        assertInstanceOf(CouponDiscount.class, loadedPolicy.getDiscounts().get(2));

        ConditionalDiscount conditionalDiscount =
                (ConditionalDiscount) loadedPolicy.getDiscounts().get(1);

        DiscountCondition loadedCondition = conditionalDiscount.getCondition();

        assertEquals(AndDiscountCondition.class, Hibernate.getClass(loadedCondition));

        AndDiscountCondition andCondition =
                (AndDiscountCondition) Hibernate.unproxy(loadedCondition);

        List<DiscountCondition> loadedConditions = andCondition.getConditions();

        assertEquals(3, loadedConditions.size());
        assertInstanceOf(MinTicketsCondition.class, loadedConditions.get(0));
        assertInstanceOf(MaxTicketsCondition.class, loadedConditions.get(1));
        assertInstanceOf(DateRangeCondition.class, loadedConditions.get(2));

        BigDecimal result = loadedPolicy.calculateDiscount(
                new BigDecimal("100.00"),
                3,
                "SAVE30"
        );

        assertEquals(0, new BigDecimal("49.6000").compareTo(result));
    }

    @Test
    void GivenPersistedDiscountPolicy_WhenDeleted_ThenDiscountsAndConditionsAreDeleted() {
        DiscountPolicy policy = createDiscountPolicy();

        entityManager.persistAndFlush(policy);

        Long policyId = policy.getId();
        List<Long> discountIds =
                collectDiscountIds(policy);

        List<Long> conditionIds =
                collectConditionIds(policy);

        assertEquals(3, discountIds.size());
        assertEquals(4, conditionIds.size());

        entityManager.remove(policy);
        entityManager.flush();
        entityManager.clear();

        assertNull(
                entityManager.find(
                        DiscountPolicy.class,
                        policyId
                )
        );

        for (Long discountId : discountIds) {
            assertNull(
                    entityManager.find(
                            DiscountTypes.class,
                            discountId
                    )
            );
        }

        for (Long conditionId : conditionIds) {
            assertNull(
                    entityManager.find(
                            DiscountCondition.class,
                            conditionId
                    )
            );
        }
    }

    private DiscountPolicy createDiscountPolicy() {
        LocalDateTime now = LocalDateTime.now();

        DiscountPolicy policy =
                new DiscountPolicy(
                        DiscountCompositionType.SUM
                );

        policy.addDiscount(
                new VisibleDiscount(
                        "Visible discount",
                        new BigDecimal("10")
                )
        );

        policy.addDiscount(
                new ConditionalDiscount(
                        "Conditional discount",
                        new BigDecimal("20"),
                        new AndDiscountCondition(
                                List.of(
                                        new MinTicketsCondition(2),
                                        new MaxTicketsCondition(4),
                                        new DateRangeCondition(
                                                now.minusDays(1),
                                                now.plusDays(1)
                                        )
                                )
                        )
                )
        );

        policy.addDiscount(
                new CouponDiscount(
                        "Coupon discount",
                        "SAVE30",
                        new BigDecimal("30"),
                        now.plusDays(2)
                )
        );

        return policy;
    }

    private List<Long> collectDiscountIds(
            DiscountPolicy policy
    ) {
        return policy.getDiscounts()
                .stream()
                .map(DiscountTypes::getId)
                .toList();
    }

    private List<Long> collectConditionIds(
            DiscountPolicy policy
    ) {
        List<Long> ids = new ArrayList<>();

        for (DiscountTypes discount :
                policy.getDiscounts()) {
            if (discount instanceof
                    ConditionalDiscount conditional) {
                collectConditionIds(
                        conditional.getCondition(),
                        ids
                );
            }
        }

        return ids;
    }

    private void collectConditionIds(
            DiscountCondition condition,
            List<Long> ids
    ) {
        ids.add(condition.getId());

        if (condition instanceof
                AndDiscountCondition andCondition) {
            for (DiscountCondition child :
                    andCondition.getConditions()) {
                collectConditionIds(child, ids);
            }
        }
    }
}