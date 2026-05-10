package ticketsystem.DomainLayer.event;

import java.time.LocalDateTime;

public abstract class Area extends Element {

    public Area(Long id, String name, Pair<Integer, Integer> location, Pair<Integer, Integer> size) {
        super(id, name, location, size);
    }

    public abstract boolean isSoldOut();

}
