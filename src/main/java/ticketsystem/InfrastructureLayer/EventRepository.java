package ticketsystem.InfrastructureLayer;

import java.util.List;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.exception.OptimisticLockException;
import ticketsystem.InfrastructureLayer.persistence.EventJpaRepository;

@Repository
public class EventRepository implements IEventRepository {

    private final EventJpaRepository eventJpaRepository;

    public EventRepository(EventJpaRepository eventJpaRepository) {
        this.eventJpaRepository = eventJpaRepository;
    }

    @Override
    @Transactional
    public void addEvent(Event event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        if (event.getId() != null && eventJpaRepository.existsById(event.getId())) {
            throw new IllegalArgumentException("Event already exists with id: " + event.getId());
        }

        eventJpaRepository.save(event);
    }

    @Override
    @Transactional(readOnly = true)
    public Event getEventById(Long eventId) {
        return eventJpaRepository.findByIdWithMap(eventId)
                .map(Event::copy)
                .orElse(null);
    }

    @Override
    @Transactional
    public void updateEvent(Event event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        if (!eventJpaRepository.existsById(event.getId())) {
            throw new IllegalArgumentException("Event not found with id: " + event.getId());
        }

        try {
            eventJpaRepository.save(event);
        } catch (OptimisticLockingFailureException exception) {
            throw new OptimisticLockException(
                    "Event was modified by another request.\nEvent id: " + event.getId()
            );
        }
    }

    @Override
    @Transactional
    public void deleteEvent(Long eventId, long expectedVersion) {
        Event event = eventJpaRepository.findByIdWithMap(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with id: " + eventId));

        if (event.getVersion() != expectedVersion) {
            throw new OptimisticLockException(
                    "Event was modified by another request.\nEvent id: " + eventId
            );
        }

        try {
            eventJpaRepository.delete(event);
        } catch (OptimisticLockingFailureException exception) {
            throw new OptimisticLockException(
                    "Event was modified by another request.\nEvent id: " + eventId
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Event> getEventsByCompanyId(Long companyId) {
        return eventJpaRepository.findByCompanyIdWithMap(companyId).stream()
                .map(Event::copy)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Event> getAllEvents() {
        return eventJpaRepository.findAllWithMap().stream()
                .map(Event::copy)
                .toList();
    }
}