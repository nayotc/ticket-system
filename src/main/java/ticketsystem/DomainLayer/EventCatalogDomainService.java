package ticketsystem.DomainLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.jsonwebtoken.lang.Objects;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.company.Company;

public class EventCatalogDomainService {

    public List<Event> globalSearch(List<Event> events, List<Long> companies, SearchCriteria criteria) {
        if (events == null) {
            throw new IllegalArgumentException("Events list cannot be null");
        }

        if (companies == null) {
            throw new IllegalArgumentException("Companies list cannot be null");
        }

        if (criteria == null) {
            throw new IllegalArgumentException("Search criteria cannot be null");
        }

        return events.stream()
                .filter(event -> event != null)
                .filter(event -> matchesSearchCriteria(event,companies,criteria))
                .collect(Collectors.toList());
    }

    public List<Event> searchByCompany(List<Event> events, SearchCriteria criteria) {
        if (events == null) {
            throw new IllegalArgumentException("Events list cannot be null");
        }

        if (criteria == null) {
            throw new IllegalArgumentException("Search criteria cannot be null");
        }

        if (criteria.getCompanyRate() != null) {
            throw new IllegalArgumentException("Company rating criteria is not applicable for company-specific search");
        }

        return events.stream()
                .filter(event -> event != null)
                .filter(event -> event.matchesSearchCriteria(criteria))
                .collect(Collectors.toList());
    }

    private boolean matchesSearchCriteria(Event event, List<Long> companies, SearchCriteria criteria) {
        if (!companies.contains(event.getCompanyId())) {
            return false; // Event does not belong to one of the specified companies
        }
        return event.matchesSearchCriteria(criteria);
    }
    
}
