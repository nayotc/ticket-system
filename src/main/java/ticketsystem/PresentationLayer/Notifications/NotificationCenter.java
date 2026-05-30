package ticketsystem.PresentationLayer.Notifications;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.stereotype.Component;
import ticketsystem.ApplicationLayer.IBrodcaster;
import ticketsystem.ApplicationLayer.NotificationService;
import ticketsystem.DomainLayer.notifications.Notification;
import ticketsystem.PresentationLayer.Components.MessagePopup;

@Component
public class NotificationCenter {

    private static final String PUSH_UNREGISTER_KEY = "pushUnregister";
    private static final String PUSH_TARGET_KEY = "pushTargetId";

    private final IBrodcaster broadcaster;
    private final NotificationService notificationService;

    public NotificationCenter(IBrodcaster broadcaster,
                                NotificationService notificationService) {
        this.broadcaster = broadcaster;
        this.notificationService = notificationService;
    }

    public void connect(UI ui, String targetId) {
        if (ui == null || targetId == null || targetId.isBlank()) {
            return;
        }

        VaadinSession session = VaadinSession.getCurrent();

        if (session == null) {
            return;
        }

        Object currentTarget = session.getAttribute(PUSH_TARGET_KEY);
        Object currentUnregister = session.getAttribute(PUSH_UNREGISTER_KEY);

        if (targetId.equals(currentTarget) && currentUnregister instanceof Runnable) {
            return;
        }

        disconnect();

        Runnable unregister = broadcaster.registerListener(targetId, notification -> {
            ui.access(() -> {
                show(notification);

                if (notification.getId() != null) {
                    notificationService.markAsDelivered(notification.getId());
                }
            });
        });

        session.setAttribute(PUSH_UNREGISTER_KEY, unregister);
        session.setAttribute(PUSH_TARGET_KEY, targetId);
    }

    public void disconnect() {
        VaadinSession session = VaadinSession.getCurrent();

        if (session == null) {
            return;
        }

        Object unregister = session.getAttribute(PUSH_UNREGISTER_KEY);

        if (unregister instanceof Runnable runnable) {
            runnable.run();
        }

        session.setAttribute(PUSH_UNREGISTER_KEY, null);
        session.setAttribute(PUSH_TARGET_KEY, null);
    }

    public void showPending(UI ui, String targetId) {
        if (ui == null || targetId == null || targetId.isBlank()) {
            return;
        }

        notificationService.getPendingNotifications(targetId).forEach(notification -> {
            ui.access(() -> {
                show(notification);

                if (notification.getId() != null) {
                    notificationService.markAsDelivered(notification.getId());
                }
            });
        });
    }

    private void show(Notification notification) {
        String message = resolveMessage(notification);
        MessagePopup.showNotification(message);
    }

    private String resolveMessage(Notification notification) {
        if (notification == null) {
            return "התקבלה התראה חדשה";
        }

        String message = notification.getMessage();

        if (message == null || message.isBlank()) {
            return "התקבלה התראה חדשה";
        }

        return message;
    }
}