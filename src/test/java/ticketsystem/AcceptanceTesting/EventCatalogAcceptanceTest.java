package ticketsystem.AcceptanceTesting;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.DomainLayer.discount.DiscountPolicy;
import ticketsystem.DomainLayer.discount.DiscountCompositionType;
import ticketsystem.ApplicationLayer.EventCatalogService;
import ticketsystem.ApplicationLayer.ISystemLogger;
import ticketsystem.ApplicationLayer.ITokenService;
import ticketsystem.DTO.Event.EventSearchResultDTO;
import ticketsystem.DomainLayer.EventCatalogDomainService;
import ticketsystem.DomainLayer.SearchCriteria;
import ticketsystem.DomainLayer.company.Company;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.event.EventCategory;
import ticketsystem.DomainLayer.event.EventLocation;
import ticketsystem.DomainLayer.event.Pair;
import ticketsystem.DomainLayer.user.User;
import ticketsystem.InfrastructureLayer.CompanyRepository;
import ticketsystem.InfrastructureLayer.EventRepository;
import ticketsystem.InfrastructureLayer.LogbackSystemLogger;
import ticketsystem.DomainLayer.policy.PurchasePolicy;

public class EventCatalogAcceptanceTest {

    private EventCatalogService eventCatalogService;

    private EventRepository eventRepository;
    private CompanyRepository companyRepository;
    private FakeTokenService tokenService;
    private LogbackSystemLogger logger;

    private final String validSessionId = "valid-session";
    private final String invalidSessionId = "invalid-session";

    private Event rockConcert;
    private Event theaterShow;
    private Event jazzFestival;

    private long company1Id;
    private long company2Id;
    private long inactiveCompanyId;

    @BeforeEach
    void setUp() {
        eventRepository = new EventRepository();
        companyRepository = new CompanyRepository();
        tokenService = new FakeTokenService();
        logger = new LogbackSystemLogger();

        EventCatalogDomainService domainService = new EventCatalogDomainService(companyRepository);

        eventCatalogService = new EventCatalogService(
                domainService,
                eventRepository,
                companyRepository,
                tokenService,
                logger);

        tokenService.addValidSession(validSessionId);

        Company company1 = createCompany("Live Nation", 1L, 4.8);
        Company company2 = createCompany("Stage Group", 2L, 3.9);
        Company inactiveCompany3 = createCompany("Closed Company", 3L, 5.0);
        inactiveCompany3.inactivate();

        companyRepository.save(company1);
        companyRepository.save(company2);
        companyRepository.save(inactiveCompany3);

        company1Id = company1.getId();
        company2Id = company2.getId();
        inactiveCompanyId = inactiveCompany3.getId();

        rockConcert = new Event(
                1L,
                LocalDateTime.now().plusDays(10),
                "Rock Concert",
                company1Id,
                1L,
                EventLocation.TEL_AVIV,
                100L,
                EventCategory.CONCERT,
                "The Rockers",
                BigDecimal.valueOf(99.99),
                new Pair<>(10, 10));

        theaterShow = new Event(
                2L,
                LocalDateTime.now().plusDays(20),
                "Theater Show",
                company2Id,
                1L,
                EventLocation.JERUSALEM,
                100L,
                EventCategory.THEATER,
                "The Theater Group",
                BigDecimal.valueOf(79.99),
                new Pair<>(10, 10));

        jazzFestival = new Event(
                3L,
                LocalDateTime.now().plusDays(30),
                "Jazz Festival",
                company1Id,
                1L,
                EventLocation.HAIFA,
                100L,
                EventCategory.CONCERT,
                "Jazz Ensemble",
                BigDecimal.valueOf(129.99),
                new Pair<>(10, 10));

        Event inactiveCompanyEvent = new Event(
                4L,
                LocalDateTime.now().plusDays(40),
                "Closed Company Event",
                inactiveCompanyId,
                1L,
                EventLocation.TEL_AVIV,
                100L,
                EventCategory.CONCERT,
                "Closed Artist",
                BigDecimal.valueOf(49.99),
                new Pair<>(10, 10));

        eventRepository.addEvent(rockConcert);
        eventRepository.addEvent(theaterShow);
        eventRepository.addEvent(jazzFestival);
        eventRepository.addEvent(inactiveCompanyEvent);
    }

    @Test
    void GivenUserEnteredSystemAndMatchingEventsExist_WhenGlobalSearch_ThenSystemReturnsMatchingEvents() {
        // Arrange
        SearchCriteria criteria = new SearchCriteria();
        criteria.setSearchTerm("Rock");

        // Act
        List<EventSearchResultDTO> results = eventCatalogService.globalSearch(validSessionId, criteria);

        // Assert
        assertEquals(1, results.size());
        assertTrue(containsEventId(results, rockConcert.getId()));
        assertFalse(containsEventId(results, theaterShow.getId()));
        assertFalse(containsEventId(results, jazzFestival.getId()));
    }

    @Test
    void GivenUserEnteredSystemAndFiltersMatchOnlyOneEvent_WhenGlobalSearch_ThenSystemReturnsFilteredResults() {
        // Arrange
        SearchCriteria criteria = new SearchCriteria();
        criteria.setSearchTerm("Jazz");
        criteria.setCategory(EventCategory.CONCERT);
        criteria.setLocation(EventLocation.HAIFA);

        // Act
        List<EventSearchResultDTO> results = eventCatalogService.globalSearch(validSessionId, criteria);

        // Assert
        assertEquals(1, results.size());
        assertTrue(containsEventId(results, jazzFestival.getId()));
        assertFalse(containsEventId(results, rockConcert.getId()));
        assertFalse(containsEventId(results, theaterShow.getId()));
    }

    @Test
    void GivenUserEnteredSystemAndNoEventMatchesSearch_WhenGlobalSearch_ThenSystemReturnsEmptyResults() {
        // Arrange
        SearchCriteria criteria = new SearchCriteria();
        criteria.setSearchTerm("Basketball");

        // Act
        List<EventSearchResultDTO> results = eventCatalogService.globalSearch(validSessionId, criteria);

        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void GivenUserEnteredSystemAndCompanyRatingFilterIsApplied_WhenGlobalSearch_ThenSystemReturnsEventsOnlyFromCompaniesWithEnoughRating() {
        // Arrange
        SearchCriteria criteria = new SearchCriteria();
        criteria.setCompanyRate(4.5);

        // Act
        List<EventSearchResultDTO> results = eventCatalogService.globalSearch(validSessionId, criteria);

        // Assert
        assertEquals(2, results.size());
        assertTrue(containsEventId(results, rockConcert.getId()));
        assertTrue(containsEventId(results, jazzFestival.getId()));
        assertFalse(containsEventId(results, theaterShow.getId()));
    }

    @Test
    void GivenUserEnteredSystemAndInactiveCompanyHasEvent_WhenGlobalSearch_ThenSystemDoesNotReturnInactiveCompanyEvents() {
        // Arrange
        SearchCriteria criteria = new SearchCriteria();
        criteria.setSearchTerm("Closed Company Event");

        // Act
        List<EventSearchResultDTO> results = eventCatalogService.globalSearch(validSessionId, criteria);

        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void GivenInvalidSession_WhenGlobalSearch_ThenSystemRejectsTheSearch() {
        // Arrange
        SearchCriteria criteria = new SearchCriteria();
        criteria.setSearchTerm("Rock");

        // Act
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> eventCatalogService.globalSearch(invalidSessionId, criteria));

        // Assert
        assertTrue(exception.getMessage().contains("Invalid session ID"));
    }

    @Test
    void GivenNullSearchCriteria_WhenGlobalSearch_ThenSystemRejectsTheSearch() {
        // Act
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> eventCatalogService.globalSearch(validSessionId, null));

        // Assert
        assertTrue(exception.getMessage().contains("Search criteria cannot be null"));
    }

    @Test
    void GivenUserEnteredSystemAndCompanyHasEvents_WhenSearchByCompany_ThenSystemReturnsCompanyEvents() {
        // Arrange
        long companyId = company1Id;
        SearchCriteria criteria = new SearchCriteria();

        // Act
        List<EventSearchResultDTO> results = eventCatalogService.SearchByCompany(validSessionId, companyId, criteria);

        // Assert
        assertEquals(2, results.size());
        assertTrue(containsEventId(results, rockConcert.getId()));
        assertTrue(containsEventId(results, jazzFestival.getId()));
        assertFalse(containsEventId(results, theaterShow.getId()));
    }

    @Test
    void GivenUserEnteredSystemAndCompanySearchFiltersMatchOneEvent_WhenSearchByCompany_ThenSystemReturnsFilteredResults() {
        // Arrange
        long companyId = company1Id;

        SearchCriteria criteria = new SearchCriteria();
        criteria.setSearchTerm("Jazz");

        // Act
        List<EventSearchResultDTO> results = eventCatalogService.SearchByCompany(validSessionId, companyId, criteria);

        // Assert
        assertEquals(1, results.size());
        assertTrue(containsEventId(results, jazzFestival.getId()));
        assertFalse(containsEventId(results, rockConcert.getId()));
    }

    @Test
    void GivenCompanyExistsButNoEventsMatchFilters_WhenSearchByCompany_ThenSystemReturnsEmptyResults() {
        // Arrange
        long companyId = company1Id;

        SearchCriteria criteria = new SearchCriteria();
        criteria.setSearchTerm("Opera");

        // Act
        List<EventSearchResultDTO> results = eventCatalogService.SearchByCompany(validSessionId, companyId, criteria);

        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void GivenInvalidSession_WhenSearchByCompany_ThenSystemRejectsTheSearch() {
        // Arrange
        long companyId = company1Id;
        SearchCriteria criteria = new SearchCriteria();

        // Act
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> eventCatalogService.SearchByCompany(invalidSessionId, companyId, criteria));

        // Assert
        assertTrue(exception.getMessage().contains("Invalid session ID"));
    }

    @Test
    void GivenNullSearchCriteria_WhenSearchByCompany_ThenSystemRejectsTheSearch() {
        // Arrange
        long companyId = company1Id;

        // Act
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> eventCatalogService.SearchByCompany(validSessionId, companyId, null));

        // Assert
        assertTrue(exception.getMessage().contains("Search criteria cannot be null"));
    }

    @Test
    void GivenCompanyRateFilter_WhenSearchByCompany_ThenSystemRejectsTheSearch() {
        // Arrange
        long companyId = company1Id;

        SearchCriteria criteria = new SearchCriteria();
        criteria.setCompanyRate(4.5);

        // Act
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> eventCatalogService.SearchByCompany(validSessionId, companyId, criteria));

        // Assert
        assertTrue(exception.getMessage().contains("Company rating criteria is not applicable"));
    }

    // Helper Methods
    private boolean containsEventId(List<EventSearchResultDTO> events, Long eventId) {
        return events.stream()
                .anyMatch(event -> event.id().equals(eventId));
    }

    /*
     * Adjust this method only if your Company constructor or methods are different.
     */
    private Company createCompany(String name, Long founderId, double rate) {
        Company company = new Company(name, founderId, PurchasePolicy.noRestrictions(), new DiscountPolicy(DiscountCompositionType.MAX));
        company.setRate(rate);

        return company;
    }

    private static class FakeTokenService implements ITokenService {

        private final Set<String> validSessions = new HashSet<>();

        void addValidSession(String sessionId) {
            validSessions.add(sessionId);
        }

        @Override
        public boolean validateToken(String sessionId) {
            return validSessions.contains(sessionId);
        }

        @Override
        public String addActiveSession(User user) {
            String sessionId = "test-session-" + (validSessions.size() + 1);
            validSessions.add(sessionId);
            return sessionId;
        }

        @Override
        public boolean isActiveSession(String sessionToken) {
            return validSessions.contains(sessionToken);
        }

        @Override
        public int getTotalActiveSessions() {
            return validSessions.size();
        }

        @Override
        public void removeActiveSession(String sessionToken) {
            validSessions.remove(sessionToken);
        }

        @Override
        public String generateNewGuestToken() {
            return "guest-token";
        }

        @Override
        public String generateNewMemberToken(Long userId) {
            return "member-token-" + userId;
        }

        @Override
        public String extractRole(String token) {
            return null;
        }

        @Override
        public boolean isGuestToken(String token) {
            return false;
        }

        @Override
        public boolean isMemberToken(String token) {
            return false;
        }

        @Override
        public Long extractUserId(String token) {
            return null;
        }
    }
}