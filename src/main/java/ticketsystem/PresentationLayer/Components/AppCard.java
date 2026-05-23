package ticketsystem.PresentationLayer.Components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;

public class AppCard extends Div {

    public AppCard(Component... components) {
        addClassName("app-card");
        add(components);
    }
}
