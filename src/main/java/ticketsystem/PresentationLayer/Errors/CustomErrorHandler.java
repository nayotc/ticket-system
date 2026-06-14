package ticketsystem.PresentationLayer.Errors;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.ErrorHandler;
import ticketsystem.PresentationLayer.Components.Notifications;
import ticketsystem.PresentationLayer.Constants.UiRoutes;
import ticketsystem.PresentationLayer.Session.UiSession;

/**
 * Global error handler that catches unhandled UI exceptions across the application.
 * It intercepts session timeout errors (e.g., expired JWT tokens), both in their
 * raw backend format and translated presenter formats, and gracefully redirects 
 * the user to the login page with a standardized notification.
 */
public class CustomErrorHandler implements ErrorHandler {

    @Override
    public void error(ErrorEvent event) {
        Throwable cause = event.getThrowable();

        while (cause != null) {
            String msg = cause.getMessage();
            
            if (msg != null) {
                boolean isTimeout = msg.contains("פג תוקף") || 
                                                    msg.contains("לא תקין") || 
                                                    msg.contains("לא פעיל") ||
                                                    msg.contains("Invalid or expired") ||
                                                    msg.contains("JWT expired");

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
                boolean wasLoggedIn = UiSession.isLoggedIn();
                UiSession.exit();
                if (wasLoggedIn) {
                    ui.getPage().setLocation("/" + UiRoutes.LOGIN + "?timeout=true");
                } else {
                    ui.getPage().reload();
                }
            });
        }
    }
}