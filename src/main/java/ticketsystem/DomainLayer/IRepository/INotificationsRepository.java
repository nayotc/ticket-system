package ticketsystem.DomainLayer.IRepository;

import java.util.List;

import ticketsystem.DomainLayer.notifications.Notification;

public interface INotificationsRepository {

    void save(Notification notification);

    List<Notification> getAndClear(String memberId);

    long generateNotificationId();

    Notification getById(long notificationId);

    List<Notification> findPendingByMemberId(long memberId);

    List<Notification> findAllByMemberId(long memberId);

    void markAsDelivered(long notificationId);

    void markAsRead(long notificationId);
}
