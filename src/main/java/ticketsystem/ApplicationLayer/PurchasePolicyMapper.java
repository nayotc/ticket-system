package ticketsystem.ApplicationLayer;

import java.util.List;

import ticketsystem.DTO.PurchasePolicyDTO;
import ticketsystem.DTO.PurchaseRuleDTO;
import ticketsystem.DomainLayer.policy.*;
import ticketsystem.DomainLayer.policy.PurchaseRule;


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
}