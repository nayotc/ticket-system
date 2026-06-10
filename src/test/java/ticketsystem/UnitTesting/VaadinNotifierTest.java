package ticketsystem.UnitTesting;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.DomainLayer.notifications.Notification;
import ticketsystem.DomainLayer.notifications.Notification.Type;
import ticketsystem.InfrastructureLayer.NotificationsRepository;
import ticketsystem.InfrastructureLayer.VaadinNotifier;

public class VaadinNotifierTest {

    private NotificationsRepository notificationsRepository;
    private VaadinNotifier notifier;

    @BeforeEach
    void setUp() {
        notificationsRepository = new NotificationsRepository();
        notifier = new VaadinNotifier(notificationsRepository);
    }

    @Test
    void GivenValidMemberAndMessage_WhenNotifyMember_ThenNotificationIsSavedForMember() {
        Long memberId = 10L;

        notifier.notifyMember(memberId, "Test notification");

        List<Notification> notifications =
                notificationsRepository.findPendingByTargetId(memberId.toString());

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

        List<Notification> notifications =
                notificationsRepository.findPendingByTargetId("null");

        assertTrue(notifications.isEmpty());
    }

    @Test
    void GivenBlankMessage_WhenNotifyMember_ThenNoNotificationIsSaved() {
        Long memberId = 10L;

        notifier.notifyMember(memberId, "   ");

        List<Notification> notifications =
                notificationsRepository.findPendingByTargetId(memberId.toString());

        assertTrue(notifications.isEmpty());
    }

    @Test
    void GivenSeveralMembers_WhenNotifyMembers_ThenNotificationIsSavedForEachMember() {
        notifier.notifyMembers(List.of(10L, 20L, 30L), "Group notification");

        assertEquals(1, notificationsRepository.findPendingByTargetId("10").size());
        assertEquals(1, notificationsRepository.findPendingByTargetId("20").size());
        assertEquals(1, notificationsRepository.findPendingByTargetId("30").size());
    }

    @Test
    void GivenMembersListWithNull_WhenNotifyMembers_ThenNullMemberIsIgnored() {
        List<Long> memberIds = new ArrayList<>();
        memberIds.add(10L);
        memberIds.add(null);
        memberIds.add(20L);

        notifier.notifyMembers(memberIds, "Group notification");

        assertEquals(1, notificationsRepository.findPendingByTargetId("10").size());
        assertEquals(1, notificationsRepository.findPendingByTargetId("20").size());
        assertTrue(notificationsRepository.findPendingByTargetId("null").isEmpty());
    }

    @Test
    void GivenValidAssignmentNotification_WhenNotifyMemberAssignment_ThenActionNotificationIsSavedWithCompanyId() {
        Long memberId = 10L;
        Long companyId = 99L;

        notifier.notifyMemberAssignment(memberId, "Please approve assignment", companyId);

        List<Notification> notifications =
                notificationsRepository.findPendingByTargetId(memberId.toString());

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

        List<Notification> notifications =
                notificationsRepository.findPendingByTargetId(memberId.toString());

        assertTrue(notifications.isEmpty());
    }
}