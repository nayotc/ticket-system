package ticketsystem.PresentationLayer.Components;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import ticketsystem.PresentationLayer.Constants.UiRoutes;
import ticketsystem.PresentationLayer.Session.UiSession;
import ticketsystem.PresentationLayer.Session.UiVisitCoordinator;

public class PublicHeader extends Header {

    private final HeaderPresenter presenter;
    private final UiVisitCoordinator visitCoordinator;

    public PublicHeader(boolean showAuthAction,
                        UiVisitCoordinator visitCoordinator) {
        this(showAuthAction, new EmptyHeaderPresenter(), visitCoordinator);
    }

    public PublicHeader(boolean showAuthAction,
                        HeaderPresenter presenter,
                        UiVisitCoordinator visitCoordinator) {
        this.presenter = presenter == null ? new EmptyHeaderPresenter() : presenter;
        this.visitCoordinator = visitCoordinator;

        addClassName("top-nav");

        Span brand = new Span("TixNow");
        brand.addClassName("brand-logo");
        brand.addClickListener(event -> UI.getCurrent().navigate(UiRoutes.HOME));

        HorizontalLayout navLinks = new HorizontalLayout();
        navLinks.add(
                createEventsButton(),
                createProductionsButton()
                //navLink("עזרה", UiRoutes.HELP)
        );

        if (shouldShowSystemAdminButton()) {
            navLinks.add(createSystemAdminButton());
        }

        navLinks.addClassName("nav-links");
        navLinks.addClassName("nav-links");

        TextField search = new TextField();
        search.setPlaceholder("חיפוש אירועים...");
        search.setPrefixComponent(VaadinIcon.SEARCH.create());
        search.addClassName("top-search");

        HorizontalLayout actions = new HorizontalLayout();
        actions.addClassName("top-actions");

        if (UiSession.isLoggedIn()) {
            actions.add(createAccountButton());
        }

        actions.add(createCartButton());

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
        button.addClickListener(event -> UI.getCurrent().navigate(UiRoutes.SEARCH_RESULTS));
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

    private Div createCartButton() {
        Div wrapper = new Div();
        wrapper.addClassName("header-icon-wrapper");

        Button button = new Button(VaadinIcon.CART.create());
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        button.addClassName("header-icon-button");
        button.getElement().setAttribute("aria-label", "עגלת קניות");
        button.addClickListener(event -> UI.getCurrent().navigate(UiRoutes.ACTIVE_ORDER_CART));

        Span badge = new Span(String.valueOf(getCartItemsCount()));
        badge.addClassName("header-cart-badge");

        wrapper.add(button, badge);
        return wrapper;
    }

    private Button createAccountButton() {
        Button button = new Button(VaadinIcon.USER.create());
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        button.addClassName("header-icon-button");
        button.getElement().setAttribute("aria-label", "אזור אישי");
        button.addClickListener(event -> UI.getCurrent().navigate(UiRoutes.MY_ACCOUNT));
        return button;
    }

    private int getCartItemsCount() {
        try {
            int count = presenter.getActiveCartItemsCount(UiSession.getMemberToken());
            return Math.max(count, 0);
        } catch (Exception exception) {
            return 0;
        }
    }

    private void navigateToProductions() {
        if (!UiSession.isLoggedIn()) {
            Notification notification = Notification.show("כדי לגשת לעמוד הפקות יש להתחבר למערכת", 3500, Notification.Position.TOP_CENTER);
            notification.addThemeVariants(NotificationVariant.LUMO_WARNING);
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
                visitCoordinator.logoutToGuest(UI.getCurrent());
                UI.getCurrent().navigate(UiRoutes.HOME);
                UI.getCurrent().getPage().reload();
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

    private boolean shouldShowSystemAdminButton() {
        if (!UiSession.isLoggedIn()) {
            return false;
        }

        try {
            return presenter.canAccessSystemAdmin(UiSession.getMemberToken());
        } catch (Exception exception) {
            return false;
        }
    }

    private Button createSystemAdminButton() {
        Button button = new Button("ניהול מערכת");
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        button.addClassName("nav-link");
        button.addClassName("nav-link-button");
        button.addClickListener(event -> UI.getCurrent().navigate(UiRoutes.ADMIN_DASHBOARD));
        return button;
    }

    public interface HeaderPresenter {
        int getActiveCartItemsCount(String sessionToken);

        Long getFirstManagedCompanyId(String sessionToken) throws Exception;

        default boolean canAccessSystemAdmin(String sessionToken) throws Exception {
            return false;
        }
    }

    private static class EmptyHeaderPresenter implements HeaderPresenter {
        @Override
        public Long getFirstManagedCompanyId(String sessionToken) {
            return null;
        }
        @Override
        public int getActiveCartItemsCount(String sessionToken) {
            return 0;
        }
    }
}
