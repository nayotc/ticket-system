package ticketsystem.InfrastructureLayer.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ticketsystem.DomainLayer.company.Company;

public interface CompanyJpaRepository extends JpaRepository<Company, Long> {

    boolean existsByName(String name);

    @Query("SELECT c.id FROM Company c WHERE c.active = true")
    List<Long> findActiveCompanyIds();

    @Query("SELECT c.id FROM Company c WHERE c.active = true AND c.rate >= :rate")
    List<Long> findActiveCompanyIdsByMinimumRate(@Param("rate") Double rate);

    @Query("SELECT c FROM Company c WHERE c.active = true")
    List<Company> findAllActiveCompanies();
}