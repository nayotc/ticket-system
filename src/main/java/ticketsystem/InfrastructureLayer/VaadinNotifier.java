package ticketsystem.InfrastructureLayer;

import ticketsystem.ApplicationLayer.INotifier;
import ticketsystem.DomainLayer.notifications.Notification;

public class VaadinNotifier implements INotifier {

    private final Broadcaster broadcaster;

    public VaadinNotifier(Broadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @Override
    public void notifyGuest(String sessionId, String message) {
        // user logged in:send notification immediately to user
        Broadcaster.broadcastToGuest(sessionId, message);
    }

    @Override
    public void notifyMember(Notification notification) {
        broadcaster.broadcastToMember(notification);
    }

}
