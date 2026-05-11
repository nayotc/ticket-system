package ticketsystem.DomainLayer.order;
import java.math.BigDecimal;

public class Ticket {

    private Long ticketId;
    private Long eventId;
    private Long areaId;
    private int row;
    private int chair;
    private BigDecimal price;

    //location 0,0 is standing

    public Ticket(Long ticketId, Long eventId, Long areaId, int row, int chair, BigDecimal price) {
        this.ticketId = ticketId;
        this.eventId = eventId;
        this.areaId = areaId;
        this.row = row;
        this.chair = chair;
        this.price = price;
      
    }

    public Long getTicketId() {
        return this.ticketId;
    }

    public Long getEventId() {
        return this.eventId;
    }


    public Long getAreaId() {
        return this.areaId;
    }

    public int getRow() {
        return this.row;
    }

    public int getChair() {
        return this.chair;
    }


    public BigDecimal getPrice() {
        return this.price;
    }  



}
