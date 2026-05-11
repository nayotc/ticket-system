package ticketsystem.InfrastructureLayer;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.event.Event;

public class EventRepository implements IEventRepository {
    private AtomicLong currentId = new AtomicLong(1L);
    private final ConcurrentHashMap<Long, Event> eventStorage = new ConcurrentHashMap<>();

    @Override
    public void addEvent(Event event) {
        // Implementation for adding an event
        eventStorage.put(event.getId(), event);
    }

    @Override
    public long getNextId() {
        return currentId.getAndIncrement();
    }

    @Override
    public Event getEventById(long eventId){
        return eventStorage.get(eventId);
    }
    
    @Override
    public void updateEvent(Event event) {
        eventStorage.put(event.getId(), event);
    }

    @Override
    public void deleteEvent(long eventId) {
        eventStorage.remove(eventId);
    }

    @Override
    public List<Event> getEventsByCompanyId(long companyId) {
        return eventStorage.values().stream()
                .filter(event -> event.getCompanyId() == companyId)
                .toList();
    }

    @Override
    public List<Event> getAllEvents() {
        return eventStorage.values().stream().toList();
    }



}

