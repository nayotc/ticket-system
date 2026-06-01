package ticketsystem.InfrastructureLayer;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.exception.OptimisticLockException;

@Repository
public class EventRepository implements IEventRepository {

    private AtomicLong currentId = new AtomicLong(1L);
    private final ConcurrentHashMap<Long, Event> eventStorage = new ConcurrentHashMap<>();

    @Override
    public void addEvent(Event event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        Event copy = event.copy();
        Event existing = eventStorage.putIfAbsent(event.getId(), copy);

        if (existing != null) {
            throw new IllegalArgumentException("Event already exists with id: " + event.getId());
        }
    }

    @Override
    public long getNextId() {
        return currentId.getAndIncrement();
    }

    @Override
    public Event getEventById(Long eventId){
        Event event = eventStorage.get(eventId);

        if (event == null) {
            return null;
        }

        return event.copy();
    }

    @Override
    public void updateEvent(Event event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        eventStorage.compute(event.getId(), (id, currentEvent) -> {
            if (currentEvent == null) {
                throw new IllegalArgumentException("Event not found with id: " + id);
            }

            if (currentEvent.getVersion() != event.getVersion()) {
                throw new OptimisticLockException(
                        "Event was modified by another request. Event id: " + id
                );
            }

            Event copy = event.copy();
            copy.incrementVersion();
            return copy;
        });
    }

    @Override
    public void deleteEvent(Long eventId, long expectedVersion) {
        eventStorage.compute(eventId, (id, currentEvent) -> {
            if (currentEvent == null) {
                throw new IllegalArgumentException("Event not found with id: " + id);
            }

            if (currentEvent.getVersion() != expectedVersion) {
                throw new OptimisticLockException(
                        "Event was modified by another request. Event id: " + id
                );
            }

            return null;
        });
    }

    @Override
    public List<Event> getEventsByCompanyId(Long companyId) {
        return eventStorage.values().stream()
            .filter(event -> event.getCompanyId() != null)
            .filter(event -> event.getCompanyId().equals(companyId))
            .map(Event::copy)
            .toList();
    }

    @Override
    public List<Event> getAllEvents() {
        return eventStorage.values().stream()
            .map(Event::copy)
            .toList();
    }

}
