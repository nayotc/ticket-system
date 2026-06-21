package ticketsystem.DTO;

import java.math.BigDecimal;

public class TicketDTO {
    private Long ticketId;
    private Long eventId;
    private int row;
    private int chair;
    private BigDecimal price;
    private Long areaId;

    //for json
    public TicketDTO() {}

   public TicketDTO(Long ticketId, Long eventId, Long areaId, int row, int chair, BigDecimal price) {
    this.ticketId = ticketId;
    this.eventId = eventId;
    this.areaId = areaId;
    this.row = row;
    this.chair = chair;
    this.price = price;
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
    public Long getAreaId() { return areaId; }
public void setAreaId(Long areaId) { this.areaId = areaId; }
   
}