package ticketsystem.DomainLayer.IRepository;

import java.util.Optional;
import ticketsystem.DomainLayer.company.Company;

public interface ICompanyRepository {
    void save(Company company);
    boolean existsByName(String name);
    Optional<Company> findByName(String name);
}
