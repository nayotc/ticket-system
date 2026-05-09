package ticketsystem.DomainLayer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import ticketsystem.DomainLayer.event.EventCategory;
import ticketsystem.DomainLayer.event.EventLocation;

public class SearchCriteria {
    private String searchTerm;
    private EventCategory category;
    private EventLocation location;
    private String artist;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Double companyRate;
    private Double eventRate; 

    public SearchCriteria(String searchTerm, EventCategory category, EventLocation location, String artist, LocalDateTime fromDate, LocalDateTime toDate, BigDecimal minPrice, BigDecimal maxPrice, Double companyRate, Double eventRate) {
        this.searchTerm = normalize(searchTerm);
        this.category = category;
        this.location = location;
        this.artist = normalize(artist);
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.companyRate = companyRate;
        this.eventRate = eventRate;
        
    }

    public SearchCriteria() {
        this.searchTerm = null;
        this.category = null;
        this.location = null;
        this.artist = null;
        this.fromDate = null;
        this.toDate = null;
        this.minPrice = null;
        this.maxPrice = null;
        this.companyRate = null;
        this.eventRate = null;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim().toLowerCase();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = normalize(searchTerm);
    }

    public EventCategory getCategory() {
        return category;
    }

    public void setCategory(EventCategory category) {
        this.category = category;
    }

    public EventLocation getLocation() {
        return location;
    }

    public void setLocation(EventLocation location) {
        this.location = location;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = normalize(artist);
    }

    public LocalDateTime getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDateTime fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDateTime getToDate() {
        return toDate;
    }

    public void setToDate(LocalDateTime toDate) {
        this.toDate = toDate;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }

    public Double getCompanyRate() {
        return companyRate;
    }

    public void setCompanyRate(Double companyRate) {
        this.companyRate = companyRate;
    }

    public Double getEventRate() {
        return eventRate;
    }

    public void setEventRate(Double eventRate) {
        this.eventRate = eventRate;
    }

}