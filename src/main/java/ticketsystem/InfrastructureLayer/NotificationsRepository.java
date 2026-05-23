package ticketsystem.InfrastructureLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import ticketsystem.DomainLayer.IRepository.INotificationsRepository;
import ticketsystem.DomainLayer.notifications.Notification;

public class NotificationsRepository implements INotificationsRepository {

    private final ConcurrentHashMap<Long, List<Notification>> pendingNotifications = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public void save(Notification notification) {
        if (notification != null && notification.getRecipientMemberId() != null) {
            if (notification.getId() == 0) {
                notification.setId(generateNotificationId());
            }

            pendingNotifications
                    .computeIfAbsent(notification.getRecipientMemberId(), k -> new CopyOnWriteArrayList<>())
                    .add(notification);
        }
    }

    @Override
    // show member's pending notifications and mark them as delivered
    public List<Notification> getAndClear(String member_Id) {
        long memberId = Long.parseLong(member_Id);
        List<Notification> memberNotifications = pendingNotifications.get(memberId);

        if (memberNotifications == null || memberNotifications.isEmpty()) {
            return new ArrayList<>();
        }

        List<Notification> pendingNotifications = memberNotifications.stream()
                .filter(Notification::isPending)
                .collect(Collectors.toList());

        for (Notification notification : pendingNotifications) {
            notification.markDelivered();
            save(notification);
        }

        return pendingNotifications;
    }

    @Override
    public long generateNotificationId() {
        return idGenerator.getAndIncrement();
    }

    @Override
    public Notification getById(long notificationId) {
        for (List<Notification> userNotifications : pendingNotifications.values()) {
            for (Notification n : userNotifications) {
                if (n.getId() == notificationId) {
                    return n;
                }
            }
        }
        return null;
    }

    @Override
    public List<Notification> findPendingByMemberId(long memberId) {
        List<Notification> userNotifications = pendingNotifications.get(memberId);
        if (userNotifications == null) {
            return new ArrayList<>();
        }

        return userNotifications.stream()
                .filter(Notification::isPending)
                .collect(Collectors.toList());
    }

    @Override
    public List<Notification> findAllByMemberId(long memberId) {
        List<Notification> userNotifications = pendingNotifications.get(memberId);
        // return a copy to avoid external modifications
        return userNotifications != null ? new ArrayList<>(userNotifications) : new ArrayList<>();
    }

    @Override
    public void markAsDelivered(long notificationId) {
        Notification notification = getById(notificationId);
        if (notification != null) {
            notification.markDelivered();
        }
    }

    @Override
    public void markAsRead(long notificationId) {
        Notification notification = getById(notificationId);
        if (notification != null) {
            notification.markRead();
        }
    }
}
