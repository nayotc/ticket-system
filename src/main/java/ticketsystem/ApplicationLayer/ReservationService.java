package ticketsystem.ApplicationLayer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import ticketsystem.ApplicationLayer.Events.OrderCompletedListener;
import ticketsystem.DTO.OrderDTO;
import ticketsystem.DTO.PaymentDetails;
import ticketsystem.DTO.seatPositionDTO;
import ticketsystem.DomainLayer.Reservation;
import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.IRepository.IOrderRepository;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.order.ActiveOrder;

public class ReservationService {

    private final IOrderRepository orderRepository;
    private final IEventRepository eventRepository;
    private final TokenService tokenService;
    private final IPaymentService paymentService;
  // private final ISecureBarcode secureBarcode;
    private final Reservation reservation;
    private final List<OrderCompletedListener> listeners = new ArrayList<>();




    public ReservationService(
            IOrderRepository orderRepository,
            IEventRepository eventRepository,
            TokenService tokenService,IPaymentService paymentService) {
        this.orderRepository = orderRepository;
        this.eventRepository = eventRepository;
        this.tokenService = tokenService;
        this.paymentService=paymentService;
        this.reservation=new Reservation();
        

    }

//UC 2.5,2.4
     public boolean selectSeatTicket(String token, Long eventId, Long areaId, seatPositionDTO position) {
        expireOldOrders();
        try {
            tokenService.validateToken(token);

            if(eventRepository.getEventById(eventId)==null) {
                throw new IllegalArgumentException("Event not found");
            }

            ActiveOrder order = getOrCreateOrder(token, eventId);
            Event event = eventRepository.getEventById(eventId);
            reservation.selectSeatTicket(order, event, areaId, position);

            saveAll(order, event);
            return true;

        } catch (Exception e) {
            logWarning("selectSeatTicket failed: " + e.getMessage());
            throw e;
        }
    }
    public boolean selectStandingTicket(String token, Long eventId, Long areaId, int quantity) {
        expireOldOrders();
        try {
            tokenService.validateToken(token);
            if(eventRepository.getEventById(eventId)==null) {
                    throw new IllegalArgumentException("Event not found");
                }
            ActiveOrder order = getOrCreateOrder(token, eventId);
            if (quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than zero");
            }
            Event event = eventRepository.getEventById(eventId);
            reservation.selectStandingTicket(order, event, areaId, quantity);
            saveAll(order, event);
            return true;
        } catch (Exception e) {
            logWarning("selectStandingTicket failed: " + e.getMessage());
            throw e;
        }
    }


    //UC 2.7
    public boolean removeTicketFromActiveOrder(String token, Long eventId, Long ticketId) {
        expireOldOrders();
        try {
            tokenService.validateToken(token);
            ActiveOrder order = findActiveOrder(token, eventId);
            if (order==null) {
                throw new IllegalStateException("No active order found for this event");
            }
            Event event = eventRepository.getEventById(eventId);
            reservation.removeTicketFromActiveOrder(order, event, ticketId);

            saveAll(order, event);
            return true;

        } catch (Exception e) {
            logWarning("removeTicketFromActiveOrder failed: " + e.getMessage());
            throw e;
        }
    }

    // UC 2.8
    private boolean submitActiveOrderForCheckout(String token, Long eventId) {
            ActiveOrder order = findActiveOrder(token, eventId);
            Event event = eventRepository.getEventById(eventId);
            reservation.submitActiveOrderForCheckout(order, event);
            saveAll(order, event);
            return true;
    }

    public boolean enterUserDetails(String name, String Email){
        return true;
    }


    //
    public boolean checkout(String token, Long eventId, PaymentDetails details) {
        expireOldOrders();
        try {    
            tokenService.validateToken(token);
            submitActiveOrderForCheckout(token, eventId);
            ActiveOrder order = findActiveOrder(token, eventId);
            Event event = eventRepository.getEventById(eventId);
            BigDecimal amount = reservation.calculateTotalPrice(order, event);
            OrderDTO orderDTO = order.toDTO(event.getName(),event.getLocation().toString(), event.getCompanyId() );
            

            //pay
            boolean paymentResult=paymentService.pay(amount,details);
            if (!paymentResult) {
                throw new IllegalStateException("Payment failed");
            }
            
            reservation.completeCheckout(order, event);
            saveAll(order, event);
            notifyListeners(orderDTO);
            return true;

        } catch (Exception e) {
            logWarning("checkout failed: " + e.getMessage());
            return false;
        }
    }
    //listener
    public void addOrderListener(OrderCompletedListener listener) {
            listeners.add(listener);
    }

    private void notifyListeners(OrderDTO order) {
        for (OrderCompletedListener listener : listeners) {
            listener.onOrderCompleted(order);
        }
    }

    //Helper methods

    private ActiveOrder getOrCreateOrder(String token, Long eventId) {
         ActiveOrder order = findActiveOrder(token, eventId);

    if (order == null) {
        Long userId = tokenService.isMemberToken(token)
                ? tokenService.extractUserId(token)
                : null;

        order = new ActiveOrder(
                orderRepository.getNextId(),token,
                userId,
                eventId
        );

        orderRepository.updateOrder(order);
    }

        return order;
    }
    

    private ActiveOrder findActiveOrder(String token, Long eventId) {

        ActiveOrder order;

        if (tokenService.isGuestToken(token)) {
            order = orderRepository.getActiveOrderBySessionTokenAndEventId(
                    token,
                    eventId
            );
        } else {
            Long userId = tokenService.extractUserId(token);

            order = orderRepository.getActiveOrderByUserIdAndEventId(
                    userId,
                    eventId
            );
        }
        if (order == null) {
            throw new IllegalArgumentException(
                    "Active order was expired or does not exist for this event"
            );
        }
        return order;
    }
    

    private void saveAll( ActiveOrder order, Event event) {
        if(order.getStatus()==ActiveOrder.OrderStatus.CANCELLED|| order.getStatus()==ActiveOrder.OrderStatus.COMPLETED) {
            orderRepository.deleteOrder(order.getOrderId());
        }
        else{
            orderRepository.updateOrder(order);
        }
        eventRepository.updateEvent(event);
        
    }


    private void expireOldOrders() {
        List<ActiveOrder> allOrders = orderRepository.getAll();
        for (ActiveOrder order : allOrders) {
            if (order.getStatus() != ActiveOrder.OrderStatus.PENDING_CHECKOUT && order.isExpired()) {
                Event event = eventRepository.getEventById(order.getEventId());
                reservation.expire(event,order);
                orderRepository.deleteOrder(order.getOrderId());
                //logWarning("Expired order cancelled: " + order.getOrderId());
         }
    }
    }
    
    //for logging - can be replaced with a proper logging framework
    private void logWarning(String msg) {
        /* ... */ }

}