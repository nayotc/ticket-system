package ticketsystem.ApplicationLayer;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ticketsystem.ApplicationLayer.Events.EventUpdatesListener;
import ticketsystem.ApplicationLayer.Events.UserExitListener;
import ticketsystem.ApplicationLayer.Events.UserLoginListener;
import ticketsystem.ApplicationLayer.ISystemLogger.LogLevel;
import ticketsystem.DomainLayer.IRepository.IOrderRepository;
import ticketsystem.DomainLayer.order.ActiveOrder;
import ticketsystem.DomainLayer.order.Ticket;

@Service
public class OrderService implements UserLoginListener, EventUpdatesListener,UserExitListener {

    private final IOrderRepository orderRepository;
    private final TokenService tokenService;
    private final ISystemLogger logger;
    private final INotifier notificationsService;

    public OrderService(IOrderRepository orderRepository, TokenService tokenService, ISystemLogger logger,
            INotifier notificationsService) {
        this.orderRepository = orderRepository;
        this.tokenService = tokenService;
        this.logger = logger;
        this.notificationsService = notificationsService;
    }

    @Override
    @Transactional
    public void onUserLogin(String guestToken, String memberToken) {

        try {
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

            // User already has an active order for another event
            if (!memberOrder.getEventId().equals(guestOrder.getEventId())) {
                guestOrder.cancelOrder();
                orderRepository.updateOrder(guestOrder);
                return;
            }

            for (Ticket ticket : guestOrder.getTickets()) {
                memberOrder.addTicket(ticket);
            }

            orderRepository.updateOrder(memberOrder);
            orderRepository.deleteOrder(guestOrder.getOrderId());

        } catch (Exception e) {
            logger.logEvent("mergeGuestOrderIntoMemberOrders failed: " + e.getMessage(), LogLevel.WARN);
            throw e;
        }

    }
    @Override
    @Transactional
    public void onUserExit(String guestToken) {

        try {
            ActiveOrder guestOrder = orderRepository.getActiveOrderBySessionToken(guestToken);
            if (guestOrder == null || guestOrder.getUserId()!= null) {
                return;
            }
            guestOrder.cancelOrder();
            orderRepository.updateOrder(guestOrder);

        } catch (Exception e) {
            logger.logEvent("Failed to cancel guest order: " + e.getMessage(), LogLevel.WARN);
            throw e;
        }

    }
    
    @Override
    @Transactional
    public void onEventCanceled(Long eventId) {
        List<ActiveOrder> affectedOrders = orderRepository.getActiveOrdersByEventId(eventId);
        for (ActiveOrder order : affectedOrders) {
            order.cancelOrder();
            orderRepository.updateOrder(order);
            notifyTokenHolder(order.getSessionToken(),
                    "Your order number: " + order.getOrderId() + " - has been canceled due to event cancellation.");
        }
    }

        @Override
        @Transactional
        public boolean onEventCancellationRequested(Long eventId) {
            return true;
        }



    private void notifyTokenHolder(String token, String message) {
        if (notificationsService == null || token == null || token.isBlank()
                || message == null || message.isBlank()) {
            return;
        }

        if (tokenService.isMemberToken(token)) {
            Long memberId = tokenService.extractUserId(token);
            if (memberId != null) {
                notificationsService.notifyMember(memberId, message);
                return;
            }
        }

        notificationsService.notifyGuest(token, message);
    }

    @Override
    public void onEventUpdated(Long eventId, LocalDateTime date, String Location, String updateMessage) {
        List<ActiveOrder> affectedOrders = orderRepository.getActiveOrdersByEventId(eventId);
        for (ActiveOrder order : affectedOrders) {
            notifyTokenHolder(order.getSessionToken(),
                    "Update for your order number - " + order.getOrderId() + " :/n" + updateMessage);
        }
    }

}
