package ticketsystem.PresentationLayer.Views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import ticketsystem.DTO.ActiveOrderDTO;
import ticketsystem.DTO.PaymentDetails;
import ticketsystem.DTO.TicketDTO;
import ticketsystem.PresentationLayer.Components.EmptyState;
import ticketsystem.PresentationLayer.Constants.Photos;
import ticketsystem.PresentationLayer.Constants.UiRoutes;
import ticketsystem.PresentationLayer.Session.UiSession;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Route(value = UiRoutes.CHECKOUT)
public class Checkout extends VerticalLayout {

    private final Presenter presenter;

    private ActiveOrderDTO activeOrder;
    private CheckoutEventInfo eventInfo;
    private CheckoutPricing pricing;

    private int currentStep = 1;
    private boolean personalDetailsLoadedFromProfile = false;
    private String selectedPaymentMethod = "credit_card";

    private final TextField fullName = new TextField("שם מלא *");
    private final EmailField email = new EmailField("דואר אלקטרוני *");
    private final TextField phone = new TextField("מספר טלפון *");

    private final TextField payerName = new TextField("שם בעל הכרטיס *");
    private final TextField cardNumber = new TextField("מספר כרטיס *");
    private final TextField expiry = new TextField("תוקף *");
    private final PasswordField cvv = new PasswordField("CVV *");

    public Checkout() {
        this(new DemoCheckoutPresenter());
    }

    public Checkout(Presenter presenter) {
        this.presenter = presenter;

        getElement().setAttribute("dir", "rtl");
        addClassName("checkout-page");
        setPadding(false);
        setSpacing(false);
        setWidthFull();

        configureFields();
        loadCheckout();
    }

    private void configureFields() {
        fullName.setPlaceholder("ישראל ישראלי");
        fullName.addClassName("checkout-field");

        email.setPlaceholder("example@mail.com");
        email.getElement().setAttribute("dir", "ltr");
        email.addClassName("checkout-field");

        phone.setPlaceholder("050-1234567");
        phone.getElement().setAttribute("dir", "ltr");
        phone.addClassName("checkout-field");

        payerName.setPlaceholder("ישראל ישראלי");
        payerName.addClassName("checkout-field");

        cardNumber.setPlaceholder("0000 0000 0000 0000");
        cardNumber.getElement().setAttribute("dir", "ltr");
        cardNumber.setMaxLength(19);
        cardNumber.addClassName("checkout-field");

        expiry.setPlaceholder("MM/YY");
        expiry.getElement().setAttribute("dir", "ltr");
        expiry.setMaxLength(5);
        expiry.addClassName("checkout-field");

        cvv.setPlaceholder("123");
        cvv.getElement().setAttribute("dir", "ltr");
        cvv.setMaxLength(4);
        cvv.addClassName("checkout-field");
    }

    private void loadCheckout() {
        try {
            String token = resolveSessionToken();

            activeOrder = presenter.loadActiveOrder(token);

            if (activeOrder == null || tickets().isEmpty()) {
                renderEmptyCheckout();
                return;
            }

            eventInfo = presenter.loadEventInfo(activeOrder.getEventId());
            pricing = presenter.calculatePricing(activeOrder.getOrderId());

            prefillBuyerDetailsIfLoggedIn(token);
            renderCheckout();

        } catch (Exception exception) {
            showError(exception.getMessage());
            renderEmptyCheckout();
        }
    }

    private String resolveSessionToken() {
        return UiSession.getMemberToken();
    }

    private void prefillBuyerDetailsIfLoggedIn(String token) {
        if (!UiSession.isLoggedIn()) {
            return;
        }

        CheckoutBuyerDetails details = presenter.loadBuyerDetails(token);

        if (details == null) {
            return;
        }

        if (!isBlank(details.fullName())) {
            fullName.setValue(details.fullName());
        }

        if (!isBlank(details.email())) {
            email.setValue(details.email());
        }

        if (!isBlank(details.phone())) {
            phone.setValue(details.phone());
        }

        personalDetailsLoadedFromProfile = true;
    }

    private void renderCheckout() {
        removeAll();

        Div page = new Div();
        page.addClassName("checkout-shell");

        page.add(
                createMinimalHeader(),
                createMainContent()
        );

        add(page);
    }

    private Div createMinimalHeader() {
        Div header = new Div();
        header.addClassName("checkout-header");

        Span brand = new Span("TixNow");
        brand.addClassName("brand-logo");
        brand.addClickListener(event -> UI.getCurrent().navigate(UiRoutes.HOME));

        Button cancel = new Button("ביטול", VaadinIcon.CLOSE.create());
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        cancel.addClassName("checkout-cancel-button");
        cancel.addClickListener(event -> UI.getCurrent().navigate(UiRoutes.ACTIVE_ORDER_CART));

        header.add(brand, cancel);
        return header;
    }

    private Div createMainContent() {
        Div main = new Div();
        main.addClassName("checkout-main");

        Div formArea = new Div();
        formArea.addClassName("checkout-form-area");
        formArea.add(createStepIndicator());

        if (currentStep == 1) {
            formArea.add(createPersonalDetailsCard());
        } else {
            formArea.add(createPaymentDetailsCard());
        }

        main.add(formArea, createSummaryCard());
        return main;
    }

    private Div createStepIndicator() {
        Div wrapper = new Div();
        wrapper.addClassName("checkout-stepper");

        Div line = new Div();
        line.addClassName("checkout-stepper-line");

        Div fill = new Div();
        fill.addClassName("checkout-stepper-fill");
        fill.addClassName(currentStep == 1 ? "checkout-stepper-fill-half" : "checkout-stepper-fill-full");

        wrapper.add(line, fill);

        wrapper.add(
                createStep("1", "פרטים אישיים", currentStep >= 1),
                createStep("2", "תשלום", currentStep >= 2)
        );

        return wrapper;
    }

    private Div createStep(String number, String label, boolean active) {
        Div step = new Div();
        step.addClassName("checkout-step");

        Span circle = new Span(number);
        circle.addClassName("checkout-step-circle");

        Span labelElement = new Span(label);
        labelElement.addClassName("checkout-step-label");

        if (active) {
            circle.addClassName("checkout-step-circle-active");
            labelElement.addClassName("checkout-step-label-active");
        }

        step.add(circle, labelElement);
        return step;
    }

    private Div createPersonalDetailsCard() {
        Div card = new Div();
        card.addClassName("app-card");
        card.addClassName("checkout-card");

        H1 title = new H1("פרטים אישיים");
        title.addClassName("checkout-title");

        Paragraph subtitle = new Paragraph("אנא מלא את פרטיך כדי להמשיך ברכישה. הכרטיסים יישלחו לכתובת הדוא\"ל שתזין כאן.");
        subtitle.addClassName("checkout-subtitle");

        card.add(title, subtitle);

        if (personalDetailsLoadedFromProfile) {
            Span notice = iconText(VaadinIcon.CHECK, "הפרטים מולאו אוטומטית מהחשבון המחובר. ניתן לערוך אותם לפני התשלום.");
            notice.addClassName("checkout-profile-notice");
            card.add(notice);
        }

        Div form = new Div();
        form.addClassName("checkout-personal-form");

        Div fullNameRow = new Div(fullName);
        fullNameRow.addClassName("checkout-field-row");

        Div contactGrid = new Div();
        contactGrid.addClassName("checkout-two-columns");
        contactGrid.add(email, phone);

        form.add(fullNameRow, contactGrid);

        Button continueButton = new Button("המשך לתשלום", VaadinIcon.ARROW_BACKWARD.create());
        continueButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        continueButton.addClassName("checkout-primary-action");
        continueButton.addClickListener(event -> continueToPayment());

        Div actions = new Div(continueButton);
        actions.addClassName("checkout-actions");

        card.add(form, actions);
        return card;
    }

    private Div createPaymentDetailsCard() {
        Div card = new Div();
        card.addClassName("app-card");
        card.addClassName("checkout-card");

        H1 title = new H1("אמצעי תשלום");
        title.addClassName("checkout-title");

        Paragraph subtitle = new Paragraph("בחר אמצעי תשלום והשלם את הרכישה. פרטי הכרטיס אינם נשמרים במערכת.");
        subtitle.addClassName("checkout-subtitle");

        Div methods = new Div();
        methods.addClassName("checkout-payment-methods");
        methods.add(
                createPaymentMethodCard("credit_card", "כרטיס אשראי", "תשלום מאובטח בכרטיס אשראי", "💳"),
                createPaymentMethodCard("paypal", "PayPal", "תשתית עתידית לחיבור PayPal", "P"),
                createPaymentMethodCard("apple_pay", "Apple Pay", "תשתית עתידית לחיבור Apple Pay", "")
        );

        card.add(title, subtitle, methods);

        if ("credit_card".equals(selectedPaymentMethod)) {
            card.add(createCreditCardForm());
        } else {
            Span externalPaymentNotice = iconText(
                    VaadinIcon.INFO_CIRCLE,
                    "כרגע זהו placeholder. בהמשך ה־Presenter יחבר את הבחירה לשירות התשלום המתאים."
            );
            externalPaymentNotice.addClassName("checkout-policy-message");
            card.add(externalPaymentNotice);
        }

        Div actions = new Div();
        actions.addClassName("checkout-payment-actions");

        Button back = new Button("חזרה לפרטים", VaadinIcon.ARROW_FORWARD.create());
        back.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        back.addClassName("checkout-secondary-action");
        back.addClickListener(event -> {
            currentStep = 1;
            renderCheckout();
        });

        Button pay = new Button("בצע תשלום", VaadinIcon.LOCK.create());
        pay.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        pay.addClassName("checkout-primary-action");
        pay.addClickListener(event -> submitPayment());

        actions.add(back, pay);
        card.add(actions);

        return card;
    }

    private Div createPaymentMethodCard(String id, String title, String subtitle, String symbol) {
        Div card = new Div();
        card.addClassName("checkout-payment-method-card");

        if (Objects.equals(selectedPaymentMethod, id)) {
            card.addClassName("checkout-payment-method-card-selected");
        }

        Span selector = new Span();
        selector.addClassName("checkout-payment-selector");

        Span icon = new Span(symbol);
        icon.addClassName("checkout-payment-symbol");

        Div text = new Div();
        text.addClassName("checkout-payment-method-text");

        Span titleElement = new Span(title);
        titleElement.addClassName("checkout-payment-method-title");

        Span subtitleElement = new Span(subtitle);
        subtitleElement.addClassName("checkout-payment-method-subtitle");

        text.add(titleElement, subtitleElement);
        card.add(selector, text, icon);

        card.addClickListener(event -> {
            selectedPaymentMethod = id;
            renderCheckout();
        });

        return card;
    }

    private Div createCreditCardForm() {
        Div wrapper = new Div();
        wrapper.addClassName("checkout-credit-card-form");

        Div separator = new Div();
        separator.addClassName("checkout-separator");

        Div cardNumberRow = new Div(cardNumber);
        cardNumberRow.addClassName("checkout-field-row");

        Div grid = new Div();
        grid.addClassName("checkout-two-columns");
        grid.add(expiry, cvv);

        Div payerRow = new Div(payerName);
        payerRow.addClassName("checkout-field-row");

        if (isBlank(payerName.getValue()) && !isBlank(fullName.getValue())) {
            payerName.setValue(fullName.getValue());
        }

        wrapper.add(separator, cardNumberRow, grid, payerRow);
        return wrapper;
    }

    private Div createSummaryCard() {
        Div card = new Div();
        card.addClassName("app-card");
        card.addClassName("checkout-summary-card");

        H2 title = new H2("סיכום הזמנה");
        title.addClassName("checkout-summary-title");

        Div eventBlock = new Div();
        eventBlock.addClassName("checkout-summary-event");

        Image image = new Image(Photos.EVENT_LIGHTS, "תמונת אירוע");
        image.addClassName("checkout-summary-image");

        Div eventText = new Div();
        eventText.addClassName("checkout-summary-event-text");

        H3 eventName = new H3(eventInfo == null ? "אירוע" : eventInfo.eventName());
        eventName.addClassName("checkout-summary-event-name");

        Span date = iconText(VaadinIcon.CALENDAR, eventInfo == null ? "" : eventInfo.dateText());
        Span location = iconText(VaadinIcon.MAP_MARKER, eventInfo == null ? "" : eventInfo.locationText());

        eventText.add(eventName, date, location);
        eventBlock.add(image, eventText);

        Div ticketsBlock = new Div();
        ticketsBlock.addClassName("checkout-summary-tickets");

        for (TicketDTO ticket : tickets()) {
            ticketsBlock.add(createTicketSummaryRow(ticket));
        }

        Div prices = new Div();
        prices.addClassName("checkout-summary-prices");

        prices.add(priceRow("סכום ביניים (" + tickets().size() + " כרטיסים)", pricing.subtotal()));

        if (pricing.discountTotal().compareTo(BigDecimal.ZERO) > 0) {
            prices.add(priceRow("הנחות", pricing.discountTotal().negate()));
        }

        if (!pricing.appliedDiscounts().isEmpty()) {
            Div discounts = new Div();
            discounts.addClassName("checkout-discounts");

            Span discountsTitle = new Span("הנחות שהופעלו");
            discountsTitle.addClassName("checkout-discounts-title");
            discounts.add(discountsTitle);

            for (AppliedDiscount discount : pricing.appliedDiscounts()) {
                Div row = new Div();
                row.addClassName("checkout-discount-row");
                row.add(new Span(discount.name() + " · " + discount.description()), new Span("-" + formatMoney(discount.amount())));
                discounts.add(row);
            }

            prices.add(discounts);
        }

        Div total = new Div();
        total.addClassName("checkout-total-row");
        total.add(new Span("סך הכל"), new Span(formatMoney(pricing.total())));

        Div policyMessages = new Div();
        policyMessages.addClassName("checkout-policy-messages");

        for (String message : pricing.policyMessages()) {
            Span messageElement = iconText(VaadinIcon.INFO_CIRCLE, message);
            messageElement.addClassName("checkout-policy-message");
            policyMessages.add(messageElement);
        }

        Span secureText = iconText(VaadinIcon.LOCK, "תשלום מאובטח באמצעות TixNow");
        secureText.addClassName("checkout-secure-text");

        card.add(title, eventBlock, ticketsBlock, prices, total, policyMessages, secureText);
        return card;
    }

    private Div createTicketSummaryRow(TicketDTO ticket) {
        Div row = new Div();
        row.addClassName("checkout-ticket-row");

        Div text = new Div();
        text.addClassName("checkout-ticket-text");

        Span title = new Span("כרטיס #" + ticket.getTicketId());
        title.addClassName("checkout-ticket-title");

        Span seat = new Span(seatText(ticket));
        seat.addClassName("checkout-ticket-seat");

        text.add(title, seat);

        Span price = new Span(formatMoney(ticket.getPrice()));
        price.addClassName("checkout-ticket-price");

        row.add(text, price);
        return row;
    }

    private Div priceRow(String label, BigDecimal value) {
        Div row = new Div();
        row.addClassName("checkout-price-row");
        row.add(new Span(label), new Span(formatMoney(value)));
        return row;
    }

    private void continueToPayment() {
        if (!validatePersonalDetails()) {
            return;
        }

        if (isBlank(payerName.getValue())) {
            payerName.setValue(fullName.getValue());
        }

        currentStep = 2;
        renderCheckout();
    }

    private void submitPayment() {
        if (!validatePersonalDetails() || !validatePaymentDetails()) {
            return;
        }

        try {
            PaymentDetails details = new PaymentDetails(resolvePaymentMethodId(), payerName.getValue().trim(), LocalDate.now() );

            boolean success = presenter.checkout(
                    resolveSessionToken(),
                    activeOrder.getEventId(),
                    details,
                    collectBuyerDetails()
            );

            if (success) {
                showSuccess("הרכישה הושלמה בהצלחה");
                UI.getCurrent().navigate(UiRoutes.MY_ACCOUNT);
            } else {
                showError("התשלום לא הושלם");
            }

        } catch (Exception exception) {
            showError(exception.getMessage());
        }
    }

    private CheckoutBuyerDetails collectBuyerDetails() {
        return new CheckoutBuyerDetails(
                fullName.getValue().trim(),
                email.getValue().trim(),
                phone.getValue().trim(),
                personalDetailsLoadedFromProfile
        );
    }

    private boolean validatePersonalDetails() {
        resetPersonalValidation();

        boolean valid = true;

        if (isBlank(fullName.getValue())) {
            fullName.setInvalid(true);
            fullName.setErrorMessage("יש להזין שם מלא");
            valid = false;
        }

        if (isBlank(email.getValue()) || !email.getValue().contains("@")) {
            email.setInvalid(true);
            email.setErrorMessage("יש להזין כתובת דוא\"ל תקינה");
            valid = false;
        }

        if (isBlank(phone.getValue())) {
            phone.setInvalid(true);
            phone.setErrorMessage("יש להזין מספר טלפון");
            valid = false;
        }

        if (!valid) {
            showError("יש להשלים את הפרטים האישיים");
        }

        return valid;
    }

    private boolean validatePaymentDetails() {
        resetPaymentValidation();

        if (!"credit_card".equals(selectedPaymentMethod)) {
            return true;
        }

        boolean valid = true;

        String digits = onlyDigits(cardNumber.getValue());

        if (digits.length() < 12) {
            cardNumber.setInvalid(true);
            cardNumber.setErrorMessage("מספר הכרטיס קצר מדי");
            valid = false;
        }

        if (isBlank(expiry.getValue()) || !expiry.getValue().contains("/")) {
            expiry.setInvalid(true);
            expiry.setErrorMessage("יש להזין תוקף בפורמט MM/YY");
            valid = false;
        }

        if (onlyDigits(cvv.getValue()).length() < 3) {
            cvv.setInvalid(true);
            cvv.setErrorMessage("יש להזין CVV תקין");
            valid = false;
        }

        if (isBlank(payerName.getValue())) {
            payerName.setInvalid(true);
            payerName.setErrorMessage("יש להזין שם בעל הכרטיס");
            valid = false;
        }

        if (!valid) {
            showError("יש להשלים את פרטי התשלום");
        }

        return valid;
    }

    private void resetPersonalValidation() {
        fullName.setInvalid(false);
        email.setInvalid(false);
        phone.setInvalid(false);
    }

    private void resetPaymentValidation() {
        cardNumber.setInvalid(false);
        expiry.setInvalid(false);
        cvv.setInvalid(false);
        payerName.setInvalid(false);
    }

    private String resolvePaymentMethodId() {
        if (!"credit_card".equals(selectedPaymentMethod)) {
            return selectedPaymentMethod;
        }

        String digits = onlyDigits(cardNumber.getValue());
        String lastFour = digits.length() <= 4 ? digits : digits.substring(digits.length() - 4);

        return "fake-credit-card-" + lastFour;
    }

    private void renderEmptyCheckout() {
        removeAll();

        Div page = new Div();
        page.addClassName("checkout-shell");

        Button goToCart = new Button("חזרה לעגלה");
        goToCart.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        goToCart.addClickListener(event -> UI.getCurrent().navigate(UiRoutes.ACTIVE_ORDER_CART));

        EmptyState emptyState = new EmptyState(
                "🧾",
                "אין הזמנה פעילה לתשלום",
                "כדי להשלים רכישה יש לבחור כרטיסים ולהגיע לעמוד התשלום מתוך עגלת הקניות.",
                goToCart
        );

        Div emptyWrapper = new Div(emptyState);
        emptyWrapper.addClassName("checkout-empty-wrapper");

        page.add(createMinimalHeader(), emptyWrapper);
        add(page);
    }

    private Span iconText(VaadinIcon icon, String text) {
        Span row = new Span();
        row.addClassName("checkout-icon-text");
        row.add(icon.create(), new Span(isBlank(text) ? "פרטים יעודכנו בהמשך" : text));
        return row;
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

    private String onlyDigits(String value) {
        if (value == null) {
            return "";
        }

        return value.replaceAll("\\D", "");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void showError(String message) {
        Notification notification = Notification.show(
                isBlank(message) ? "הפעולה נכשלה" : message,
                3500,
                Notification.Position.TOP_CENTER
        );
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void showSuccess(String message) {
        Notification notification = Notification.show(
                isBlank(message) ? "הפעולה הושלמה בהצלחה" : message,
                3500,
                Notification.Position.TOP_CENTER
        );
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    public interface Presenter {
        ActiveOrderDTO loadActiveOrder(String sessionToken);

        CheckoutEventInfo loadEventInfo(Long eventId);

        CheckoutPricing calculatePricing(Long orderId);

        CheckoutBuyerDetails loadBuyerDetails(String sessionToken);

        boolean checkout(
                String sessionToken,
                Long eventId,
                PaymentDetails paymentDetails,
                CheckoutBuyerDetails buyerDetails
        );
    }

    public record CheckoutEventInfo(
            String eventName,
            String dateText,
            String locationText
    ) {
    }

    public record CheckoutBuyerDetails(
            String fullName,
            String email,
            String phone,
            boolean loadedFromProfile
    ) {
    }

    public record AppliedDiscount(
            String name,
            String description,
            BigDecimal amount
    ) {
    }

    public record CheckoutPricing(
            BigDecimal subtotal,
            BigDecimal discountTotal,
            BigDecimal total,
            List<AppliedDiscount> appliedDiscounts,
            List<String> policyMessages
    ) {
    }

    private static final class DemoCheckoutPresenter implements Presenter {

        private final ActiveOrderDTO order = new ActiveOrderDTO(
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
        public CheckoutEventInfo loadEventInfo(Long eventId) {
            return new CheckoutEventInfo(
                    "פסטיבל אלקטרוניקה 2026",
                    "15 אוגוסט, 21:00",
                    "גני התערוכה, תל אביב"
            );
        }

        @Override
        public CheckoutPricing calculatePricing(Long orderId) {
            BigDecimal subtotal = order.getTickets().stream()
                    .map(TicketDTO::getPrice)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal discount = subtotal.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);

            return new CheckoutPricing(
                    subtotal,
                    discount,
                    subtotal.subtract(discount),
                    List.of(new AppliedDiscount(
                            "קופון EARLY10",
                            "10% הנחה לפי מדיניות הקופונים",
                            discount
                    )),
                    List.of("המחיר הסופי חושב לפי מדיניות הרכישה וההנחות לפני ביצוע התשלום.")
            );
        }

        @Override
        public CheckoutBuyerDetails loadBuyerDetails(String sessionToken) {
            if (sessionToken == null || sessionToken.isBlank()) {
                return new CheckoutBuyerDetails("", "", "", false);
            }

            return new CheckoutBuyerDetails(
                    "שם של הלקוח",
                    "name@example.com",
                    "050-1234567",
                    true
            );
        }

        @Override
        public boolean checkout(
                String sessionToken,
                Long eventId,
                PaymentDetails paymentDetails,
                CheckoutBuyerDetails buyerDetails
        ) {
            // Later:
            // return reservationService.checkout(sessionToken, eventId, paymentDetails);
            return true;
        }
    }
}