package ticketsystem.PresentationLayer.Views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import ticketsystem.PresentationLayer.Components.FormCard;
import ticketsystem.PresentationLayer.Components.PageContainer;
import ticketsystem.PresentationLayer.Constants.UiRoutes;
import ticketsystem.PresentationLayer.Layouts.AuthLayout;
import ticketsystem.PresentationLayer.Session.UiSession;
import ticketsystem.PresentationLayer.Presenters.AuthPresenter;
import ticketsystem.PresentationLayer.Presenters.PresentationException;
import ticketsystem.PresentationLayer.Components.Notifications;
import ticketsystem.PresentationLayer.Session.UiVisitCoordinator;

@PageTitle("TixNow | התחברות")
@Route(value = UiRoutes.LOGIN, layout = AuthLayout.class)
public class Login extends PageContainer implements BeforeEnterObserver {

    private final EmailField email = createEmailField();
    private final PasswordField password = createPasswordField();
    private final UiVisitCoordinator uiVisitCoordinator;

    @Autowired
    public Login(UiVisitCoordinator uiVisitCoordinator) {
        this.uiVisitCoordinator = uiVisitCoordinator;

        addClassName("auth-page");
        setSpacing(false);

        FormCard card = new FormCard(
                null,
                null,
                createBrandBlock(),
                createForm(),
                createRegisterLink()
        );

        card.addClassName("auth-card");

        add(card);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // כאן אנחנו תופסים את הניתובים שבאו בעקבות ניתוק ומקפיצים את ההודעה המסודרת
        if (event.getLocation().getQueryParameters().getParameters().containsKey("timeout")) {
            Notifications.error("זמן החיבור פג. אנא התחבר מחדש למערכת.");
        }
    }

    private Div createBrandBlock() {
        Div block = new Div();
        block.addClassName("auth-brand-block");

        H1 brand = new H1("TixNow");
        brand.addClassName("auth-brand-title");

        H2 title = new H2("ברוכים הבאים");
        title.addClassName("auth-view-title");

        Paragraph subtitle = new Paragraph("התחבר כדי לנהל את האירועים והכרטיסים שלך");
        subtitle.addClassName("auth-brand-subtitle");

        block.add(brand, title, subtitle);
        return block;
    }

    private VerticalLayout createForm() {
        VerticalLayout form = new VerticalLayout();
        form.addClassName("auth-form");
        form.setPadding(false);
        form.setSpacing(false);
        form.setWidthFull();

        Button loginButton = new Button("התחברות", VaadinIcon.ARROW_LEFT.create());
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginButton.addClassName("auth-submit-button");
        loginButton.setIconAfterText(true);
        loginButton.setWidthFull();
        loginButton.addClickListener(event -> handleLogin());

        form.add(
                email,
                createPasswordRow(),
                password,
                loginButton
        );

        return form;
    }

    private HorizontalLayout createPasswordRow() {
        HorizontalLayout row = new HorizontalLayout();
        row.addClassName("auth-field-header-row");
        row.setPadding(false);
        row.setSpacing(false);
        row.setWidthFull();
        row.setAlignItems(Alignment.CENTER);

        Span label = new Span("סיסמה");
        label.addClassName("auth-field-label");

        Anchor forgotPassword = new Anchor("#", "שכחתי סיסמה?");
        forgotPassword.addClassName("auth-link");

        row.add(label, forgotPassword);
        row.expand(label);

        return row;
    }

    private Paragraph createRegisterLink() {
        Paragraph paragraph = new Paragraph();
        paragraph.addClassName("auth-switch-text");

        Anchor register = new Anchor("/" + UiRoutes.REGISTRATION, "הירשם עכשיו");
        register.addClassName("auth-link");

        paragraph.add(new Span("אין לך חשבון עדיין? "), register);
        return paragraph;
    }

    private EmailField createEmailField() {
        EmailField field = new EmailField("דואר אלקטרוני");
        field.setPlaceholder("name@example.com");
        field.setPrefixComponent(VaadinIcon.ENVELOPE.create());
        field.setRequiredIndicatorVisible(true);
        field.setErrorMessage("יש להזין כתובת אימייל תקינה");
        field.setWidthFull();
        field.addClassName("auth-field");
        return field;
    }

    private PasswordField createPasswordField() {
        PasswordField field = new PasswordField();
        field.setPlaceholder("••••••••");
        field.setPrefixComponent(VaadinIcon.LOCK.create());
        field.setRequiredIndicatorVisible(true);
        field.setRevealButtonVisible(true);
        field.setWidthFull();
        field.addClassName("auth-field");
        return field;
    }

    private void handleLogin() {

        if (isBlank(email.getValue()) || isBlank(password.getValue())) {
            showError("יש למלא אימייל וסיסמה");
            return;
        }

        try {

            uiVisitCoordinator.login(
                    UI.getCurrent(),
                    email.getValue(),
                    password.getValue()
            );

            Notifications.success("התחברת בהצלחה");
            UI.getCurrent().navigate(UiRoutes.HOME);

        } catch (PresentationException e) {
            if (e.isSessionTimeout()) {
                UiSession.handleTimeoutRedirect();
                return;
            }
            showError(e.getMessage());
            
        } catch (Exception e) {
            showError(e.getMessage() == null ? "שגיאה בהתחברות" : e.getMessage());
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void showError(String message) {
        Notifications.error(message);
    }
}