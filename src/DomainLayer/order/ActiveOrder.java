package DomainLayer.order;

import java.util.ArrayList;
import java.util.List;

public class ActiveOrder extends Order {

    private int orderId;
    private List<Ticket> tickets;

    public ActiveOrder(int orderId) {
        super(orderId);
        this.tickets = new ArrayList<>();
    }

    public void addTicket() {

    }

    public void deleteTicket(int ticketId) {

    }
    
}
