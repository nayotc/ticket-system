package ticketsystem.ApplicationLayer;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import ticketsystem.DTO.ActiveOrderDTO;
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

    public ActiveOrderDTO viewActiveOrder(String token, Long orderId) {
        try {
            validateToken(token);
            ActiveOrder order = orderRepository.findOrderById(orderId);
            if (order == null) {
                throw new IllegalStateException("No active order found for this event");
            }
            ObjectMapper objectMapper = new ObjectMapper();
           ActiveOrderDTO activeOrderDTO = objectMapper.convertValue(
                order, 
                ActiveOrderDTO.class
            );
            return activeOrderDTO;
        } 
        catch (Exception e) {
            logWarning("viewActiveOrder failed: " + e.getMessage());
            throw e;
        }
    }

    private void logWarning(String msg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'logWarning'");
    }

    private void validateToken(String token) {
        if (token == null || !tokenService.validateToken(token)) {
            throw new IllegalArgumentException("Invalid or missing token");
        }
    }

}