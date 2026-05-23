package ticketsystem.PresentationLayer.Components;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
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
                navLink("אירועים", UiRoutes.EVENTS),
                navLink("הפקות", UiRoutes.OWNER),
                navLink("עזרה", UiRoutes.HELP)
        );
        navLinks.addClassName("nav-links");

        TextField search = new TextField();
        search.setPlaceholder("חיפוש אירועים...");
        search.setPrefixComponent(VaadinIcon.SEARCH.create());
        search.addClassName("top-search");

        HorizontalLayout actions = new HorizontalLayout();
        actions.addClassName("top-actions");

        actions.add(createCartButton());

        if (UiSession.isLoggedIn()) {
            actions.add(createAccountButton());
        }

        if (showAuthAction) {
            actions.add(createAuthButton());
        }

        add(brand, navLinks, search, actions);
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
        int getActiveCartItemsCount(String sessionToken);
    }

    private static final class EmptyHeaderPresenter implements HeaderPresenter {
        @Override
        public int getActiveCartItemsCount(String sessionToken) {
            return 0;
        }
    }
}