package ticketsystem.DomainLayer.event;

public class StandingArea extends Area {
    private long capacity;
    private long reserved;
    private long sold;

    public StandingArea(Long id, String name, Pair<Integer, Integer> location, Pair<Integer, Integer> size,long capacity) {
        super(id, name, location, size);
        this.capacity = capacity;
    }

    public StandingArea(StandingArea other) {
        super(other);
        this.capacity = other.capacity;
        this.reserved = other.reserved;
        this.sold = other.sold;
    }

    public StandingArea copy() {
        return new StandingArea(this);
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

    public void reserveSpot(int quantity) {
        if (reserved + sold >= capacity) {
            throw new IllegalStateException("No more spots available");
        }
        reserved += quantity;
    }

    public void releaseSpot(int quantity) {
        if (reserved < quantity) {
            throw new IllegalStateException("Not enough reserved spots to release");
        }
        reserved -= quantity;
    }

    public void sellSpot(int quantity) {
        if (sold >= capacity) {
            throw new IllegalStateException("No more spots available");
        }
        reserved -= quantity; // Assuming selling a spot that was reserved
        sold += quantity;
    }

    public boolean isSoldOut() {
        return sold >= capacity;
    }

}
