package ticketsystem.ApplicationLayer;

import java.util.function.Consumer;

import ticketsystem.DomainLayer.notifications.Notification;

public interface IBrodcaster {

    Runnable registerListener(String sessionId, Consumer<Notification> notifier);
}
