package ticketsystem.ApplicationLayer;

import java.util.List;

import ticketsystem.DomainLayer.EventCatalogDomainService;
import ticketsystem.DomainLayer.SearchCriteria;
import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.ApplicationLayer.ISystemLogger.LogLevel;
import ticketsystem.DTO.Event.EventSearchResultDTO;
import ticketsystem.ApplicationLayer.ITokenService;
import ticketsystem.DomainLayer.event.Event;

public class EventCatalogService {

    private final EventCatalogDomainService domainService;
    private final IEventRepository eventRepository;
    private final ICompanyRepository companyRepository;
    private final ITokenService tokenService;
    private final ISystemLogger logger;
    

    public EventCatalogService(EventCatalogDomainService domainService, IEventRepository eventRepository,ICompanyRepository companyRepository, ITokenService tokenService, ISystemLogger logger) {
        this.domainService = domainService;
        this.eventRepository = eventRepository;
        this.companyRepository = companyRepository;
        this.tokenService = tokenService;
        this.logger = logger;
    }

    public List<EventSearchResultDTO> globalSearch(String sessionId, SearchCriteria criteria){
        String context = "criteria=" + String.valueOf(criteria);
        logger.logEvent("started - globalSearch. " + context, LogLevel.INFO);
        try{
            if (!tokenService.validateToken(sessionId)) {
                throw new IllegalArgumentException("Invalid session ID");
            }
            if (criteria == null) {
                throw new IllegalArgumentException("Search criteria cannot be null");
            }
            List<Event> events = eventRepository.getAllEvents();
            List<Long> companiesIds = companyRepository.getCompanyIdsByCriteria(criteria);

            logger.logEvent(
                    "Loaded data - globalSearch. eventsCount=" + events.size()
                            + ", CompanyCount=" + companiesIds.size(),
                    LogLevel.DEBUG);

            List<Event> results = domainService.globalSearch(events, companiesIds, criteria);

            logger.logEvent(
                    "Completed - globalSearch. resultsCount=" + results.size(),
                    LogLevel.INFO);

            return results.stream().map(EventSearchResultDTO::from).toList();
        }
        catch (IllegalArgumentException e) {
            logger.logEvent("Invalid search criteria: " + e.getMessage(), LogLevel.WARN);
            throw e;
        } 
        catch (Exception e) {
            logger.logError("Unexpected system error in globalSearch. " + context + ". reason=" + e.getMessage(),e);
            throw new RuntimeException(
                "An error occurred while performing global search: " + e.getMessage(),e);
        }
    }

    public List<EventSearchResultDTO> SearchByCompany(String sessionId, long companyId, SearchCriteria criteria){
        String context = "companyId=" + companyId + ", criteria=" + String.valueOf(criteria);
        logger.logEvent("Started - SearchByCompany. " + context, LogLevel.INFO);
        try{
            if (!tokenService.validateToken(sessionId)) {
                throw new IllegalArgumentException("Invalid session ID");
            }
            if (criteria == null) {
                throw new IllegalArgumentException("Search criteria cannot be null");
            }
            List<Event> events = eventRepository.getEventsByCompanyId(companyId);

            logger.logEvent("Loaded data - SearchByCompany. companyId=" + companyId
                            + ", companyEventsCount=" + events.size(),LogLevel.DEBUG);
            List<Event> results = domainService.searchByCompany(events, criteria);

            logger.logEvent("Completed - SearchByCompany. resultsCount=" + results.size(),LogLevel.INFO);

            return results.stream().map(EventSearchResultDTO::from).toList(); 
        }
        catch (IllegalArgumentException e) {
            logger.logEvent("Invalid search criteria: " + e.getMessage(), LogLevel.WARN);
            throw e;
        } 
        catch (Exception e) {
            logger.logError("Unexpected system error in SearchByCompany. companyId=" + companyId + ". reason=" + e.getMessage(),e);
            throw new RuntimeException(
                "An error occurred while performing company search: " + e.getMessage(),e);
        }
    }

    
}
