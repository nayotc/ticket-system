package ticketsystem.PresentationLayer.Errors;

import org.springframework.stereotype.Component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.ErrorHandler;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;

import ticketsystem.PresentationLayer.Constants.UiRoutes;
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

        while (cause != null) {
            String msg = cause.getMessage();
            
            if (msg != null) {
                boolean isTimeout =       
                    msg.contains("JWT") ||
                    msg.contains("Invalid session ID") ||
                    msg.contains("Invalid session token") ||
                    msg.contains("Token is missing or null") ||
                    msg.contains("Session is no longer active") ||
                    msg.contains("Invalid or expired security token");

                if (isTimeout) {
                    handleSessionTimeout();
                    return; 
                }
            }
            cause = cause.getCause();
        }

        // Fallback for other unexpected errors
        event.getThrowable().printStackTrace();
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