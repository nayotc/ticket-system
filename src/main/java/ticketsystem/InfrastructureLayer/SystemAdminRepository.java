package ticketsystem.InfrastructureLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import ticketsystem.DomainLayer.IRepository.ISystemAdminRepository;
import ticketsystem.DomainLayer.systemAdmin.SystemAdmin;

@Repository
public class SystemAdminRepository implements ISystemAdminRepository {
// Using ConcurrentHashMap for thread safety in case of concurrent access

    private final ConcurrentHashMap<String, SystemAdmin> storage = new ConcurrentHashMap<>();

    @Override
    public void addAdmin(SystemAdmin systemAdmin) {
        storage.put(systemAdmin.getAdminId(), systemAdmin);
    }

    @Override
    public Optional<SystemAdmin> findById(String adminId) {
        return Optional.ofNullable(storage.get(adminId));
    }

    @Override
    public List<SystemAdmin> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public void deleteById(String adminId) {
        storage.remove(adminId);
    }

    @Override
    public int countAdmins() {
        return storage.size();
    }

    @Override
    public boolean isSystemAdmin(String adminId) {
        SystemAdmin admin = storage.get(adminId);
        return admin != null && admin.isActive();
    }

    @Override
    public SystemAdmin getAdminById(String adminId) {
        return storage.get(adminId);
    }
}
