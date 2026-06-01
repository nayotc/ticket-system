package ticketsystem.PresentationLayer.Views;

import org.springframework.beans.factory.annotation.Autowired;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import ticketsystem.DTO.CompanyDTO;
import ticketsystem.PresentationLayer.Components.AppCard;
import ticketsystem.PresentationLayer.Components.Notifications;
import ticketsystem.PresentationLayer.Constants.UiRoutes;
import ticketsystem.PresentationLayer.Layouts.MainLayout;
import ticketsystem.PresentationLayer.Presenters.CompanyPresenter;
import ticketsystem.PresentationLayer.Presenters.PresentationException;
import ticketsystem.PresentationLayer.Session.UiSession;

@PageTitle("TixNow | My productions")
@Route(value = UiRoutes.CREATE_PRODUCTION_COMPANY, layout = MainLayout.class)
public class CreateProductionCompany extends Div {

    private final CompanyPresenter presenter;
    private final TextField companyName = new TextField("שם החברה");
    private final Button revealFormButton = new Button("חברה חדשה", VaadinIcon.PLUS.create());
    private final Div form = new Div();

    public CreateProductionCompany() {
        this(null);
    }

    @Autowired
    public CreateProductionCompany(CompanyPresenter presenter) {
        this.presenter = presenter;
        addClassName("create-production-company-page");
        getElement().setAttribute("dir", "rtl");

        configureForm();
        add(createBackground(), createCard());
    }

    private Div createBackground() {
        Div background = new Div();
        background.addClassName("create-production-company-background");

        Div topGlow = new Div();
        topGlow.addClassNames(
                "create-production-company-glow",
                "create-production-company-glow-primary"
        );

        Div bottomGlow = new Div();
        bottomGlow.addClassNames(
                "create-production-company-glow",
                "create-production-company-glow-secondary"
        );

        background.add(topGlow, bottomGlow);
        return background;
    }

    private AppCard createCard() {
        AppCard card = new AppCard();
        card.addClassName("create-production-company-card");

        Div iconCircle = new Div();
        iconCircle.addClassName("create-production-company-icon-circle");
        iconCircle.add(VaadinIcon.BUILDING.create());

        H1 title = new H1("הפקות שלי");
        title.addClassName("create-production-company-title");

        Paragraph description = new Paragraph("אינך משויך לחברת הפקה. צור חברה חדשה כדי להתחיל לנהל אירועים, מדיניות ומכירות.");
        description.addClassName("create-production-company-description");

        revealFormButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        revealFormButton.addClassName("create-production-company-reveal-button");
        revealFormButton.addClickListener(event -> showForm());

        card.add(iconCircle, title, description, revealFormButton, form);
        return card;
    }

    private void configureForm() {
        form.addClassName("create-production-company-form");
        form.setVisible(false);

        companyName.setPlaceholder("הכנס שם חברה...");
        companyName.setWidthFull();
        companyName.addClassName("create-production-company-field");

        Button create = new Button("צור", VaadinIcon.PLUS.create());
        create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        create.addClassName("create-production-company-submit-button");
        create.addClickListener(event -> createCompany(create));

        form.add(companyName, create);
    }

    private void showForm() {
        revealFormButton.setVisible(false);
        form.setVisible(true);
        companyName.focus();
    }

    private void createCompany(Button createButton) {
        String name = companyName.getValue() == null ? "" : companyName.getValue().trim();

        if (name.isBlank()) {
            showWarning("שם החברה הוא שדה חובה");
            return;
        }

        if (!UiSession.isLoggedIn()) {
            showWarning("כדי ליצור חברת הפקה יש להתחבר כמנוי");
            UI.getCurrent().navigate(UiRoutes.LOGIN);
            return;
        }

        createButton.setEnabled(false);

        try {
            CompanyDTO company = presenter.createProductionCompany(UiSession.getMemberToken(), name);
            if (company == null) {
                showError("יצירת החברה נכשלה");
                createButton.setEnabled(true);
                return;
            }
            Notifications.success("חברת ההפקה נוצרה בהצלחה");
            UI.getCurrent().navigate(UiRoutes.COMPANY_MANAGEMENT.replace(":companyId", String.valueOf(company.getId())));
        } catch (PresentationException e) {
            createButton.setEnabled(true);
            showError(e.getMessage());
        }
    }

    private void showWarning(String message) {
        Notifications.warning(message);
    }

    private void showError(String message) {
        Notifications.error(message);
    }

}
