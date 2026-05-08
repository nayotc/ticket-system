package ticketsystem.DomainLayer.event;

public class StandingArea extends Area {
    private long capacity;
    private long reserved;
    private long sold;

    public StandingArea(Long id, String name, Pair<Integer, Integer> location, Pair<Integer, Integer> size, String description, long capacity) {
        super(id, name, location, size);
        this.capacity = capacity;
    }

    public long getCapacity() {
        return capacity;
    }

    public void setCapacity(long capacity) {
        this.capacity = capacity;
    }

    public long getReserved() {
        return reserved;
    }

    public void setReserved(long reserved) {
        this.reserved = reserved;
    }

    public long getSold() {
        return sold;
    }

    public void setSold(long sold) {
        this.sold = sold;
    }

    public void reserveSpot() {
        if (reserved >= capacity) {
            throw new IllegalStateException("No more spots available");
        }
        reserved++;
    }

    public void releaseSpot() {
        if (reserved <= 0) {
            throw new IllegalStateException("No reserved spots to release");
        }
        reserved--;
    }

    public void sellSpot() {
        if (sold >= capacity) {
            throw new IllegalStateException("No more spots available");
        }
        sold++;
    }

    public boolean isSoldOut() {
        return sold >= capacity;
    }

}
