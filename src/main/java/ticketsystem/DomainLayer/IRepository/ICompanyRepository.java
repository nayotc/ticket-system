package ticketsystem.DomainLayer.IRepository;

import java.util.List;
import java.util.Optional;
import ticketsystem.DomainLayer.company.Company;

public interface ICompanyRepository {
    void save(Company company);
    boolean existsByName(String name);
    Optional<Company> findByName(String name);

    boolean existsById(long id);
    Optional<Company> findById(long id);
    List<Company> findAll();

    // Checks if there is ANY company where this user is the founder
    boolean existsByFounderUsername(String founderUsername);

    // Fetches ONLY the companies where the user is listed in the owners OR managers lists
    List<Company> findByOwnersContainingOrManagersContaining(String owner, String manager);

}
