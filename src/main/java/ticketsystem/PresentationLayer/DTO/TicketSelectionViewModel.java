package ticketsystem.PresentationLayer.DTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public final class TicketSelectionViewModel {

    private TicketSelectionViewModel() {
    }

    public record EventTicketSelectionDto(
            String eventId,
            String eventName,
            LocalDateTime date,
            String location,
            EventMapDto map
    ) {
    }

    public record EventMapDto(
            int rows,
            int columns,
            List<MapElementDto> elements
    ) {
    }

    public record MapPositionDto(
            int row,
            int column,
            int rowSpan,
            int columnSpan
    ) {
    }

    public enum MapElementTypeDto {
        STAGE,
        ENTRANCE,
        EXIT,
        GENERIC,
        SEATING_AREA,
        STANDING_AREA
    }

    public static class MapElementDto {
        private final Long id;
        private final String name;
        private final MapElementTypeDto type;
        private final MapPositionDto position;

        public MapElementDto(Long id, String name, MapElementTypeDto type, MapPositionDto position) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.position = position;
        }

        public Long id() {
            return id;
        }

        public String name() {
            return name;
        }

        public MapElementTypeDto type() {
            return type;
        }

        public MapPositionDto position() {
            return position;
        }
    }

    public static class SeatingAreaDto extends MapElementDto {
        private final BigDecimal ticketPrice;
        private final int rows;
        private final int columns;
        private final List<SeatDto> seats;

        public SeatingAreaDto(
                Long id,
                String name,
                MapPositionDto position,
                BigDecimal ticketPrice,
                int rows,
                int columns,
                List<SeatDto> seats
        ) {
            super(id, name, MapElementTypeDto.SEATING_AREA, position);
            this.ticketPrice = ticketPrice;
            this.rows = rows;
            this.columns = columns;
            this.seats = seats == null ? List.of() : seats;
        }

        public BigDecimal ticketPrice() {
            return ticketPrice;
        }

        public int rows() {
            return rows;
        }

        public int columns() {
            return columns;
        }

        public List<SeatDto> seats() {
            return seats;
        }

        public Optional<SeatDto> findSeat(int row, int number) {
            return seats.stream()
                    .filter(seat -> seat.row() == row && seat.number() == number)
                    .findFirst();
        }
    }

    public static class StandingAreaDto extends MapElementDto {
        private final BigDecimal ticketPrice;
        private final int capacity;
        private final int reserved;
        private final int sold;

        public StandingAreaDto(
                Long id,
                String name,
                MapPositionDto position,
                BigDecimal ticketPrice,
                int capacity,
                int reserved,
                int sold
        ) {
            super(id, name, MapElementTypeDto.STANDING_AREA, position);
            this.ticketPrice = ticketPrice;
            this.capacity = capacity;
            this.reserved = reserved;
            this.sold = sold;
        }

        public BigDecimal ticketPrice() {
            return ticketPrice;
        }

        public int capacity() {
            return capacity;
        }

        public int reserved() {
            return reserved;
        }

        public int sold() {
            return sold;
        }

        public int availableCapacity() {
            return Math.max(0, capacity - reserved - sold);
        }
    }

    public record SeatDto(int row, int number, SeatStatusDto status) {
    }

    public enum SeatStatusDto {
        AVAILABLE,
        RESERVED,
        SOLD
    }
}
