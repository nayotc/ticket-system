package ticketsystem.ApplicationLayer;

import ticketsystem.DTO.PurchasePolicyDTO;
import ticketsystem.DTO.PurchaseRuleDTO;
import ticketsystem.DTO.PurchaseRuleType;

public final class PurchasePolicyValidator {

    private PurchasePolicyValidator() {
    }

    public static void validate(PurchasePolicyDTO policy) {
        if (policy == null || policy.getRootRule() == null) {
            return;
        }

        validateRule(policy.getRootRule());
    }

    private static void validateRule(PurchaseRuleDTO rule) {
        if (rule == null || rule.getType() == null || rule.getType() == PurchaseRuleType.ALWAYS_ALLOW) {
            return;
        }

        switch (rule.getType()) {
            case MIN_AGE -> validateMinAge(rule);
            case MIN_TICKETS -> validateMinTickets(rule);
            case MAX_TICKETS -> validateMaxTickets(rule);
            case AND -> validateAndRule(rule);
            case OR -> validateOrRule(rule);
            default -> throw new IllegalArgumentException("Unsupported purchase rule type: " + rule.getType());
        }
    }

    private static void validateAndRule(PurchaseRuleDTO andRule) {
        if (andRule.getChildren() == null || andRule.getChildren().isEmpty()) {
            throw new IllegalArgumentException("AND purchase rule must contain at least one child rule.");
        }

        for (PurchaseRuleDTO child : andRule.getChildren()) {
            validateRule(child);
        }

        TicketBounds bounds = collectTicketBoundsUnderAnd(andRule);
        if (bounds.minTickets != null
                && bounds.maxTickets != null
                && bounds.minTickets > bounds.maxTickets) {
            throw new IllegalArgumentException(
                    "Purchase policy is invalid: minimum tickets ("
                            + bounds.minTickets
                            + ") cannot be greater than maximum tickets ("
                            + bounds.maxTickets
                            + ")."
            );
        }
    }

    private static void validateOrRule(PurchaseRuleDTO orRule) {
        if (orRule.getChildren() == null || orRule.getChildren().isEmpty()) {
            throw new IllegalArgumentException("OR purchase rule must contain at least one child rule.");
        }

        for (PurchaseRuleDTO child : orRule.getChildren()) {
            validateRule(child);
        }
    }

    private static TicketBounds collectTicketBoundsUnderAnd(PurchaseRuleDTO andRule) {
        TicketBounds bounds = new TicketBounds();

        if (andRule.getChildren() != null) {
            for (PurchaseRuleDTO child : andRule.getChildren()) {
                collectTicketBoundsFromAndSubtree(child, bounds);
            }
        }

        return bounds;
    }

    private static void collectTicketBoundsFromAndSubtree(PurchaseRuleDTO rule, TicketBounds bounds) {
        if (rule == null || rule.getType() == null) {
            return;
        }

        switch (rule.getType()) {
            case MIN_TICKETS -> {
                int minTickets = requireNonNegative(rule.getValue(), "Minimum tickets");
                bounds.minTickets = bounds.minTickets == null
                        ? minTickets
                        : Math.max(bounds.minTickets, minTickets);
            }
            case MAX_TICKETS -> {
                int maxTickets = requirePositive(rule.getValue(), "Maximum tickets");
                bounds.maxTickets = bounds.maxTickets == null
                        ? maxTickets
                        : Math.min(bounds.maxTickets, maxTickets);
            }
            case AND -> {
                if (rule.getChildren() != null) {
                    for (PurchaseRuleDTO child : rule.getChildren()) {
                        collectTicketBoundsFromAndSubtree(child, bounds);
                    }
                }
            }
            case OR, MIN_AGE, ALWAYS_ALLOW -> {
                // OR branches are evaluated independently; do not merge them into parent AND bounds.
            }
            default -> throw new IllegalArgumentException("Unsupported purchase rule type: " + rule.getType());
        }
    }

    private static void validateMinAge(PurchaseRuleDTO rule) {
        requireValue(rule.getValue(), "Minimum age is required");
        if (rule.getValue() < 0) {
            throw new IllegalArgumentException("Minimum age cannot be negative.");
        }
    }

    private static void validateMinTickets(PurchaseRuleDTO rule) {
        requireValue(rule.getValue(), "Minimum tickets is required");
        if (rule.getValue() < 0) {
            throw new IllegalArgumentException("Minimum tickets cannot be negative.");
        }
    }

    private static void validateMaxTickets(PurchaseRuleDTO rule) {
        requireValue(rule.getValue(), "Maximum tickets is required");
        if (rule.getValue() <= 0) {
            throw new IllegalArgumentException("Maximum tickets must be greater than zero.");
        }
    }

    private static int requireValue(Integer value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static int requireNonNegative(Integer value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " cannot be negative.");
        }
        return value;
    }

    private static int requirePositive(Integer value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero.");
        }
        return value;
    }

    private static final class TicketBounds {
        private Integer minTickets;
        private Integer maxTickets;
    }
}
