package ticketsystem.InfrastructureLayer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import ticketsystem.DomainLayer.IRepository.IOrderRepository;
import ticketsystem.DomainLayer.exception.OptimisticLockException;
import ticketsystem.DomainLayer.order.ActiveOrder;

/**
 * In-memory implementation used by acceptance/unit tests that construct repositories manually.
 */
public class InMemoryOrderRepository implements IOrderRepository {

    private final Object lock = new Object();
    private long nextGeneratedOrderId = 1L;
    private long nextGeneratedTicketId = 1L;
    private final Map<Long, ActiveOrder> orders = new HashMap<>();

    @Override
    public void addOrder(ActiveOrder order) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }

        synchronized (lock) {
            for (ActiveOrder existingOrder : orders.values()) {
                boolean sameSession = existingOrder.getSessionToken() != null
                        && order.getSessionToken() != null
                        && existingOrder.getSessionToken().equals(order.getSessionToken());

                boolean sameUser = existingOrder.getUserId() != null
                        && order.getUserId() != null
                        && existingOrder.getUserId().equals(order.getUserId());

                if (sameSession || sameUser) {
                    throw new IllegalArgumentException(
                            "An active order already exists for this user to another event");
                }
            }

            long orderId = resolveOrderId(order);
            ActiveOrder persisted = order.copy();
            if (persisted.getOrderId() == null) {
                persisted.setOrderId(orderId);
            }
            persisted.assignMissingTicketIds(this::nextTicketId);

            if (orders.containsKey(persisted.getOrderId())) {
                throw new IllegalArgumentException("Order already exists with id: " + persisted.getOrderId());
            }

            orders.put(persisted.getOrderId(), persisted);
            if (order.getOrderId() == null) {
                order.setOrderId(persisted.getOrderId());
            }
        }
    }

    @Override
    public ActiveOrder findOrderById(Long orderId) {
        synchronized (lock) {
            ActiveOrder order = orders.get(orderId);
            if (order == null) {
                return null;
            }
            return order.copy();
        }
    }

    @Override
    public void updateOrder(ActiveOrder order) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }

        synchronized (lock) {
            if (order.getTickets().isEmpty()) {
                removeOrder(order.getOrderId());
                return;
            }

            ActiveOrder currentOrder = orders.get(order.getOrderId());
            if (currentOrder == null) {
                throw new IllegalArgumentException("Order not found with id: " + order.getOrderId());
            }

            if (!Objects.equals(currentOrder.getVersion(), order.getVersion())) {
                throw new OptimisticLockException(
                        "Order was modified by another request. Order id: " + order.getOrderId());
            }

            ActiveOrder copy = order.copy();
            copy.assignMissingTicketIds(this::nextTicketId);
            copy.incrementVersion();
            orders.put(order.getOrderId(), copy);
        }
    }

    @Override
    public void deleteOrder(Long orderId) {
        synchronized (lock) {
            removeOrder(orderId);
        }
    }

    @Override
    public List<ActiveOrder> getAll() {
        synchronized (lock) {
            List<ActiveOrder> activeOrders = new ArrayList<>();
            for (ActiveOrder order : orders.values()) {
                activeOrders.add(order.copy());
            }
            return activeOrders;
        }
    }

    @Override
    public ActiveOrder getActiveOrderByUserIdAndEventId(Long userId, Long eventId) {
        synchronized (lock) {
            return orders.values().stream()
                    .filter(order -> order.getUserId() != null
                            && order.getUserId().equals(userId)
                            && order.getEventId().equals(eventId))
                    .findFirst()
                    .map(ActiveOrder::copy)
                    .orElse(null);
        }
    }

    @Override
    public Long getNextId() {
        synchronized (lock) {
            return nextGeneratedOrderId++;
        }
    }

    @Override
    public ActiveOrder getActiveOrderBySessionTokenAndEventId(String sessionToken, Long eventId) {
        synchronized (lock) {
            return orders.values().stream()
                    .filter(order -> order.getSessionToken() != null
                            && order.getSessionToken().equals(sessionToken)
                            && order.getEventId().equals(eventId))
                    .findFirst()
                    .map(ActiveOrder::copy)
                    .orElse(null);
        }
    }

    @Override
    public void deleteActiveOrdersByUserId(Long userId) {
        synchronized (lock) {
            orders.values().removeIf(order -> order.getUserId() != null && order.getUserId().equals(userId));
        }
    }

    @Override
    public ActiveOrder getActiveOrderBySessionToken(String sessionToken) {
        synchronized (lock) {
            return orders.values().stream()
                    .filter(order -> order.getSessionToken() != null
                            && order.getSessionToken().equals(sessionToken))
                    .findFirst()
                    .map(ActiveOrder::copy)
                    .orElse(null);
        }
    }

    @Override
    public ActiveOrder getActiveOrderByUserId(Long userId) {
        synchronized (lock) {
            return orders.values().stream()
                    .filter(order -> order.getUserId() != null && order.getUserId().equals(userId))
                    .findFirst()
                    .map(ActiveOrder::copy)
                    .orElse(null);
        }
    }

    @Override
    public List<ActiveOrder> getActiveOrdersByEventId(Long eventId) {
        synchronized (lock) {
            List<ActiveOrder> activeOrders = new ArrayList<>();
            for (ActiveOrder order : orders.values()) {
                if (order.getEventId() != null && order.getEventId().equals(eventId)) {
                    activeOrders.add(order.copy());
                }
            }
            return activeOrders;
        }
    }

    private void removeOrder(Long orderId) {
        if (!orders.containsKey(orderId)) {
            throw new IllegalArgumentException("Order with ID " + orderId + " does not exist");
        }
        orders.remove(orderId);
    }

    private long resolveOrderId(ActiveOrder order) {
        if (order.getOrderId() != null) {
            return order.getOrderId();
        }
        return nextGeneratedOrderId++;
    }

    private long nextTicketId() {
        return nextGeneratedTicketId++;
    }

	@Override
public List<ActiveOrder> findExpiredActiveOrders() {
    LocalDateTime now = LocalDateTime.now();

    synchronized (lock) {
        return orders.values().stream()
                .filter(order -> order.getStatus() == ActiveOrder.OrderStatus.ACTIVE)
                .filter(order -> order.getExpiresAt() != null)
                .filter(order -> !order.getExpiresAt().isAfter(now))
                .map(ActiveOrder::copy)
                .toList();
    }
}

@Override
public List<ActiveOrder> findExpiredActiveOrdersByEventId(Long eventId) {
    LocalDateTime now = LocalDateTime.now();

    synchronized (lock) {
        return orders.values().stream()
                .filter(order -> order.getStatus() == ActiveOrder.OrderStatus.ACTIVE)
                .filter(order -> order.getEventId() != null && order.getEventId().equals(eventId))
                .filter(order -> order.getExpiresAt() != null)
                .filter(order -> !order.getExpiresAt().isAfter(now))
                .map(ActiveOrder::copy)
                .toList();
    }
}

@Override
public List<ActiveOrder> findOrdersAboutToExpire() {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime warningThreshold = now.plusMinutes(1);

    synchronized (lock) {
        return orders.values().stream()
                .filter(order -> order.getStatus() == ActiveOrder.OrderStatus.ACTIVE)
                .filter(order -> order.getExpiresAt() != null)
                .filter(order -> order.getExpiresAt().isAfter(now))
                .filter(order -> !order.getExpiresAt().isAfter(warningThreshold))
                .map(ActiveOrder::copy)
                .toList();
    }
}
}
