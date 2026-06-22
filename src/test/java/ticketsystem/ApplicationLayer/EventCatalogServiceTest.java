package ticketsystem.ApplicationLayer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.DTO.Event.EventSearchResultDTO;
import ticketsystem.DomainLayer.EventCatalogDomainService;
import ticketsystem.DomainLayer.SearchCriteria;
import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.event.Event.eventStatus;
import ticketsystem.DomainLayer.event.EventCategory;
import ticketsystem.DomainLayer.event.EventLocation;
import ticketsystem.DomainLayer.event.EventSearchResultView;
import ticketsystem.DomainLayer.event.Pair;

public class EventCatalogServiceTest {

    private EventCatalogService eventCatalogService;

    private IEventRepository eventRepository;
    private ICompanyRepository companyRepository;
    private ITokenService tokenService;
    private EventCatalogDomainService domainService;
    private ISystemLogger logger;

    private final String validSessionId = "valid-session";
    private final String invalidSessionId = "invalid-session";

    private Event rockConcert;
    private Event theaterShow;
    private Event jazzFestival;
    private Event cancelledRockConcert;
    private Event draftRockConcert;

    private EventSearchResultView rockConcertView;
    private EventSearchResultView theaterShowView;
    private EventSearchResultView jazzFestivalView;

    @BeforeEach
    void setUp() {
        eventRepository = mock(IEventRepository.class);
        companyRepository = mock(ICompanyRepository.class);
        tokenService = mock(ITokenService.class);
        logger = mock(ISystemLogger.class);

        domainService = new EventCatalogDomainService(companyRepository);

        eventCatalogService = new EventCatalogService(
                domainService,
                eventRepository,
                companyRepository,
                tokenService,
                logger
        );

        rockConcert = new Event(
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

        cancelledRockConcert = new Event(
                LocalDateTime.now().plusDays(15),
                "Cancelled Rock Concert",
                100L,
                1L,
                EventLocation.TEL_AVIV,
                100L,
                EventCategory.CONCERT,
                "Cancelled Artist",
                BigDecimal.valueOf(59.99),
                new Pair<>(10, 10)
        );

        draftRockConcert = new Event(
                LocalDateTime.now().plusDays(25),
                "Draft Rock Concert",
                100L,
                1L,
                EventLocation.TEL_AVIV,
                100L,
                EventCategory.CONCERT,
                "Draft Artist",
                BigDecimal.valueOf(69.99),
                new Pair<>(10, 10)
        );

        activateEvent(rockConcert);
        activateEvent(theaterShow);
        activateEvent(jazzFestival);
        activateEvent(cancelledRockConcert);

        cancelledRockConcert.cancel();

        setEventId(rockConcert, 1L);
        setEventId(theaterShow, 2L);
        setEventId(jazzFestival, 3L);
        setEventId(cancelledRockConcert, 4L);
        setEventId(draftRockConcert, 5L);

        rockConcertView = createSearchResultView(rockConcert);
        theaterShowView = createSearchResultView(theaterShow);
        jazzFestivalView = createSearchResultView(jazzFestival);
    }

    @Test
    void GivenUserEnteredSystemAndMatchingEventsExist_WhenGlobalSearch_ThenSystemReturnsOnlyActiveMatchingEvents() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setSearchTerm("Rock");

        List<Long> companyIds = List.of(100L, 200L);

        when(tokenService.validateToken(validSessionId)).thenReturn(true);
        when(companyRepository.getCompanyIdsByCriteria(criteria)).thenReturn(companyIds);
        when(eventRepository.searchEvents(criteria, companyIds))
                .thenReturn(List.of(rockConcertView));

        List<EventSearchResultDTO> results =
                eventCatalogService.globalSearch(validSessionId, criteria);

        assertEquals(1, results.size());
        assertTrue(containsEventId(results, rockConcert.getId()));
        assertFalse(containsEventId(results, theaterShow.getId()));
        assertFalse(containsEventId(results, jazzFestival.getId()));

        verify(companyRepository).getCompanyIdsByCriteria(criteria);
        verify(eventRepository).searchEvents(criteria, companyIds);
    }

    @Test
    void GivenUserEnteredSystemAndSearchFiltersMatchOnlyOneEvent_WhenGlobalSearch_ThenSystemReturnsFilteredResults() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setCategory(EventCategory.CONCERT);
        criteria.setLocation(EventLocation.HAIFA);

        List<Long> companyIds = List.of(100L, 200L);

        when(tokenService.validateToken(validSessionId)).thenReturn(true);
        when(companyRepository.getCompanyIdsByCriteria(criteria)).thenReturn(companyIds);
        when(eventRepository.searchEvents(criteria, companyIds))
                .thenReturn(List.of(jazzFestivalView));

        List<EventSearchResultDTO> results =
                eventCatalogService.globalSearch(validSessionId, criteria);

        assertEquals(1, results.size());
        assertTrue(containsEventId(results, jazzFestival.getId()));
        assertFalse(containsEventId(results, rockConcert.getId()));
        assertFalse(containsEventId(results, theaterShow.getId()));

        verify(eventRepository).searchEvents(criteria, companyIds);
    }

    @Test
    void GivenUserEnteredSystemAndNoEventMatchesSearch_WhenGlobalSearch_ThenSystemReturnsEmptyResults() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setSearchTerm("Basketball");

        List<Long> companyIds = List.of(100L, 200L);

        when(tokenService.validateToken(validSessionId)).thenReturn(true);
        when(companyRepository.getCompanyIdsByCriteria(criteria)).thenReturn(companyIds);
        when(eventRepository.searchEvents(criteria, companyIds))
                .thenReturn(List.of());

        List<EventSearchResultDTO> results =
                eventCatalogService.globalSearch(validSessionId, criteria);

        assertNotNull(results);
        assertTrue(results.isEmpty());

        verify(eventRepository).searchEvents(criteria, companyIds);
    }

    @Test
    void GivenUserEnteredSystemAndCancelledEventMatchesSearch_WhenGlobalSearch_ThenSystemDoesNotReturnCancelledEvent() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setSearchTerm("Cancelled Rock");

        List<Long> companyIds = List.of(100L, 200L);

        when(tokenService.validateToken(validSessionId)).thenReturn(true);
        when(companyRepository.getCompanyIdsByCriteria(criteria)).thenReturn(companyIds);
        when(eventRepository.searchEvents(criteria, companyIds))
                .thenReturn(List.of());

        List<EventSearchResultDTO> results =
                eventCatalogService.globalSearch(validSessionId, criteria);

        assertNotNull(results);
        assertTrue(results.isEmpty());

        verify(eventRepository).searchEvents(criteria, companyIds);
    }

    @Test
    void GivenUserEnteredSystemAndDraftEventMatchesSearch_WhenGlobalSearch_ThenSystemDoesNotReturnDraftEvent() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setSearchTerm("Draft Rock");

        List<Long> companyIds = List.of(100L, 200L);

        when(tokenService.validateToken(validSessionId)).thenReturn(true);
        when(companyRepository.getCompanyIdsByCriteria(criteria)).thenReturn(companyIds);
        when(eventRepository.searchEvents(criteria, companyIds))
                .thenReturn(List.of());

        List<EventSearchResultDTO> results =
                eventCatalogService.globalSearch(validSessionId, criteria);

        assertNotNull(results);
        assertTrue(results.isEmpty());

        verify(eventRepository).searchEvents(criteria, companyIds);
    }

    @Test
    void GivenUserEnteredSystemAndCompanyRatingFilterIsApplied_WhenGlobalSearch_ThenSystemReturnsOnlyActiveEventsFromMatchingCompanies() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setCompanyRate(4.5);

        List<Long> companyIds = List.of(100L);

        when(tokenService.validateToken(validSessionId)).thenReturn(true);
        when(companyRepository.getCompanyIdsByCriteria(criteria)).thenReturn(companyIds);
        when(eventRepository.searchEvents(criteria, companyIds))
                .thenReturn(List.of(rockConcertView, jazzFestivalView));

        List<EventSearchResultDTO> results =
                eventCatalogService.globalSearch(validSessionId, criteria);

        assertEquals(2, results.size());
        assertTrue(containsEventId(results, rockConcert.getId()));
        assertTrue(containsEventId(results, jazzFestival.getId()));
        assertFalse(containsEventId(results, theaterShow.getId()));

        verify(companyRepository).getCompanyIdsByCriteria(criteria);
        verify(eventRepository).searchEvents(criteria, companyIds);
    }

    @Test
    void GivenInvalidSession_WhenGlobalSearch_ThenSystemRejectsTheSearch() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setSearchTerm("Rock");

        when(tokenService.validateToken(invalidSessionId)).thenReturn(false);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> eventCatalogService.globalSearch(
                        invalidSessionId,
                        criteria
                )
        );

        assertTrue(exception.getMessage().contains("Invalid session ID"));
    }

    @Test
    void GivenNullSearchCriteria_WhenGlobalSearch_ThenSystemRejectsTheSearch() {
        when(tokenService.validateToken(validSessionId)).thenReturn(true);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> eventCatalogService.globalSearch(
                        validSessionId,
                        null
                )
        );

        assertTrue(
                exception.getMessage()
                        .contains("Search criteria cannot be null")
        );
    }

    @Test
    void GivenUserEnteredSystemAndCompanyHasEvents_WhenSearchByCompany_ThenSystemReturnsOnlyActiveCompanyEvents() {
        long companyId = 100L;
        SearchCriteria criteria = new SearchCriteria();

        when(tokenService.validateToken(validSessionId)).thenReturn(true);
        when(eventRepository.searchEvents(criteria, List.of(companyId)))
                .thenReturn(List.of(
                        rockConcertView,
                        jazzFestivalView
                ));

        List<EventSearchResultDTO> results =
                eventCatalogService.SearchByCompany(
                        validSessionId,
                        companyId,
                        criteria
                );

        assertEquals(2, results.size());
        assertTrue(containsEventId(results, rockConcert.getId()));
        assertTrue(containsEventId(results, jazzFestival.getId()));
        assertFalse(containsEventId(results, theaterShow.getId()));

        verify(eventRepository).searchEvents(
                criteria,
                List.of(companyId)
        );
    }

    @Test
    void GivenUserEnteredSystemAndCompanySearchFiltersMatchOneEvent_WhenSearchByCompany_ThenSystemReturnsFilteredResults() {
        long companyId = 100L;

        SearchCriteria criteria = new SearchCriteria();
        criteria.setSearchTerm("Jazz");

        when(tokenService.validateToken(validSessionId)).thenReturn(true);
        when(eventRepository.searchEvents(criteria, List.of(companyId)))
                .thenReturn(List.of(jazzFestivalView));

        List<EventSearchResultDTO> results =
                eventCatalogService.SearchByCompany(
                        validSessionId,
                        companyId,
                        criteria
                );

        assertEquals(1, results.size());
        assertTrue(containsEventId(results, jazzFestival.getId()));
        assertFalse(containsEventId(results, rockConcert.getId()));

        verify(eventRepository).searchEvents(
                criteria,
                List.of(companyId)
        );
    }

    @Test
    void GivenCompanyExistsButNoEventsMatchFilters_WhenSearchByCompany_ThenSystemReturnsEmptyResults() {
        long companyId = 100L;

        SearchCriteria criteria = new SearchCriteria();
        criteria.setSearchTerm("Opera");

        when(tokenService.validateToken(validSessionId)).thenReturn(true);
        when(eventRepository.searchEvents(criteria, List.of(companyId)))
                .thenReturn(List.of());

        List<EventSearchResultDTO> results =
                eventCatalogService.SearchByCompany(
                        validSessionId,
                        companyId,
                        criteria
                );

        assertNotNull(results);
        assertTrue(results.isEmpty());

        verify(eventRepository).searchEvents(
                criteria,
                List.of(companyId)
        );
    }

    @Test
    void GivenCancelledCompanyEventMatchesSearch_WhenSearchByCompany_ThenSystemDoesNotReturnCancelledEvent() {
        long companyId = 100L;

        SearchCriteria criteria = new SearchCriteria();
        criteria.setSearchTerm("Cancelled Rock");

        when(tokenService.validateToken(validSessionId)).thenReturn(true);
        when(eventRepository.searchEvents(criteria, List.of(companyId)))
                .thenReturn(List.of());

        List<EventSearchResultDTO> results =
                eventCatalogService.SearchByCompany(
                        validSessionId,
                        companyId,
                        criteria
                );

        assertNotNull(results);
        assertTrue(results.isEmpty());

        verify(eventRepository).searchEvents(
                criteria,
                List.of(companyId)
        );
    }

    @Test
    void GivenDraftCompanyEventMatchesSearch_WhenSearchByCompany_ThenSystemDoesNotReturnDraftEvent() {
        long companyId = 100L;

        SearchCriteria criteria = new SearchCriteria();
        criteria.setSearchTerm("Draft Rock");

        when(tokenService.validateToken(validSessionId)).thenReturn(true);
        when(eventRepository.searchEvents(criteria, List.of(companyId)))
                .thenReturn(List.of());

        List<EventSearchResultDTO> results =
                eventCatalogService.SearchByCompany(
                        validSessionId,
                        companyId,
                        criteria
                );

        assertNotNull(results);
        assertTrue(results.isEmpty());

        verify(eventRepository).searchEvents(
                criteria,
                List.of(companyId)
        );
    }

    @Test
    void GivenCompanyRateFilter_WhenSearchByCompany_ThenSystemRejectsTheSearch() {
        long companyId = 100L;

        SearchCriteria criteria = new SearchCriteria();
        criteria.setCompanyRate(4.5);

        when(tokenService.validateToken(validSessionId)).thenReturn(true);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> eventCatalogService.SearchByCompany(
                        validSessionId,
                        companyId,
                        criteria
                )
        );

        assertTrue(
                exception.getMessage()
                        .contains(
                                "Company rating criteria is not applicable"
                        )
        );
    }

    @Test
    void GivenCompanyDoesNotExist_WhenSearchByCompany_ThenSystemReturnsEmptyResults() {
        long nonExistingCompanyId = 999L;
        SearchCriteria criteria = new SearchCriteria();

        when(tokenService.validateToken(validSessionId)).thenReturn(true);
        when(eventRepository.searchEvents(
                criteria,
                List.of(nonExistingCompanyId)
        )).thenReturn(List.of());

        List<EventSearchResultDTO> results =
                eventCatalogService.SearchByCompany(
                        validSessionId,
                        nonExistingCompanyId,
                        criteria
                );

        assertNotNull(results);
        assertTrue(results.isEmpty());

        verify(eventRepository).searchEvents(
                criteria,
                List.of(nonExistingCompanyId)
        );
    }

    @Test
    void GivenInvalidSession_WhenSearchByCompany_ThenSystemRejectsTheSearch() {
        long companyId = 100L;
        SearchCriteria criteria = new SearchCriteria();

        when(tokenService.validateToken(invalidSessionId)).thenReturn(false);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> eventCatalogService.SearchByCompany(
                        invalidSessionId,
                        companyId,
                        criteria
                )
        );

        assertTrue(exception.getMessage().contains("Invalid session ID"));
    }

    @Test
    void GivenNullSearchCriteria_WhenSearchByCompany_ThenSystemRejectsTheSearch() {
        long companyId = 100L;

        when(tokenService.validateToken(validSessionId)).thenReturn(true);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> eventCatalogService.SearchByCompany(
                        validSessionId,
                        companyId,
                        null
                )
        );

        assertTrue(
                exception.getMessage()
                        .contains("Search criteria cannot be null")
        );
    }

    @Test
    void GivenValidSessionAndFeaturedEventsExist_WhenGetFeaturedEvents_ThenSystemReturnsRequestedEvents() {
        int limit = 2;

        when(tokenService.validateToken(validSessionId)).thenReturn(true);
        when(eventRepository.getFeaturedEvents(limit))
                .thenReturn(List.of(
                        jazzFestivalView,
                        rockConcertView
                ));

        List<EventSearchResultDTO> results =
                eventCatalogService.getFeaturedEvents(
                        validSessionId,
                        limit
                );

        assertEquals(2, results.size());
        assertEquals(jazzFestival.getId(), results.get(0).id());
        assertEquals(rockConcert.getId(), results.get(1).id());

        verify(eventRepository).getFeaturedEvents(limit);
    }

    @Test
    void GivenInvalidSession_WhenGetFeaturedEvents_ThenSystemRejectsTheRequest() {
        when(tokenService.validateToken(invalidSessionId)).thenReturn(false);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> eventCatalogService.getFeaturedEvents(
                        invalidSessionId,
                        2
                )
        );

        assertTrue(exception.getMessage().contains("Invalid session ID"));
    }

    @Test
    void GivenInvalidLimit_WhenGetFeaturedEvents_ThenSystemRejectsTheRequest() {
        when(tokenService.validateToken(validSessionId)).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventCatalogService.getFeaturedEvents(
                        validSessionId,
                        0
                )
        );

        assertTrue(
                exception.getMessage()
                        .contains("Limit must be greater than zero")
        );
    }

    private void activateEvent(Event event) {
        event.setStatus(eventStatus.ACTIVE);
    }

    private void setEventId(Event event, Long id) {
        try {
            Field idField = Event.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(event, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Failed to set generated event ID in unit test",
                    exception
            );
        }
    }

    private EventSearchResultView createSearchResultView(Event event) {
        EventSearchResultView view =
                mock(EventSearchResultView.class);

        when(view.getId()).thenReturn(event.getId());
        when(view.getName()).thenReturn(event.getName());
        when(view.getCompanyId()).thenReturn(event.getCompanyId());
        when(view.getDate()).thenReturn(event.getDate());
        when(view.getLocation()).thenReturn(event.getLocation());
        when(view.getCategory()).thenReturn(event.getCategory());
        when(view.getArtistName()).thenReturn(event.getArtistName());
        when(view.getTicketPrice()).thenReturn(event.getMinimalTicketPrice());
        when(view.getRate()).thenReturn(event.getRate());
        when(view.getSaleStatus()).thenReturn(event.getSaleStatus());

        return view;
    }

    private boolean containsEventId(
            List<EventSearchResultDTO> events,
            Long eventId
    ) {
        return events.stream()
                .anyMatch(
                        event -> java.util.Objects.equals(
                                event.id(),
                                eventId
                        )
                );
    }

}
