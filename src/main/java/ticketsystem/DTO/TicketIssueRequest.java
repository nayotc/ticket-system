package ticketsystem.DTO;

public class TicketIssueRequest {

    private String customerId;
    private String eventId;
    private TicketZoneType zone;

    private Integer quantity;

    private boolean seating;
    private String seatsJson;

    public TicketIssueRequest() {
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public TicketZoneType getZoneType() {
        return zone;
    }

    public void setZone(TicketZoneType zone) {
        this.zone = zone;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public boolean isSeating() {
        return seating;
    }

    public void setSeating(boolean seating) {
        this.seating = seating;
    }

    public String getSeatsJson() {
        return seatsJson;
    }

    public void setSeatsJson(String seatsJson) {
        this.seatsJson = seatsJson;
    }

    public enum TicketZoneType {
    STANDING,
    SEATING
}
}