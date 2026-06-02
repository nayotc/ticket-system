package ticketsystem.UnitTesting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.DomainLayer.EventCatalogDomainService;
import ticketsystem.DomainLayer.SearchCriteria;
import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.company.Company;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.event.Event.eventStatus;

public class EventCatalogDomainTest {

    private EventCatalogDomainService domainService;
    private ICompanyRepository companyRepository;

    private SearchCriteria criteria;

    private Event eventFromAllowedCompanyAndMatching;
    private Event eventFromAllowedCompanyButNotMatching;
    private Event eventFromNotAllowedCompany;
    private Event cancelledEventFromAllowedCompany;
    private Event nonActiveEventFromAllowedCompany;

    @BeforeEach
    void setUp() {
        companyRepository = mock(ICompanyRepository.class);
        domainService = new EventCatalogDomainService(companyRepository);
        criteria = mock(SearchCriteria.class);

        eventFromAllowedCompanyAndMatching = mock(Event.class);
        eventFromAllowedCompanyButNotMatching = mock(Event.class);
        eventFromNotAllowedCompany = mock(Event.class);
        cancelledEventFromAllowedCompany = mock(Event.class);
        nonActiveEventFromAllowedCompany = mock(Event.class);

        when(eventFromAllowedCompanyAndMatching.getCompanyId()).thenReturn(100L);
        when(eventFromAllowedCompanyButNotMatching.getCompanyId()).thenReturn(100L);
        when(eventFromNotAllowedCompany.getCompanyId()).thenReturn(200L);
        when(cancelledEventFromAllowedCompany.getCompanyId()).thenReturn(100L);
        when(nonActiveEventFromAllowedCompany.getCompanyId()).thenReturn(100L);

        markActive(eventFromAllowedCompanyAndMatching);
        markActive(eventFromAllowedCompanyButNotMatching);
        markActive(eventFromNotAllowedCompany);

        when(cancelledEventFromAllowedCompany.getStatus()).thenReturn(eventStatus.CANCELLED);
        when(nonActiveEventFromAllowedCompany.getStatus()).thenReturn(null);

        when(eventFromAllowedCompanyAndMatching.matchesSearchCriteria(criteria)).thenReturn(true);
        when(eventFromAllowedCompanyButNotMatching.matchesSearchCriteria(criteria)).thenReturn(false);
        when(eventFromNotAllowedCompany.matchesSearchCriteria(criteria)).thenReturn(true);
        when(cancelledEventFromAllowedCompany.matchesSearchCriteria(criteria)).thenReturn(true);
        when(nonActiveEventFromAllowedCompany.matchesSearchCriteria(criteria)).thenReturn(true);
    }

    @Test
    void GivenEventsCompaniesAndCriteria_WhenGlobalSearch_ThenReturnOnlyActiveEventsFromAllowedCompaniesThatMatchCriteria() {
        // Arrange
        List<Event> events = List.of(
                eventFromAllowedCompanyAndMatching,
                eventFromAllowedCompanyButNotMatching,
                eventFromNotAllowedCompany,
                cancelledEventFromAllowedCompany,
                nonActiveEventFromAllowedCompany
        );

        List<Long> allowedCompanies = List.of(100L);

        // Act
        List<Event> results = domainService.globalSearch(events, allowedCompanies, criteria);

        // Assert
        assertEquals(1, results.size());
        assertTrue(results.contains(eventFromAllowedCompanyAndMatching));
        assertFalse(results.contains(eventFromAllowedCompanyButNotMatching));
        assertFalse(results.contains(eventFromNotAllowedCompany));
        assertFalse(results.contains(cancelledEventFromAllowedCompany));
        assertFalse(results.contains(nonActiveEventFromAllowedCompany));
    }

    @Test
    void GivenEventsListContainsNull_WhenGlobalSearch_ThenIgnoreNullEvents() {
        // Arrange
        List<Event> events = Arrays.asList(
                eventFromAllowedCompanyAndMatching,
                null
        );

        List<Long> allowedCompanies = List.of(100L);

        // Act
        List<Event> results = domainService.globalSearch(events, allowedCompanies, criteria);

        // Assert
        assertEquals(1, results.size());
        assertTrue(results.contains(eventFromAllowedCompanyAndMatching));
    }

    @Test
    void GivenEventFromCompanyNotInCompanyList_WhenGlobalSearch_ThenEventIsNotReturned() {
        // Arrange
        List<Event> events = List.of(eventFromNotAllowedCompany);
        List<Long> allowedCompanies = List.of(100L);

        // Act
        List<Event> results = domainService.globalSearch(events, allowedCompanies, criteria);

        // Assert
        assertTrue(results.isEmpty());
    }

    @Test
    void GivenAllowedCompanyButEventDoesNotMatchCriteria_WhenGlobalSearch_ThenEventIsNotReturned() {
        // Arrange
        List<Event> events = List.of(eventFromAllowedCompanyButNotMatching);
        List<Long> allowedCompanies = List.of(100L);

        // Act
        List<Event> results = domainService.globalSearch(events, allowedCompanies, criteria);

        // Assert
        assertTrue(results.isEmpty());
    }

    @Test
    void GivenAllowedCompanyAndCriteriaMatchButEventIsCancelled_WhenGlobalSearch_ThenEventIsNotReturned() {
        // Arrange
        List<Event> events = List.of(cancelledEventFromAllowedCompany);
        List<Long> allowedCompanies = List.of(100L);

        // Act
        List<Event> results = domainService.globalSearch(events, allowedCompanies, criteria);

        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void GivenAllowedCompanyAndCriteriaMatchButEventIsNotActive_WhenGlobalSearch_ThenEventIsNotReturned() {
        // Arrange
        List<Event> events = List.of(nonActiveEventFromAllowedCompany);
        List<Long> allowedCompanies = List.of(100L);

        // Act
        List<Event> results = domainService.globalSearch(events, allowedCompanies, criteria);

        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void GivenNullEventsList_WhenGlobalSearch_ThenThrowException() {
        // Arrange
        List<Long> allowedCompanies = List.of(100L);

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> domainService.globalSearch(null, allowedCompanies, criteria)
        );

        // Assert
        assertEquals("Events list cannot be null", exception.getMessage());
    }

    @Test
    void GivenNullCompaniesList_WhenGlobalSearch_ThenThrowException() {
        // Arrange
        List<Event> events = List.of(eventFromAllowedCompanyAndMatching);

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> domainService.globalSearch(events, null, criteria)
        );

        // Assert
        assertEquals("Companies list cannot be null", exception.getMessage());
    }

    @Test
    void GivenNullCriteria_WhenGlobalSearch_ThenThrowException() {
        // Arrange
        List<Event> events = List.of(eventFromAllowedCompanyAndMatching);
        List<Long> allowedCompanies = List.of(100L);

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> domainService.globalSearch(events, allowedCompanies, null)
        );

        // Assert
        assertEquals("Search criteria cannot be null", exception.getMessage());
    }

    @Test
    void GivenCompanyEventsAndCriteria_WhenSearchByCompany_ThenReturnOnlyActiveMatchingEvents() {
        // Arrange
        when(criteria.getCompanyRate()).thenReturn(null);

        List<Event> events = List.of(
                eventFromAllowedCompanyAndMatching,
                eventFromAllowedCompanyButNotMatching,
                cancelledEventFromAllowedCompany,
                nonActiveEventFromAllowedCompany
        );

        // Act
        List<Event> results = domainService.searchByCompany(events, criteria);

        // Assert
        assertEquals(1, results.size());
        assertTrue(results.contains(eventFromAllowedCompanyAndMatching));
        assertFalse(results.contains(eventFromAllowedCompanyButNotMatching));
        assertFalse(results.contains(cancelledEventFromAllowedCompany));
        assertFalse(results.contains(nonActiveEventFromAllowedCompany));
    }

    @Test
    void GivenCompanyEventsListContainsNull_WhenSearchByCompany_ThenIgnoreNullEvents() {
        // Arrange
        when(criteria.getCompanyRate()).thenReturn(null);

        List<Event> events = Arrays.asList(
                eventFromAllowedCompanyAndMatching,
                null
        );

        // Act
        List<Event> results = domainService.searchByCompany(events, criteria);

        // Assert
        assertEquals(1, results.size());
        assertTrue(results.contains(eventFromAllowedCompanyAndMatching));
    }

    @Test
    void GivenNoCompanyEventsMatchCriteria_WhenSearchByCompany_ThenReturnEmptyList() {
        // Arrange
        when(criteria.getCompanyRate()).thenReturn(null);

        List<Event> events = List.of(eventFromAllowedCompanyButNotMatching);

        // Act
        List<Event> results = domainService.searchByCompany(events, criteria);

        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void GivenCompanyEventMatchesCriteriaButIsCancelled_WhenSearchByCompany_ThenReturnEmptyList() {
        // Arrange
        when(criteria.getCompanyRate()).thenReturn(null);

        List<Event> events = List.of(cancelledEventFromAllowedCompany);

        // Act
        List<Event> results = domainService.searchByCompany(events, criteria);

        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void GivenCompanyEventMatchesCriteriaButIsNotActive_WhenSearchByCompany_ThenReturnEmptyList() {
        // Arrange
        when(criteria.getCompanyRate()).thenReturn(null);

        List<Event> events = List.of(nonActiveEventFromAllowedCompany);

        // Act
        List<Event> results = domainService.searchByCompany(events, criteria);

        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void GivenNullEventsList_WhenSearchByCompany_ThenThrowException() {
        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> domainService.searchByCompany(null, criteria)
        );

        // Assert
        assertEquals("Events list cannot be null", exception.getMessage());
    }

    @Test
    void GivenNullCriteria_WhenSearchByCompany_ThenThrowException() {
        // Arrange
        List<Event> events = List.of(eventFromAllowedCompanyAndMatching);

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> domainService.searchByCompany(events, null)
        );

        // Assert
        assertEquals("Search criteria cannot be null", exception.getMessage());
    }

    @Test
    void GivenCompanyRateCriteria_WhenSearchByCompany_ThenThrowException() {
        // Arrange
        List<Event> events = List.of(eventFromAllowedCompanyAndMatching);

        when(criteria.getCompanyRate()).thenReturn(4.5);

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> domainService.searchByCompany(events, criteria)
        );

        // Assert
        assertEquals(
                "Company rating criteria is not applicable for company-specific search",
                exception.getMessage()
        );
    }

    @Test
    void GivenNoDiscounts_WhenCalculateFinalPrice_ThenReturnOriginalPrice() {
        // Arrange
        Long companyId = 1L;
        Event event = mock(Event.class);
        Company company = mock(Company.class);

        markActive(event);

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(company.calculateDiscountCompany(BigDecimal.valueOf(100), 2, null))
                .thenReturn(BigDecimal.ZERO);
        when(event.calculateDiscountEvent(BigDecimal.valueOf(100), 2, null))
                .thenReturn(BigDecimal.ZERO);

        // Act
        BigDecimal result = domainService.calculateFinalPrice(
                companyId,
                event,
                BigDecimal.valueOf(100),
                2,
                null
        );

        // Assert
        assertEquals(0, BigDecimal.valueOf(100).compareTo(result));
    }

    @Test
    void GivenCompanyAndEventDiscounts_WhenCalculateFinalPrice_ThenApplyCompanyDiscountFirstAndThenEventDiscount() {
        // Arrange
        Long companyId = 1L;
        Event event = mock(Event.class);
        Company company = mock(Company.class);

        markActive(event);

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));

        when(company.calculateDiscountCompany(BigDecimal.valueOf(100), 2, "SAVE10"))
                .thenReturn(BigDecimal.valueOf(10));

        when(event.calculateDiscountEvent(BigDecimal.valueOf(90), 2, "SAVE10"))
                .thenReturn(BigDecimal.valueOf(20));

        // Act
        BigDecimal result = domainService.calculateFinalPrice(
                companyId,
                event,
                BigDecimal.valueOf(100),
                2,
                "SAVE10"
        );

        // Assert
        assertEquals(0, BigDecimal.valueOf(70).compareTo(result));

        verify(company).calculateDiscountCompany(BigDecimal.valueOf(100), 2, "SAVE10");
        verify(event).calculateDiscountEvent(BigDecimal.valueOf(90), 2, "SAVE10");
    }

    @Test
    void GivenCompanyDiscountGreaterThanTotalPrice_WhenCalculateFinalPrice_ThenPriceAfterCompanyDiscountIsZero() {
        // Arrange
        Long companyId = 1L;
        Event event = mock(Event.class);
        Company company = mock(Company.class);

        markActive(event);

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));

        when(company.calculateDiscountCompany(BigDecimal.valueOf(100), 1, null))
                .thenReturn(BigDecimal.valueOf(150));

        when(event.calculateDiscountEvent(BigDecimal.ZERO, 1, null))
                .thenReturn(BigDecimal.ZERO);

        // Act
        BigDecimal result = domainService.calculateFinalPrice(
                companyId,
                event,
                BigDecimal.valueOf(100),
                1,
                null
        );

        // Assert
        assertEquals(0, BigDecimal.ZERO.compareTo(result));
        verify(event).calculateDiscountEvent(BigDecimal.ZERO, 1, null);
    }

    @Test
    void GivenEventDiscountGreaterThanRemainingPrice_WhenCalculateFinalPrice_ThenFinalPriceIsZero() {
        // Arrange
        Long companyId = 1L;
        Event event = mock(Event.class);
        Company company = mock(Company.class);

        markActive(event);

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));

        when(company.calculateDiscountCompany(BigDecimal.valueOf(100), 1, null))
                .thenReturn(BigDecimal.valueOf(20));

        when(event.calculateDiscountEvent(BigDecimal.valueOf(80), 1, null))
                .thenReturn(BigDecimal.valueOf(100));

        // Act
        BigDecimal result = domainService.calculateFinalPrice(
                companyId,
                event,
                BigDecimal.valueOf(100),
                1,
                null
        );

        // Assert
        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    @Test
    void GivenCancelledEvent_WhenCalculateFinalPrice_ThenThrowException() {
        // Arrange
        Long companyId = 1L;
        Event event = mock(Event.class);

        when(event.getStatus()).thenReturn(eventStatus.CANCELLED);

        // Act
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> domainService.calculateFinalPrice(
                        companyId,
                        event,
                        BigDecimal.valueOf(100),
                        1,
                        null
                )
        );

        // Assert
        assertEquals("Cannot purchase tickets for a cancelled event", exception.getMessage());
    }

    @Test
    void GivenNullCompanyId_WhenCalculateFinalPrice_ThenThrowException() {
        // Arrange
        Event event = mock(Event.class);

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> domainService.calculateFinalPrice(
                        null,
                        event,
                        BigDecimal.valueOf(100),
                        1,
                        null
                )
        );

        // Assert
        assertEquals("Company id cannot be null", exception.getMessage());
    }

    @Test
    void GivenNullEvent_WhenCalculateFinalPrice_ThenThrowException() {
        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> domainService.calculateFinalPrice(
                        1L,
                        null,
                        BigDecimal.valueOf(100),
                        1,
                        null
                )
        );

        // Assert
        assertEquals("Event cannot be null", exception.getMessage());
    }

    @Test
    void GivenNullTotalPrice_WhenCalculateFinalPrice_ThenThrowException() {
        // Arrange
        Event event = mock(Event.class);
        markActive(event);

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> domainService.calculateFinalPrice(
                        1L,
                        event,
                        null,
                        1,
                        null
                )
        );

        // Assert
        assertEquals("Total price cannot be null", exception.getMessage());
    }

    @Test
    void GivenNegativeTicketCount_WhenCalculateFinalPrice_ThenThrowException() {
        // Arrange
        Event event = mock(Event.class);
        markActive(event);

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> domainService.calculateFinalPrice(
                        1L,
                        event,
                        BigDecimal.valueOf(100),
                        -1,
                        null
                )
        );

        // Assert
        assertEquals("Ticket count cannot be negative", exception.getMessage());
    }

    @Test
    void GivenCompanyDoesNotExist_WhenCalculateFinalPrice_ThenThrowException() {
        // Arrange
        Long companyId = 999L;
        Event event = mock(Event.class);

        markActive(event);

        when(companyRepository.findById(companyId)).thenReturn(Optional.empty());

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> domainService.calculateFinalPrice(
                        companyId,
                        event,
                        BigDecimal.valueOf(100),
                        1,
                        null
                )
        );

        // Assert
        assertEquals("Company not found", exception.getMessage());
    }

    private void markActive(Event event) {
        when(event.getStatus()).thenReturn(eventStatus.ACTIVE);
    }
}