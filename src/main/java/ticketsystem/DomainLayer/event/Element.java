package ticketsystem.DomainLayer.event;

public class Element {
    private final long id;
    private String name;
    private Pair<Integer, Integer> location;
    private Pair<Integer, Integer> size;

    public Element(long id, String name, Pair<Integer, Integer> location, Pair<Integer, Integer> size) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.size = size;
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
