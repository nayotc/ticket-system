package ticketsystem.DomainLayer.order;

import java.util.List;

public class Purchase extends Order {

    private int orderId;
    private List<Ticket> tickets;

    public Purchase(int orderId, List<Ticket> tickets) {
        super(orderId);
        this.tickets = tickets;
    }
    
}
