package ticketsystem.DomainLayer.event;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("STANDING")
public class StandingArea extends Area {
    private long capacity;
    private long reserved;
    private long sold;

    protected StandingArea() {
    }

    public StandingArea(String name, Pair<Integer, Integer> location, Pair<Integer, Integer> size, long capacity) {
        super(name, location, size);

        if (capacity < 0) {
            throw new IllegalArgumentException("Capacity cannot be negative");
        }

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
        if (capacity < reserved + sold) {
            throw new IllegalArgumentException("Capacity cannot be smaller than reserved and sold spots");
        }

        this.capacity = capacity;
    }

    public long getReserved() {
        return reserved;
    }

    public void setReserved(long reserved) {
        if (reserved < 0) {
            throw new IllegalArgumentException("Reserved cannot be negative");
        }

        if (reserved + sold > capacity) {
            throw new IllegalArgumentException("Reserved and sold spots cannot exceed capacity");
        }

        this.reserved = reserved;
    }

    public long getSold() {
        return sold;
    }

    public void setSold(long sold) {
        if (sold < 0) {
            throw new IllegalArgumentException("Sold cannot be negative");
        }

        if (reserved + sold > capacity) {
            throw new IllegalArgumentException("Reserved and sold spots cannot exceed capacity");
        }

        this.sold = sold;
    }

    public void reserveSpot(int quantity) {
        validatePositiveQuantity(quantity);

        if (reserved + sold + quantity > capacity) {
            throw new IllegalStateException("No more spots available");
        }

        reserved += quantity;
    }

    public void releaseSpot(int quantity) {
        validatePositiveQuantity(quantity);

        if (reserved < quantity) {
            throw new IllegalStateException("Not enough reserved spots to release");
        }

        reserved -= quantity;
    }

    public void sellSpot(int quantity) {
        validatePositiveQuantity(quantity);

        if (reserved < quantity) {
            throw new IllegalStateException("Not enough reserved spots to sell");
        }

        if (sold + quantity > capacity) {
            throw new IllegalStateException("No more spots available");
        }

        reserved -= quantity;
        sold += quantity;
    }

    public boolean isSoldOut() {
        return sold >= capacity;
    }

    private void validatePositiveQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
    }
}