package ticketsystem.PresentationLayer.Components;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

public class MetricCard extends AppCard {

    public MetricCard(String label, String value, String helperText) {
        super();

        addClassName("metric-card");

        Span labelElement = new Span(label);
        labelElement.addClassName("metric-label");

        Span valueElement = new Span(value);
        valueElement.addClassName("metric-value");

        Div content = new Div(labelElement, valueElement);
        content.addClassName("metric-content");

        add(content);

        if (helperText != null && !helperText.isBlank()) {
            Span helper = new Span(helperText);
            helper.addClassName("metric-helper");
            add(helper);
        }
    }
}