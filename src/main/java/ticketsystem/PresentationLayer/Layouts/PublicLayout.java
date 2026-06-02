package ticketsystem.PresentationLayer.Layouts;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLayout;
import org.springframework.beans.factory.annotation.Autowired;
import ticketsystem.PresentationLayer.Components.FooterBar;
import ticketsystem.PresentationLayer.Components.PublicHeader;
import ticketsystem.PresentationLayer.Session.UiVisitCoordinator;
import ticketsystem.PresentationLayer.Components.PublicHeader.HeaderPresenter;

public class PublicLayout extends VerticalLayout implements RouterLayout {

    private final Div headerContainer = new Div();
    private final Div content = new Div();
    private final FooterBar footer = new FooterBar();
    private final HeaderPresenter headerPresenter;

    private final UiVisitCoordinator visitCoordinator;

    @Autowired
    public PublicLayout(UiVisitCoordinator visitCoordinator, HeaderPresenter headerPresenter) {
        this.visitCoordinator = visitCoordinator;
        this.headerPresenter = headerPresenter;

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
    protected void onAttach(AttachEvent event) {
        super.onAttach(event);
        visitCoordinator.ensureVisitAndNotifications(event.getUI());
    }

    @Override
    protected void onDetach(DetachEvent event) {
        visitCoordinator.disconnect();
        super.onDetach(event);
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

    public void refreshHeader() {
        renderHeader();
    }

    private void renderHeader() {
        headerContainer.removeAll();
        headerContainer.add(new PublicHeader(
                shouldShowAuthAction(),
                headerPresenter,
                visitCoordinator
        ));
    }
}