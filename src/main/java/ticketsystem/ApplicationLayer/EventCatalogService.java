package ticketsystem.ApplicationLayer;

import java.util.List;

import ticketsystem.DomainLayer.EventCatalogDomainService;
import ticketsystem.DomainLayer.SearchCriteria;
import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.ApplicationLayer.ITokenService;
import ticketsystem.DomainLayer.event.Event;

public class EventCatalogService {

    private final EventCatalogDomainService domainService;
    private final IEventRepository eventRepository;
    private final ICompanyRepository companyRepository;
    private final ITokenService tokenService;
    

    public EventCatalogService(EventCatalogDomainService domainService, IEventRepository eventRepository,ICompanyRepository companyRepository, ITokenService tokenService) {
        this.domainService = domainService;
        this.eventRepository = eventRepository;
        this.companyRepository = companyRepository;
        this.tokenService = tokenService;
    }

    public List<Event> globalSearch(String sessionId, SearchCriteria criteria){
        try{
            if (!tokenService.validateToken(sessionId)) {
                throw new IllegalArgumentException("Invalid session ID");
            }
            if (criteria == null) {
                throw new IllegalArgumentException("Search criteria cannot be null");
            }
            List<Event> events = eventRepository.getAllEvents();
            List<Long> companiesIds = companyRepository.getCompanyIdsByCriteria(criteria);

            return domainService.globalSearch(events, companiesIds, criteria);
        }
        catch (Exception e){
            // Log the exception (not implemented here)
            throw new RuntimeException("An error occurred while performing global search: " + e.getMessage(), e);
        }
    }

    public List<Event> SearchByCompany(String sessionId, long companyId, SearchCriteria criteria){
        try{
            if (!tokenService.validateToken(sessionId)) {
                throw new IllegalArgumentException("Invalid session ID");
            }
            if (criteria == null) {
                throw new IllegalArgumentException("Search criteria cannot be null");
            }
            List<Event> events = eventRepository.getEventsByCompanyId(companyId);
            return domainService.searchByCompany(events, criteria);
        }
        catch (Exception e){
            // Log the exception (not implemented here)
            throw new RuntimeException("An error occurred while performing company search: " + e.getMessage(), e);
        }
    }

    
}
