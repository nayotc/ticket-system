package ticketsystem.InfrastructureLayer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ticketsystem.DomainLayer.IRepository.IOrderRepository;
import ticketsystem.DomainLayer.exception.OptimisticLockException;
import ticketsystem.DomainLayer.order.ActiveOrder;
import ticketsystem.InfrastructureLayer.persistence.OrderJpaRepository;

@Repository
public class OrderRepository implements IOrderRepository {

    private final OrderJpaRepository orderJpaRepository;

    public OrderRepository(OrderJpaRepository orderJpaRepository) {
        this.orderJpaRepository = orderJpaRepository;
    }

    @Override
    @Transactional
    public void addOrder(ActiveOrder order) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }
        validateNoConflictingActiveOrder(order);
        ActiveOrder saved = orderJpaRepository.save(order);
        if (order.getOrderId() == null && saved.getOrderId() != null) {
            order.setOrderId(saved.getOrderId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ActiveOrder findOrderById(Long orderId) {
        return orderJpaRepository.findByIdWithTickets(orderId)
                .map(ActiveOrder::copy)
                .orElse(null);
    }

    @Override
    @Transactional
    public void updateOrder(ActiveOrder order) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }
        if (order.getTickets().isEmpty()) {
            deleteOrder(order.getOrderId());
            return;
        }
        try {
            orderJpaRepository.save(order.copy());
        } catch (OptimisticLockingFailureException ex) {
            throw new OptimisticLockException(
                    "Order was modified by another request. Order id: " + order.getOrderId());
        }
    }

    @Override
    @Transactional
    public void deleteOrder(Long orderId) {
        if (!orderJpaRepository.existsById(orderId)) {
            throw new IllegalArgumentException("Order with ID " + orderId + " does not exist");
        }
        orderJpaRepository.deleteById(orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActiveOrder> getAll() {
        return orderJpaRepository.findAllWithTickets().stream()
                .map(ActiveOrder::copy)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ActiveOrder getActiveOrderByUserIdAndEventId(Long userId, Long eventId) {
        return orderJpaRepository.findByUserIdAndEventIdWithTickets(userId, eventId)
                .map(ActiveOrder::copy)
                .orElse(null);
    }

    @Override
    public Long getNextId() {
        throw new UnsupportedOperationException(
                "Order IDs are assigned by the database; pass null when creating ActiveOrder.");
    }

    @Override
    @Transactional(readOnly = true)
    public ActiveOrder getActiveOrderBySessionTokenAndEventId(String sessionToken, Long eventId) {
        return orderJpaRepository.findBySessionTokenAndEventIdWithTickets(sessionToken, eventId)
                .map(ActiveOrder::copy)
                .orElse(null);
    }

    @Override
    @Transactional
    public void deleteActiveOrdersByUserId(Long userId) {
        List<ActiveOrder> orders = orderJpaRepository.findAllWithTickets().stream()
                .filter(order -> userId.equals(order.getUserId()))
                .toList();
        orderJpaRepository.deleteAll(orders);
    }

    @Override
    @Transactional(readOnly = true)
    public ActiveOrder getActiveOrderBySessionToken(String sessionToken) {
        return orderJpaRepository.findBySessionTokenWithTickets(sessionToken)
                .map(ActiveOrder::copy)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public ActiveOrder getActiveOrderByUserId(Long userId) {
        return orderJpaRepository.findByUserIdWithTickets(userId)
                .map(ActiveOrder::copy)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActiveOrder> getActiveOrdersByEventId(Long eventId) {
        return orderJpaRepository.findByEventIdWithTickets(eventId).stream()
                .map(ActiveOrder::copy)
                .collect(Collectors.toList());
    }

    private void validateNoConflictingActiveOrder(ActiveOrder order) {
        if (order.getSessionToken() != null) {
            orderJpaRepository.findBySessionTokenWithTickets(order.getSessionToken())
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException(
                                "An active order already exists for this user to another event");
                    });
        }
        if (order.getUserId() != null) {
            orderJpaRepository.findByUserIdWithTickets(order.getUserId())
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException(
                                "An active order already exists for this user to another event");
                    });
        }
    }
@Override
@Transactional(readOnly = true)
public List<ActiveOrder> findExpiredActiveOrders() {
    return orderJpaRepository.findExpiredActiveOrders(LocalDateTime.now()).stream()
            .map(ActiveOrder::copy)
            .collect(Collectors.toList());
}

@Override
@Transactional(readOnly = true)
public List<ActiveOrder> findExpiredActiveOrdersByEventId(Long eventId) {
    return orderJpaRepository.findExpiredActiveOrdersByEventId(eventId, LocalDateTime.now()).stream()
            .map(ActiveOrder::copy)
            .collect(Collectors.toList());
}

@Override
@Transactional(readOnly = true)
public List<ActiveOrder> findOrdersAboutToExpire() {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime warningThreshold = now.plusMinutes(1);

    return orderJpaRepository.findOrdersAboutToExpire(now, warningThreshold).stream()
            .map(ActiveOrder::copy)
            .collect(Collectors.toList());
}
}
