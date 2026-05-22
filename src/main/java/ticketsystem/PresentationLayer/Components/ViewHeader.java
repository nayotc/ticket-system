package ticketsystem.PresentationLayer.Components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;

public class ViewHeader extends Div {

    private final Div actions = new Div();

    public ViewHeader(String title, String description, Component... actionComponents) {
        addClassName("view-header");

        Div textBlock = new Div();
        textBlock.addClassName("view-header-text");

        H1 titleElement = new H1(title);
        titleElement.addClassName("view-title");

        textBlock.add(titleElement);

        if (description != null && !description.isBlank()) {
            Paragraph descriptionElement = new Paragraph(description);
            descriptionElement.addClassName("view-description");
            textBlock.add(descriptionElement);
        }

        actions.addClassName("view-header-actions");
        actions.add(actionComponents);

        add(textBlock, actions);
    }

    public Div getActions() {
        return actions;
    }
}