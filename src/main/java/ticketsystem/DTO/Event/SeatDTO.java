package ticketsystem.DTO.Event;

import ticketsystem.DomainLayer.event.Seat;

public record SeatDTO(SeatPositionDTO position,String status){

    public static SeatDTO from(Seat seat) {
        if (seat == null) {
            return null;
        }

        return new SeatDTO(
                SeatPositionDTO.from(seat.getPosition()),
                seat.getStatus().name()
        );
    }
}
