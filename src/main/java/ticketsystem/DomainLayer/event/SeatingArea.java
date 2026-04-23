package ticketsystem.DomainLayer.event;

import java.util.HashMap;
import java.util.Map;

public class SeatingArea extends Area {
    private final Map<SeatPosition, Seat> seats = new HashMap<>();


    public SeatingArea(String name, Pair<Integer, Integer> location, Pair<Integer, Integer> size, String description, int rows, int columns) {
        super(name, location, size);
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
    
}
