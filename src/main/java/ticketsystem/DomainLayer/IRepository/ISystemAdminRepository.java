package ticketsystem.DomainLayer.IRepository;

import java.util.List;
import java.util.Optional;

import ticketsystem.DomainLayer.systemAdmin.SystemAdmin;

public interface ISystemAdminRepository {

    void addAdmin(SystemAdmin systemAdmin);

    Optional<SystemAdmin> findById(String adminId);

    List<SystemAdmin> findAll();

    void deleteById(String adminId);

    int countAdmins();
}
