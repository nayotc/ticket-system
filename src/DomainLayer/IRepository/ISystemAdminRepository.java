package DomainLayer.IRepository;

import java.util.List;
import java.util.Optional;

import DomainLayer.systemAdmin.SystemAdmin;

public interface ISystemAdminRepository {
    void addAdmin(SystemAdmin systemAdmin);
    Optional<SystemAdmin> findById(String adminId);
    List<SystemAdmin> findAll();
    void deleteById(String adminId);
}