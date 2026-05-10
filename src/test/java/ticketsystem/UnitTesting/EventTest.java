package ticketsystem.UnitTesting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.DomainLayer.event.EventCategory;
import ticketsystem.DomainLayer.event.EventMap;
import ticketsystem.DomainLayer.event.Pair;
import ticketsystem.DomainLayer.event.PurchasePolicy;
import ticketsystem.DomainLayer.event.SeatPosition;
import ticketsystem.DomainLayer.event.DiscountPolicy;
import ticketsystem.DomainLayer.event.Event;

public class EventTest {
    private Event event;

    private final Long eventId = 1L;
    private final Long companyId = 10L;
    private final Long openedBy = 100L;
    private final LocalDateTime eventDate = LocalDateTime.now().plusDays(10);

    @BeforeEach
    void setUp() {
        event = new Event(
                eventId,
                eventDate,
                "Music Festival",
                companyId,
                openedBy,
                "Central Park",
                3L,
                EventCategory.CONCERT,
                new Pair<>(10, 10)
        );
    }

    @Test
    void givenValidDetails_whenCreateEvent_thenFieldsAreInitializedCorrectly() {
        assertEquals(eventId, event.getId());
        assertEquals("Music Festival", event.getName());
        assertEquals(eventDate, event.getDate());
        assertEquals(companyId, event.getCompanyId());
        assertEquals(openedBy, event.getOpenedBy());
        assertEquals("Central Park", event.getLocation());
        assertEquals(3L, event.getTrafficThreshold());
        assertEquals(EventCategory.CONCERT, event.getCategory());

        assertNotNull(event.getMap());
        assertNotNull(event.getPurchasePolicy());
        assertNotNull(event.getDiscountPolicy());
        assertEquals(0, event.getActiveReservationsCount());
    }

    @Test
    void givenEvent_whenSetName_thenNameIsUpdated() {
        event.setName("Updated Festival");

        assertEquals("Updated Festival", event.getName());
    }

    @Test
    void givenEvent_whenSetDate_thenDateIsUpdated() {
        LocalDateTime newDate = LocalDateTime.now().plusDays(20);

        event.setDate(newDate);

        assertEquals(newDate, event.getDate());
    }

    @Test
    void givenEvent_whenSetCompanyId_thenCompanyIdIsUpdated() {
        event.setCompanyId(99L);

        assertEquals(99L, event.getCompanyId());
    }

    @Test
    void givenEvent_whenSetLocation_thenLocationIsUpdated() {
        event.setLocation("Tel Aviv");

        assertEquals("Tel Aviv", event.getLocation());
    }

    @Test
    void givenEvent_whenSetTrafficThreshold_thenTrafficThresholdIsUpdated() {
        event.setTrafficThreshold(5L);

        assertEquals(5L, event.getTrafficThreshold());
    }

    @Test
    void givenEvent_whenSetCategory_thenCategoryIsUpdated() {
        event.setCategory(EventCategory.SPORTS);

        assertEquals(EventCategory.SPORTS, event.getCategory());
    }

    @Test
    void givenEvent_whenSetMap_thenMapIsUpdated() {
        EventMap mockMap = mock(EventMap.class);

        event.setMap(mockMap);

        assertSame(mockMap, event.getMap());
    }

    @Test
    void givenEvent_whenSetPurchasePolicy_thenPurchasePolicyIsUpdated() {
        PurchasePolicy policy = new PurchasePolicy("New policy");

        event.setPurchasePolicy(policy);

        assertSame(policy, event.getPurchasePolicy());
    }

    @Test
    void givenEvent_whenSetDiscountPolicy_thenDiscountPolicyIsUpdated() {
        DiscountPolicy policy = new DiscountPolicy();

        event.setDiscountPolicy(policy);

        assertSame(policy, event.getDiscountPolicy());
    }

    @Test
    void givenEventWithNoActiveReservations_whenCheckOverloaded_thenReturnFalse() {
        assertFalse(event.isOverloaded());
        assertEquals(0, event.getActiveReservationsCount());
    }

    @Test
    void givenActiveReservationsBelowThreshold_whenCheckOverloaded_thenReturnFalse() {
        event.incrementActiveReservations();
        event.incrementActiveReservations();

        assertFalse(event.isOverloaded());
        assertEquals(2, event.getActiveReservationsCount());
    }

    @Test
    void givenActiveReservationsEqualThreshold_whenCheckOverloaded_thenReturnTrue() {
        event.incrementActiveReservations();
        event.incrementActiveReservations();
        event.incrementActiveReservations();

        assertTrue(event.isOverloaded());
        assertEquals(3, event.getActiveReservationsCount());
    }

    @Test
    void givenActiveReservationsAboveThreshold_whenCheckOverloaded_thenReturnTrue() {
        event.incrementActiveReservations();
        event.incrementActiveReservations();
        event.incrementActiveReservations();
        event.incrementActiveReservations();

        assertTrue(event.isOverloaded());
        assertEquals(4, event.getActiveReservationsCount());
    }

    @Test
    void givenActiveReservations_whenDecrement_thenCountIsReduced() {
        event.incrementActiveReservations();
        event.incrementActiveReservations();

        event.decrementActiveReservations();

        assertEquals(1, event.getActiveReservationsCount());
    }

    @Test
    void givenZeroActiveReservations_whenDecrement_thenCountDoesNotGoBelowZero() {
        event.decrementActiveReservations();
        event.decrementActiveReservations();

        assertEquals(0, event.getActiveReservationsCount());
    }

    @Test
    void givenMockedMap_whenIsSoldOut_thenEventDelegatesToMap() {
        EventMap mockMap = mock(EventMap.class);
        event.setMap(mockMap);

        when(mockMap.isSoldOut()).thenReturn(true);

        assertTrue(event.isSoldOut());
        verify(mockMap, times(1)).isSoldOut();
    }

    @Test
    void givenSeatDetails_whenReserveSeat_thenEventDelegatesToMap() {
        EventMap mockMap = mock(EventMap.class);
        SeatPosition position = mock(SeatPosition.class);
        event.setMap(mockMap);

        event.reserveSeat(1L, position);

        verify(mockMap, times(1)).reserveSeat(1L, position);
    }

    @Test
    void givenSeatDetails_whenReleaseSeat_thenEventDelegatesToMap() {
        EventMap mockMap = mock(EventMap.class);
        SeatPosition position = mock(SeatPosition.class);
        event.setMap(mockMap);

        event.releaseSeat(1L, position);

        verify(mockMap, times(1)).releaseSeat(1L, position);
    }

    @Test
    void givenSeatDetails_whenSellSeat_thenEventDelegatesToMap() {
        EventMap mockMap = mock(EventMap.class);
        SeatPosition position = mock(SeatPosition.class);
        event.setMap(mockMap);

        event.sellSeat(1L, position);

        verify(mockMap, times(1)).sellSeat(1L, position);
    }

    @Test
    void givenAreaId_whenReserveSpot_thenEventDelegatesToMap() {
        EventMap mockMap = mock(EventMap.class);
        event.setMap(mockMap);

        event.reserveSpot(2L,1);

        verify(mockMap, times(1)).reserveSpot(2L);
    }

    @Test
    void givenAreaId_whenReleaseSpot_thenEventDelegatesToMap() {
        EventMap mockMap = mock(EventMap.class);
        event.setMap(mockMap);

        event.releaseSpot(2L);

        verify(mockMap, times(1)).releaseSpot(2L);
    }

    @Test
    void givenAreaId_whenSellSpot_thenEventDelegatesToMap() {
        EventMap mockMap = mock(EventMap.class);
        event.setMap(mockMap);

        event.sellSpot(2L);

        verify(mockMap, times(1)).sellSpot(2L);
    }
}

