package ticketsystem.DomainLayer.IRepository;

import java.util.List;
import java.util.Optional;

import ticketsystem.DomainLayer.SearchCriteria;
import ticketsystem.DomainLayer.company.Company;

public interface ICompanyRepository {
    void save(Company company);
    boolean existsByName(String name);

    boolean existsById(long id);
    Optional<Company> findById(long id);
    List<Company> findAll();

    List<Long> getCompanyIdsByCriteria(SearchCriteria criteria);
}
