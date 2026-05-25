package ticketsystem.DTO;

import java.util.List;

public class PurchaseRuleDTO {

    private PurchaseRuleType type;
    private Integer value;
    private List<PurchaseRuleDTO> children;

    public PurchaseRuleDTO() {
    }

    public PurchaseRuleDTO(PurchaseRuleType type, Integer value, List<PurchaseRuleDTO> children) {
        this.type = type;
        this.value = value;
        this.children = children;
    }

    public PurchaseRuleType getType() {
        return type;
    }

    public void setType(PurchaseRuleType type) {
        this.type = type;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public List<PurchaseRuleDTO> getChildren() {
        return children;
    }

    public void setChildren(List<PurchaseRuleDTO> children) {
        this.children = children;
    }
}
