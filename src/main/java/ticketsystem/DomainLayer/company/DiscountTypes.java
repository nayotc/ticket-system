package ticketsystem.DomainLayer.company;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.order.ActiveOrder;

public abstract class DiscountTypes {
    protected String name;
    public DiscountTypes(String name) {
        this.name = name;

    }

    public String getName() {
        return name;
    }

    public abstract BigDecimal calculateDiscount(BigDecimal totalPrice,int ticketCount,String couponCode);
}
