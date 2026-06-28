package ticketsystem.ApplicationLayer;

import java.util.List;

import ticketsystem.DTO.DiscountConditionDTO;
import ticketsystem.DTO.DiscountDTO;
import ticketsystem.DTO.DiscountPolicyDTO;

public final class DiscountPolicyValidator {

    private DiscountPolicyValidator() {
    }

    public static void validate(DiscountPolicyDTO policy) {
        if (policy == null || policy.getDiscounts() == null) {
            return;
        }

        for (DiscountDTO discount : policy.getDiscounts()) {
            validateDiscount(discount);
        }
    }

    private static void validateDiscount(DiscountDTO discount) {
        if (discount == null || discount.getType() == null) {
            return;
        }

        if (!"CONDITIONAL".equalsIgnoreCase(discount.getType().trim())) {
            return;
        }

        validateConditions(discount.getName(), discount.getConditions());
    }

    private static void validateConditions(String discountName, List<DiscountConditionDTO> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return;
        }

        Integer minTickets = null;
        Integer maxTickets = null;
        String label = discountName == null || discountName.isBlank() ? "Conditional discount" : discountName;

        for (DiscountConditionDTO condition : conditions) {
            if (condition == null || condition.getType() == null) {
                continue;
            }

            String type = condition.getType().trim().toUpperCase();

            if (isMinTicketCondition(type)) {
                int threshold = requirePositive(condition.getTicketThreshold(), "Minimum ticket condition threshold");
                minTickets = minTickets == null ? threshold : Math.max(minTickets, threshold);
            } else if (isMaxTicketCondition(type)) {
                int threshold = requirePositive(condition.getTicketThreshold(), "Maximum ticket condition threshold");
                maxTickets = maxTickets == null ? threshold : Math.min(maxTickets, threshold);
            } else if (isDateCondition(type)) {
                validateDateRange(label, condition);
            }
        }

        if (minTickets != null && maxTickets != null && minTickets > maxTickets) {
            throw new IllegalArgumentException(
                    "Discount policy is invalid for '"
                            + label
                            + "': minimum tickets ("
                            + minTickets
                            + ") cannot be greater than maximum tickets ("
                            + maxTickets
                            + ")."
            );
        }
    }

    private static void validateDateRange(String discountName, DiscountConditionDTO condition) {
        if (condition.getStartTime() == null || condition.getEndTime() == null) {
            return;
        }

        if (condition.getStartTime().isAfter(condition.getEndTime())) {
            throw new IllegalArgumentException(
                    "Discount policy is invalid for '"
                            + discountName
                            + "': condition start date cannot be after end date."
            );
        }
    }

    private static boolean isMinTicketCondition(String type) {
        return type.contains("MIN_TICKET");
    }

    private static boolean isMaxTicketCondition(String type) {
        return type.contains("MAX_TICKET");
    }

    private static boolean isDateCondition(String type) {
        return type.contains("DATE");
    }

    private static int requirePositive(Integer value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero.");
        }
        return value;
    }
}
