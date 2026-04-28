package ticketsystem.InfrastructureLayer;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.company.Company;

public class CompanyRepository implements ICompanyRepository{
    private final Map<String, Company> companies = new ConcurrentHashMap<>();

    @Override
    public void save(Company company) {
        companies.put(company.getName(), company);
    }

    @Override
    public boolean existsByName(String name) {
        return companies.containsKey(name);
    }

    @Override
    public Optional<Company> findByName(String name) {
        return Optional.ofNullable(companies.get(name));
    }

    @Override
    public boolean existsById(long id) {
        return companies.containsKey(id);
    }

    @Override
    public Optional<Company> findById(long id) {
        return Optional.ofNullable(companies.get(id));
    }
}
