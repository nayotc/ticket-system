package ticketsystem.DomainLayer.event;

import jakarta.persistence.Entity;
import java.math.BigDecimal;
import jakarta.persistence.Column;

@Entity
public abstract class Area extends Element {

    @Column(name = "price", precision = 12, scale = 2)
    private BigDecimal price;

    protected Area() {}

    public Area(String name, Pair<Integer, Integer> location, Pair<Integer, Integer> size, BigDecimal price) {
        super(name, location, size);
        this.price = price;
    }

    public Area(Area other) {
        super(other);
        this.price = other.price;
    }

    public abstract boolean isSoldOut();

    public BigDecimal getPrice() {
        return price;
    }
}
