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
import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.Route;
import ticketsystem.DTO.ActiveOrderDTO;
import ticketsystem.DTO.TicketDTO;
import ticketsystem.PresentationLayer.Session.UiVisitCoordinator;
import ticketsystem.PresentationLayer.Components.EmptyState;
import ticketsystem.PresentationLayer.Components.ReservationTimer;
import ticketsystem.PresentationLayer.Constants.UiRoutes;
import ticketsystem.PresentationLayer.Layouts.PublicLayout;
import ticketsystem.PresentationLayer.Session.UiSession;
import ticketsystem.PresentationLayer.Presenters.ReservationPresenter;
import ticketsystem.PresentationLayer.DTO.AppliedDiscount;
import ticketsystem.PresentationLayer.DTO.OrderEventInfo;
import ticketsystem.PresentationLayer.DTO.OrderPricing;
import ticketsystem.DomainLayer.discount.DiscountKind;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;


@Route(value = UiRoutes.ACTIVE_ORDER_CART, layout = PublicLayout.class)
public class ActiveOrderCart extends VerticalLayout {

    private final UiVisitCoordinator visitCoordinator;
    private final ReservationPresenter presenter;

    private ActiveOrderDTO activeOrder;
    private OrderEventInfo eventInfo;
    private OrderPricing pricing;
    private String currentCouponCode = "";
    private final ReservationTimer reservationTimer = new ReservationTimer();



    public ActiveOrderCart(ReservationPresenter presenter, UiVisitCoordinator visitCoordinator) {
        this.presenter = presenter;
        this.visitCoordinator = visitCoordinator;

        getElement().setAttribute("dir", "rtl");
        addClassName("active-order-cart-page");
        setPadding(false);
        setSpacing(false);
        setWidthFull();

        this.visitCoordinator.ensureVisitAndNotifications(UI.getCurrent());
        loadCart();
    }

    /**
     * Loads the current active order and its pricing information.
     *
     * <p>When a coupon was previously confirmed for this order, the coupon is
     * restored from the UI session and the pricing is recalculated. A stored
     * coupon that no longer affects the order is removed automatically.</p>
     */
    private void loadCart() {
        try {
            activeOrder = presenter.loadActiveOrder(resolveSessionToken());

            if (activeOrder == null || tickets().isEmpty()) {
                if (activeOrder != null) {
                    UiSession.clearCouponCode(activeOrder.getOrderId());
                }

                ReservationTimer.clear();
                renderEmptyCart();
                return;
            }

            eventInfo = loadEventInfo(activeOrder.getEventId());
            loadPricingWithStoredCoupon();
            reservationTimer.setDeadline(activeOrder.getExpiresAtEpochMillis());

            renderCart();
        } catch (Exception exception) {
            showError(exception.getMessage());
            renderEmptyCart();
        }
    }

    private String resolveSessionToken() {
        return UiSession.getCurrentToken();
    }

    private void renderCart() {
        removeAll();

        Div shell = new Div();
        shell.addClassName("cart-shell");

        shell.add(reservationTimer, createHeader(), createLayout());

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

        Div couponRow = createCouponRow();
        Div discounts = createDiscountsBlock();

        Div discountTotalRows = null;

        if (pricing.discountTotal().compareTo(BigDecimal.ZERO) > 0) {
            discountTotalRows = new Div();
            discountTotalRows.addClassName("cart-summary-rows");
            discountTotalRows.add(priceRow("סך הנחות", pricing.discountTotal().negate()));
        }

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

        card.add(title, rows, couponRow, discounts);

        if (discountTotalRows != null) {
            card.add(discountTotalRows);
        }

        card.add(totalRow, continueButton, secureText);
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
            String enteredCouponCode = couponField.getValue() == null
                    ? ""
                    : couponField.getValue().trim();

            applyCoupon(enteredCouponCode);
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
                Span amount = new Span(formatMoney(discount.amount().negate()));

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

    /**
     * Applies the coupon code entered by the user to the current pricing preview.
     *
     * <p>A non-blank code is stored in the UI session only when the resulting
     * pricing contains an applied coupon discount. An invalid code does not
     * replace a previously confirmed coupon. A blank value removes the stored
     * coupon from the current order.</p>
     *
     * @param enteredCouponCode coupon code entered in the cart
     */
    private void applyCoupon(String enteredCouponCode) {
        if (activeOrder == null) {
            return;
        }

        String normalizedCouponCode = enteredCouponCode == null
                ? ""
                : enteredCouponCode.trim();

        try {
            if (normalizedCouponCode.isBlank()) {
                OrderPricing pricingWithoutCoupon = presenter.calculatePricing(
                        resolveSessionToken(),
                        activeOrder,
                        ""
                );

                currentCouponCode = "";
                pricing = pricingWithoutCoupon;
                UiSession.clearCouponCode(activeOrder.getOrderId());

                renderCart();
                return;
            }

            OrderPricing candidatePricing = presenter.applyCoupon(
                    resolveSessionToken(),
                    activeOrder,
                    normalizedCouponCode
            );

            if (hasAppliedCoupon(candidatePricing)) {
                currentCouponCode = normalizedCouponCode;
                pricing = candidatePricing;

                UiSession.setCouponCode(
                        activeOrder.getOrderId(),
                        currentCouponCode
                );

                renderCart();
                return;
            }

            loadPricingWithStoredCoupon();
            renderCart();
            showError("קוד הקופון לא הפעיל הנחה עבור ההזמנה הנוכחית");

        } catch (Exception exception) {
            String storedCouponCode = UiSession.getCouponCode(
                    activeOrder.getOrderId()
            );

            currentCouponCode = storedCouponCode == null
                    ? ""
                    : storedCouponCode;

            renderCart();
            showError(exception.getMessage());
        }
    }

    /**
     * Loads pricing using the coupon code previously stored for the active order.
     *
     * <p>If the stored code no longer produces an applied coupon discount, it is
     * removed and the order is recalculated without a coupon.</p>
     */
    private void loadPricingWithStoredCoupon() {
        String storedCouponCode = UiSession.getCouponCode(activeOrder.getOrderId());

        currentCouponCode = storedCouponCode == null
                ? ""
                : storedCouponCode;

        pricing = presenter.calculatePricing(
                resolveSessionToken(),
                activeOrder,
                currentCouponCode
        );

        if (!currentCouponCode.isBlank() && !hasAppliedCoupon(pricing)) {
            UiSession.clearCouponCode(activeOrder.getOrderId());
            currentCouponCode = "";

            pricing = presenter.calculatePricing(
                    resolveSessionToken(),
                    activeOrder,
                    currentCouponCode
            );
        }
    }

    /**
     * Checks whether the calculated pricing contains a coupon discount that
     * actually affected the final price.
     *
     * @param calculatedPricing pricing result returned by the presenter
     * @return {@code true} when a coupon discount was applied
     */
    private boolean hasAppliedCoupon(OrderPricing calculatedPricing) {
        return calculatedPricing != null
                && calculatedPricing.appliedDiscounts() != null
                && calculatedPricing.appliedDiscounts().stream()
                .anyMatch(discount ->
                        discount.kind() == DiscountKind.COUPON
                );
    }

    private void removeTicket(TicketDTO ticket) {
        if (activeOrder == null || ticket == null) {
            return;
        }

        try {
            presenter.removeTicketFromActiveOrder(resolveSessionToken(), activeOrder.getEventId(), ticket.getTicketId());
            loadCart();
            refreshHeader();
        } catch (Exception exception) {
            showError(exception.getMessage());
        }
    }

    private void refreshHeader() {
        getParent()
                .flatMap(Component::getParent)
                .filter(PublicLayout.class::isInstance)
                .map(PublicLayout.class::cast)
                .ifPresent(PublicLayout::refreshHeader);
    }

    private void continueToCheckout() {
        if (activeOrder == null || activeOrder.getEventId() == null) {
            showError("לא ניתן להמשיך לתשלום עבור הזמנה לא תקינה");
            return;
        }

        try {
            UI.getCurrent().navigate(UiRoutes.CHECKOUT.replace(":eventId", String.valueOf(activeOrder.getEventId())));
        } catch (Exception exception) {
            showError(exception.getMessage());
        }
    }

    private OrderEventInfo loadEventInfo(Long eventId) {
        return presenter.loadActiveOrderEventInfo(resolveSessionToken(), eventId);
    }


    private void renderEmptyCart() {
        removeAll();
        ReservationTimer.clear();

        Button searchEvents = new Button("חיפוש אירועים");
        searchEvents.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        searchEvents.addClickListener(event -> UI.getCurrent().navigate(UiRoutes.SEARCH_RESULTS));

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
        boolean negative = normalized.signum() < 0;
        BigDecimal absolute = normalized.abs();

        String value = absolute.toPlainString();

        if (negative) {
            return "\u200E- ₪" + value;
        }

        return "₪" + value;
    }

    private void showError(String message) {
        Notification notification = Notification.show(
                message == null || message.isBlank() ? "הפעולה נכשלה" : message,
                3500,
                Notification.Position.TOP_CENTER
        );
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
