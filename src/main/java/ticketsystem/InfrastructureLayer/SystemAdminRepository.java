package ticketsystem.InfrastructureLayer;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import ticketsystem.DomainLayer.IRepository.ISystemAdminRepository;
import ticketsystem.DomainLayer.systemAdmin.SystemAdmin;
import ticketsystem.InfrastructureLayer.persistence.SystemAdminJpaRepository;

@Repository
public class SystemAdminRepository implements ISystemAdminRepository {

    private final SystemAdminJpaRepository systemAdminJpaRepository;

    public SystemAdminRepository(SystemAdminJpaRepository systemAdminJpaRepository) {
        this.systemAdminJpaRepository = systemAdminJpaRepository;
    }

    @Override
    public void addAdmin(SystemAdmin systemAdmin) {
        if (systemAdmin == null) {
            throw new IllegalArgumentException("System admin cannot be null");
        }

        try {
            systemAdminJpaRepository.saveAndFlush(systemAdmin);
        } catch (OptimisticLockingFailureException e) {
            throw new RuntimeException(
                    "OptimisticLockingFailureException: System admin "
                            + systemAdmin.getAdminId()
                            + " version mismatch or concurrent modification.",
                    e
            );
        } catch (DataAccessException e) {
            throw new RuntimeException(
                    "Failed to save system admin " + systemAdmin.getAdminId(),
                    e
            );
        }
    }

    @Override
    public boolean isSystemAdmin(String adminId) {
        if (adminId == null || adminId.isBlank()) {
            return false;
        }

        try {
            return systemAdminJpaRepository.existsByAdminIdAndActiveTrue(adminId);
        } catch (DataAccessException e) {
            throw new RuntimeException(
                    "Failed to check system admin status for " + adminId,
                    e
            );
        }
    }

    @Override
    public Optional<SystemAdmin> findById(String adminId) {
        if (adminId == null || adminId.isBlank()) {
            return Optional.empty();
        }

        try {
            return systemAdminJpaRepository.findById(adminId);
        } catch (DataAccessException e) {
            throw new RuntimeException(
                    "Failed to find system admin by ID " + adminId,
                    e
            );
        }
    }

    @Override
    public List<SystemAdmin> findAll() {
        try {
            return systemAdminJpaRepository.findAll();
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to find all system admins", e);
        }
    }

    @Override
    public void deleteById(String adminId) {
        if (adminId == null || adminId.isBlank()) {
            return;
        }

        try {
            systemAdminJpaRepository.deleteById(adminId);
        } catch (DataAccessException e) {
            throw new RuntimeException(
                    "Failed to delete system admin " + adminId,
                    e
            );
        }
    }

    @Override
    public int countAdmins() {
        try {
            return Math.toIntExact(systemAdminJpaRepository.count());
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to count system admins", e);
        }
    }

    @Override
    public SystemAdmin getAdminById(String adminId) {
        return findById(adminId).orElse(null);
    }
}