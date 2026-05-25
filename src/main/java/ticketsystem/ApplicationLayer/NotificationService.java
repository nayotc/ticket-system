package ticketsystem.ApplicationLayer;

import java.util.List;

import ticketsystem.DomainLayer.notifications.Notification;
import ticketsystem.InfrastructureLayer.NotificationsRepository;

public class NotificationService {

    private final NotificationsRepository notificationRepository;
    private final INotifier notifier;

    public NotificationService(NotificationsRepository notificationRepository, INotifier notifier) {
        this.notificationRepository = notificationRepository;
        this.notifier = notifier;
    }

    public void sendNotificationToGuest(String sessionId, String message) {
        notifier.notifyGuest(sessionId, message);
    }

    public void sendNotificationToMember(Long memberId, String message) {
        Notification notification = new Notification(memberId, message);
        notificationRepository.save(notification);
        notifier.notifyMember(notification);
    }

    public List<Notification> getDelayedNotificationsForUser(Long userId, String sessionId) {
        return notificationRepository.getAndClear(userId);
    }

    public void markAsDeliverd(Long notificationId, Long memberId) {
        notificationRepository.markAsDelivered(notificationId, memberId);
    }
}
