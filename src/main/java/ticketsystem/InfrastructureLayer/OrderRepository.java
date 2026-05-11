package ticketsystem.InfrastructureLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ticketsystem.DomainLayer.order.ActiveOrder;
import ticketsystem.DomainLayer.order.ActiveOrder;
import ticketsystem.DomainLayer.IRepository.IOrderRepository;

public class OrderRepository implements IOrderRepository {

    private Long counter;
    private OrderRepository instance;
    private ConcurrentHashMap<Long, ActiveOrder> orders;

    private OrderRepository() {
        this.counter =0L;
        this.orders = new ConcurrentHashMap<Long, ActiveOrder>();
    }

    public OrderRepository getInstance() {
        if (instance == null) {
            instance = new OrderRepository();
        }
        return instance;
    }
    
    public synchronized void addOrder(ActiveOrder order) {
        //
        for (ActiveOrder existingOrder : orders.values()) {
            if (existingOrder.getSessionToken().equals(order.getSessionToken()) || existingOrder.getUserId().equals(order.getUserId())) {
                throw new IllegalArgumentException("An active order already exists for this user to another event");
            }
        }
        this.orders.put(order.getOrderId(), order);
    }

    public ActiveOrder findOrderById(Long orderId) {
        return this.orders.get(orderId);
    }


    public synchronized void deleteOrder(Long orderId) {
        if (!orders.containsKey(orderId)) {
            throw new IllegalArgumentException("Order with ID " + orderId + " does not exist");
        }
        orders.remove(orderId);
    }

    public synchronized void deleteOrderBySessionToken(String sessionToken) {
        orders.values().removeIf(order -> order.getSessionToken().equals(sessionToken));
    }

    public List<ActiveOrder> getAll() {
        List<ActiveOrder> activeOrders=new ArrayList<>();
        for(Map.Entry<Long,ActiveOrder> entry: orders.entrySet()){
            activeOrders.add(entry.getValue());
        }
        return activeOrders;
    }

    public ActiveOrder getActiveOrderByUserIdAndEventId(Long userId, Long eventId) {
        return orders.values().stream()
                .filter(order -> order.getUserId().equals(userId) && order.getEventId().equals(eventId))
                .findFirst()
                .orElse(null);
    }

    public Long getNextId() {
        return counter++;
    }

    @Override
  
    public void updateOrder(ActiveOrder order) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }
        orders.put(order.getOrderId(), order);
    }

    public ActiveOrder getActiveOrderBySessionTokenAndEventId(String sessionToken, Long eventId) {
         return orders.values().stream()
                .filter(order -> order.getSessionToken().equals(sessionToken) && order.getEventId() == eventId)
                .findFirst()
                .orElse(null);

    }

    public void clear() {
        orders.clear();
    }

    public void deleteActiveOrdersByUserId(Long userId) {
        orders.values().removeIf(order -> order.getUserId() != null && order.getUserId().equals(userId));
    }

}
