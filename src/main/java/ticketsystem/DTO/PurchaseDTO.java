package ticketsystem.DTO;

import java.math.BigDecimal;

public class PurchaseDTO {
    private Long ticketId;
    private Long eventId;
    private int row;
    private int chair;
    private BigDecimal price;
    private String status;
    private String secureBarcode;

    //for json
    public PurchaseDTO() {}

    public PurchaseDTO(Long ticketId, Long eventId, int row, int chair, BigDecimal price, String status, String secureBarcode) {
        this.ticketId = ticketId;
        this.eventId = eventId;
        this.row = row;
        this.chair = chair;
        this.price = price;
        this.status = status;
        this.secureBarcode = secureBarcode;
    }

    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
    
    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }
    
    public int getRow() { return row; }
    public void setRow(int row) { this.row = row; }
    
    public int getChair() { return chair; }
    public void setChair(int chair) { this.chair = chair; }
    
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSecureBarcode() { return secureBarcode; }
    public void setSecureBarcode(String secureBarcode) { this.secureBarcode = secureBarcode; }

}