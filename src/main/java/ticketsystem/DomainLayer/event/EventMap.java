package ticketsystem.DomainLayer.event;

import java.util.List;

public class EventMap {
    private Pair<Integer, Integer> size;
    private List<Element> elements;

    public EventMap(Pair<Integer, Integer> size) {
        this.size = size;
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

}
