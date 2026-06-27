package ticketsystem.UnitTesting;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import ticketsystem.ApplicationLayer.DiscountPolicyValidator;
import ticketsystem.DTO.DiscountConditionDTO;
import ticketsystem.DTO.DiscountDTO;
import ticketsystem.DTO.DiscountPolicyDTO;

class DiscountPolicyValidatorTest {

    @Test
    void GivenMinTicketGreaterThanMaxTicket_WhenValidate_ThenThrows() {
        DiscountPolicyDTO policy = new DiscountPolicyDTO();
        DiscountDTO discount = new DiscountDTO();
        discount.setName("Family pack");
        discount.setType("CONDITIONAL");
        discount.setConditions(List.of(
                new DiscountConditionDTO("MIN_TICKET", 10, null, null),
                new DiscountConditionDTO("MAX_TICKET", 5, null, null)
        ));
        policy.setDiscounts(List.of(discount));

        assertThrows(
                IllegalArgumentException.class,
                () -> DiscountPolicyValidator.validate(policy)
        );
    }

    @Test
    void GivenStartDateAfterEndDate_WhenValidate_ThenThrows() {
        DiscountPolicyDTO policy = new DiscountPolicyDTO();
        DiscountDTO discount = new DiscountDTO();
        discount.setName("Early bird");
        discount.setType("CONDITIONAL");
        discount.setConditions(List.of(
                new DiscountConditionDTO(
                        "DATE",
                        null,
                        LocalDateTime.of(2026, 6, 10, 0, 0),
                        LocalDateTime.of(2026, 6, 1, 23, 59)
                )
        ));
        policy.setDiscounts(List.of(discount));

        assertThrows(
                IllegalArgumentException.class,
                () -> DiscountPolicyValidator.validate(policy)
        );
    }

    @Test
    void GivenValidConditionalDiscount_WhenValidate_ThenPasses() {
        DiscountPolicyDTO policy = new DiscountPolicyDTO();
        DiscountDTO discount = new DiscountDTO();
        discount.setName("Bundle");
        discount.setType("CONDITIONAL");
        discount.setConditions(List.of(
                new DiscountConditionDTO("MIN_TICKET", 2, null, null),
                new DiscountConditionDTO("MAX_TICKET", 8, null, null),
                new DiscountConditionDTO(
                        "DATE",
                        null,
                        LocalDateTime.of(2026, 6, 1, 0, 0),
                        LocalDateTime.of(2026, 6, 30, 23, 59)
                )
        ));
        policy.setDiscounts(List.of(discount));

        assertDoesNotThrow(() -> DiscountPolicyValidator.validate(policy));
    }
}
