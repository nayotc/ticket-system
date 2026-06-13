package ticketsystem.DomainLayer.event;

import jakarta.persistence.Entity;

@Entity
public abstract class Area extends Element {

    protected Area() {}

    public Area(String name, Pair<Integer, Integer> location, Pair<Integer, Integer> size) {
        super(name, location, size);
    }

    public Area(Area other) {
        super(other);
    }

    public abstract boolean isSoldOut();

}
