package ticketsystem.DTO.Event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.event.EventSearchResultView;

public record EventSearchResultDTO(
        Long id,
        String name,
        Long companyId,
        LocalDateTime date,
        String location,
        String category,
        String artistName,
        BigDecimal ticketPrice,
        Double rate,
        String saleStatus

) {

    public static EventSearchResultDTO from(Event event) {
        if (event == null) {
            return null;
        }

        return new EventSearchResultDTO(
                event.getId(),
                event.getName(),
                event.getCompanyId(),
                event.getDate(),
                event.getLocation() == null ? null : event.getLocation().name(),
                event.getCategory() == null ? null : event.getCategory().name(),
                event.getArtistName(),
                event.getMinimalTicketPrice(),
                event.getRate(),
                event.getSaleStatus() == null ? null : event.getSaleStatus().name()
        );
    }

    public static EventSearchResultDTO from(EventSearchResultView event) {
        if (event == null) {
            return null;
        }

        return new EventSearchResultDTO(
                event.getId(),
                event.getName(),
                event.getCompanyId(),
                event.getDate(),
                event.getLocation() == null
                        ? null
                        : event.getLocation().name(),
                event.getCategory() == null
                        ? null
                        : event.getCategory().name(),
                event.getArtistName(),
                event.getTicketPrice(),
                event.getRate(),
                event.getSaleStatus() == null
                        ? null
                        : event.getSaleStatus().name()
        );
    }
}