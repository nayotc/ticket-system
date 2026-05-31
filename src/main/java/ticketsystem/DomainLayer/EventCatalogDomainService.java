package ticketsystem.DomainLayer;


import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.company.Company;

@Service
public class EventCatalogDomainService {

    ICompanyRepository companyRepository;
    public EventCatalogDomainService(ICompanyRepository companyRepository){
        this.companyRepository=companyRepository;
    }

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
    
     public BigDecimal calculateFinalPrice(Long companyId,Event event,BigDecimal totalPrice,int ticketCount,String couponCode) {
        if (companyId == null)  throw new IllegalArgumentException("Company id cannot be null");
        if (event == null) throw new IllegalArgumentException("Event cannot be null");
        if (totalPrice == null) throw new IllegalArgumentException("Total price cannot be null");
        if (ticketCount < 0) throw new IllegalArgumentException("Ticket count cannot be negative");
           Company company=companyRepository.findById(companyId).orElseThrow(() -> new IllegalArgumentException("Company not found"));
            BigDecimal companyDiscount =
                    company.calculateDiscountCompany(totalPrice, ticketCount, couponCode);

            BigDecimal priceAfterCompanyDiscount =
                    totalPrice.subtract(companyDiscount);

            if (priceAfterCompanyDiscount.compareTo(BigDecimal.ZERO) < 0) {
                priceAfterCompanyDiscount = BigDecimal.ZERO;
            }

            BigDecimal eventDiscount =event.calculateDiscountEvent(
                            priceAfterCompanyDiscount,
                            ticketCount,
                            couponCode
                    );

            BigDecimal finalPrice =
                    priceAfterCompanyDiscount.subtract(eventDiscount);

            if (finalPrice.compareTo(BigDecimal.ZERO) < 0) {
                finalPrice = BigDecimal.ZERO;
            }
            return finalPrice;

    }
    public void canPurchaseByCompanyPolicy(long companyId, int ticketCount, int buyerAge) {
        companyRepository.findById(companyId).orElseThrow(() -> new IllegalArgumentException("Company not found")).canPurchase(ticketCount, buyerAge);
    }
}
