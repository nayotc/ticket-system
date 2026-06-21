package ticketsystem.PersistenceTesting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import ticketsystem.DomainLayer.IRepository.ISystemAdminRepository;
import ticketsystem.DomainLayer.systemAdmin.SystemAdmin;
import ticketsystem.InfrastructureLayer.SystemAdminRepository;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.ANY
)
@Import(SystemAdminRepository.class)
class SystemAdminPersistenceTest {

    @Autowired
    private ISystemAdminRepository systemAdminRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void GivenSystemAdmin_WhenPersistedAndReloaded_ThenStateIsRestored() {
        SystemAdmin systemAdmin = new SystemAdmin(
                "101",
                "admin101@test.com",
                true
        );

        systemAdminRepository.addAdmin(systemAdmin);

        entityManager.flush();
        entityManager.clear();

        SystemAdmin reloadedAdmin = systemAdminRepository
                .findById("101")
                .orElseThrow();

        assertEquals("101", reloadedAdmin.getAdminId());
        assertEquals("admin101@test.com", reloadedAdmin.getUsername());
        assertTrue(reloadedAdmin.isActive());
        assertTrue(systemAdminRepository.isSystemAdmin("101"));
    }

    @Test
    void GivenInactiveSystemAdmin_WhenChecked_ThenIsSystemAdminReturnsFalse() {
        SystemAdmin systemAdmin = new SystemAdmin(
                "102",
                "admin102@test.com",
                false
        );

        systemAdminRepository.addAdmin(systemAdmin);

        entityManager.flush();
        entityManager.clear();

        assertTrue(systemAdminRepository.findById("102").isPresent());
        assertFalse(systemAdminRepository.isSystemAdmin("102"));
    }

    @Test
    void GivenInactiveSystemAdmin_WhenActivatedAndSaved_ThenActivationPersists() {
        SystemAdmin systemAdmin = new SystemAdmin(
                "103",
                "admin103@test.com",
                false
        );

        systemAdminRepository.addAdmin(systemAdmin);

        entityManager.flush();
        entityManager.clear();

        SystemAdmin persistedAdmin = systemAdminRepository
                .findById("103")
                .orElseThrow();

        persistedAdmin.activate();
        systemAdminRepository.addAdmin(persistedAdmin);

        entityManager.flush();
        entityManager.clear();

        SystemAdmin reloadedAdmin = systemAdminRepository
                .findById("103")
                .orElseThrow();

        assertTrue(reloadedAdmin.isActive());
        assertTrue(systemAdminRepository.isSystemAdmin("103"));
    }

    @Test
    void GivenSystemAdmins_WhenCounted_ThenCorrectCountIsReturned() {
        systemAdminRepository.addAdmin(
                new SystemAdmin("104", "admin104@test.com", true)
        );

        systemAdminRepository.addAdmin(
                new SystemAdmin("105", "admin105@test.com", false)
        );

        entityManager.flush();
        entityManager.clear();

        assertEquals(2, systemAdminRepository.countAdmins());
    }

    @Test
    void GivenSystemAdmin_WhenDeleted_ThenItCannotBeLoaded() {
        systemAdminRepository.addAdmin(
                new SystemAdmin("106", "admin106@test.com", true)
        );

        entityManager.flush();
        entityManager.clear();

        systemAdminRepository.deleteById("106");

        entityManager.flush();
        entityManager.clear();

        assertTrue(systemAdminRepository.findById("106").isEmpty());
        assertFalse(systemAdminRepository.isSystemAdmin("106"));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void GivenTwoCopiesOfSystemAdmin_WhenStaleCopyIsSaved_ThenOptimisticLockingFailureIsTranslated() {
        String adminId = "107";

        try {
            systemAdminRepository.addAdmin(
                    new SystemAdmin(
                            adminId,
                            "admin107@test.com",
                            false
                    )
            );

            SystemAdmin firstCopy = systemAdminRepository
                    .findById(adminId)
                    .orElseThrow();

            SystemAdmin staleCopy = systemAdminRepository
                    .findById(adminId)
                    .orElseThrow();

            long staleVersion = staleCopy.getVersion();

            firstCopy.activate();
            systemAdminRepository.addAdmin(firstCopy);

            SystemAdmin stateAfterFirstUpdate = systemAdminRepository
                    .findById(adminId)
                    .orElseThrow();

            assertTrue(stateAfterFirstUpdate.isActive());
            assertTrue(stateAfterFirstUpdate.getVersion() > staleVersion);

            staleCopy.deactivate();

            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> systemAdminRepository.addAdmin(staleCopy)
            );

            assertTrue(
                    exception.getCause() instanceof OptimisticLockingFailureException,
                    "The repository should translate the optimistic-locking failure"
            );

            SystemAdmin reloadedAdmin = systemAdminRepository
                    .findById(adminId)
                    .orElseThrow();

            assertTrue(
                    reloadedAdmin.isActive(),
                    "The successful first update should remain persisted"
            );

            assertEquals(
                    stateAfterFirstUpdate.getVersion(),
                    reloadedAdmin.getVersion(),
                    "The stale update must not modify the persisted version"
            );
        } finally {
            systemAdminRepository.deleteById(adminId);
        }
    }
}