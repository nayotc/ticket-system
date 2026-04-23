package ticketsystem.DomainLayer.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DiscountPolicy {
    private List<DiscountTypes> discounts;

    public DiscountPolicy() {
        this.discounts = new ArrayList<>();
    }
    public List<DiscountTypes> getDiscounts() {
        return discounts;
    }
}
