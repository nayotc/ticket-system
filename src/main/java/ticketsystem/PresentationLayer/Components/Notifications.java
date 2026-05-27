package ticketsystem.PresentationLayer.Components;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

public final class Notifications
{

    private static final int DEFAULT_DURATION_MS = 3500;
    private static final Notification.Position DEFAULT_POSITION = Notification.Position.TOP_CENTER;

    private Notifications() {
    }

    public static void success(String message) {
        show(message, "הפעולה הושלמה בהצלחה", NotificationVariant.LUMO_SUCCESS);
    }

    public static void error(String message) {
        show(message, "הפעולה נכשלה", NotificationVariant.LUMO_ERROR);
    }

    public static void warning(String message) {
        show(message, "נא לבדוק את הפרטים שהוזנו", NotificationVariant.LUMO_WARNING);
    }

    public static void info(String message) {
        show(message, "הודעה", NotificationVariant.LUMO_CONTRAST);
    }

    private static void show(String message, String fallbackMessage, NotificationVariant variant) {
        String safeMessage = message == null || message.isBlank()
                ? fallbackMessage
                : message;

        Notification notification = Notification.show(
                safeMessage,
                DEFAULT_DURATION_MS,
                DEFAULT_POSITION
        );

        notification.addThemeVariants(variant);
    }
}