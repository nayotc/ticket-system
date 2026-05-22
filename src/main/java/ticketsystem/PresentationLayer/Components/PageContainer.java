package ticketsystem.PresentationLayer.Components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class PageContainer extends VerticalLayout {

    public PageContainer(Component... components) {
        addClassName("page-container");
        setPadding(false);
        setSpacing(true);
        setWidthFull();

        add(components);
    }
}
