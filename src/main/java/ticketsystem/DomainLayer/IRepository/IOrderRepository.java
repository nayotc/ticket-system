package ticketsystem.DomainLayer.IRepository;

import java.util.List;
import ticketsystem.DomainLayer.order.ActiveOrder;

public interface IOrderRepository {

    void addOrder(ActiveOrder order);
    ActiveOrder findOrderById(int orderId);
    void updateOrder(ActiveOrder order);
    void deleteOrder(int orderId);
    List<ActiveOrder> getAll();
    ActiveOrder getActiveOrderByUserIdAndEventId(Integer userId, int eventId);
    int getNextId();
    ActiveOrder getActiveOrderBySessionTokenAndEventId(String sessionToken, int eventId);
    
}
