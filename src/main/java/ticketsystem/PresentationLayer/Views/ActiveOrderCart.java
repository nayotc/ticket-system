package ticketsystem.PresentationLayer.Views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import ticketsystem.DTO.ActiveOrderDTO;
import ticketsystem.DTO.TicketDTO;
import ticketsystem.PresentationLayer.Components.EmptyState;
import ticketsystem.PresentationLayer.Constants.UiRoutes;
import ticketsystem.PresentationLayer.Layouts.PublicLayout;
import ticketsystem.PresentationLayer.Session.UiSession;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Route(value = UiRoutes.ACTIVE_ORDER_CART, layout = PublicLayout.class)
public class ActiveOrderCart extends VerticalLayout {

    private final Presenter presenter;

    private ActiveOrderDTO activeOrder;
    private CartEventInfo eventInfo;
    private CartPricing pricing;
    private String currentCouponCode = "";

    public ActiveOrderCart() {
        this(new DemoActiveOrderCartPresenter());
    }

    public ActiveOrderCart(Presenter presenter) {
        this.presenter = presenter;

        getElement().setAttribute("dir", "rtl");
        addClassName("active-order-cart-page");
        setPadding(false);
        setSpacing(false);
        setWidthFull();

        loadCart();
    }

    private void loadCart() {
        try {
            activeOrder = presenter.loadActiveOrder(resolveSessionToken());

            if (activeOrder == null || tickets().isEmpty()) {
                renderEmptyCart();
                return;
            }

            eventInfo = presenter.loadEventInfo(activeOrder.getEventId());
            pricing = presenter.calculatePricing(activeOrder.getOrderId(), currentCouponCode);
            renderCart();
        } catch (Exception exception) {
            showError(exception.getMessage());
            renderEmptyCart();
        }
    }

    private String resolveSessionToken() {
        return UiSession.getMemberToken();
    }

    private void renderCart() {
        removeAll();

        Div shell = new Div();
        shell.addClassName("cart-shell");

        shell.add(createHeader(), createLayout());

        add(shell);
    }

    private Div createHeader() {
        Div header = new Div();
        header.addClassName("cart-view-header");

        H1 title = new H1("עגלת הקניות שלך");
        title.addClassName("cart-view-title");

        Paragraph subtitle = new Paragraph(tickets().size() + " כרטיסים נבחרו לאירוע אחד");
        subtitle.addClassName("cart-view-subtitle");

        header.add(title, subtitle);
        return header;
    }

    private Div createLayout() {
        Div layout = new Div();
        layout.addClassName("cart-layout");

        Div itemsList = new Div();
        itemsList.addClassName("cart-items-list");

        for (TicketDTO ticket : tickets()) {
            itemsList.add(createTicketCard(ticket));
        }

        layout.add(itemsList, createSummaryCard());
        return layout;
    }

    private Div createTicketCard(TicketDTO ticket) {
        Div card = new Div();
        card.addClassName("app-card");
        card.addClassName("cart-ticket-card");

        Div image = new Div();
        image.addClassName("cart-ticket-image");
        image.add(VaadinIcon.TICKET.create());

        Div details = new Div();
        details.addClassName("cart-ticket-details");

        H3 eventName = new H3(eventInfo.eventName());
        eventName.addClassName("cart-ticket-title");

        Span date = iconText(VaadinIcon.CALENDAR, eventInfo.dateText());
        Span location = iconText(VaadinIcon.MAP_MARKER, eventInfo.locationText());

        Span seat = new Span(seatText(ticket));
        seat.addClassName("cart-ticket-seat");

        details.add(eventName, date, location, seat);

        Div priceBlock = new Div();
        priceBlock.addClassName("cart-ticket-price-block");

        Span price = new Span(formatMoney(ticket.getPrice()));
        price.addClassName("cart-ticket-price");

        Button removeButton = new Button(VaadinIcon.TRASH.create());
        removeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        removeButton.addClassName("cart-ticket-remove");
        removeButton.getElement().setAttribute("aria-label", "הסר כרטיס");
        removeButton.addClickListener(event -> removeTicket(ticket));

        priceBlock.add(price, removeButton);

        card.add(image, details, priceBlock);
        return card;
    }

    private Span iconText(VaadinIcon icon, String text) {
        Span row = new Span();
        row.addClassName("cart-ticket-meta");

        if (text == null || text.isBlank()) {
            text = "פרטים יעודכנו בהמשך";
        }

        row.add(icon.create(), new Span(text));
        return row;
    }

    private Div createSummaryCard() {
        Div card = new Div();
        card.addClassName("app-card");
        card.addClassName("cart-summary-card");

        H3 title = new H3("סיכום הזמנה");
        title.addClassName("cart-summary-title");

        Div rows = new Div();
        rows.addClassName("cart-summary-rows");

        rows.add(priceRow("סכום ביניים (" + tickets().size() + " כרטיסים)", pricing.subtotal()));

        if (pricing.discountTotal().compareTo(BigDecimal.ZERO) > 0) {
            rows.add(priceRow("הנחות", pricing.discountTotal().negate()));
        }

        Div couponRow = createCouponRow();
        Div discounts = createDiscountsBlock();

        Div totalRow = new Div();
        totalRow.addClassName("cart-total-row");

        Span totalLabel = new Span("סך הכל");
        totalLabel.addClassName("cart-total-label");

        Span totalValue = new Span(formatMoney(pricing.total()));
        totalValue.addClassName("cart-total-value");

        totalRow.add(totalLabel, totalValue);

        Button continueButton = new Button("המשך לתשלום", VaadinIcon.ARROW_BACKWARD.create());
        continueButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        continueButton.addClassName("cart-checkout-button");
        continueButton.setEnabled(!tickets().isEmpty());
        continueButton.addClickListener(event -> continueToCheckout());

        Span secureText = iconText(VaadinIcon.LOCK, "תשלום מאובטח באמצעות TixNow");
        secureText.addClassName("cart-secure-text");

        card.add(title, rows, couponRow, discounts, totalRow, continueButton, secureText);
        return card;
    }

    private Div createCouponRow() {
        Div wrapper = new Div();
        wrapper.addClassName("cart-coupon-row");

        TextField couponField = new TextField();
        couponField.setPlaceholder("קוד קופון");
        couponField.setValue(currentCouponCode == null ? "" : currentCouponCode);
        couponField.addClassName("cart-coupon-field");

        Button applyButton = new Button("הפעל");
        applyButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        applyButton.addClassName("cart-coupon-button");

        applyButton.addClickListener(event -> {
            currentCouponCode = couponField.getValue() == null ? "" : couponField.getValue().trim();
            applyCoupon();
        });

        wrapper.add(couponField, applyButton);
        return wrapper;
    }

    private Div createDiscountsBlock() {
        Div block = new Div();
        block.addClassName("cart-discounts-block");

        if (!pricing.appliedDiscounts().isEmpty()) {
            Span title = new Span("הנחות שהופעלו");
            title.addClassName("cart-discounts-title");
            block.add(title);

            for (AppliedDiscount discount : pricing.appliedDiscounts()) {
                Div row = new Div();
                row.addClassName("cart-discount-row");

                Span text = new Span(discount.name() + " · " + discount.description());
                Span amount = new Span("-" + formatMoney(discount.amount()));

                row.add(text, amount);
                block.add(row);
            }
        }

        if (!pricing.policyMessages().isEmpty()) {
            Div messages = new Div();
            messages.addClassName("cart-policy-messages");

            for (String message : pricing.policyMessages()) {
                Span policyMessage = new Span(message);
                policyMessage.addClassName("cart-policy-message");
                messages.add(policyMessage);
            }

            block.add(messages);
        }

        return block;
    }

    private Div priceRow(String label, BigDecimal value) {
        Div row = new Div();
        row.addClassName("cart-price-row");

        Span labelElement = new Span(label);
        Span valueElement = new Span(formatMoney(value));

        row.add(labelElement, valueElement);
        return row;
    }

    private void applyCoupon() {
        if (activeOrder == null) {
            return;
        }

        try {
            pricing = presenter.applyCoupon(activeOrder.getOrderId(), currentCouponCode);
            renderCart();
        } catch (Exception exception) {
            showError(exception.getMessage());
        }
    }

    private void removeTicket(TicketDTO ticket) {
        if (activeOrder == null || ticket == null) {
            return;
        }

        try {
            presenter.removeTicket(activeOrder.getOrderId(), ticket.getTicketId());
            loadCart();
        } catch (Exception exception) {
            showError(exception.getMessage());
        }
    }

    private void continueToCheckout() {
        try {
            presenter.continueToCheckout(activeOrder.getOrderId());
            UI.getCurrent().navigate(UiRoutes.CHECKOUT);
        } catch (Exception exception) {
            showError(exception.getMessage());
        }
    }

    private void renderEmptyCart() {
        removeAll();

        Button searchEvents = new Button("חיפוש אירועים");
        searchEvents.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        searchEvents.addClickListener(event -> UI.getCurrent().navigate(UiRoutes.EVENT_SEARCH));

        EmptyState emptyState = new EmptyState(
                "🛒",
                "העגלה שלך ריקה",
                "לא קיימת הזמנה פעילה כרגע. לאחר בחירת כרטיסים, הם יוצגו כאן עד לסיום התשלום או פקיעת השריון.",
                searchEvents
        );

        Div shell = new Div(emptyState);
        shell.addClassName("cart-shell");
        shell.addClassName("cart-empty-shell");

        add(shell);
    }

    private List<TicketDTO> tickets() {
        if (activeOrder == null || activeOrder.getTickets() == null) {
            return List.of();
        }

        return activeOrder.getTickets();
    }

    private String seatText(TicketDTO ticket) {
        if (ticket.getRow() <= 0 || ticket.getChair() <= 0) {
            return "כרטיס כניסה כללי";
        }

        return "שורה " + ticket.getRow() + ", כיסא " + ticket.getChair();
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }

        BigDecimal normalized = amount.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();

        if (normalized.scale() <= 0) {
            return "₪" + normalized.toPlainString();
        }

        return "₪" + normalized.toPlainString();
    }

    private void showError(String message) {
        Notification notification = Notification.show(
                message == null || message.isBlank() ? "הפעולה נכשלה" : message,
                3500,
                Notification.Position.TOP_CENTER
        );
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    public interface Presenter {
        ActiveOrderDTO loadActiveOrder(String sessionToken);

        CartEventInfo loadEventInfo(Long eventId);

        CartPricing calculatePricing(Long orderId, String couponCode);

        CartPricing applyCoupon(Long orderId, String couponCode);

        void removeTicket(Long orderId, Long ticketId);

        void continueToCheckout(Long orderId);
    }

    public record CartEventInfo(
            String eventName,
            String dateText,
            String locationText
    ) {
    }

    public record AppliedDiscount(
            String name,
            String description,
            BigDecimal amount
    ) {
    }

    public record CartPricing(
            BigDecimal subtotal,
            BigDecimal discountTotal,
            BigDecimal total,
            List<AppliedDiscount> appliedDiscounts,
            List<String> policyMessages
    ) {
    }

    private static final class DemoActiveOrderCartPresenter implements Presenter {

        private ActiveOrderDTO order = new ActiveOrderDTO(
                101L,
                501L,
                9001L,
                new ArrayList<>(List.of(
                        new TicketDTO(1L, 9001L, 4, 12, new BigDecimal("350")),
                        new TicketDTO(2L, 9001L, 4, 13, new BigDecimal("350"))
                ))
        );

        @Override
        public ActiveOrderDTO loadActiveOrder(String sessionToken) {
            return order;
        }

        @Override
        public CartEventInfo loadEventInfo(Long eventId) {
            return new CartEventInfo(
                    "פסטיבל אלקטרוניקה 2026",
                    "15 אוגוסט, 21:00",
                    "גני התערוכה, תל אביב"
            );
        }

        @Override
        public CartPricing calculatePricing(Long orderId, String couponCode) {
            BigDecimal subtotal = order.getTickets().stream()
                    .map(TicketDTO::getPrice)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            List<AppliedDiscount> discounts = new ArrayList<>();
            List<String> messages = new ArrayList<>();

            messages.add("המחיר הסופי מחושב לפי מדיניות הרכישה וההנחות לפני המעבר לתשלום.");

            BigDecimal discountTotal = BigDecimal.ZERO;

            if ("EARLY10".equalsIgnoreCase(couponCode)) {
                discountTotal = subtotal.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
                discounts.add(new AppliedDiscount(
                        "קופון EARLY10",
                        "10% הנחה לפי מדיניות הקופונים",
                        discountTotal
                ));
            }

            BigDecimal total = subtotal.subtract(discountTotal);

            return new CartPricing(
                    subtotal,
                    discountTotal,
                    total,
                    discounts,
                    messages
            );
        }

        @Override
        public CartPricing applyCoupon(Long orderId, String couponCode) {
            return calculatePricing(orderId, couponCode);
        }

        @Override
        public void removeTicket(Long orderId, Long ticketId) {
            List<TicketDTO> remaining = order.getTickets().stream()
                    .filter(ticket -> !Objects.equals(ticket.getTicketId(), ticketId))
                    .toList();

            order = new ActiveOrderDTO(
                    order.getOrderId(),
                    order.getUserId(),
                    order.getEventId(),
                    new ArrayList<>(remaining)
            );
        }

        @Override
        public void continueToCheckout(Long orderId) {
            // Later: call the real presenter/service validation before navigation.
        }
    }
}