package ticketsystem.PresentationLayer.Views.Preview;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import ticketsystem.PresentationLayer.Components.ActionBar;
import ticketsystem.PresentationLayer.Components.MessagePopup;
import ticketsystem.PresentationLayer.Components.PageContainer;
import ticketsystem.PresentationLayer.Components.ViewHeader;
import ticketsystem.PresentationLayer.Layouts.MainLayout;

@PageTitle("TixNow | הודעות מערכת")
@Route(value = "ui-preview/messages", layout = MainLayout.class)
public class MessagePopupDemoView extends PageContainer {

    public MessagePopupDemoView() {
        addClassName("preview-page");

        Button success = new Button("הצג הודעת הצלחה");
        success.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        success.addClickListener(event -> MessagePopup.showSuccess("האירוע עודכן ונשמר במערכת"));

        Button error = new Button("הצג הודעת שגיאה");
        error.addClickListener(event -> MessagePopup.showError("לא הצלחנו לשמור את השינויים. אנא בדוק את החיבור לאינטרנט ונסה שוב."));

        Button retryError = new Button("הצג שגיאה עם ניסיון חוזר");
        retryError.addClickListener(event -> MessagePopup.showError(
                "לא הצלחנו לבצע את הפעולה. אפשר לנסות שוב.",
                () -> MessagePopup.showSuccess("הבקשה נשלחה שוב")
        ));

        add(
                new ViewHeader(
                        "תצוגת הודעות",
                        "Popup אחיד לשגיאות ולהצלחות מעל המסך הפעיל"
                ),
                new ActionBar(success, error, retryError)
        );
    }
}
