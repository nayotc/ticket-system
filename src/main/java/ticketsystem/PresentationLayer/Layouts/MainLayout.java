package ticketsystem.PresentationLayer.Layouts;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import ticketsystem.PresentationLayer.Constants.UiRoutes;

public class MainLayout extends AppLayout {

    public MainLayout() {
        getElement().setAttribute("dir", "rtl");
        addToNavbar(createTopNav());
    }

    private Header createTopNav() {
        Header header = new Header();
        header.addClassName("top-nav");

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

        Button loginButton = new Button("התחברות");
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button notificationsButton = new Button(VaadinIcon.BELL.create());
        notificationsButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);

        Button profileButton = new Button(VaadinIcon.USER.create());
        profileButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);

        HorizontalLayout actions = new HorizontalLayout(loginButton, notificationsButton, profileButton);
        actions.addClassName("top-actions");

        header.add(brand, navLinks, search, actions);
        return header;
    }

    private Anchor navLink(String text, String route) {
        String href = route == null || route.isBlank() ? "/" : "/" + route;
        Anchor anchor = new Anchor(href, text);
        anchor.addClassName("nav-link");
        return anchor;
    }
}
