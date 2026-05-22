package ticketsystem.PresentationLayer.Components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;

public class EventCard extends AppCard {

    public EventCard(
            String category,
            String title,
            String date,
            String location,
            String priceText,
            String imageUrl,
            boolean urgent
    ) {
        super();
        addClassName("event-card");

        if (urgent) {
            Span badge = new Span("אחרונים");
            badge.addClassName("event-urgent-badge");
            add(badge);
        }

        Div imageArea = new Div();
        imageArea.addClassName("event-card-image-area");

        Image image = new Image(imageUrl, title);
        image.addClassName("event-card-image");

        Div overlay = new Div();
        overlay.addClassName("event-card-image-overlay");

        Div imageText = new Div();
        imageText.addClassName("event-card-image-text");

        Span categoryBadge = new Span(category);
        categoryBadge.addClassName("event-category-badge");

        H3 titleElement = new H3(title);
        titleElement.addClassName("event-title");

        imageText.add(categoryBadge, titleElement);
        imageArea.add(image, overlay, imageText);

        Div content = new Div();
        content.addClassName("event-card-content");

        content.add(
                detailRow(VaadinIcon.CALENDAR, date),
                detailRow(VaadinIcon.MAP_MARKER, location),
                createFooter(priceText)
        );

        add(imageArea, content);
    }

    private Span detailRow(VaadinIcon icon, String text) {
        Span row = new Span();
        row.addClassName("event-detail-row");

        row.add(icon.create(), new Span(text));
        return row;
    }

    private Div createFooter(String priceText) {
        Span fromText = new Span("החל מ-");
        fromText.addClassName("event-price-label");

        Span price = new Span(priceText);
        price.addClassName("event-price");

        Div priceBlock = new Div(fromText, price);
        priceBlock.addClassName("event-price-block");

        Button buyButton = new Button("רכישה");
        buyButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        buyButton.addClassName("event-buy-button");

        Div footer = new Div(priceBlock, buyButton);
        footer.addClassName("event-card-footer");

        return footer;
    }
}