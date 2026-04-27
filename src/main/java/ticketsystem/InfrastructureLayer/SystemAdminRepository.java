package ticketsystem.InfrastructureLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import ticketsystem.ApplicationLayer.ISystemLogger;
import ticketsystem.DomainLayer.IRepository.ISystemAdminRepository;
import ticketsystem.DomainLayer.systemAdmin.SystemAdmin;

public class SystemAdminRepository implements ISystemAdminRepository {
// Using ConcurrentHashMap for thread safety in case of concurrent access

    private final ConcurrentHashMap<String, SystemAdmin> storage = new ConcurrentHashMap<>();
    private final ISystemLogger logger = new LogbackSystemLogger();

    @Override
    public void addAdmin(SystemAdmin systemAdmin) {
        logger.logEvent("addAdmin", "Adding new system admin with ID: " + systemAdmin.getAdminId());
        storage.put(systemAdmin.getAdminId(), systemAdmin);
    }

    @Override
    public Optional<SystemAdmin> findById(String adminId) {
        logger.logEvent("findById", "Finding system admin with ID: " + adminId);
        return Optional.ofNullable(storage.get(adminId));
    }

    @Override
    public List<SystemAdmin> findAll() {
        logger.logEvent("findAll", "Retrieving all system admins. Total count: " + storage.size());
        return new ArrayList<>(storage.values());
    }

    @Override
    public void deleteById(String adminId) {
        logger.logEvent("deleteById", "Deleting system admin with ID: " + adminId);
        storage.remove(adminId);
    }

    @Override
    public int countAdmins() {
        logger.logEvent("countAdmins", "Counting total system admins. Current count: " + storage.size());
        return storage.size();
    }
}
