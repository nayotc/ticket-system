package ticketsystem.UnitTesting;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import ticketsystem.ApplicationLayer.PurchasePolicyValidator;
import ticketsystem.DTO.PurchasePolicyDTO;
import ticketsystem.DTO.PurchaseRuleDTO;
import ticketsystem.DTO.PurchaseRuleType;

class PurchasePolicyValidatorTest {

    @Test
    void GivenMinTicketsGreaterThanMaxTicketsUnderAnd_WhenValidate_ThenThrows() {
        PurchasePolicyDTO policy = new PurchasePolicyDTO(
                new PurchaseRuleDTO(
                        PurchaseRuleType.AND,
                        0,
                        List.of(
                                new PurchaseRuleDTO(PurchaseRuleType.MIN_TICKETS, 10, null),
                                new PurchaseRuleDTO(PurchaseRuleType.MAX_TICKETS, 5, null)
                        )
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> PurchasePolicyValidator.validate(policy)
        );
    }

    @Test
    void GivenValidMinAndMaxTicketsUnderAnd_WhenValidate_ThenPasses() {
        PurchasePolicyDTO policy = new PurchasePolicyDTO(
                new PurchaseRuleDTO(
                        PurchaseRuleType.AND,
                        0,
                        List.of(
                                new PurchaseRuleDTO(PurchaseRuleType.MIN_TICKETS, 2, null),
                                new PurchaseRuleDTO(PurchaseRuleType.MAX_TICKETS, 8, null)
                        )
                )
        );

        assertDoesNotThrow(() -> PurchasePolicyValidator.validate(policy));
    }

    @Test
    void GivenConflictingTicketsInsideOrBranch_WhenValidate_ThenPasses() {
        PurchasePolicyDTO policy = new PurchasePolicyDTO(
                new PurchaseRuleDTO(
                        PurchaseRuleType.AND,
                        0,
                        List.of(
                                new PurchaseRuleDTO(PurchaseRuleType.MIN_AGE, 18, null),
                                new PurchaseRuleDTO(
                                        PurchaseRuleType.OR,
                                        0,
                                        List.of(
                                                new PurchaseRuleDTO(PurchaseRuleType.MAX_TICKETS, 2, null),
                                                new PurchaseRuleDTO(PurchaseRuleType.MIN_TICKETS, 100, null)
                                        )
                                )
                        )
                )
        );

        assertDoesNotThrow(() -> PurchasePolicyValidator.validate(policy));
    }
}
