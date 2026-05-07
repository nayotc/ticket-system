package ticketsystem.DomainLayer.order;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Ticket {

    private int ticketId;
    private int eventId;
    private int row;
    private int chair;
    private double price;

    //location 0,0 is standing

    public Ticket(int ticketId, int eventId, int row, int chair, double price) {
        this.ticketId = ticketId;
        this.eventId = eventId;
        this.row = row;
        this.chair = chair;
        this.price = price;
      
    }

    public int getTicketId() {
        return this.ticketId;
    }

    public void setTicketId(int ticketId) {
        this.ticketId = ticketId;
    }

    public int getEventId() {
        return this.eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
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
