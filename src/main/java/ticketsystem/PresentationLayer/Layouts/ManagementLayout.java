package ticketsystem.PresentationLayer.Layouts;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.RouterLayout;
import ticketsystem.PresentationLayer.Components.ManagementSideNav;

public class ManagementLayout extends Div implements RouterLayout, BeforeEnterObserver {

    private final Div content = new Div();
    private final ManagementSideNav sideNav = new ManagementSideNav();

    public ManagementLayout() {
        getElement().setAttribute("dir", "rtl");
        addClassName("management-layout");

        Div shell = new Div();
        shell.addClassName("management-shell");

        Div main = new Div();
        main.addClassName("management-main");

        content.addClassName("management-content");

        main.add(createMobileHeader(), content);
        shell.add(sideNav, main);

        add(shell);
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
    public void beforeEnter(BeforeEnterEvent event) {
        event.getRouteParameters()
                .get("companyId")
                .ifPresent(sideNav::setCompanyId);
    }

    @Override
    public void showRouterLayoutContent(HasElement routerContent) {
        content.getElement().removeAllChildren();

        if (routerContent != null) {
            content.getElement().appendChild(routerContent.getElement());
        }
    }
}