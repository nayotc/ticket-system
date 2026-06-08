package ticketsystem.DomainLayer.IRepository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ticketsystem.DomainLayer.notifications.Notification;

public interface INotificationsRepository extends JpaRepository<Notification, Long> {

    //Notification save(Notification notification);
    List<Notification> findByTargetId(String targetId);

    // void removeById(Long notificationId);
}
