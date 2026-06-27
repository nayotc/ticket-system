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
                // בדיקת ניתוק DB בכל שרשרת השגיאות
                if (PresentationException.isDbDisconnectMessage(msg)) {
                    setText("השירות אינו זמין כרגע עקב עומס או ניתוק ממסד הנתונים. אנא נסו לרענן את העמוד בעוד מספר רגעים.");
                    getStyle().set("text-align", "center");
                    return 503;
                }
                
                // בדיקת Timeout
                if (PresentationException.isSessionTimeoutMessage(msg)) {
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
            }
            cause = cause.getCause(); // צלילה לעומק השגיאה
        }

        // הודעת ברירת המחדל החדשה והברורה:
        setText("אירעה שגיאה בטעינת העמוד. אנא ודאו את חיבור הרשת ונסו לרענן.");
        return 500;
    }
}