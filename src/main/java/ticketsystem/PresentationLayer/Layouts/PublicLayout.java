package ticketsystem.PresentationLayer.Layouts;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLayout;
import ticketsystem.PresentationLayer.Components.FooterBar;
import ticketsystem.PresentationLayer.Components.PublicHeader;

public class PublicLayout extends VerticalLayout implements RouterLayout {

    private final Div headerContainer = new Div();
    private final Div content = new Div();
    private final FooterBar footer = new FooterBar();

    public PublicLayout() {
        getElement().setAttribute("dir", "rtl");

        addClassName("public-layout");
        setWidthFull();
        setMinHeight("100vh");
        setPadding(false);
        setSpacing(false);

        headerContainer.addClassName("public-header-container");

        content.addClassName("public-content");
        content.setWidthFull();

        add(headerContainer, content, footer);
        expand(content);

        renderHeader();
    }

    @Override
    public void showRouterLayoutContent(HasElement routerContent) {
        content.getElement().removeAllChildren();

        renderHeader();

        if (routerContent != null) {
            content.getElement().appendChild(routerContent.getElement());
        }
    }

    protected boolean shouldShowAuthAction() {
        return true;
    }

    protected Div getContentContainer() {
        return content;
    }

    private void renderHeader() {
        headerContainer.removeAll();
        headerContainer.add(new PublicHeader(shouldShowAuthAction()));
    }
}