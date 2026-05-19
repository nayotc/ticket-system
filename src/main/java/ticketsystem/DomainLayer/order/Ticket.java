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
    //copy constructor
        public Ticket(Ticket other) {
        this.ticketId = other.ticketId;
        this.eventId = other.eventId;
        this.areaId = other.areaId;
        this.row = other.row;
        this.chair = other.chair;
        this.price = other.price;
    }

    public Ticket copy() {
        return new Ticket(this);
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
