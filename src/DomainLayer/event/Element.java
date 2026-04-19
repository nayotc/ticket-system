package DomainLayer.event;

public class Element {
    private String name;
    private pair<Integer, Integer> location;
    private pair<Integer, Integer> size;

    public Element(String name, pair<Integer, Integer> location, pair<Integer, Integer> size) {
        this.name = name;
        this.location = location;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public pair<Integer, Integer> getLocation() {
        return location;
    }

    public void setLocation(pair<Integer, Integer> location) {
        this.location = location;
    }

    public pair<Integer, Integer> getSize() {
        return size;
    }

    public void setSize(pair<Integer, Integer> size) {
        this.size = size;
    }

}
