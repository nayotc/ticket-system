package ticketsystem.InfrastructureLayer;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import ticketsystem.DomainLayer.IRepository.IOrderRepository;
import ticketsystem.DomainLayer.exception.OptimisticLockException;
import ticketsystem.DomainLayer.order.ActiveOrder;

@Repository
public class OrderRepository implements IOrderRepository {

    private final AtomicLong counter;
    
    private final ConcurrentHashMap<Long, ActiveOrder> orders;

    public OrderRepository() {
        this.counter = new AtomicLong(0L);
        this.orders = new ConcurrentHashMap<>();
    }

    public synchronized void addOrder(ActiveOrder order) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }

        for (ActiveOrder existingOrder : orders.values()) {

            boolean sameSession =
                    existingOrder.getSessionToken() != null
                            && order.getSessionToken() != null
                            && existingOrder.getSessionToken().equals(order.getSessionToken());

            boolean sameUser =
                    existingOrder.getUserId() != null
                            && order.getUserId() != null
                            && existingOrder.getUserId().equals(order.getUserId());

            if (sameSession || sameUser) {
                throw new IllegalArgumentException(
                        "An active order already exists for this user to another event"
                );
            }
        }

        ActiveOrder copy = order.copy();
        ActiveOrder existing = orders.putIfAbsent(order.getOrderId(), copy);

        if (existing != null) {
            throw new IllegalArgumentException(
                    "Order already exists with id: " + order.getOrderId()
            );
        }
    }

    public ActiveOrder findOrderById(Long orderId) {
        ActiveOrder order = orders.get(orderId);

        if (order == null) {
            return null;
        }

        return order.copy();
    }

    public synchronized void deleteOrder(Long orderId) {
        if (!orders.containsKey(orderId)) {
            throw new IllegalArgumentException(
                    "Order with ID " + orderId + " does not exist"
            );
        }

        orders.remove(orderId);
    }

    public synchronized void deleteOrderBySessionToken(String sessionToken) {
        orders.values().removeIf(order ->
                order.getSessionToken() != null
                        && order.getSessionToken().equals(sessionToken)
        );
    }

    public List<ActiveOrder> getAll() {
        List<ActiveOrder> activeOrders = new ArrayList<>();

        for (Map.Entry<Long, ActiveOrder> entry : orders.entrySet()) {
            activeOrders.add(entry.getValue().copy());
        }

        return activeOrders;
    }

    public ActiveOrder getActiveOrderByUserIdAndEventId(Long userId, Long eventId) {
        return orders.values().stream()
                .filter(order ->
                        order.getUserId() != null
                                && order.getUserId().equals(userId)
                                && order.getEventId().equals(eventId)
                )
                .findFirst()
                .map(ActiveOrder::copy)
                .orElse(null);
    }

    public Long getNextId() {
        return counter.incrementAndGet();
    }

    @Override
    public void updateOrder(ActiveOrder order) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }

        orders.compute(order.getOrderId(), (id, currentOrder) -> {
            if (currentOrder == null) {
                throw new IllegalArgumentException("Order not found with id: " + id);
            }

            if (currentOrder.getVersion() != order.getVersion()) {
                throw new OptimisticLockException(
                        "Order was modified by another request. Order id: " + id
                );
            }

            ActiveOrder copy = order.copy();
            copy.incrementVersion();

            return copy;
        });
    }

    public ActiveOrder getActiveOrderBySessionTokenAndEventId(String sessionToken, Long eventId) {
        return orders.values().stream()
                .filter(order ->
                        order.getSessionToken() != null
                                && order.getSessionToken().equals(sessionToken)
                                && order.getEventId().equals(eventId)
                )
                .findFirst()
                .map(ActiveOrder::copy)
                .orElse(null);
    }

    public synchronized void deleteActiveOrdersByUserId(Long userId) {
        orders.values().removeIf(order ->
                order.getUserId() != null
                        && order.getUserId().equals(userId)
        );
    }

    public ActiveOrder getActiveOrderBySessionToken(String sessionToken) {
        return orders.values().stream()
                .filter(order ->
                        order.getSessionToken() != null
                                && order.getSessionToken().equals(sessionToken)
                )
                .findFirst()
                .map(ActiveOrder::copy)
                .orElse(null);
    }

    public ActiveOrder getActiveOrderByUserId(Long userId) {
        return orders.values().stream()
                .filter(order ->
                        order.getUserId() != null
                                && order.getUserId().equals(userId)
                )
                .findFirst()
                .map(ActiveOrder::copy)
                .orElse(null);
    }

    public List<ActiveOrder> getActiveOrdersByEventId(Long eventId) {
        List<ActiveOrder> activeOrders = new ArrayList<>();

        for (ActiveOrder order : orders.values()) {
            if (order.getEventId() != null && order.getEventId().equals(eventId)) {
                activeOrders.add(order.copy());
            }
        }

        return activeOrders;
    }

    public void clear() {
        orders.clear();
    }
}