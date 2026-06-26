package ticketsystem.PresentationLayer.Errors;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;

import ticketsystem.PresentationLayer.Constants.UiRoutes;
import ticketsystem.PresentationLayer.Presenters.PresentationException;
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
                boolean isTimeout = PresentationException.isSessionTimeoutMessage(msg);

                if (isTimeout) {
                    boolean wasLoggedIn = UiSession.isLoggedIn();
                    UiSession.exit();
                    
                    if (wasLoggedIn) {
                        event.getUI().getPage().setLocation("/" + UiRoutes.LOGIN + "?timeout=true");
                    } else {
                        Notification.show(
                                "תוקף החיבור פג. המערכת חודשה, אנא נסו ללחוץ שוב על הפעולה.", 
                                5000, 
                                Position.TOP_CENTER
                        );
                    }
                    return 302; //Temporary Redirect: Used for Session Timeout
                }

                boolean isDbError = PresentationException.isDbDisconnectMessage(msg);

                if (isDbError) {
                    event.getUI().access(() -> {
                        Notification.show(
                            "השירות אינו זמין זמנית עקב בעיית תקשורת. נסו שוב עוד מספר רגעים.", 
                            5000, 
                            Position.TOP_CENTER
                        );
                    });

                    setText("השירות אינו זמין כרגע עקב עומס או ניתוק ממסד הנתונים. אנא נסו לרענן את העמוד בעוד מספר רגעים.");
                    return 503; //Service Unavailable: Used for Database Disconnection
                }
            }
            cause = cause.getCause();
        }

        setText("אופס! אירעה שגיאה בלתי צפויה בטעינת העמוד. נסה לרענן או לחזור לדף הבית.");
        return 500; //Internal Server Error: Used as a Generic Fallback
    }
}