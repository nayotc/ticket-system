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
        setPrice(price);
    }

    public Area(Area other) {
        super(other);
        this.price = other.price;
    }

    public abstract boolean isSoldOut();

    public BigDecimal getPrice() {
        return price;
    }

    public void  setPrice(BigDecimal price) {
        if (price == null) {
            throw new IllegalArgumentException("Area price cannot be null");
        }

        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Area price cannot be negative");
        }
        this.price = price;
    }
}
