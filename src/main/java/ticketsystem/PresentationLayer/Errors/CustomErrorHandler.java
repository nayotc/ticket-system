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
                boolean isTimeout = msg != null && (         
                    msg.contains("JWT") ||
                    msg.contains("expired") ||
                    msg.contains("Invalid") ||
                    msg.contains("Invalid session ID") ||
                    msg.contains("Token is missing or null") ||
                    msg.contains("Session is no longer active") ||
                    msg.contains("Invalid or expired security token")
                );

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
                boolean wasLoggedIn = UiSession.isLoggedIn(); // המערכת בודקת: האם זה מנוי או אורח?
                UiSession.exit(); // מנקים את הסשן הפגום
                if (wasLoggedIn) {
                    // אם זה מנוי: זורקים אותו ל-Login עם הפרמטר שמדליק את ההודעה האדומה
                    ui.getPage().setLocation("/" + UiRoutes.LOGIN + "?timeout=true");
                } else {
                    // אם זה אורח: עושים רענון שקט לאותו העמוד!
                    ui.getPage().reload();
                }
            });
        }
    }
}