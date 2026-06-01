package ticketsystem.ApplicationLayer;

import java.util.List;

import ticketsystem.DTO.PurchasePolicyDTO;
import ticketsystem.DTO.PurchaseRuleDTO;
import ticketsystem.DTO.PurchaseRuleType;
import ticketsystem.DomainLayer.policy.*;


public class PurchasePolicyMapper {

    public PurchasePolicy toDomain(PurchasePolicyDTO dto) {
        if (dto == null || dto.getRootRule() == null) {
            return PurchasePolicy.noRestrictions();
        }

        return new PurchasePolicy(toRule(dto.getRootRule()));
    }

    private PurchaseRule toRule(PurchaseRuleDTO dto) {
        if (dto == null || dto.getType() == null) {
            throw new IllegalArgumentException("Purchase rule type is required");
        }

        switch (dto.getType()) {
            case ALWAYS_ALLOW:
                return new AlwaysAllowRule();

            case MIN_AGE:
                return new MinAgeRule(requireValue(dto, "Minimum age is required"));

            case MIN_TICKETS:
                return new MinTicketsRule(requireValue(dto, "Minimum tickets is required"));

            case MAX_TICKETS:
                return new MaxTicketsRule(requireValue(dto, "Maximum tickets is required"));

            case AND:
                return new AndPurchaseRule(toChildrenRules(dto));

            case OR:
                return new OrPurchaseRule(toChildrenRules(dto));

            default:
                throw new IllegalArgumentException("Unsupported purchase rule type");
        }
    }

    private int requireValue(PurchaseRuleDTO dto, String message) {
        if (dto.getValue() == null) {
            throw new IllegalArgumentException(message);
        }
        return dto.getValue();
    }

    private List<PurchaseRule> toChildrenRules(PurchaseRuleDTO dto) {
        if (dto.getChildren() == null || dto.getChildren().isEmpty()) {
            throw new IllegalArgumentException("Composite rule must have children");
        }

        return dto.getChildren()
                .stream()
                .map(this::toRule)
                .toList();
    }

    // Converts the Domain PurchasePolicy back to a DTO
    public PurchasePolicyDTO toDTO(PurchasePolicy policy) {
        if (policy == null || policy.getRootRule() == null) {
            return new PurchasePolicyDTO(); 
        }

        return new PurchasePolicyDTO(ruleToDTO(policy.getRootRule()));
    }

    // Recursively converts a Domain PurchaseRule to a PurchaseRuleDTO
    private PurchaseRuleDTO ruleToDTO(PurchaseRule rule) {
        if (rule == null) {
            return null;
        }

        PurchaseRuleDTO dto = new PurchaseRuleDTO();

        // Check the specific instance type of the rule and map accordingly
        if (rule instanceof AlwaysAllowRule) {
            dto.setType(PurchaseRuleType.ALWAYS_ALLOW);
            dto.setValue(0);
            
        } else if (rule instanceof MinAgeRule minAgeRule) {
            dto.setType(PurchaseRuleType.MIN_AGE);
            dto.setValue(minAgeRule.getMinAge()); 
            
        } else if (rule instanceof MinTicketsRule minTicketsRule) {
            dto.setType(PurchaseRuleType.MIN_TICKETS);
            dto.setValue(minTicketsRule.getMinTickets());
            
        } else if (rule instanceof MaxTicketsRule maxTicketsRule) {
            dto.setType(PurchaseRuleType.MAX_TICKETS);
            dto.setValue(maxTicketsRule.getMaxTickets());
            
        } else if (rule instanceof AndPurchaseRule andRule) {
            dto.setType(PurchaseRuleType.AND);
            dto.setChildren(andRule.getRules().stream().map(this::ruleToDTO).toList());
            
        } else if (rule instanceof OrPurchaseRule orRule) {
            dto.setType(PurchaseRuleType.OR);
            dto.setChildren(orRule.getRules().stream().map(this::ruleToDTO).toList());
            
        } else {
            throw new IllegalArgumentException("Unsupported purchase rule type encountered during mapping");
        }

        return dto;
    }
}