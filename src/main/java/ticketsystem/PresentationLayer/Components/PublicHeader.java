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

    public PublicHeader(boolean showAuthAction) {
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

        if (showAuthAction) {
            actions.add(createAuthButton());
        }

        add(brand, navLinks, search, actions);
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
}