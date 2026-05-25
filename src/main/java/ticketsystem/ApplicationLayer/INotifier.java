package ticketsystem.ApplicationLayer;

import ticketsystem.DomainLayer.notifications.Notification;

public interface INotifier {

    void notifyGuest(String sessionId, String message);

    void notifyMember(Notification notification);
}
