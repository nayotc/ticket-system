package InfrastructureLayer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import DomainLayer.IRepository.ISystemAdminRepository;
import DomainLayer.systemAdmin.SystemAdmin;


public class SystemAdminRepository implements ISystemAdminRepository {

    private final ConcurrentHashMap<String, SystemAdmin> storage = new ConcurrentHashMap<>();

    @Override
    public  synchronized void addAdmin(SystemAdmin systemAdmin) {
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
    public synchronized void deleteById(String adminId) {
        storage.remove(adminId);
    }
}