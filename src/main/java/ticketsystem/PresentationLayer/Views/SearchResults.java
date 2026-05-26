package ticketsystem.PresentationLayer.Views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ticketsystem.PresentationLayer.Components.EventCard;
import ticketsystem.PresentationLayer.Components.PageContainer;
import ticketsystem.PresentationLayer.Components.SearchPanel;
import ticketsystem.PresentationLayer.Constants.Photos;
import ticketsystem.PresentationLayer.Constants.UiRoutes;
import ticketsystem.PresentationLayer.Layouts.MainLayout;

@PageTitle("TixNow - תוצאות חיפוש")
@Route(value = UiRoutes.SEARCH_RESULTS, layout = MainLayout.class)
public class SearchResults extends PageContainer implements BeforeEnterObserver {

    private static final String DEFAULT_SEARCH_TERM = "פסטיבל מוזיקה אלקטרונית";

    private final SearchPanel searchPanel = new SearchPanel();
    private String currentSearchTerm = DEFAULT_SEARCH_TERM;

    public SearchResults() {
        super();

        addClassName("search-results-view");
        setSpacing(false);
        setPadding(false);

        configureSearchPanel();
        renderPage();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Map<String, List<String>> parameters = event.getLocation()
                .getQueryParameters()
                .getParameters();

        currentSearchTerm = firstParam(parameters, "q", DEFAULT_SEARCH_TERM);

        applyQueryParametersToPanel(parameters);
        renderPage();
    }

    private void configureSearchPanel() {
        searchPanel.getFreeText().setValue(currentSearchTerm);
        searchPanel.getCategory().setValue("כל הקטגוריות");
        searchPanel.getLocation().setValue("כל האזורים");

        searchPanel.getSearchButton().addClickListener(event -> navigateToSearchResults());
    }

    private void renderPage() {
        removeAll();

        add(
                createTopArea(),
                createResultsSection()
        );
    }

    private Div createTopArea() {
        Div section = new Div();
        section.addClassName("search-results-top-area");
        section.getStyle()
                .set("width", "100%")
                .set("box-sizing", "border-box")
                .set("padding", "24px 24px 20px")
                .set("margin", "0")
                .set("display", "flex")
                .set("justify-content", "center");

        Div inner = new Div();
        inner.addClassName("search-results-top-inner");
        inner.getStyle()
                .set("width", "100%")
                .set("max-width", "1240px")
                .set("margin", "0 auto")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("align-items", "center")
                .set("gap", "18px");

        inner.add(createCompactHeader(), searchPanel);
        section.add(inner);

        return section;
    }

    private Div createCompactHeader() {
        Div header = new Div();
        header.addClassName("search-results-compact-header");
        header.getStyle()
                .set("width", "100%")
                .set("max-width", "900px")
                .set("margin", "0 auto")
                .set("text-align", "center");

        H2 title = new H2("תוצאות עבור \"" + currentSearchTerm + "\"");
        title.addClassName("search-results-title");
        title.getStyle()
                .set("font-size", "24px")
                .set("line-height", "1.25")
                .set("margin", "0 0 4px")
                .set("font-weight", "700");

        Paragraph description = new Paragraph("נמצאו 24 אירועים קרובים שמתאימים לחיפוש שלך");
        description.addClassName("search-results-description");
        description.getStyle()
                .set("margin", "0")
                .set("font-size", "14px")
                .set("line-height", "1.4");

        header.add(title, description);
        return header;
    }

    private Div createResultsSection() {
        Div section = new Div();
        section.addClassName("search-results-events-section");
        section.getStyle()
                .set("width", "100%")
                .set("box-sizing", "border-box")
                .set("padding", "28px 24px 48px")
                .set("margin", "0");

        Div inner = new Div();
        inner.addClassName("search-results-events-inner");
        inner.getStyle()
                .set("width", "100%")
                .set("max-width", "1080px")
                .set("margin", "0 auto");

        Div grid = new Div();
        grid.addClassName("events-grid");
        grid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(3, minmax(0, 1fr))")
                .set("gap", "24px")
                .set("align-items", "stretch");

        grid.add(
                new EventCard(
                        "מופע עשור",
                        "פסטיבל אורות הלילה",
                        "24 אוקטובר, 21:00",
                        "פארק הירקון, תל אביב",
                        "₪249",
                        Photos.EVENT_LIGHTS,
                        true,
                        "Amazing Events",
                        2L,
                        30L,
                        true,
                        false,
                        false
                ),
                new EventCard(
                        "סטנדאפ",
                        "מרתון צחוק תל אביבי",
                        "15 נובמבר, 22:30",
                        "מועדון זאפה, הרצליה",
                        "₪119",
                        Photos.EVENT_STANDUP,
                        false,
                        "Laugh Factory",
                        3L,
                        20L,
                        true,
                        false,
                        true
                ),
                new EventCard(
                        "מסיבה",
                        "ליין שישי אלקטרוני",
                        "20 אוקטובר, 23:55",
                        "האומן 17, תל אביב",
                        "₪90",
                        Photos.EVENT_ELECTRONIC,
                        false,
                        "TixNow Productions",
                        1L,
                        15L,
                        false,
                        true,
                        false
                )
        );

        inner.add(grid);
        section.add(inner);

        return section;
    }

    private void navigateToSearchResults() {
        Map<String, String> params = new LinkedHashMap<>();

        String term = searchPanel.getFreeText().getValue();
        if (term != null && !term.isBlank()) {
            params.put("q", term.trim());
        }

        if (searchPanel.getFromDate().getValue() != null) {
            params.put("fromDate", searchPanel.getFromDate().getValue().toString());
        }

        if (searchPanel.getToDate().getValue() != null) {
            params.put("toDate", searchPanel.getToDate().getValue().toString());
        }

        String selectedLocation = searchPanel.getLocation().getValue();
        if (selectedLocation != null && !"כל האזורים".equals(selectedLocation)) {
            params.put("location", selectedLocation);
        }

        String selectedCategory = searchPanel.getCategory().getValue();
        if (selectedCategory != null && !"כל הקטגוריות".equals(selectedCategory)) {
            params.put("category", selectedCategory);
        }

        String artist = searchPanel.getArtist().getValue();
        if (artist != null && !artist.isBlank()) {
            params.put("artist", artist.trim());
        }

        if (searchPanel.getMinPriceValue() > 0) {
            params.put("minPrice", String.valueOf(searchPanel.getMinPriceValue()));
        }

        if (searchPanel.getMaxPriceValue() < searchPanel.getMaxPriceLimit()) {
            params.put("maxPrice", String.valueOf(searchPanel.getMaxPriceValue()));
        }

        if (searchPanel.getEventRateValue() > 0) {
            params.put("eventRate", String.valueOf(searchPanel.getEventRateValue()));
        }

        if (searchPanel.getCompanyRateValue() > 0) {
            params.put("companyRate", String.valueOf(searchPanel.getCompanyRateValue()));
        }

        if (params.isEmpty()) {
            UI.getCurrent().navigate(UiRoutes.SEARCH_RESULTS);
            return;
        }

        UI.getCurrent().navigate(
                UiRoutes.SEARCH_RESULTS,
                QueryParameters.simple(params)
        );
    }

    private void applyQueryParametersToPanel(Map<String, List<String>> parameters) {
        searchPanel.getFreeText().setValue(currentSearchTerm);

        setComboBoxValueIfPresent(searchPanel.getLocation(), firstParam(parameters, "location", null));
        setComboBoxValueIfPresent(searchPanel.getCategory(), firstParam(parameters, "category", null));

        searchPanel.getFromDate().setValue(parseDate(firstParam(parameters, "fromDate", null)));
        searchPanel.getToDate().setValue(parseDate(firstParam(parameters, "toDate", null)));

        String artist = firstParam(parameters, "artist", "");
        searchPanel.getArtist().setValue(artist);

        boolean hasAdvancedFilters = false;

        Double minPrice = parseDouble(firstParam(parameters, "minPrice", null));
        if (minPrice != null) {
            searchPanel.setMinPriceValue(minPrice);
            hasAdvancedFilters = true;
        }

        Double maxPrice = parseDouble(firstParam(parameters, "maxPrice", null));
        if (maxPrice != null) {
            searchPanel.setMaxPriceValue(maxPrice);
            hasAdvancedFilters = true;
        }

        Double eventRate = parseDouble(firstParam(parameters, "eventRate", null));
        if (eventRate != null) {
            searchPanel.setEventRateValue(eventRate);
            hasAdvancedFilters = true;
        }

        Double companyRate = parseDouble(firstParam(parameters, "companyRate", null));
        if (companyRate != null) {
            searchPanel.setCompanyRateValue(companyRate);
            hasAdvancedFilters = true;
        }

        if (artist != null && !artist.isBlank()) {
            hasAdvancedFilters = true;
        }

        searchPanel.setAdvancedFiltersVisible(hasAdvancedFilters);
    }

    private void setComboBoxValueIfPresent(com.vaadin.flow.component.combobox.ComboBox<String> comboBox, String value) {
        if (value != null && !value.isBlank()) {
            comboBox.setValue(value);
        }
    }

    private String firstParam(Map<String, List<String>> parameters, String key, String fallback) {
        List<String> values = parameters.get(key);

        if (values == null || values.isEmpty()) {
            return fallback;
        }

        String value = values.get(0);

        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value.trim();
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(value);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}