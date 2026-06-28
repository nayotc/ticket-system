package ticketsystem.PresentationLayer.Components;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import ticketsystem.DTO.CompanyDTO;
import ticketsystem.PresentationLayer.Constants.UiRoutes;
import ticketsystem.PresentationLayer.Presenters.CompanyPresenter;
import ticketsystem.PresentationLayer.Presenters.PresentationException;
import ticketsystem.PresentationLayer.Session.UiSession;
import ticketsystem.DomainLayer.user.Permission;

public class ManagementSideNav extends Div {

    private Long companyId;
    private final Div links = new Div();
    private final ManagementSideNavPresenter presenter;

    public ManagementSideNav() {
        this(null);
    }

    public ManagementSideNav(ManagementSideNavPresenter presenter) {
        this.presenter = presenter;
        addClassName("management-side-nav");

        links.addClassName("management-side-nav-links");

        add(
                createHeader(),
                createCreateEventButton(),
                links,
                createBottomActions()
        );

        rebuildLinks();
    }

    public void setCompanyId(String companyId) {
        if (companyId == null || companyId.isBlank()) {
            return;
        }

        try {
            this.companyId = Long.parseLong(companyId);
            rebuildLinks();
        } catch (NumberFormatException e) {
            showError("מזהה החברה אינו תקין");
        }
    }

    private Div createHeader() {
        Div header = new Div();
        header.addClassName("management-side-nav-header");

        Span brand = new Span("TixNow");
        brand.addClassName("brand-logo");
        brand.addClickListener(event -> UI.getCurrent().navigate(UiRoutes.HOME));

        Span subtitle = new Span("Manage your events");
        subtitle.addClassName("management-side-nav-subtitle");

        header.add(brand, subtitle);
        return header;
    }

    private Button createCreateEventButton() {
        Button button = new Button("אירוע חדש", VaadinIcon.PLUS.create());
        button.addClassName("management-create-button");
        button.addClickListener(event -> navigate(UiRoutes.CREATE_EVENT));
        return button;
    }

    private Div createBottomActions() {
        Div bottom = new Div();
        bottom.addClassName("management-side-nav-bottom");

        Button button = new Button("יצירת חברה", VaadinIcon.PLUS.create());
        button.addClassName("management-create-company-button");
        button.addClickListener(event -> openCreateCompanyDialog());

        bottom.add(button);
        return bottom;
    }

    private void rebuildLinks() {
        links.removeAll();

        links.add(navButton("ניהול חברה", UiRoutes.COMPANY_MANAGEMENT));

        if (canViewPoliciesEditor()) {
            links.add(navButton("עורך מדיניות", UiRoutes.POLICIES_EDITOR));
        }

        if (hasPermission(Permission.GENERATE_SALES_REPORT)) {
            links.add(navButton("דוח מכירות", UiRoutes.SALES_REPORT));
        }

        if (hasPermission(Permission.VIEW_PURCHASE_HISTORY)) {
            links.add(navButton("היסטוריית רכישות", UiRoutes.PURCHASE_HISTORY));
        }

        links.add(navButton("עץ תפקידים והרשאות", UiRoutes.ROLES_AND_PERMISSIONS_TREE));
    }

    private boolean canViewPoliciesEditor() {
        return hasPermission(Permission.SET_PURCHASING_POLICY)
                || hasPermission(Permission.SET_DISCOUNT_POLICY);
    }

    private boolean hasPermission(Permission permission) {
        if (presenter == null || !UiSession.isLoggedIn() || companyId == null) {
            return false;
        }

        try {
            return presenter.hasPermission(
                    UiSession.getMemberToken(),
                    companyId,
                    permission
            );
        } catch (PresentationException e) {
            // 1. בודקים מפורשות אם זה ניתוק
            if (PresentationException.isSessionTimeoutMessage(e.getMessage())) {
                UiSession.handleTimeoutRedirect(); // מנתבים החוצה
            }
            // 2. מחזירים false גם במקרה של ניתוק וגם במקרה של חוסר הרשאה רגיל
            return false; 
            
        } catch (Exception e) {
            // כל שגיאה טכנית אחרת (כמו שרת למטה) תחזיר false
            return false;
        }
    }

    private Button navButton(String text, String routeTemplate) {
        Button button = new Button(text);
        button.addClassName("management-nav-item");
        button.addClickListener(event -> navigate(routeTemplate));
        return button;
    }

    private void navigate(String routeTemplate) {
        if (companyId == null) {
            showError("לא ניתן לפתוח את העמוד כי מזהה החברה חסר");
            return;
        }

        String route = routeTemplate.replace(":companyId", String.valueOf(companyId));
        UI.getCurrent().navigate(route);
    }

    private void openCreateCompanyDialog() {
        Dialog dialog = new Dialog();
        dialog.addClassName("create-company-dialog");
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(true);

        Div card = new Div();
        card.addClassName("create-company-dialog-card");

        H3 title = new H3("יצירת חברת הפקה");
        title.addClassName("create-company-dialog-title");

        Paragraph description = new Paragraph("הזן שם חברה. לאחר יצירה תועבר ישירות לעמוד ניהול החברה החדשה.");
        description.addClassName("create-company-dialog-description");

        TextField companyName = new TextField("שם החברה");
        companyName.setPlaceholder("לדוגמה: הפקות במה פתוחה");
        companyName.setWidthFull();
        companyName.addClassName("create-company-dialog-field");

        Button cancel = new Button("ביטול", event -> dialog.close());
        cancel.addClassName("company-secondary-button");

        Button create = new Button("חברה חדשה", VaadinIcon.PLUS.create());
        create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        create.addClassName("create-company-dialog-submit");
        create.addClickListener(event -> createProductionCompany(companyName, dialog));

        HorizontalLayout actions = new HorizontalLayout(cancel, create);
        actions.addClassName("create-company-dialog-actions");

        card.add(title, description, companyName, actions);
        dialog.add(card);
        dialog.open();
    }

    private void createProductionCompany(TextField companyNameField, Dialog dialog) {
        String companyName = companyNameField.getValue() == null ? "" : companyNameField.getValue().trim();

        if (companyName.isBlank()) {
            showWarning("שם החברה הוא שדה חובה");
            return;
        }

        if (!UiSession.isLoggedIn()) {
            dialog.close();
            showWarning("כדי ליצור חברת הפקה יש להתחבר כמנוי");
            UI.getCurrent().navigate(UiRoutes.LOGIN);
            return;
        }

        if (presenter == null) {
            showWarning("יצירת חברה מוכנה לחיבור Presenter. חבר כאן את CompanyService.createProductionCompany.");
            return;
        }

        try {
            CompanyDTO company = presenter.createProductionCompany(UiSession.getMemberToken(), companyName);
            if (company == null) {
                showError("יצירת החברה נכשלה");
                return;
            }

            dialog.close();
            UI.getCurrent().navigate(routeForCompany(company.getId()));
            
        } catch (PresentationException e) {
            if (PresentationException.isSessionTimeoutMessage(e.getMessage())) {
                dialog.close();
                UiSession.handleTimeoutRedirect();
                return; 
            }
            showError(e.getMessage());
            
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    private String routeForCompany(long newCompanyId) {
        return UiRoutes.COMPANY_MANAGEMENT.replace(":companyId", String.valueOf(newCompanyId));
    }

    private void showWarning(String message) {
        Notification notification = Notification.show(message, 3500, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_CONTRAST);
    }

    private void showError(String message) {
        Notification notification = Notification.show(message == null || message.isBlank() ? "הפעולה נכשלה" : message, 3500, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    public interface ManagementSideNavPresenter {
        CompanyDTO createProductionCompany(String sessionToken, String companyName) throws Exception;

        boolean hasPermission(String sessionToken, long companyId, Permission permission);
    }
}
