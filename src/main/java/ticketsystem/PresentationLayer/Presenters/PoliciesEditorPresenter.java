package ticketsystem.PresentationLayer.Presenters;

import org.springframework.stereotype.Component;
import ticketsystem.ApplicationLayer.CompanyService;
import ticketsystem.DTO.DiscountPolicyDTO;
import ticketsystem.DTO.DiscountDTO;
import ticketsystem.DTO.PurchasePolicyDTO;
import ticketsystem.DTO.PurchaseRuleDTO;
import ticketsystem.DTO.PurchaseRuleType;

import ticketsystem.PresentationLayer.Views.Management.PoliciesEditor.DiscountPolicyDraftDTO;
import ticketsystem.PresentationLayer.Views.Management.PoliciesEditor.DiscountCompositionStrategy;
import ticketsystem.PresentationLayer.Views.Management.PoliciesEditor.DiscountConditionType;
import ticketsystem.PresentationLayer.Views.Management.PoliciesEditor.PurchasePolicyDraftDTO;
import ticketsystem.PresentationLayer.Views.Management.PoliciesEditor.PurchasePolicyExpressionDraftDTO;
import ticketsystem.PresentationLayer.Views.Management.PoliciesEditor.PurchaseExpressionNodeDTO;
import ticketsystem.PresentationLayer.Views.Management.PoliciesEditor.PurchaseNodeType;
import ticketsystem.PresentationLayer.Views.Management.PoliciesEditor.LogicalOperator;
import ticketsystem.PresentationLayer.Views.Management.PoliciesEditor.PurchaseRuleField;
import ticketsystem.PresentationLayer.Views.Management.PoliciesEditor.ComparisonOperator;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class PoliciesEditorPresenter {

    private final CompanyService companyService;

    public PoliciesEditorPresenter(CompanyService companyService) {
        this.companyService = companyService;
    }

    // A container record to return both policy drafts to the UI
    public record PoliciesDraftData(PurchasePolicyDraftDTO purchaseDraft, DiscountPolicyDraftDTO discountDraft) {}

    public PurchasePolicyExpressionDraftDTO loadPurchasePolicy(String sessionToken, Long companyId) {
        try {
            PurchasePolicyDTO appPurchasePolicy = companyService.getCompanyPurchasePolicy(sessionToken, companyId);
            return mapToUiPurchasePolicyExpression(companyId.toString(), appPurchasePolicy);

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(e.getMessage());
        } catch (Exception e) {
            throw new PresentationException("An error occurred while loading the purchase policy.");
        }
    }

    public void savePurchasePolicy(String sessionToken, Long companyId, PurchasePolicyExpressionDraftDTO purchaseDraft) {
        try {
            PurchasePolicyDTO appPurchasePolicy = mapToAppPurchasePolicyExpression(purchaseDraft);
            companyService.setCompanyPurchasePolicy(sessionToken, companyId, appPurchasePolicy);

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(e.getMessage());
        } catch (Exception e) {
            throw new PresentationException("An error occurred while saving the purchase policy. Please try again.");
        }
    }

    public DiscountPolicyDraftDTO loadDiscountPolicy(String sessionToken, Long companyId) {
        try {
            DiscountPolicyDTO appDiscountPolicy = companyService.getCompanyDiscountPolicy(sessionToken, companyId);
            return mapToUiDiscountPolicy(companyId.toString(), appDiscountPolicy);

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(e.getMessage());
        } catch (Exception e) {
            throw new PresentationException("An error occurred while loading the discount policy.");
        }
    }

    public void saveDiscountPolicy(String sessionToken, Long companyId, DiscountPolicyDraftDTO discountDraft) {
        try {
            DiscountPolicyDTO appDiscountPolicy = mapToAppDiscountPolicy(discountDraft);
            companyService.setCompanyDiscountPolicy(sessionToken, companyId, appDiscountPolicy);

            if (appDiscountPolicy.getCompositionType() != null) {
                companyService.setCompositionType(sessionToken, companyId, appDiscountPolicy.getCompositionType());
            }

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(e.getMessage());
        } catch (Exception e) {
            throw new PresentationException("An error occurred while saving the discount policy. Please try again.");
        }
    }


    // =================================================================================
    // MAPPING METHODS: APPLICATION TO UI (FLATTENING / READING)
    // =================================================================================

    /**
     * Converts the Application PurchasePolicyDTO tree into the nested UI expression model.
     */
    private PurchasePolicyExpressionDraftDTO mapToUiPurchasePolicyExpression(String companyId, PurchasePolicyDTO appPolicy) {
        if (appPolicy == null || appPolicy.getRootRule() == null) {
            PurchaseExpressionNodeDTO root = new PurchaseExpressionNodeDTO(
                    UUID.randomUUID().toString(),
                    PurchaseNodeType.GROUP,
                    LogicalOperator.AND,
                    null,
                    new ArrayList<>()
            );
            return new PurchasePolicyExpressionDraftDTO(companyId, root);
        }

        return new PurchasePolicyExpressionDraftDTO(companyId, mapToUiPurchaseNode(appPolicy.getRootRule()));
    }

    private PurchaseExpressionNodeDTO mapToUiPurchaseNode(PurchaseRuleDTO appRule) {
        if (appRule == null || appRule.getType() == null || appRule.getType() == PurchaseRuleType.ALWAYS_ALLOW) {
            return new PurchaseExpressionNodeDTO(
                    UUID.randomUUID().toString(),
                    PurchaseNodeType.GROUP,
                    LogicalOperator.AND,
                    null,
                    new ArrayList<>()
            );
        }

        if (appRule.getType() == PurchaseRuleType.AND || appRule.getType() == PurchaseRuleType.OR) {
            List<PurchaseExpressionNodeDTO> children = new ArrayList<>();
            if (appRule.getChildren() != null) {
                for (PurchaseRuleDTO child : appRule.getChildren()) {
                    children.add(mapToUiPurchaseNode(child));
                }
            }

            return new PurchaseExpressionNodeDTO(
                    UUID.randomUUID().toString(),
                    PurchaseNodeType.GROUP,
                    appRule.getType() == PurchaseRuleType.AND ? LogicalOperator.AND : LogicalOperator.OR,
                    null,
                    children
            );
        }

        return new PurchaseExpressionNodeDTO(
                UUID.randomUUID().toString(),
                PurchaseNodeType.RULE,
                null,
                mapToUiLeafRule(appRule),
                new ArrayList<>()
        );
    }


    /**
     * Converts a single Application leaf rule into a UI PurchaseRuleDTO.
     */
    private ticketsystem.PresentationLayer.Views.Management.PoliciesEditor.PurchaseRuleDTO mapToUiLeafRule(PurchaseRuleDTO appRule) {
        PurchaseRuleField field;
        ComparisonOperator operator;
        String unit = "";

        switch (appRule.getType()) {
            case MIN_AGE:
                field = PurchaseRuleField.AGE;
                operator = ComparisonOperator.GREATER_OR_EQUALS;
                unit = "שנים";
                break;
            case MIN_TICKETS:
                field = PurchaseRuleField.MIN_TICKETS;
                operator = ComparisonOperator.GREATER_OR_EQUALS;
                unit = "לרוכש";
                break;
            case MAX_TICKETS:
                field = PurchaseRuleField.MAX_TICKETS;
                operator = ComparisonOperator.LESS_OR_EQUALS;
                unit = "לרוכש";
                break;
            default:
                field = PurchaseRuleField.MAX_TICKETS;
                operator = ComparisonOperator.EQUALS;
        }

        return new ticketsystem.PresentationLayer.Views.Management.PoliciesEditor.PurchaseRuleDTO(
                UUID.randomUUID().toString(),
                field,
                operator,
                appRule.getValue() != null ? appRule.getValue() : 0,
                unit
        );
    }

    /**
     * Converts the Application DiscountPolicyDTO to a DiscountPolicyDraftDTO for the UI.
     */
    private DiscountPolicyDraftDTO mapToUiDiscountPolicy(String companyId, DiscountPolicyDTO appPolicy) {
        if (appPolicy == null) {
            return new DiscountPolicyDraftDTO(companyId, DiscountCompositionStrategy.MAXIMUM, new ArrayList<>());
        }

        DiscountCompositionStrategy strategy = DiscountCompositionStrategy.MAXIMUM;
        if (appPolicy.getCompositionType() == ticketsystem.DomainLayer.discount.DiscountCompositionType.SUM) {
            strategy = DiscountCompositionStrategy.SUM;
        }

        List<ticketsystem.PresentationLayer.Views.Management.PoliciesEditor.DiscountDTO> uiDiscounts = new ArrayList<>();
        if (appPolicy.getDiscounts() != null) {
            for (DiscountDTO appDiscount : appPolicy.getDiscounts()) {
                uiDiscounts.add(mapToUiDiscount(appDiscount));
            }
        }

        return new DiscountPolicyDraftDTO(companyId, strategy, uiDiscounts);
    }

    /**
     * Converts an Application DiscountDTO into a UI DiscountDTO.
     */
    private ticketsystem.PresentationLayer.Views.Management.PoliciesEditor.DiscountDTO mapToUiDiscount(DiscountDTO appDiscount) {
        ticketsystem.PresentationLayer.Views.Management.PoliciesEditor.DiscountType type =
                ticketsystem.PresentationLayer.Views.Management.PoliciesEditor.DiscountType.SIMPLE;

        if ("COUPON".equals(appDiscount.getType())) {
            type = ticketsystem.PresentationLayer.Views.Management.PoliciesEditor.DiscountType.COUPON;
        } else if ("CONDITIONAL".equals(appDiscount.getType())) {
            type = ticketsystem.PresentationLayer.Views.Management.PoliciesEditor.DiscountType.CONDITIONAL;
        }

        java.time.LocalDate validUntil = appDiscount.getEndTime() != null
                ? appDiscount.getEndTime().toLocalDate()
                : null;

        String conditionText = appDiscount.getConditionText() == null
                ? ""
                : appDiscount.getConditionText().trim();

        DiscountConditionType conditionType = resolveDiscountConditionType(conditionText);
        Integer ticketThreshold = resolveTicketThreshold(conditionText);

        java.time.LocalDateTime endTime = appDiscount.getEndTime();
        java.time.LocalDateTime startTime = null;

        return new ticketsystem.PresentationLayer.Views.Management.PoliciesEditor.DiscountDTO(
                UUID.randomUUID().toString(),
                appDiscount.getName(),
                type,
                ticketsystem.PresentationLayer.Views.Management.PoliciesEditor.DiscountValueType.PERCENTAGE,
                appDiscount.getPercentage() != null ? appDiscount.getPercentage().doubleValue() : 0.0,
                appDiscount.getCouponCode() != null ? appDiscount.getCouponCode() : "",
                validUntil,
                type == ticketsystem.PresentationLayer.Views.Management.PoliciesEditor.DiscountType.CONDITIONAL ? conditionType : null,
                type == ticketsystem.PresentationLayer.Views.Management.PoliciesEditor.DiscountType.CONDITIONAL ? ticketThreshold : null,
                type == ticketsystem.PresentationLayer.Views.Management.PoliciesEditor.DiscountType.CONDITIONAL ? startTime : null,
                type == ticketsystem.PresentationLayer.Views.Management.PoliciesEditor.DiscountType.CONDITIONAL ? endTime : null
        );
    }

    private DiscountConditionType resolveDiscountConditionType(String conditionText) {
        if (conditionText == null || conditionText.isBlank()) {
            return DiscountConditionType.MIN_TICKET;
        }

        if (conditionText.contains("<=") || conditionText.contains("MAX_TICKET")) {
            return DiscountConditionType.MAX_TICKET;
        }

        if (conditionText.contains("DATE") || conditionText.contains("תאריך") || conditionText.contains("פעיל")) {
            return DiscountConditionType.DATE;
        }

        return DiscountConditionType.MIN_TICKET;
    }

    private Integer resolveTicketThreshold(String conditionText) {
        if (conditionText == null || conditionText.isBlank()) {
            return null;
        }

        String digits = conditionText.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return null;
        }

        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    // =================================================================================
    // MAPPING METHODS: UI TO APPLICATION (BUILDING / WRITING)
    // =================================================================================

    /**
     * Converts the nested UI Purchase Policy Draft into the Application PurchasePolicyDTO tree.
     */
    private PurchasePolicyDTO mapToAppPurchasePolicyExpression(PurchasePolicyExpressionDraftDTO draft) {
        if (draft == null || draft.root() == null) {
            return new PurchasePolicyDTO(alwaysAllowRule());
        }

        return new PurchasePolicyDTO(mapToAppPurchaseNode(draft.root()));
    }

    private PurchaseRuleDTO mapToAppPurchaseNode(PurchaseExpressionNodeDTO node) {
        if (node == null) {
            return alwaysAllowRule();
        }

        if (node.type() == PurchaseNodeType.RULE) {
            if (node.rule() == null) {
                return alwaysAllowRule();
            }
            return mapToAppLeafRule(node.rule());
        }

        List<PurchaseRuleDTO> children = new ArrayList<>();
        if (node.children() != null) {
            for (PurchaseExpressionNodeDTO child : node.children()) {
                PurchaseRuleDTO mappedChild = mapToAppPurchaseNode(child);
                if (mappedChild.getType() != PurchaseRuleType.ALWAYS_ALLOW) {
                    children.add(mappedChild);
                }
            }
        }

        if (children.isEmpty()) {
            return alwaysAllowRule();
        }

        PurchaseRuleDTO appRule = new PurchaseRuleDTO();
        appRule.setType(node.operator() == LogicalOperator.OR ? PurchaseRuleType.OR : PurchaseRuleType.AND);
        appRule.setChildren(children);
        appRule.setValue(0);
        return appRule;
    }

    private PurchaseRuleDTO alwaysAllowRule() {
        PurchaseRuleDTO rule = new PurchaseRuleDTO();
        rule.setType(PurchaseRuleType.ALWAYS_ALLOW);
        rule.setValue(0);
        rule.setChildren(null);
        return rule;
    }


    /**
     * Converts a single UI rule into an Application leaf rule node.
     */
    private PurchaseRuleDTO mapToAppLeafRule(ticketsystem.PresentationLayer.Views.Management.PoliciesEditor.PurchaseRuleDTO uiRule) {
        PurchaseRuleDTO appRule = new PurchaseRuleDTO();
        appRule.setValue(uiRule.value());
        appRule.setChildren(null);

        switch (uiRule.field()) {
            case MIN_TICKETS:
                appRule.setType(PurchaseRuleType.MIN_TICKETS);
                break;
            case MAX_TICKETS:
                appRule.setType(PurchaseRuleType.MAX_TICKETS);
                break;
            case AGE:
                appRule.setType(PurchaseRuleType.MIN_AGE);
                break;
            default:
                throw new PresentationException("Unknown purchase rule field selected.");
        }

        return appRule;
    }

    /**
     * Converts the UI Discount Policy Draft into the Application's DiscountPolicyDTO.
     */
    private DiscountPolicyDTO mapToAppDiscountPolicy(DiscountPolicyDraftDTO draft) {
        DiscountPolicyDTO policyDTO = new DiscountPolicyDTO();

        if (draft.compositionStrategy() == DiscountCompositionStrategy.SUM) {
            policyDTO.setCompositionType(ticketsystem.DomainLayer.discount.DiscountCompositionType.SUM);
        } else {
            policyDTO.setCompositionType(ticketsystem.DomainLayer.discount.DiscountCompositionType.MAX);
        }

        List<DiscountDTO> appDiscounts = new ArrayList<>();
        if (draft.discounts() != null) {
            for (ticketsystem.PresentationLayer.Views.Management.PoliciesEditor.DiscountDTO uiDiscount : draft.discounts()) {
                appDiscounts.add(mapToAppDiscount(uiDiscount));
            }
        }
        policyDTO.setDiscounts(appDiscounts);

        return policyDTO;
    }

    /**
     * Converts a single UI discount into an Application DiscountDTO.
     */
    private DiscountDTO mapToAppDiscount(ticketsystem.PresentationLayer.Views.Management.PoliciesEditor.DiscountDTO uiDiscount) {
        DiscountDTO appDiscount = new DiscountDTO();

        appDiscount.setName(uiDiscount.name());
        appDiscount.setPercentage(java.math.BigDecimal.valueOf(uiDiscount.value()));

        switch (uiDiscount.type()) {
            case SIMPLE:
                appDiscount.setType("VISIBLE");
                break;
            case COUPON:
                appDiscount.setType("COUPON");
                appDiscount.setCouponCode(uiDiscount.couponCode());
                if (uiDiscount.validUntil() != null) {
                    appDiscount.setEndTime(uiDiscount.validUntil().atTime(23, 59));
                }
                break;
            case CONDITIONAL:
                appDiscount.setType("CONDITIONAL");
                appDiscount.setConditionText(uiDiscount.conditionText());
                if (uiDiscount.endTime() != null) {
                    appDiscount.setEndTime(uiDiscount.endTime());
                }
                break;
            default:
                throw new PresentationException("Unknown discount type selected.");
        }

        return appDiscount;
    }
}