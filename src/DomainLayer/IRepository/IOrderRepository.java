package DomainLayer.IRepository;

import java.util.List;
import DomainLayer.order.Order;
import DomainLayer.order.Ticket;

public interface IOrderRepository {

    void addOrder(int orderId, List<Ticket> tickets);
    Order findOrderById(int orderId);
    void updateOrder();
    void deleteOrder(int orderId);
    List<Order> getAll();
    
}
