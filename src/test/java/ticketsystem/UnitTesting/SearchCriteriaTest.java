package ticketsystem.UnitTesting;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import ticketsystem.DomainLayer.SearchCriteria;
import ticketsystem.DomainLayer.event.EventCategory;
import ticketsystem.DomainLayer.event.EventLocation;

import static org.junit.jupiter.api.Assertions.*;

public class SearchCriteriaTest {

    @Test
    void GivenEmptyConstructor_WhenCreateSearchCriteria_ThenAllFieldsAreNull() {
        // Act
        SearchCriteria criteria = new SearchCriteria();

        // Assert
        assertNull(criteria.getSearchTerm());
        assertNull(criteria.getCategory());
        assertNull(criteria.getLocation());
        assertNull(criteria.getArtist());
        assertNull(criteria.getFromDate());
        assertNull(criteria.getToDate());
        assertNull(criteria.getMinPrice());
        assertNull(criteria.getMaxPrice());
        assertNull(criteria.getCompanyRate());
        assertNull(criteria.getEventRate());
    }

    @Test
    void GivenFullConstructor_WhenCreateSearchCriteria_ThenFieldsAreInitializedCorrectly() {
        // Arrange
        LocalDateTime fromDate = LocalDateTime.now();
        LocalDateTime toDate = fromDate.plusDays(7);
        BigDecimal minPrice = BigDecimal.valueOf(50);
        BigDecimal maxPrice = BigDecimal.valueOf(200);

        // Act
        SearchCriteria criteria = new SearchCriteria(
                " Concert ",
                EventCategory.CONCERT,
                EventLocation.NEW_YORK,
                " Artist Name ",
                fromDate,
                toDate,
                minPrice,
                maxPrice,
                4.5,
                4.8
        );

        // Assert
        assertEquals("concert", criteria.getSearchTerm());
        assertEquals(EventCategory.CONCERT, criteria.getCategory());
        assertEquals(EventLocation.NEW_YORK, criteria.getLocation());
        assertEquals("artist name", criteria.getArtist());
        assertEquals(fromDate, criteria.getFromDate());
        assertEquals(toDate, criteria.getToDate());
        assertEquals(minPrice, criteria.getMinPrice());
        assertEquals(maxPrice, criteria.getMaxPrice());
        assertEquals(4.5, criteria.getCompanyRate());
        assertEquals(4.8, criteria.getEventRate());
    }

    @Test
    void GivenBlankSearchTermAndArtist_WhenCreateSearchCriteria_ThenNormalizedToNull() {
        // Act
        SearchCriteria criteria = new SearchCriteria(
                "   ",
                null,
                null,
                "   ",
                null,
                null,
                null,
                null,
                null,
                null
        );

        // Assert
        assertNull(criteria.getSearchTerm());
        assertNull(criteria.getArtist());
    }

    @Test
    void GivenSearchCriteria_WhenSetSearchTerm_ThenValueIsTrimmedAndLowerCased() {
        // Arrange
        SearchCriteria criteria = new SearchCriteria();

        // Act
        criteria.setSearchTerm("  Event Name  ");

        // Assert
        assertEquals("event name", criteria.getSearchTerm());
    }

    @Test
    void GivenSearchCriteria_WhenSetBlankSearchTerm_ThenValueIsNull() {
        // Arrange
        SearchCriteria criteria = new SearchCriteria();

        // Act
        criteria.setSearchTerm("   ");

        // Assert
        assertNull(criteria.getSearchTerm());
    }

    @Test
    void GivenSearchCriteria_WhenSetArtist_ThenValueIsTrimmedAndLowerCased() {
        // Arrange
        SearchCriteria criteria = new SearchCriteria();

        // Act
        criteria.setArtist("  Some Artist  ");

        // Assert
        assertEquals("some artist", criteria.getArtist());
    }

    @Test
    void GivenSearchCriteria_WhenSetBlankArtist_ThenValueIsNull() {
        // Arrange
        SearchCriteria criteria = new SearchCriteria();

        // Act
        criteria.setArtist("   ");

        // Assert
        assertNull(criteria.getArtist());
    }

    @Test
    void GivenSearchCriteria_WhenSetAllFields_ThenFieldsAreUpdated() {
        // Arrange
        SearchCriteria criteria = new SearchCriteria();

        LocalDateTime fromDate = LocalDateTime.now();
        LocalDateTime toDate = fromDate.plusDays(3);
        BigDecimal minPrice = BigDecimal.valueOf(100);
        BigDecimal maxPrice = BigDecimal.valueOf(300);

        // Act
        criteria.setSearchTerm(" Show ");
        criteria.setCategory(EventCategory.CONCERT);
        criteria.setLocation(EventLocation.NEW_YORK);
        criteria.setArtist(" Singer ");
        criteria.setFromDate(fromDate);
        criteria.setToDate(toDate);
        criteria.setMinPrice(minPrice);
        criteria.setMaxPrice(maxPrice);
        criteria.setCompanyRate(3.7);
        criteria.setEventRate(4.2);

        // Assert
        assertEquals("show", criteria.getSearchTerm());
        assertEquals(EventCategory.CONCERT, criteria.getCategory());
        assertEquals(EventLocation.NEW_YORK, criteria.getLocation());
        assertEquals("singer", criteria.getArtist());
        assertEquals(fromDate, criteria.getFromDate());
        assertEquals(toDate, criteria.getToDate());
        assertEquals(minPrice, criteria.getMinPrice());
        assertEquals(maxPrice, criteria.getMaxPrice());
        assertEquals(3.7, criteria.getCompanyRate());
        assertEquals(4.2, criteria.getEventRate());
    }

    @Test
    void GivenSearchCriteria_WhenSetNullableFieldsToNull_ThenFieldsAreNull() {
        // Arrange
        SearchCriteria criteria = new SearchCriteria();

        // Act
        criteria.setSearchTerm(null);
        criteria.setCategory(null);
        criteria.setLocation(null);
        criteria.setArtist(null);
        criteria.setFromDate(null);
        criteria.setToDate(null);
        criteria.setMinPrice(null);
        criteria.setMaxPrice(null);
        criteria.setCompanyRate(null);
        criteria.setEventRate(null);

        // Assert
        assertNull(criteria.getSearchTerm());
        assertNull(criteria.getCategory());
        assertNull(criteria.getLocation());
        assertNull(criteria.getArtist());
        assertNull(criteria.getFromDate());
        assertNull(criteria.getToDate());
        assertNull(criteria.getMinPrice());
        assertNull(criteria.getMaxPrice());
        assertNull(criteria.getCompanyRate());
        assertNull(criteria.getEventRate());
    }
}