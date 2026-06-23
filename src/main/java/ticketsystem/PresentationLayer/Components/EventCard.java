package ticketsystem.PresentationLayer.Components;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import ticketsystem.DomainLayer.event.SaleStatus;
import ticketsystem.PresentationLayer.Constants.UiRoutes;
import com.vaadin.flow.router.QueryParameters;
import java.util.LinkedHashMap;
import java.util.Map;

public class EventCard extends AppCard {

    private final Long eventId;
    private EventCardActionHandler actionHandler = EventCardActionHandler.navigationOnly();

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
            SaleStatus saleStatus,
            boolean hasLottery
    ) {
        super();
        this.eventId = eventId;

        SaleStatus resolvedSaleStatus = saleStatus == null ? SaleStatus.NOT_STARTED : saleStatus;

        addClassName("event-card");
        addClassName(statusClass(resolvedSaleStatus));

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

        content.add(createFooter(priceText, resolvedSaleStatus, hasLottery));
        add(imageArea, content);
    }

    public void setActionHandler(EventCardActionHandler actionHandler) {
        this.actionHandler = actionHandler == null
                ? EventCardActionHandler.navigationOnly()
                : actionHandler;
    }

    private String statusClass(SaleStatus saleStatus) {
        return switch (saleStatus) {
            case NOT_STARTED -> "event-card-sale-not-started";
            case PRE_SALE -> "event-card-pre-sale";
            case ONGOING -> "event-card-sale-ongoing";
            case SOLD_OUT -> "event-card-sold-out";
            case ENDED -> "event-card-sale-ended";
        };
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
        companyButton.addClickListener(event -> navigateToCompanyEvents(companyId, companyName));

        row.add(VaadinIcon.BUILDING.create(), companyButton);
        return row;
    }

    private Div createFooter(String priceText, SaleStatus saleStatus, boolean hasLottery) {
        Span fromText = new Span("החל מ-");
        fromText.addClassName("event-price-label");

        Span price = new Span(priceText);
        price.addClassName("event-price");

        Div priceBlock = new Div(fromText, price);
        priceBlock.addClassName("event-price-block");

        Div actions = new Div();
        actions.addClassName("event-card-actions");

        switch (saleStatus) {
            case NOT_STARTED -> {
                if (hasLottery) {
                    actions.add(createLotteryButton());
                }
                actions.add(createDisabledRibbonButton(
                        "רכישה",
                        "event-buy-button-closed",
                        "המכירה טרם נפתחה",
                        "event-sale-not-started-ribbon"
                ));
            }
            case PRE_SALE -> actions.add(createPreSaleButton());
            case ONGOING -> actions.add(createPurchaseButton());
            case SOLD_OUT -> actions.add(createDisabledRibbonButton(
                    "רכישה",
                    "event-buy-button-sold-out",
                    "Sold Out",
                    "event-sold-out-ribbon"
            ));
            case ENDED -> actions.add(createDisabledButton(
                    "המכירה הסתיימה",
                    "event-buy-button-ended"
            ));
        }

        Div footer = new Div(priceBlock, actions);
        footer.addClassName("event-card-footer");
        return footer;
    }

    private Button createLotteryButton() {
        Button lotteryButton = new Button("הרשמה להגרלה למכירה מוקדמת", VaadinIcon.TICKET.create());
        lotteryButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        lotteryButton.addClassName("event-lottery-button");

        if (eventId == null) {
            lotteryButton.setEnabled(false);
            return lotteryButton;
        }

        lotteryButton.addClickListener(event -> actionHandler.onLotteryRegistrationRequested(eventId));
        return lotteryButton;
    }

    private Div createPreSaleButton() {
        Button preSaleButton = new Button("מכירה מוקדמת", VaadinIcon.KEY.create());
        preSaleButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        preSaleButton.addClassName("event-buy-button");
        preSaleButton.addClassName("event-buy-button-pre-sale");

        if (eventId == null) {
            preSaleButton.setEnabled(false);
            return wrap(preSaleButton);
        }

        preSaleButton.addClickListener(event -> openPreSaleCodeDialog());
        return wrap(preSaleButton);
    }

    private Div createPurchaseButton() {
        Button buyButton = new Button("רכישה", VaadinIcon.TICKET.create());
        buyButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        buyButton.addClassName("event-buy-button");

        if (eventId == null) {
            buyButton.setEnabled(false);
        } else {
            buyButton.addClickListener(event -> actionHandler.onPurchaseRequested(eventId));
        }

        return wrap(buyButton);
    }

    private Div createDisabledButton(String text, String className) {
        Button button = new Button(text, VaadinIcon.TICKET.create());
        button.setEnabled(false);
        button.addClassName("event-buy-button");
        button.addClassName(className);
        return wrap(button);
    }

    private Div createDisabledRibbonButton(String buttonText, String buttonClassName, String ribbonText, String ribbonClassName) {
        Button button = new Button(buttonText, VaadinIcon.TICKET.create());
        button.setEnabled(false);
        button.addClassName("event-buy-button");
        button.addClassName(buttonClassName);

        Span ribbon = new Span(ribbonText);
        ribbon.addClassName(ribbonClassName);

        Div wrapper = new Div(button, ribbon);
        wrapper.addClassName("event-buy-button-wrapper");
        return wrapper;
    }

    private Div wrap(Button button) {
        Div wrapper = new Div(button);
        wrapper.addClassName("event-buy-button-wrapper");
        return wrapper;
    }

    private void openPreSaleCodeDialog() {
        Dialog dialog = new Dialog();
        dialog.getElement().setAttribute("dir", "rtl");
        dialog.getElement().getThemeList().add("message-popup");
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(true);
        dialog.setDraggable(false);
        dialog.setResizable(false);

        Div card = new Div();
        card.addClassName("pre-sale-code-dialog-card");

        H3 title = new H3("כניסה למכירה מוקדמת");
        title.addClassName("pre-sale-code-dialog-title");

        Paragraph description = new Paragraph("הזן את קוד הזכייה שקיבלת בהגרלה כדי להמשיך לרכישת כרטיסים.");
        description.addClassName("pre-sale-code-dialog-description");

        TextField codeField = new TextField("קוד הגרלה");
        codeField.setWidthFull();
        codeField.setPlaceholder("לדוגמה: TIX-2026-LUCKY");
        codeField.addClassName("pre-sale-code-field");

        Span errorMessage = new Span();
        errorMessage.addClassName("pre-sale-code-error");
        errorMessage.setVisible(false);

        Button cancel = new Button("ביטול", event -> dialog.close());
        cancel.addClassName("message-popup-secondary-button");

        Button submit = new Button("המשך לרכישה", VaadinIcon.ARROW_BACKWARD.create());
        submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submit.addClassName("pre-sale-code-submit-button");
        submit.addClickListener(event -> handlePreSaleCodeSubmit(dialog, codeField, errorMessage));

        Div actions = new Div(cancel, submit);
        actions.addClassName("pre-sale-code-actions");

        card.add(title, description, codeField, errorMessage, actions);
        dialog.add(card);
        dialog.open();
        codeField.focus();
    }

    private void handlePreSaleCodeSubmit(Dialog dialog, TextField codeField, Span errorMessage) {
        String code = codeField.getValue() == null ? "" : codeField.getValue().trim();

        if (code.isBlank()) {
            showPreSaleError(errorMessage, "יש להזין את קוד ההגרלה.");
            return;
        }

        boolean valid;
        try {
            valid = actionHandler.isPreSaleCodeValid(eventId, code);
        } catch (Exception exception) {
            showPreSaleError(errorMessage, safeMessage(exception, "לא הצלחנו לבדוק את הקוד. נסה שוב."));
            return;
        }

        if (!valid) {
            showPreSaleError(errorMessage, "הקוד שהוזן אינו תקין או שאינו מתאים לאירוע הזה.");
            return;
        }

        dialog.close();
        actionHandler.onPreSaleApproved(eventId, code);
    }

    private void showPreSaleError(Span errorMessage, String message) {
        errorMessage.setText(message);
        errorMessage.setVisible(true);
    }

    private String safeMessage(Exception exception, String fallback) {
        if (exception == null || exception.getMessage() == null || exception.getMessage().isBlank()) {
            return fallback;
        }
        return exception.getMessage();
    }

    private static void navigate(String route) {
        UI ui = UI.getCurrent();
        if (ui != null && route != null && !route.isBlank()) {
            ui.navigate(route);
        }
    }

    private static String routeForLottery(Long eventId) {
        if (eventId == null) {
            return UiRoutes.SEARCH_RESULTS;
        }
        return UiRoutes.LOTTERY_REGISTRATION.replace(":eventId", String.valueOf(eventId));
    }

    private static String routeForTickets(Long eventId) {
        if (eventId == null) {
            return UiRoutes.SEARCH_RESULTS;
        }
        return UiRoutes.TICKET_SELECTION.replace(":eventId", String.valueOf(eventId));
    }

    private static void navigateToCompanyEvents(Long companyId, String companyName) {
        UI ui = UI.getCurrent();
        if (ui == null) {
            return;
        }

        if (companyId == null) {
            ui.navigate(UiRoutes.SEARCH_RESULTS);
            return;
        }

        Map<String, String> params = new LinkedHashMap<>();
        if (companyName != null && !companyName.isBlank()) {
            params.put("companyName", companyName.trim());
        }

        ui.navigate(
                UiRoutes.COMPANY_SEARCH_RESULTS.replace(":companyId", String.valueOf(companyId)),
                QueryParameters.simple(params)
        );
    }

    public interface EventCardActionHandler {
        void onPurchaseRequested(Long eventId);

        void onLotteryRegistrationRequested(Long eventId);

        boolean isPreSaleCodeValid(Long eventId, String lotteryCode) throws Exception;

        void onPreSaleApproved(Long eventId, String lotteryCode);

        static EventCardActionHandler navigationOnly() {
            return new EventCardActionHandler() {
                @Override
                public void onPurchaseRequested(Long eventId) {
                    navigate(routeForTickets(eventId));
                }

                @Override
                public void onLotteryRegistrationRequested(Long eventId) {
                    navigate(routeForLottery(eventId));
                }

                @Override
                public boolean isPreSaleCodeValid(Long eventId, String lotteryCode) {
                    return lotteryCode != null && !lotteryCode.trim().isEmpty();
                }

                @Override
                public void onPreSaleApproved(Long eventId, String lotteryCode) {
                    navigate(routeForTickets(eventId));
                }
            };
        }
    }
}
