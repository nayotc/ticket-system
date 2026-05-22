package ticketsystem.PresentationLayer.Layouts;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLayout;
import ticketsystem.PresentationLayer.Constants.UiRoutes;

public class AuthLayout extends VerticalLayout implements RouterLayout {

    private final Div content = new Div();

    public AuthLayout() {
        getElement().setAttribute("dir", "rtl");

        addClassName("auth-layout");
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        add(createHeader(), content);

        content.addClassName("auth-content");
        content.setWidthFull();

        expand(content);
    }

    private Header createHeader() {
        Header header = new Header();
        header.addClassName("auth-header");

        Span brand = new Span("TixNow");
        brand.addClassName("brand-logo");
        brand.addClickListener(event -> UI.getCurrent().navigate(UiRoutes.HOME));

        header.add(brand);
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