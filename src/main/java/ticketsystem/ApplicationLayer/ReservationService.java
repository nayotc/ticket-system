package ticketsystem.ApplicationLayer;

import java.util.ArrayList;
import java.util.List;

import ticketsystem.DTO.OrderDTO;
import ticketsystem.DTO.PaymentDetails;
import ticketsystem.DomainLayer.Reservation;
import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.IRepository.IOrderRepository;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.order.ActiveOrder;

public class ReservationService {

    private final IOrderRepository orderRepository;
    private final IEventRepository eventRepository;
    private final IPaymentService paymentService;
    private final ISecureBarcode secureBarcode;
    private final Reservation reservation;
    private final TokenService tokenService;


    public ReservationService(
            IOrderRepository orderRepository,
            IEventRepository eventRepository,
            TokenService tokenService
           
    ) {
        this.orderRepository = orderRepository;
        this.eventRepository = eventRepository;
        this.reservation=new Reservation();
        this.tokenService = tokenService;
    }
//UC 2.5,2.4
     public void selectSeatTicket(String token, int eventId, int row, int chair) {
        try {
            validateToken(token);
            ActiveOrder order = getOrCreateOrder(token, eventId);
            Event event = getEvent(eventId);

            reservation.selectSeatTicket(order, event, row, chair);
            saveAll(order, event);

        } catch (Exception e) {
            logError("selectSeatTicket failed: " + e.getMessage());
            throw e;
        }
    }
    public void selectStandingTicket(String token, int eventId, int quantity) {
        try {
            
            validateToken(token);
            ActiveOrder order = getOrCreateOrder(token, eventId);
            Event event = getEvent(eventId);
            reservation.selectStandingTicket(order, event, quantity);
            saveAll(order, event);

        } catch (Exception e) {
            logError("selectStandingTicket failed: " + e.getMessage());
            throw e;
        }
    }

    //UC 2.7
    public String viewActiveOrder(String token, int eventId) {
        try {
            validateToken(token);
            ActiveOrder order = getExistingOrder(token, eventId);

            reservation.viewActiveOrder(order);

            return order.toString();

        } catch (Exception e) {
            logError("viewActiveOrder failed: " + e.getMessage());
            throw e;
        }
    }

    public void removeTicketFromActiveOrder(String token, int eventId, int ticketId) {
        try {
            validateToken(token);
            ActiveOrder order = getExistingOrder(token, eventId);
            Event event = getEvent(eventId);
            reservation.removeTicketFromActiveOrder(order, event, ticketId);

            saveAll(order, event);

        } catch (Exception e) {
            logError("removeTicketFromActiveOrder failed: " + e.getMessage());
            throw e;
        }
    }

    // UC 2.8
    public void submitActiveOrderForCheckout(String token, int orderId, int eventId) {
        try {
            validateToken(token);
            ActiveOrder order = getExistingOrder(token, eventId);
            Reservation reservation = getExistingReservation(order);

            reservation.submitActiveOrderForCheckout(order);

            saveAll(reservation, order);
           
        } catch (Exception e) {

            logError("submitActiveOrderForCheckout failed: " + e.getMessage());
            throw e;
        }
    }

    public void checkout(String token, int orderId, int eventId, PaymentDetails details) {
        try {
            validateToken(token);
            ActiveOrder order = getExistingOrder(token, eventId);
   
            Reservation reservation = getExistingReservation(order);

            double amount = order.calculateTotalPrice();
            OrderDTO orderDTO = OrderDTO.from(order);

            //pay
            paymentService.pay(orderDTO,details );

            //update reservation and order status
            reservation.completeCheckout();

            saveAll(reservation, order);

            notifyListeners(orderDTO);

        } catch (Exception e) {
            logError("checkout failed: " + e.getMessage());
            throw e;
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

    // Helper methods
   
    private Event getEvent(int eventId) {
        Event event = eventRepository.getEventById(eventId);

        if (event == null) {
            logWarning("Event not found. eventId=" + eventId);
            throw new IllegalArgumentException("Event not found");
        }

        return event;
    }

    private ActiveOrder getOrCreateOrder(String token, int eventId) {
         ActiveOrder order = findActiveOrder(token, eventId);

    if (order == null) {
        Long userId = tokenService.isMemberToken(token)
                ? tokenService.extractUserId(token)
                : null;

        order = new ActiveOrder(
                orderRepository.getNextId(),
                userId,
                token,
                eventId
        );

        orderRepository.updateOrder(order);
    }

        return order;
    }
    
    private ActiveOrder getExistingOrder(String token, int eventId) {
        ActiveOrder order = findActiveOrder(token, eventId);

        if (order == null) {
            throw new IllegalArgumentException("Active order not found");
        }

        return order;
    }

    private ActiveOrder findActiveOrder(String token, int eventId) {
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

        orderRepository.updateOrder(order);
        eventRepository.updateEvent(event);
    }

  private void validateToken(String token) {
    if (!tokenService.validateToken(token)) {
        throw new IllegalArgumentException("Invalid or expired token");
        }
    }

    
    //for logging - can be replaced with a proper logging framework
    private void logWarning(String msg) {
        /* ... */ }

    private void logError(String msg) {
        /* ... */ }
}