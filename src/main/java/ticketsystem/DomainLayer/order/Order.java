package ticketsystem.DomainLayer.order;

import java.util.List;

public abstract class Order {

    protected int orderId;
    protected List<Ticket> tickets;

    public Order(int orderId) {
        this.orderId = orderId;
    }

    public int getId() {
        return orderId;
    }

    public List<Ticket> getTickets() {
        return tickets;
        
    }
    
}
