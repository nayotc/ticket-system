package ticketsystem.DomainLayer.IRepository;

import java.util.List;

import ticketsystem.DomainLayer.notifications.Notification;

public interface INotificationsRepository {

    void save(Notification notification);

    List<Notification> getAndClear(Long memberId);

}
