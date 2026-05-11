package ticketsystem.DTO.Event;

import ticketsystem.DomainLayer.event.Area;

public record AreaDTO(
        long id,
        String name,
        PairDTO<Integer, Integer> location,
        PairDTO<Integer, Integer> size,
        String type,
        boolean soldOut
) {

    public static AreaDTO from(Area area) {
        if (area == null) {
            return null;
        }

        return new AreaDTO(
                area.getId(),
                area.getName(),
                PairDTO.from(area.getLocation()),
                PairDTO.from(area.getSize()),
                area.getClass().getSimpleName(),
                area.isSoldOut()
        );
    }
}
