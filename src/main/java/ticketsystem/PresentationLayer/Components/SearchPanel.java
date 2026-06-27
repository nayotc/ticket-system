package ticketsystem.PresentationLayer.Components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;

import java.time.LocalDate;

public class SearchPanel extends AppCard {

    private static final double MAX_PRICE_LIMIT = 1000.0;

    private final TextField freeText = new TextField("חיפוש חופשי");
    private final ComboBox<String> location = new ComboBox<>("אזור");
    private final DatePicker fromDate = new DatePicker("מתאריך");
    private final DatePicker toDate = new DatePicker("עד תאריך");
    private final ComboBox<String> category = new ComboBox<>("קטגוריה");

    private final Button searchButton = new Button("חפש אירועים");
    private final Button advancedToggle = new Button("סינון מתקדם");

    private final Div advancedFilters = new Div();

    private final TextField artist = new TextField("אמן");

    private final Input minPrice = new Input();
    private final Input maxPrice = new Input();
    private final Div priceTrack = new Div();
    private final Span priceRangeValue = new Span();

    private final Input eventRate = new Input();
    private final Input companyRate = new Input();

    private final Span eventRateValue = new Span();
    private final Span companyRateValue = new Span();

    public SearchPanel() {
        super();

        addClassName("search-panel");

        getStyle()
                .set("width", "min(1240px, calc(100vw - 48px))")
                .set("max-width", "1240px")
                .set("box-sizing", "border-box")
                .set("margin", "0 auto")
                .set("padding", "24px 28px")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "18px");

        configureMainFields();
        configureActions();
        configureAdvancedFields();

        add(createMainRow(), advancedFilters);
    }

    private void configureMainFields() {
        freeText.setPlaceholder("שם אירוע או אמן...");
        freeText.setPrefixComponent(VaadinIcon.SEARCH.create());

        location.setItems("כל האזורים", "ניו יורק", "לוס אנג׳לס", "שיקגו", "יוסטון", "מיאמי", "תל אביב", "ירושלים", "באר שבע", "חיפה", "אחר");
        location.setValue("כל האזורים");

        fromDate.setPlaceholder("תאריך התחלה");
        fromDate.setClearButtonVisible(true);
        fromDate.setMin(LocalDate.now());
        fromDate.addValueChangeListener(event -> enforceDateRange());

        toDate.setPlaceholder("תאריך סיום");
        toDate.setClearButtonVisible(true);
        toDate.setMin(LocalDate.now());
        toDate.addValueChangeListener(event -> enforceDateRange());

        category.setItems(
                "כל הקטגוריות",
                "הופעה",
                "ספורט",
                "תיאטרון",
                "תערוכה",
                "אחר"
        );
        category.setValue("כל הקטגוריות");
    }

    private void enforceDateRange() {
        LocalDate today = LocalDate.now();
        LocalDate from = fromDate.getValue();
        LocalDate to = toDate.getValue();

        if (from != null && from.isBefore(today)) {
            fromDate.setValue(today);
            return;
        }

        LocalDate earliestToDate = from == null ? today : from;
        toDate.setMin(earliestToDate);

        if (to != null && to.isBefore(earliestToDate)) {
            toDate.setValue(earliestToDate);
        }
    }

    private void configureActions() {
        searchButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        searchButton.setIcon(VaadinIcon.SEARCH.create());
        searchButton.setIconAfterText(true);
        searchButton.getStyle()
                .set("height", "44px")
                .set("width", "150px")
                .set("white-space", "nowrap");

        advancedToggle.setIcon(VaadinIcon.FILTER.create());
        advancedToggle.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        advancedToggle.getStyle()
                .set("height", "44px")
                .set("width", "150px")
                .set("white-space", "nowrap");

        advancedToggle.addClickListener(event -> toggleAdvancedFilters());
    }

    private void configureAdvancedFields() {
        artist.setPlaceholder("שם אמן...");

        configureRangeInput(minPrice, "0", "0", String.valueOf((int) MAX_PRICE_LIMIT), "10");
        configureRangeInput(maxPrice, String.valueOf((int) MAX_PRICE_LIMIT), "0", String.valueOf((int) MAX_PRICE_LIMIT), "10");
        configureRangeInput(eventRate, "0", "0", "5", "0.5");
        configureRangeInput(companyRate, "0", "0", "5", "0.5");

        minPrice.addClassName("dual-range-input");
        minPrice.addClassName("dual-range-min");

        maxPrice.addClassName("dual-range-input");
        maxPrice.addClassName("dual-range-max");

        addRangeListener(minPrice, () -> {
            if (getMinPriceValue() > getMaxPriceValue()) {
                setMaxPriceValue(getMinPriceValue());
            }
            updatePriceValues();
        });

        addRangeListener(maxPrice, () -> {
            if (getMaxPriceValue() < getMinPriceValue()) {
                setMinPriceValue(getMaxPriceValue());
            }
            updatePriceValues();
        });

        addRangeListener(eventRate, this::updateRateValues);
        addRangeListener(companyRate, this::updateRateValues);

        updatePriceValues();
        updateRateValues();

        advancedFilters.addClassName("search-advanced-filters");
        advancedFilters.setVisible(false);

        advancedFilters.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "minmax(220px, 1fr) minmax(340px, 1.5fr) minmax(220px, 1fr) minmax(220px, 1fr)")
                .set("gap", "18px")
                .set("align-items", "end")
                .set("width", "100%")
                .set("padding-top", "18px")
                .set("border-top", "1px solid rgba(255,255,255,0.10)")
                .set("direction", "rtl");

        advancedFilters.add(
                artist,
                priceRangeBlock(),
                rangeBlock("דירוג אירוע", eventRate, eventRateValue),
                rangeBlock("דירוג חברת הפקה", companyRate, companyRateValue)
        );
    }

    private Div createMainRow() {
        Div row = new Div(
                freeText,
                location,
                fromDate,
                toDate,
                category,
                createActionsColumn()
        );

        row.addClassName("search-fields");
        row.getStyle()
                .set("width", "100%")
                .set("display", "grid")
                .set("grid-template-columns",
                        "minmax(230px, 1.6fr) minmax(145px, 1fr) minmax(145px, 1fr) minmax(145px, 1fr) minmax(165px, 1fr) 150px")
                .set("gap", "14px")
                .set("align-items", "start")
                .set("direction", "rtl");

        return row;
    }

    private Div createActionsColumn() {
        Div actions = new Div(searchButton, advancedToggle);

        actions.addClassName("search-panel-actions-column");
        actions.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "8px")
                .set("padding-top", "0")
                .set("align-items", "stretch");

        return actions;
    }

    private Div priceRangeBlock() {
        Span label = new Span("טווח מחירים");
        label.getStyle()
                .set("font-size", "13px")
                .set("font-weight", "700")
                .set("color", "var(--lumo-body-text-color)");

        Div rangeWrapper = new Div(priceTrack, minPrice, maxPrice);
        rangeWrapper.addClassName("dual-range");
        rangeWrapper.getStyle()
                .set("position", "relative")
                .set("height", "34px")
                .set("width", "100%")
                .set("margin", "4px 0")
                .set("direction", "ltr");


        priceTrack.addClassName("dual-range-track");
        priceTrack.getStyle()
                .set("position", "absolute")
                .set("left", "0")
                .set("right", "0")
                .set("top", "14px")
                .set("height", "6px")
                .set("border-radius", "999px");

        priceRangeValue.addClassName("range-value");
        priceRangeValue.getStyle()
                .set("font-size", "13px")
                .set("color", "var(--lumo-secondary-text-color)");

        Div block = new Div(label, rangeWrapper, priceRangeValue);
        block.addClassName("advanced-range-block");
        block.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "6px")
                .set("width", "100%");

        return block;
    }

    private Div rangeBlock(String labelText, Input input, Span value) {
        Span label = new Span(labelText);
        label.getStyle()
                .set("font-size", "13px")
                .set("font-weight", "700")
                .set("color", "var(--lumo-body-text-color)");

        value.addClassName("range-value");
        value.getStyle()
                .set("font-size", "13px")
                .set("color", "var(--lumo-secondary-text-color)");

        Div block = new Div(label, input, value);
        block.addClassName("advanced-range-block");
        block.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "6px")
                .set("width", "100%");

        return block;
    }

    private void toggleAdvancedFilters() {
        setAdvancedFiltersVisible(!advancedFilters.isVisible());
    }

    public void setAdvancedFiltersVisible(boolean visible) {
        advancedFilters.setVisible(visible);
        advancedToggle.setText(visible ? "סגור סינון" : "סינון מתקדם");
    }

    private void configureRangeInput(Input input, String value, String min, String max, String step) {
        input.getElement().setAttribute("type", "range");
        input.getElement().setAttribute("min", min);
        input.getElement().setAttribute("max", max);
        input.getElement().setAttribute("step", step);
        input.getElement().setProperty("value", value);

        input.getStyle()
                .set("width", "100%")
                .set("cursor", "pointer");
    }

    private void addRangeListener(Input input, Runnable action) {
        input.getElement()
                .addEventListener("input", event -> {
                    String value = event.getEventData().getString("element.value");
                    input.getElement().setProperty("value", value);
                    action.run();
                })
                .addEventData("element.value");
    }

    private void updatePriceValues() {
        long min = Math.round(getMinPriceValue());
        long max = Math.round(getMaxPriceValue());

        priceRangeValue.setText("טווח: ₪" + min + " - ₪" + max);

        double minPercent = (getMinPriceValue() / MAX_PRICE_LIMIT) * 100.0;
        double maxPercent = (getMaxPriceValue() / MAX_PRICE_LIMIT) * 100.0;

        priceTrack.getStyle().set(
                "background",
                "linear-gradient(to right, "
                        + "rgba(255,255,255,0.24) 0%, "
                        + "rgba(255,255,255,0.24) " + minPercent + "%, "
                        + "var(--lumo-primary-color) " + minPercent + "%, "
                        + "var(--lumo-primary-color) " + maxPercent + "%, "
                        + "rgba(255,255,255,0.24) " + maxPercent + "%, "
                        + "rgba(255,255,255,0.24) 100%)"
        );
    }

    private void updateRateValues() {
        eventRateValue.setText("דירוג מינימלי: " + getEventRateValue());
        companyRateValue.setText("דירוג מינימלי: " + getCompanyRateValue());
    }

    private double readRangeValue(Input input) {
        String value = input.getElement().getProperty("value");

        if (value == null || value.isBlank()) {
            return 0;
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private void setRangeValue(Input input, double value) {
        input.getElement().setProperty("value", String.valueOf(value));
    }

    public TextField getFreeText() {
        return freeText;
    }

    public ComboBox<String> getLocation() {
        return location;
    }

    public DatePicker getFromDate() {
        return fromDate;
    }

    public DatePicker getToDate() {
        return toDate;
    }

    public ComboBox<String> getCategory() {
        return category;
    }

    public TextField getArtist() {
        return artist;
    }

    public Button getSearchButton() {
        return searchButton;
    }

    public double getMinPriceValue() {
        return readRangeValue(minPrice);
    }

    public double getMaxPriceValue() {
        return readRangeValue(maxPrice);
    }

    public double getEventRateValue() {
        return readRangeValue(eventRate);
    }

    public double getCompanyRateValue() {
        return readRangeValue(companyRate);
    }

    public void setMinPriceValue(double value) {
        setRangeValue(minPrice, value);
        updatePriceValues();
    }

    public void setMaxPriceValue(double value) {
        setRangeValue(maxPrice, value);
        updatePriceValues();
    }

    public void setEventRateValue(double value) {
        setRangeValue(eventRate, value);
        updateRateValues();
    }

    public void setCompanyRateValue(double value) {
        setRangeValue(companyRate, value);
        updateRateValues();
    }

    public double getMaxPriceLimit() {
        return MAX_PRICE_LIMIT;
    }
}