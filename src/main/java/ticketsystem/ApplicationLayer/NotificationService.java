package ticketsystem.ApplicationLayer;

import java.util.List;

import org.springframework.stereotype.Service;
import ticketsystem.DomainLayer.IRepository.INotificationsRepository;
import ticketsystem.DomainLayer.notifications.Notification;

@Service
public class NotificationService {

    private final INotificationsRepository notificationRepository;

    public NotificationService(INotificationsRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public List<Notification> getPendingNotifications(String targetId) {
        return notificationRepository.findPendingByTargetId(targetId);
    }

    public void markAsDelivered(Long notificationId) {
        notificationRepository.removeById(notificationId);
    }
}
