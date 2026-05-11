package ticketsystem.DTO.Event;

import ticketsystem.DomainLayer.event.SeatPosition;

public record SeatPositionDTO(int row, int number) {

    public static SeatPositionDTO from(SeatPosition position) {
        if (position == null) {
            return null;
        }

        return new SeatPositionDTO(position.row(), position.number());
    }
}