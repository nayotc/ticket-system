package ticketsystem.DomainLayer.IRepository;

import java.util.List;
import java.util.Optional;

import ticketsystem.DomainLayer.systemAdmin.SystemAdmin;

public interface ISystemAdminRepository {

    void addAdmin(SystemAdmin systemAdmin);

    boolean isSystemAdmin(String adminId);

    Optional<SystemAdmin> findById(String adminId);

    List<SystemAdmin> findAll();

    void deleteById(String adminId);

    int countAdmins();

    SystemAdmin getAdminById(String adminId);
}
