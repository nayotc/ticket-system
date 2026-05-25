package ticketsystem.UnitTesting.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.DomainLayer.event.EventCategory;
import ticketsystem.DomainLayer.event.EventLocation;
import ticketsystem.DomainLayer.event.EventMap;
import ticketsystem.DomainLayer.event.Pair;
import ticketsystem.DomainLayer.policy.PurchasePolicy;
import ticketsystem.DomainLayer.event.SeatPosition;
import ticketsystem.DomainLayer.event.DiscountPolicy;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.SearchCriteria;

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
                EventLocation.NEW_YORK,
                3L,
                EventCategory.CONCERT,
                "Famous Artist",
                new BigDecimal("99.99"),
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
        assertEquals(EventLocation.NEW_YORK, event.getLocation());
        assertEquals(3L, event.getTrafficThreshold());
        assertEquals(Event.eventStatus.DRAFT, event.getStatus());
        assertEquals(EventCategory.CONCERT, event.getCategory());
        assertEquals("Famous Artist", event.getArtistName());
        assertEquals(new BigDecimal("99.99"), event.getTicketPrice());
        assertEquals(0.0, event.getRate(), 0.0001);

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
    void givenEvent_whenSetLocation_thenLocationIsUpdated() {
        event.setLocation(EventLocation.TEL_AVIV);

        assertEquals(EventLocation.TEL_AVIV, event.getLocation());
    }

    @Test
    void givenEvent_whenSetTrafficThreshold_thenTrafficThresholdIsUpdated() {
        event.setTrafficThreshold(5L);

        assertEquals(5L, event.getTrafficThreshold());
    }

    @Test
    void givenEvent_whenSetStatus_thenStatusIsUpdated() {
        event.setStatus(Event.eventStatus.ACTIVE);

        assertEquals(Event.eventStatus.ACTIVE, event.getStatus());
    }

    @Test
    void givenEvent_whenSetCategory_thenCategoryIsUpdated() {
        event.setCategory(EventCategory.SPORTS);

        assertEquals(EventCategory.SPORTS, event.getCategory());
    }

    @Test
    void givenEvent_whenSetArtistName_thenArtistNameIsUpdated() {
        event.setArtistName("New Artist");

        assertEquals("New Artist", event.getArtistName());
    }

    @Test
    void givenEvent_whenSetMap_thenMapIsUpdated() {
        EventMap mockMap = mock(EventMap.class);

        event.setMap(mockMap);

        assertSame(mockMap, event.getMap());
    }

    @Test
    void givenEvent_whenSetTicketPrice_thenTicketPriceIsUpdated() {
        BigDecimal newPrice = new BigDecimal("149.90");

        event.setTicketPrice(newPrice);

        assertEquals(newPrice, event.getTicketPrice());
    }

    @Test
    void givenEvent_whenSetRateOnce_thenRateIsUpdatedToGivenRate() {
        event.setRate(4.0);

        assertEquals(4.0, event.getRate(), 0.0001);
    }

    @Test
    void givenEvent_whenSetRateMultipleTimes_thenRateIsAverage() {
        event.setRate(4.0);
        event.setRate(2.0);

        assertEquals(3.0, event.getRate(), 0.0001);
    }

    @Test
    void givenEvent_whenSetPurchasePolicy_thenPurchasePolicyIsUpdated() {
        PurchasePolicy policy = PurchasePolicy.noRestrictions();

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

        verify(mockMap, times(1)).reserveSpot(2L,1);
    }

    @Test
    void givenAreaId_whenReleaseSpot_thenEventDelegatesToMap() {
        EventMap mockMap = mock(EventMap.class);
        event.setMap(mockMap);

        event.releaseSpot(2L, 1);

        verify(mockMap, times(1)).releaseSpot(2L, 1);
    }

    @Test
    void givenAreaId_whenSellSpot_thenEventDelegatesToMap() {
        EventMap mockMap = mock(EventMap.class);
        event.setMap(mockMap);

        event.sellSpot(2L, 1);

        verify(mockMap, times(1)).sellSpot(2L, 1);
    }

    @Test
    void givenNullSearchCriteria_WhenMatchesSearchCriteria_ThenThrowException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> event.matchesSearchCriteria(null)
        );
    }

    @Test
    void givenSearchCriteriaWithNoFilters_WhenMatchesSearchCriteria_ThenReturnTrue() {
        SearchCriteria criteria = mockCriteria(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertTrue(event.matchesSearchCriteria(criteria));
    }

    @Test
    void givenSearchTermMatchingEventName_WhenMatchesSearchCriteria_ThenReturnTrue() {
        SearchCriteria criteria = mockCriteria(
                "music-festival",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertTrue(event.matchesSearchCriteria(criteria));
    }

    @Test
    void givenSearchTermMatchingArtistName_WhenMatchesSearchCriteria_ThenReturnTrue() {
        SearchCriteria criteria = mockCriteria(
                "famous/artist",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertTrue(event.matchesSearchCriteria(criteria));
    }

    @Test
    void givenSearchTermMatchingLocation_WhenMatchesSearchCriteria_ThenReturnTrue() {
        SearchCriteria criteria = mockCriteria(
                "new_york",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertTrue(event.matchesSearchCriteria(criteria));
    }

    @Test
    void givenSearchTermMatchingCategory_WhenMatchesSearchCriteria_ThenReturnTrue() {
        SearchCriteria criteria = mockCriteria(
                "con-cert",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertTrue(event.matchesSearchCriteria(criteria));
    }

    @Test
    void givenSearchTermNotMatchingEvent_WhenMatchesSearchCriteria_ThenReturnFalse() {
        SearchCriteria criteria = mockCriteria(
                "basketball",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertFalse(event.matchesSearchCriteria(criteria));
    }

    @Test
    void givenMatchingCategory_WhenMatchesSearchCriteria_ThenReturnTrue() {
        SearchCriteria criteria = mockCriteria(
                null,
                EventCategory.CONCERT,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertTrue(event.matchesSearchCriteria(criteria));
    }

    @Test
    void givenDifferentCategory_WhenMatchesSearchCriteria_ThenReturnFalse() {
        SearchCriteria criteria = mockCriteria(
                null,
                EventCategory.SPORTS,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertFalse(event.matchesSearchCriteria(criteria));
    }

    @Test
    void givenEventInsideDateRange_WhenMatchesSearchCriteria_ThenReturnTrue() {
        SearchCriteria criteria = mockCriteria(
                null,
                null,
                eventDate.minusDays(1),
                eventDate.plusDays(1),
                null,
                null,
                null,
                null,
                null
        );

        assertTrue(event.matchesSearchCriteria(criteria));
    }

    @Test
    void givenEventBeforeStartDate_WhenMatchesSearchCriteria_ThenReturnFalse() {
        SearchCriteria criteria = mockCriteria(
                null,
                null,
                eventDate.plusDays(1),
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertFalse(event.matchesSearchCriteria(criteria));
    }

    @Test
    void givenEventAfterEndDate_WhenMatchesSearchCriteria_ThenReturnFalse() {
        SearchCriteria criteria = mockCriteria(
                null,
                null,
                null,
                eventDate.minusDays(1),
                null,
                null,
                null,
                null,
                null
        );

        assertFalse(event.matchesSearchCriteria(criteria));
    }

    @Test
    void givenMatchingLocation_WhenMatchesSearchCriteria_ThenReturnTrue() {
        SearchCriteria criteria = mockCriteria(
                null,
                null,
                null,
                null,
                EventLocation.NEW_YORK,
                null,
                null,
                null,
                null
        );

        assertTrue(event.matchesSearchCriteria(criteria));
    }

    @Test
    void givenDifferentLocation_WhenMatchesSearchCriteria_ThenReturnFalse() {
        SearchCriteria criteria = mockCriteria(
                null,
                null,
                null,
                null,
                EventLocation.TEL_AVIV,
                null,
                null,
                null,
                null
        );

        assertFalse(event.matchesSearchCriteria(criteria));
    }

    @Test
    void givenPriceInsideRange_WhenMatchesSearchCriteria_ThenReturnTrue() {
        SearchCriteria criteria = mockCriteria(
                null,
                null,
                null,
                null,
                null,
                new BigDecimal("50.00"),
                new BigDecimal("150.00"),
                null,
                null
        );

        assertTrue(event.matchesSearchCriteria(criteria));
    }

    @Test
    void givenPriceBelowMinimum_WhenMatchesSearchCriteria_ThenReturnFalse() {
        SearchCriteria criteria = mockCriteria(
                null,
                null,
                null,
                null,
                null,
                new BigDecimal("100.00"),
                null,
                null,
                null
        );

        assertFalse(event.matchesSearchCriteria(criteria));
    }

    @Test
    void givenPriceAboveMaximum_WhenMatchesSearchCriteria_ThenReturnFalse() {
        SearchCriteria criteria = mockCriteria(
                null,
                null,
                null,
                null,
                null,
                null,
                new BigDecimal("50.00"),
                null,
                null
        );

        assertFalse(event.matchesSearchCriteria(criteria));
    }

    @Test
    void givenMinimumPriceGreaterThanMaximumPrice_WhenMatchesSearchCriteria_ThenThrowException() {
        SearchCriteria criteria = mockCriteria(
                null,
                null,
                null,
                null,
                null,
                new BigDecimal("150.00"),
                new BigDecimal("50.00"),
                null,
                null
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> event.matchesSearchCriteria(criteria)
        );
    }

    @Test
    void givenEventRateEqualRequestedRate_WhenMatchesSearchCriteria_ThenReturnTrue() {
        event.setRate(4.0);

        SearchCriteria criteria = mockCriteria(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                4.0,
                null
        );

        assertTrue(event.matchesSearchCriteria(criteria));
    }

    @Test
    void givenEventRateHigherThanRequestedRate_WhenMatchesSearchCriteria_ThenReturnTrue() {
        event.setRate(4.0);

        SearchCriteria criteria = mockCriteria(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                3.0,
                null
        );

        assertTrue(event.matchesSearchCriteria(criteria));
    }

    @Test
    void givenEventRateLowerThanRequestedRate_WhenMatchesSearchCriteria_ThenReturnFalse() {
        event.setRate(2.0);

        SearchCriteria criteria = mockCriteria(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                3.0,
                null
        );

        assertFalse(event.matchesSearchCriteria(criteria));
    }

    @Test
    void givenMatchingArtistFilter_WhenMatchesSearchCriteria_ThenReturnTrue() {
        SearchCriteria criteria = mockCriteria(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "famous-artist"
        );

        assertTrue(event.matchesSearchCriteria(criteria));
    }

    @Test
    void givenDifferentArtistFilter_WhenMatchesSearchCriteria_ThenReturnFalse() {
        SearchCriteria criteria = mockCriteria(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "unknown artist"
        );

        assertFalse(event.matchesSearchCriteria(criteria));
    }

    private SearchCriteria mockCriteria(
            String searchTerm,
            EventCategory category,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            EventLocation location,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Double eventRate,
            String artist
    ) {
        SearchCriteria criteria = mock(SearchCriteria.class);

        when(criteria.getSearchTerm()).thenReturn(searchTerm);
        when(criteria.getCategory()).thenReturn(category);
        when(criteria.getFromDate()).thenReturn(fromDate);
        when(criteria.getToDate()).thenReturn(toDate);
        when(criteria.getLocation()).thenReturn(location);
        when(criteria.getMinPrice()).thenReturn(minPrice);
        when(criteria.getMaxPrice()).thenReturn(maxPrice);
        when(criteria.getEventRate()).thenReturn(eventRate);
        when(criteria.getArtist()).thenReturn(artist);

        return criteria;
    }
}