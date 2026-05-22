package ticketsystem.InfrastructureLayer;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import ticketsystem.DomainLayer.IRepository.INotificationsRepository;

public class NotificationsRepository implements INotificationsRepository {

    private final ConcurrentHashMap<String, List<String>> pendingNotifications = new ConcurrentHashMap<>();

    @Override
    public void save(String userId, String message) {
        pendingNotifications.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(message);
    }

    @Override
    public List<String> getAndClear(String userId) {
        return pendingNotifications.remove(userId);
    }
}
