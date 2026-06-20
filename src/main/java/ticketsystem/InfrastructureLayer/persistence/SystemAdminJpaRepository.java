package ticketsystem.InfrastructureLayer.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import ticketsystem.DomainLayer.systemAdmin.SystemAdmin;

public interface SystemAdminJpaRepository extends JpaRepository<SystemAdmin, String> {

    boolean existsByAdminIdAndActiveTrue(String adminId);
}