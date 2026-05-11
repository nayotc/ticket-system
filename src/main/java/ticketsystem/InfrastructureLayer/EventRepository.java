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
    public Event getEventById(long eventId) {
        return eventStorage.get(eventId);
    }

    @Override
    public void updateEvent(Event event) {
        long id = event.getId();
        eventStorage.compute(id, (key, existingEvent) -> {
            if (existingEvent == null) {
                throw new RuntimeException("Event not found in Database");
            }

            if (event.getVersion() != existingEvent.getVersion()) {
                throw new RuntimeException("Version mismatch - possible concurrent modification");
            }
            event.incrementVersion();
            return event;
        });
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
