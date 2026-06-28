package ticketsystem.PresentationLayer.Errors;

import org.springframework.stereotype.Component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.ErrorHandler;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;

import ticketsystem.PresentationLayer.Constants.UiRoutes;
import ticketsystem.PresentationLayer.Presenters.PresentationException;
import ticketsystem.PresentationLayer.Session.UiSession;
import ticketsystem.ApplicationLayer.ISystemLogger;
import ticketsystem.ApplicationLayer.ISystemLogger.LogLevel;

/**
 * Global error handler that catches unhandled UI exceptions across the application.
 * It intercepts session timeout errors (e.g., expired JWT tokens), both in their
 * raw backend format and translated presenter formats, and gracefully redirects 
 * the user to the login page with a standardized notification.
 */
@Component
public class CustomErrorHandler implements ErrorHandler {

    private final ISystemLogger logger;

    public CustomErrorHandler(ISystemLogger logger) {
        this.logger = logger;
    }

    @Override
    public void error(ErrorEvent event) {
        Throwable cause = event.getThrowable();

        // לולאה שעוברת על כל שרשרת השגיאות כדי למצוא את המקור
        while (cause != null) {
            String msg = cause.getMessage();
            
            if (msg != null) {
                // 1. בדיקת Timeout
                if (PresentationException.isSessionTimeoutMessage(msg)) {
                    handleSessionTimeout();
                    return; 
                }

                // 2. בדיקת ניתוק DB - כאן השינוי המרכזי
                if (PresentationException.isDbDisconnectMessage(msg)) {
                    UI ui = UI.getCurrent();
                    if (ui != null) {
                        ui.access(() -> {
                            // שימוש בהודעה המוגדרת ב-PresentationException
                            Notification.show(
                                PresentationException.DB_DISCONNECT_HEBREW_MSG, 
                                5000, 
                                Position.TOP_CENTER
                            );
                        });
                    }
                    return;
                }
            }
            cause = cause.getCause(); // חשוב: צלילה לשגיאת המקור
        }

        // Fallback: שגיאה לא מזוהה
        logger.logEvent("Unhandled UI exception: " + event.getThrowable().getMessage(), LogLevel.DEBUG);
        UI ui = UI.getCurrent();
        if (ui != null) {
            ui.access(() -> {
                Notification.show(
                    "אירעה שגיאה בביצוע הפעולה. במידה והבעיה נמשכת, ודאו חיבור תקין לרשת.", 
                    5000, 
                    Position.TOP_CENTER
                );
            });
        }
    }

    private void showNotification(String text) {
        UI ui = UI.getCurrent();
        if (ui != null) {
            ui.access(() -> Notification.show(text, 5000, Position.TOP_CENTER));
        }
    }

    private void handleSessionTimeout() {
        UI ui = UI.getCurrent();
        if (ui != null) {
            ui.access(() -> {
                boolean wasLoggedIn = UiSession.isLoggedIn(); // The system checks: is this a logged-in user or a guest?
                UiSession.exit(); // Clear the invalid session
                
                if (wasLoggedIn) {
                    // If it's a logged-in user: redirect to Login with the parameter that triggers the red message
                    ui.getPage().setLocation("/" + UiRoutes.LOGIN + "?timeout=true");
                } else {
                    // If it's a עuest user: no reload (to prevent loops) and no redirect (to prevent data loss on screens like registration)!
                    Notification.show(
                            "תוקף החיבור פג. המערכת חודשה, אנא נסו ללחוץ שוב על הפעולה.", 
                            5000, 
                            Position.TOP_CENTER
                    );
                }
            });
        } else {
            logger.logEvent(
                    "Session timeout detected globally, but UI.getCurrent() is null. This likely occurred in a background thread.",
                    LogLevel.WARN
            );
        }
    }
}