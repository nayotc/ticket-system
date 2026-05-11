package ticketsystem.DTO.Event;

import java.util.List;
import java.util.stream.Collectors;

import ticketsystem.DomainLayer.event.SeatingArea;

public record SeatingAreaDTO(
        long id,
        String name,
        PairDTO<Integer, Integer> location,
        PairDTO<Integer, Integer> size,
        String type,
        boolean soldOut,
        int rows,
        int columns,
        List<SeatDTO> seats
) {

    public static SeatingAreaDTO from(SeatingArea area) {
        if (area == null) {
            return null;
        }

        return new SeatingAreaDTO(
                area.getId(),
                area.getName(),
                PairDTO.from(area.getLocation()),
                PairDTO.from(area.getSize()),
                "SeatingArea",
                area.isSoldOut(),
                area.getRows(),
                area.getColumns(),
                area.getSeats()
                        .values()
                        .stream()
                        .map(SeatDTO::from)
                        .collect(Collectors.toList())
        );
    }
}