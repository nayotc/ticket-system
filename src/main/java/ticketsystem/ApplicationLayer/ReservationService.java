package ticketsystem.ApplicationLayer;

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
        ActiveOrder order = getOrCreateOrder(token, eventId);
        try {
            validateToken(token);
            Event event = eventRepository.getEventById(eventId);
            reservation.selectSeatTicket(order, event, areaId, position);

            saveAll(order, event);
            return true;

        } catch (Exception e) {
            logWarning("selectSeatTicket failed: " + e.getMessage());
            if(order.getStatus()==ActiveOrder.OrderStatus.CANCELLED) {
                orderRepository.deleteOrder(order.getOrderId());
            }
            return false;
        }
    }
    public boolean selectStandingTicket(String token, Long eventId, Long areaId, int quantity) {
        ActiveOrder order = getOrCreateOrder(token, eventId);
        try {
            if (quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than zero");
            }

            validateToken(token);
            
            Event event = eventRepository.getEventById(eventId);
            reservation.selectStandingTicket(order, event, areaId, quantity);
            saveAll(order, event);
            return true;
        } catch (Exception e) {
            logWarning("selectStandingTicket failed: " + e.getMessage());
            if(order.getStatus()==ActiveOrder.OrderStatus.CANCELLED) {
                orderRepository.deleteOrder(order.getOrderId());
            }
            return false;
        }
    }


    //UC 2.7
    public boolean removeTicketFromActiveOrder(String token, Long eventId, Long ticketId) {
        ActiveOrder order = findActiveOrder(token, eventId);
     
        try {
            validateToken(token); 
            if (order==null) {
                throw new IllegalStateException("No active order found for this event");
            }
            Event event = eventRepository.getEventById(eventId);
            reservation.removeTicketFromActiveOrder(order, event, ticketId);

            saveAll(order, event);
            return true;

        } catch (Exception e) {
            if(order!=null&& order.getStatus()==ActiveOrder.OrderStatus.CANCELLED) {
                orderRepository.deleteOrder(order.getOrderId());
            }

            logWarning("removeTicketFromActiveOrder failed: " + e.getMessage());
            return false;
        }
    }

    // UC 2.8
    public boolean submitActiveOrderForCheckout(String token, Long eventId) {
        try {
            validateToken(token);
            ActiveOrder order = findActiveOrder(token, eventId);
            Event event = eventRepository.getEventById(eventId);
            reservation.submitActiveOrderForCheckout(order, event);
            saveAll(order, event);
            
            return true;
           
        } catch (Exception e) {
            ActiveOrder order = findActiveOrder(token, eventId);
                if(order != null && order.getStatus()==ActiveOrder.OrderStatus.CANCELLED) {
                    orderRepository.deleteOrder(order.getOrderId());
                }
            logWarning("submitActiveOrderForCheckout failed: " + e.getMessage());
            return false;
        }
    }

    public boolean enterUserDetails(String name, String Email){
        return true;
    }


    //
    public boolean checkout(String token, Long eventId, PaymentDetails details) {
        try {
            validateToken(token);
            ActiveOrder order = findActiveOrder(token, eventId);
            Event event = eventRepository.getEventById(eventId);
            double amount = reservation.calculateTotalPrice(order, event);
            OrderDTO orderDTO = order.toDTO(event.getName(),event.getLocation(), event.getCompanyId() );
            

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
        if (tokenService.isGuestToken(token)) {
            return orderRepository.getActiveOrderBySessionTokenAndEventId(
                    token,
                    eventId
            );
        }
        
        Long userId = tokenService.extractUserId(token);

        return orderRepository.getActiveOrderByUserIdAndEventId(
                userId,
                eventId
        );
     }
    

    private void saveAll( ActiveOrder order, Event event) {
        if(order.getStatus()==ActiveOrder.OrderStatus.CANCELLED) {
            orderRepository.deleteOrder(order.getOrderId());
        }
        else{
            orderRepository.updateOrder(order);
        }
        eventRepository.updateEvent(event);
        
    }

  private void validateToken(String token) {
    if (!tokenService.validateToken(token)) {
        throw new IllegalArgumentException("Invalid or expired token");
        }
    }

    public void start() {
        ScheduledExecutorService scheduler =
                Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(
                () ->expireOldOrders(),
                0,
                1,
                TimeUnit.MINUTES
        );
    }

    public void stop() {
        // Implement if needed to gracefully shut down the scheduler
    }

    public void expireOldOrders() {
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

    private void logError(String msg) {
        /* ... */ }
}