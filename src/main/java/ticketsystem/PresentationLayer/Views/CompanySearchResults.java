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
import ticketsystem.DTO.Event.EventSearchResultDTO;
import ticketsystem.DomainLayer.event.SaleStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ticketsystem.PresentationLayer.Components.EmptyState;
import ticketsystem.PresentationLayer.Components.EventCard;
import ticketsystem.PresentationLayer.Components.PageContainer;
import ticketsystem.PresentationLayer.Components.SearchPanel;
import ticketsystem.PresentationLayer.Constants.Photos;
import ticketsystem.PresentationLayer.Constants.UiRoutes;
import ticketsystem.PresentationLayer.Layouts.MainLayout;

@PageTitle("TixNow | Company Events")
@Route(value = UiRoutes.COMPANY_SEARCH_RESULTS, layout = MainLayout.class)
public class CompanySearchResults extends PageContainer implements BeforeEnterObserver {

    private static final String DEFAULT_COMPANY_NAME = "חברת הפקה";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm");

    private final SearchPanel searchPanel = new SearchPanel();

    private Long companyId;
    private String currentCompanyName = DEFAULT_COMPANY_NAME;
    private String currentSearchTerm = "";

    public CompanySearchResults() {
        super();

        addClassName("search-results-view");
        addClassName("company-search-results-view");
        setSpacing(false);
        setPadding(false);

        configureSearchPanel();
        renderPage();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        companyId = parseLong(event.getRouteParameters().get("companyId").orElse(null));

        Map<String, List<String>> parameters = event.getLocation()
                .getQueryParameters()
                .getParameters();

        currentCompanyName = firstParam(parameters, "companyName", resolveCompanyName(companyId));
        currentSearchTerm = firstParam(parameters, "q", "");

        applyQueryParametersToPanel(parameters);
        renderPage();
    }

    private void configureSearchPanel() {
        searchPanel.getFreeText().setValue(currentSearchTerm);
        searchPanel.getCategory().setValue("כל הקטגוריות");
        searchPanel.getLocation().setValue("כל האזורים");

        searchPanel.getSearchButton().addClickListener(event -> navigateToCompanySearchResults());
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
        section.addClassName("company-search-results-top-area");
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
        header.addClassName("company-search-results-header");
        header.getStyle()
                .set("width", "100%")
                .set("max-width", "900px")
                .set("margin", "0 auto")
                .set("text-align", "center");

        String titleText = currentSearchTerm == null || currentSearchTerm.isBlank()
                ? "אירועים של " + currentCompanyName
                : "אירועים של " + currentCompanyName + " עבור \"" + currentSearchTerm + "\"";

        H2 title = new H2(titleText);
        title.addClassName("search-results-title");
        title.getStyle()
                .set("font-size", "24px")
                .set("line-height", "1.25")
                .set("margin", "0 0 4px")
                .set("font-weight", "700");

        int resultsCount = getFilteredCompanyEvents().size();
        Paragraph description = new Paragraph("נמצאו " + resultsCount + " אירועים של החברה שמתאימים לסינון שלך");
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

        List<EventSearchResultDTO> events = getFilteredCompanyEvents();

        if (events.isEmpty()) {
            inner.add(new EmptyState(
                    "🔎",
                    "לא נמצאו אירועים",
                    "נסה לשנות את מילות החיפוש או להסיר חלק מהסינונים.",
                    null
            ));
            section.add(inner);
            return section;
        }

        Div grid = new Div();
        grid.addClassName("events-grid");
        grid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(3, minmax(0, 1fr))")
                .set("gap", "24px")
                .set("align-items", "stretch");

        for (EventSearchResultDTO event : events) {
            grid.add(toEventCard(event));
        }

        inner.add(grid);
        section.add(inner);

        return section;
    }

    private void navigateToCompanySearchResults() {
        if (companyId == null) {
            UI.getCurrent().navigate(UiRoutes.EVENTS);
            return;
        }

        Map<String, String> params = new LinkedHashMap<>();

        if (currentCompanyName != null && !currentCompanyName.isBlank()) {
            params.put("companyName", currentCompanyName.trim());
        }

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

        UI.getCurrent().navigate(
                UiRoutes.COMPANY_SEARCH_RESULTS.replace(":companyId", String.valueOf(companyId)),
                QueryParameters.simple(params)
        );
    }

    private void applyQueryParametersToPanel(Map<String, List<String>> parameters) {
        searchPanel.getFreeText().setValue(currentSearchTerm == null ? "" : currentSearchTerm);

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

    private List<EventSearchResultDTO> getFilteredCompanyEvents() {
        return getCompanyEvents().stream()
                .filter(this::matchesSearchTerm)
                .filter(this::matchesArtist)
                .filter(this::matchesLocation)
                .filter(this::matchesCategory)
                .filter(this::matchesDateRange)
                .filter(this::matchesPriceRange)
                .filter(this::matchesRating)
                .toList();
    }

    private List<EventSearchResultDTO> getCompanyEvents() {
        long resolvedCompanyId = companyId == null ? 0 : companyId;

        if (resolvedCompanyId == 3L) {
            return List.of(
                    new EventSearchResultDTO(20L, "מרתון צחוק תל אביבי", resolvedCompanyId, LocalDateTime.of(2026, 11, 15, 22, 30), "תל אביב והמרכז", "תיאטרון וסטנדאפ", "צוות Laugh Factory", BigDecimal.valueOf(119), 4.1, SaleStatus.ENDED.name()),
                    new EventSearchResultDTO(21L, "ערב קומדיה פתוח", resolvedCompanyId, LocalDateTime.of(2026, 11, 28, 21, 30), "תל אביב והמרכז", "תיאטרון וסטנדאפ", "צוות Laugh Factory", BigDecimal.valueOf(99), 4.4, SaleStatus.ONGOING.name())
            );
        }

        if (resolvedCompanyId == 1L) {
            return List.of(
                    new EventSearchResultDTO(15L, "ליין שישי אלקטרוני", resolvedCompanyId, LocalDateTime.of(2026, 10, 20, 23, 55), "תל אביב והמרכז", "הופעות חיות", "DJ Nova", BigDecimal.valueOf(90), 4.3, SaleStatus.NOT_STARTED.name()),
                    new EventSearchResultDTO(16L, "פסטיבל קיץ פתוח", resolvedCompanyId, LocalDateTime.of(2026, 12, 2, 20, 0), "צפון", "הופעות חיות", "TixNow Live", BigDecimal.valueOf(160), 4.6, SaleStatus.ONGOING.name()),
                    new EventSearchResultDTO(17L, "מסיבת רייב לילית", resolvedCompanyId, LocalDateTime.of(2026, 12, 12, 0, 0), "דרום", "הופעות חיות", "DJ Nova & Friends", BigDecimal.valueOf(120), 4.4, SaleStatus.PRE_SALE.name()),
                    new EventSearchResultDTO(18L, "פסטיבל האורות הליליים", resolvedCompanyId, LocalDateTime.of(2026, 10, 24, 21, 0), "תל אביב והמרכז", "הופעות חיות", "The Night Lights", BigDecimal.valueOf(249), 4.8, SaleStatus.PRE_SALE.name()),
                    new EventSearchResultDTO(19L, "לילה לבן על הבמה", resolvedCompanyId, LocalDateTime.of(2026, 12, 6, 20, 30), "ירושלים", "הופעות חיות", "The Night Lights", BigDecimal.valueOf(199), 4.5, SaleStatus.ONGOING.name())
            );
        }

        return List.of(
                new EventSearchResultDTO(30L, "פסטיבל אורות הלילה", resolvedCompanyId, LocalDateTime.of(2026, 10, 24, 21, 0), "תל אביב והמרכז", "הופעות חיות", "The Night Lights", BigDecimal.valueOf(249), 4.8, SaleStatus.PRE_SALE.name()),
                new EventSearchResultDTO(31L, "לילה לבן על הבמה", resolvedCompanyId, LocalDateTime.of(2026, 12, 6, 20, 30), "ירושלים", "הופעות חיות", "The Night Lights", BigDecimal.valueOf(199), 4.5, SaleStatus.ONGOING.name()),
                new EventSearchResultDTO(32L, "After Party Neon", resolvedCompanyId, LocalDateTime.of(2026, 12, 14, 23, 0), "תל אביב והמרכז", "הופעות חיות", "DJ Nova", BigDecimal.valueOf(139), 4.2, SaleStatus.NOT_STARTED.name())
        );
    }

    private EventCard toEventCard(EventSearchResultDTO event) {
        return new EventCard(
                displayCategory(event.category()),
                safeText(event.name(), "אירוע ללא שם"),
                formatDate(event.date()),
                displayLocation(event.location()),
                formatPrice(event.ticketPrice()),
                imageForEvent(event),
                isUrgent(event),
                currentCompanyName,
                event.companyId() == null ? companyId : event.companyId(),
                event.id(),
                parseSaleStatus(event.saleStatus()),
                hasLottery(event)
        );
    }

    private boolean matchesSearchTerm(EventSearchResultDTO event) {
        if (currentSearchTerm == null || currentSearchTerm.isBlank()) {
            return true;
        }

        String search = currentSearchTerm.trim();
        return contains(event.name(), search)
                || contains(event.artistName(), search)
                || contains(event.category(), search)
                || contains(event.location(), search);
    }

    private boolean matchesArtist(EventSearchResultDTO event) {
        String artist = searchPanel.getArtist().getValue();
        if (artist == null || artist.isBlank()) {
            return true;
        }

        return contains(event.artistName(), artist);
    }

    private boolean matchesLocation(EventSearchResultDTO event) {
        String selectedLocation = searchPanel.getLocation().getValue();
        if (selectedLocation == null || selectedLocation.isBlank() || "כל האזורים".equals(selectedLocation)) {
            return true;
        }

        return equalsNormalized(selectedLocation, event.location())
                || equalsNormalized(selectedLocation, displayLocation(event.location()));
    }

    private boolean matchesCategory(EventSearchResultDTO event) {
        String selectedCategory = searchPanel.getCategory().getValue();
        if (selectedCategory == null || selectedCategory.isBlank() || "כל הקטגוריות".equals(selectedCategory)) {
            return true;
        }

        return equalsNormalized(selectedCategory, event.category())
                || equalsNormalized(selectedCategory, displayCategory(event.category()));
    }

    private boolean matchesDateRange(EventSearchResultDTO event) {
        LocalDate fromDate = searchPanel.getFromDate().getValue();
        LocalDate toDate = searchPanel.getToDate().getValue();

        if (fromDate == null && toDate == null) {
            return true;
        }

        if (event.date() == null) {
            return false;
        }

        LocalDate eventDate = event.date().toLocalDate();

        if (fromDate != null && eventDate.isBefore(fromDate)) {
            return false;
        }

        return toDate == null || !eventDate.isAfter(toDate);
    }

    private boolean matchesPriceRange(EventSearchResultDTO event) {
        BigDecimal price = event.ticketPrice() == null ? BigDecimal.ZERO : event.ticketPrice();
        BigDecimal minPrice = BigDecimal.valueOf(searchPanel.getMinPriceValue());
        BigDecimal maxPrice = BigDecimal.valueOf(searchPanel.getMaxPriceValue());

        return price.compareTo(minPrice) >= 0 && price.compareTo(maxPrice) <= 0;
    }

    private boolean matchesRating(EventSearchResultDTO event) {
        double eventRate = event.rate() == null ? 0 : event.rate();
        double companyRate = resolveCompanyRate(companyId);

        return eventRate >= searchPanel.getEventRateValue()
                && companyRate >= searchPanel.getCompanyRateValue();
    }

    private void setComboBoxValueIfPresent(com.vaadin.flow.component.combobox.ComboBox<String> comboBox, String value) {
        if (value == null || value.isBlank()) {
            return;
        }

        try {
            comboBox.setValue(value);
        } catch (IllegalArgumentException ignored) {
            // Ignore unsupported demo values until the real presenter supplies normalized values.
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

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private SaleStatus parseSaleStatus(String saleStatus) {
        if (saleStatus == null || saleStatus.isBlank()) {
            return SaleStatus.NOT_STARTED;
        }

        try {
            return SaleStatus.valueOf(saleStatus.trim().toUpperCase().replace(' ', '_').replace('-', '_'));
        } catch (IllegalArgumentException exception) {
            return SaleStatus.NOT_STARTED;
        }
    }

    private String resolveCompanyName(Long companyId) {
        if (companyId == null) {
            return DEFAULT_COMPANY_NAME;
        }

        if (companyId == 1L) {
            return "TixNow Productions";
        }

        if (companyId == 2L) {
            return "Amazing Events";
        }

        if (companyId == 3L) {
            return "Laugh Factory";
        }

        return DEFAULT_COMPANY_NAME + " " + companyId;
    }

    private double resolveCompanyRate(Long companyId) {
        if (companyId == null) {
            return 0;
        }

        if (companyId == 1L) {
            return 4.6;
        }

        if (companyId == 2L) {
            return 4.7;
        }

        if (companyId == 3L) {
            return 4.5;
        }

        return 0;
    }

    private String formatDate(LocalDateTime date) {
        if (date == null) {
            return "תאריך לא ידוע";
        }

        return date.format(DATE_TIME_FORMATTER);
    }

    private String displayLocation(String location) {
        return safeText(location, "מיקום לא ידוע").replace('_', ' ');
    }

    private String displayCategory(String category) {
        if (category == null || category.isBlank()) {
            return "אירוע";
        }

        String normalized = normalize(category);
        if (normalized.contains("standup") || normalized.contains("comedy") || normalized.contains("theater") || normalized.contains("תיאטרון") || normalized.contains("סטנדאפ")) {
            return "תיאטרון וסטנדאפ";
        }

        if (normalized.contains("sport") || normalized.contains("ספורט")) {
            return "ספורט";
        }

        if (normalized.contains("concert") || normalized.contains("music") || normalized.contains("live") || normalized.contains("הופעות") || normalized.contains("מוזיקה")) {
            return "הופעות חיות";
        }

        return category.replace('_', ' ');
    }

    private String formatPrice(BigDecimal price) {
        if (price == null) {
            return "₪0";
        }

        BigDecimal normalizedPrice = price.stripTrailingZeros();
        return "₪" + normalizedPrice.toPlainString();
    }

    private String imageForEvent(EventSearchResultDTO event) {
        String text = normalize(event.category()) + " " + normalize(event.name());

        if (text.contains("סטנדאפ") || text.contains("comedy") || text.contains("standup") || text.contains("תיאטרון")) {
            return Photos.EVENT_STANDUP;
        }

        if (text.contains("electronic") || text.contains("מסיבה") || text.contains("party") || text.contains("dj")) {
            return Photos.EVENT_ELECTRONIC;
        }

        return Photos.EVENT_LIGHTS;
    }

    private boolean isUrgent(EventSearchResultDTO event) {
        return event.rate() != null && event.rate() >= 4.7;
    }

    private boolean hasLottery(EventSearchResultDTO event) {
        return parseSaleStatus(event.saleStatus()) == SaleStatus.PRE_SALE;
    }

    private boolean contains(String value, String search) {
        return normalize(value).contains(normalize(search));
    }

    private boolean equalsNormalized(String first, String second) {
        return normalize(first).equals(normalize(second));
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.trim()
                .toLowerCase()
                .replace('_', ' ')
                .replace('-', ' ')
                .replaceAll("\\s+", " ");
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value.trim();
    }
}
