package ticketsystem.UnitTesting;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import ticketsystem.ApplicationLayer.PurchasePolicyMapper;
import ticketsystem.DTO.PurchasePolicyDTO;
import ticketsystem.DTO.PurchaseRuleDTO;
import ticketsystem.DTO.PurchaseRuleType;
import ticketsystem.DomainLayer.policy.PolicyResult;
import ticketsystem.DomainLayer.policy.PurchasePolicy;

class PurchasePolicyMapperTest {

    private final PurchasePolicyMapper mapper = new PurchasePolicyMapper();

    @Test
    void GivenNullPolicyDTO_WhenMapToDomain_ThenReturnNoRestrictionsPolicy() {
        PurchasePolicy policy = mapper.toDomain(null);

        PolicyResult result = policy.validate(100, 0);

        assertTrue(result.isAllowed());
    }

    @Test
    void GivenPolicyDTOWithNullRootRule_WhenMapToDomain_ThenReturnNoRestrictionsPolicy() {
        PurchasePolicyDTO dto = new PurchasePolicyDTO(null);

        PurchasePolicy policy = mapper.toDomain(dto);

        PolicyResult result = policy.validate(100, 0);

        assertTrue(result.isAllowed());
    }

    @Test
    void GivenRuleDTOWithNullType_WhenMapToDomain_ThenThrowException() {
        PurchaseRuleDTO rootRule = new PurchaseRuleDTO(null, null, null);
        PurchasePolicyDTO dto = new PurchasePolicyDTO(rootRule);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> mapper.toDomain(dto)
        );

        assertEquals("Purchase rule type is required", exception.getMessage());
    }

    @Test
    void GivenAlwaysAllowRuleDTO_WhenMapToDomain_ThenPolicyAllowsPurchase() {
        PurchaseRuleDTO rootRule = new PurchaseRuleDTO(
                PurchaseRuleType.ALWAYS_ALLOW,
                null,
                null
        );

        PurchasePolicy policy = mapper.toDomain(new PurchasePolicyDTO(rootRule));

        PolicyResult result = policy.validate(100, 0);

        assertTrue(result.isAllowed());
    }

    @Test
    void GivenMinAgeRuleDTOWithoutValue_WhenMapToDomain_ThenThrowException() {
        PurchaseRuleDTO rootRule = new PurchaseRuleDTO(
                PurchaseRuleType.MIN_AGE,
                null,
                null
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> mapper.toDomain(new PurchasePolicyDTO(rootRule))
        );

        assertEquals("Minimum age is required", exception.getMessage());
    }

    @Test
    void GivenMinAgeRuleDTO_WhenMapToDomain_ThenPolicyUsesMinAgeRule() {
        PurchaseRuleDTO rootRule = new PurchaseRuleDTO(
                PurchaseRuleType.MIN_AGE,
                18,
                null
        );

        PurchasePolicy policy = mapper.toDomain(new PurchasePolicyDTO(rootRule));

        assertTrue(policy.validate(1, 18).isAllowed());
        assertFalse(policy.validate(1, 17).isAllowed());
    }

    @Test
    void GivenMinTicketsRuleDTOWithoutValue_WhenMapToDomain_ThenThrowException() {
        PurchaseRuleDTO rootRule = new PurchaseRuleDTO(
                PurchaseRuleType.MIN_TICKETS,
                null,
                null
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> mapper.toDomain(new PurchasePolicyDTO(rootRule))
        );

        assertEquals("Minimum tickets is required", exception.getMessage());
    }

    @Test
    void GivenMinTicketsRuleDTO_WhenMapToDomain_ThenPolicyUsesMinTicketsRule() {
        PurchaseRuleDTO rootRule = new PurchaseRuleDTO(
                PurchaseRuleType.MIN_TICKETS,
                2,
                null
        );

        PurchasePolicy policy = mapper.toDomain(new PurchasePolicyDTO(rootRule));

        assertTrue(policy.validate(2, 20).isAllowed());
        assertFalse(policy.validate(1, 20).isAllowed());
    }

    @Test
    void GivenMaxTicketsRuleDTOWithoutValue_WhenMapToDomain_ThenThrowException() {
        PurchaseRuleDTO rootRule = new PurchaseRuleDTO(
                PurchaseRuleType.MAX_TICKETS,
                null,
                null
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> mapper.toDomain(new PurchasePolicyDTO(rootRule))
        );

        assertEquals("Maximum tickets is required", exception.getMessage());
    }

    @Test
    void GivenMaxTicketsRuleDTO_WhenMapToDomain_ThenPolicyUsesMaxTicketsRule() {
        PurchaseRuleDTO rootRule = new PurchaseRuleDTO(
                PurchaseRuleType.MAX_TICKETS,
                5,
                null
        );

        PurchasePolicy policy = mapper.toDomain(new PurchasePolicyDTO(rootRule));

        assertTrue(policy.validate(5, 20).isAllowed());
        assertFalse(policy.validate(6, 20).isAllowed());
    }

    @Test
    void GivenAndRuleDTOWithoutChildren_WhenMapToDomain_ThenThrowException() {
        PurchaseRuleDTO rootRule = new PurchaseRuleDTO(
                PurchaseRuleType.AND,
                null,
                null
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> mapper.toDomain(new PurchasePolicyDTO(rootRule))
        );

        assertEquals("Composite rule must have children", exception.getMessage());
    }

    @Test
    void GivenAndRuleDTO_WhenMapToDomain_ThenAllChildRulesMustPass() {
        PurchaseRuleDTO rootRule = new PurchaseRuleDTO(
                PurchaseRuleType.AND,
                null,
                List.of(
                        new PurchaseRuleDTO(PurchaseRuleType.MIN_AGE, 18, null),
                        new PurchaseRuleDTO(PurchaseRuleType.MAX_TICKETS, 5, null)
                )
        );

        PurchasePolicy policy = mapper.toDomain(new PurchasePolicyDTO(rootRule));

        assertTrue(policy.validate(5, 18).isAllowed());
        assertFalse(policy.validate(6, 18).isAllowed());
        assertFalse(policy.validate(5, 17).isAllowed());
    }

    @Test
    void GivenOrRuleDTOWithoutChildren_WhenMapToDomain_ThenThrowException() {
        PurchaseRuleDTO rootRule = new PurchaseRuleDTO(
                PurchaseRuleType.OR,
                null,
                List.of()
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> mapper.toDomain(new PurchasePolicyDTO(rootRule))
        );

        assertEquals("Composite rule must have children", exception.getMessage());
    }

    @Test
    void GivenOrRuleDTO_WhenMapToDomain_ThenAtLeastOneChildRuleMustPass() {
        PurchaseRuleDTO rootRule = new PurchaseRuleDTO(
                PurchaseRuleType.OR,
                null,
                List.of(
                        new PurchaseRuleDTO(PurchaseRuleType.MAX_TICKETS, 2, null),
                        new PurchaseRuleDTO(PurchaseRuleType.MIN_TICKETS, 100, null)
                )
        );

        PurchasePolicy policy = mapper.toDomain(new PurchasePolicyDTO(rootRule));

        assertTrue(policy.validate(2, 20).isAllowed());
        assertTrue(policy.validate(100, 20).isAllowed());
        assertFalse(policy.validate(50, 20).isAllowed());
    }

    @Test
    void GivenNestedRuleDTO_WhenMapToDomain_ThenPolicySupportsNestedRules() {
        PurchaseRuleDTO rootRule = new PurchaseRuleDTO(
                PurchaseRuleType.AND,
                null,
                List.of(
                        new PurchaseRuleDTO(PurchaseRuleType.MIN_AGE, 18, null),
                        new PurchaseRuleDTO(
                                PurchaseRuleType.OR,
                                null,
                                List.of(
                                        new PurchaseRuleDTO(PurchaseRuleType.MAX_TICKETS, 2, null),
                                        new PurchaseRuleDTO(PurchaseRuleType.MIN_TICKETS, 100, null)
                                )
                        )
                )
        );

        PurchasePolicy policy = mapper.toDomain(new PurchasePolicyDTO(rootRule));

        assertTrue(policy.validate(2, 18).isAllowed());
        assertTrue(policy.validate(100, 18).isAllowed());
        assertFalse(policy.validate(50, 18).isAllowed());
        assertFalse(policy.validate(2, 17).isAllowed());
    }
}
