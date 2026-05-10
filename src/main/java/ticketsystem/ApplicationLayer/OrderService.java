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

}