package ticketsystem.ApplicationLayer;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import ticketsystem.ApplicationLayer.ISystemLogger.LogLevel;
import ticketsystem.ApplicationLayer.Events.UserSignInListener;
import ticketsystem.DTO.ActiveOrderDTO;
import ticketsystem.DTO.OrderDTO;
import ticketsystem.DomainLayer.IRepository.IOrderRepository;
import ticketsystem.DomainLayer.order.ActiveOrder;
import ticketsystem.DomainLayer.order.Ticket;

public class OrderService implements UserSignInListener {

    private final IOrderRepository orderRepository;
    private final TokenService tokenService;
    private final ISystemLogger logger;

    public OrderService(IOrderRepository orderRepository, TokenService tokenService, ISystemLogger logger) {
        this.orderRepository = orderRepository;
        this.tokenService = tokenService;
        this.logger = logger;
    }

    @Override
    public void onUserSignIn(String guestToken, String memberToken) {
            
        try{
        ActiveOrder guestOrder = orderRepository.getActiveOrderBySessionToken(guestToken);
        Long userId = tokenService.extractUserId(memberToken);
        if (guestOrder == null) {
            return;
        }

        ActiveOrder memberOrder = orderRepository.getActiveOrderByUserId(userId);

        if (memberOrder == null) {
            guestOrder.setUserId(userId);
            guestOrder.setSessionToken(memberToken);
            orderRepository.updateOrder(guestOrder);
            return;
        }

        //User already has an active order for another event
        if (!memberOrder.getEventId().equals(guestOrder.getEventId())) {
            orderRepository.deleteOrder(guestOrder.getOrderId());
            return;
        
        }

        for (Ticket ticket : guestOrder.getTickets()) {
            memberOrder.addTicket(ticket);
        }

        orderRepository.updateOrder(memberOrder);
        orderRepository.deleteOrder(guestOrder.getOrderId());

    }
    catch (Exception e) {
        logger.logEvent("mergeGuestOrderIntoMemberOrders failed: " + e.getMessage(), LogLevel.WARN);
        throw e;
    }

}
}