package ticketsystem.ApplicationLayer;

import java.util.List;
import ticketsystem.DomainLayer.order.Ticket;
import ticketsystem.DomainLayer.IRepository.IOrderRepository;

public class OrderService {
    
    private final IOrderRepository orderReposetory;

    public OrderService(IOrderRepository orderReposetory) {
        this.orderReposetory = orderReposetory;   
    }

    public void addOrder(int orderId, List<Ticket> tickets) {

    }

    public void deleteOrder(int orderId) {

    }

    public void addTicketToOrder(int orderId, int ticketId) {

    }

    public void removeTicketFromOrder(int orderId, int ticketId) {

    }
    
}

