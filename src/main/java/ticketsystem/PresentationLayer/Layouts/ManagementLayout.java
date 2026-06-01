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
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.RouterLayout;
import ticketsystem.PresentationLayer.Components.ManagementHeader;
import ticketsystem.PresentationLayer.Components.ManagementSideNav;
import ticketsystem.PresentationLayer.Constants.UiRoutes;
import ticketsystem.PresentationLayer.Session.UiSession;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import ticketsystem.PresentationLayer.Session.UiVisitCoordinator;

public class ManagementLayout extends Div implements RouterLayout, BeforeEnterObserver {

    private final Div content = new Div();
    private final ManagementSideNav sideNav;
    private final ManagementHeader managementHeader;
    private final UiVisitCoordinator visitCoordinator;

    @Autowired
    public ManagementLayout(
            ObjectProvider<ManagementSideNav.ManagementSideNavPresenter> sideNavPresenterProvider,
            ObjectProvider<ManagementHeader.ManagementHeaderPresenter> headerPresenterProvider,
            UiVisitCoordinator visitCoordinator
    ) {
        ManagementSideNav.ManagementSideNavPresenter sideNavPresenter =
                sideNavPresenterProvider.getIfAvailable();

        ManagementHeader.ManagementHeaderPresenter headerPresenter =
                headerPresenterProvider.getIfAvailable();

        this.sideNav = new ManagementSideNav(sideNavPresenter);
        this.visitCoordinator = visitCoordinator;
        this.managementHeader = new ManagementHeader(headerPresenter, visitCoordinator);
        getElement().setAttribute("dir", "rtl");
        addClassName("management-layout");

        Div shell = new Div();
        shell.addClassName("management-shell");

        Div main = new Div();
        main.addClassName("management-main");

        content.addClassName("management-content");

        main.add(createMobileHeader(), managementHeader, content);
        shell.add(sideNav, main);

        add(shell);
    }

    private Header createMobileHeader() {
        Header header = new Header();
        header.addClassName("management-mobile-header");

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
            try {
                if (visitCoordinator != null) {
                    visitCoordinator.logoutToGuest(UI.getCurrent());
                } else {
                    UiSession.logout();
                }
            } catch (Exception ignored) {
                UiSession.logout();
            }

            UI.getCurrent().navigate(UiRoutes.HOME);
            UI.getCurrent().getPage().reload();
        });

        actions.add(account, logout);
        header.add(brand, actions);
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
