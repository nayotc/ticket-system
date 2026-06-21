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
import com.vaadin.flow.component.notification.Notification;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import ticketsystem.PresentationLayer.Components.EmptyState;
import ticketsystem.PresentationLayer.Components.EventCard;
import ticketsystem.PresentationLayer.Components.Notifications;
import ticketsystem.PresentationLayer.Components.PageContainer;
import ticketsystem.PresentationLayer.Components.SearchPanel;
import ticketsystem.PresentationLayer.Constants.UiRoutes;
import ticketsystem.PresentationLayer.Layouts.MainLayout;
import ticketsystem.PresentationLayer.Presenters.EventCardPresenter;
import ticketsystem.PresentationLayer.Presenters.EventCatalogPresenter;
import ticketsystem.PresentationLayer.Presenters.EventCatalogPresenter.EventCardViewModel;
import ticketsystem.PresentationLayer.Session.UiSession;
import ticketsystem.PresentationLayer.Session.UiVisitCoordinator;

@PageTitle("TixNow | Company Events")
@Route(value = UiRoutes.COMPANY_SEARCH_RESULTS, layout = MainLayout.class)
public class CompanySearchResults extends PageContainer implements BeforeEnterObserver {

    private static final String DEFAULT_COMPANY_NAME = "חברת הפקה";

    private final SearchPanel searchPanel = new SearchPanel();
    private final EventCatalogPresenter eventCatalogPresenter;
    private final EventCardPresenter eventCardPresenter;
    private final UiVisitCoordinator visitCoordinator;

    private Long companyId;
    private String currentCompanyName = DEFAULT_COMPANY_NAME;
    private String currentSearchTerm = "";
    private Map<String, List<String>> currentParameters = Map.of();
    private List<EventCardViewModel> currentResults = List.of();

    public CompanySearchResults( EventCatalogPresenter eventCatalogPresenter, EventCardPresenter eventCardPresenter, UiVisitCoordinator visitCoordinator) {
        super();

        this.eventCatalogPresenter = eventCatalogPresenter;
        this.eventCardPresenter = eventCardPresenter;
        this.visitCoordinator = visitCoordinator;

        addClassName("search-results-view");
        addClassName("company-search-results-view");
        setSpacing(false);
        setPadding(false);

        configureSearchPanel();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        companyId = parseLong(event.getRouteParameters().get("companyId").orElse(null));

        Map<String, List<String>> parameters = event.getLocation()
                .getQueryParameters()
                .getParameters();

        currentParameters = parameters;
        currentSearchTerm = firstParam(parameters, "q", "");

        visitCoordinator.ensureVisitAndNotifications(UI.getCurrent());

        currentCompanyName = firstParam(
                parameters,
                "companyName",
                eventCatalogPresenter.getCompanyDisplayName(UiSession.getCurrentToken(), companyId)
        );

        currentResults = eventCatalogPresenter.getCompanySearchResultEvents(
                UiSession.getCurrentToken(),
                companyId,
                currentParameters
        );

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

        int resultsCount = currentResults.size();
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


        if (currentResults.isEmpty()) {
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

        currentResults.stream()
                .map(this::createEventCard)
                .forEach(grid::add);

        inner.add(grid);
        section.add(inner);

        return section;
    }

    private EventCard createEventCard(EventCardViewModel event) {
        EventCard card = new EventCard(
                event.category(),
                event.title(),
                event.date(),
                event.location(),
                event.priceText(),
                event.imageUrl(),
                event.urgent(),
                event.companyName(),
                event.companyId(),
                event.eventId(),
                event.saleStatus(),
                event.hasLottery()
        );

        card.setActionHandler(createEventCardActionHandler());

        return card;
    }

    private EventCard.EventCardActionHandler createEventCardActionHandler() {
        return new EventCard.EventCardActionHandler() {
            @Override
            public void onPurchaseRequested(Long eventId) {
                UI.getCurrent().navigate(eventCardPresenter.purchaseRoute(eventId));
            }

            @Override
            public void onLotteryRegistrationRequested(Long eventId) {
                try {
                    eventCardPresenter.registerToLottery(UiSession.getMemberToken(), eventId);
                    Notification.show("נרשמת להגרלה בהצלחה.", 3000, Notification.Position.TOP_CENTER);
                    
                } catch (ticketsystem.PresentationLayer.Presenters.PresentationException e) {
                    if (e.isSessionTimeout()) {
                        UiSession.handleTimeoutRedirect();
                        return;
                    }
                    Notifications.error(e.getMessage());
                    
                } catch (Exception e) {
                    Notifications.error(e.getMessage() == null ? "הפעולה נכשלה" : e.getMessage());
                }
            }

            @Override
            public boolean isPreSaleCodeValid(Long eventId, String lotteryCode) {
                try {
                    return eventCardPresenter.isPreSaleCodeValid(UiSession.getMemberToken(), eventId, lotteryCode);
                    
                } catch (ticketsystem.PresentationLayer.Presenters.PresentationException e) {
                    if (e.isSessionTimeout()) {
                        UiSession.handleTimeoutRedirect();
                    } else {
                        Notifications.error(e.getMessage());
                    }
                    return false;
                    
                } catch (Exception e) {
                    return false;
                }
            }

            @Override
            public void onPreSaleApproved(Long eventId, String lotteryCode) {
                UI.getCurrent().navigate(eventCardPresenter.purchaseRoute(eventId));
            }
        };
    }

    private void navigateToCompanySearchResults() {
        if (companyId == null) {
            UI.getCurrent().navigate(UiRoutes.SEARCH_RESULTS);
            return;
        }

        Map<String, String> params = eventCatalogPresenter.buildCompanySearchQueryParameters(
                searchPanel.getFreeText().getValue(),
                searchPanel.getFromDate().getValue(),
                searchPanel.getToDate().getValue(),
                searchPanel.getLocation().getValue(),
                searchPanel.getCategory().getValue(),
                searchPanel.getArtist().getValue(),
                searchPanel.getMinPriceValue(),
                searchPanel.getMaxPriceValue(),
                searchPanel.getMaxPriceLimit(),
                searchPanel.getEventRateValue()
        );

        if (currentCompanyName != null && !currentCompanyName.isBlank()) {
            params.put("companyName", currentCompanyName.trim());
        }

        UI.getCurrent().navigate(
                UiRoutes.COMPANY_SEARCH_RESULTS.replace(":companyId", String.valueOf(companyId)),
                QueryParameters.simple(params)
        );
    }

    private void applyQueryParametersToPanel(Map<String, List<String>> parameters) {
        searchPanel.getFreeText().setValue(currentSearchTerm == null ? "" : currentSearchTerm);

        setComboBoxValueIfPresent(
                searchPanel.getLocation(),
                mapLocationParamToLabel(firstParam(parameters, "location", null))
        );

        setComboBoxValueIfPresent(
                searchPanel.getCategory(),
                mapCategoryParamToLabel(firstParam(parameters, "category", null))
        );

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

    private String mapCategoryParamToLabel(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }

        return switch (category) {
            case "CONCERT" -> "הופעה";
            case "SPORTS" -> "ספורט";
            case "THEATER" -> "תיאטרון";
            case "EXHIBITION" -> "תערוכה";
            case "OTHER" -> "אחר";
            default -> category;
        };
    }

    private String mapLocationParamToLabel(String location) {
        if (location == null || location.isBlank()) {
            return null;
        }

        return switch (location) {
            case "NEW_YORK" -> "ניו יורק";
            case "LOS_ANGELES" -> "לוס אנג׳לס";
            case "CHICAGO" -> "שיקגו";
            case "HOUSTON" -> "יוסטון";
            case "MIAMI" -> "מיאמי";
            case "TEL_AVIV" -> "תל אביב";
            case "JERUSALEM" -> "ירושלים";
            case "BEER_SHEVA" -> "באר שבע";
            case "HAIFA" -> "חיפה";
            case "OTHER" -> "אחר";
            default -> location;
        };
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
}
