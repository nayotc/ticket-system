package ticketsystem.DomainLayer.company;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.order.ActiveOrder;

public abstract class DiscountTypes {
    protected String name;
    protected LocalDateTime startTime;
    protected LocalDateTime endTime;

    public DiscountTypes(String name, LocalDateTime startTime, LocalDateTime endTime) {
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getName() {
        return name;
    }

    protected boolean isActiveNow() {
        LocalDateTime now = LocalDateTime.now();

        if (startTime != null && now.isBefore(startTime)) {
            return false;
        }

        if (endTime != null && now.isAfter(endTime)) {
            return false;
        }

        return true;
    }

    public abstract BigDecimal calculateDiscount(BigDecimal totalPrice,int ticketCount,String couponCode);
}
