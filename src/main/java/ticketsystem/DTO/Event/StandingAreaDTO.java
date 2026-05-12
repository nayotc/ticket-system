package ticketsystem.DTO.Event;

import ticketsystem.DomainLayer.event.StandingArea;

public record StandingAreaDTO(
        long id,
        String name,
        PairDTO<Integer, Integer> location,
        PairDTO<Integer, Integer> size,
        String type,
        boolean soldOut,
        long capacity,
        long reserved,
        long sold
) implements IMapElementDTO {

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
                area.getCapacity(),
                area.getReserved(),
                area.getSold()
        );
    }
}
