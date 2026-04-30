package ticketsystem.ApplicationLayer;

import ticketsystem.DomainLayer.Reservation;
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
        OrderOwner owner = getOrderOwnerFromToken(token);

        ActiveOrder order;

        if (owner.isGuest()) {
            order = orderRepository.getActiveOrderBySessionTokenAndEventId(
                    owner.getSessionToken(),
                    eventId
            );
        } else {
            order = orderRepository.getActiveOrderByUserIdAndEventId(
                    owner.getUserId(),
                    eventId
            );
        }

        if (order == null) {
            int orderId = orderRepository.getNextId();

            order = new ActiveOrder(
                    orderId,
                    owner.getUserId(),
                    owner.getSessionToken(),
                    eventId
            );

            orderRepository.addOrder(order);
        }

        return order;
    }


    private OrderOwner getOrderOwnerFromToken(String token) {
        if (!tokenService.validateToken(token)) {
            throw new IllegalArgumentException("Invalid or expired token");
        }

        String subject = tokenService.extractSubject(token);

        if (tokenService.isGuestToken(token)) {
            return new OrderOwner(null, subject);
        }

        return new OrderOwner(Integer.parseInt(subject), subject);
    }

    private static class OrderOwner {
        private final Integer userId;
        private final String sessionToken;

        public OrderOwner(Integer userId, String sessionToken) {
            this.userId = userId;
            this.sessionToken = sessionToken;
        }

        public Integer getUserId() {
            return userId;
        }

        public String getSessionToken() {
            return sessionToken;
        }

        public boolean isGuest() {
            return userId == null;
        }
    }
}