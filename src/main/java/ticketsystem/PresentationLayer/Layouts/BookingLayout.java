package ticketsystem.PresentationLayer.Layouts;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLayout;

public class BookingLayout extends VerticalLayout implements RouterLayout {

    private final Div content = new Div();

    public BookingLayout() {
        getElement().setAttribute("dir", "rtl");

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        Header header = new Header();
        header.addClassName("booking-header");

        Span backIcon = new Span();
        backIcon.add(VaadinIcon.ARROW_FORWARD.create());
        backIcon.addClassName("clickable-icon");

        H1 title = new H1("בחירת כרטיסים");
        title.addClassName("booking-title");

        Span brand = new Span("TixNow");
        brand.addClassName("brand-logo");

        header.add(backIcon, title, brand);

        content.addClassName("booking-content");
        content.setSizeFull();

        add(header, content);
        expand(content);
    }

    @Override
    public void showRouterLayoutContent(HasElement routerContent) {
        content.getElement().removeAllChildren();

        if (routerContent != null) {
            content.getElement().appendChild(routerContent.getElement());
        }
    }
}
