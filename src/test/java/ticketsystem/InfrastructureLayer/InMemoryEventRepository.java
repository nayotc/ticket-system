package ticketsystem.InfrastructureLayer;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import ticketsystem.ApplicationLayer.DiscountPolicyMapper;
import ticketsystem.ApplicationLayer.PurchasePolicyMapper;
import ticketsystem.DTO.DiscountPolicyDTO;
import ticketsystem.DTO.PurchasePolicyDTO;
import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.SearchCriteria;
import ticketsystem.DomainLayer.event.EventSearchResultView;
import ticketsystem.DomainLayer.event.SaleStatus;
import ticketsystem.DomainLayer.event.Seat.SeatStatus;
import ticketsystem.DomainLayer.event.SeatPosition;

/**
 * Test-only in-memory implementation of IEventRepository.
 * 
 * It assigns event IDs similarly to a database-generated ID.
 * 
 * Policy state is stored as a DTO snapshot. This prevents an Event returned
 * from getEventById from changing the stored policy unless updateEvent is
 * called explicitly.
 * 
 * This makes the repository suitable for testing EventService policy use cases
 * before purchase and discount policies are mapped with JPA.
 */
public final class InMemoryEventRepository implements IEventRepository {

    private static final Field EVENT_ID_FIELD = findEventIdField();

    private final Map<Long, StoredEvent> events = new LinkedHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1L);

    private final DiscountPolicyMapper discountPolicyMapper = new DiscountPolicyMapper();

    private final PurchasePolicyMapper purchasePolicyMapper = new PurchasePolicyMapper();

    @Override
    public synchronized void addEvent(Event event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        Long eventId = event.getId();

        if (eventId == null) {
            eventId = nextId.getAndIncrement();
            assignGeneratedId(event, eventId);
        } else {
            long existingId = eventId;
            nextId.updateAndGet(current -> Math.max(current, existingId + 1));
        }

        if (events.containsKey(eventId)) {
            throw new IllegalArgumentException(
                    "Event with ID " + eventId + " already exists");
        }

        events.put(eventId, new StoredEvent(event));
    }

    @Override
    public synchronized Event getEventById(Long eventId) {
        if (eventId == null) {
            return null;
        }

        StoredEvent storedEvent = events.get(eventId);

        if (storedEvent == null) {
            return null;
        }

        return storedEvent.load();
    }

    @Override
    public synchronized void updateEvent(Event event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        Long eventId = event.getId();

        if (eventId == null) {
            throw new IllegalArgumentException(
                    "Cannot update an event without an ID");
        }

        StoredEvent storedEvent = events.get(eventId);

        if (storedEvent == null) {
            throw new IllegalArgumentException(
                    "Event with ID " + eventId + " does not exist");
        }

        storedEvent.update(event);
    }

    @Override
    public synchronized void deleteEvent(
            Long eventId,
            long expectedVersion) {
        StoredEvent storedEvent = events.get(eventId);

        if (storedEvent == null) {
            throw new IllegalArgumentException(
                    "Event with ID " + eventId + " does not exist");
        }

        Event event = storedEvent.load();

        if (event.getVersion() != expectedVersion) {
            throw new IllegalStateException(
                    "Event was updated by another request");
        }

        events.remove(eventId);
    }

    @Override
    public synchronized List<Event> getEventsByCompanyId(Long companyId) {
        List<Event> result = new ArrayList<>();

        for (StoredEvent storedEvent : events.values()) {
            Event event = storedEvent.load();

            if (Objects.equals(event.getCompanyId(), companyId)) {
                result.add(event);
            }
        }

        return result;
    }

    @Override
    public synchronized List<Event> getAllEvents() {
        List<Event> result = new ArrayList<>();

        for (StoredEvent storedEvent : events.values()) {
            result.add(storedEvent.load());
        }

        return result;
    }

    @Override
    public synchronized List<EventSearchResultView> searchEvents(
            SearchCriteria criteria,
            List<Long> companyIds) {
        throw new UnsupportedOperationException(
                "Catalog search is not supported by the policy test repository");
    }

    @Override
    public synchronized List<EventSearchResultView> getFeaturedEvents(int limit) {
        throw new UnsupportedOperationException(
                "Featured event search is not supported by the policy test repository");
    }

    /**
     * Test helper that verifies whether EventService requested persistence.
     */
    public synchronized int getUpdateCount(Long eventId) {
        StoredEvent storedEvent = events.get(eventId);

        if (storedEvent == null) {
            return 0;
        }

        return storedEvent.updateCount;
    }

    public synchronized boolean containsEvent(Long eventId) {
        return events.containsKey(eventId);
    }

    public synchronized void clear() {
        events.clear();
        nextId.set(1L);
    }

    private final class StoredEvent {

        private Event event;
        private DiscountPolicyDTO discountPolicySnapshot;
        private PurchasePolicyDTO purchasePolicySnapshot;
        private int updateCount;

        private StoredEvent(Event event) {
            this.event = event;
            capturePolicies(event);
        }

        private Event load() {
            restorePolicies(event);
            return event;
        }

        private void update(Event updatedEvent) {
            this.event = updatedEvent;
            capturePolicies(updatedEvent);
            updateCount++;
        }

        private void capturePolicies(Event source) {
            discountPolicySnapshot = source.getDiscountPolicy() == null
                    ? null
                    : discountPolicyMapper.toDTO(
                            source.getDiscountPolicy());

            purchasePolicySnapshot = source.getPurchasePolicy() == null
                    ? null
                    : purchasePolicyMapper.toDTO(
                            source.getPurchasePolicy());
        }

        private void restorePolicies(Event target) {
            target.setDiscountPolicy(
                    discountPolicySnapshot == null
                            ? null
                            : discountPolicyMapper.toDomain(
                                    discountPolicySnapshot));

            target.setPurchasePolicy(
                    purchasePolicySnapshot == null
                            ? null
                            : purchasePolicyMapper.toDomain(
                                    purchasePolicySnapshot));
        }
    }

    private static Field findEventIdField() {
        try {
            Field field = Event.class.getDeclaredField("id");
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException exception) {
            throw new IllegalStateException(
                    "Event no longer contains an id field",
                    exception);
        }
    }

    private static void assignGeneratedId(Event event, Long eventId) {
        try {
            EVENT_ID_FIELD.set(event, eventId);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException(
                    "Failed to assign generated event ID",
                    exception);
        }
    }

    @Override
    public synchronized void updateSeatStatus(
            Long eventId,
            Long areaId,
            int row,
            int number,
            SeatStatus newStatus) {
        StoredEvent storedEvent = events.get(eventId);

        if (storedEvent == null) {
            throw new IllegalArgumentException(
                    "Event with ID " + eventId + " does not exist");
        }

        Event event = storedEvent.load();
        SeatPosition position = new SeatPosition(row, number);

        switch (newStatus) {
            case RESERVED -> event.reserveSeat(areaId, position);
            case AVAILABLE -> event.releaseSeat(areaId, position);
            case SOLD -> event.sellSeat(areaId, position);
            default -> throw new IllegalArgumentException(
                    "Unsupported seat status: " + newStatus);
        }

        storedEvent.update(event);
    }

    @Override
    public synchronized void updateStandingAreaReservedCount(
            Long eventId,
            Long areaId,
            int reservedDelta) {
        StoredEvent storedEvent = events.get(eventId);

        if (storedEvent == null) {
            throw new IllegalArgumentException(
                    "Event with ID " + eventId + " does not exist");
        }

        Event event = storedEvent.load();

        if (reservedDelta > 0) {
            event.reserveSpot(areaId, reservedDelta);
        } else if (reservedDelta < 0) {
            event.releaseSpot(areaId, -reservedDelta);
        }

        storedEvent.update(event);
    }

    @Override
    public synchronized void markStandingTicketsAsSold(
            Long eventId,
            Long areaId,
            int quantity) {
        StoredEvent storedEvent = events.get(eventId);

        if (storedEvent == null) {
            throw new IllegalArgumentException(
                    "Event with ID " + eventId + " does not exist");
        }

        Event event = storedEvent.load();
        event.sellSpot(areaId, quantity);
        storedEvent.update(event);
    }

    @Override
    public synchronized void updateSaleStatus(
            Long eventId,
            SaleStatus saleStatus) {
        StoredEvent storedEvent = events.get(eventId);

        if (storedEvent == null) {
            throw new IllegalArgumentException(
                    "Event with ID " + eventId + " does not exist");
        }

        Event event = storedEvent.load();
        event.setSaleStatus(saleStatus);
        storedEvent.update(event);
    }

	@Override
        public Event getEventForReservation(Long eventId) {
            return getEventById(eventId);
        }
}