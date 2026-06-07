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
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.component.datepicker.DatePicker;
import ticketsystem.DTO.ActiveOrderDTO;
import ticketsystem.DTO.PaymentDetails;
import ticketsystem.DTO.TicketDTO;
import ticketsystem.PresentationLayer.Session.UiVisitCoordinator;
import ticketsystem.PresentationLayer.Components.EmptyState;
import ticketsystem.PresentationLayer.Components.ReservationTimer;
import ticketsystem.PresentationLayer.Constants.Photos;
import ticketsystem.PresentationLayer.Constants.UiRoutes;
import ticketsystem.PresentationLayer.Session.UiSession;
import ticketsystem.PresentationLayer.Presenters.ReservationPresenter;
import ticketsystem.PresentationLayer.DTO.AppliedDiscount;
import ticketsystem.PresentationLayer.DTO.OrderEventInfo;
import ticketsystem.PresentationLayer.DTO.OrderPricing;
import ticketsystem.DTO.MyAccountDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Route(value = UiRoutes.CHECKOUT)
public class Checkout extends VerticalLayout implements BeforeEnterObserver {

    private final UiVisitCoordinator visitCoordinator;
    private final ReservationPresenter presenter;

    private ActiveOrderDTO activeOrder;
    private OrderEventInfo eventInfo;
    private OrderPricing pricing;
    private Long requestedRouteEventId;
    private final ReservationTimer reservationTimer = new ReservationTimer();

    private int currentStep = 1;
    private boolean personalDetailsLoadedFromProfile = false;
    private String selectedPaymentMethod = "credit_card";

    private final TextField fullName = new TextField("שם מלא *");
    private final EmailField email = new EmailField("דואר אלקטרוני *");
    private final TextField phone = new TextField("מספר טלפון *");
    private final DatePicker birthDate = new DatePicker("תאריך לידה *");
    private final TextField couponCode = new TextField("קוד קופון");

    private final TextField payerName = new TextField("שם בעל הכרטיס *");
    private final TextField cardNumber = new TextField("מספר כרטיס *");
    private final TextField expiry = new TextField("תוקף *");
    private final PasswordField cvv = new PasswordField("CVV *");

    public Checkout(ReservationPresenter presenter, UiVisitCoordinator visitCoordinator) {
        this.presenter = presenter;
        this.visitCoordinator = visitCoordinator;

        getElement().setAttribute("dir", "rtl");
        addClassName("checkout-page");
        setPadding(false);
        setSpacing(false);
        setWidthFull();

        configureFields();

        this.visitCoordinator.ensureVisitAndNotifications(UI.getCurrent());
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        requestedRouteEventId = parseRouteEventId(event);
        loadCheckout(event);
    }

    private Long parseRouteEventId(BeforeEnterEvent event) {
        return event.getRouteParameters()
                .get("eventId")
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(this::parseLongOrNull)
                .orElse(null);
    }

    private Long parseLongOrNull(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return null;
        }
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

        birthDate.setPlaceholder("בחר תאריך לידה");
        birthDate.setRequiredIndicatorVisible(true);
        birthDate.setMax(LocalDate.now());
        birthDate.setWidthFull();
        birthDate.addClassName("checkout-field");
        birthDate.setErrorMessage("יש להזין תאריך לידה תקין");

        couponCode.setPlaceholder("לדוגמה: SUMMER26");
        couponCode.getElement().setAttribute("dir", "ltr");
        couponCode.setClearButtonVisible(true);
        couponCode.setWidthFull();
        couponCode.addClassName("checkout-field");

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

    private void loadCheckout(BeforeEnterEvent event) {
        try {
            String token = resolveSessionToken();

            activeOrder = presenter.loadActiveOrder(token);

            if (activeOrder == null || tickets().isEmpty()) {
                ReservationTimer.clear();
                renderEmptyCheckout();
                return;
            }

            if (requestedRouteEventId == null || !Objects.equals(requestedRouteEventId, activeOrder.getEventId())) {
                event.forwardTo(UiRoutes.CHECKOUT.replace(":eventId", String.valueOf(activeOrder.getEventId())));
                return;
            }

            reservationTimer.setDeadline(activeOrder.getExpiresAtEpochMillis());

            eventInfo = presenter.loadActiveOrderEventInfo(token, activeOrder.getEventId());
            pricing = presenter.calculatePricing(activeOrder, normalizedCouponCode());

            prefillBuyerDetailsIfLoggedIn(token);
            renderCheckout();

        } catch (Exception exception) {
            showError(exception.getMessage());
            renderEmptyCheckout();
        }
    }

    private String resolveSessionToken() {
        return UiSession.getCurrentToken();
    }

    /**
     * Prefills buyer contact details for logged-in members.
     *
     * Checkout supports both guest and member purchases. Guest users must enter
     * their buyer details manually, while logged-in members can reuse the profile
     * details stored in the system.
     *
     * The method intentionally does nothing for guest sessions, because guest
     * tokens do not have member profile data and calling the profile loading flow
     * with a guest token would fail.
     *
     * Existing field values are not overwritten. This protects details that the
     * user may have already typed manually before the checkout view is re-rendered.
     *
     * @param token the current UI session token, expected to be a member token
     *              when the user is logged in
     */
    private void prefillBuyerDetailsIfLoggedIn(String token) {
        if (!UiSession.isLoggedIn()) {
            return;
        }

        MyAccountDTO buyer = presenter.loadBuyerDetails(token);
        if (buyer == null) {
            return;
        }

        if (isBlank(fullName.getValue()) && !isBlank(buyer.getFullName())) {
            fullName.setValue(buyer.getFullName());
        }

        if (isBlank(email.getValue()) && !isBlank(buyer.getEmail())) {
            email.setValue(buyer.getEmail());
        }

        if (isBlank(phone.getValue()) && !isBlank(buyer.getPhone())) {
            phone.setValue(buyer.getPhone());
        }

        if (birthDate.getValue() == null && buyer.getBirthDate() != null) {
            birthDate.setValue(buyer.getBirthDate());
        }
    }

    private void renderCheckout() {
        removeAll();

        Div page = new Div();
        page.addClassName("checkout-shell");

        page.add(
                reservationTimer,
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

        Div birthDateRow = new Div(birthDate);
        birthDateRow.addClassName("checkout-field-row");

        form.add(fullNameRow, contactGrid, birthDateRow);

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

        card.add(title, eventBlock, ticketsBlock, createCheckoutCouponSection(), prices, total, policyMessages, secureText);
        return card;
    }

    private Div createCheckoutCouponSection() {
        Div section = new Div();
        section.addClassName("checkout-field-row");

        Button applyCoupon = new Button("הפעל קופון");
        applyCoupon.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        applyCoupon.addClassName("checkout-secondary-action");
        applyCoupon.addClickListener(event -> applyCheckoutCoupon());

        section.add(couponCode, applyCoupon);
        return section;
    }

    private void applyCheckoutCoupon() {
        if (activeOrder == null) {
            return;
        }

        try {
            pricing = presenter.calculatePricing(activeOrder, normalizedCouponCode());
            renderCheckout();

            if (isBlank(normalizedCouponCode())) {
                showSuccess("קוד הקופון נוקה");
            } else if (pricing.discountTotal().compareTo(BigDecimal.ZERO) > 0 || !pricing.appliedDiscounts().isEmpty()) {
                showSuccess("קוד הקופון הופעל");
            } else {
                showSuccess("קוד הקופון נשמר וייבדק בעת התשלום");
            }
        } catch (Exception exception) {
            showError(exception.getMessage());
        }
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
        try{
            PaymentDetails details = new PaymentDetails(
                resolvePaymentMethodId(),
                payerName.getValue().trim(),
                birthDate.getValue()
                );
            presenter.validateOrderPolicyBeforePayment(resolveSessionToken(), activeOrder.getEventId(), details, normalizedCouponCode());
            currentStep = 2;
            renderCheckout();
        }catch (Exception exception) {
            showError(exception.getMessage());
        }

    }

    private void submitPayment() {
        if (!validatePersonalDetails() || !validatePaymentDetails()) {
            return;
        }

        try {
            PaymentDetails details = new PaymentDetails(
                    resolvePaymentMethodId(),
                    payerName.getValue().trim(),
                    birthDate.getValue()
            );

            boolean success = presenter.checkout(
                    resolveSessionToken(),
                    activeOrder.getEventId(),
                    details,
                    pricing.total()

            );

            if (success) {
                ReservationTimer.clear();
                reservationTimer.refreshFromSession();
                showSuccess("הרכישה הושלמה בהצלחה");
                if (UiSession.isLoggedIn()) {
                    UI.getCurrent().navigate(UiRoutes.MY_ACCOUNT);
                } else {
                    UI.getCurrent().navigate(UiRoutes.HOME);
                }
            } else {
                showError("התשלום לא הושלם");
            }

        } catch (Exception exception) {
            showError(exception.getMessage());
        }
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

        if (birthDate.getValue() == null) {
            birthDate.setInvalid(true);
            birthDate.setErrorMessage("יש להזין תאריך לידה");
            valid = false;
        } else if (birthDate.getValue().isAfter(LocalDate.now())) {
            birthDate.setInvalid(true);
            birthDate.setErrorMessage("תאריך לידה לא יכול להיות עתידי");
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
        birthDate.setInvalid(false);
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
        ReservationTimer.clear();

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

    private String normalizedCouponCode() {
        String value = couponCode.getValue();
        return value == null ? "" : value.trim();
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
}