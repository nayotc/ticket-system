package ticketsystem.PresentationLayer.Views.Preview;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import ticketsystem.PresentationLayer.Components.ActionBar;
import ticketsystem.PresentationLayer.Components.FormCard;
import ticketsystem.PresentationLayer.Components.PageContainer;
import ticketsystem.PresentationLayer.Constants.UiRoutes;
import ticketsystem.PresentationLayer.Layouts.AuthLayout;

@PageTitle("Auth Preview")
@Route(value = UiRoutes.UI_PREVIEW_AUTH, layout = AuthLayout.class)
public class AuthPreview extends PageContainer {

    public AuthPreview() {
        addClassName("auth-preview-page");

        TextField email = new TextField("אימייל");
        email.setPlaceholder("name@example.com");

        PasswordField password = new PasswordField("סיסמה");
        password.setPlaceholder("הכנס סיסמה");

        Button login = new Button("התחברות");
        login.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button register = new Button("הרשמה");

        add(
                new FormCard(
                        "התחברות למערכת",
                        "תצוגה מקדימה למסכי Login ו־Registration.",
                        email,
                        password,
                        new ActionBar(login, register)
                )
        );
    }
}