package ticketsystem.PresentationLayer.Layouts;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.RouterLayout;
import ticketsystem.PresentationLayer.Components.SystemAdminHeader;
import ticketsystem.PresentationLayer.Components.SystemAdminSideNav;
import ticketsystem.PresentationLayer.Constants.UiRoutes;
import ticketsystem.PresentationLayer.Session.UiSession;

public class AdminLayout extends Div implements RouterLayout {

    private final Div content = new Div();
    private final SystemAdminSideNav sideNav;
    private final SystemAdminHeader header;

    public AdminLayout() {
        this(null);
    }

    public AdminLayout(SystemAdminHeader.SystemAdminHeaderPresenter headerPresenter) {
        this.sideNav = new SystemAdminSideNav();
        this.header = new SystemAdminHeader(headerPresenter);

        getElement().setAttribute("dir", "rtl");
        addClassName("admin-layout");

        Div shell = new Div();
        shell.addClassName("management-shell");

        Div main = new Div();
        main.addClassName("management-main");

        content.addClassName("management-content");

        main.add(createMobileHeader(), header, content);
        shell.add(sideNav, main);

        add(shell);
    }

    private Header createMobileHeader() {
        Header mobileHeader = new Header();
        mobileHeader.addClassName("management-mobile-header");

        Span brand = new Span("TixNow");
        brand.addClassName("brand-logo");
        brand.addClickListener(event -> UI.getCurrent().navigate(UiRoutes.HOME));

        HorizontalLayout actions = new HorizontalLayout();
        actions.addClassName("management-mobile-actions");

        Button account = new Button(VaadinIcon.USER.create());
        account.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        account.addClassName("header-icon-button");
        account.getElement().setAttribute("aria-label", "אזור אישי");
        account.addClickListener(event -> UI.getCurrent().navigate(UiRoutes.MY_ACCOUNT));

        Button logout = new Button("התנתקות");
        logout.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        logout.addClassName("management-mobile-logout-button");
        logout.addClickListener(event -> {
            UiSession.logout();
            UI.getCurrent().navigate(UiRoutes.HOME);
        });

        actions.add(account, logout);
        mobileHeader.add(brand, actions);
        return mobileHeader;
    }

    @Override
    public void showRouterLayoutContent(HasElement routerContent) {
        content.getElement().removeAllChildren();

        if (routerContent != null) {
            content.getElement().appendChild(routerContent.getElement());
        }
    }
}