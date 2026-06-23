package ticketsystem.PersistenceTesting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;


import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.hibernate.Hibernate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ticketsystem.DomainLayer.discount.*;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.event.EventCategory;
import ticketsystem.DomainLayer.event.EventLocation;
import ticketsystem.DomainLayer.event.Pair;
import ticketsystem.DomainLayer.event.Seat;
import ticketsystem.DomainLayer.event.SeatPosition;
import ticketsystem.DomainLayer.event.SeatingArea;
import ticketsystem.DomainLayer.event.StandingArea;
import ticketsystem.DomainLayer.exception.OptimisticLockException;
import ticketsystem.DomainLayer.policy.AndPurchaseRule;
import ticketsystem.DomainLayer.policy.MaxTicketsRule;
import ticketsystem.DomainLayer.policy.MinAgeRule;
import ticketsystem.DomainLayer.policy.PurchasePolicy;
import ticketsystem.DomainLayer.SearchCriteria;
import ticketsystem.DomainLayer.event.EventSearchResultView;
import ticketsystem.InfrastructureLayer.EventRepository;

@DataJpaTest
@Import(EventRepository.class)
public class EventRepositoryPersistenceTest {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void GivenValidEvent_WhenAddEvent_ThenEventAndMapArePersisted() {
        Event event = createEvent(1L, "Test Event");

        eventRepository.addEvent(event);
        flushAndClear();

        assertNotNull(event.getId());

        Event loadedEvent = eventRepository.getEventById(event.getId());

        assertNotNull(loadedEvent);
        assertEquals(event.getId(), loadedEvent.getId());
        assertEquals("Test Event", loadedEvent.getName());
        assertEquals(1L, loadedEvent.getCompanyId());
        assertEquals(10L, loadedEvent.getOpenedBy());
        assertEquals(EventLocation.TEL_AVIV, loadedEvent.getLocation());
        assertEquals(EventCategory.CONCERT, loadedEvent.getCategory());
        assertEquals("Test Artist", loadedEvent.getArtistName());
        assertEquals(0, new BigDecimal("80.00").compareTo(loadedEvent.getMinimalTicketPrice()));
        assertEquals(new Pair<>(20, 15), loadedEvent.getMap().getSize());
        assertEquals(2, loadedEvent.getMap().getElements().size());

        SeatingArea seatingArea = getSeatingArea(loadedEvent);
        StandingArea standingArea = getStandingArea(loadedEvent);

        assertNotNull(seatingArea.getId());
        assertEquals("Main Seating Area", seatingArea.getName());
        assertEquals(2, seatingArea.getRows());
        assertEquals(2, seatingArea.getColumns());
        assertEquals(4, seatingArea.getSeats().size());
        assertEquals(0, new BigDecimal("150.00").compareTo(seatingArea.getPrice()));
        assertEquals(
                Seat.SeatStatus.AVAILABLE,
                seatingArea.getSeats()
                        .get(new SeatPosition(1, 1))
                        .getStatus()
        );

        assertNotNull(standingArea.getId());
        assertEquals("Main Standing Area", standingArea.getName());
        assertEquals(100, standingArea.getCapacity());
        assertEquals(0, standingArea.getReserved());
        assertEquals(0, standingArea.getSold());
        assertEquals(0, new BigDecimal("80.00").compareTo(standingArea.getPrice()));
    }

    @Test
    void GivenSavedEvent_WhenGetEventById_ThenDetachedCopyIsReturned() {
        Event event = createEvent(1L, "Original Name");

        eventRepository.addEvent(event);
        flushAndClear();

        Event firstResult = eventRepository.getEventById(event.getId());

        assertNotNull(firstResult);
        assertNotSame(event, firstResult);

        firstResult.setName("Changed Without Update");

        Event secondResult = eventRepository.getEventById(event.getId());

        assertNotNull(secondResult);
        assertEquals("Original Name", secondResult.getName());
    }

    @Test
    void GivenUnknownEventId_WhenGetEventById_ThenReturnNull() {
        Event loadedEvent = eventRepository.getEventById(999999L);

        assertNull(loadedEvent);
    }

    @Test
    void GivenExistingEvent_WhenAddEventAgain_ThenThrowException() {
        Event event = createEvent(1L, "Duplicate Event");

        eventRepository.addEvent(event);
        flushAndClear();

        Event existingEvent = eventRepository.getEventById(event.getId());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventRepository.addEvent(existingEvent)
        );

        assertTrue(exception.getMessage().contains("Event already exists"));
    }

    @Test
    void GivenSavedEvent_WhenUpdateEvent_ThenChangesAndVersionArePersisted() {
        Event event = createEvent(1L, "Original Event");

        eventRepository.addEvent(event);
        flushAndClear();

        Event eventToUpdate = eventRepository.getEventById(event.getId());
        int originalVersion = eventToUpdate.getVersion();

        eventToUpdate.setName("Updated Event");
        eventToUpdate.setArtistName("Updated Artist");

        eventRepository.updateEvent(eventToUpdate);
        flushAndClear();

        Event updatedEvent = eventRepository.getEventById(event.getId());

        assertNotNull(updatedEvent);
        assertEquals("Updated Event", updatedEvent.getName());
        assertEquals("Updated Artist", updatedEvent.getArtistName());
        assertEquals(originalVersion + 1, updatedEvent.getVersion());
    }

    @Test
    void GivenDetachedEventCopy_WhenSeatIsUpdated_ThenSeatRelationshipAndStatusArePersisted() {
        Event event = createEvent(1L, "Copied Seating Area Event");
        eventRepository.addEvent(event);
        Long eventId = event.getId();
        flushAndClear();

        Event detachedEvent = eventRepository.getEventById(eventId);
        assertNotNull(detachedEvent);

        SeatingArea detachedSeatingArea = getSeatingArea(detachedEvent);
        SeatPosition position = new SeatPosition(1, 1);

        detachedSeatingArea.reserveSeat(position);
        eventRepository.updateEvent(detachedEvent);
        flushAndClear();

        Event reloadedEvent = eventRepository.getEventById(eventId);
        assertNotNull(reloadedEvent);

        SeatingArea reloadedSeatingArea = getSeatingArea(reloadedEvent);

        assertEquals(4, reloadedSeatingArea.getSeats().size());
        assertEquals(
                Seat.SeatStatus.RESERVED,
                reloadedSeatingArea.getSeats().get(position).getStatus()
        );
    }

    @Test
    void GivenSavedEvent_WhenDeleteWithCorrectVersion_ThenEventIsDeleted() {
        Event event = createEvent(1L, "Event To Delete");

        eventRepository.addEvent(event);
        flushAndClear();

        Event savedEvent = eventRepository.getEventById(event.getId());

        eventRepository.deleteEvent(
                savedEvent.getId(),
                savedEvent.getVersion()
        );
        flushAndClear();

        assertNull(eventRepository.getEventById(savedEvent.getId()));
    }

    @Test
    void GivenSavedEvent_WhenDeleteWithWrongVersion_ThenThrowAndKeepEvent() {
        Event event = createEvent(1L, "Protected Event");

        eventRepository.addEvent(event);
        flushAndClear();

        Event savedEvent = eventRepository.getEventById(event.getId());

        OptimisticLockException exception = assertThrows(
                OptimisticLockException.class,
                () -> eventRepository.deleteEvent(
                        savedEvent.getId(),
                        savedEvent.getVersion() + 1L
                )
        );

        assertTrue(
                exception.getMessage()
                        .contains("Event was modified by another request")
        );
        assertNotNull(eventRepository.getEventById(savedEvent.getId()));
    }

    @Test
    void GivenEventsFromDifferentCompanies_WhenGetEventsByCompanyId_ThenReturnOnlyMatchingEvents() {
        Event firstCompanyEvent = createEvent(1L, "Company One Event");
        Event secondCompanyEvent = createEvent(2L, "Company Two Event");
        Event anotherFirstCompanyEvent = createEvent(
                1L,
                "Another Company One Event"
        );

        eventRepository.addEvent(firstCompanyEvent);
        eventRepository.addEvent(secondCompanyEvent);
        eventRepository.addEvent(anotherFirstCompanyEvent);
        flushAndClear();

        List<Event> companyEvents =
                eventRepository.getEventsByCompanyId(1L);

        assertEquals(2, companyEvents.size());
        assertTrue(
                companyEvents.stream()
                        .allMatch(event -> event.getCompanyId().equals(1L))
        );
        assertTrue(
                companyEvents.stream()
                        .anyMatch(event -> event.getName()
                                .equals("Company One Event"))
        );
        assertTrue(
                companyEvents.stream()
                        .anyMatch(event -> event.getName()
                                .equals("Another Company One Event"))
        );
        assertFalse(
                companyEvents.stream()
                        .anyMatch(event -> event.getName()
                                .equals("Company Two Event"))
        );
    }

    @Test
    void GivenSeveralSavedEvents_WhenGetAllEvents_ThenReturnAllEvents() {
        Event firstEvent = createEvent(1L, "First Event");
        Event secondEvent = createEvent(2L, "Second Event");

        eventRepository.addEvent(firstEvent);
        eventRepository.addEvent(secondEvent);
        flushAndClear();

        List<Event> events = eventRepository.getAllEvents();

        assertEquals(2, events.size());
        assertTrue(
                events.stream()
                        .anyMatch(event -> event.getId()
                                .equals(firstEvent.getId()))
        );
        assertTrue(
                events.stream()
                        .anyMatch(event -> event.getId()
                                .equals(secondEvent.getId()))
        );
    }

    @Test
    void GivenNoEventsForCompany_WhenGetEventsByCompanyId_ThenReturnEmptyList() {
        Event event = createEvent(1L, "Existing Event");

        eventRepository.addEvent(event);
        flushAndClear();

        List<Event> events =
                eventRepository.getEventsByCompanyId(999L);

        assertNotNull(events);
        assertTrue(events.isEmpty());
    }

    @Test
    void GivenNullEvent_WhenAddEvent_ThenThrowException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventRepository.addEvent(null)
        );

        assertEquals("Event cannot be null", exception.getMessage());
    }

    @Test
    void GivenNullEvent_WhenUpdateEvent_ThenThrowException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventRepository.updateEvent(null)
        );

        assertEquals("Event cannot be null", exception.getMessage());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void GivenTwoDetachedCopies_WhenUpdatingStaleCopy_ThenThrowOptimisticLockExceptionAndKeepFirstUpdate() {
        Event event = createEvent(1L, "Original Event");

        eventRepository.addEvent(event);

        Event firstCopy = eventRepository.getEventById(event.getId());
        Event staleSecondCopy = eventRepository.getEventById(event.getId());

        assertNotNull(firstCopy);
        assertNotNull(staleSecondCopy);
        assertNotSame(firstCopy, staleSecondCopy);

        int originalVersion = firstCopy.getVersion();

        firstCopy.setName("First Successful Update");
        eventRepository.updateEvent(firstCopy);

        staleSecondCopy.setName("Stale Update");

        OptimisticLockException exception = assertThrows(
                OptimisticLockException.class,
                () -> eventRepository.updateEvent(staleSecondCopy)
        );

        assertTrue(
                exception.getMessage()
                        .contains("Event was modified by another request")
        );

        Event persistedEvent = eventRepository.getEventById(event.getId());

        assertNotNull(persistedEvent);
        assertEquals("First Successful Update", persistedEvent.getName());
        assertEquals(originalVersion + 1, persistedEvent.getVersion());
    }

    @Test
    void GivenDuplicateSeatPositionInSameArea_WhenPersisted_ThenThrowPersistenceException() {
        Event event = createEvent(1L, "Duplicate Seat Event");
        eventRepository.addEvent(event);

        Long seatingAreaId = getSeatingArea(event).getId();
        assertNotNull(seatingAreaId);

        flushAndClear();

        SeatingArea managedSeatingArea = entityManager.find(
                SeatingArea.class,
                seatingAreaId
        );

        assertNotNull(managedSeatingArea);

        Seat duplicateSeat = new Seat(new SeatPosition(1, 1));
        duplicateSeat.setSeatingArea(managedSeatingArea);

        assertThrows(PersistenceException.class, () -> {
            entityManager.persist(duplicateSeat);
            entityManager.flush();
        });
    }

    @Test
    void GivenAreasWithDifferentPrices_WhenPersistedAndReloaded_ThenEachPriceIsRestored() {
        Event event = createEvent(1L, "Area Price Event");

        eventRepository.addEvent(event);
        Long eventId = event.getId();

        assertNotNull(eventId);

        flushAndClear();

        Event loadedEvent = eventRepository.getEventById(eventId);

        assertNotNull(loadedEvent);

        SeatingArea seatingArea = getSeatingArea(loadedEvent);
        StandingArea standingArea = getStandingArea(loadedEvent);

        assertEquals(0, new BigDecimal("150.00").compareTo(seatingArea.getPrice()));
        assertEquals(0, new BigDecimal("80.00").compareTo(standingArea.getPrice()));
    }

    @Test
    void GivenOneAreaMatchesPriceRange_WhenSearchEvents_ThenEventIsReturned() {
        Event matchingEvent = createEvent(1L, "Matching Event");
        matchingEvent.setStatus(Event.eventStatus.ACTIVE);

        Event expensiveEvent = createEvent(1L, "Expensive Event");
        expensiveEvent.setStatus(Event.eventStatus.ACTIVE);

        getSeatingArea(expensiveEvent).setPrice(new BigDecimal("200.00"));
        getStandingArea(expensiveEvent).setPrice(new BigDecimal("250.00"));

        eventRepository.addEvent(matchingEvent);
        eventRepository.addEvent(expensiveEvent);

        Long matchingEventId = matchingEvent.getId();

        flushAndClear();

        SearchCriteria criteria = new SearchCriteria();
        criteria.setMinPrice(new BigDecimal("70.00"));
        criteria.setMaxPrice(new BigDecimal("100.00"));

        List<EventSearchResultView> results =
                eventRepository.searchEvents(
                        criteria,
                        List.of(1L)
                );

        assertEquals(1, results.size());
        assertEquals(matchingEventId, results.get(0).getId());
        assertEquals(0, new BigDecimal("80.00").compareTo(results.get(0).getTicketPrice()));
    }

    @Test
    void GivenAreaRowWithoutPrice_WhenPersisted_ThenDatabaseRejectsIt() {
        assertThrows(PersistenceException.class, () -> {
            entityManager
                    .getEntityManager()
                    .createNativeQuery("""
                    INSERT INTO event_elements (
                        element_type,
                        name,
                        location_x,
                        location_y,
                        size_width,
                        size_height,
                        price
                    )
                    VALUES (
                        'SEATING',
                        'Invalid seating area',
                        0,
                        0,
                        10,
                        10,
                        NULL
                    )
                    """)
                    .executeUpdate();

            entityManager.flush();
        });
    }

    // ------------------- Purchase Policy Tests ------------------

    @Test
    void GivenEventWithNestedPurchasePolicy_WhenSavedAndReloaded_ThenPolicyIsPreserved() {
        Event event = createEvent(1L, "Policy Event");

        event.setPurchasePolicy(
                new PurchasePolicy(
                        new AndPurchaseRule(
                                List.of(
                                        new MinAgeRule(18),
                                        new MaxTicketsRule(5)
                                )
                        )
                )
        );

        eventRepository.addEvent(event);

        Long eventId = event.getId();
        assertNotNull(eventId);

        flushAndClear();

        Event loadedEvent = eventRepository.getEventById(eventId);

        assertNotNull(loadedEvent);
        assertNotSame(event, loadedEvent);
        assertNotNull(loadedEvent.getPurchasePolicy());
        assertTrue(
                loadedEvent.getPurchasePolicy()
                        .validate(5, 18)
                        .isAllowed()
        );
        assertFalse(
                loadedEvent.getPurchasePolicy()
                        .validate(6, 18)
                        .isAllowed()
        );
        assertFalse(
                loadedEvent.getPurchasePolicy()
                        .validate(5, 17)
                        .isAllowed()
        );
    }

    // ------------------- Discount Policy Tests ------------------

    @Test
    void GivenEventWithDiscountPolicy_WhenSavedAndReloaded_ThenPolicyIsPreserved() {
        Event event = createEvent(1L, "Discount Event");

        DiscountPolicy policy =
                new DiscountPolicy(
                        DiscountCompositionType.MAX
                );

        policy.addDiscount(
                new VisibleDiscount(
                        "Visible discount",
                        new BigDecimal("10")
                )
        );

        policy.addDiscount(
                new ConditionalDiscount(
                        "Conditional discount",
                        new BigDecimal("25"),
                        new AndDiscountCondition(
                                List.of(
                                        new MinTicketsCondition(2),
                                        new MaxTicketsCondition(5)
                                )
                        )
                )
        );

        policy.addDiscount(
                new CouponDiscount(
                        "Coupon discount",
                        "SAVE30",
                        new BigDecimal("30"),
                        LocalDateTime.now().plusDays(5)
                )
        );

        event.setDiscountPolicy(policy);

        eventRepository.addEvent(event);
        flushAndClear();

        Event loadedEvent =
                eventRepository.getEventById(event.getId());

        assertNotNull(loadedEvent);
        assertNotNull(loadedEvent.getDiscountPolicy());
        assertEquals(
                DiscountCompositionType.MAX,
                loadedEvent.getDiscountPolicy()
                        .getDiscountCompositionType()
        );
        assertEquals(
                3,
                loadedEvent.getDiscountPolicy()
                        .getDiscounts()
                        .size()
        );

        BigDecimal discount =
                loadedEvent.getDiscountPolicy()
                        .calculateDiscount(
                                new BigDecimal("100.00"),
                                3,
                                "SAVE30"
                        );

        assertEquals(
                0,
                new BigDecimal("30.00")
                        .compareTo(discount)
        );
    }

    @Test
    void GivenEventWithDiscountPolicy_WhenPolicyIsReplaced_ThenOldPolicyGraphIsDeleted() {
        Event event = createEvent(
                1L,
                "Discount policy replacement event"
        );

        DiscountPolicy originalPolicy =
                createNestedDiscountPolicy();

        event.setDiscountPolicy(originalPolicy);

        eventRepository.addEvent(event);
        flushAndClear();

        Event persistedEvent =
                eventRepository.getEventById(event.getId());

        assertNotNull(persistedEvent);
        assertNotNull(persistedEvent.getDiscountPolicy());

        DiscountPolicy persistedOriginalPolicy =
                persistedEvent.getDiscountPolicy();

        Long originalPolicyId =
                persistedOriginalPolicy.getId();

        List<Long> originalDiscountIds =
                persistedOriginalPolicy.getDiscounts()
                        .stream()
                        .map(DiscountTypes::getId)
                        .toList();

        List<Long> originalConditionIds =
                collectConditionIds(
                        persistedOriginalPolicy
                );

        assertNotNull(originalPolicyId);
        assertEquals(3, originalDiscountIds.size());
        assertEquals(5, originalConditionIds.size());

        DiscountPolicy replacementPolicy =
                new DiscountPolicy(
                        DiscountCompositionType.MAX
                );

        replacementPolicy.addDiscount(
                new VisibleDiscount(
                        "Replacement discount",
                        new BigDecimal("15")
                )
        );

        persistedEvent.setDiscountPolicy(
                replacementPolicy
        );

        eventRepository.updateEvent(persistedEvent);
        flushAndClear();

        Event updatedEvent =
                eventRepository.getEventById(event.getId());

        assertNotNull(updatedEvent);
        assertNotNull(updatedEvent.getDiscountPolicy());

        assertNotEquals(
                originalPolicyId,
                updatedEvent.getDiscountPolicy().getId()
        );

        assertEquals(
                1,
                updatedEvent.getDiscountPolicy()
                        .getDiscounts()
                        .size()
        );

        assertNull(
                entityManager.find(
                        DiscountPolicy.class,
                        originalPolicyId
                )
        );

        for (Long discountId : originalDiscountIds) {
            assertNull(
                    entityManager.find(
                            DiscountTypes.class,
                            discountId
                    )
            );
        }

        for (Long conditionId : originalConditionIds) {
            assertNull(
                    entityManager.find(
                            DiscountCondition.class,
                            conditionId
                    )
            );
        }
    }

    // ------------------- Helper Methods ------------------

    private Event createEvent(Long companyId, String eventName) {
        Event event = new Event(
                LocalDateTime.now().plusDays(30),
                eventName,
                companyId,
                10L,
                EventLocation.TEL_AVIV,
                100L,
                EventCategory.CONCERT,
                "Test Artist",
                new BigDecimal("100.00"),
                new Pair<>(20, 15)
        );

        event.getMap().addElement(
                new SeatingArea(
                        "Main Seating Area",
                        new Pair<>(0, 0),
                        new Pair<>(10, 10),
                        2,
                        2,
                        new BigDecimal("150.00")
                )
        );

        event.getMap().addElement(
                new StandingArea(
                        "Main Standing Area",
                        new Pair<>(10, 0),
                        new Pair<>(10, 10),
                        100,
                        new BigDecimal("80.00")
                )
        );

        return event;
    }

    private SeatingArea getSeatingArea(Event event) {
        return event.getMap()
                .getElements()
                .stream()
                .filter(SeatingArea.class::isInstance)
                .map(SeatingArea.class::cast)
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException(
                                "Seating area was not found"
                        )
                );
    }

    private StandingArea getStandingArea(Event event) {
        return event.getMap()
                .getElements()
                .stream()
                .filter(StandingArea.class::isInstance)
                .map(StandingArea.class::cast)
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException(
                                "Standing area was not found"
                        )
                );
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private DiscountPolicy createNestedDiscountPolicy() {
        LocalDateTime now = LocalDateTime.now();

        DiscountPolicy policy =
                new DiscountPolicy(
                        DiscountCompositionType.SUM
                );

        policy.addDiscount(
                new VisibleDiscount(
                        "Visible discount",
                        new BigDecimal("10")
                )
        );

        policy.addDiscount(
                new ConditionalDiscount(
                        "Conditional discount",
                        new BigDecimal("20"),
                        new AndDiscountCondition(
                                List.of(
                                        new MinTicketsCondition(2),
                                        new AndDiscountCondition(
                                                List.of(
                                                        new MaxTicketsCondition(4),
                                                        new DateRangeCondition(
                                                                now.minusDays(1),
                                                                now.plusDays(1)
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );

        policy.addDiscount(
                new CouponDiscount(
                        "Coupon discount",
                        "SAVE30",
                        new BigDecimal("30"),
                        now.plusDays(2)
                )
        );

        return policy;
    }

    private List<Long> collectConditionIds(
            DiscountPolicy policy
    ) {
        List<Long> ids = new ArrayList<>();

        for (DiscountTypes discount :
                policy.getDiscounts()) {
            DiscountTypes unproxiedDiscount =
                    (DiscountTypes)
                            Hibernate.unproxy(discount);

            if (unproxiedDiscount instanceof
                    ConditionalDiscount conditionalDiscount) {
                collectConditionIds(
                        conditionalDiscount.getCondition(),
                        ids
                );
            }
        }

        return ids;
    }

    private void collectConditionIds(
            DiscountCondition condition,
            List<Long> ids
    ) {
        DiscountCondition unproxiedCondition =
                (DiscountCondition)
                        Hibernate.unproxy(condition);

        ids.add(unproxiedCondition.getId());

        if (unproxiedCondition instanceof
                AndDiscountCondition andCondition) {
            for (DiscountCondition child :
                    andCondition.getConditions()) {
                collectConditionIds(child, ids);
            }
        }
    }
}