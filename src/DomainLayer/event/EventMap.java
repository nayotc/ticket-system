package DomainLayer.event;

public class EventMap {
    private pair<Integer, Integer> size;
    private List<Element> elements;

    public EventMap(pair<Integer, Integer> size) {
        this.size = size;
    }

    public pair<Integer, Integer> getSize() {
        return size;
    }

    public void setSize(pair<Integer, Integer> size) {
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
