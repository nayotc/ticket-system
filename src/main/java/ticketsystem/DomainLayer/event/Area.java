package ticketsystem.DomainLayer.event;

public abstract class Area extends Element {

    public Area(String name, Pair<Integer, Integer> location, Pair<Integer, Integer> size) {
        super(name, location, size);
    }
    
}
