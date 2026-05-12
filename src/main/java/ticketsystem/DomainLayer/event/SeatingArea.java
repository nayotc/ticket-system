package ticketsystem.DomainLayer.event;

import java.util.HashMap;
import java.util.Map;

import ticketsystem.DomainLayer.event.Seat.SeatStatus;

public class SeatingArea extends Area {
    private final Map<SeatPosition, Seat> seats = new HashMap<>();
    private int rows;
    private int columns;


    public SeatingArea(Long id, String name, Pair<Integer, Integer> location, Pair<Integer, Integer> size, int rows, int columns) {
        super(id, name, location, size);
        this.rows = rows;
        this.columns = columns;
        for (int row = 1; row <= rows; row++) {
            for (int col = 1; col <= columns; col++) {
                SeatPosition position = new SeatPosition(row, col);
                seats.put(position, new Seat(position));
            }
        }
    }

    public SeatingArea(SeatingArea other) {
        super(other);
        this.rows = other.rows;
        this.columns = other.columns;
        for (Map.Entry<SeatPosition, Seat> entry : other.seats.entrySet()) {
            this.seats.put(entry.getKey(), new Seat(entry.getValue()));
        }
    }

    public SeatingArea copy() {
        return new SeatingArea(this);
    }

    public int getRows() {
        return this.rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public int getColumns() {
        return this.columns;
    }

    public void setColumns(int columns) {
        this.columns = columns;
    }

    public Map<SeatPosition, Seat> getSeats() {
        return this.seats;
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

    public SeatStatus isSeatAvailable(SeatPosition position) {
        Seat seat = seats.get(position);
        if (seat == null) {
            throw new IllegalArgumentException("Seat not found");
        }
        return seat.getStatus();
    }

}
