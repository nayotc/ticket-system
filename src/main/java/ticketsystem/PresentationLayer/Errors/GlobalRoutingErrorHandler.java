package ticketsystem.PresentationLayer.Errors;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import ticketsystem.PresentationLayer.Components.Notifications;
import ticketsystem.PresentationLayer.Constants.UiRoutes;
import ticketsystem.PresentationLayer.Session.UiSession;

/**
 * Global routing error handler that catches exceptions thrown during navigation
 * (e.g., inside beforeEnter methods). It intercepts session timeouts before Vaadin
 * can display the default "Internal Server Error" white screen, clears the session,
 * and securely reroutes the user to the login page.
 */
public class GlobalRoutingErrorHandler extends Div implements HasErrorParameter<Exception> {

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<Exception> parameter) {
        Throwable cause = parameter.getCaughtException();

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
                    boolean wasLoggedIn = UiSession.isLoggedIn();
                    UiSession.exit();
                    
                    if (wasLoggedIn) {
                        event.getUI().getPage().setLocation("/" + UiRoutes.LOGIN + "?timeout=true");
                    } else {
                        event.getUI().getPage().reload();
                    }
                    return 302; // עוצר את בניית העמוד ומונע את ציור ה"אופס"
                }
            }
            cause = cause.getCause();
        }

        // ברירת מחדל לשאר הקריסות הלא צפויות (רק לשגיאות אמיתיות)
        setText("אופס! אירעה שגיאה בלתי צפויה בטעינת העמוד. נסה לרענן או לחזור לדף הבית.");
        return 500; 
    }
}