package ticketsystem.AcceptanceTesting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import ticketsystem.ApplicationLayer.NotificationService;
import ticketsystem.DomainLayer.notifications.Notification;
import ticketsystem.DomainLayer.notifications.Notification.Type;
import ticketsystem.InfrastructureLayer.NotificationsRepository;

@SpringBootTest
@Transactional
public class NotificationServiceAcceptanceTest {

    @Autowired
    private NotificationsRepository notificationsRepository;

    @Autowired
    private NotificationService notificationService;

    @Test
    void AcceptanceTest_WhenNotificationSaved_ThenGetPendingReturnsIt() {
        Notification saved = notificationsRepository.save(
                new Notification("42", "Pending message", Type.INFO));

        var pending = notificationService.getPendingNotifications("42");

        assertEquals(1, pending.size());
        assertEquals(saved.getId(), pending.get(0).getId());
        assertEquals("Pending message", pending.get(0).getMessage());
    }

    @Test
    void AcceptanceTest_WhenMarkAsDelivered_ThenNotificationIsRemoved() {
        Notification saved = notificationsRepository.save(
                new Notification("42", "Delivered message", Type.INFO));

        notificationService.markAsDelivered(saved.getId());

        assertTrue(notificationService.getPendingNotifications("42").isEmpty());
    }

    @Test
    void AcceptanceTest_WhenNoNotificationsForTarget_ThenGetPendingReturnsEmptyList() {
        assertTrue(notificationService.getPendingNotifications("missing-target").isEmpty());
    }
}
