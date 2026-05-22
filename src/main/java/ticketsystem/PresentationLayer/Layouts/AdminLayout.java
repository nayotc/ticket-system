package ticketsystem.PresentationLayer.Layouts;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.RouterLayout;
import ticketsystem.PresentationLayer.Constants.UiRoutes;

public class AdminLayout extends Div implements RouterLayout {

    private final Div content = new Div();

    public AdminLayout() {
        getElement().setAttribute("dir", "rtl");
        addClassName("admin-layout");

        Div shell = new Div();
        shell.addClassName("management-shell");

        shell.add(createSideNav(), createMainArea());

        add(shell);
    }

    private Div createSideNav() {
        Div sideNav = new Div();
        sideNav.addClassName("management-side-nav");

        Div header = new Div();
        header.addClassName("management-side-nav-header");

        Span title = new Span("Admin Panel");
        title.addClassName("management-side-nav-title");

        Span subtitle = new Span("System management");
        subtitle.addClassName("management-side-nav-subtitle");

        header.add(title, subtitle);

        Div links = new Div();
        links.addClassName("management-side-nav-links");

        Button dashboard = new Button("לוח ניהול מערכת");
        dashboard.addClassName("management-nav-item");
        dashboard.addClickListener(event -> UI.getCurrent().navigate(UiRoutes.ADMIN_DASHBOARD));

        links.add(dashboard);

        sideNav.add(header, links);
        return sideNav;
    }

    private Div createMainArea() {
        Div main = new Div();
        main.addClassName("management-main");

        main.add(createMobileHeader(), content);

        content.addClassName("management-content");

        return main;
    }

    private Header createMobileHeader() {
        Header header = new Header();
        header.addClassName("management-mobile-header");

        Span brand = new Span("TixNow");
        brand.addClassName("brand-logo");

        Span menu = new Span("☰");
        menu.addClassName("mobile-menu-icon");

        header.add(brand, menu);
        return header;
    }

    @Override
    public void showRouterLayoutContent(HasElement routerContent) {
        content.getElement().removeAllChildren();

        if (routerContent != null) {
            content.getElement().appendChild(routerContent.getElement());
        }
    }
}