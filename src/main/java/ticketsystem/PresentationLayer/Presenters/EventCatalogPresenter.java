package ticketsystem.PresentationLayer.Presenters;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import ticketsystem.DomainLayer.event.SaleStatus;
import ticketsystem.PresentationLayer.Constants.Photos;

import java.util.List;

/**
 * Presenter for event catalog UI actions.
 *
 * Keeps search-related view logic out of the Home view by preparing
 * query parameters that can be used for navigation to the search results page.
 */
@Component
public class EventCatalogPresenter {

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

        if (location != null && !"כל האזורים".equals(location)) {
            params.put("location", location);
        }

        if (category != null && !"כל הקטגוריות".equals(category)) {
            params.put("category", category);
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
    public List<HomeEventCard> getFeaturedHomeEvents() {
        return List.of(
                new HomeEventCard(
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
                        SaleStatus.ONGOING,
                        false
                ),
                new HomeEventCard(
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
                        SaleStatus.SOLD_OUT,
                        false
                ),
                new HomeEventCard(
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
                        SaleStatus.NOT_STARTED,
                        true
                )
        );
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
}
