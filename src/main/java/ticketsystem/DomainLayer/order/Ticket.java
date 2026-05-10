package ticketsystem.DomainLayer.order;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Ticket {

    private Long ticketId;
    private Long eventId;
    private Long areaId;
    private int row;
    private int chair;
    private double price;

    //location 0,0 is standing

    public Ticket(Long ticketId, Long eventId, Long areaId, int row, int chair, double price) {
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

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public Long getEventId() {
        return this.eventId;
    }


    public Long getAreaId() {
        return this.areaId;
    }

    public void setAreaId(Long areaId) {
        this.areaId = areaId;
    }

    public int getRow() {
        return this.row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getChair() {
        return this.chair;
    }

    public void setChair(int chair) {
        this.chair = chair;
    }

    public double getPrice() {
        return this.price;
    }

    public void setPrice(double price) {
        this.price = price;
    }
    



}
