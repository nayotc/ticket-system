package ticketsystem.InfrastructureLayer;

import ticketsystem.ApplicationLayer.INotifier;
import ticketsystem.DomainLayer.IRepository.INotificationsRepository;
import ticketsystem.DomainLayer.notifications.Notification;

public class VaadinNotifier implements INotifier {

    private final INotificationsRepository notificationsRepository;

    public VaadinNotifier(INotificationsRepository notificationsRepository) {
        this.notificationsRepository = notificationsRepository;
    }

    @Override
    public void notifyMember(Long memberId, String message) {
        Notification notification = new Notification(memberId.toString(), message);
        Notification savedNotification = notificationsRepository.save(notification);
        Broadcaster.broadcast(memberId.toString(), savedNotification);
    }

    @Override
    public void notifyGuest(String sessionId, String message) {
        Notification notification = new Notification(sessionId, message);
        Broadcaster.broadcast(sessionId, notification);
    }

}
