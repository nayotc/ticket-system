package ticketsystem.DomainLayer.IRepository;

import java.util.List;
import ticketsystem.DomainLayer.order.Order;

public interface IOrderRepository {

    void addOrder();
    Order findOrderById(int orderId);
    void updateOrder(Order order);
    void deleteOrder(int orderId);
    List<Order> getAll();
    
}
