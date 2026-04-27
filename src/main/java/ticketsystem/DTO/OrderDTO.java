package ticketsystem.DTO;

import java.util.List;

public class OrderDTO {
    
    private int orderId;
    private List<TicketDTO> tickets;
    private String eventName;
    private String location;
    private int memberId;
    private int companyId;

    //for json 
    public OrderDTO() {
    }

    public OrderDTO(int orderId, List<TicketDTO> tickets, String eventName, 
                    String location, int memberId, int companyId) {
        this.orderId = orderId;
        this.tickets = tickets;
        this.eventName = eventName;
        this.location = location;
        this.memberId = memberId;
        this.companyId = companyId;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public List<TicketDTO> getTickets() {
        return tickets;
    }

    public void setTickets(List<TicketDTO> tickets) {
        this.tickets = tickets;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getMemberId() {
        return memberId;
    }

    public void setMemberId(int memberId) {
        this.memberId = memberId;
    }

    public int getCompanyId() {
        return companyId;
    }

    public void setCompanyId(int companyId) {
        this.companyId = companyId;
    }
}