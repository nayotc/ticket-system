package ticketsystem.PresentationLayer.Components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;

public class FormCard extends AppCard {

    private final Div content = new Div();

    public FormCard(String title, String subtitle, Component... components) {
        super();

        addClassName("form-card");

        if (title != null && !title.isBlank()) {
            H2 titleElement = new H2(title);
            titleElement.addClassName("form-card-title");
            add(titleElement);
        }

        if (subtitle != null && !subtitle.isBlank()) {
            Paragraph subtitleElement = new Paragraph(subtitle);
            subtitleElement.addClassName("form-card-subtitle");
            add(subtitleElement);
        }

        content.addClassName("form-card-content");
        content.add(components);

        add(content);
    }

    public Div getContent() {
        return content;
    }
}