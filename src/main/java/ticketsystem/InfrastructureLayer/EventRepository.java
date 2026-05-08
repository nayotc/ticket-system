package ticketsystem.InfrastructureLayer;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.event.Event;

public class EventRepository implements IEventRepository {
    private AtomicLong currentId = new AtomicLong(1L);
    private final ConcurrentHashMap<Long, Event> eventStorage = new ConcurrentHashMap<>();

    public void addEvent(Event event) {
        // Implementation for adding an event
        eventStorage.put(event.getId(), event);
    }

    public long getNextId() {
        return currentId.getAndIncrement();
    }

    
}

