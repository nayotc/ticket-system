package ticketsystem.DTO.Event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import ticketsystem.DomainLayer.event.Event;

public record EventDTO(
        Long id,
        String name,
        Long companyId,
        Long openedBy,
        LocalDateTime date,
        String location,
        Long trafficThreshold,
        String status,
        String category,
        String artistName,
        BigDecimal ticketPrice,
        PairDTO<Integer, Integer> mapSize,
        Double rate,
        String saleStatus,
        boolean overloaded,
        int activeReservationsCount,
        int version,
        EventMapDTO map
) {

    public static EventDTO from(Event event) {
        if (event == null) {
            return null;
        }

        return new EventDTO(
                event.getId(),
                event.getName(),
                event.getCompanyId(),
                event.getOpenedBy(),
                event.getDate(),
                event.getLocation() == null ? null : event.getLocation().name(),
                event.getTrafficThreshold(),
                event.getStatus() == null ? null : event.getStatus().name(),
                event.getCategory() == null ? null : event.getCategory().name(),
                event.getArtistName(),
                event.getMinimalTicketPrice(),
                event.getMap() == null ? null : new PairDTO<>(event.getMap().getSize().getFirst(), event.getMap().getSize().getSecond()),
                event.getRate(),
                event.getSaleStatus() == null ? null : event.getSaleStatus().name(),
                event.isOverloaded(),
                event.getActiveReservationsCount(),
                event.getVersion(),
                EventMapDTO.from(event.getMap())
        );
    }
}
