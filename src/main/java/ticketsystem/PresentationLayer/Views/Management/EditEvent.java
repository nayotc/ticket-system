package ticketsystem.PresentationLayer.Views.Management;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;
import ticketsystem.DTO.Event.EventDTO;
import ticketsystem.DomainLayer.event.EventCategory;
import ticketsystem.DomainLayer.event.EventLocation;
import ticketsystem.DomainLayer.event.SaleStatus;
import ticketsystem.PresentationLayer.Components.AppCard;
import ticketsystem.PresentationLayer.Components.Notifications;
import ticketsystem.PresentationLayer.Components.PageContainer;
import ticketsystem.PresentationLayer.Components.StatusBadge;
import ticketsystem.PresentationLayer.Components.ViewHeader;
import ticketsystem.PresentationLayer.Layouts.ManagementLayout;
import ticketsystem.PresentationLayer.Presenters.CompanyManagementState;
import ticketsystem.PresentationLayer.Presenters.ManageEventPresenter;
import ticketsystem.PresentationLayer.Presenters.PresentationException;
import ticketsystem.PresentationLayer.Session.UiSession;
import ticketsystem.PresentationLayer.Views.Management.EditEvent.EditEventPresenter;
import ticketsystem.PresentationLayer.Views.Management.EditEvent.UpdateEventRequest;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

@Route(value = "companies/:companyId/events/:eventId/edit", layout = ManagementLayout.class)
public class EditEvent extends PageContainer implements BeforeEnterObserver {

    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final String DATE_PICKER_DISPLAY_FORMAT = "dd/MM/yy";
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern(DATE_PICKER_DISPLAY_FORMAT);
    private static final DateTimeFormatter DISPLAY_DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");

    private final EditEventPresenter presenter;

    private Long companyId;
    private Long eventId;
    private EventDTO loadedEvent;

    private EditTab activeTab = EditTab.GENERAL;

    private final Div tabsShell = new Div();
    private final Div tabContent = new Div();

    private final TextField eventName = new TextField("שם האירוע");
    private final TextField artistName = new TextField("שם האמן / המופע");
    private final ComboBox<EventCategory> category = new ComboBox<>("קטגוריה");
    private final DatePicker eventDate = new DatePicker("תאריך");
    private final TimePicker eventTime = new TimePicker("שעה");
    private final ComboBox<EventLocation> location = new ComboBox<>("מיקום");
    private final TextField ticketPrice = new TextField("מחיר כרטיס");
    private final TextField trafficThreshold = new TextField("רף עומס");

    private PurchaseExpressionNode purchasePolicyRoot = PurchaseExpressionNode.group(LogicalOperator.AND);
    private final List<DiscountDTO> discounts = new ArrayList<>();
    private DiscountCompositionStrategy discountCompositionStrategy = DiscountCompositionStrategy.MAXIMUM;

    private final Div purchaseExpressionContainer = new Div();
    private final Div discountsContainer = new Div();

    private final Button maximumDiscountButton = new Button("מקסימום");
    private final Button sumDiscountButton = new Button("סכום");

    public EditEvent() {
        this(null);
    }

    @Autowired
    public EditEvent(EditEventPresenter presenter) {
        this.presenter = presenter;

        getElement().setAttribute("dir", "rtl");
        addClassName("edit-event-page");
        addClassName("policy-editor-page");

        configureGeneralFields();
        tabsShell.addClassName("edit-event-tabs-shell");
        tabContent.addClassName("edit-event-tab-content");
        tabContent.addClassName("policy-editor-tab-content");

        add(createHeader(), tabsShell);

        resetPolicyDrafts();

        tabContent.removeAll();
        tabContent.add(paragraph("טוען פרטי אירוע..."));
        tabsShell.add(tabContent);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        companyId = parseLongRouteParameter(event, "companyId");
        eventId = parseLongRouteParameter(event, "eventId");

        if (companyId == null || eventId == null) {
            Notifications.error("כתובת העריכה אינה תקינה.");
            return;
        }

        loadEventDetails();
    }

    private Long parseLongRouteParameter(BeforeEnterEvent event, String parameterName) {
        return event.getRouteParameters()
                .get(parameterName)
                .map(value -> {
                    try {
                        return Long.valueOf(value);
                    } catch (NumberFormatException exception) {
                        return null;
                    }
                })
                .orElse(null);
    }

    private ViewHeader createHeader() {
        Button reloadButton = createSecondaryButton("בטל שינויים", "↺");
        reloadButton.addClickListener(event -> loadEventDetails());

        return new ViewHeader(
                "ניהול אירוע",
                "עדכון פרטי אירוע, מצב מכירה, מדיניות רכישה ומדיניות הנחות של האירוע.",
                reloadButton
        );
    }

    private void configureGeneralFields() {
        eventName.setPrefixComponent(VaadinIcon.TICKET.create());
        artistName.setPrefixComponent(VaadinIcon.MICROPHONE.create());
        ticketPrice.setPrefixComponent(VaadinIcon.MONEY.create());
        trafficThreshold.setPrefixComponent(VaadinIcon.USERS.create());

        Span shekel = new Span("₪");
        shekel.addClassName("edit-event-price-currency");
        ticketPrice.setSuffixComponent(shekel);

        ticketPrice.getElement().setAttribute("inputmode", "decimal");
        trafficThreshold.getElement().setAttribute("inputmode", "numeric");

        category.setItems(EventCategory.values());
        category.setItemLabelGenerator(this::prettyEnum);

        location.setItems(EventLocation.values());
        location.setItemLabelGenerator(this::prettyEnum);

        DatePicker.DatePickerI18n datePickerI18n = new DatePicker.DatePickerI18n();
        datePickerI18n.setDateFormat(DATE_PICKER_DISPLAY_FORMAT);
        datePickerI18n.setFirstDayOfWeek(0);
        datePickerI18n.setToday("היום");
        datePickerI18n.setCancel("ביטול");

        eventDate.setI18n(datePickerI18n);
        eventDate.setLocale(Locale.forLanguageTag("he-IL"));
        eventDate.setMin(LocalDate.now());

        eventTime.setLocale(Locale.forLanguageTag("en-GB"));
        eventTime.setStep(Duration.ofMinutes(15));

        List<Component> fields = List.of(
                eventName,
                artistName,
                category,
                eventDate,
                eventTime,
                location,
                ticketPrice,
                trafficThreshold
        );

        fields.forEach(field -> {
            field.getElement().setAttribute("dir", "rtl");
            field.getElement().getStyle().set("width", "100%");
        });
    }

    private void loadEventDetails() {
        if (presenter == null) {
            Notifications.error("העמוד לא מחובר לפרזנטר.");
            return;
        }

        try {
            String token = UiSession.getMemberToken();
            loadedEvent = presenter.getEvent(token, eventId);

            if (loadedEvent == null) {
                Notifications.error("האירוע לא נמצא.");
                return;
            }

            if (!Objects.equals(companyId, loadedEvent.companyId())) {
                Notifications.warning("האירוע שייך לחברה אחרת מזו שמופיעה בכתובת.");
            }

            populateGeneralFields(loadedEvent);
            loadPolicyDrafts();
            refreshTabs();

        } catch (Exception exception) {
            Notifications.error("שגיאה בטעינת פרטי האירוע: " + exception.getMessage());
        }
    }

    private void populateGeneralFields(EventDTO event) {
        eventName.setValue(safeText(event.name()));
        artistName.setValue(safeText(event.artistName()));
        ticketPrice.setValue(event.ticketPrice() == null ? "" : event.ticketPrice().stripTrailingZeros().toPlainString());
        trafficThreshold.setValue(event.trafficThreshold() == null ? "" : String.valueOf(event.trafficThreshold()));

        category.setValue(parseEnum(EventCategory.class, event.category()));
        location.setValue(parseEnum(EventLocation.class, event.location()));

        if (event.date() != null) {
            eventDate.setValue(event.date().toLocalDate());
            eventTime.setValue(event.date().toLocalTime().withSecond(0).withNano(0));
        }

        boolean active = isActiveEvent();
        eventName.setEnabled(!active);
        ticketPrice.setEnabled(!active);
    }

    private void loadPolicyDrafts() {
        try {
            PurchasePolicyExpressionDraftDTO purchaseDraft = presenter.loadEventPurchasePolicy(
                    UiSession.getMemberToken(),
                    eventId
            );
            applyPurchasePolicyDraft(purchaseDraft);
        } catch (Exception exception) {
            purchasePolicyRoot = PurchaseExpressionNode.group(LogicalOperator.AND);
            refreshPurchaseExpression();
        }

        try {
            DiscountPolicyDraftDTO discountDraft = presenter.loadEventDiscountPolicy(
                    UiSession.getMemberToken(),
                    eventId
            );
            applyDiscountPolicyDraft(discountDraft);
        } catch (Exception exception) {
            discounts.clear();
            setDiscountCompositionStrategy(DiscountCompositionStrategy.MAXIMUM);
            refreshDiscounts();
        }
    }

    private void refreshTabs() {
        tabsShell.removeAll();
        tabContent.removeAll();

        Tabs tabs = new Tabs(
                createTab("פרטים כלליים", VaadinIcon.INFO_CIRCLE.create()),
                createTab("מדיניות רכישה", VaadinIcon.CART.create()),
                createTab("מדיניות הנחות", VaadinIcon.MONEY.create())
        );
        tabs.addClassName("edit-event-tabs");
        tabs.addClassName("policy-editor-tabs");
        tabs.setSelectedIndex(activeTab.ordinal());
        tabs.addSelectedChangeListener(event -> {
            activeTab = EditTab.values()[tabs.getSelectedIndex()];
            refreshTabContent();
        });

        tabsShell.add(tabs, tabContent);
        refreshTabContent();
    }

    private Tab createTab(String title, Component icon) {
        Div label = new Div();
        label.addClassName("policy-tab-label");

        Span iconWrapper = new Span(icon);
        iconWrapper.addClassName("policy-tab-icon");

        Div text = new Div();
        text.addClassName("policy-tab-text");

        Span titleSpan = new Span(title);
        titleSpan.addClassName("policy-tab-title");

        Span subtitleSpan = new Span(resolveTabSubtitle(title));
        subtitleSpan.addClassName("policy-tab-subtitle");

        text.add(titleSpan, subtitleSpan);
        label.add(iconWrapper, text);

        Tab tab = new Tab(label);
        tab.addClassName("policy-editor-tab");
        return tab;
    }

    private String resolveTabSubtitle(String title) {
        return switch (title) {
            case "פרטים כלליים" -> "פרטי אירוע ומצב מכירה";
            case "מדיניות רכישה" -> "תנאים וקבוצות AND / OR";
            case "מדיניות הנחות" -> "סוגי הנחות ושילוב הנחות";
            default -> "";
        };
    }

    private void refreshTabContent() {
        tabContent.removeAll();

        switch (activeTab) {
            case GENERAL -> tabContent.add(createGeneralDetailsTab());
            case PURCHASE -> tabContent.add(createPurchasePolicySection());
            case DISCOUNT -> tabContent.add(createDiscountPolicySection());
        }
    }

    private Component createGeneralDetailsTab() {
        Div layout = new Div();
        layout.addClassName("edit-event-general-grid");

        AppCard detailsCard = new AppCard();
        detailsCard.addClassNames("edit-event-card", "edit-event-details-card");
        detailsCard.add(
                createPolicyAccent("purchase"),
                createPolicyTitle("✎", "פרטי האירוע"),
                createGeneralFormGrid(),
                createGeneralActions()
        );

        AppCard statusCard = new AppCard();
        statusCard.addClassNames("edit-event-card", "edit-event-status-card");
        statusCard.add(
                createPolicyAccent("discount"),
                createPolicyTitle("ⓘ", "מצב ותפעול מכירה"),
                createStatusSummary(),
                createSaleStatusActions()
        );

        layout.add(detailsCard, statusCard);
        return layout;
    }

    private Component createGeneralFormGrid() {
        Div grid = new Div();
        grid.addClassName("edit-event-form-grid");
        grid.add(
                eventName,
                artistName,
                category,
                location,
                eventDate,
                eventTime,
                ticketPrice,
                trafficThreshold
        );
        return grid;
    }

    private Component createGeneralActions() {
        Button save = createPrimaryButton("שמירת פרטים", "✓");
        save.addClickListener(event -> saveGeneralDetails());

        Div actions = new Div(save);
        actions.addClassName("policy-section-actions");
        return actions;
    }

    private Component createStatusSummary() {
        Div summary = new Div();
        summary.addClassName("edit-event-status-summary");

        if (loadedEvent == null) {
            summary.add(paragraph("האירוע עדיין לא נטען."));
            return summary;
        }

        summary.add(
                statusRow("סטטוס אירוע", new StatusBadge(translateStatus(loadedEvent.status()), statusType(loadedEvent.status()))),
                statusRow("מצב מכירה", new StatusBadge(translateSaleStatus(parseEnum(SaleStatus.class, loadedEvent.saleStatus())), saleStatusBadgeType(loadedEvent.saleStatus()))),
                statusTextRow("כרטיסים שנמכרו", "0 / 0")
        );

        Paragraph placeholder = new Paragraph("ספירת הכרטיסים שנמכרו מתוך הסך הכול עדיין לא מחוברת ללוגיקה. כרגע זה מקום שמור לתצוגה בלבד.");
        placeholder.addClassName("edit-event-status-note");
        summary.add(placeholder);

        if (isActiveEvent()) {
            Paragraph note = new Paragraph("אירוע פעיל מאפשר שינוי תאריך, מיקום, קטגוריה, אמן ורף עומס בלבד");
            note.addClassName("edit-event-status-note");
            summary.add(note);
        }

        return summary;
    }

    private Component createSaleStatusActions() {
        Div wrapper = new Div();
        wrapper.addClassName("edit-event-sale-status-panel");

        if (loadedEvent == null) {
            wrapper.add(paragraph("פעולות המכירה יוצגו לאחר טעינת האירוע."));
            return wrapper;
        }

        Div quickActions = new Div();
        quickActions.addClassName("edit-event-sale-status-actions");

        if ("ACTIVE".equalsIgnoreCase(loadedEvent.status())) {
            boolean hasLottery = presenter.hasLottery(UiSession.getMemberToken(), eventId);
            SaleStatus currentStatus = currentSaleStatus();

            boolean notStarted = currentStatus == null || currentStatus == SaleStatus.NOT_STARTED;
            boolean preSale = currentStatus == SaleStatus.PRE_SALE;
            boolean ongoing = currentStatus == SaleStatus.ONGOING;

            boolean showPreSaleButton = hasLottery && notStarted;

            boolean showRegularSaleButton =
                    (hasLottery && preSale)
                            || (!hasLottery && notStarted);

            boolean showCloseSaleButton = preSale || ongoing;

            if (showPreSaleButton) {
                Button startPreSale = createPrimaryButton("פתח מכירה מוקדמת", "🔑");
                startPreSale.addClassName("edit-event-sale-status-main-button");
                startPreSale.addClassName("edit-event-lottery-sale-button");

                startPreSale.addClickListener(event -> conductLottery());

                applyDisabledState(
                        startPreSale,
                        canMoveToPreSale(),
                        "לא ניתן לפתוח מכירה מוקדמת במצב האירוע הנוכחי"
                );

                quickActions.add(startPreSale);
            }

            if (showRegularSaleButton) {
                Button openRegularSale = createPrimaryButton("פתח מכירה רגילה", "🎟");
                openRegularSale.addClassName("edit-event-sale-status-main-button");

                openRegularSale.addClickListener(event -> updateSaleStatus(SaleStatus.ONGOING));

                applyDisabledState(
                        openRegularSale,
                        canMoveToOngoing(),
                        "לא ניתן לפתוח מכירה רגילה במצב האירוע הנוכחי"
                );

                quickActions.add(openRegularSale);
            }

//            if (showCloseSaleButton) {
//                Button closeSale = createDangerButton("סגור מכירה", "⏹");
//                closeSale.addClassName("edit-event-sale-status-main-button");
//                closeSale.addClassName("edit-event-sale-status-close-button");
//
//                closeSale.addClickListener(event -> updateSaleStatus(SaleStatus.ENDED));
//
//                applyDisabledState(
//                        closeSale,
//                        canCloseSale(),
//                        "לא ניתן לסגור מכירה במצב האירוע הנוכחי"
//                );
//
//                quickActions.add(closeSale);
//            }
        }

        Button cancelEvent = createDangerButton("בטל אירוע", "×");
        cancelEvent.addClassName("edit-event-sale-status-main-button");
        cancelEvent.addClassName("edit-event-sale-status-cancel-button");

        applyDisabledState(
                cancelEvent,
                !isCancelledEvent(),
                "האירוע כבר מבוטל"
        );

        cancelEvent.addClickListener(event -> confirmCancelEvent());
        quickActions.add(cancelEvent);

        wrapper.add(quickActions);
        return wrapper;
    }

    private void applyDisabledState(Button button, boolean enabled, String disabledTitle) {
        button.setEnabled(enabled);

        if (enabled) {
            button.removeClassName("tn-disabled-action-button");
            button.getElement().removeAttribute("title");
            button.getElement().removeAttribute("aria-label");
            return;
        }

        button.addClassName("tn-disabled-action-button");
        button.getElement().setAttribute("title", disabledTitle);
        button.getElement().setAttribute("aria-label", disabledTitle);
    }

    private boolean isCancelledEvent() {
        return loadedEvent != null && "CANCELLED".equalsIgnoreCase(loadedEvent.status());
    }

    private boolean canMoveToPreSale() {
        if (isCancelledEvent()) {
            return false;
        }

        SaleStatus current = currentSaleStatus();
        return current == null || current == SaleStatus.NOT_STARTED;
    }

    private boolean canMoveToOngoing() {
        if (isCancelledEvent()) {
            return false;
        }

        SaleStatus current = currentSaleStatus();
        return current == null || current == SaleStatus.NOT_STARTED || current == SaleStatus.PRE_SALE;
    }

    private boolean canCloseSale() {
        if (isCancelledEvent()) {
            return false;
        }

        SaleStatus current = currentSaleStatus();
        return current == SaleStatus.PRE_SALE || current == SaleStatus.ONGOING;
    }

    private SaleStatus currentSaleStatus() {
        return loadedEvent == null ? null : parseEnum(SaleStatus.class, loadedEvent.saleStatus());
    }

    private void updateSaleStatus(SaleStatus targetStatus) {
        try {
            presenter.updateEventSaleStatus(UiSession.getMemberToken(), eventId, targetStatus);
            Notifications.success("מצב המכירה עודכן בהצלחה.");
            loadEventDetails();
        } catch (Exception exception) {
            Notifications.error("שגיאה בעדכון מצב המכירה: " + exception.getMessage());
        }
    }

    private void conductLottery() {
        try {
            presenter.conductLottery(UiSession.getMemberToken(), eventId, companyId);
            presenter.updateEventSaleStatus(UiSession.getMemberToken(), eventId, SaleStatus.PRE_SALE);
            Notifications.success("הגרלה בוצעה בהצלחה, מכירה מוקדמת נפתחה.");
            loadEventDetails();
        } catch (Exception exception) {
            Notifications.error("שגיאה בביצוע הגרלה: " + exception.getMessage());
        }
    }

    private Component statusRow(String label, Component value) {
        Div row = new Div();
        row.addClassName("edit-event-status-row");
        Span labelSpan = new Span(label);
        labelSpan.addClassName("edit-event-status-label");
        row.add(labelSpan, value);
        return row;
    }

    private Component statusTextRow(String label, String value) {
        Span valueSpan = new Span(safeText(value, "לא הוגדר"));
        valueSpan.addClassName("edit-event-status-value");
        return statusRow(label, valueSpan);
    }

    private void saveGeneralDetails() {
        if (loadedEvent == null) {
            Notifications.error("לא ניתן לשמור לפני טעינת האירוע.");
            return;
        }

        try {
            EventDTO updateDTO = createUpdateDTO();
            presenter.updateEvent(new UpdateEventRequest(UiSession.getMemberToken(), updateDTO));
            Notifications.success("פרטי האירוע נשמרו בהצלחה.");
            loadEventDetails();
        } catch (Exception exception) {
            Notifications.error("שגיאה בשמירת פרטי האירוע: " + exception.getMessage());
        }
    }

    private EventDTO createUpdateDTO() {
        validateGeneralFields();

        boolean active = isActiveEvent();
        LocalDateTime selectedDateTime = LocalDateTime.of(eventDate.getValue(), eventTime.getValue());

        return new EventDTO(
                loadedEvent.id(),
                active ? null : safeTrim(eventName.getValue()),
                loadedEvent.companyId(),
                loadedEvent.openedBy(),
                selectedDateTime,
                location.getValue().name(),
                parseLong(trafficThreshold.getValue(), "רף עומס"),
                loadedEvent.status(),
                category.getValue().name(),
                safeTrim(artistName.getValue()),
                active ? null : parsePrice(ticketPrice.getValue()),
                loadedEvent.mapSize(),
                loadedEvent.rate(),
                loadedEvent.saleStatus(),
                loadedEvent.overloaded(),
                loadedEvent.activeReservationsCount(),
                loadedEvent.version(),
                loadedEvent.map()
        );
    }

    private void validateGeneralFields() {
        if (!isActiveEvent() && eventName.isEmpty()) {
            throw new IllegalArgumentException("יש להזין שם אירוע.");
        }

        if (artistName.isEmpty()) {
            throw new IllegalArgumentException("יש להזין שם אמן או מופע.");
        }

        if (category.isEmpty()) {
            throw new IllegalArgumentException("יש לבחור קטגוריה.");
        }

        if (location.isEmpty()) {
            throw new IllegalArgumentException("יש לבחור מיקום.");
        }

        if (eventDate.isEmpty() || eventTime.isEmpty()) {
            throw new IllegalArgumentException("יש לבחור תאריך ושעה.");
        }

        if (!isActiveEvent() && ticketPrice.isEmpty()) {
            throw new IllegalArgumentException("יש להזין מחיר כרטיס.");
        }

        if (trafficThreshold.isEmpty()) {
            throw new IllegalArgumentException("יש להזין רף עומס.");
        }
    }

    private Component createPurchasePolicySection() {
        AppCard card = new AppCard();
        card.addClassNames("policy-card", "policy-card-purchase");

        card.add(
                createPolicyAccent("purchase"),
                createPolicyTitle("⚖", "מדיניות רכישה"),
                paragraph("בנה תנאי רכישה מורכב באמצעות קבוצות AND ו־OR. המדיניות נשמרת על האירוע עצמו."),
                createPurchaseExpressionBuilder(),
                createPurchasePolicyActions()
        );

        return card;
    }

    private Component createDiscountPolicySection() {
        AppCard card = new AppCard();
        card.addClassNames("policy-card", "policy-card-discount");

        discountsContainer.addClassName("discounts-list");

        card.add(
                createPolicyAccent("discount"),
                createPolicyTitle("%", "מדיניות הנחות"),
                paragraph("בחר סוג הנחה. רק השדות הרלוונטיים לסוג שנבחר יוצגו בטופס. המדיניות נשמרת על האירוע עצמו."),
                createDiscountCompositionSelector(),
                discountsContainer,
                createAddDiscountButton(),
                createDiscountPolicyActions()
        );

        return card;
    }

    private Component createPurchaseExpressionBuilder() {
        Div builder = new Div();
        builder.addClassName("purchase-rule-builder");

        purchaseExpressionContainer.addClassName("purchase-expression-tree");

        builder.add(purchaseExpressionContainer);
        refreshPurchaseExpression();
        return builder;
    }

    private Component createPurchasePolicyActions() {
        Button savePurchaseButton = createPrimaryButton("שמור מדיניות רכישה", "✓");
        savePurchaseButton.addClickListener(event -> savePurchaseDraft());

        Div actions = new Div(savePurchaseButton);
        actions.addClassName("policy-section-actions");
        return actions;
    }

    private Component createDiscountPolicyActions() {
        Button saveDiscountButton = createPrimaryButton("שמור מדיניות הנחות", "✓");
        saveDiscountButton.addClickListener(event -> saveDiscountDraft());

        Div actions = new Div(saveDiscountButton);
        actions.addClassName("policy-section-actions");
        return actions;
    }

    private Component createDiscountCompositionSelector() {
        Div wrapper = new Div();
        wrapper.addClassName("discount-composition-selector");

        Div text = new Div();
        text.addClassName("discount-composition-text");
        text.add(new Span("לוגיקת שילוב הנחות"), smallText("כיצד לחשב כשיש מספר הנחות חופפות"));

        maximumDiscountButton.addClassName("discount-composition-button");
        sumDiscountButton.addClassName("discount-composition-button");

        maximumDiscountButton.addClickListener(event -> setDiscountCompositionStrategy(DiscountCompositionStrategy.MAXIMUM));
        sumDiscountButton.addClickListener(event -> setDiscountCompositionStrategy(DiscountCompositionStrategy.SUM));

        Div actions = new Div(maximumDiscountButton, sumDiscountButton);
        actions.addClassName("discount-composition-actions");

        wrapper.add(text, actions);
        setDiscountCompositionStrategy(discountCompositionStrategy);
        return wrapper;
    }

    private Component createAddDiscountButton() {
        Button button = createDashedButton("הוסף הנחה חדשה", "+");
        button.addClassName("policy-add-discount-button");
        button.addClickListener(event -> openDiscountDialog(null));
        return button;
    }

    private Component createPolicyTitle(String iconText, String title) {
        Div header = new Div();
        header.addClassName("policy-card-title-row");

        Span icon = new Span(iconText);
        icon.addClassName("policy-card-icon");

        H2 heading = new H2(title);
        heading.addClassName("policy-card-title");

        header.add(icon, heading);
        return header;
    }

    private Component createPolicyAccent(String type) {
        Div accent = new Div();
        accent.addClassName("policy-card-accent");
        accent.addClassName("policy-card-accent-" + type);
        return accent;
    }

    private Paragraph paragraph(String text) {
        Paragraph paragraph = new Paragraph(text);
        paragraph.addClassName("policy-card-description");
        return paragraph;
    }

    private Span smallText(String text) {
        Span span = new Span(text);
        span.addClassName("policy-small-text");
        return span;
    }

    private void refreshPurchaseExpression() {
        purchaseExpressionContainer.removeAll();
        purchaseExpressionContainer.add(createPurchaseExpressionNode(purchasePolicyRoot, null, 0));
    }

    private Component createPurchaseExpressionNode(PurchaseExpressionNode node, PurchaseExpressionNode parent, int depth) {
        if (node.isRule()) {
            return createPurchaseExpressionRuleRow(node, parent);
        }

        Div group = new Div();
        group.addClassNames("purchase-expression-group", "purchase-expression-depth-" + Math.min(depth, 3));

        Div header = new Div();
        header.addClassName("purchase-expression-group-header");

        Div titleBlock = new Div();
        titleBlock.addClassName("purchase-expression-group-title-block");

        Span badge = new Span(depth == 0 ? "שורש" : "קבוצה");
        badge.addClassName("purchase-expression-group-badge");

        Div titleText = new Div();
        titleText.addClassName("purchase-expression-group-title-text");

        Span title = new Span(depth == 0 ? "הביטוי הראשי" : "קבוצת תנאים");
        title.addClassName("purchase-expression-group-title");

        Span summary = new Span(createGroupSummary(node));
        summary.addClassName("purchase-expression-group-summary");

        titleText.add(title, summary);
        titleBlock.add(badge, titleText);

        ComboBox<LogicalOperator> operator = new ComboBox<>();
        operator.setItems(LogicalOperator.values());
        operator.setItemLabelGenerator(LogicalOperator::getLabel);
        operator.setValue(node.operator());
        operator.addClassName("purchase-expression-operator");
        operator.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                node.setOperator(event.getValue());
                refreshPurchaseExpression();
            }
        });

        Div operatorBlock = new Div();
        operatorBlock.addClassName("purchase-expression-operator-block");

        Span operatorLabel = new Span("חיבור בין הילדים");
        operatorLabel.addClassName("purchase-expression-operator-label");
        operatorBlock.add(operatorLabel, operator);

        Div actions = new Div();
        actions.addClassName("purchase-expression-group-actions");

        Button addRule = createSecondaryButton("תנאי", "+");
        addRule.addClassName("purchase-expression-action-button");
        addRule.addClickListener(event -> openPurchaseRuleDialog(null, rule -> {
            node.children().add(PurchaseExpressionNode.rule(rule));
            refreshPurchaseExpression();
        }));

        Button addGroup = createSecondaryButton("קבוצה", "+");
        addGroup.addClassName("purchase-expression-action-button");
        addGroup.addClickListener(event -> {
            node.children().add(PurchaseExpressionNode.group(LogicalOperator.AND));
            refreshPurchaseExpression();
        });

        actions.add(addRule, addGroup);

        if (parent != null) {
            Button deleteGroup = createDangerIconButton("מחיקת קבוצה", "×");
            deleteGroup.addClassName("purchase-expression-delete-group-button");
            deleteGroup.addClickListener(event -> {
                parent.children().remove(node);
                refreshPurchaseExpression();
            });
            actions.add(deleteGroup);
        }

        header.add(titleBlock, operatorBlock, actions);

        Div children = new Div();
        children.addClassName("purchase-expression-children");

        if (node.children().isEmpty()) {
            Div empty = new Div();
            empty.addClassName("policy-empty-state-inline");
            empty.add(new Span("אין תנאים בקבוצה הזו."));
            children.add(empty);
        } else {
            for (int i = 0; i < node.children().size(); i++) {
                if (i > 0) {
                    children.add(createLogicalConnector(node.operator()));
                }

                PurchaseExpressionNode child = node.children().get(i);
                children.add(createPurchaseExpressionNode(child, node, depth + 1));
            }
        }

        group.add(header, children);
        return group;
    }

    private Component createLogicalConnector(LogicalOperator operator) {
        Div connector = new Div();
        connector.addClassName("purchase-expression-connector");

        Span label = new Span(operator == LogicalOperator.OR ? "או" : "וגם");
        label.addClassName("purchase-expression-connector-label");

        connector.add(label);
        return connector;
    }

    private String createGroupSummary(PurchaseExpressionNode node) {
        int groups = 0;
        int rules = 0;

        for (PurchaseExpressionNode child : node.children()) {
            if (child.isRule()) {
                rules++;
            } else {
                groups++;
            }
        }

        if (groups == 0 && rules == 0) {
            return "הקבוצה ריקה";
        }

        List<String> parts = new ArrayList<>();

        if (rules > 0) {
            parts.add(rules + " תנאים");
        }

        if (groups > 0) {
            parts.add(groups + " קבוצות");
        }

        return String.join(" · ", parts);
    }

    private Component createPurchaseExpressionRuleRow(PurchaseExpressionNode node, PurchaseExpressionNode parent) {
        PurchaseRuleDTO rule = node.rule();

        Div row = new Div();
        row.addClassName("purchase-rule-row");

        Span drag = new Span("תנאי");
        drag.addClassName("policy-condition-badge");

        Div text = new Div();
        text.addClassName("purchase-rule-text");
        text.add(new Span(rule.toDisplayText()), smallText(rule.toTechnicalText()));

        Button edit = createIconButton("עריכה", "✎");
        edit.addClickListener(event -> openPurchaseRuleDialog(rule, updated -> {
            node.setRule(updated);
            refreshPurchaseExpression();
        }));

        Button delete = createDangerIconButton("מחיקה", "×");
        delete.addClickListener(event -> {
            if (parent != null) {
                parent.children().remove(node);
                refreshPurchaseExpression();
            }
        });

        Div actions = new Div(edit, delete);
        actions.addClassName("policy-row-actions");

        row.add(drag, text, actions);
        return row;
    }

    private void refreshDiscounts() {
        discountsContainer.removeAll();

        if (discounts.isEmpty()) {
            Div empty = new Div();
            empty.addClassName("policy-empty-state-inline");
            empty.add(new Span("לא הוגדרו הנחות. ברירת המחדל היא ללא הנחה."));
            discountsContainer.add(empty);
            return;
        }

        discounts.forEach(discount -> discountsContainer.add(createDiscountRow(discount)));
    }

    // private Component createDiscountRow(DiscountDTO discount) {
    //     Div row = new Div();
    //     row.addClassName("discount-row");

    //     Div top = new Div();
    //     top.addClassName("discount-row-top");

    //     Span icon = new Span(discount.type().getIconText());
    //     icon.addClassName("discount-row-icon");

    //     Div titleBlock = new Div();
    //     titleBlock.addClassName("discount-row-title-block");

    //     H3 title = new H3(discount.name());
    //     title.addClassName("discount-row-title");

    //     titleBlock.add(title, smallText(discount.type().getDescription()));

    //     Div labels = new Div();
    //     labels.addClassName("discount-row-labels");

    //     if (discount.type() == DiscountType.COUPON && !discount.couponCode().isBlank()) {
    //         Span coupon = new Span(discount.couponCode());
    //         coupon.addClassName("discount-coupon-badge");
    //         labels.add(coupon);
    //     }

    //     if (discount.type() == DiscountType.CONDITIONAL && !discount.conditionText().isBlank()) {
    //         Span condition = new Span(discount.conditionText());
    //         condition.addClassName("discount-condition-badge");
    //         labels.add(condition);
    //     }

    //     Button edit = createIconButton("עריכה", "✎");
    //     edit.addClickListener(event -> openDiscountDialog(discount));

    //     Button delete = createDangerIconButton("מחיקה", "×");
    //     delete.addClickListener(event -> {
    //         discounts.remove(discount);
    //         refreshDiscounts();
    //     });

    //     labels.add(edit, delete);

    //     top.add(icon, titleBlock, labels);

    //     Div data = new Div(
    //             createDiscountDataBox("אופן חישוב", discount.valueType().getLabel()),
    //             createDiscountDataBox("ערך", discount.formattedValue())
    //     );

    //     if (discount.type() == DiscountType.COUPON && discount.validUntil() != null) {
    //         data.add(createDiscountDataBox("תוקף קופון", formatDate(discount.validUntil())));
    //     }

    //     if (discount.type() == DiscountType.CONDITIONAL && discount.conditionType() != null) {
    //         data.add(createDiscountDataBox("סוג תנאי", discount.conditionType().getLabel()));
    //     }

    //     data.addClassName("discount-data-grid");

    //     row.add(top, data);
    //     return row;
    // }
    private Component createDiscountRow(DiscountDTO discount) {
    Div row = new Div();
    row.addClassName("discount-row");

    Div top = new Div();
    top.addClassName("discount-row-top");

    Span icon = new Span(discount.type().getIconText());
    icon.addClassName("discount-row-icon");

    Div titleBlock = new Div();
    titleBlock.addClassName("discount-row-title-block");

    H3 title = new H3(discount.name());
    title.addClassName("discount-row-title");

    titleBlock.add(title, smallText(discount.type().getDescription()));

    Div labels = new Div();
    labels.addClassName("discount-row-labels");

    if (discount.type() == DiscountType.COUPON
            && discount.couponCode() != null
            && !discount.couponCode().isBlank()) {
        Span coupon = new Span(discount.couponCode());
        coupon.addClassName("discount-coupon-badge");
        labels.add(coupon);
    }

    if (discount.type() == DiscountType.CONDITIONAL
            && discount.conditionText() != null
            && !discount.conditionText().isBlank()) {
        Span condition = new Span(discount.conditionText());
        condition.addClassName("discount-condition-badge");
        labels.add(condition);
    }

    Button edit = createIconButton("עריכה", "✎");
    edit.addClickListener(event -> openDiscountDialog(discount));

    Button delete = createDangerIconButton("מחיקה", "×");
    delete.addClickListener(event -> {
        discounts.remove(discount);
        refreshDiscounts();
    });

    if (discount.type() == DiscountType.CONDITIONAL) {
        Button addCondition = createIconButton("הוסף תנאי", "+");
        addCondition.addClickListener(event -> openAddConditionDialog(discount));
        labels.add(addCondition);
    }

    labels.add(edit, delete);

    top.add(icon, titleBlock, labels);

    Div data = new Div(
            createDiscountDataBox("אופן חישוב", discount.valueType().getLabel()),
            createDiscountDataBox("ערך", discount.formattedValue())
    );

    if (discount.type() == DiscountType.COUPON && discount.validUntil() != null) {
        data.add(createDiscountDataBox("תוקף קופון", formatDate(discount.validUntil())));
    }

    if (discount.type() == DiscountType.CONDITIONAL) {
        int conditionCount = discount.conditions() == null ? 0 : discount.conditions().size();

        data.add(createDiscountDataBox("מספר תנאים", String.valueOf(conditionCount)));

        if (conditionCount > 0) {
            data.add(createDiscountDataBox("שילוב תנאים", "AND"));
        }
    }

    data.addClassName("discount-data-grid");

    row.add(top, data);
    return row;
}

private void openAddConditionDialog(DiscountDTO discount) {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle("הוספת תנאי להנחה");

    ComboBox<DiscountConditionType> conditionType = new ComboBox<>("סוג תנאי");
    conditionType.setItems(DiscountConditionType.values());
    conditionType.setItemLabelGenerator(DiscountConditionType::getLabel);
    conditionType.setWidthFull();

    IntegerField ticketThreshold = new IntegerField("מספר כרטיסים");
    ticketThreshold.setMin(1);
    ticketThreshold.setWidthFull();

    DateTimePicker startTime = new DateTimePicker("מתאריך");
    startTime.setWidthFull();

    DateTimePicker endTime = new DateTimePicker("עד תאריך");
    endTime.setWidthFull();

    Button save = new Button("הוסף תנאי", event -> {
        DiscountConditionType selectedType = conditionType.getValue();

        if (selectedType == null) {
            throw new PresentationException("יש לבחור סוג תנאי.");
        }

        DiscountConditionDTO newCondition = new DiscountConditionDTO(
                selectedType,
                ticketThreshold.getValue(),
                startTime.getValue(),
                endTime.getValue()
        );

        List<DiscountConditionDTO> updatedConditions = new ArrayList<>();
        if (discount.conditions() != null) {
            updatedConditions.addAll(discount.conditions());
        }
        updatedConditions.add(newCondition);

        DiscountDTO updated = discount.withConditions(updatedConditions);

        int index = discounts.indexOf(discount);
        if (index >= 0) {
            discounts.set(index, updated);
        }

        dialog.close();
        refreshDiscounts();
    });

    Button cancel = new Button("ביטול", event -> dialog.close());

    VerticalLayout layout = new VerticalLayout(
            conditionType,
            ticketThreshold,
            startTime,
            endTime,
            new HorizontalLayout(save, cancel)
    );

    dialog.add(layout);
    dialog.open();
}

    private Component createDiscountDataBox(String label, String value) {
        Div box = new Div();
        box.addClassName("discount-data-box");

        Span labelSpan = new Span(label);
        labelSpan.addClassName("discount-data-label");

        Span valueSpan = new Span(value);
        valueSpan.addClassName("discount-data-value");

        box.add(labelSpan, valueSpan);
        return box;
    }

    private void openPurchaseRuleDialog(PurchaseRuleDTO existingRule, Consumer<PurchaseRuleDTO> onSave) {
        boolean editing = existingRule != null;
        PurchaseRuleDTO draft = editing ? existingRule.copy() : PurchaseRuleDTO.defaultRule();

        Dialog dialog = new Dialog();
        dialog.addClassName("policy-editor-dialog");
        dialog.setHeaderTitle(editing ? "עריכת תנאי רכישה" : "הוספת תנאי רכישה");

        ComboBox<PurchaseRuleField> field = new ComboBox<>("שדה");
        field.setItems(PurchaseRuleField.values());
        field.setItemLabelGenerator(PurchaseRuleField::getLabel);
        field.setValue(draft.field());

        ComboBox<ComparisonOperator> operator = new ComboBox<>("אופרטור");
        operator.setItems(ComparisonOperator.values());
        operator.setItemLabelGenerator(ComparisonOperator::getLabel);
        operator.setValue(draft.operator());

        NumberField value = new NumberField("ערך");
        value.setMin(0);
        value.setStep(1);
        value.setValue((double) draft.value());

        TextField unit = new TextField("יחידת תצוגה");
        unit.setValue(draft.unit());
        unit.setPlaceholder("לדוגמה: שנים, לרוכש");

        Div form = new Div(field, operator, value, unit);
        form.addClassName("policy-dialog-form");

        Button cancel = createSecondaryButton("ביטול", null);
        cancel.addClickListener(event -> dialog.close());

        Button save = createPrimaryButton(editing ? "עדכן תנאי" : "הוסף תנאי", null);
        save.addClickListener(event -> {
            if (field.isEmpty() || operator.isEmpty() || value.isEmpty()) {
                showError("יש למלא שדה, אופרטור וערך.");
                return;
            }

            PurchaseRuleDTO updated = new PurchaseRuleDTO(
                    draft.id(),
                    field.getValue(),
                    operator.getValue(),
                    value.getValue().intValue(),
                    unit.getValue() == null ? "" : unit.getValue().trim()
            );

            onSave.accept(updated);
            dialog.close();
        });

        dialog.getFooter().add(cancel, save);
        dialog.add(form);
        dialog.open();
    }
    private void openDiscountDialog(DiscountDTO existingDiscount) {
    boolean editing = existingDiscount != null;
    DiscountDTO draft = editing ? existingDiscount : DiscountDTO.defaultDiscount();

    Dialog dialog = new Dialog();
    dialog.addClassName("policy-editor-dialog");
    dialog.setHeaderTitle(editing ? "עריכת הנחה" : "הוספת הנחה");

    TextField name = new TextField("שם ההנחה");
    name.setValue(draft.name());

    ComboBox<DiscountType> type = new ComboBox<>("סוג הנחה");
    type.setItems(DiscountType.values());
    type.setItemLabelGenerator(DiscountType::getLabel);
    type.setValue(draft.type());

    ComboBox<DiscountValueType> valueType = new ComboBox<>("אופן חישוב");
    valueType.setItems(DiscountValueType.values());
    valueType.setItemLabelGenerator(DiscountValueType::getLabel);
    valueType.setValue(draft.valueType());

    NumberField value = new NumberField("ערך");
    value.setMin(0);
    value.setValue(draft.value());

    TextField couponCode = new TextField("קוד קופון");
    couponCode.setValue(draft.couponCode());
    couponCode.setPlaceholder("לדוגמה: EARLYBIRD20");

    DatePicker validUntil = createPolicyDatePicker("תוקף קופון עד", draft.validUntil());

    List<DiscountConditionDTO> conditionDrafts = new ArrayList<>();
    if (draft.conditions() != null) {
        conditionDrafts.addAll(draft.conditions());
    }

    Div conditionsContainer = new Div();
    conditionsContainer.addClassName("discount-conditions-container");

    Runnable refreshConditions = () -> {
        conditionsContainer.removeAll();

        if (conditionDrafts.isEmpty()) {
            conditionsContainer.add(smallText("לא הוגדרו תנאים עדיין."));
            return;
        }

        for (DiscountConditionDTO condition : new ArrayList<>(conditionDrafts)) {
            Div conditionRow = new Div();
            conditionRow.addClassName("discount-condition-row");

            Span text = new Span(conditionText(condition));
            text.addClassName("discount-condition-badge");

            Button remove = createDangerIconButton("מחיקת תנאי", "×");
            remove.addClickListener(e -> {
                conditionDrafts.remove(condition);
                conditionsContainer.removeAll();
                // קריאה מחדש ידנית
                for (DiscountConditionDTO c : new ArrayList<>(conditionDrafts)) {
                    Div row = new Div();
                    row.addClassName("discount-condition-row");

                    Span t = new Span(conditionText(c));
                    t.addClassName("discount-condition-badge");

                    Button r = createDangerIconButton("מחיקת תנאי", "×");
                    r.addClickListener(ev -> {
                        conditionDrafts.remove(c);
                        openDiscountDialog(new DiscountDTO(
                                draft.id(),
                                name.getValue().trim(),
                                type.getValue(),
                                valueType.getValue(),
                                value.getValue(),
                                couponCode.getValue(),
                                validUntil.getValue(),
                                conditionDrafts
                        ));
                        dialog.close();
                    });

                    row.add(t, r);
                    conditionsContainer.add(row);
                }

                if (conditionDrafts.isEmpty()) {
                    conditionsContainer.add(smallText("לא הוגדרו תנאים עדיין."));
                }
            });

            conditionRow.add(text, remove);
            conditionsContainer.add(conditionRow);
        }
    };

    Button addCondition = createSecondaryButton("הוסף תנאי", null);
    addCondition.addClickListener(event -> openAddConditionDialog(conditionDrafts, conditionsContainer));

    Div conditionalBox = new Div(addCondition, conditionsContainer);
    conditionalBox.addClassName("discount-conditional-box");

    Div form = new Div(
            name,
            type,
            valueType,
            value,
            couponCode,
            validUntil,
            conditionalBox
    );
    form.addClassName("policy-dialog-form");

    Runnable applyVisibility = () -> {
        DiscountType selectedType = type.getValue();

        couponCode.setVisible(selectedType == DiscountType.COUPON);
        validUntil.setVisible(selectedType == DiscountType.COUPON);

        conditionalBox.setVisible(selectedType == DiscountType.CONDITIONAL);
    };

    refreshConditions.run();
    applyVisibility.run();

    type.addValueChangeListener(event -> applyVisibility.run());

    Button delete = createDangerButton("מחיקה", null);
    delete.setVisible(editing);
    delete.addClickListener(event -> {
        discounts.remove(existingDiscount);
        refreshDiscounts();
        dialog.close();
    });

    Button cancel = createSecondaryButton("ביטול", null);
    cancel.addClickListener(event -> dialog.close());

    Button save = createPrimaryButton(editing ? "עדכן הנחה" : "הוסף הנחה", null);
    save.addClickListener(event -> {
        if (name.isEmpty() || type.isEmpty() || valueType.isEmpty() || value.isEmpty()) {
            showError("יש למלא שם, סוג, אופן חישוב וערך.");
            return;
        }

        DiscountType selectedType = type.getValue();

        if (selectedType == DiscountType.COUPON && safeTrim(couponCode.getValue()).isBlank()) {
            showError("בהנחת קופון יש למלא קוד קופון.");
            return;
        }

        if (selectedType == DiscountType.CONDITIONAL && conditionDrafts.isEmpty()) {
            showError("בהנחה מותנית יש להוסיף לפחות תנאי אחד.");
            return;
        }

        DiscountDTO updated = new DiscountDTO(
                draft.id(),
                name.getValue().trim(),
                selectedType,
                valueType.getValue(),
                value.getValue(),
                selectedType == DiscountType.COUPON ? safeTrim(couponCode.getValue()) : "",
                selectedType == DiscountType.COUPON ? validUntil.getValue() : null,
                selectedType == DiscountType.CONDITIONAL ? new ArrayList<>(conditionDrafts) : new ArrayList<>()
        );

        if (editing) {
            int index = discounts.indexOf(existingDiscount);
            discounts.set(index, updated);
        } else {
            discounts.add(updated);
        }

        refreshDiscounts();
        dialog.close();
    });

    dialog.getFooter().add(delete, cancel, save);
    dialog.add(form);
    dialog.open();
}

private void openAddConditionDialog(List<DiscountConditionDTO> conditionDrafts,
                                    Div conditionsContainer) {
    Dialog dialog = new Dialog();
    dialog.addClassName("policy-editor-dialog");
    dialog.setHeaderTitle("הוספת תנאי");

    ComboBox<DiscountConditionType> conditionType = new ComboBox<>("סוג תנאי");
    conditionType.setItems(DiscountConditionType.values());
    conditionType.setItemLabelGenerator(DiscountConditionType::getLabel);
    conditionType.setValue(DiscountConditionType.MIN_TICKET);

    NumberField ticketThreshold = new NumberField("כמות כרטיסים");
    ticketThreshold.setMin(1);
    ticketThreshold.setStep(1);
    ticketThreshold.setPlaceholder("לדוגמה: 2");

    DatePicker startDate = createPolicyDatePicker("מתאריך", null);
    DatePicker endDate = createPolicyDatePicker("עד תאריך", null);

    Runnable applyVisibility = () -> {
        DiscountConditionType selected = conditionType.getValue();

        boolean needsTickets = selected != null && selected.requiresTicketThreshold();
        boolean needsDate = selected != null && selected.requiresDateRange();

        ticketThreshold.setVisible(needsTickets);
        startDate.setVisible(needsDate);
        endDate.setVisible(needsDate);
    };

    applyVisibility.run();
    conditionType.addValueChangeListener(event -> applyVisibility.run());

    Button cancel = createSecondaryButton("ביטול", null);
    cancel.addClickListener(event -> dialog.close());

    Button save = createPrimaryButton("הוסף תנאי", null);
    save.addClickListener(event -> {
        DiscountConditionType selected = conditionType.getValue();

        if (selected == null) {
            showError("יש לבחור סוג תנאי.");
            return;
        }

        Integer normalizedThreshold = null;
        LocalDateTime normalizedStartTime = null;
        LocalDateTime normalizedEndTime = null;

        if (selected.requiresTicketThreshold()) {
            if (ticketThreshold.isEmpty() || ticketThreshold.getValue() <= 0) {
                showError("בתנאי לפי כמות כרטיסים יש להזין כמות חיובית.");
                return;
            }

            normalizedThreshold = ticketThreshold.getValue().intValue();
        }

        if (selected.requiresDateRange()) {
            if (startDate.isEmpty() || endDate.isEmpty()) {
                showError("בתנאי לפי תאריך יש למלא תאריך התחלה ותאריך סיום.");
                return;
            }

            if (endDate.getValue().isBefore(startDate.getValue())) {
                showError("תאריך הסיום לא יכול להיות לפני תאריך ההתחלה.");
                return;
            }

            normalizedStartTime = startDate.getValue().atStartOfDay();
            normalizedEndTime = endDate.getValue().atTime(23, 59);
        }

        DiscountConditionDTO newCondition = new DiscountConditionDTO(
                selected,
                normalizedThreshold,
                normalizedStartTime,
                normalizedEndTime
        );

        conditionDrafts.add(newCondition);

        conditionsContainer.removeAll();
        for (DiscountConditionDTO condition : conditionDrafts) {
            Span conditionLabel = new Span(conditionText(condition));
            conditionLabel.addClassName("discount-condition-badge");
            conditionsContainer.add(conditionLabel);
        }

        dialog.close();
    });

    Div form = new Div(conditionType, ticketThreshold, startDate, endDate);
    form.addClassName("policy-dialog-form");

    dialog.getFooter().add(cancel, save);
    dialog.add(form);
    dialog.open();
}
private String conditionText(DiscountConditionDTO condition) {
    if (condition == null || condition.conditionType() == null) {
        return "";
    }

    DiscountConditionType type = condition.conditionType();

    if (type.requiresTicketThreshold()) {
        if (condition.ticketThreshold() == null) {
            return "";
        }

        return type.getDisplayPrefix() + " " + condition.ticketThreshold();
    }

    if (type.requiresDateRange()) {
        if (condition.startTime() != null && condition.endTime() != null) {
            return "תאריך מ-" + condition.startTime().format(DISPLAY_DATE_TIME)
                    + " עד " + condition.endTime().format(DISPLAY_DATE_TIME);
        }

        return "";
    }

    return "";
}


    // private void openDiscountDialog(DiscountDTO existingDiscount) {
    //     boolean editing = existingDiscount != null;
    //     DiscountDTO draft = editing ? existingDiscount : DiscountDTO.defaultDiscount();

    //     Dialog dialog = new Dialog();
    //     dialog.addClassName("policy-editor-dialog");
    //     dialog.setHeaderTitle(editing ? "עריכת הנחה" : "הוספת הנחה");

    //     TextField name = new TextField("שם ההנחה");
    //     name.setValue(draft.name());

    //     ComboBox<DiscountType> type = new ComboBox<>("סוג הנחה");
    //     type.setItems(DiscountType.values());
    //     type.setItemLabelGenerator(DiscountType::getLabel);
    //     type.setValue(draft.type());

    //     ComboBox<DiscountValueType> valueType = new ComboBox<>("אופן חישוב");
    //     valueType.setItems(DiscountValueType.values());
    //     valueType.setItemLabelGenerator(DiscountValueType::getLabel);
    //     valueType.setValue(draft.valueType());

    //     NumberField value = new NumberField("ערך");
    //     value.setMin(0);
    //     value.setValue(draft.value());

    //     TextField couponCode = new TextField("קוד קופון");
    //     couponCode.setValue(draft.couponCode());
    //     couponCode.setPlaceholder("לדוגמה: EARLYBIRD20");

    //     DatePicker validUntil = createPolicyDatePicker("תוקף קופון עד", draft.validUntil());

    //     ComboBox<DiscountConditionType> conditionType = new ComboBox<>("תנאי להפעלה");
    //     conditionType.setItems(DiscountConditionType.values());
    //     conditionType.setItemLabelGenerator(DiscountConditionType::getLabel);
    //     conditionType.setValue(Objects.requireNonNullElse(draft.conditionType(), DiscountConditionType.MIN_TICKET));

    //     NumberField ticketThreshold = new NumberField("כמות כרטיסים");
    //     ticketThreshold.setMin(1);
    //     ticketThreshold.setStep(1);
    //     if (draft.ticketThreshold() != null) {
    //         ticketThreshold.setValue(draft.ticketThreshold().doubleValue());
    //     }
    //     ticketThreshold.setPlaceholder("לדוגמה: 2");

    //     DatePicker startDate = createPolicyDatePicker("מתאריך", draft.startTime() != null ? draft.startTime().toLocalDate() : null);
    //     DatePicker endDate = createPolicyDatePicker("עד תאריך", draft.endTime() != null ? draft.endTime().toLocalDate() : null);

    //     Div form = new Div(name, type, valueType, value, couponCode, validUntil, conditionType, ticketThreshold, startDate, endDate);
    //     form.addClassName("policy-dialog-form");

    //     applyDiscountTypeVisibility(type.getValue(), conditionType.getValue(), couponCode, validUntil, conditionType, ticketThreshold, startDate, endDate);

    //     type.addValueChangeListener(event -> {
    //         if (event.getValue() == DiscountType.CONDITIONAL && conditionType.isEmpty()) {
    //             conditionType.setValue(DiscountConditionType.MIN_TICKET);
    //         }

    //         applyDiscountTypeVisibility(event.getValue(), conditionType.getValue(), couponCode, validUntil, conditionType, ticketThreshold, startDate, endDate);
    //     });

    //     conditionType.addValueChangeListener(event -> applyDiscountTypeVisibility(type.getValue(), event.getValue(), couponCode, validUntil, conditionType, ticketThreshold, startDate, endDate));

    //     Button delete = createDangerButton("מחיקה", null);
    //     delete.setVisible(editing);
    //     delete.addClickListener(event -> {
    //         discounts.remove(existingDiscount);
    //         refreshDiscounts();
    //         dialog.close();
    //     });

    //     Button cancel = createSecondaryButton("ביטול", null);
    //     cancel.addClickListener(event -> dialog.close());

    //     Button save = createPrimaryButton(editing ? "עדכן הנחה" : "הוסף הנחה", null);
    //     save.addClickListener(event -> {
    //         if (name.isEmpty() || type.isEmpty() || valueType.isEmpty() || value.isEmpty()) {
    //             showError("יש למלא שם, סוג, אופן חישוב וערך.");
    //             return;
    //         }

    //         DiscountType selectedType = type.getValue();
    //         DiscountConditionType selectedCondition = selectedType == DiscountType.CONDITIONAL ? conditionType.getValue() : null;

    //         if (selectedType == DiscountType.COUPON && safeTrim(couponCode.getValue()).isBlank()) {
    //             showError("בהנחת קופון יש למלא קוד קופון.");
    //             return;
    //         }

    //         Integer normalizedThreshold = null;
    //         LocalDateTime normalizedStartTime = null;
    //         LocalDateTime normalizedEndTime = null;

    //         if (selectedType == DiscountType.CONDITIONAL) {
    //             if (selectedCondition == null) {
    //                 showError("בהנחה מותנית יש לבחור תנאי להפעלה.");
    //                 return;
    //             }

    //             if (selectedCondition.requiresTicketThreshold()) {
    //                 if (ticketThreshold.isEmpty() || ticketThreshold.getValue() <= 0) {
    //                     showError("בתנאי לפי כמות כרטיסים יש להזין כמות חיובית.");
    //                     return;
    //                 }

    //                 normalizedThreshold = ticketThreshold.getValue().intValue();
    //             }

    //             if (selectedCondition.requiresDateRange()) {
    //                 if (startDate.isEmpty() || endDate.isEmpty()) {
    //                     showError("בתנאי לפי תאריך יש למלא תאריך התחלה ותאריך סיום.");
    //                     return;
    //                 }

    //                 if (endDate.getValue().isBefore(startDate.getValue())) {
    //                     showError("תאריך הסיום לא יכול להיות לפני תאריך ההתחלה.");
    //                     return;
    //                 }

    //                 normalizedStartTime = startDate.getValue().atStartOfDay();
    //                 normalizedEndTime = endDate.getValue().atTime(23, 59);
    //             }
    //         }

    //         DiscountDTO updated = new DiscountDTO(
    //                 draft.id(),
    //                 name.getValue().trim(),
    //                 selectedType,
    //                 valueType.getValue(),
    //                 value.getValue(),
    //                 selectedType == DiscountType.COUPON ? safeTrim(couponCode.getValue()) : "",
    //                 selectedType == DiscountType.COUPON ? validUntil.getValue() : null,
    //                 selectedCondition,
    //                 normalizedThreshold,
    //                 normalizedStartTime,
    //                 normalizedEndTime
    //         );

    //         if (editing) {
    //             int index = discounts.indexOf(existingDiscount);
    //             discounts.set(index, updated);
    //         } else {
    //             discounts.add(updated);
    //         }

    //         refreshDiscounts();
    //         dialog.close();
    //     });

    //     dialog.getFooter().add(delete, cancel, save);
    //     dialog.add(form);
    //     dialog.open();
    // }

    private void applyDiscountTypeVisibility(
            DiscountType selectedType,
            DiscountConditionType selectedCondition,
            TextField couponCode,
            DatePicker validUntil,
            ComboBox<DiscountConditionType> conditionType,
            NumberField ticketThreshold,
            DatePicker startDate,
            DatePicker endDate
    ) {
        DiscountType safeType = Objects.requireNonNullElse(selectedType, DiscountType.SIMPLE);
        DiscountConditionType safeCondition = Objects.requireNonNullElse(selectedCondition, DiscountConditionType.MIN_TICKET);

        boolean coupon = safeType == DiscountType.COUPON;
        boolean conditional = safeType == DiscountType.CONDITIONAL;
        boolean ticketCondition = conditional && safeCondition.requiresTicketThreshold();
        boolean dateCondition = conditional && safeCondition.requiresDateRange();

        couponCode.setVisible(coupon);
        validUntil.setVisible(coupon);
        conditionType.setVisible(conditional);
        ticketThreshold.setVisible(ticketCondition);
        startDate.setVisible(dateCondition);
        endDate.setVisible(dateCondition);
    }

    private DatePicker createPolicyDatePicker(String label, LocalDate value) {
        DatePicker datePicker = new DatePicker(label);
        DatePicker.DatePickerI18n i18n = new DatePicker.DatePickerI18n();
        i18n.setDateFormat(DATE_PICKER_DISPLAY_FORMAT);
        i18n.setFirstDayOfWeek(0);
        datePicker.setI18n(i18n);
        datePicker.setLocale(Locale.forLanguageTag("he-IL"));
        datePicker.setClearButtonVisible(true);
        datePicker.setPlaceholder("DD/MM/YY");
        datePicker.setValue(value);
        return datePicker;
    }

    private String formatDate(LocalDate date) {
        return date == null ? "" : date.format(DISPLAY_DATE);
    }

    private void setDiscountCompositionStrategy(DiscountCompositionStrategy strategy) {
        discountCompositionStrategy = Objects.requireNonNullElse(strategy, DiscountCompositionStrategy.MAXIMUM);

        maximumDiscountButton.removeClassName("discount-composition-button-selected");
        sumDiscountButton.removeClassName("discount-composition-button-selected");

        if (discountCompositionStrategy == DiscountCompositionStrategy.MAXIMUM) {
            maximumDiscountButton.addClassName("discount-composition-button-selected");
        } else {
            sumDiscountButton.addClassName("discount-composition-button-selected");
        }
    }

    private void applyPurchasePolicyDraft(PurchasePolicyExpressionDraftDTO draft) {
        if (draft == null || draft.root() == null) {
            purchasePolicyRoot = PurchaseExpressionNode.group(LogicalOperator.AND);
            refreshPurchaseExpression();
            return;
        }

        purchasePolicyRoot = PurchaseExpressionNode.fromDraft(draft.root());
        refreshPurchaseExpression();
    }

    private void applyDiscountPolicyDraft(DiscountPolicyDraftDTO draft) {
        discounts.clear();

        if (draft == null) {
            setDiscountCompositionStrategy(DiscountCompositionStrategy.MAXIMUM);
            refreshDiscounts();
            return;
        }

        setDiscountCompositionStrategy(draft.compositionStrategy());

        if (draft.discounts() != null) {
            discounts.addAll(draft.discounts());
        }

        refreshDiscounts();
    }

    private void resetPolicyDrafts() {
        purchasePolicyRoot = PurchaseExpressionNode.group(LogicalOperator.AND);
        discounts.clear();
        setDiscountCompositionStrategy(DiscountCompositionStrategy.MAXIMUM);
        refreshPurchaseExpression();
        refreshDiscounts();
    }

    private void savePurchaseDraft() {
        try {
            presenter.saveEventPurchasePolicy(
                    UiSession.getMemberToken(),
                    eventId,
                    getPurchasePolicyExpressionDraft()
            );

            showSuccess("מדיניות הרכישה נשמרה והתעדכנה בהצלחה באירוע.");

        } catch (Exception e) {
            showError("שגיאה בשמירת מדיניות הרכישה: " + e.getMessage());
        }
    }

    private void saveDiscountDraft() {
        try {
            presenter.saveEventDiscountPolicy(
                    UiSession.getMemberToken(),
                    eventId,
                    getDiscountPolicyDraft()
            );

            showSuccess("מדיניות ההנחות נשמרה והתעדכנה בהצלחה באירוע.");

        } catch (Exception e) {
            showError("שגיאה בשמירת מדיניות ההנחות: " + e.getMessage());
        }
    }

    public PurchasePolicyExpressionDraftDTO getPurchasePolicyExpressionDraft() {
        return new PurchasePolicyExpressionDraftDTO(
                String.valueOf(eventId),
                purchasePolicyRoot.toDraft()
        );
    }

    public DiscountPolicyDraftDTO getDiscountPolicyDraft() {
        return new DiscountPolicyDraftDTO(
                String.valueOf(eventId),
                discountCompositionStrategy,
                new ArrayList<>(discounts)
        );
    }

    private void confirmCancelEvent() {
        confirmOwnerAction(
                "ביטול אירוע",
                "האירוע יבוטל לאחר אישור הפעולה.",
                () -> {
                    presenter.cancelEvent(UiSession.getMemberToken(), eventId);
                    Notifications.success("אירוע בוטל בהצלחה.");
                    loadEventDetails();
                }
        );
    }

    private void confirmOwnerAction(String title, String text, Runnable action) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader(title);
        dialog.setText(text);
        dialog.setCancelable(true);
        dialog.setCancelText("ביטול");
        dialog.setConfirmText("אישור");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(event -> action.run());
        dialog.open();
    }

    private Button createPrimaryButton(String text, String iconText) {
        Button button = new Button(text);
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        button.addClassName("policy-primary-button");
        addIcon(button, iconText);
        return button;
    }

    private Button createSecondaryButton(String text, String iconText) {
        Button button = new Button(text);
        button.addClassName("policy-secondary-button");
        addIcon(button, iconText);
        return button;
    }

    private Button createDangerButton(String text, String iconText) {
        Button button = new Button(text);
        button.addClassName("policy-danger-button");
        addIcon(button, iconText);
        return button;
    }

    private Button createDashedButton(String text, String iconText) {
        Button button = new Button(text);
        button.addClassName("policy-dashed-button");
        addIcon(button, iconText);
        return button;
    }

    private Button createIconButton(String title, String iconText) {
        Button button = new Button(iconText);
        button.addClassName("policy-icon-button");
        button.getElement().setAttribute("title", title);
        return button;
    }

    private Button createDangerIconButton(String title, String iconText) {
        Button button = new Button(iconText);
        button.addClassNames("policy-icon-button", "policy-icon-danger-button");
        button.getElement().setAttribute("title", title);
        return button;
    }

    private void addIcon(Button button, String iconText) {
        if (iconText == null || iconText.isBlank()) {
            return;
        }

        Span icon = new Span(iconText);
        icon.addClassName("policy-button-icon");
        button.setIcon(icon);
    }

    private void showSuccess(String message) {
        Notification notification = Notification.show(message, 3500, Notification.Position.BOTTOM_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void showError(String message) {
        Notification notification = Notification.show(message, 3500, Notification.Position.BOTTOM_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private String prettyEnum(Enum<?> value) {
        return value == null ? "" : prettyEnum(value.name());
    }

    private String prettyEnum(String value) {
        if (value == null || value.isBlank()) {
            return "לא הוגדר";
        }

        return value.replace('_', ' ').toLowerCase(Locale.ROOT);
    }

    private boolean isActiveEvent() {
        return loadedEvent != null && ACTIVE_STATUS.equalsIgnoreCase(loadedEvent.status());
    }

    private String translateStatus(String status) {
        if (status == null || status.isBlank()) {
            return "לא הוגדר";
        }

        return switch (status.toUpperCase(Locale.ROOT)) {
            case "ACTIVE" -> "פעיל";
            case "DRAFT" -> "טיוטה";
            case "INACTIVE" -> "לא פעיל";
            case "CANCELLED" -> "מבוטל";
            default -> status;
        };
    }

    private StatusBadge.Type statusType(String status) {
        if (status == null) {
            return StatusBadge.Type.NEUTRAL;
        }

        return switch (status.toUpperCase(Locale.ROOT)) {
            case "ACTIVE" -> StatusBadge.Type.SUCCESS;
            case "DRAFT" -> StatusBadge.Type.INFO;
            case "CANCELLED" -> StatusBadge.Type.ERROR;
            case "INACTIVE" -> StatusBadge.Type.WARNING;
            default -> StatusBadge.Type.NEUTRAL;
        };
    }

    private String translateSaleStatus(SaleStatus status) {
        if (status == null) {
            return "לא הוגדר";
        }

        return switch (status) {
            case NOT_STARTED -> "המכירה טרם נפתחה";
            case PRE_SALE -> "מכירה מוקדמת";
            case ONGOING -> "מכירה רגילה פתוחה";
            case SOLD_OUT -> "אזל המלאי";
            case ENDED -> "המכירה הסתיימה";
        };
    }

    private StatusBadge.Type saleStatusBadgeType(String saleStatus) {
        if (saleStatus == null) {
            return StatusBadge.Type.NEUTRAL;
        }

        return switch (saleStatus.toUpperCase(Locale.ROOT)) {
            case "ONGOING" -> StatusBadge.Type.SUCCESS;
            case "PRE_SALE" -> StatusBadge.Type.INFO;
            case "SOLD_OUT", "ENDED" -> StatusBadge.Type.ERROR;
            case "NOT_STARTED" -> StatusBadge.Type.WARNING;
            default -> StatusBadge.Type.NEUTRAL;
        };
    }

    private String safeText(String value) {
        return safeText(value, "");
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private Long parseLong(String value, String fieldName) {
        try {
            long parsed = Long.parseLong(safeTrim(value));
            if (parsed <= 0) {
                throw new NumberFormatException();
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(fieldName + " חייב להיות מספר שלם חיובי.");
        }
    }

    private BigDecimal parsePrice(String value) {
        try {
            BigDecimal price = new BigDecimal(safeTrim(value).replace(',', '.'));
            if (price.compareTo(BigDecimal.ZERO) < 0) {
                throw new NumberFormatException();
            }
            return price;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("מחיר כרטיס חייב להיות מספר חיובי או אפס.");
        }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private enum EditTab {
        GENERAL,
        PURCHASE,
        DISCOUNT
    }

    public enum LogicalOperator {
        AND("וגם (AND)"),
        OR("או (OR)");

        private final String label;

        LogicalOperator(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public enum PurchaseRuleField {
        AGE("גיל"),
        MIN_TICKETS("מינימום כרטיסים"),
        MAX_TICKETS("מקסימום כרטיסים");

        private final String label;

        PurchaseRuleField(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public enum ComparisonOperator {
        GREATER_OR_EQUALS("גדול או שווה", ">="),
        LESS_OR_EQUALS("קטן או שווה", "<="),
        EQUALS("שווה", "=");

        private final String label;
        private final String symbol;

        ComparisonOperator(String label, String symbol) {
            this.label = label;
            this.symbol = symbol;
        }

        public String getLabel() {
            return label + " (" + symbol + ")";
        }

        public String getSymbol() {
            return symbol;
        }
    }

    public enum DiscountCompositionStrategy {
        MAXIMUM("מקסימום"),
        SUM("סכום");

        private final String label;

        DiscountCompositionStrategy(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public enum DiscountType {
        SIMPLE("הנחה פשוטה", "כרוכה רק בחישוב מחיר", "%"),
        CONDITIONAL("הנחה מותנית", "מוחלת אוטומטית לפי תנאי", "?"),
        COUPON("הנחת קופון", "מופעלת בעת הזנת קוד", "#");

        private final String label;
        private final String description;
        private final String iconText;

        DiscountType(String label, String description, String iconText) {
            this.label = label;
            this.description = description;
            this.iconText = iconText;
        }

        public String getLabel() {
            return label;
        }

        public String getDescription() {
            return description;
        }

        public String getIconText() {
            return iconText;
        }
    }

    public enum DiscountConditionType {
        MIN_TICKET("מינימום כרטיסים", "כמות כרטיסים >=", true, false),
        MAX_TICKET("מקסימום כרטיסים", "כמות כרטיסים <=", true, false),
        DATE("טווח תאריכים", "פעיל בין תאריך התחלה לתאריך סיום", false, true);

        private final String label;
        private final String displayPrefix;
        private final boolean requiresTicketThreshold;
        private final boolean requiresDateRange;

        DiscountConditionType(String label, String displayPrefix, boolean requiresTicketThreshold, boolean requiresDateRange) {
            this.label = label;
            this.displayPrefix = displayPrefix;
            this.requiresTicketThreshold = requiresTicketThreshold;
            this.requiresDateRange = requiresDateRange;
        }

        public String getLabel() {
            return label;
        }

        public String getDisplayPrefix() {
            return displayPrefix;
        }

        public boolean requiresTicketThreshold() {
            return requiresTicketThreshold;
        }

        public boolean requiresDateRange() {
            return requiresDateRange;
        }
    }

    public enum DiscountValueType {
        PERCENTAGE("אחוזים (%)"),
        FIXED_AMOUNT("סכום קבוע (₪)");

        private final String label;

        DiscountValueType(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public enum PurchaseNodeType {
        GROUP,
        RULE
    }

    private static class PurchaseExpressionNode {
        private final String id;
        private final PurchaseNodeType type;
        private LogicalOperator operator;
        private PurchaseRuleDTO rule;
        private final List<PurchaseExpressionNode> children = new ArrayList<>();

        private PurchaseExpressionNode(String id, PurchaseNodeType type, LogicalOperator operator, PurchaseRuleDTO rule) {
            this.id = id;
            this.type = type;
            this.operator = operator;
            this.rule = rule;
        }

        static PurchaseExpressionNode group(LogicalOperator operator) {
            return new PurchaseExpressionNode(UUID.randomUUID().toString(), PurchaseNodeType.GROUP, operator, null);
        }

        static PurchaseExpressionNode rule(PurchaseRuleDTO rule) {
            PurchaseRuleDTO safeRule = rule == null ? PurchaseRuleDTO.defaultRule() : rule;
            return new PurchaseExpressionNode(safeRule.id(), PurchaseNodeType.RULE, null, safeRule);
        }

        boolean isRule() {
            return type == PurchaseNodeType.RULE;
        }

        LogicalOperator operator() {
            return operator;
        }

        void setOperator(LogicalOperator operator) {
            this.operator = operator;
        }

        PurchaseRuleDTO rule() {
            return rule;
        }

        void setRule(PurchaseRuleDTO rule) {
            this.rule = rule;
        }

        List<PurchaseExpressionNode> children() {
            return children;
        }

        static PurchaseExpressionNode fromDraft(PurchaseExpressionNodeDTO draft) {
            if (draft == null) {
                return PurchaseExpressionNode.group(LogicalOperator.AND);
            }

            if (draft.type() == PurchaseNodeType.RULE) {
                return PurchaseExpressionNode.rule(draft.rule());
            }

            PurchaseExpressionNode node = new PurchaseExpressionNode(
                    draft.id() == null || draft.id().isBlank() ? UUID.randomUUID().toString() : draft.id(),
                    PurchaseNodeType.GROUP,
                    Objects.requireNonNullElse(draft.operator(), LogicalOperator.AND),
                    null
            );

            if (draft.children() != null) {
                for (PurchaseExpressionNodeDTO child : draft.children()) {
                    node.children().add(PurchaseExpressionNode.fromDraft(child));
                }
            }

            return node;
        }

        PurchaseExpressionNodeDTO toDraft() {
            List<PurchaseExpressionNodeDTO> childDrafts = new ArrayList<>();
            for (PurchaseExpressionNode child : children) {
                childDrafts.add(child.toDraft());
            }

            return new PurchaseExpressionNodeDTO(id, type, operator, rule, childDrafts);
        }
    }

    public record PurchasePolicyExpressionDraftDTO(String ownerId, PurchaseExpressionNodeDTO root) {
    }

    public record PurchaseExpressionNodeDTO(
            String id,
            PurchaseNodeType type,
            LogicalOperator operator,
            PurchaseRuleDTO rule,
            List<PurchaseExpressionNodeDTO> children
    ) {
    }

    public record DiscountPolicyDraftDTO(
            String ownerId,
            DiscountCompositionStrategy compositionStrategy,
            List<DiscountDTO> discounts
    ) {
    }

    public record PurchaseRuleDTO(
            String id,
            PurchaseRuleField field,
            ComparisonOperator operator,
            int value,
            String unit
    ) {
        public static PurchaseRuleDTO defaultRule() {
            return new PurchaseRuleDTO(UUID.randomUUID().toString(), PurchaseRuleField.MAX_TICKETS, ComparisonOperator.LESS_OR_EQUALS, 5, "לרוכש");
        }

        public PurchaseRuleDTO copy() {
            return new PurchaseRuleDTO(id, field, operator, value, unit);
        }

        public String toDisplayText() {
            return field.getLabel() + " " + operator.getSymbol() + " " + value + (unit == null || unit.isBlank() ? "" : " " + unit);
        }

        public String toTechnicalText() {
            return field.name() + " " + operator.getSymbol() + " " + value;
        }
    }

    public record DiscountConditionDTO(

        EditEvent.DiscountConditionType conditionType,

        Integer ticketThreshold,

        LocalDateTime startTime,

        LocalDateTime endTime

) {}

    
    public record DiscountDTO(
            String id,
            String name,
            DiscountType type,
            DiscountValueType valueType,
            double value,
            String couponCode,
            LocalDate validUntil,
            List<DiscountConditionDTO> conditions
    ) {
        public static DiscountDTO defaultDiscount() {
            return new DiscountDTO(
                    UUID.randomUUID().toString(),
                    "הנחה חדשה",
                    DiscountType.SIMPLE,
                    DiscountValueType.PERCENTAGE,
                    10,
                    "",
                    null,null
            );
        }
        public DiscountDTO withConditions(List<DiscountConditionDTO> newConditions) {
            return new DiscountDTO(
                    id,
                    name,
                    type,
                    valueType,
                    value,
                    couponCode,
                    validUntil,
                    newConditions
            );
        }

        // public String conditionText() {
        //     if (type != DiscountType.CONDITIONAL || conditionType == null) {
        //         return "";
        //     }

        //     if (conditionType.requiresTicketThreshold()) {
        //         if (ticketThreshold == null) {
        //             return "";
        //         }

        //         return conditionType.getDisplayPrefix() + " " + ticketThreshold;
        //     }

        //     if (conditionType.requiresDateRange()) {
        //         if (startTime != null && endTime != null) {
        //             return "תאריך מ-" + startTime.format(DISPLAY_DATE_TIME) + " עד " + endTime.format(DISPLAY_DATE_TIME);
        //         }

        //         if (endTime != null) {
        //             return "תאריך עד " + endTime.format(DISPLAY_DATE_TIME);
        //         }

        //         if (startTime != null) {
        //             return "תאריך מ-" + startTime.format(DISPLAY_DATE_TIME);
        //         }

        //         return "";
        //     }

        //     return "";
        // }
        public String conditionText() {
            if (type != DiscountType.CONDITIONAL || conditions == null || conditions.isEmpty()) {
                return "";
            }

            return conditions.stream()
                    .map(this::singleConditionText)
                    .filter(text -> text != null && !text.isBlank())
                    .collect(java.util.stream.Collectors.joining(" וגם "));
        }

        private String singleConditionText(DiscountConditionDTO condition) {
            if (condition == null || condition.conditionType() == null) {
                return "";
            }

            EditEvent.DiscountConditionType conditionType = condition.conditionType();

            if (conditionType.requiresTicketThreshold()) {
                if (condition.ticketThreshold() == null) {
                    return "";
                }

                return conditionType.getDisplayPrefix() + " " + condition.ticketThreshold();
            }

            if (conditionType.requiresDateRange()) {
                if (condition.startTime() != null && condition.endTime() != null) {
                    return "תאריך מ-" + condition.startTime().format(DISPLAY_DATE_TIME)
                            + " עד " + condition.endTime().format(DISPLAY_DATE_TIME);
                }

                if (condition.endTime() != null) {
                    return "תאריך עד " + condition.endTime().format(DISPLAY_DATE_TIME);
                }

                if (condition.startTime() != null) {
                    return "תאריך מ-" + condition.startTime().format(DISPLAY_DATE_TIME);
                }

                return "";
            }

            return "";
        }
        public String formattedValue() {
            if (valueType == DiscountValueType.PERCENTAGE) {
                return removeTrailingZero(value) + "%";
            }
            return "₪" + removeTrailingZero(value);
        }

        private static String removeTrailingZero(double number) {
            if (number == Math.rint(number)) {
                return String.valueOf((long) number);
            }
            return String.valueOf(number);
        }
    }

    public interface EditEventPresenter {
        EventDTO getEvent(String sessionId, Long eventId);

        boolean updateEvent(UpdateEventRequest request);

        PurchasePolicyExpressionDraftDTO loadEventPurchasePolicy(String token, Long eventId);

        void saveEventPurchasePolicy(String token, Long eventId, PurchasePolicyExpressionDraftDTO purchaseDraft);

        DiscountPolicyDraftDTO loadEventDiscountPolicy(String token, Long eventId);

        void saveEventDiscountPolicy(String token, Long eventId, DiscountPolicyDraftDTO discountDraft);

        void updateEventSaleStatus(String token, Long eventId, SaleStatus targetStatus);

        void cancelEvent(String token, Long eventId);

        boolean hasLottery(String token, Long eventId);

        void conductLottery(String token, Long eventId, Long companyId);
    }

    public record UpdateEventRequest(String sessionId, EventDTO event) {
    }
}
