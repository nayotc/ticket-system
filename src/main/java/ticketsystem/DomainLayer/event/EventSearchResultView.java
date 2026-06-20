package ticketsystem.DomainLayer.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface EventSearchResultView {
    Long getId();
    String getName();
    Long getCompanyId();
    LocalDateTime getDate();
    EventLocation getLocation();
    EventCategory getCategory();
    String getArtistName();
    BigDecimal getTicketPrice();
    Double getRate();
    SaleStatus getSaleStatus();
}