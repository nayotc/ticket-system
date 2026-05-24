package ticketsystem.PresentationLayer.Components;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import ticketsystem.PresentationLayer.Constants.UiRoutes;
import ticketsystem.PresentationLayer.Session.UiSession;

public class PublicHeader extends Header {

    private final HeaderPresenter presenter;

    public PublicHeader(boolean showAuthAction) {
        this(showAuthAction, new EmptyHeaderPresenter());
    }

    public PublicHeader(boolean showAuthAction, HeaderPresenter presenter) {
        this.presenter = presenter == null ? new EmptyHeaderPresenter() : presenter;

        addClassName("top-nav");

        Span brand = new Span("TixNow");
        brand.addClassName("brand-logo");
        brand.addClickListener(event -> UI.getCurrent().navigate(UiRoutes.HOME));

        HorizontalLayout navLinks = new HorizontalLayout(
                createEventsButton(),
                createProductionsButton()
                //navLink("עזרה", UiRoutes.HELP)
        );
        navLinks.addClassName("nav-links");

        TextField search = new TextField();
        search.setPlaceholder("חיפוש אירועים...");
        search.setPrefixComponent(VaadinIcon.SEARCH.create());
        search.addClassName("top-search");

        HorizontalLayout actions = new HorizontalLayout();
        actions.addClassName("top-actions");

        if (showAuthAction) {
            actions.add(createAuthButton());
        }

        add(brand, navLinks, search, actions);
    }

    private Button createEventsButton() {
        Button button = new Button("אירועים");
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        button.addClassName("nav-link");
        button.addClassName("nav-link-button");
        button.addClickListener(event -> UI.getCurrent().navigate(UiRoutes.EVENTS));
        return button;
    }

    private Button createProductionsButton() {
        Button button = new Button("הפקות");
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        button.addClassName("nav-link");
        button.addClassName("nav-link-button");
        button.addClickListener(event -> navigateToProductions());
        return button;
    }

    private void navigateToProductions() {
        if (!UiSession.isLoggedIn()) {
            UI.getCurrent().navigate(UiRoutes.LOGIN);
            return;
        }

        try {
            Long managedCompanyId = presenter.getFirstManagedCompanyId(UiSession.getMemberToken());

            if (managedCompanyId == null || managedCompanyId <= 0) {
                UI.getCurrent().navigate(UiRoutes.CREATE_PRODUCTION_COMPANY);
                return;
            }

            UI.getCurrent().navigate(routeForCompanyManagement(managedCompanyId));
        } catch (Exception exception) {
            /*
             * Temporary safe fallback for UI stage.
             *
             * Later, the real presenter should decide whether the error means:
             * 1. invalid session -> navigate to login
             * 2. no managed company -> navigate to create company
             * 3. system error -> show error popup
             */
            UI.getCurrent().navigate(UiRoutes.CREATE_PRODUCTION_COMPANY);
        }
    }

    private String routeForCompanyManagement(long companyId) {
        return UiRoutes.COMPANY_MANAGEMENT.replace(":companyId", String.valueOf(companyId));
    }

    private Button createAuthButton() {
        boolean loggedIn = UiSession.isLoggedIn();

        Button button = new Button(loggedIn ? "התנתקות" : "התחברות");
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        button.addClickListener(event -> {
            if (UiSession.isLoggedIn()) {
                UiSession.logout();
                UI.getCurrent().navigate(UiRoutes.HOME);
            } else {
                UI.getCurrent().navigate(UiRoutes.LOGIN);
            }
        });

        return button;
    }

    private Anchor navLink(String text, String route) {
        String href = route == null || route.isBlank() ? "/" : "/" + route;

        Anchor anchor = new Anchor(href, text);
        anchor.addClassName("nav-link");

        return anchor;
    }

    public interface HeaderPresenter {
        /*
         * Return the id of one company the user can manage.
         *
         * Expected behavior:
         * - return company id when the member is owner/founder/manager of at least one company
         * - return null when the member is not linked to any production company
         * - throw exception only for real failures, for example invalid session or system error
         */
        Long getFirstManagedCompanyId(String sessionToken) throws Exception;
    }

    private static class EmptyHeaderPresenter implements HeaderPresenter {
        @Override
        public Long getFirstManagedCompanyId(String sessionToken) {
            return null;
        }
    }
}
