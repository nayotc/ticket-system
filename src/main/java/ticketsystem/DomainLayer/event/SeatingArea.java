package ticketsystem.DomainLayer.event;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class SeatingArea extends Area {
    private final Map<SeatPosition, Seat> seats = new HashMap();


    public SeatingArea(Long id, String name, Pair<Integer, Integer> location, Pair<Integer, Integer> size, String description, int rows, int columns) {
        super(id, name, location, size);
        // this.rows = rows;
        // this.columns = columns;
    }

    public int getRows() {
        return 0;
    }

    public void setRows(int rows) {
        //this.rows = rows;
    }

    public int getColumns() {
        return 0;
    }

    public void setColumns(int columns) {
        //this.columns = columns;
    }

    public void reserveSeat(SeatPosition position) {
        Seat seat = seats.get(position);
        if (seat == null) {
            throw new IllegalArgumentException("Seat not found");
        }
        seat.reserve();
    }

    public void releaseSeat(SeatPosition position) {
        Seat seat = seats.get(position);
        if (seat == null) {
            throw new IllegalArgumentException("Seat not found");
        }
        seat.release();
    }

    public void sellSeat(SeatPosition position) {
        Seat seat = seats.get(position);
        if (seat == null) {
            throw new IllegalArgumentException("Seat not found");
        }
        seat.sell();
    }

    public boolean isSoldOut() {
        for (Seat seat : seats.values()) {
            if (seat.getStatus() != Seat.SeatStatus.SOLD) {
                return false;
            }
        }
        return true;
    }

}
