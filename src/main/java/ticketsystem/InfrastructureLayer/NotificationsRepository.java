package ticketsystem.InfrastructureLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import ticketsystem.DomainLayer.IRepository.INotificationsRepository;
import ticketsystem.DomainLayer.notifications.Notification;

public class NotificationsRepository implements INotificationsRepository {

    private final ConcurrentHashMap<Long, List<Notification>> pendingNotifications = new ConcurrentHashMap<>();

    @Override
    public void save(Notification notification) {
        pendingNotifications.computeIfAbsent(notification.getRecipientMemberId(), k -> new CopyOnWriteArrayList<>()).add(notification);
    }

    @Override
    // show member's pending notifications and mark them as delivered
    public List<Notification> getAndClear(Long memberId) {
        List<Notification> memberNotifications = pendingNotifications.get(memberId);

        if (memberNotifications == null || memberNotifications.isEmpty()) {
            return new ArrayList<>();
        }

        // Create a copy of the notifications to return
        List<Notification> notificationsToReturn = new ArrayList<>(memberNotifications);

        // Clear the pending notifications for this member
        pendingNotifications.remove(memberId);

        return notificationsToReturn;
    }

    public void markAsDelivered(Long notificationId, Long memberId) {
        List<Notification> memberNotifications = pendingNotifications.get(memberId);
        if (memberNotifications != null) {
            for (Notification notification : memberNotifications) {
                if (notification.getId() == notificationId) {
                    memberNotifications.remove(notification);
                    break;
                }
            }
        }
    }
}
