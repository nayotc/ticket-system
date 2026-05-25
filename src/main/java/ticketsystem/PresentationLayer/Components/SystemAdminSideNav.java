package ticketsystem.PresentationLayer.Components;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import ticketsystem.PresentationLayer.Constants.UiRoutes;

public class SystemAdminSideNav extends Div {

    public SystemAdminSideNav() {
        addClassName("management-side-nav");

        add(
                createHeader(),
                createLinks(),
                createBottomActions()
        );
    }

    private Div createHeader() {
        Div header = new Div();
        header.addClassName("management-side-nav-header");

        Span brand = new Span("TixNow");
        brand.addClassName("brand-logo");
        brand.addClickListener(event -> UI.getCurrent().navigate(UiRoutes.HOME));

        Span subtitle = new Span("System control");
        subtitle.addClassName("management-side-nav-subtitle");

        header.add(brand, subtitle);
        return header;
    }

    private Div createLinks() {
        Div links = new Div();
        links.addClassName("admin-side-nav-links");

        links.add(
                navButton("לוח בקרה", VaadinIcon.DASHBOARD, "admin-overview"),
                navButton("משתמשים פעילים", VaadinIcon.USERS, "admin-users"),
                navButton("חברות פעילות", VaadinIcon.BUILDING, "admin-companies"),
                navButton("היסטוריה לפי חברה", VaadinIcon.ARCHIVE, "admin-company-history"),
                navButton("היסטוריה לפי משתמש", VaadinIcon.USER_CARD, "admin-user-history")
        );

        return links;
    }

    private Button navButton(String text, VaadinIcon icon, String sectionId) {
        Button button = new Button(text, icon.create());
        button.addClassName("management-nav-item");
        button.addClickListener(event -> scrollTo(sectionId));
        return button;
    }

    private void scrollTo(String sectionId) {
        UI.getCurrent().getPage().executeJs(
                """
                const el = document.getElementById($0);
                if (!el) return;
    
                const header =
                    document.querySelector('.management-header') ||
                    document.querySelector('.management-mobile-header');
    
                const headerHeight = header ? header.getBoundingClientRect().height : 72;
                const extraSpace = 28;
                const top = el.getBoundingClientRect().top + window.pageYOffset - headerHeight - extraSpace;
    
                window.scrollTo({
                    top: Math.max(top, 0),
                    behavior: 'smooth'
                });
                """,
                sectionId
        );
    }

    private Div createBottomActions() {
        Div bottom = new Div();
        bottom.addClassName("management-side-nav-bottom");

        Button home = new Button("חזרה לעמוד הבית", VaadinIcon.HOME.create());
        home.addClassName("management-create-company-button");
        home.addClickListener(event -> UI.getCurrent().navigate(UiRoutes.HOME));

        bottom.add(home);
        return bottom;
    }
}