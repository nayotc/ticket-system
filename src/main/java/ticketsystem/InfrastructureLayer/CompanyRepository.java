package ticketsystem.InfrastructureLayer;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.SearchCriteria;
import ticketsystem.DomainLayer.company.Company;
import ticketsystem.InfrastructureLayer.persistence.CompanyJpaRepository;

@Repository
public class CompanyRepository implements ICompanyRepository {

    private final CompanyJpaRepository companyJpaRepository;

    public CompanyRepository(CompanyJpaRepository companyJpaRepository) {
        this.companyJpaRepository = companyJpaRepository;
    }

    @Override
    public void save(Company company) {
        if (company == null) {
            throw new IllegalArgumentException("Company cannot be null");
        }

        try {
            companyJpaRepository.save(company);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new RuntimeException(
                    "OptimisticLockingFailureException: Company "
                            + company.getId()
                            + " version mismatch or concurrent modification.",
                    e
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to save company " + company.getId(), e);
        }
    }

    @Override
    public Optional<Company> findById(long id) {
        try {
            return companyJpaRepository.findById(id);
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to find company by id " + id, e);
        }
    }


    @Override
    public List<Company> findAll() {
        try {
            return companyJpaRepository.findAll();
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to find all companies", e);
        }
    }

    @Override
    public boolean existsByName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }

        try {
            return companyJpaRepository.existsByName(name);
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to check if company exists by name " + name, e);
        }
    }

    @Override
    public boolean existsById(long id) {
        try {
            return companyJpaRepository.existsById(id);
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to check if company exists by id " + id, e);
        }
    }

    @Override
    public List<Long> getCompanyIdsByCriteria(SearchCriteria criteria) {
        if (criteria == null) {
            throw new IllegalArgumentException("Search criteria cannot be null");
        }

        try {
            Double requestedRate = criteria.getCompanyRate();

            if (requestedRate == null) {
                return companyJpaRepository.findActiveCompanyIds();
            }

            return companyJpaRepository.findActiveCompanyIdsByMinimumRate(requestedRate);
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to get company ids by search criteria", e);
        }
    }
}