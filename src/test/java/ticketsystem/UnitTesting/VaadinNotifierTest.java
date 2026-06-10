package ticketsystem.UnitTesting;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.DomainLayer.notifications.Notification;
import ticketsystem.DomainLayer.notifications.Notification.Type;
import ticketsystem.InfrastructureLayer.InMemoryNotificationsRepository;
import ticketsystem.InfrastructureLayer.VaadinNotifier;

public class VaadinNotifierTest {

    private InMemoryNotificationsRepository notificationsRepository;
    private VaadinNotifier notifier;

    @BeforeEach
    void setUp() {
        notificationsRepository = new InMemoryNotificationsRepository();
        notifier = new VaadinNotifier(notificationsRepository);
    }

    @Test
    void GivenValidMemberAndMessage_WhenNotifyMember_ThenNotificationIsSavedForMember() {
        Long memberId = 10L;

        notifier.notifyMember(memberId, "Test notification");

        List<Notification> notifications
                = notificationsRepository.findByTargetId(memberId.toString());

        assertEquals(1, notifications.size());

        Notification notification = notifications.get(0);

        assertNotNull(notification.getId());
        assertEquals(memberId.toString(), notification.getTargetId());
        assertEquals("Test notification", notification.getMessage());
        assertEquals(Type.INFO, notification.getType());
        assertNull(notification.getCompanyId());
    }

    @Test
    void GivenNullMember_WhenNotifyMember_ThenNoNotificationIsSaved() {
        notifier.notifyMember(null, "Test notification");

        List<Notification> notifications
                = notificationsRepository.findByTargetId("null");

        assertTrue(notifications.isEmpty());
    }

    @Test
    void GivenBlankMessage_WhenNotifyMember_ThenNoNotificationIsSaved() {
        Long memberId = 10L;

        notifier.notifyMember(memberId, "   ");

        List<Notification> notifications
                = notificationsRepository.findByTargetId(memberId.toString());

        assertTrue(notifications.isEmpty());
    }

    @Test
    void GivenSeveralMembers_WhenNotifyMembers_ThenNotificationIsSavedForEachMember() {
        notifier.notifyMembers(List.of(10L, 20L, 30L), "Group notification");

        assertEquals(1, notificationsRepository.findByTargetId("10").size());
        assertEquals(1, notificationsRepository.findByTargetId("20").size());
        assertEquals(1, notificationsRepository.findByTargetId("30").size());
    }

    @Test
    void GivenMembersListWithNull_WhenNotifyMembers_ThenNullMemberIsIgnored() {
        List<Long> memberIds = new ArrayList<>();
        memberIds.add(10L);
        memberIds.add(null);
        memberIds.add(20L);

        notifier.notifyMembers(memberIds, "Group notification");

        assertEquals(1, notificationsRepository.findByTargetId("10").size());
        assertEquals(1, notificationsRepository.findByTargetId("20").size());
        assertTrue(notificationsRepository.findByTargetId("null").isEmpty());
    }

    @Test
    void GivenValidAssignmentNotification_WhenNotifyMemberAssignment_ThenActionNotificationIsSavedWithCompanyId() {
        Long memberId = 10L;
        Long companyId = 99L;

        notifier.notifyMemberAssignment(memberId, "Please approve assignment", companyId);

        List<Notification> notifications
                = notificationsRepository.findByTargetId(memberId.toString());

        assertEquals(1, notifications.size());

        Notification notification = notifications.get(0);

        assertEquals(memberId.toString(), notification.getTargetId());
        assertEquals("Please approve assignment", notification.getMessage());
        assertEquals(companyId, notification.getCompanyId());
        assertEquals(Type.ACTION, notification.getType());
    }

    @Test
    void GivenInvalidAssignmentNotification_WhenNotifyMemberAssignment_ThenNoNotificationIsSaved() {
        Long memberId = 10L;

        notifier.notifyMemberAssignment(memberId, "Please approve assignment", null);

        List<Notification> notifications
                = notificationsRepository.findByTargetId(memberId.toString());

        assertTrue(notifications.isEmpty());
    }
}
