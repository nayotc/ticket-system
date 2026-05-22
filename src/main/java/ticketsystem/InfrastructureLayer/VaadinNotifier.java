package ticketsystem.InfrastructureLayer;

import java.util.List;
import java.util.Set;

import ticketsystem.ApplicationLayer.INotifier;

public class VaadinNotifier implements INotifier {

    private final Broadcaster broadcaster;
    private final NotificationsRepository notificationRepository;

    public VaadinNotifier(Broadcaster broadcaster, NotificationsRepository notificationRepository) {
        this.broadcaster = broadcaster;
        this.notificationRepository = notificationRepository;
    }

    @Override
    public void notifyUser(String userId, String message) {
        if (SessionManager.isUserOnline(userId)) {
            // user logged in:send notification immediately to all active sessions of the user
            Set<String> sessionIds = SessionManager.getUserSessions(userId);
            for (String sessionId : sessionIds) {
                broadcaster.broadcast(sessionId, message);
            }
        } else {
            // user not logged in: save the notification for later delivery when the user logs in
            notificationRepository.save(userId, message);
        }
    }

    //called when a user logs in and becomes a subscribed user
    public void handleUserLogin(String userId, String sessionId) {
        SessionManager.registerSession(userId, sessionId);
        List<String> delayedMessages = notificationRepository.getAndClear(userId);
        if (delayedMessages != null) {
            for (String msg : delayedMessages) {
                broadcaster.broadcast(sessionId, msg);
            }
        }
    }

    public void handleUserLogout(String sessionId) {
        SessionManager.removeSession(sessionId);
    }
}
