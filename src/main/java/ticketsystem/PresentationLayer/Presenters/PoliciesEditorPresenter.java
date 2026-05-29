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
import ticketsystem.PresentationLayer.Views.Management.PoliciesEditor.PurchasePolicyDraftDTO;
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

    public PoliciesDraftData loadPolicies(String sessionToken, Long companyId) {
        try {
            // 1. Fetch and Map Purchase Policy
            PurchasePolicyDTO appPurchasePolicy = companyService.getCompanyPurchasePolicy(sessionToken, companyId);
            PurchasePolicyDraftDTO uiPurchaseDraft = mapToUiPurchasePolicy(companyId.toString(), appPurchasePolicy);
            
            // 2. Fetch and Map Discount Policy
            DiscountPolicyDTO appDiscountPolicy = companyService.getCompanyDiscountPolicy(sessionToken, companyId);
            DiscountPolicyDraftDTO uiDiscountDraft = mapToUiDiscountPolicy(companyId.toString(), appDiscountPolicy);

            return new PoliciesDraftData(uiPurchaseDraft, uiDiscountDraft);

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(e.getMessage());
        } catch (Exception e) {
            throw new PresentationException("An error occurred while loading policy data.");
        }
    }

    public void savePolicies(String sessionToken, Long companyId, PurchasePolicyDraftDTO purchaseDraft, DiscountPolicyDraftDTO discountDraft) {
        try {
            // 1. Save Purchase Policy
            if (purchaseDraft != null && !purchaseDraft.rules().isEmpty()) {
                PurchasePolicyDTO appPurchasePolicy = mapToAppPurchasePolicy(purchaseDraft);
                companyService.setCompanyPurchasePolicy(sessionToken, companyId, appPurchasePolicy);
            }

            // 2. Save Discount Policy
            if (discountDraft != null) {
                DiscountPolicyDTO appDiscountPolicy = mapToAppDiscountPolicy(discountDraft);
                companyService.setCompanyDiscountPolicy(sessionToken, companyId, appDiscountPolicy);
            }

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(e.getMessage());
        } catch (Exception e) {
            throw new PresentationException("An error occurred while saving the policies. Please try again.");
        }
    }

    // =================================================================================
    // MAPPING METHODS: APPLICATION TO UI (FLATTENING / READING)
    // =================================================================================

    /**
     * Converts the Application PurchasePolicyDTO tree into a flat PurchasePolicyDraftDTO for the UI.
     */
    private PurchasePolicyDraftDTO mapToUiPurchasePolicy(String companyId, PurchasePolicyDTO appPolicy) {
        if (appPolicy == null || appPolicy.getRootRule() == null) {
            return new PurchasePolicyDraftDTO(companyId, LogicalOperator.AND, new ArrayList<>());
        }

        PurchaseRuleDTO root = appPolicy.getRootRule();
        LogicalOperator rootOperator = LogicalOperator.AND;
        List<ticketsystem.PresentationLayer.Views.Management.PoliciesEditor.PurchaseRuleDTO> uiRules = new ArrayList<>();

        if (root.getType() == PurchaseRuleType.AND || root.getType() == PurchaseRuleType.OR) {
            rootOperator = root.getType() == PurchaseRuleType.AND ? LogicalOperator.AND : LogicalOperator.OR;
            
            if (root.getChildren() != null) {
                for (PurchaseRuleDTO child : root.getChildren()) {
                    uiRules.add(mapToUiLeafRule(child));
                }
            }
        } else if (root.getType() != PurchaseRuleType.ALWAYS_ALLOW) {
            uiRules.add(mapToUiLeafRule(root));
        }

        return new PurchasePolicyDraftDTO(companyId, rootOperator, uiRules);
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

        java.time.LocalDate validUntil = appDiscount.getEndTime() != null ? appDiscount.getEndTime().toLocalDate() : null;

        return new ticketsystem.PresentationLayer.Views.Management.PoliciesEditor.DiscountDTO(
                UUID.randomUUID().toString(),
                appDiscount.getName(),
                type,
                ticketsystem.PresentationLayer.Views.Management.PoliciesEditor.DiscountValueType.PERCENTAGE, 
                appDiscount.getPercentage() != null ? appDiscount.getPercentage().doubleValue() : 0.0,
                appDiscount.getCouponCode() != null ? appDiscount.getCouponCode() : "",
                validUntil,
                appDiscount.getConditionText() != null ? appDiscount.getConditionText() : ""
        );
    }

    // =================================================================================
    // MAPPING METHODS: UI TO APPLICATION (BUILDING / WRITING)
    // =================================================================================

    /**
     * Converts the flat UI Purchase Policy Draft into the Application's PurchasePolicyDTO tree.
     */
    private PurchasePolicyDTO mapToAppPurchasePolicy(PurchasePolicyDraftDTO draft) {
        List<PurchaseRuleDTO> childRules = draft.rules().stream()
                .map(this::mapToAppLeafRule)
                .collect(Collectors.toList());

        PurchaseRuleDTO rootRule = new PurchaseRuleDTO();
        
        if (draft.rootOperator() == LogicalOperator.AND) {
            rootRule.setType(PurchaseRuleType.AND);
        } else {
            rootRule.setType(PurchaseRuleType.OR);
        }
        
        rootRule.setChildren(childRules);
        rootRule.setValue(0); 

        return new PurchasePolicyDTO(rootRule);
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
        
        if (uiDiscount.validUntil() != null) {
            appDiscount.setEndTime(uiDiscount.validUntil().atStartOfDay());
        }

        switch (uiDiscount.type()) {
            case SIMPLE:
                appDiscount.setType("VISIBLE"); 
                break;
            case COUPON:
                appDiscount.setType("COUPON");
                appDiscount.setCouponCode(uiDiscount.couponCode());
                break;
            case CONDITIONAL:
                appDiscount.setType("CONDITIONAL");
                appDiscount.setConditionText(uiDiscount.conditionText()); 
                break;
            default:
                throw new PresentationException("Unknown discount type selected.");
        }

        return appDiscount;
    }
}