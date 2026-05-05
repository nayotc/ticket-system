package ticketsystem.InfrastructureLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.company.Company;

public class CompanyRepository implements ICompanyRepository {
    private final Map<Long, Company> companies = new ConcurrentHashMap<>();

    @Override
    public void save(Company company) {
        companies.put(company.getId(), company); 
    }

    @Override
    public boolean existsByName(String name) {
        return companies.values().stream()
                .anyMatch(c -> c.getName().equals(name));
    }

    @Override
    public Optional<Company> findByName(String name) {
        return companies.values().stream()
                .filter(c -> c.getName().equals(name))
                .findFirst();
    }

    @Override
    public boolean existsById(long id) {
        return companies.containsKey(id);
    }

    @Override
    public Optional<Company> findById(long id) {
        return Optional.ofNullable(companies.get(id));
    }

    @Override
    public List<Company> findAll() {
        return new ArrayList<>(companies.values());
    }
    @Override
    public boolean existsByFounderId(long founderId) {
        // Efficiently checks if any company has this user as its founder
        return companies.values().stream()
                .anyMatch(c -> c.getFounderId() == founderId);
    }

    @Override
    public List<Company> findByOwnersContainingOrManagersContaining(long ownerId, long managerId) {
        // Filters and returns only the companies where the user is an owner or a manager
        return companies.values().stream()
                .filter(c -> c.getOwners().contains(ownerId) || c.getManagers().contains(managerId))
                .collect(Collectors.toList());
    }
}