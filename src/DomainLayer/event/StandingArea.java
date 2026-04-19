package DomainLayer.event;

public class StandingArea Extends Area {
    private long capacity;
    private long occupied;

    public StandingArea(String name, pair<Integer, Integer> location, pair<Integer, Integer> size, String description, long capacity) {
        super(name, location, size, description);
        this.capacity = capacity;
    }

    public long getCapacity() {
        return capacity;
    }

    public void setCapacity(long capacity) {
        this.capacity = capacity;
    }

    public long getOccupied() {
        return occupied;
    }

    public void setOccupied(long occupied) {
        this.occupied = occupied;
    }

}
