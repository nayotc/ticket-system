package ticketsystem.InfrastructureLayer;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import ticketsystem.DomainLayer.SearchCriteria;
import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.company.Company;

public class CompanyRepository implements ICompanyRepository {
    private final ConcurrentHashMap<Long, Company> companies = new ConcurrentHashMap<>(); 
   @Override
    public void save(Company company) {
        Company currentCompany = companies.get(company.getId()); 

        if (currentCompany == null) {
            companies.put(company.getId(), new Company(company));
        } else {
            Company updatedCompany = new Company(company); 
            updatedCompany.setVersion(company.getVersion() + 1); 

            boolean replaced = companies.replace(company.getId(), currentCompany, updatedCompany); 
            
            if (!replaced) {
                throw new RuntimeException("OptimisticLockingFailureException: Company " + company.getId() + " version mismatch or concurrent modification."); 
            }
            
            company.setVersion(updatedCompany.getVersion());
        }
    }

    @Override
    public Optional<Company> findById(long id) {
        Company dbCompany = companies.get(id); 
        if (dbCompany != null) {
            return Optional.of(new Company(dbCompany)); 
        }
        return Optional.empty(); 
    }

    @Override
    public Optional<Company> findByName(String name) {
        return companies.values().stream()
                .filter(c -> c.getName().equals(name))
                .findFirst()
                .map(Company::new); 
    }

    @Override
    public List<Company> findAll() {
        return companies.values().stream()
                .map(Company::new) 
                .collect(Collectors.toList());
    }
    
    
    @Override
    public boolean existsByName(String name) {
        return companies.values().stream()
                .anyMatch(c -> c.getName().equals(name));
    }

    @Override
    public boolean existsById(long id) {
        return companies.containsKey(id);
    }

    @Override
    public List<Long> getCompanyIdsByCriteria(SearchCriteria criteria) {
        if (criteria == null) {
            throw new IllegalArgumentException("Search criteria cannot be null");
        }
        Double requestedRate = criteria.getCompanyRate();
        return companies.values().stream()
                .filter(company -> company != null)
                .filter(company -> company.isActive())
                .filter(company -> matchesCompanyRate(company, requestedRate))
                .map(Company::getId)
                .collect(Collectors.toList());
    }

    private boolean matchesCompanyRate(Company company, Double requestedRate) {
        if (requestedRate == null) {
            return true; 
        }

        return company.getRate() >= requestedRate;
    }
}