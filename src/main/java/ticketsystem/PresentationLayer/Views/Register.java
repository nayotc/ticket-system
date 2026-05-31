package ticketsystem.PresentationLayer.Views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import ticketsystem.PresentationLayer.Presenters.AuthPresenter;
import ticketsystem.PresentationLayer.Presenters.PresentationException;
import ticketsystem.PresentationLayer.Session.UiSession;
import ticketsystem.PresentationLayer.Components.ActionBar;
import ticketsystem.PresentationLayer.Components.FormCard;
import ticketsystem.PresentationLayer.Components.PageContainer;
import ticketsystem.PresentationLayer.Constants.UiRoutes;
import ticketsystem.PresentationLayer.Layouts.AuthLayout;
import ticketsystem.PresentationLayer.Components.Notifications;

@PageTitle("TixNow | Registration")
@Route(value = UiRoutes.REGISTRATION, layout = AuthLayout.class)
public class Register extends PageContainer {

    private final TextField fullName = createTextField("שם מלא", "לדוגמה: ישראל ישראלי", VaadinIcon.USER);
    private final EmailField email = createEmailField();
    private final TextField phone = createTextField("טלפון", "לדוגמה: 050-0000000", VaadinIcon.PHONE);
    private final DatePicker birthDate = createBirthDateField();
    private final PasswordField password = createPasswordField("סיסמה");
    private final PasswordField confirmPassword = createPasswordField("אימות סיסמה");
    private final Checkbox termsAccepted = new Checkbox();
    private final AuthPresenter authPresenter;

    @Autowired
    public Register(AuthPresenter authPresenter) {
        this.authPresenter = authPresenter;

        addClassName("auth-page");
        setSpacing(false);

        FormCard card = new FormCard(
                null,
                null,
                createBrandBlock(),
                createForm(),
                createLoginLink()
        );

        card.addClassName("auth-card");

        add(card);
    }

    private Div createBrandBlock() {
        Div block = new Div();
        block.addClassName("auth-brand-block");

        H1 title = new H1("TixNow");
        title.addClassName("auth-brand-title");

        Paragraph subtitle = new Paragraph("צרו חשבון כדי להמשיך לרכישת כרטיסים וניהול אירועים");
        subtitle.addClassName("auth-brand-subtitle");

        block.add(title, subtitle);
        return block;
    }

    private VerticalLayout createForm() {
        VerticalLayout form = new VerticalLayout();
        form.addClassName("auth-form");
        form.setPadding(false);
        form.setSpacing(false);
        form.setWidthFull();

        Button registerButton = new Button("הרשמה", VaadinIcon.ARROW_LEFT.create());
        registerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        registerButton.addClassName("auth-submit-button");
        registerButton.setIconAfterText(true);
        registerButton.setWidthFull();
        registerButton.addClickListener(event -> handleRegister());

        form.add(
                fullName,
                email,
                phone,
                birthDate,
                password,
                confirmPassword,
                createTermsRow(),
                registerButton
        );

        return form;
    }

    private HorizontalLayout createTermsRow() {
        HorizontalLayout row = new HorizontalLayout();
        row.addClassName("auth-terms-row");
        row.setPadding(false);
        row.setSpacing(false);
        row.setWidthFull();
        row.setAlignItems(Alignment.CENTER);

        Anchor terms = new Anchor("#", "תנאי השימוש");
        terms.addClassName("auth-link");

        Anchor privacy = new Anchor("#", "מדיניות הפרטיות");
        privacy.addClassName("auth-link");

        Span text = new Span();
        text.addClassName("auth-terms-text");
        text.add(new Span("אני מאשר/ת את "), terms, new Span(" ואת "), privacy);

        row.add(termsAccepted, text);
        return row;
    }


    private Paragraph createLoginLink() {
        Paragraph paragraph = new Paragraph();
        paragraph.addClassName("auth-switch-text");

        Anchor login = new Anchor("/" + UiRoutes.LOGIN, "התחברות");
        login.addClassName("auth-link");

        paragraph.add(new Span("כבר יש לך חשבון? "), login);
        return paragraph;
    }

    private TextField createTextField(String label, String placeholder, VaadinIcon icon) {
        TextField field = new TextField(label);
        field.setPlaceholder(placeholder);
        field.setPrefixComponent(icon.create());
        field.setRequiredIndicatorVisible(true);
        field.setWidthFull();
        field.addClassName("auth-field");
        return field;
    }

    private EmailField createEmailField() {
        EmailField field = new EmailField("אימייל");
        field.setPlaceholder("you@example.com");
        field.setPrefixComponent(VaadinIcon.ENVELOPE.create());
        field.setRequiredIndicatorVisible(true);
        field.setErrorMessage("יש להזין כתובת אימייל תקינה");
        field.setWidthFull();
        field.addClassName("auth-field");
        return field;
    }
    private DatePicker createBirthDateField() {
        DatePicker field = new DatePicker("תאריך לידה");
        field.setPlaceholder("בחר תאריך לידה");
        field.setRequiredIndicatorVisible(true);
        field.setMax(LocalDate.now());
        field.setWidthFull();
        field.addClassName("auth-field");
        field.setErrorMessage("יש להזין תאריך לידה תקין");
        return field;
    }

    private PasswordField createPasswordField(String label) {
        PasswordField field = new PasswordField(label);
        field.setPlaceholder("••••••••");
        field.setPrefixComponent(VaadinIcon.LOCK.create());
        field.setRequiredIndicatorVisible(true);
        field.setRevealButtonVisible(false);
        field.setWidthFull();
        field.addClassName("auth-field");
        return field;
    }

    private void handleRegister() {
        if (isBlank(fullName.getValue()) || isBlank(email.getValue()) || isBlank(phone.getValue())
                || isBlank(password.getValue()) || isBlank(confirmPassword.getValue())) {
            showError("יש למלא את כל שדות החובה");
            return;
        }
        if (isBlank(fullName.getValue()) || isBlank(email.getValue()) || isBlank(phone.getValue())
            || birthDate.getValue() == null
            || isBlank(password.getValue()) || isBlank(confirmPassword.getValue())) {
        showError("יש למלא את כל שדות החובה");
        return;
        }
        if (birthDate.getValue().isAfter(LocalDate.now())) {
            showError("תאריך לידה לא יכול להיות בעתיד");
            return;
        }

        if (!password.getValue().equals(confirmPassword.getValue())) {
            showError("הסיסמאות אינן תואמות");
            return;
        }

        if (!termsAccepted.getValue()) {
            showError("יש לאשר את תנאי השימוש ומדיניות הפרטיות");
            return;
        }

        try {
            if (UiSession.getGuestToken() == null) {
                authPresenter.visitSystem();
            }

            authPresenter.signUp(
                    email.getValue(),
                    password.getValue(),
                    fullName.getValue(),
                    phone.getValue(),birthDate.getValue()
            );

            Notifications.success("ההרשמה נקלטה בהצלחה");

            UI.getCurrent().navigate(UiRoutes.LOGIN);

        } catch (PresentationException e) {
            showError(e.getMessage());
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void showError(String message) {
        Notifications.error(message);
    }
}