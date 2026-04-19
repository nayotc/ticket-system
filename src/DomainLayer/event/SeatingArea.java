package DomainLayer.event;

public class SeatingArea Extends Area {
    private final Map<SeatPosition, Seat> seats = new HashMap<>();


    public SeatingArea(String name, pair<Integer, Integer> location, pair<Integer, Integer> size, String description, int rows, int columns) {
        super(name, location, size, description);
        this.rows = rows;
        this.columns = columns;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public int getColumns() {
        return columns;
    }

    public void setColumns(int columns) {
        this.columns = columns;
    }
    
}
