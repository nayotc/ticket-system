package ticketsystem.DomainLayer.history;

public class PurchasedTicket {

    private int ticketId;
    private int eventId;
    private int row;
    private int chair;
    private double price;
    private TicketStatus status;
    private String secureBarcode;

    public PurchasedTicket() {
    }
    public PurchasedTicket(int ticketId, int eventId, int row, int chair, double price, String secureBarcode) {
        this.ticketId = ticketId;
        this.eventId = eventId;
        this.row = row;
        this.chair = chair;
        this.price = price;
        this.secureBarcode = secureBarcode;
        this.status = TicketStatus.ACTIVE;
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
    
    public TicketStatus getStatus() {
        return this.status;
    }
    public void setStatus(TicketStatus status) {
        this.status = status;
    }
    
    public String getSecureBarcode() {
        return this.secureBarcode;
    }
    
}