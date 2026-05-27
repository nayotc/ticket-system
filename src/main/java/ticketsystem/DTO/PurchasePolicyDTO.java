package ticketsystem.DTO;

public class PurchasePolicyDTO {

    private PurchaseRuleDTO rootRule;

    public PurchasePolicyDTO() {
    }

    public PurchasePolicyDTO(PurchaseRuleDTO rootRule) {
        this.rootRule = rootRule;
    }

    public PurchaseRuleDTO getRootRule() {
        return rootRule;
    }

    public void setRootRule(PurchaseRuleDTO rootRule) {
        this.rootRule = rootRule;
    }
}