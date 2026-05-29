package ticketsystem.DTO;

import ticketsystem.DomainLayer.discount.DiscountCompositionType;
import java.util.ArrayList;
import java.util.List;

public class DiscountPolicyDTO {

    private DiscountCompositionType compositionType;
    private List<DiscountDTO> discounts;

    public DiscountPolicyDTO() {
        this.discounts = new ArrayList<>();
    }

    public DiscountPolicyDTO(DiscountCompositionType compositionType, List<DiscountDTO> discounts) {
        this.compositionType = compositionType;
        this.discounts = discounts != null ? discounts : new ArrayList<>();
    }

    public DiscountCompositionType getCompositionType() {
        return compositionType;
    }

    public void setCompositionType(DiscountCompositionType compositionType) {
        this.compositionType = compositionType;
    }

    public List<DiscountDTO> getDiscounts() {
        return discounts;
    }

    public void setDiscounts(List<DiscountDTO> discounts) {
        this.discounts = discounts;
    }
}