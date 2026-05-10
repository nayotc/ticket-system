package ticketsystem.ApplicationLayer;

import ticketsystem.DTO.OrderDTO;
import ticketsystem.DomainLayer.IRepository.IOrderRepository;
import ticketsystem.DomainLayer.order.ActiveOrder;

public class OrderService {

    private final IOrderRepository orderRepository;
    private final TokenService tokenService;

    public OrderService(IOrderRepository orderRepository, TokenService tokenService) {
        this.orderRepository = orderRepository;
        this.tokenService = tokenService;
    }

    //uc 2.7

    public OrderDTO viewActiveOrder(String token, int orderId) {
        try {
            validateToken(token);
            ActiveOrder order = orderRepository.getActiveOrderById(orderId);
            if (order == null) {
                throw new IllegalStateException("No active order found for this event");
            }
            OrderDTO orderDTO = order.toDTO();
            return orderDTO;
        } 
        catch (Exception e) {
            logError("viewActiveOrder failed: " + e.getMessage());
            throw e;
        }
    }

    private void logError(String string) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'logError'");
    }

    private void validateToken(String token) {
        if (token == null || !tokenService.validateToken(token)) {
            throw new IllegalArgumentException("Invalid or missing token");
        }
    }

}