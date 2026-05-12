package ticketsystem.DomainLayer.IRepository;

import java.util.List;
import ticketsystem.DomainLayer.order.ActiveOrder;

public interface IOrderRepository {

    void addOrder(ActiveOrder order);

    ActiveOrder findOrderById(Long orderId);

    void updateOrder(ActiveOrder order);

    void deleteOrder(Long orderId);

    List<ActiveOrder> getAll();

    ActiveOrder getActiveOrderByUserIdAndEventId(Long userId, Long eventId);

    Long getNextId();

    ActiveOrder getActiveOrderBySessionTokenAndEventId(String sessionToken, Long eventId);

    void deleteActiveOrdersByUserId(Long userId);

}
