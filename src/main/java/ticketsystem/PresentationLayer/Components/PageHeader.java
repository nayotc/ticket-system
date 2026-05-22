package ticketsystem.PresentationLayer.Components;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class PageHeader extends VerticalLayout {

    public PageHeader(String title, String description) {
        setPadding(false);
        setSpacing(false);
        setWidthFull();

        H1 titleElement = new H1(title);
        titleElement.addClassName("page-title");

        Paragraph descriptionElement = new Paragraph(description);
        descriptionElement.addClassName("page-description");

        add(titleElement, descriptionElement);
    }
}
