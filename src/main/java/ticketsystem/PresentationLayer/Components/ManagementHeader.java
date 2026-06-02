package ticketsystem.PresentationLayer.Components;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import ticketsystem.PresentationLayer.Constants.UiRoutes;
import ticketsystem.PresentationLayer.Session.UiSession;
import ticketsystem.PresentationLayer.Session.UiVisitCoordinator;

public class ManagementHeader extends Header {

    private final ManagementHeaderPresenter presenter;
    private final UiVisitCoordinator visitCoordinator;

    public ManagementHeader() {
        this(null, null);
    }

    public ManagementHeader(ManagementHeaderPresenter presenter) {
        this(presenter, null);
    }

    public ManagementHeader(ManagementHeaderPresenter presenter,
                            UiVisitCoordinator visitCoordinator) {
        this.presenter = presenter;
        this.visitCoordinator = visitCoordinator;

        addClassName("management-header");

        Span title = new Span("ניהול הפקות");
        title.addClassName("management-header-title");

        HorizontalLayout actions = new HorizontalLayout();
        actions.addClassName("management-header-actions");
        actions.add(createAccountButton(), createLogoutButton());

        add(title, actions);
    }

    private Button createAccountButton() {
        Button button = new Button(VaadinIcon.USER.create());
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        button.addClassName("header-icon-button");
        button.getElement().setAttribute("aria-label", "אזור אישי");
        button.addClickListener(event -> UI.getCurrent().navigate(UiRoutes.MY_ACCOUNT));
        return button;
    }

    private Button createLogoutButton() {
        Button button = new Button("התנתקות");
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        button.addClassName("management-header-logout-button");

        button.addClickListener(event -> {
            UI ui = UI.getCurrent();

            if (ui == null) {
                return;
            }

            if (!UiSession.isLoggedIn()) {
                ui.navigate(UiRoutes.LOGIN);
                return;
            }

            try {
                if (visitCoordinator != null) {
                    visitCoordinator.logoutToGuest(ui);
                } else {
                    fallbackLogout();
                }
            } catch (Exception exception) {
                UiSession.logout();
            }

            ui.navigate(UiRoutes.HOME);

            /*
             * Do not call:
             * ui.getPage().reload();
             *
             * It can reload /companies/{id}/manage before the client finishes navigation.
             */
        });

        return button;
    }

    private void fallbackLogout() throws Exception {
        String memberToken = UiSession.getMemberToken();

        if (presenter != null && memberToken != null && !memberToken.isBlank()) {
            presenter.logout(memberToken);
        }

        UiSession.logout();
    }

    public interface ManagementHeaderPresenter {
        void logout(String sessionToken) throws Exception;
    }
}