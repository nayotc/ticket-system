package ticketsystem.DTO.Event;

import ticketsystem.DomainLayer.event.StandingArea;

import java.math.BigDecimal;

public record StandingAreaDTO(
        Long id,
        String name,
        PairDTO<Integer, Integer> location,
        PairDTO<Integer, Integer> size,
        String type,
        boolean soldOut,
        BigDecimal price,
        long capacity,
        long reserved,
        long sold
) implements IAreaDTO {

    public static StandingAreaDTO from(StandingArea area) {
        if (area == null) {
            return null;
        }

        return new StandingAreaDTO(
                area.getId(),
                area.getName(),
                PairDTO.from(area.getLocation()),
                PairDTO.from(area.getSize()),
                "StandingArea",
                area.isSoldOut(),
                area.getPrice(),
                area.getCapacity(),
                area.getReserved(),
                area.getSold()
        );
    }
}
