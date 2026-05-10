package ticketsystem.DTO;

public class TicketDTO {
    private int ticketId;
    private int eventId;
    private int row;
    private int chair;
    private double price;
    private String status;

    //for json
    public TicketDTO() {}

    public TicketDTO(int ticketId, int eventId, int row, int chair, double price, String status) {
        this.ticketId = ticketId;
        this.eventId = eventId;
        this.row = row;
        this.chair = chair;
        this.price = price;
        this.status = status;
    }

    public int getTicketId() { return ticketId; }
    public void setTicketId(int ticketId) { this.ticketId = ticketId; }
    
    public int getEventId() { return eventId; }
    public void setEventId(int eventId) { this.eventId = eventId; }
    
    public int getRow() { return row; }
    public void setRow(int row) { this.row = row; }
    
    public int getChair() { return chair; }
    public void setChair(int chair) { this.chair = chair; }
    
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}