package ticketsystem.ApplicationLayer;

import java.time.LocalDateTime;
import java.util.List;

import ticketsystem.ApplicationLayer.ISystemLogger.LogLevel;
import ticketsystem.ApplicationLayer.Events.EventUpdatesListener;
import ticketsystem.ApplicationLayer.Events.UserLoginListener;
import ticketsystem.DomainLayer.IRepository.IOrderRepository;
import ticketsystem.DomainLayer.order.ActiveOrder;
import ticketsystem.DomainLayer.order.Ticket;

public class OrderService implements UserLoginListener, EventUpdatesListener{

    private final IOrderRepository orderRepository;
    private final TokenService tokenService;
    private final ISystemLogger logger;
    private final INotifier notificationsService;

    public OrderService(IOrderRepository orderRepository,TokenService tokenService,ISystemLogger logger,INotifier notificationsService) {
        this.orderRepository = orderRepository;
        this.tokenService = tokenService;
        this.logger = logger;
        this.notificationsService = notificationsService;
    }

     @Override
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
    public void onEventCanceled(Long eventId) {
        List<ActiveOrder> affectedOrders = orderRepository.getActiveOrdersByEventId(eventId);
        for (ActiveOrder order : affectedOrders) {
            order.cancelOrder();
            orderRepository.updateOrder(order);
            notificationsService.notifyUser(order.getSessionToken(), "Your order number: " + order.getOrderId() + " - has been canceled due to event cancellation.");
        }
    }

    @Override
    public void onEventUpdated(Long eventId, LocalDateTime date, String Location, String updateMessage) {
        List<ActiveOrder> affectedOrders = orderRepository.getActiveOrdersByEventId(eventId);
        for (ActiveOrder order : affectedOrders) {
            notificationsService.notifyUser(order.getSessionToken(), "Update for your order number - " + order.getOrderId() + " :/n" + updateMessage);
        }
    }



}