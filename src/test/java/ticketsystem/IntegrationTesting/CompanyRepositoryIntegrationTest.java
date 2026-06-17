package ticketsystem.IntegrationTesting;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ticketsystem.DomainLayer.SearchCriteria;
import ticketsystem.DomainLayer.company.Company;
import ticketsystem.DomainLayer.discount.DiscountCompositionType;
import ticketsystem.DomainLayer.discount.DiscountPolicy;
import ticketsystem.DomainLayer.policy.PurchasePolicy;
import ticketsystem.InfrastructureLayer.persistence.CompanyJpaRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import ticketsystem.InfrastructureLayer.CompanyRepository;

/**
 * Integration tests for the JPA-based company repository.
 *
 * <p>These tests use an embedded test database and the real Hibernate/JPA
 * mappings. They intentionally do not use {@link InMemoryCompanyRepository},
 * because their purpose is to verify persistence, database queries and
 * optimistic locking.</p>
 *
 * <p>PurchasePolicy and DiscountPolicy persistence is not tested here while
 * those fields remain {@code @Transient}. Their save-and-reload tests should
 * be added when the separate policy-persistence implementation is merged.</p>
 */
@DataJpaTest(
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop"
        }
)
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.ANY
)
@Import(CompanyRepository.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CompanyRepositoryIntegrationTest {

    private static final long FIRST_FOUNDER_ID = 101L;
    private static final long SECOND_FOUNDER_ID = 202L;
    private static final long THIRD_FOUNDER_ID = 303L;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyJpaRepository companyJpaRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    /**
     * Removes all companies before every test so that tests remain independent
     * and do not rely on identifiers or state created by another test.
     */
    @BeforeEach
    void cleanDatabase() {
        companyJpaRepository.deleteAll();
    }

    /**
     * Verifies that a new company receives its identifier from the database and
     * that its basic persistent fields can be read from a fresh persistence
     * context.
     */
    @Test
    void GivenNewCompany_WhenSavedAndReloaded_ThenDatabaseGeneratesIdAndFieldsPersist() {
        // Arrange
        Company company = createCompany(
                "Generated ID Productions",
                FIRST_FOUNDER_ID
        );

        assertNull(
                company.getIdOrNull(),
                "A new company must not have an ID before it is persisted."
        );

        // Act
        companyRepository.save(company);

        Long generatedId = company.getIdOrNull();
        Company reloadedCompany = loadFreshCompany(generatedId);

        // Assert
        assertNotNull(
                generatedId,
                "The database should generate an ID during save."
        );

        assertTrue(
                generatedId > 0,
                "The database-generated company ID should be positive."
        );

        assertNotNull(
                reloadedCompany,
                "The saved company should be found in a fresh persistence context."
        );

        assertEquals(
                generatedId.longValue(),
                reloadedCompany.getId()
        );

        assertEquals(
                "Generated ID Productions",
                reloadedCompany.getName()
        );

        assertEquals(
                FIRST_FOUNDER_ID,
                reloadedCompany.getFounderId()
        );

        assertTrue(
                reloadedCompany.isActive(),
                "A newly created company should be active."
        );

        assertEquals(
                0.0,
                reloadedCompany.getRate()
        );
    }

    /**
     * Verifies that closing a company changes the persisted active status and
     * not only the Java object currently held in memory.
     */
    @Test
    void GivenPersistedActiveCompany_WhenClosedAndReloaded_ThenInactiveStatusPersists()
            throws Exception {

        // Arrange
        Company company = createCompany(
                "Inactive State Productions",
                FIRST_FOUNDER_ID
        );

        companyRepository.save(company);

        // Act
        company.closeOrSuspend();
        companyRepository.save(company);

        Company reloadedCompany = loadFreshCompany(company.getId());

        // Assert
        assertNotNull(reloadedCompany);

        assertFalse(
                reloadedCompany.isActive(),
                "The inactive state should remain after loading from the database."
        );
    }

    /**
     * Verifies that the repository query returns identifiers of active
     * companies only.
     */
    @Test
    void GivenActiveAndInactiveCompanies_WhenQueryingCompanyIds_ThenOnlyActiveIdsAreReturned() {
        // Arrange
        Company firstActiveCompany = createCompany(
                "First Active Company",
                FIRST_FOUNDER_ID
        );

        Company secondActiveCompany = createCompany(
                "Second Active Company",
                SECOND_FOUNDER_ID
        );

        Company inactiveCompany = createCompany(
                "Inactive Company",
                THIRD_FOUNDER_ID
        );

        inactiveCompany.inactivate();

        companyRepository.save(firstActiveCompany);
        companyRepository.save(secondActiveCompany);
        companyRepository.save(inactiveCompany);

        SearchCriteria criteria = mock(SearchCriteria.class);
        when(criteria.getCompanyRate()).thenReturn(null);

        // Act
        List<Long> actualCompanyIds =
                companyRepository.getCompanyIdsByCriteria(criteria);

        // Assert
        Set<Long> expectedCompanyIds = Set.of(
                firstActiveCompany.getId(),
                secondActiveCompany.getId()
        );

        assertEquals(
                expectedCompanyIds,
                new HashSet<>(actualCompanyIds),
                "The query should return all and only active company IDs."
        );

        assertFalse(
                actualCompanyIds.contains(inactiveCompany.getId()),
                "An inactive company must not appear in search results."
        );
    }

    /**
     * Verifies that the company search query applies the requested minimum
     * company rating inside the database query.
     */
    @Test
    void GivenCompaniesWithDifferentRates_WhenFilteringByMinimumRate_ThenOnlyMatchingIdsAreReturned() {
        // Arrange
        Company lowRatedCompany = createCompany(
                "Low Rated Company",
                FIRST_FOUNDER_ID
        );

        Company exactRateCompany = createCompany(
                "Exact Rate Company",
                SECOND_FOUNDER_ID
        );

        Company highRatedCompany = createCompany(
                "High Rated Company",
                THIRD_FOUNDER_ID
        );

        lowRatedCompany.setRate(3.5);
        exactRateCompany.setRate(4.0);
        highRatedCompany.setRate(4.8);

        companyRepository.save(lowRatedCompany);
        companyRepository.save(exactRateCompany);
        companyRepository.save(highRatedCompany);

        SearchCriteria criteria = mock(SearchCriteria.class);
        when(criteria.getCompanyRate()).thenReturn(4.0);

        // Act
        List<Long> actualCompanyIds =
                companyRepository.getCompanyIdsByCriteria(criteria);

        // Assert
        Set<Long> expectedCompanyIds = Set.of(
                exactRateCompany.getId(),
                highRatedCompany.getId()
        );

        assertEquals(
                expectedCompanyIds,
                new HashSet<>(actualCompanyIds),
                "Only active companies whose rate is at least the requested rate should be returned."
        );

        assertFalse(
                actualCompanyIds.contains(lowRatedCompany.getId()),
                "A company below the requested minimum rate must be filtered out."
        );
    }

    /**
     * Verifies that passing null search criteria is rejected instead of
     * accidentally returning unfiltered database data.
     */
    @Test
    void GivenNullSearchCriteria_WhenQueryingCompanyIds_ThenIllegalArgumentExceptionIsThrown() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> companyRepository.getCompanyIdsByCriteria(null)
        );

        assertEquals(
                "Search criteria cannot be null",
                exception.getMessage()
        );
    }

    /**
     * Verifies that two different companies may have the same display name,
     * because company names are not defined as unique business identifiers.
     */
    @Test
    void GivenTwoCompaniesWithSameName_WhenSaved_ThenBothReceiveDifferentIds() {
        // Arrange
        Company firstCompany = createCompany(
                "Shared Company Name",
                FIRST_FOUNDER_ID
        );

        Company secondCompany = createCompany(
                "Shared Company Name",
                SECOND_FOUNDER_ID
        );

        // Act
        companyRepository.save(firstCompany);
        companyRepository.save(secondCompany);

        List<Company> storedCompanies = companyRepository.findAll();

        // Assert
        assertNotNull(firstCompany.getIdOrNull());
        assertNotNull(secondCompany.getIdOrNull());

        assertNotEquals(
                firstCompany.getId(),
                secondCompany.getId(),
                "Each company should receive its own database identifier."
        );

        long companiesWithSharedName = storedCompanies.stream()
                .filter(company ->
                        "Shared Company Name".equals(company.getName()))
                .count();

        assertEquals(
                2L,
                companiesWithSharedName,
                "Company names are not unique and both companies should be persisted."
        );
    }

    /**
     * Verifies that an update based on an obsolete entity version is rejected
     * by Hibernate's optimistic locking mechanism.
     */
    @Test
    void GivenTwoDetachedCopies_WhenSecondCopyUpdatesAfterFirst_ThenOptimisticLockingRejectsStaleUpdate() {
        // Arrange
        Company originalCompany = createCompany(
                "Optimistic Lock Company",
                FIRST_FOUNDER_ID
        );

        companyRepository.save(originalCompany);

        long companyId = originalCompany.getId();

        Company firstCopy = loadFreshCompany(companyId);
        Company staleSecondCopy = loadFreshCompany(companyId);

        assertNotNull(firstCopy);
        assertNotNull(staleSecondCopy);

        assertEquals(
                firstCopy.getVersion(),
                staleSecondCopy.getVersion(),
                "Both copies should initially represent the same database version."
        );

        // Act: the first copy performs a valid update.
        firstCopy.setName("First Successful Update");
        companyRepository.save(firstCopy);

        // The second copy still contains the old @Version value.
        staleSecondCopy.setName("Stale Second Update");

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> companyRepository.save(staleSecondCopy)
        );

        // Assert
        assertTrue(
                hasCause(exception, OptimisticLockingFailureException.class)
                        || hasCause(exception, OptimisticLockException.class),
                "Saving a stale company copy should fail because of optimistic locking."
        );

        Company finalStoredCompany = loadFreshCompany(companyId);

        assertNotNull(finalStoredCompany);

        assertEquals(
                "First Successful Update",
                finalStoredCompany.getName(),
                "The stale update must not overwrite the successful update."
        );
    }

    /**
     * Creates a valid company with the policies currently required by the
     * domain constructor.
     */
    private Company createCompany(String name, long founderId) {
        return new Company(
                name,
                founderId,
                PurchasePolicy.noRestrictions(),
                new DiscountPolicy(DiscountCompositionType.MAX)
        );
    }

    /**
     * Loads a company using a new EntityManager so that the returned value
     * cannot come from the persistence context used by a previous save.
     */
    private Company loadFreshCompany(long companyId) {
        EntityManager entityManager =
                entityManagerFactory.createEntityManager();

        try {
            return entityManager.find(Company.class, companyId);
        } finally {
            entityManager.close();
        }
    }

    /**
     * Checks the entire exception chain because the infrastructure repository
     * may wrap the exception thrown by Hibernate.
     */
    private boolean hasCause(
            Throwable throwable,
            Class<? extends Throwable> expectedCauseType
    ) {
        Throwable current = throwable;

        while (current != null) {
            if (expectedCauseType.isInstance(current)) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }
}