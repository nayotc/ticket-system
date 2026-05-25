package ticketsystem.ApplicationLayer;

import java.util.List;

import ticketsystem.InfrastructureLayer.NotificationsRepository;

public class NotificationService {

    private final NotificationsRepository notificationRepository;

    public NotificationService(NotificationsRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public List<String> getDelayedNotificationsForUser(Long userId, String sessionId) {
        return notificationRepository.getAndClear(userId);
    }
}
