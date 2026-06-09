package ticketsystem.InfrastructureLayer;

import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ticketsystem.DomainLayer.IRepository.INotificationsRepository;
import ticketsystem.DomainLayer.notifications.Notification;

/**
 * Spring Data JPA implementation of {@link INotificationsRepository}.
 * Activated only with the {@code jpa} profile once a database is configured.
 * {@code findByTargetId} is derived automatically from the {@code targetId} field.
 */
@Repository
@Profile("jpa")
public interface JpaNotificationsRepository extends JpaRepository<Notification, Long>, INotificationsRepository {
}
