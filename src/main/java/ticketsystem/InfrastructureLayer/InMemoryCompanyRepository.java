package ticketsystem.InfrastructureLayer;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.SearchCriteria;
import ticketsystem.DomainLayer.company.Company;

public class InMemoryCompanyRepository implements ICompanyRepository {

    private final ConcurrentHashMap<Long, Company> companies = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public synchronized void save(Company company) {
        if (company == null) {
            throw new IllegalArgumentException("Company cannot be null");
        }

        if (company.getId() <= 0) {
            company.setId(idCounter.getAndIncrement());
        }

        Company currentCompany = companies.get(company.getId());

        Company copyToStore = new Company(company);

        if (currentCompany != null) {
            if (company.getVersion() != currentCompany.getVersion()) {
                throw new RuntimeException(
                        "OptimisticLockingFailureException: Company "
                                + company.getId()
                                + " version mismatch or concurrent modification."
                );
            }

            copyToStore.setVersion(company.getVersion() + 1);
            company.setVersion(copyToStore.getVersion());
        }

        companies.put(company.getId(), copyToStore);
    }

    @Override
    public Optional<Company> findById(long id) {
        Company company = companies.get(id);

        if (company == null) {
            return Optional.empty();
        }

        return Optional.of(new Company(company));
    }

    @Override
    public Optional<Company> findByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }

        return companies.values().stream()
                .filter(company -> company.getName().equals(name))
                .min(Comparator.comparingLong(Company::getId))
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
        if (name == null || name.isBlank()) {
            return false;
        }

        return companies.values().stream()
                .anyMatch(company -> company.getName().equals(name));
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
                .filter(Company::isActive)
                .filter(company -> requestedRate == null || company.getRate() >= requestedRate)
                .map(Company::getId)
                .collect(Collectors.toList());
    }

    public void clearForTests() {
        companies.clear();
        idCounter.set(1);
    }
}