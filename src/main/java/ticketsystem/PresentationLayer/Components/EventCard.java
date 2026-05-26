package ticketsystem.PresentationLayer.Components;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import ticketsystem.PresentationLayer.Constants.UiRoutes;

public class EventCard extends AppCard {

    public EventCard(
            String category,
            String title,
            String date,
            String location,
            String priceText,
            String imageUrl,
            boolean urgent,
            String companyName,
            Long companyId,
            Long eventId,
            boolean saleOpen,
            boolean hasLottery,
            boolean isSoldOut
    ) {
        super();
        addClassName("event-card");

        if (!saleOpen) {
            addClassName("event-card-sale-closed");
        }

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
                detailRow(VaadinIcon.MAP_MARKER, location)
        );

        if (companyName != null && !companyName.isBlank()) {
            content.add(companyRow(companyName, companyId));
        }

        content.add(createFooter(priceText, saleOpen, hasLottery, isSoldOut, eventId));

        add(imageArea, content);
    }

    private Span detailRow(VaadinIcon icon, String text) {
        Span row = new Span();
        row.addClassName("event-detail-row");

        row.add(icon.create(), new Span(text));
        return row;
    }

    private Span companyRow(String companyName, Long companyId) {
        Span row = new Span();
        row.addClassName("event-detail-row");
        row.addClassName("event-company-row");

        Button companyButton = new Button(companyName);
        companyButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        companyButton.addClassName("event-company-link");
        companyButton.addClickListener(event -> UI.getCurrent().navigate(routeForCompanyEvents(companyId)));

        row.add(VaadinIcon.BUILDING.create(), companyButton);
        return row;
    }

    private Div createFooter(String priceText, boolean saleOpen, boolean hasLottery, boolean isSoldOut, Long eventId) {
        Span fromText = new Span("החל מ-");
        fromText.addClassName("event-price-label");

        Span price = new Span(priceText);
        price.addClassName("event-price");

        Div priceBlock = new Div(fromText, price);
        priceBlock.addClassName("event-price-block");

        Div actions = new Div();
        actions.addClassName("event-card-actions");

        if (!saleOpen && hasLottery) {
            actions.add(createLotteryButton(eventId));
        }

        actions.add(createBuyButton(saleOpen, isSoldOut));

        Div footer = new Div(priceBlock, actions);
        footer.addClassName("event-card-footer");

        return footer;
    }

    private Button createLotteryButton(Long eventId) {
        Button lotteryButton = new Button("הרשמה להגרלה", VaadinIcon.TICKET.create());
        lotteryButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        lotteryButton.addClassName("event-lottery-button");

        if (eventId == null) {
            lotteryButton.setEnabled(false);
            return lotteryButton;
        }

        lotteryButton.addClickListener(event -> UI.getCurrent().navigate(routeForLottery(eventId)));
        return lotteryButton;
    }

    private Div createBuyButton(boolean saleOpen, boolean soldOut) {
        Button buyButton = new Button("רכישה", VaadinIcon.TICKET.create());
        buyButton.addClassName("event-buy-button");

        if (soldOut) {
            buyButton.setEnabled(false);
            buyButton.addClassName("event-buy-button-sold-out");

            Span ribbon = new Span("Sold Out");
            ribbon.addClassName("event-sold-out-ribbon");

            Div wrapper = new Div(buyButton, ribbon);
            wrapper.addClassName("event-buy-button-wrapper");
            return wrapper;
        }

        if (!saleOpen) {
            buyButton.setEnabled(false);
            buyButton.addClassName("event-buy-button-closed");

            Span ribbon = new Span("המכירה טרם נפתחה");
            ribbon.addClassName("event-sale-closed-ribbon");

            Div wrapper = new Div(buyButton, ribbon);
            wrapper.addClassName("event-buy-button-wrapper");
            return wrapper;
        }

        buyButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Div wrapper = new Div(buyButton);
        wrapper.addClassName("event-buy-button-wrapper");
        return wrapper;
    }

    private String routeForLottery(Long eventId) {
        return UiRoutes.LOTTERY_REGISTRATION.replace(":eventId", "eventId=" + String.valueOf(eventId));
    }

    private String routeForCompanyEvents(Long companyId) {
        if (companyId == null) {
            return UiRoutes.EVENTS;
        }

        return UiRoutes.EVENTS + "companyId=" + companyId;
    }
}
