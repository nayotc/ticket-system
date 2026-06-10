package ticketsystem.InfrastructureLayer.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ticketsystem.DomainLayer.notifications.Notification;

public interface NotificationJpaRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByTargetId(String targetId);
}
