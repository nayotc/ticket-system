package ticketsystem.PresentationLayer.Presenters;

import org.springframework.stereotype.Component;
import ticketsystem.ApplicationLayer.CompanyService;
import ticketsystem.ApplicationLayer.EventCatalogService;
import ticketsystem.ApplicationLayer.LotteryService;
import ticketsystem.DTO.CompanyDTO;
import ticketsystem.DTO.Event.EventSearchResultDTO;
import ticketsystem.DomainLayer.SearchCriteria;
import ticketsystem.DomainLayer.event.EventCategory;
import ticketsystem.DomainLayer.event.EventLocation;
import ticketsystem.DomainLayer.event.SaleStatus;
import ticketsystem.PresentationLayer.Constants.Photos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Presenter for event catalog UI actions.
 *
 * This presenter keeps catalog/search view logic out of the Vaadin views.
 * It is responsible for:
 * - Building URL query parameters from search panel values.
 * - Loading featured events for the Home page.
 * - Loading global search results for the SearchResults page.
 * - Mapping application-layer event DTOs into card data used by the UI.
 */
@Component
public class EventCatalogPresenter {

    private final EventCatalogService eventCatalogService;
    private final CompanyService companyService;
    private final LotteryService lotteryService;

    public EventCatalogPresenter(
            EventCatalogService eventCatalogService,
            CompanyService companyService,
            LotteryService lotteryService
    ) {
        this.eventCatalogService = eventCatalogService;
        this.companyService = companyService;
        this.lotteryService = lotteryService;
    }

    /**
     * Builds URL query parameters from the values selected in the search panel.
     *
     * Empty values and default filter values are ignored so the generated URL
     * contains only filters that were actually selected by the user.
     *
     * UI labels are converted into stable domain enum values before being added
     * to the URL. For example: "ירושלים" becomes "JERUSALEM".
     *
     * @param freeText free-text search input
     * @param fromDate selected start date filter
     * @param toDate selected end date filter
     * @param location selected location label from the UI
     * @param category selected category label from the UI
     * @param artist selected artist filter
     * @param minPrice selected minimum price
     * @param maxPrice selected maximum price
     * @param maxPriceLimit maximum possible price value in the UI
     * @param eventRate selected minimum event rating
     * @param companyRate selected minimum company rating
     * @return query parameters for the search results route
     */
    public Map<String, String> buildSearchQueryParameters(
            String freeText,
            LocalDate fromDate,
            LocalDate toDate,
            String location,
            String category,
            String artist,
            double minPrice,
            double maxPrice,
            double maxPriceLimit,
            double eventRate,
            double companyRate
    ) {
        Map<String, String> params = new LinkedHashMap<>();

        if (freeText != null && !freeText.isBlank()) {
            params.put("q", freeText.trim());
        }

        if (fromDate != null) {
            params.put("fromDate", fromDate.toString());
        }

        if (toDate != null) {
            params.put("toDate", toDate.toString());
        }

        String mappedLocation = mapLocationLabelToParam(location);
        if (mappedLocation != null) {
            params.put("location", mappedLocation);
        }

        String mappedCategory = mapCategoryLabelToParam(category);
        if (mappedCategory != null) {
            params.put("category", mappedCategory);
        }

        if (artist != null && !artist.isBlank()) {
            params.put("artist", artist.trim());
        }

        if (minPrice > 0) {
            params.put("minPrice", String.valueOf(minPrice));
        }

        if (maxPrice < maxPriceLimit) {
            params.put("maxPrice", String.valueOf(maxPrice));
        }

        if (eventRate > 0) {
            params.put("eventRate", String.valueOf(eventRate));
        }

        if (companyRate > 0) {
            params.put("companyRate", String.valueOf(companyRate));
        }

        return params;
    }

    /**
     * Loads featured events for the Home page.
     *
     * Featured events are loaded from the application layer and converted into
     * EventCardViewModel records so the view can render EventCard components without
     * knowing how to resolve company names, prices, dates, sale status, or lottery state.
     *
     * @param sessionToken active guest/member session token
     * @return featured event card data for the Home page
     */
    public List<EventCardViewModel> getFeaturedHomeEvents(String sessionToken) {
        return eventCatalogService.getFeaturedEvents(sessionToken, 3)
                .stream()
                .map(event -> toEventCardViewModel(sessionToken, event))
                .toList();
    }

    /**
     * Loads global event search results from URL query parameters.
     *
     * The SearchResults view passes the raw query parameters from the route.
     * This presenter converts them into a domain SearchCriteria object,
     * delegates the global search to EventCatalogService, and maps the returned
     * events into card data used by the UI.
     *
     * This method is intentionally scoped to global search. Company-specific
     * search should use a separate presenter method so the two flows stay explicit.
     *
     * @param sessionToken active guest/member session token
     * @param parameters query parameters from the current route
     * @return event card data matching the global search criteria
     */
    public List<EventCardViewModel> getGlobalSearchResultEvents(
            String sessionToken,
            Map<String, List<String>> parameters
    ) {
        SearchCriteria criteria = buildSearchCriteria(parameters);

        return eventCatalogService.globalSearch(sessionToken, criteria)
                .stream()
                .map(event -> toEventCardViewModel(sessionToken, event))
                .toList();
    }

    /**
     * Loads company-specific event search results from URL query parameters.
     *
     * The CompanySearchResults view passes the company id and the raw query parameters.
     * This presenter converts the parameters into search criteria, delegates the company
     * search to EventCatalogService, and maps the returned events into card data.
     *
     * Company rating is intentionally ignored here because the search is already scoped
     * to a single company.
     *
     * @param sessionToken active guest/member session token
     * @param companyId company whose events should be searched
     * @param parameters query parameters from the current route
     * @return event card data matching the company-specific search criteria
     */
    public List<EventCardViewModel> getCompanySearchResultEvents(
            String sessionToken,
            Long companyId,
            Map<String, List<String>> parameters
    ) {
        if (companyId == null) {
            return List.of();
        }

        SearchCriteria criteria = buildCompanySearchCriteria(parameters);

        return eventCatalogService.SearchByCompany(sessionToken, companyId, criteria)
                .stream()
                .map(event -> toEventCardViewModel(sessionToken, event))
                .toList();
    }

    /**
     * View model used by EventCard-based screens.
     *
     * The presenter prepares this record so views do not need to know how to
     * format dates/prices, resolve company names, parse sale statuses, or check
     * whether an event has a lottery.
     */
    public record EventCardViewModel(
            String category,
            String title,
            String date,
            String location,
            String priceText,
            String imageUrl,
            boolean urgent,
            String companyName,
            Long companyId,
            Long eventId,
            SaleStatus saleStatus,
            boolean hasLottery
    ) {
    }

    /**
     * Converts an application-layer event search DTO into UI card data.
     */
    private EventCardViewModel toEventCardViewModel(String sessionToken, EventSearchResultDTO event) {
        String companyName = resolveCompanyName(sessionToken, event.companyId());

        return new EventCardViewModel(
                prettyEnum(event.category()),
                event.name(),
                formatDate(event.date()),
                prettyEnum(event.location()),
                formatPrice(event.ticketPrice()),
                resolveImageUrl(event.category(), event.id()),
                false,
                companyName,
                event.companyId(),
                event.id(),
                parseSaleStatus(event.saleStatus()),
                lotteryService.hasLotteryForEvent(sessionToken, event.id())
        );
    }

    /**
     * Converts route query parameters into domain search criteria.
     *
     * URL parameters are received as strings because they come from the browser.
     * This method converts them into the types expected by the domain layer:
     * enum values, date ranges, prices, and rating filters.
     *
     * Missing parameters are treated as null, meaning the matching filter should
     * not restrict the search.
     */
    private SearchCriteria buildSearchCriteria(Map<String, List<String>> parameters) {
        Map<String, List<String>> safeParameters = parameters == null ? Map.of() : parameters;

        return new SearchCriteria(
                firstParam(safeParameters, "q"),
                parseCategory(firstParam(safeParameters, "category")),
                parseLocation(firstParam(safeParameters, "location")),
                firstParam(safeParameters, "artist"),
                parseStartDate(firstParam(safeParameters, "fromDate")),
                parseEndDate(firstParam(safeParameters, "toDate")),
                parseBigDecimal(firstParam(safeParameters, "minPrice")),
                parseBigDecimal(firstParam(safeParameters, "maxPrice")),
                parseDouble(firstParam(safeParameters, "companyRate")),
                parseDouble(firstParam(safeParameters, "eventRate"))
        );
    }

    /**
     * Builds search criteria for company-specific search.
     *
     * Company rating is not applicable here because the results are already scoped
     * to one company. Passing companyRate into the domain company-search flow would
     * be rejected by EventCatalogDomainService.
     */
    private SearchCriteria buildCompanySearchCriteria(Map<String, List<String>> parameters) {
        Map<String, List<String>> safeParameters = parameters == null ? Map.of() : parameters;

        return new SearchCriteria(
                firstParam(safeParameters, "q"),
                parseCategory(firstParam(safeParameters, "category")),
                parseLocation(firstParam(safeParameters, "location")),
                firstParam(safeParameters, "artist"),
                parseStartDate(firstParam(safeParameters, "fromDate")),
                parseEndDate(firstParam(safeParameters, "toDate")),
                parseBigDecimal(firstParam(safeParameters, "minPrice")),
                parseBigDecimal(firstParam(safeParameters, "maxPrice")),
                null,
                parseDouble(firstParam(safeParameters, "eventRate"))
        );
    }

    private String resolveCompanyName(String sessionToken, Long companyId) {
        if (companyId == null) {
            return "";
        }

        try {
            CompanyDTO company = companyService.getCompanyDetails(sessionToken, companyId);
            return company == null ? "" : company.getName();
        } catch (Exception e) {
            return "";
        }
    }

    private String formatDate(LocalDateTime date) {
        if (date == null) {
            return "";
        }

        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm"));
    }

    private String formatPrice(BigDecimal price) {
        if (price == null) {
            return "";
        }

        return "₪" + price;
    }

    private String resolveImageUrl(String category, Long eventId) {
        String[] images = imagesForCategory(category);

        if (eventId == null) {
            return images[0];
        }

        int index = Math.floorMod(eventId.hashCode(), images.length);
        return images[index];
    }

    private String[] imagesForCategory(String category) {
        if (category == null) {
            return new String[] {
                    Photos.EVENT_LIGHTS,
                    Photos.EVENT_STANDUP,
                    Photos.EVENT_ELECTRONIC
            };
        }

        return switch (category) {
            case "THEATER" -> new String[] {
                    Photos.EVENT_STANDUP,
                    Photos.EVENT_LIGHTS
            };
            case "SPORTS" -> new String[] {
                    Photos.EVENT_ELECTRONIC,
                    Photos.EVENT_LIGHTS
            };
            case "CONCERT" -> new String[] {
                    Photos.EVENT_LIGHTS,
                    Photos.EVENT_ELECTRONIC
            };
            case "EXHIBITION" -> new String[] {
                    Photos.EVENT_LIGHTS,
                    Photos.EVENT_STANDUP
            };
            default -> new String[] {
                    Photos.EVENT_LIGHTS,
                    Photos.EVENT_STANDUP,
                    Photos.EVENT_ELECTRONIC
            };
        };
    }

    private SaleStatus parseSaleStatus(String saleStatus) {
        if (saleStatus == null || saleStatus.isBlank()) {
            return SaleStatus.NOT_STARTED;
        }

        try {
            return SaleStatus.valueOf(saleStatus);
        } catch (IllegalArgumentException e) {
            return SaleStatus.NOT_STARTED;
        }
    }

    /**
     * Reads the first value of a query parameter.
     *
     * Vaadin stores query parameters as a list of values per key. For this screen
     * each filter is expected to have a single value, so only the first value is used.
     */
    private String firstParam(Map<String, List<String>> parameters, String name) {
        if (parameters == null || name == null) {
            return null;
        }

        List<String> values = parameters.get(name);
        if (values == null || values.isEmpty()) {
            return null;
        }

        String value = values.get(0);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private EventCategory parseCategory(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return EventCategory.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private EventLocation parseLocation(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return EventLocation.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private LocalDateTime parseStartDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return LocalDate.parse(value).atStartOfDay();
    }

    private LocalDateTime parseEndDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return LocalDate.parse(value).atTime(23, 59, 59);
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return new BigDecimal(value.trim());
    }

    private Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return Double.parseDouble(value.trim());
    }

    private String prettyEnum(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return Arrays.stream(value.toLowerCase().split("_"))
                .map(part -> part.isBlank()
                        ? part
                        : part.substring(0, 1).toUpperCase() + part.substring(1))
                .reduce((left, right) -> left + " " + right)
                .orElse(value);
    }

    private String mapCategoryLabelToParam(String category) {
        if (category == null || category.isBlank() || "כל הקטגוריות".equals(category)) {
            return null;
        }

        return switch (category) {
            case "הופעה" -> "CONCERT";
            case "ספורט" -> "SPORTS";
            case "תיאטרון" -> "THEATER";
            case "תערוכה" -> "EXHIBITION";
            case "אחר" -> "OTHER";
            default -> category;
        };
    }

    private String mapLocationLabelToParam(String location) {
        if (location == null || location.isBlank() || "כל האזורים".equals(location)) {
            return null;
        }

        return switch (location) {
            case "ניו יורק" -> "NEW_YORK";
            case "לוס אנג׳לס" -> "LOS_ANGELES";
            case "שיקגו" -> "CHICAGO";
            case "יוסטון" -> "HOUSTON";
            case "מיאמי" -> "MIAMI";
            case "תל אביב" -> "TEL_AVIV";
            case "ירושלים" -> "JERUSALEM";
            case "באר שבע" -> "BEER_SHEVA";
            case "חיפה" -> "HAIFA";
            case "אחר" -> "OTHER";
            default -> location;
        };
    }
}
