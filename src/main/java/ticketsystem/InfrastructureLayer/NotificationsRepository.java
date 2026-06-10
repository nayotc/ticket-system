package ticketsystem.InfrastructureLayer;

import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ticketsystem.DomainLayer.IRepository.INotificationsRepository;
import ticketsystem.DomainLayer.notifications.Notification;
import ticketsystem.InfrastructureLayer.persistence.NotificationJpaRepository;

@Repository
public class NotificationsRepository implements INotificationsRepository {

    private final NotificationJpaRepository notificationJpaRepository;

    public NotificationsRepository(NotificationJpaRepository notificationJpaRepository) {
        this.notificationJpaRepository = notificationJpaRepository;
    }

    @Override
    @Transactional
    public Notification save(Notification notification) {
        return notificationJpaRepository.save(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> findByTargetId(String targetId) {
        return notificationJpaRepository.findByTargetId(targetId);
    }

    @Override
    @Transactional
    public void deleteById(Long notificationId) {
        notificationJpaRepository.deleteById(notificationId);
    }
}
