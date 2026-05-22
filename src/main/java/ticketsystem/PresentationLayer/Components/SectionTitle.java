package ticketsystem.PresentationLayer.Components;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class SectionTitle extends VerticalLayout {

    public SectionTitle(String title, String subtitle) {
        setPadding(false);
        setSpacing(false);
        setWidthFull();

        H2 titleElement = new H2(title);
        titleElement.addClassName("section-title");

        Paragraph subtitleElement = new Paragraph(subtitle);
        subtitleElement.addClassName("section-subtitle");

        add(titleElement, subtitleElement);
    }
}