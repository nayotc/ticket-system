package ticketsystem.DomainLayer.event;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

@Entity
@Table(name = "event_elements")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "element_type")
public class Element {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "first", column = @Column(name = "location_x")),
            @AttributeOverride(name = "second", column = @Column(name = "location_y"))
    })
    private Pair<Integer, Integer> location;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "first", column = @Column(name = "size_width")),
            @AttributeOverride(name = "second", column = @Column(name = "size_height"))
    })
    private Pair<Integer, Integer> size;

    protected Element() {}

    public Element(String name, Pair<Integer, Integer> location, Pair<Integer, Integer> size) {
        this.name = name;
        this.location = location;
        this.size = size;
    }

    @Deprecated
    public Element(Long id,
                   String name,
                   Pair<Integer, Integer> location,
                   Pair<Integer, Integer> size) {
        this(name, location, size);
        this.id = id;
    }

    public Element(Element other) {
        this.id = other.id;
        this.name = other.name;
        this.location = other.location;
        this.size = other.size;
    }

    public Element copy() {
        return new Element(this);
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Pair<Integer, Integer> getLocation() {
        return location;
    }

    public void setLocation(Pair<Integer, Integer> location) {
        this.location = location;
    }

    public Pair<Integer, Integer> getSize() {
        return size;
    }

    public void setSize(Pair<Integer, Integer> size) {
        this.size = size;
    }

}
