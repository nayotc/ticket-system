package ticketsystem.ApplicationLayer;

import ticketsystem.DomainLayer.IRepository.IOrderRepository;
import ticketsystem.DomainLayer.order.ActiveOrder;

public class OrderService {

    private final IOrderRepository orderRepository;
    private final TokenService tokenService;

    public OrderService(IOrderRepository orderRepository, TokenService tokenService) {
        this.orderRepository = orderRepository;
        this.tokenService = tokenService;
    }

    public ActiveOrder getOrCreateActiveOrder(String token, int eventId) {
        int userId = getUserIdFromToken(token);

        ActiveOrder order =
                orderRepository.getActiveOrderByUserIdAndEventId(userId, eventId);

        if (order == null) {
            int orderId = orderRepository.getNextId();
            order = new ActiveOrder(orderId, userId, eventId);
            orderRepository.addOrder(order);
        }

        return order;
        }
    }