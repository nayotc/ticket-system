package ticketsystem.DomainLayer.event;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import ticketsystem.DomainLayer.event.Seat.SeatStatus;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;

import org.hibernate.annotations.BatchSize;

@Embeddable
public class EventMap {

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "first", column = @Column(name = "map_width")),
        @AttributeOverride(name = "second", column = @Column(name = "map_height"))
    })
    private Pair<Integer, Integer> size;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    @BatchSize(size = 50)
    private List<Element> elements;

    protected EventMap() {
    }

    public EventMap(Pair<Integer, Integer> size) {
        this.size = size;
        this.elements = new java.util.ArrayList<>();
    }

    // Copy constructor
    public EventMap(EventMap other) {
        this.size = other.size.copy();
        this.elements = other.elements.stream()
                .map(Element::copy)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public Pair<Integer, Integer> getSize() {
        return size;
    }

    public void setSize(Pair<Integer, Integer> size) {
        this.size = size;
    }

    public List<Element> getElements() {
        return elements;
    }

    public void setElements(List<Element> elements) {
        this.elements = elements;
    }

    public void addElement(Element element) {
        this.elements.add(element);
    }

    public void removeElement(Element element) {
        this.elements.remove(element);
    }

    public void reserveSeat(Long areaId, SeatPosition position) {
        for (Element element : elements) {
            if (element instanceof SeatingArea && Objects.equals(element.getId(), areaId)) {
                ((SeatingArea) element).reserveSeat(position);
                return;
            }
        }
        throw new IllegalArgumentException("Area not found");
    }

    public void releaseSeat(Long areaId, SeatPosition position) {
        for (Element element : elements) {
            if (element instanceof SeatingArea && Objects.equals(element.getId(), areaId)) {
                ((SeatingArea) element).releaseSeat(position);
                return;
            }
        }
        throw new IllegalArgumentException("Area not found");
    }

    public void sellSeat(Long areaId, SeatPosition position) {
        for (Element element : elements) {
            if (element instanceof SeatingArea && Objects.equals(element.getId(), areaId)) {
                ((SeatingArea) element).sellSeat(position);
                return;
            }
        }
        throw new IllegalArgumentException("Area not found");
    }

    public void reserveSpot(Long areaId, int quantity) {
        for (Element element : elements) {
            if (element instanceof StandingArea && Objects.equals(element.getId(), areaId)) {
                ((StandingArea) element).reserveSpot(quantity);
                return;
            }
        }
        throw new IllegalArgumentException("Area not found");
    }

    public void releaseSpot(Long areaId, int quantity) {
        for (Element element : elements) {
            if (element instanceof StandingArea && Objects.equals(element.getId(), areaId)) {
                ((StandingArea) element).releaseSpot(quantity);
                return;
            }
        }
        throw new IllegalArgumentException("Area not found");
    }

    public void sellSpot(Long areaId, int quantity) {
        for (Element element : elements) {
            if (element instanceof StandingArea && Objects.equals(element.getId(), areaId)) {
                ((StandingArea) element).sellSpot(quantity);
                return;
            }
        }
        throw new IllegalArgumentException("Area not found");
    }

    public boolean isSoldOut() {
        if (elements == null || elements.isEmpty()) {
            return false;
        }

        boolean foundAnyArea = false;

        for (Element element : elements) {
            if (element instanceof Area) {
                foundAnyArea = true;
                if (!((Area) element).isSoldOut()) {
                    return false;
                }
            }
        }
        return foundAnyArea;
    }

    public SeatStatus isSeatAvailable(Long areaId, SeatPosition position) {
        for (Element element : elements) {
            if (element instanceof SeatingArea && Objects.equals(element.getId(), areaId)) {
                return ((SeatingArea) element).isSeatAvailable(position);
            }
        }
        throw new IllegalArgumentException("Area not found");
    }

    public String getAreaName(Long areaId) {
        if (areaId == null) {
            return "אזור לא ידוע";
        }

        for (Element element : elements) {
            if (areaId.equals(element.getId())) {
                return element.getName();
            }
        }

        return "אזור לא ידוע";
    }

    public BigDecimal getAreaPrice(Long areaId) {
        return findArea(areaId).getPrice();
    }

    public BigDecimal getMinimumAreaPrice() {
        if (elements == null) {
            return null;
        }

        return elements.stream()
                .filter(Area.class::isInstance)
                .map(Area.class::cast)
                .map(Area::getPrice)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(null);
    }

    public boolean hasAreaInPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice != null
                && maxPrice != null
                && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException(
                    "Minimum price cannot be greater than maximum price"
            );
        }

        if (elements == null) {
            return false;
        }

        return elements.stream()
                .filter(Area.class::isInstance)
                .map(Area.class::cast)
                .map(Area::getPrice)
                .filter(Objects::nonNull)
                .anyMatch(price ->
                        (minPrice == null || price.compareTo(minPrice) >= 0)
                                && (maxPrice == null || price.compareTo(maxPrice) <= 0)
                );
    }

    private Area findArea(Long areaId) {
        if (areaId == null) {
            throw new IllegalArgumentException("Area ID cannot be null");
        }

        if (elements == null) {
            throw new IllegalArgumentException("Area not found");
        }

        return elements.stream()
                .filter(Area.class::isInstance)
                .map(Area.class::cast)
                .filter(area -> Objects.equals(area.getId(), areaId))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Area not found: " + areaId
                        )
                );
    }

    public long getTicketCapacity() {
        if (elements == null) {
            return 0;
        }

        return elements.stream()
                .filter(Area.class::isInstance)
                .map(Area.class::cast)
                .mapToLong(Area::getCapacity)
                .sum();
    }

    public long addArea(Area area) {
        if (area == null) {
            throw new IllegalArgumentException("Area cannot be null");
        }

        if (area.getId() != null) {
            throw new IllegalArgumentException(
                    "A new area must not already have an ID"
            );
        }

        if (elements == null) {
            elements = new ArrayList<>();
        }

        elements.add(area);
        return area.getCapacity();
    }

    public int expandSeatingArea(
            Long areaId,
            int newRows,
            int newColumns
    ) {
        SeatingArea area = findSeatingArea(areaId);
        return area.expandTo(newRows, newColumns);
    }

    public long increaseStandingAreaCapacity(
            Long areaId,
            long newCapacity
    ) {
        StandingArea area = findStandingArea(areaId);
        return area.increaseCapacityTo(newCapacity);
    }

    private SeatingArea findSeatingArea(Long areaId) {
        if (areaId == null) {
            throw new IllegalArgumentException("Area ID cannot be null");
        }

        if (elements == null) {
            throw new IllegalArgumentException("Seating area not found");
        }

        return elements.stream()
                .filter(SeatingArea.class::isInstance)
                .map(SeatingArea.class::cast)
                .filter(area -> Objects.equals(area.getId(), areaId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Seating area not found: " + areaId
                ));
    }

    private StandingArea findStandingArea(Long areaId) {
        if (areaId == null) {
            throw new IllegalArgumentException("Area ID cannot be null");
        }

        if (elements == null) {
            throw new IllegalArgumentException("Standing area not found");
        }

        return elements.stream()
                .filter(StandingArea.class::isInstance)
                .map(StandingArea.class::cast)
                .filter(area -> Objects.equals(area.getId(), areaId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Standing area not found: " + areaId
                ));
    }

}
