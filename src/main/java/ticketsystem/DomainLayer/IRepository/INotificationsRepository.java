package ticketsystem.DomainLayer.IRepository;

import java.util.List;

import ticketsystem.DomainLayer.notifications.Notification;

public interface INotificationsRepository {

    Notification save(Notification notification);

    List<Notification> findPendingByTargetId(String targetId);

    void removeById(Long notificationId);

}
