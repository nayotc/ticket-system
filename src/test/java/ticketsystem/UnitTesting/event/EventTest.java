package ticketsystem.UnitTesting.event;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.DomainLayer.event.EventCategory;
import ticketsystem.DomainLayer.event.EventLocation;
import ticketsystem.DomainLayer.event.EventMap;
import ticketsystem.DomainLayer.event.Pair;
import ticketsystem.DomainLayer.event.Seat;
import ticketsystem.DomainLayer.event.Seat.SeatStatus;
import ticketsystem.DomainLayer.policy.MaxTicketsRule;
import ticketsystem.DomainLayer.policy.MinAgeRule;
import ticketsystem.DomainLayer.policy.PolicyResult;
import ticketsystem.DomainLayer.policy.PurchasePolicy;
import ticketsystem.DomainLayer.policy.PurchaseRule;
import ticketsystem.DomainLayer.event.SeatPosition;
import ticketsystem.DomainLayer.event.SeatingArea;
import ticketsystem.DomainLayer.event.StandingArea;
import ticketsystem.DomainLayer.discount.DiscountPolicy;
import ticketsystem.DomainLayer.discount.VisibleDiscount;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.SearchCriteria;
import ticketsystem.DomainLayer.discount.ConditionalDiscount;
import ticketsystem.DomainLayer.discount.CouponDiscount;
import ticketsystem.DomainLayer.discount.DiscountCompositionType;

public class EventTest {

    private Event event;

    private Long eventId = 1L;
    private final Long companyId = 10L;
    private final Long openedBy = 100L;
    private final LocalDateTime eventDate = LocalDateTime.now().plusDays(10);

    @BeforeEach
    void setUp() {
        event = new Event(
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
        eventId = event.getId();
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
        DiscountPolicy policy = new DiscountPolicy(DiscountCompositionType.MAX);

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

    @Test
    void givenDefaultPurchasePolicy_whenCanPurchase_thenDoesNotThrow() {
        assertDoesNotThrow(() -> event.canPurchase(100, 0));
    }

    @Test
    void givenMaxTicketsPolicyAndValidQuantity_whenCanPurchase_thenDoesNotThrow() {
        event.setPurchasePolicy(new PurchasePolicy(new MaxTicketsRule(5)));

        assertDoesNotThrow(() -> event.canPurchase(5, 20));
    }

    @Test
    void givenMaxTicketsPolicyAndTooManyTickets_whenCanPurchase_thenThrowExceptionWithPolicyMessage() {
        event.setPurchasePolicy(new PurchasePolicy(new MaxTicketsRule(5)));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> event.canPurchase(6, 20)
        );

        assertEquals("Cannot purchase more than 5 tickets.", exception.getMessage());
    }

    @Test
    void givenMinAgePolicyAndValidAge_whenCanPurchase_thenDoesNotThrow() {
        event.setPurchasePolicy(new PurchasePolicy(new MinAgeRule(18)));

        assertDoesNotThrow(() -> event.canPurchase(1, 18));
    }

    @Test
    void givenMinAgePolicyAndAgeBelowMinimum_whenCanPurchase_thenThrowExceptionWithPolicyMessage() {
        event.setPurchasePolicy(new PurchasePolicy(new MinAgeRule(18)));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> event.canPurchase(1, 17)
        );

        assertEquals(
                "Customer does not meet the minimum age requirement of 18",
                exception.getMessage()
        );
    }

    @Test
    void givenDeniedPurchasePolicyWithNullMessage_whenCanPurchase_thenThrowDefaultMessage() {
        event.setPurchasePolicy(new PurchasePolicy(
                new FixedResultPurchaseRule(PolicyResult.denied(null))
        ));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> event.canPurchase(1, 20)
        );

        assertEquals(
                "User does not satisfy the purchase policy",
                exception.getMessage()
        );
    }

    @Test
    void givenDeniedPurchasePolicyWithBlankMessage_whenCanPurchase_thenThrowDefaultMessage() {
        event.setPurchasePolicy(new PurchasePolicy(
                new FixedResultPurchaseRule(PolicyResult.denied("   "))
        ));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> event.canPurchase(1, 20)
        );

        assertEquals(
                "User does not satisfy the purchase policy",
                exception.getMessage()
        );
    }

    @Test
    void givenPurchasePolicyReturnsNull_whenCanPurchase_thenThrowIllegalStateException() {
        event.setPurchasePolicy(new PurchasePolicy(
                new FixedResultPurchaseRule(null)
        ));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> event.canPurchase(1, 20)
        );

        assertEquals("Purchase policy validation failed", exception.getMessage());
    }

    @Test
    void givenNullPurchasePolicy_whenSetPurchasePolicy_thenThrowException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> event.setPurchasePolicy(null)
        );

        assertEquals("Purchase policy cannot be null", exception.getMessage());
    }
    // --- Discount Policy: Add Discount To Event ---

    @Test
    void GivenValidVisibleDiscount_WhenAddVisibleDiscountToEvent_ThenDiscountIsAdded() {
        event.addVisibleDiscountToEvent("Student Discount", BigDecimal.valueOf(10));

        assertEquals(1, event.getDiscounts().size());
        assertTrue(event.getDiscounts().get(0) instanceof VisibleDiscount);
    }

    @Test
    void GivenNullName_WhenAddVisibleDiscountToEvent_ThenExceptionIsThrown() {
        assertThrows(IllegalArgumentException.class, () ->
                event.addVisibleDiscountToEvent(null, BigDecimal.valueOf(10))
        );
    }

    @Test
    void GivenEmptyName_WhenAddVisibleDiscountToEvent_ThenExceptionIsThrown() {
        assertThrows(IllegalArgumentException.class, () ->
                event.addVisibleDiscountToEvent("", BigDecimal.valueOf(10))
        );
    }

    @Test
    void GivenNullPercentage_WhenAddVisibleDiscountToEvent_ThenExceptionIsThrown() {
        assertThrows(IllegalArgumentException.class, () ->
                event.addVisibleDiscountToEvent("Discount", null)
        );
    }

    @Test
    void GivenNegativePercentage_WhenAddVisibleDiscountToEvent_ThenExceptionIsThrown() {
        assertThrows(IllegalArgumentException.class, () ->
                event.addVisibleDiscountToEvent("Discount", BigDecimal.valueOf(-1))
        );
    }

    @Test
    void GivenPercentageAbove100_WhenAddVisibleDiscountToEvent_ThenExceptionIsThrown() {
        assertThrows(IllegalArgumentException.class, () ->
                event.addVisibleDiscountToEvent("Discount", BigDecimal.valueOf(101))
        );
    }


    // --- Discount Policy: Add Coupon Discount To Event ---

    @Test
    void GivenValidCouponDiscount_WhenAddCouponDiscountToEvent_ThenDiscountIsAdded() {
        event.addCouponDiscountToEvent(
                "Coupon Discount",
                "BGU10",
                BigDecimal.valueOf(10),
                LocalDateTime.now().plusDays(7)
        );

        assertEquals(1, event.getDiscounts().size());
        assertTrue(event.getDiscounts().get(0) instanceof CouponDiscount);
    }

    @Test
    void GivenNullCouponCode_WhenAddCouponDiscountToEvent_ThenExceptionIsThrown() {
        assertThrows(IllegalArgumentException.class, () ->
                event.addCouponDiscountToEvent(
                        "Coupon Discount",
                        null,
                        BigDecimal.valueOf(10),
                        LocalDateTime.now().plusDays(7)
                )
        );
    }

    @Test
    void GivenEmptyCouponCode_WhenAddCouponDiscountToEvent_ThenExceptionIsThrown() {
        assertThrows(IllegalArgumentException.class, () ->
                event.addCouponDiscountToEvent(
                        "Coupon Discount",
                        "",
                        BigDecimal.valueOf(10),
                        LocalDateTime.now().plusDays(7)
                )
        );
    }


    // --- Discount Policy: Add Conditional Discount To Event --

    


    // --- Discount Policy: Composition Type ---

    @Test
    void GivenCompositionTypeSum_WhenSetDiscountCompositionType_ThenTypeIsUpdated() {
        event.setDiscountCompositionType(DiscountCompositionType.SUM);

        assertEquals(DiscountCompositionType.SUM, event.getDiscountPolicy().getDiscountCompositionType());
    }

    @Test
    void GivenCompositionTypeMax_WhenSetDiscountCompositionType_ThenTypeIsUpdated() {
        event.setDiscountCompositionType(DiscountCompositionType.MAX);

        assertEquals(DiscountCompositionType.MAX, event.getDiscountPolicy().getDiscountCompositionType());
    }

    @Test
    void givenEmptyMap_whenGetCapacity_thenReturnZero() {
        EventMap mockMap = mock(EventMap.class);
        when(mockMap.getElements()).thenReturn(List.of());
        event.setMap(mockMap);

        assertEquals(0, event.getCapacity());
    }

    @Test
    void givenEmptyMap_whenGetSoldTicketsCount_thenReturnZero() {
        EventMap mockMap = mock(EventMap.class);
        when(mockMap.getElements()).thenReturn(List.of());
        event.setMap(mockMap);

        assertEquals(0, event.getSoldTicketsCount());
    }

    @Test
    void givenStandingArea_whenGetCapacity_thenReturnStandingCapacity() {
        StandingArea standingArea = mock(StandingArea.class);
        when(standingArea.getCapacity()).thenReturn(120L);

        EventMap mockMap = mock(EventMap.class);
        when(mockMap.getElements()).thenReturn(List.of(standingArea));
        event.setMap(mockMap);

        assertEquals(120, event.getCapacity());
    }

    @Test
    void givenStandingAreaWithSoldTickets_whenGetSoldTicketsCount_thenReturnStandingSoldCount() {
        StandingArea standingArea = mock(StandingArea.class);
        when(standingArea.getSold()).thenReturn(7L);

        EventMap mockMap = mock(EventMap.class);
        when(mockMap.getElements()).thenReturn(List.of(standingArea));
        event.setMap(mockMap);

        assertEquals(7, event.getSoldTicketsCount());
    }

    @Test
    void givenSeatingArea_whenGetCapacity_thenReturnSeatsCount() {
        SeatingArea seatingArea = mock(SeatingArea.class);

        Map<SeatPosition, Seat> seats = new HashMap<>();
        seats.put(mock(SeatPosition.class), mock(Seat.class));
        seats.put(mock(SeatPosition.class), mock(Seat.class));
        seats.put(mock(SeatPosition.class), mock(Seat.class));

        when(seatingArea.getSeats()).thenReturn(seats);

        EventMap mockMap = mock(EventMap.class);
        when(mockMap.getElements()).thenReturn(List.of(seatingArea));
        event.setMap(mockMap);

        assertEquals(3, event.getCapacity());
    }

    @Test
    void givenSeatingAreaWithSoldSeats_whenGetSoldTicketsCount_thenReturnSoldSeatsCount() {
        Seat soldSeat1 = mock(Seat.class);
        Seat soldSeat2 = mock(Seat.class);
        Seat availableSeat = mock(Seat.class);

        when(soldSeat1.getStatus()).thenReturn(SeatStatus.SOLD);
        when(soldSeat2.getStatus()).thenReturn(SeatStatus.SOLD);
        when(availableSeat.getStatus()).thenReturn(SeatStatus.AVAILABLE);

        Map<SeatPosition, Seat> seats = new HashMap<>();
        seats.put(mock(SeatPosition.class), soldSeat1);
        seats.put(mock(SeatPosition.class), soldSeat2);
        seats.put(mock(SeatPosition.class), availableSeat);

        SeatingArea seatingArea = mock(SeatingArea.class);
        when(seatingArea.getSeats()).thenReturn(seats);

        EventMap mockMap = mock(EventMap.class);
        when(mockMap.getElements()).thenReturn(List.of(seatingArea));
        event.setMap(mockMap);

        assertEquals(2, event.getSoldTicketsCount());
    }

    @Test
    void givenMixedStandingAndSeatingAreas_whenGetCapacity_thenReturnTotalCapacity() {
        StandingArea standingArea = mock(StandingArea.class);
        when(standingArea.getCapacity()).thenReturn(120L);

        SeatingArea seatingArea = mock(SeatingArea.class);

        Map<SeatPosition, Seat> seats = new HashMap<>();
        seats.put(mock(SeatPosition.class), mock(Seat.class));
        seats.put(mock(SeatPosition.class), mock(Seat.class));
        seats.put(mock(SeatPosition.class), mock(Seat.class));

        when(seatingArea.getSeats()).thenReturn(seats);

        EventMap mockMap = mock(EventMap.class);
        when(mockMap.getElements()).thenReturn(List.of(standingArea, seatingArea));
        event.setMap(mockMap);

        assertEquals(123, event.getCapacity());
    }

    @Test
    void givenMixedStandingAndSeatingAreasWithSoldTickets_whenGetSoldTicketsCount_thenReturnTotalSoldTickets() {
        StandingArea standingArea = mock(StandingArea.class);
        when(standingArea.getSold()).thenReturn(5L);

        Seat soldSeat1 = mock(Seat.class);
        Seat soldSeat2 = mock(Seat.class);
        Seat availableSeat = mock(Seat.class);

        when(soldSeat1.getStatus()).thenReturn(SeatStatus.SOLD);
        when(soldSeat2.getStatus()).thenReturn(SeatStatus.SOLD);
        when(availableSeat.getStatus()).thenReturn(SeatStatus.AVAILABLE);

        Map<SeatPosition, Seat> seats = new HashMap<>();
        seats.put(mock(SeatPosition.class), soldSeat1);
        seats.put(mock(SeatPosition.class), soldSeat2);
        seats.put(mock(SeatPosition.class), availableSeat);

        SeatingArea seatingArea = mock(SeatingArea.class);
        when(seatingArea.getSeats()).thenReturn(seats);

        EventMap mockMap = mock(EventMap.class);
        when(mockMap.getElements()).thenReturn(List.of(standingArea, seatingArea));
        event.setMap(mockMap);

        assertEquals(7, event.getSoldTicketsCount());
    }

    private static class FixedResultPurchaseRule extends PurchaseRule {

        private final PolicyResult result;

        private FixedResultPurchaseRule(PolicyResult result) {
            this.result = result;
        }

        @Override
        public PolicyResult isValid(int quantity, int age) {
            return result;
        }
    }

}