package ticketsystem.DomainLayer.event;

import java.util.List;

public class EventMap {

    private Pair<Integer, Integer> size;
    private List<Element> elements;

    public EventMap(Pair<Integer, Integer> size) {
        this.size = size;
        this.elements = new java.util.ArrayList<>();
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
            if (element instanceof SeatingArea && element.getId() == areaId) {
                ((SeatingArea) element).reserveSeat(position);
                return;
            }
        }
        throw new IllegalArgumentException("Area not found");
    }

    public void releaseSeat(Long areaId, SeatPosition position) {
        for (Element element : elements) {
            if (element instanceof SeatingArea && element.getId() == areaId) {
                ((SeatingArea) element).releaseSeat(position);
                return;
            }
        }
        throw new IllegalArgumentException("Area not found");
    }

    public void sellSeat(Long areaId, SeatPosition position) {
        for (Element element : elements) {
            if (element instanceof SeatingArea && element.getId() == areaId) {
                ((SeatingArea) element).sellSeat(position);
                return;
            }
        }
        throw new IllegalArgumentException("Area not found");
    }

    public void reserveSpot(Long areaId) {
        for (Element element : elements) {
            if (element instanceof StandingArea && element.getId() == areaId) {
                ((StandingArea) element).reserveSpot();
                return;
            }
        }
        throw new IllegalArgumentException("Area not found");
    }

    public void releaseSpot(Long areaId) {
        for (Element element : elements) {
            if (element instanceof StandingArea && element.getId() == areaId) {
                ((StandingArea) element).releaseSpot();
                return;
            }
        }
        throw new IllegalArgumentException("Area not found");
    }

    public void sellSpot(Long areaId) {
        for (Element element : elements) {
            if (element instanceof StandingArea && element.getId() == areaId) {
                ((StandingArea) element).sellSpot();
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
    //     for (Element element : elements) {
    //         if (element instanceof Area) {
    //             if (!((Area) element).isSoldOut()) {
    //                 return false;
    //             }
    //         }
    //     }
    //     return true;
    // }

}
