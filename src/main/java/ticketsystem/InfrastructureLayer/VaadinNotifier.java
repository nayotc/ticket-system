package ticketsystem.InfrastructureLayer;

import java.util.List;

import ticketsystem.ApplicationLayer.INotifier;
import ticketsystem.DomainLayer.notifications.Notification;

public class VaadinNotifier implements INotifier {

    private final Broadcaster broadcaster;
    private final NotificationsRepository notificationRepository;

    public VaadinNotifier(Broadcaster broadcaster, NotificationsRepository notificationRepository) {
        this.broadcaster = broadcaster;
        this.notificationRepository = notificationRepository;
    }

    @Override
    public void notifyUser(String sessionId, String message) {
        String userId = SessionManager.getUserIdForSession(sessionId);
        if (SessionManager.isUserOnline(userId)) {
            // user logged in:send notification immediately to user
            broadcaster.broadcast(sessionId, message, notificationRepository);
        } else {
            // user not logged in: save the notification for later delivery when the user logs in
            Notification notification = new Notification(Long.parseLong(userId), sessionId, message);
            notificationRepository.save(notification);
        }
    }

    //called when a user logs in and becomes a subscribed user
    public void handleUserLogin(String userId, String sessionId) {
        SessionManager.registerSession(userId, sessionId);
        List<Notification> delayedMessages = notificationRepository.getAndClear(userId);
        if (delayedMessages != null) {
            for (Notification msg : delayedMessages) {
                broadcaster.broadcast(sessionId, msg.getMessage(), notificationRepository);
            }
        }
    }

    public void handleUserLogout(String sessionId) {
        SessionManager.removeSession(sessionId);
    }
}
