package ticketsystem.DomainLayer.event;

import org.springframework.beans.factory.annotation.Value;

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
    @Value("#{target.minimalTicketPrice}")
    BigDecimal getTicketPrice();
    Double getRate();
    SaleStatus getSaleStatus();
}