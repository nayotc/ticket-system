package ticketsystem.InfrastructureLayer;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.event.Event;

public class EventRepository implements IEventRepository {
    private Long maxId = 1L;
    private final ConcurrentHashMap<Long, Event> eventStorage = new ConcurrentHashMap<>();

    public void addEvent(Event event) {
        // Implementation for adding an event
        eventStorage.put(event.getId(), event);
    }

    public long getMaxId() {
        Long currentMaxId = maxId;
        maxId++;
        return currentMaxId;
    }

    
}

