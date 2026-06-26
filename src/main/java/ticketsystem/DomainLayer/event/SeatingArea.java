package ticketsystem.DomainLayer.event;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import ticketsystem.DomainLayer.event.Seat.SeatStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.MapKey;
import jakarta.persistence.OneToMany;

import org.hibernate.annotations.BatchSize;

@Entity
@DiscriminatorValue("SEATING")
public class SeatingArea extends Area {

    @OneToMany(mappedBy = "seatingArea", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @MapKey(name = "position")
    @BatchSize(size = 50)
    private Map<SeatPosition, Seat> seats = new HashMap<>();
    private int rows;
    private int columns;
    private int SoldSeats;

    protected SeatingArea() {}

    public SeatingArea(String name, Pair<Integer, Integer> location, Pair<Integer, Integer> size, int rows, int columns, BigDecimal price) {
        super(name, location, size, price);
        validateDimensions(rows, columns);
        this.rows = rows;
        this.columns = columns;
        for (int row = 1; row <= rows; row++) {
            for (int col = 1; col <= columns; col++) {
                SeatPosition position = new SeatPosition(row, col);
                Seat seat = new Seat(position);
                seat.setSeatingArea(this);
                seats.put(position, seat);
            }
        }
    }

    public SeatingArea(SeatingArea other) {
        super(other);
        this.rows = other.rows;
        this.columns = other.columns;
        this.SoldSeats = other.SoldSeats;

        for (Seat originalSeat : other.seats.values()) {
            Seat copiedSeat = new Seat(originalSeat);
            copiedSeat.setSeatingArea(this);
            this.seats.put(copiedSeat.getPosition(), copiedSeat);
        }
    }

    public SeatingArea copy() {
        return new SeatingArea(this);
    }

    public int getRows() {
        return this.rows;
    }

//    public void setRows(int rows) {
//        this.rows = rows;
//    }

    public int getColumns() {
        return this.columns;
    }

//    public void setColumns(int columns) {
//        this.columns = columns;
//    }

    public Map<SeatPosition, Seat> getSeats() {
        return this.seats;
    }

    public int getSoldSeats() {return this.SoldSeats;}

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
        SoldSeats++;
    }

    public boolean isSoldOut() {
        return SoldSeats >= seats.size();
    }

    public SeatStatus isSeatAvailable(SeatPosition position) {
        Seat seat = seats.get(position);
        if (seat == null) {
            throw new IllegalArgumentException("Seat not found");
        }
        return seat.getStatus();
    }

    @Override
    public long getCapacity() {
        return seats.size();
    }

    public void validateExpansionTo(int newRows, int newColumns) {
        validateDimensions(newRows, newColumns);

        if (newRows < rows) {
            throw new IllegalArgumentException(
                    "Rows cannot be reduced for an active event"
            );
        }

        if (newColumns < columns) {
            throw new IllegalArgumentException(
                    "Columns cannot be reduced for an active event"
            );
        }
    }

    public int expandTo(int newRows, int newColumns) {
        validateExpansionTo(newRows, newColumns);

        int previousSeatCount = seats.size();

        for (int row = 1; row <= newRows; row++) {
            for (int column = 1; column <= newColumns; column++) {
                if (row > rows || column > columns) {
                    addSeat(row, column);
                }
            }
        }

        rows = newRows;
        columns = newColumns;

        return seats.size() - previousSeatCount;
    }

    private void addSeat(int row, int column) {
        SeatPosition position = new SeatPosition(row, column);

        if (seats.containsKey(position)) {
            return;
        }

        Seat seat = new Seat(position);
        seat.setSeatingArea(this);
        seats.put(position, seat);
    }

    private void validateDimensions(int rows, int columns) {
        if (rows <= 0) {
            throw new IllegalArgumentException("Rows must be positive");
        }

        if (columns <= 0) {
            throw new IllegalArgumentException("Columns must be positive");
        }
    }
}
