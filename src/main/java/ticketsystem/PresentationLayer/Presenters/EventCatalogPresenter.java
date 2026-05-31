package ticketsystem.PresentationLayer.Presenters;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import ticketsystem.DomainLayer.event.SaleStatus;
import ticketsystem.PresentationLayer.Constants.Photos;
import ticketsystem.ApplicationLayer.CompanyService;
import ticketsystem.ApplicationLayer.EventCatalogService;
import ticketsystem.ApplicationLayer.LotteryService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import ticketsystem.DTO.CompanyDTO;
import ticketsystem.DTO.Event.EventSearchResultDTO;

import java.util.List;

/**
 * Presenter for event catalog UI actions.
 *
 * Keeps search-related view logic out of the Home view by preparing
 * query parameters that can be used for navigation to the search results page.
 */
@Component
public class EventCatalogPresenter {
    private final EventCatalogService eventCatalogService;
    private final CompanyService companyService;
    private final LotteryService lotteryService;

    public EventCatalogPresenter( EventCatalogService eventCatalogService, CompanyService companyService, LotteryService lotteryService) {
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
     * @param freeText free-text search input
     * @param fromDate selected start date filter
     * @param toDate selected end date filter
     * @param location selected location filter
     * @param category selected category filter
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

    public List<HomeEventCard> getFeaturedHomeEvents(String sessionToken) {
        return eventCatalogService.getFeaturedEvents(sessionToken, 3)
                .stream()
                .map(event -> toHomeEventCard(sessionToken, event))
                .toList();
    }

    public record HomeEventCard(
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

    private HomeEventCard toHomeEventCard(String sessionToken, EventSearchResultDTO event) {
        String companyName = resolveCompanyName(sessionToken, event.companyId());

        return new HomeEventCard(
                prettyEnum(event.category()),
                event.name(),
                formatDate(event.date()),
                prettyEnum(event.location()),
                formatPrice(event.ticketPrice()),
                resolveImageUrl(event.category()),
                false,
                companyName,
                event.companyId(),
                event.id(),
                parseSaleStatus(event.saleStatus()),
                lotteryService.hasLotteryForEvent(sessionToken, event.id())
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

    private String resolveImageUrl(String category) {
        if (category == null) {
            return Photos.EVENT_LIGHTS;
        }

        return switch (category) {
            case "THEATER" -> Photos.EVENT_STANDUP;
            case "SPORTS" -> Photos.EVENT_ELECTRONIC;
            case "CONCERT" -> Photos.EVENT_LIGHTS;
            default -> Photos.EVENT_LIGHTS;
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
