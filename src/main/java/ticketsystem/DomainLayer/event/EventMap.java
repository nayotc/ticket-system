package ticketsystem.DomainLayer.event;

import java.math.BigDecimal;
import java.util.*;
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
        this(size, new ArrayList<>());
    }

    public EventMap(Pair<Integer, Integer> size, List<Element> elements) {
        this.size = size;
        this.elements = elements;
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
        if (element == null) {
            throw new IllegalArgumentException("Map elements cannot contain null");
        }

        if (elements == null) {
            throw new IllegalStateException("Map elements collection is not initialized");
        }

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

    public void updateActiveAreas(List<Area> newAreas, Map<Long, Area> updatedAreas) {
        if (newAreas == null) {
            throw new IllegalArgumentException("New areas list cannot be null");
        }

        if (updatedAreas == null) {
            throw new IllegalArgumentException("Updated areas list cannot be null");
        }

        for (Area newArea : newAreas) {
            if (newArea == null) {
                throw new IllegalArgumentException("New areas cannot contain null");
            }

            if (newArea.getId() != null) {
                throw new IllegalArgumentException("A new area must not already have an ID");
            }
        }

        /*
         * This is only a temporary list for bounds and overlap validation.
         * It does not modify the real event map.
         */
        List<Element> candidateElements = new ArrayList<>(elements);

        candidateElements.addAll(newAreas);

        for (Map.Entry<Long, Area> entry : updatedAreas.entrySet()) {

            Long areaId = entry.getKey();
            Area requestedArea = entry.getValue();
            if (areaId == null || requestedArea == null) {
                throw new IllegalArgumentException("Updated area ID and data cannot be null");
            }
            Area existingArea = findArea(areaId);
            validateActiveAreaUpdate(existingArea, requestedArea);

            /*
             * Replace the existing area only in the validation list.
             * requestedArea already contains the newly calculated size.
             */
            int existingIndex = candidateElements.indexOf(existingArea);
            candidateElements.set(existingIndex, requestedArea);
        }

        System.err.println();
        System.err.println("[ACTIVE MAP UPDATE DEBUG]");
        System.err.println(
                "Map size convention: first=height, second=width"
        );
        System.err.println(
                "Map height=" + size.getFirst()
                        + ", map width=" + size.getSecond()
        );
        System.err.println(
                "Existing and candidate elements count="
                        + candidateElements.size()
        );

        for (int i = 0; i < candidateElements.size(); i++) {
            Element candidate = candidateElements.get(i);

            System.err.printf(
                    "candidate[%d]: class=%s, id=%s, name=%s, "
                            + "location=(%s,%s), size=(%s,%s)%n",
                    i,
                    candidate.getClass().getSimpleName(),
                    candidate.getId(),
                    candidate.getName(),
                    candidate.getLocation() == null
                            ? null
                            : candidate.getLocation().getFirst(),
                    candidate.getLocation() == null
                            ? null
                            : candidate.getLocation().getSecond(),
                    candidate.getSize() == null
                            ? null
                            : candidate.getSize().getFirst(),
                    candidate.getSize() == null
                            ? null
                            : candidate.getSize().getSecond()
            );
        }

        /*
         * Validate new areas and enlarged seating areas together.
         */
        validateElementsInsideMapBounds(candidateElements);
        validateElementsDoNotOverlap(candidateElements);

        /*
         * Only after all validation succeeds, update the real areas.
         */
        for (Map.Entry<Long, Area> entry : updatedAreas.entrySet()) {
            Area existingArea = findArea(entry.getKey());
            Area requestedArea = entry.getValue();
            if (existingArea instanceof StandingArea standingArea) {
                standingArea.increaseCapacityTo(requestedArea.getCapacity());
            } else if (existingArea instanceof SeatingArea seatingArea) {
                SeatingArea updatedSeatingArea = (SeatingArea) requestedArea;
                seatingArea.expandTo(updatedSeatingArea.getRows(), updatedSeatingArea.getColumns());
            } else {
                throw new IllegalArgumentException("Unknown area type");
            }
        }
        elements.addAll(newAreas);
    }

    private void validateActiveAreaUpdate(Area existingArea, Area updatedArea) {
        if (!Objects.equals(existingArea.getLocation(), updatedArea.getLocation())) {
            throw new IllegalArgumentException("Existing area location cannot be changed");
        }
        if (existingArea instanceof StandingArea existingStanding && updatedArea instanceof StandingArea updatedStanding) {
            if (updatedStanding.getCapacity() < existingStanding.getCapacity()) {
                throw new IllegalArgumentException("Standing area capacity cannot be reduced");
            }
            return;
        }

        if (existingArea instanceof SeatingArea existingSeating && updatedArea instanceof SeatingArea updatedSeating) {
            if (updatedSeating.getRows() < existingSeating.getRows()) {
                throw new IllegalArgumentException("Seating area rows cannot be reduced");
            }

            if (updatedSeating.getColumns() < existingSeating.getColumns()) {
                throw new IllegalArgumentException("Seating area columns cannot be reduced");
            }
            return;
        }

        throw new IllegalArgumentException("Area type cannot be changed");
    }

    public void validateForActivation() {
        validateMapSize();
        validateElementsCollection();
        validateHasAtLeastOneTicketArea();
        validateElementsInsideMapBounds(elements);
        validateElementsDoNotOverlap(elements);
    }

    private void validateMapSize() {
        if (size == null
                || size.getFirst() == null
                || size.getSecond() == null) {
            throw new IllegalArgumentException(
                    "Map size cannot be null"
            );
        }

        // Preserve the existing system convention:
        // first = height, second = width
        int mapHeight = size.getFirst();
        int mapWidth = size.getSecond();

        if (mapWidth <= 0 || mapHeight <= 0) {
            throw new IllegalArgumentException(
                    "Map size must be positive"
            );
        }
    }

    private void validateElementsCollection() {
        if (elements == null) {
            throw new IllegalArgumentException(
                    "Map elements cannot be null"
            );
        }

        if (elements.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "Map elements cannot contain null"
            );
        }
    }

    private void validateHasAtLeastOneTicketArea() {
        boolean hasTicketArea = elements.stream()
                .anyMatch(Area.class::isInstance);

        if (!hasTicketArea) {
            throw new IllegalArgumentException(
                    "Event map must contain at least one seating area or standing area"
            );
        }
    }

    private void validateElementsInsideMapBounds(List<? extends Element> elementsToValidate) {
        // Preserve the existing system convention:
        // first = height, second = width
        int mapHeight = size.getFirst();
        int mapWidth = size.getSecond();

        for (Element element : elementsToValidate) {
            validateSingleElementInsideMapBounds(
                    element,
                    mapWidth,
                    mapHeight
            );
        }
    }

    private void validateSingleElementInsideMapBounds(Element element, int mapWidth, int mapHeight) {
        if (element == null) {
            throw new IllegalArgumentException(
                    "Map elements cannot contain null"
            );
        }

        Pair<Integer, Integer> location = element.getLocation();
        Pair<Integer, Integer> elementSize = element.getSize();
        String elementName = safeElementName(element.getName());

        if (location == null || elementSize == null) {
            throw new IllegalArgumentException("Element location and size cannot be null: " + elementName);
        }

        if (location.getFirst() == null
                || location.getSecond() == null
                || elementSize.getFirst() == null
                || elementSize.getSecond() == null) {
            throw new IllegalArgumentException("Element location and size values cannot be null: " + elementName);
        }

        int x = location.getFirst();
        int y = location.getSecond();
        int width = elementSize.getFirst();
        int height = elementSize.getSecond();

        if (x < 0 || y < 0){
            throw new IllegalArgumentException("Element location cannot be negative: " + elementName);
        }

        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Element size must be positive: " + elementName);
        }

        long rightBoundary = (long) x + width - 1 ;
        long bottomBoundary = (long) y + height - 1;

        if (rightBoundary > mapWidth || bottomBoundary > mapHeight) {
            throw new IllegalArgumentException("Element is outside map bounds: " + elementName);
        }
    }

    private void validateElementsDoNotOverlap(
            List<? extends Element> elementsToValidate
    ) {
        List<MapElementBounds> existingBounds = new ArrayList<>();

        for (Element element : elementsToValidate) {
            MapElementBounds currentBounds =
                    toMapElementBounds(element);

            for (MapElementBounds existing : existingBounds) {
                if (currentBounds.overlaps(existing)) {
                    throw new IllegalArgumentException(
                            "Map elements cannot overlap: "
                                    + existing.name()
                                    + " and "
                                    + currentBounds.name()
                    );
                }
            }

            existingBounds.add(currentBounds);
        }
    }

    private MapElementBounds toMapElementBounds(
            Element element
    ) {
        Pair<Integer, Integer> location = element.getLocation();
        Pair<Integer, Integer> elementSize = element.getSize();

        return new MapElementBounds(
                safeElementName(element.getName()),
                location.getFirst(),
                location.getSecond(),
                elementSize.getFirst(),
                elementSize.getSecond()
        );
    }

    private String safeElementName(String elementName) {
        if (elementName == null || elementName.isBlank()) {
            return "Unnamed element";
        }

        return elementName.trim();
    }

    private record MapElementBounds(
            String name,
            int x,
            int y,
            int width,
            int height
    ) {
        private boolean overlaps(MapElementBounds other) {
            return x < other.x + other.width
                    && x + width > other.x
                    && y < other.y + other.height
                    && y + height > other.y;
        }
    }


}
