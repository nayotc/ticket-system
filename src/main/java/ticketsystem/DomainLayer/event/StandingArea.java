package ticketsystem.DomainLayer.event;

public class StandingArea extends Area {
    private long capacity;
    private long occupied;

    public StandingArea(String name, Pair<Integer, Integer> location, Pair<Integer, Integer> size, String description, long capacity) {
        super(name, location, size/* , description*/);
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
