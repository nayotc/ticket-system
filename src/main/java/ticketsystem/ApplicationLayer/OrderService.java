package ticketsystem.ApplicationLayer;

import java.util.List;

import ticketsystem.ApplicationLayer.Events.EventUpdatesListener;
import ticketsystem.DomainLayer.IRepository.IOrderRepository;
import ticketsystem.DomainLayer.order.ActiveOrder;

public class OrderService implements EventUpdatesListener{

    private final IOrderRepository orderRepository;
    private final NotificationsService notificationsService;

    public OrderService(IOrderRepository orderRepository, NotificationsService notificationsService) {
        this.orderRepository = orderRepository;
        this.notificationsService = notificationsService;
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
    public void onEventUpdated(Long eventId, String updateMessage) {
        List<ActiveOrder> affectedOrders = orderRepository.getActiveOrdersByEventId(eventId);
        for (ActiveOrder order : affectedOrders) {
            notificationsService.notifyUser(order.getSessionToken(), "Update for your order number - " + order.getOrderId() + " :/n" + updateMessage);
        }
    }

}