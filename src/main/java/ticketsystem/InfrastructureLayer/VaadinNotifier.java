package ticketsystem.InfrastructureLayer;


import java.util.Collection;

import ticketsystem.ApplicationLayer.INotifier;
import ticketsystem.DomainLayer.IRepository.INotificationsRepository;
import ticketsystem.DomainLayer.notifications.Notification;
import ticketsystem.DomainLayer.notifications.Notification.Type;

@Service
public class VaadinNotifier implements INotifier {

    private final INotificationsRepository notificationsRepository;

    public VaadinNotifier(INotificationsRepository notificationsRepository) {
        this.notificationsRepository = notificationsRepository;
    }

    @Override
    public void notifyMember(Long memberId, String message) {
        if (memberId == null || message == null || message.isBlank()) {
            return;
        }

        Notification notification = new Notification(memberId.toString(), message, Type.INFO);
        Notification savedNotification = notificationsRepository.save(notification);

        Broadcaster.broadcast(memberId.toString(), savedNotification);
    }

    @Override
    public void notifyGuest(String sessionId, String message) {
        if (sessionId == null || sessionId.isBlank() || message == null || message.isBlank()) {
            return;
        }

        Notification notification = new Notification(sessionId, message, Type.INFO);

        Broadcaster.broadcast(sessionId, notification);
    }

    @Override
    public void notifyMembers(Collection<Long> memberIds, String message) {
        if (memberIds == null || memberIds.isEmpty()) {
            return;
        }

        for (Long memberId : memberIds) {
            if (memberId != null) {
                notifyMember(memberId, message);
            }
        }
    }

    @Override
    public void notifyGuests(Collection<String> guestTokens, String message) {
        if (guestTokens == null || guestTokens.isEmpty()) {
            return;
        }

        for (String guestToken : guestTokens) {
            if (guestToken != null && !guestToken.isBlank()) {
                notifyGuest(guestToken, message);
            }
        }

    }
    @Override
    public void notifyMemberAssignment(Long memberId, String message, Long companyId) {
        if (memberId == null || message == null || message.isBlank() || companyId == null) {
            return;
        }

        Notification notification = new Notification(memberId.toString(), message, companyId, Type.ACTION);
        Notification savedNotification = notificationsRepository.save(notification);
        Broadcaster.broadcast(memberId.toString(), savedNotification);
    }
}