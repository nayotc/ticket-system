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
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import org.hibernate.annotations.BatchSize;

@Entity
@DiscriminatorValue("SEATING")
public class SeatingArea extends Area {

    /*
     * Two seats occupy one map unit.
     * Therefore, one seat occupies 0.5 map units.
     */
    public static final int SEATS_PER_MAP_UNIT = 2;

    /*
     * Reserved vertical space for the area name and price.
     */
    public static final int HEADER_HEIGHT_UNITS = 1;

    /*
     * Prevents the name and price header from becoming too narrow.
     */
    public static final int MIN_HEADER_WIDTH_UNITS = 2;

    @OneToMany(mappedBy = "seatingArea", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @MapKey(name = "position")
    @BatchSize(size = 50)
    private Map<SeatPosition, Seat> seats = new HashMap<>();
    private int rows;
    private int columns;
    private int SoldSeats;

    protected SeatingArea() {}

    /*
     * Use this constructor for all new code.
     * The area size is generated from rows and columns.
     */
    public SeatingArea(String name, Pair<Integer, Integer> location, int rows, int columns, BigDecimal price) {
        super(name, location, calculateSize(rows, columns), price);
        this.rows = rows;
        this.columns = columns;
        for (int row = 1; row <= rows; row++) {
            for (int column = 1; column <= columns; column++) {
                addSeat(row, column);
            }
        }
    }

    /*
     * Keep this constructor temporarily so existing tests and initializers
     * continue compiling.
     *
     * The supplied size is ignored intentionally.
     */
    @Deprecated
    public SeatingArea(
            String name,
            Pair<Integer, Integer> location,
            Pair<Integer, Integer> ignoredSize,
            int rows,
            int columns,
            BigDecimal price
    ) {
        this(
                name,
                location,
                rows,
                columns,
                price
        );
    }

    public SeatingArea(SeatingArea other) {
        super(other);
        this.rows = other.rows;
        this.columns = other.columns;
        this.SoldSeats = other.SoldSeats;

        synchronizeSizeWithDimensions();

        for (Seat originalSeat : other.seats.values()) {
            Seat copiedSeat = new Seat(originalSeat);
            copiedSeat.setSeatingArea(this);
            this.seats.put(copiedSeat.getPosition(), copiedSeat);
        }
    }

    @Override
    public SeatingArea copy() {
        return new SeatingArea(this);
    }

    public static Pair<Integer, Integer> calculateSize(int rows, int columns) {
        validateDimensions(rows, columns);
        int seatGridWidth = divideAndRoundUp(columns, SEATS_PER_MAP_UNIT);
        int seatGridHeight = divideAndRoundUp(rows, SEATS_PER_MAP_UNIT);
        int finalWidth = Math.max(seatGridWidth, MIN_HEADER_WIDTH_UNITS);
        int finalHeight = Math.addExact(seatGridHeight, HEADER_HEIGHT_UNITS);
        return new Pair<>(finalWidth, finalHeight);
    }

    private static int divideAndRoundUp(int value, int divisor) {
        return (value + divisor - 1) / divisor;
    }

    public int getRows() {
        return this.rows;
    }

    public int getColumns() {
        return this.columns;
    }

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

    @Override
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

        synchronizeSizeWithDimensions();

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

    private static void validateDimensions(int rows, int columns) {
        if (rows <= 0) {
            throw new IllegalArgumentException("Rows must be positive");
        }

        if (columns <= 0) {
            throw new IllegalArgumentException("Columns must be positive");
        }
    }

    @PostLoad
    @PrePersist
    @PreUpdate
    private void synchronizeSizeWithDimensions() {super.setSize(calculateSize(rows, columns));
    }

    @Override
    public void setSize(Pair<Integer, Integer> ignoredSize) {
        throw new UnsupportedOperationException("Seating area size is calculated from rows and columns");
    }
}