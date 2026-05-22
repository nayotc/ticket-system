package ticketsystem.PresentationLayer.Components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;

public class EmptyState extends AppCard {

    public EmptyState(String iconText, String title, String description, Component action) {
        super();

        addClassName("empty-state");

        if (iconText != null && !iconText.isBlank()) {
            Span icon = new Span(iconText);
            icon.addClassName("empty-state-icon");
            add(icon);
        }

        H2 titleElement = new H2(title);
        titleElement.addClassName("empty-state-title");

        Paragraph descriptionElement = new Paragraph(description);
        descriptionElement.addClassName("empty-state-description");

        Div content = new Div(titleElement, descriptionElement);
        content.addClassName("empty-state-content");

        add(content);

        if (action != null) {
            Div actionWrapper = new Div(action);
            actionWrapper.addClassName("empty-state-action");
            add(actionWrapper);
        }
    }
}