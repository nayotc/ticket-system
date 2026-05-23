package ticketsystem.PresentationLayer.Components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

public class ActionBar extends HorizontalLayout {

    public ActionBar(Component... actions) {
        addClassName("action-bar");
        setPadding(false);
        setSpacing(false);
        setWidthFull();

        add(actions);
    }
}
