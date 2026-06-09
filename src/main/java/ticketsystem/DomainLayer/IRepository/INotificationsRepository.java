package ticketsystem.DomainLayer.IRepository;

import java.util.List;

import ticketsystem.DomainLayer.notifications.Notification;

public interface INotificationsRepository {

    Notification save(Notification notification);

    List<Notification> findByTargetId(String targetId);

    void deleteById(Long notificationId);
}
