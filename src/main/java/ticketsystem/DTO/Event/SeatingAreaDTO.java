package ticketsystem.DTO.Event;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import ticketsystem.DomainLayer.event.Pair;
import ticketsystem.DomainLayer.event.SeatingArea;

public record SeatingAreaDTO(
        Long id,
        String name,
        PairDTO<Integer, Integer> location,
        PairDTO<Integer, Integer> size,
        String type,
        boolean soldOut,
        BigDecimal price,
        int rows,
        int columns,
        List<SeatDTO> seats
) implements IAreaDTO {

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
                area.getPrice(),
                area.getRows(),
                area.getColumns(),
                area.getSeats()
                        .values()
                        .stream()
                        .map(SeatDTO::from)
                        .collect(Collectors.toList())
        );
    }

    public static PairDTO<Integer, Integer> calculateSize(int rows, int columns) {
        Pair<Integer,Integer> size = SeatingArea.calculateSize(rows, columns);
        return new PairDTO<>(size.getFirst(), size.getSecond());
    }

    public static int headerHeightUnits() {
        return SeatingArea.HEADER_HEIGHT_UNITS;
    }
}