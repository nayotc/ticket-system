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

public class SystemAdminHeader extends Header {

    private final SystemAdminHeaderPresenter presenter;

    public SystemAdminHeader() {
        this(null);
    }

    public SystemAdminHeader(SystemAdminHeaderPresenter presenter) {
        this.presenter = presenter;

        addClassName("management-header");

        Span title = new Span("ניהול מערכת");
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
            try {
                if (presenter != null) {
                    presenter.logout(UiSession.getMemberToken());
                }
            } catch (Exception ignored) {
                // Later the real presenter can show a message popup.
            }

            UiSession.logout();
            UI.getCurrent().navigate(UiRoutes.HOME);
        });

        return button;
    }

    public interface SystemAdminHeaderPresenter {
        void logout(String sessionToken) throws Exception;
    }
}