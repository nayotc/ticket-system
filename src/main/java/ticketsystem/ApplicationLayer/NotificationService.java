package ticketsystem.ApplicationLayer;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ticketsystem.DomainLayer.IRepository.INotificationsRepository;
import ticketsystem.DomainLayer.notifications.Notification;

@Service
public class NotificationService {

    private final INotificationsRepository notificationRepository;

    public NotificationService(INotificationsRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public List<Notification> getPendingNotifications(String targetId) {
        return notificationRepository.findByTargetId(targetId);
    }

    @Transactional
    public void markAsDelivered(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }
}
