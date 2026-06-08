package ticketsystem.InfrastructureLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

import ticketsystem.DomainLayer.notifications.Notification;

@Repository
public class NotificationsRepository {

    private final ConcurrentHashMap<String, List<Notification>> notificationsByTarget = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Notification> notificationsById = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0L);

    public Notification save(Notification notification) {
        if (notification.getId() == null) {
            notification.setId(idGenerator.incrementAndGet());
        }
        notificationsByTarget
                .computeIfAbsent(notification.getTargetId(), ignored -> new CopyOnWriteArrayList<>())
                .add(notification);
        notificationsById.put(notification.getId(), notification);
        return notification;
    }

    public List<Notification> findByTargetId(String targetId) {
        List<Notification> notifications = notificationsByTarget.get(targetId);
        if (notifications == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(notifications);
    }

    public void removeById(Long notificationId) {
        Notification notification = notificationsById.get(notificationId);
        if (notification == null) {
            return;
        }

        List<Notification> notifications = notificationsByTarget.get(notification.getTargetId());
        if (notifications != null) {
            notifications.removeIf(notificationItem -> notificationId.equals(notificationItem.getId()));
            if (notifications.isEmpty()) {
                notificationsByTarget.remove(notification.getTargetId(), notifications);
            }
        }
        notificationsById.remove(notificationId);
    }
}
