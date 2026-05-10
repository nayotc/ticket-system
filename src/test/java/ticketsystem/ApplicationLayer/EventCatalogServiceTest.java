package ticketsystem.ApplicationLayer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.ApplicationLayer.EventCatalogService;
import ticketsystem.ApplicationLayer.ITokenService;
import ticketsystem.DomainLayer.EventCatalogDomainService;
import ticketsystem.DomainLayer.SearchCriteria;
import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.event.EventCategory;
import ticketsystem.DomainLayer.event.EventLocation;
import ticketsystem.DomainLayer.event.Pair;


public class EventCatalogServiceTest {
    private EventCatalogService eventCatalogService;

    private IEventRepository eventRepository;
    private ICompanyRepository companyRepository;
    private ITokenService tokenService;
    private EventCatalogDomainService domainService;

    private final String validSessionId = "valid-session";
    private final String invalidSessionId = "invalid-session";

    private Event rockConcert;
    private Event theaterShow;
    private Event jazzFestival;

    @BeforeEach
    void setUp() {
        eventRepository = mock(IEventRepository.class);
        companyRepository = mock(ICompanyRepository.class);
        tokenService = mock(ITokenService.class);

        // Real domain service, because acceptance tests should test behavior,
        // not only orchestration.
        domainService = new EventCatalogDomainService();

        eventCatalogService = new EventCatalogService(
                domainService,
                eventRepository,
                companyRepository,
                tokenService
        );

        rockConcert = new Event(
                1L,
                LocalDateTime.now().plusDays(10),
                "Rock Concert",
                100L,
                1L,
                EventLocation.TEL_AVIV,
                100L,
                EventCategory.CONCERT,
                "The Rockers",
                BigDecimal.valueOf(99.99),
                new Pair<>(10, 10)
        );

        theaterShow = new Event(
                2L,
                LocalDateTime.now().plusDays(20),
                "Theater Show",
                200L,
                1L,
                EventLocation.JERUSALEM,
                100L,
                EventCategory.THEATER,
                "The Theater Group",
                BigDecimal.valueOf(79.99),
                new Pair<>(10, 10)
        );

        jazzFestival = new Event(
                3L,
                LocalDateTime.now().plusDays(30),
                "Jazz Festival",
                100L,
                1L,
                EventLocation.HAIFA,
                100L,
                EventCategory.CONCERT,
                "Jazz Ensemble",
                BigDecimal.valueOf(129.99),
                new Pair<>(10, 10)
        );
    }

    @Test
    void GivenUserEnteredSystemAndMatchingEventsExist_WhenGlobalSearch_ThenSystemReturnsMatchingEvents() {
        // Arrange
        SearchCriteria criteria = new SearchCriteria();
        criteria.setSearchTerm("Rock");

        when(tokenService.validateToken(validSessionId)).thenReturn(true);
        when(eventRepository.getAllEvents()).thenReturn(List.of(rockConcert, theaterShow, jazzFestival));
        when(companyRepository.getCompanyIdsByCriteria(criteria)).thenReturn(List.of(100L, 200L));

        // Act
        List<Event> results = eventCatalogService.globalSearch(validSessionId, criteria);

        // Assert
        assertEquals(1, results.size());
        assertTrue(results.contains(rockConcert));
        assertFalse(results.contains(theaterShow));
        assertFalse(results.contains(jazzFestival));
    }

    @Test
    void GivenUserEnteredSystemAndSearchFiltersMatchOnlyOneEvent_WhenGlobalSearch_ThenSystemReturnsFilteredResults() {
        // Arrange
        SearchCriteria criteria = new SearchCriteria();
        criteria.setCategory(EventCategory.CONCERT);
        criteria.setLocation(EventLocation.HAIFA);

        when(tokenService.validateToken(validSessionId)).thenReturn(true);
        when(eventRepository.getAllEvents()).thenReturn(List.of(rockConcert, theaterShow, jazzFestival));
        when(companyRepository.getCompanyIdsByCriteria(criteria)).thenReturn(List.of(100L, 200L));

        // Act
        List<Event> results = eventCatalogService.globalSearch(validSessionId, criteria);

        // Assert
        assertEquals(1, results.size());
        assertTrue(results.contains(jazzFestival));
    }

    @Test
    void GivenUserEnteredSystemAndNoEventMatchesSearch_WhenGlobalSearch_ThenSystemReturnsEmptyResults() {
        // Arrange
        SearchCriteria criteria = new SearchCriteria();
        criteria.setSearchTerm("Basketball");

        when(tokenService.validateToken(validSessionId)).thenReturn(true);
        when(eventRepository.getAllEvents()).thenReturn(List.of(rockConcert, theaterShow, jazzFestival));
        when(companyRepository.getCompanyIdsByCriteria(criteria)).thenReturn(List.of(100L, 200L));

        // Act
        List<Event> results = eventCatalogService.globalSearch(validSessionId, criteria);

        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void GivenInvalidSession_WhenGlobalSearch_ThenSystemRejectsTheSearch() {
        // Arrange
        SearchCriteria criteria = new SearchCriteria();
        criteria.setSearchTerm("Rock");

        when(tokenService.validateToken(invalidSessionId)).thenReturn(false);

        // Act
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> eventCatalogService.globalSearch(invalidSessionId, criteria)
        );

        // Assert
        assertTrue(exception.getMessage().contains("Invalid session ID"));
    }

    @Test
    void GivenNullSearchCriteria_WhenGlobalSearch_ThenSystemRejectsTheSearch() {
        // Arrange
        when(tokenService.validateToken(validSessionId)).thenReturn(true);

        // Act
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> eventCatalogService.globalSearch(validSessionId, null)
        );

        // Assert
        assertTrue(exception.getMessage().contains("Search criteria cannot be null"));
    }

    @Test
    void GivenUserEnteredSystemAndCompanyHasEvents_WhenSearchByCompany_ThenSystemReturnsCompanyEvents() {
        // Arrange
        long companyId = 100L;

        SearchCriteria criteria = new SearchCriteria();

        when(tokenService.validateToken(validSessionId)).thenReturn(true);
        when(eventRepository.getEventsByCompanyId(companyId)).thenReturn(List.of(rockConcert, jazzFestival));

        // Act
        List<Event> results = eventCatalogService.SearchByCompany(validSessionId, companyId, criteria);

        // Assert
        assertEquals(2, results.size());
        assertTrue(results.contains(rockConcert));
        assertTrue(results.contains(jazzFestival));
        assertFalse(results.contains(theaterShow));
    }

    @Test
    void GivenUserEnteredSystemAndCompanySearchFiltersMatchOneEvent_WhenSearchByCompany_ThenSystemReturnsFilteredResults() {
        // Arrange
        long companyId = 100L;

        SearchCriteria criteria = new SearchCriteria();
        criteria.setSearchTerm("Jazz");

        when(tokenService.validateToken(validSessionId)).thenReturn(true);
        when(eventRepository.getEventsByCompanyId(companyId)).thenReturn(List.of(rockConcert, jazzFestival));

        // Act
        List<Event> results = eventCatalogService.SearchByCompany(validSessionId, companyId, criteria);

        // Assert
        assertEquals(1, results.size());
        assertTrue(results.contains(jazzFestival));
        assertFalse(results.contains(rockConcert));
    }

    @Test
    void GivenCompanyExistsButNoEventsMatchFilters_WhenSearchByCompany_ThenSystemReturnsEmptyResults() {
        // Arrange
        long companyId = 100L;

        SearchCriteria criteria = new SearchCriteria();
        criteria.setSearchTerm("Opera");

        when(tokenService.validateToken(validSessionId)).thenReturn(true);
        when(eventRepository.getEventsByCompanyId(companyId)).thenReturn(List.of(rockConcert, jazzFestival));

        // Act
        List<Event> results = eventCatalogService.SearchByCompany(validSessionId, companyId, criteria);

        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void GivenCompanyDoesNotExist_WhenSearchByCompany_ThenSystemRejectsTheSearch() {
        // Arrange
        long nonExistingCompanyId = 999L;

        SearchCriteria criteria = new SearchCriteria();

        when(tokenService.validateToken(validSessionId)).thenReturn(true);
        when(eventRepository.getEventsByCompanyId(nonExistingCompanyId))
                .thenThrow(new IllegalArgumentException("Company not found"));

        // Act
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> eventCatalogService.SearchByCompany(validSessionId, nonExistingCompanyId, criteria)
        );

        // Assert
        assertTrue(exception.getMessage().contains("Company not found"));
    }

    @Test
    void GivenInvalidSession_WhenSearchByCompany_ThenSystemRejectsTheSearch() {
        // Arrange
        long companyId = 100L;

        SearchCriteria criteria = new SearchCriteria();

        when(tokenService.validateToken(invalidSessionId)).thenReturn(false);

        // Act
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> eventCatalogService.SearchByCompany(invalidSessionId, companyId, criteria)
        );

        // Assert
        assertTrue(exception.getMessage().contains("Invalid session ID"));
    }

    @Test
    void GivenNullSearchCriteria_WhenSearchByCompany_ThenSystemRejectsTheSearch() {
        // Arrange
        long companyId = 100L;

        when(tokenService.validateToken(validSessionId)).thenReturn(true);

        // Act
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> eventCatalogService.SearchByCompany(validSessionId, companyId, null)
        );

        // Assert
        assertTrue(exception.getMessage().contains("Search criteria cannot be null"));
    }
}

    

