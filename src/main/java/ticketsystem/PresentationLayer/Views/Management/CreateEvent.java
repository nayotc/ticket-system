package ticketsystem.PresentationLayer.Views.Management;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;
import ticketsystem.DomainLayer.event.EventCategory;
import ticketsystem.DomainLayer.event.EventLocation;
import ticketsystem.PresentationLayer.Components.AppCard;
import ticketsystem.PresentationLayer.Components.Notifications;
import ticketsystem.PresentationLayer.Components.PageContainer;
import ticketsystem.PresentationLayer.Components.ViewHeader;
import ticketsystem.PresentationLayer.Constants.UiRoutes;
import ticketsystem.PresentationLayer.Layouts.ManagementLayout;
import ticketsystem.PresentationLayer.Presenters.PresentationException;
import ticketsystem.PresentationLayer.Session.UiSession;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

@Route(value = UiRoutes.CREATE_EVENT, layout = ManagementLayout.class)
public class CreateEvent extends PageContainer implements BeforeEnterObserver {

    private Long companyId;
    private CreateEventPresenter presenter;

    private final TextField eventName = new TextField("שם האירוע");
    private final TextField artistName = new TextField("שם האמן / המופע");
    private final ComboBox<EventCategory> category = new ComboBox<>("קטגוריה");
    private final DatePicker eventDate = new DatePicker("תאריך");
    private final TimePicker eventTime = new TimePicker("שעה");
    private final ComboBox<EventLocation> location = new ComboBox<>("מיקום");
    private final TextField ticketPrice = new TextField("מחיר כרטיס");
    private final TextField trafficThreshold = new TextField("רף עומס");
    private final ComboBox<SaleOpeningOption> saleOpening = new ComboBox<>("אופן פתיחת המכירה");
    private final IntegerField lotteryWinnersNumber = new IntegerField("מספר זוכים בהגרלה");
    private final ComboBox<MapSizeOption> mapSize = new ComboBox<>("גודל מפה");

    private final Span selectedMapSizeText = new Span();
    private final Div mapGridPreview = new Div();

    public CreateEvent() {
        this(null);
    }

    @Autowired
    public CreateEvent(CreateEventPresenter presenter) {
        this.presenter = presenter;

        getElement().setAttribute("dir", "rtl");
        addClassName("create-event-page");

        configureFields();

        add(
                createAmbientBackground(),
                createHeader(),
                createFormCard()
        );
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        event.getRouteParameters()
                .get("companyId")
                .ifPresent(this::setCompanyIdFromRoute);
    }

    public void setPresenter(CreateEventPresenter presenter) {
        this.presenter = presenter;
    }

    private void setCompanyIdFromRoute(String value) {
        try {
            companyId = Long.valueOf(value);
        } catch (NumberFormatException e) {
            showError("מזהה החברה בכתובת אינו תקין.");
        }
    }

    private void configureFields() {
        eventName.setPlaceholder("לדוגמה: Neon Nights Festival");
        artistName.setPlaceholder("לדוגמה: עומר אדם, Coldplay, Tech Summit");
        ticketPrice.setPlaceholder("לדוגמה: 149.90");
        trafficThreshold.setPlaceholder("לדוגמה: 1000");
        lotteryWinnersNumber.setPlaceholder("לדוגמה: 250");

        eventName.setPrefixComponent(VaadinIcon.TICKET.create());
        artistName.setPrefixComponent(VaadinIcon.MICROPHONE.create());
        ticketPrice.addClassName("create-event-price-field");
        trafficThreshold.addClassName("create-event-traffic-field");

        ticketPrice.setPrefixComponent(VaadinIcon.MONEY.create());
        trafficThreshold.setPrefixComponent(VaadinIcon.USERS.create());

        Span shekel = new Span("₪");
        shekel.addClassName("create-event-price-currency");
        ticketPrice.setSuffixComponent(shekel);

        ticketPrice.getElement().setAttribute("inputmode", "decimal");
        trafficThreshold.getElement().setAttribute("inputmode", "numeric");

        category.setItems(EventCategory.values());
        category.setItemLabelGenerator(this::prettyEnum);

        location.setItems(EventLocation.values());
        location.setItemLabelGenerator(this::prettyEnum);

        saleOpening.setItems(saleOpeningOptions());
        saleOpening.setItemLabelGenerator(SaleOpeningOption::label);
        saleOpening.setValue(SaleOpeningOption.REGULAR);
        saleOpening.addValueChangeListener(event -> refreshSaleOpeningFields());

        lotteryWinnersNumber.setMin(1);
        lotteryWinnersNumber.setStep(1);
        lotteryWinnersNumber.setStepButtonsVisible(true);
        lotteryWinnersNumber.setVisible(false);
        lotteryWinnersNumber.getElement().setAttribute("dir", "rtl");

        eventDate.setMin(LocalDate.now());
        eventTime.setStep(Duration.ofMinutes(15));

        mapSize.setItems(mapSizeOptions());
        mapSize.setItemLabelGenerator(MapSizeOption::label);
        mapSize.setValue(mapSizeOptions().get(1));
        mapSize.addValueChangeListener(event -> refreshMapPreview());

        List<Component> requiredFields = List.of(
                eventName,
                artistName,
                category,
                eventDate,
                eventTime,
                location,
                ticketPrice,
                trafficThreshold,
                saleOpening,
                mapSize
        );

        requiredFields.forEach(component -> {
            component.getElement().setAttribute("dir", "rtl");
            component.getElement().getStyle().set("width", "100%");
        });

        lotteryWinnersNumber.getElement().getStyle().set("width", "100%");

        refreshSaleOpeningFields();
        refreshMapPreview();
    }

    private Component createAmbientBackground() {
        Div background = new Div();
        background.addClassName("create-event-background");

        Div image = new Div();
        image.addClassName("create-event-background-image");

        Div fade = new Div();
        fade.addClassName("create-event-background-fade");

        background.add(image, fade);
        return background;
    }

    private Component createHeader() {
        Div progress = new Div();
        progress.addClassName("create-event-progress");

        Div labels = new Div();
        labels.addClassName("create-event-progress-labels");

        Span step = new Span("שלב 1 מתוך 2");
        step.addClassName("create-event-step-label");

        Span title = new Span("פרטי אירוע");
        title.addClassName("create-event-step-title");

        labels.add(step, title);

        Div bars = new Div();
        bars.addClassName("create-event-progress-bars");

        Div active = new Div();
        active.addClassNames("create-event-progress-bar", "create-event-progress-bar-active");

        Div inactive = new Div();
        inactive.addClassNames("create-event-progress-bar", "create-event-progress-bar-inactive");

        bars.add(active, inactive);
        progress.add(labels, bars);

        return new ViewHeader(
                "יצירת אירוע חדש",
                "הגדר את פרטי האירוע . לאחר מכן ניתן יהיה להמשיך לבניית מפת האולם והמלאי.",
                progress
        );
    }

    private Component createFormCard() {
        AppCard card = new AppCard();
        card.addClassName("create-event-card");

        Div topBorder = new Div();
        topBorder.addClassName("create-event-card-top-border");

        Div grid = new Div();
        grid.addClassName("create-event-grid");

        grid.add(createEventDetailsSection(), createMapSection());

        Div actions = createActions();

        card.add(topBorder, grid, actions);
        return card;
    }

    private Component createEventDetailsSection() {
        VerticalLayout section = new VerticalLayout();
        section.addClassName("create-event-section");
        section.setPadding(false);
        section.setSpacing(false);

        H3 title = new H3("פרטי האירוע");
        title.addClassName("create-event-section-title");

        Div twoColumns = new Div();
        twoColumns.addClassName("create-event-two-columns");
        twoColumns.add(eventDate, eventTime);

        section.add(
                title,
                eventName,
                artistName,
                category,
                twoColumns,
                location,
                ticketPrice,
                trafficThreshold
        );

        return section;
    }

    private Component createSaleOpeningSection() {
        Div wrapper = new Div();
        wrapper.addClassName("create-event-sale-options");

        H3 title = new H3("פתיחת מכירה");
        title.addClassName("create-event-subsection-title");

        Paragraph description = new Paragraph("בחר האם האירוע ייפתח במכירה רגילה או בהגרלת זכות רכישה למכירה מוקדמת.");
        description.addClassName("create-event-section-subtitle");

        Paragraph lotteryHint = new Paragraph("בהגרלה, רק מנויים שיעלו בגורל יקבלו קוד רכישה למכירה המוקדמת.");
        lotteryHint.addClassName("create-event-sale-hint");

        wrapper.add(title, description, saleOpening, lotteryWinnersNumber, lotteryHint);
        return wrapper;
    }

    private Component createMapSection() {
        VerticalLayout section = new VerticalLayout();
        section.addClassName("create-event-section");
        section.setPadding(false);
        section.setSpacing(false);

        H3 title = new H3("גודל מפת האירוע");
        title.addClassName("create-event-section-title");

        Paragraph subtitle = new Paragraph("בחר את גודל שטח העבודה של המפה. בשלב הבא תוכל להוסיף אזורי ישיבה, עמידה, במה וכניסות.");
        subtitle.addClassName("create-event-section-subtitle");

        Div preview = new Div();
        preview.addClassName("create-event-map-preview");

        Div previewOverlay = new Div();
        previewOverlay.addClassName("create-event-map-preview-overlay");

        mapGridPreview.addClassName("create-event-map-grid-preview");

        previewOverlay.add(mapGridPreview);
        preview.add(previewOverlay);

        section.add(
                title,
                subtitle,
                mapSize,
                preview,
                createMapInfoCard(),
                createSaleOpeningSection()
        );

        return section;
    }

    private Component createMapInfoCard() {
        Div card = new Div();
        card.addClassName("create-event-map-info-card");

        Div first = createInfoRow(VaadinIcon.GRID_SMALL, "גודל המפה ישמש כבסיס לבונה האולם.");
        Div second = createInfoRow(VaadinIcon.TICKET, "המלאי עצמו יוגדר בשלב הבא דרך אזורי ישיבה / עמידה.");
        Div third = createInfoRow(VaadinIcon.USERS, "רף העומס יקבע מתי להפעיל תור וירטואלי לאירוע.");

        card.add(first, second, third);
        return card;
    }

    private Div createInfoRow(VaadinIcon icon, String text) {
        Div row = new Div();
        row.addClassName("create-event-info-row");
        row.add(icon.create(), new Span(text));
        return row;
    }

    private Div createActions() {
        Button cancel = new Button("ביטול");
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cancel.addClassName("create-event-secondary-button");
        cancel.addClickListener(event -> navigateToCompanyManagement());

        Button submit = new Button("המשך לבניית מפת האולם");
        submit.setIcon(VaadinIcon.ARROW_BACKWARD.create());
        submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submit.addClassName("create-event-primary-button");
        submit.addClickListener(event -> submitForm());

        Div actions = new Div(cancel, submit);
        actions.addClassName("create-event-actions");

        return actions;
    }

    private void submitForm() {
        try {
            CreateEventRequest request = readRequestFromFields();

            Long eventId = presenter.createEvent(request);

            if (eventId != null) {
                showSuccess("טיוטת האירוע נוצרה בהצלחה. כעת ניתן להגדיר את מלאי הכרטיסים");
                UI.getCurrent().navigate(
                        UiRoutes.HALL_MAP_BUILDER
                                .replace(":companyId", String.valueOf(companyId))
                                .replace(":eventId", String.valueOf(eventId))
                );
            } else {
                showError("אירעה שגיאה בעת יצירת האירוע. נסו שוב.");
                navigateToCompanyManagement();
            }
        } catch (PresentationException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            showError("אירעה שגיאה לא צפויה בעת יצירת האירוע.");
        }
    }

    private CreateEventRequest readRequestFromFields() {
        String sessionToken = UiSession.getMemberToken();

        if (sessionToken == null || sessionToken.isBlank()) {
            throw new PresentationException("יש להתחבר למערכת לפני יצירת אירוע.");
        }

        if (companyId == null) {
            throw new PresentationException("לא נמצא מזהה חברה תקין.");
        }

        String name = readRequiredText(eventName, "שם האירוע");
        String artist = readRequiredText(artistName, "שם האמן / המופע");
        EventCategory selectedCategory = readRequiredValue(category, "קטגוריה");
        EventLocation selectedLocation = readRequiredValue(location, "מיקום");
        LocalDateTime dateTime = readDateTime();
        BigDecimal price = readPrice();
        Long threshold = readTrafficThreshold();
        SaleOpeningOption selectedSaleOpening = readRequiredValue(saleOpening, "אופן פתיחת המכירה");
        Integer winnersNumber = readLotteryWinnersNumber(selectedSaleOpening);
        MapSizeOption selectedMapSize = readRequiredValue(mapSize, "גודל מפה");

        if (dateTime.isBefore(LocalDateTime.now())) {
            throw new PresentationException("תאריך האירוע חייב להיות עתידי.");
        }

        return new CreateEventRequest(
                sessionToken,
                companyId,
                name,
                dateTime,
                selectedLocation,
                threshold,
                selectedCategory,
                artist,
                price,
                selectedMapSize.height(),
                selectedMapSize.width(),
                selectedSaleOpening,
                winnersNumber
        );
    }

    private String readRequiredText(TextField field, String label) {
        String value = field.getValue();

        if (value == null || value.isBlank()) {
            throw new PresentationException("חובה למלא " + label + ".");
        }

        return value.trim();
    }

    private <T> T readRequiredValue(ComboBox<T> field, String label) {
        T value = field.getValue();

        if (value == null) {
            throw new PresentationException("חובה לבחור " + label + ".");
        }

        return value;
    }

    private LocalDateTime readDateTime() {
        LocalDate date = eventDate.getValue();
        LocalTime time = eventTime.getValue();

        if (date == null) {
            throw new PresentationException("חובה לבחור תאריך.");
        }

        if (time == null) {
            throw new PresentationException("חובה לבחור שעה.");
        }

        return LocalDateTime.of(date, time);
    }

    private BigDecimal readPrice() {
        String rawValue = ticketPrice.getValue();

        if (rawValue == null || rawValue.isBlank()) {
            throw new PresentationException("חובה להזין מחיר כרטיס.");
        }

        try {
            BigDecimal price = new BigDecimal(rawValue.trim());

            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                throw new PresentationException("מחיר כרטיס חייב להיות מספר חיובי.");
            }

            return price.setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            throw new PresentationException("מחיר כרטיס חייב להיות מספר תקין.");
        }
    }

    private Long readTrafficThreshold() {
        String rawValue = trafficThreshold.getValue();

        if (rawValue == null || rawValue.isBlank()) {
            throw new PresentationException("חובה להזין רף עומס.");
        }

        try {
            long value = Long.parseLong(rawValue.trim());

            if (value <= 0) {
                throw new PresentationException("רף עומס חייב להיות מספר חיובי.");
            }

            return value;
        } catch (NumberFormatException e) {
            throw new PresentationException("רף עומס חייב להיות מספר שלם תקין.");
        }
    }

    private Integer readLotteryWinnersNumber(SaleOpeningOption selectedSaleOpening) {
        if (selectedSaleOpening == null || !selectedSaleOpening.hasLottery()) {
            return null;
        }

        Integer value = lotteryWinnersNumber.getValue();

        if (value == null || value <= 0) {
            throw new PresentationException("כאשר נבחרת הגרלה חובה להזין מספר זוכים חיובי.");
        }

        return value;
    }

    private void refreshSaleOpeningFields() {
        SaleOpeningOption selected = saleOpening.getValue();
        boolean lotteryMode = selected != null && selected.hasLottery();

        lotteryWinnersNumber.setVisible(lotteryMode);

        if (!lotteryMode) {
            lotteryWinnersNumber.clear();
        }
    }

    private void refreshMapPreview() {
        MapSizeOption selected = mapSize.getValue();

        if (selected == null) {
            selectedMapSizeText.setText("");
            mapGridPreview.removeAll();
            return;
        }

        selectedMapSizeText.setText("");
        mapGridPreview.removeAll();

        Div canvas = new Div();
        canvas.addClassName("create-event-proportional-canvas");

        int height = selected.height();
        int width = selected.width();

        double ratio = width / (double) height;

        int previewHeight = switch (selected.name()) {
            case "קטנה" -> 150;
            case "בינונית" -> 180;
            case "גדולה" -> 215;
            default -> 235;
        };

        int previewWidth = (int) Math.round(previewHeight * ratio);

        canvas.getStyle().set("width", previewWidth + "px");
        canvas.getStyle().set("height", previewHeight + "px");

        Div stage = new Div();
        stage.addClassName("create-event-proportional-stage");
        stage.setText("במה");

        Div dimensions = new Div();
        dimensions.addClassName("create-event-proportional-dimensions");
        dimensions.setText(height + "X" + width);

        canvas.add(stage, dimensions);
        mapGridPreview.add(canvas);
    }

    private List<SaleOpeningOption> saleOpeningOptions() {
        return List.of(
                SaleOpeningOption.REGULAR,
                SaleOpeningOption.LOTTERY_PRE_SALE
        );
    }

    private List<MapSizeOption> mapSizeOptions() {
        return List.of(
                new MapSizeOption("קטנה", "אולם קטן / מופע אינטימי", 20, 30),
                new MapSizeOption("בינונית", "אולם רגיל / תיאטרון", 40, 60),
                new MapSizeOption("גדולה", "היכל / אירוע גדול", 80, 120),
                new MapSizeOption("ענקית", "פסטיבל / מתחם פתוח", 120, 160)
        );
    }

    private String prettyEnum(Enum<?> value) {
        if (value == null) {
            return "";
        }

        return Arrays.stream(value.name().toLowerCase().split("_"))
                .map(part -> part.isBlank()
                        ? part
                        : part.substring(0, 1).toUpperCase() + part.substring(1))
                .reduce((left, right) -> left + " " + right)
                .orElse(value.name());
    }

    private void navigateToCompanyManagement() {
        if (companyId == null) {
            UI.getCurrent().navigate(UiRoutes.HOME);
            return;
        }

        UI.getCurrent().navigate(
                UiRoutes.COMPANY_MANAGEMENT.replace(":companyId", String.valueOf(companyId))
        );
    }

    private void showSuccess(String message) {
        Notifications.success(message);
    }

    private void showInfo(String message) {
        Notifications.info(message);
    }

    private void showError(String message) {
        Notifications.error(message);
    }

    public interface CreateEventPresenter {
        Long createEvent(CreateEventRequest request);
    }

    public record CreateEventRequest(
            String sessionId,
            Long companyId,
            String eventName,
            LocalDateTime date,
            EventLocation location,
            Long trafficThreshold,
            EventCategory category,
            String artist,
            BigDecimal price,
            Integer mapHeight,
            Integer mapWidth,
            SaleOpeningOption saleOpening,
            Integer lotteryWinnersNumber
    ) {

        public boolean hasLottery() {
            return saleOpening != null && saleOpening.hasLottery();
        }
    }

    public enum SaleOpeningOption {
        REGULAR("מכירה רגילה", false),
        LOTTERY_PRE_SALE("הגרלת זכות רכישה למכירה מוקדמת", true);

        private final String label;
        private final boolean hasLottery;

        SaleOpeningOption(String label, boolean hasLottery) {
            this.label = label;
            this.hasLottery = hasLottery;
        }

        public String label() {
            return label;
        }

        public boolean hasLottery() {
            return hasLottery;
        }
    }

    private record MapSizeOption(
            String name,
            String description,
            Integer height,
            Integer width
    ) {
        private String label() {
            return name + " - " + description;
        }
    }
}