package ticketsystem.UnitTesting;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.DomainLayer.EventCatalogDomainService;
import ticketsystem.DomainLayer.SearchCriteria;
import ticketsystem.DomainLayer.event.Event;


public class EventCatalogDomainTest {

    private EventCatalogDomainService domainService;

    private SearchCriteria criteria;

    private Event eventFromAllowedCompanyAndMatching;
    private Event eventFromAllowedCompanyButNotMatching;
    private Event eventFromNotAllowedCompany;

    @BeforeEach
    void setUp() {
        domainService = new EventCatalogDomainService();

        criteria = mock(SearchCriteria.class);

        eventFromAllowedCompanyAndMatching = mock(Event.class);
        eventFromAllowedCompanyButNotMatching = mock(Event.class);
        eventFromNotAllowedCompany = mock(Event.class);

        when(eventFromAllowedCompanyAndMatching.getCompanyId()).thenReturn(100L);
        when(eventFromAllowedCompanyButNotMatching.getCompanyId()).thenReturn(100L);
        when(eventFromNotAllowedCompany.getCompanyId()).thenReturn(200L);

        when(eventFromAllowedCompanyAndMatching.matchesSearchCriteria(criteria)).thenReturn(true);
        when(eventFromAllowedCompanyButNotMatching.matchesSearchCriteria(criteria)).thenReturn(false);
        when(eventFromNotAllowedCompany.matchesSearchCriteria(criteria)).thenReturn(true);
    }

    @Test
    void GivenEventsCompaniesAndCriteria_WhenGlobalSearch_ThenReturnOnlyEventsFromAllowedCompaniesThatMatchCriteria() {
        // Arrange
        List<Event> events = List.of(
                eventFromAllowedCompanyAndMatching,
                eventFromAllowedCompanyButNotMatching,
                eventFromNotAllowedCompany
        );

        List<Long> allowedCompanies = List.of(100L);

        // Act
        List<Event> results = domainService.globalSearch(events, allowedCompanies, criteria);

        // Assert
        assertEquals(1, results.size());
        assertTrue(results.contains(eventFromAllowedCompanyAndMatching));
        assertFalse(results.contains(eventFromAllowedCompanyButNotMatching));
        assertFalse(results.contains(eventFromNotAllowedCompany));
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
    void GivenCompanyEventsAndCriteria_WhenSearchByCompany_ThenReturnOnlyMatchingEvents() {
        // Arrange
        when(criteria.getCompanyRate()).thenReturn(null);

        List<Event> events = List.of(
                eventFromAllowedCompanyAndMatching,
                eventFromAllowedCompanyButNotMatching
        );

        // Act
        List<Event> results = domainService.searchByCompany(events, criteria);

        // Assert
        assertEquals(1, results.size());
        assertTrue(results.contains(eventFromAllowedCompanyAndMatching));
        assertFalse(results.contains(eventFromAllowedCompanyButNotMatching));
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
}
