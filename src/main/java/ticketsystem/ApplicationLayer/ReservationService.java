package ticketsystem.ApplicationLayer;

import java.util.ArrayList;
import java.util.List;

import ticketsystem.ApplicationLayer.Events.OrderCompletedListener;
import ticketsystem.DTO.OrderDTO;
import ticketsystem.DTO.seatPositionDTO;
import ticketsystem.DomainLayer.Reservation;
import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.IRepository.IOrderRepository;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.order.ActiveOrder;

public class ReservationService {

    private final IOrderRepository orderRepository;
    private final IEventRepository eventRepository;
    private final IPaymentService paymentService;
  //  private final ISecureBarcode secureBarcode;
    private final Reservation reservation;
    private final TokenService tokenService;
    private final List<OrderCompletedListener> listeners = new ArrayList<>();




    public ReservationService(
            IOrderRepository orderRepository,
            IEventRepository eventRepository,
            TokenService tokenService) {
        this.orderRepository = orderRepository;
        this.eventRepository = eventRepository;
        this.reservation=new Reservation();
        this.tokenService = tokenService;

    }

//UC 2.5,2.4
     public void selectSeatTicket(String token, Long eventId, Long areaId, seatPositionDTO position) {
        try {
            validateToken(token);
            ActiveOrder order = getOrCreateOrder(token, eventId);
            Event event = getEvent(eventId);

            reservation.selectSeatTicket(order, event, areaId, position);
            saveAll(order, event);

        } catch (Exception e) {
            logError("selectSeatTicket failed: " + e.getMessage());
            throw e;
        }
    }
    public void selectStandingTicket(String token, Long eventId, Long areaId, int quantity) {
        try {
            if (quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than zero");
            }

            validateToken(token);
            ActiveOrder order = getOrCreateOrder(token, eventId);
            Event event = getEvent(eventId);
            reservation.selectStandingTicket(order, event, areaId, quantity);
            saveAll(order, event);

        } catch (Exception e) {
            logError("selectStandingTicket failed: " + e.getMessage());
            throw e;
        }
    }


    //UC 2.7
    public void removeTicketFromActiveOrder(String token, Long eventId, Long ticketId) {
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
    public void submitActiveOrderForCheckout(String token, int eventId) {
        try {
            validateToken(token);
            ActiveOrder order = getExistingOrder(token, eventId);
            Event event = getEvent(eventId);
            reservation.submitActiveOrderForCheckout(order, event);

            saveAll(order, event);
           
        } catch (Exception e) {

            logError("submitActiveOrderForCheckout failed: " + e.getMessage());
            throw e;
        }
    }

    public void checkout(String token, int eventId, PaymentDetails details) {
        try {
            validateToken(token);
            ActiveOrder order = getExistingOrder(token, eventId);
            Event event = getEvent(eventId);

            double amount = order.calculateTotalPrice();
            OrderDTO orderDTO = order.toDTO();

            //pay
            boolean paymentResult=paymentService.pay(orderDTO,details );
            if (!paymentResult) {
                throw new IllegalStateException("Payment failed");
            }
            
            reservation.completeCheckout(order, event);
            saveAll(order, event);
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

    //Helper methods
    private Event getEvent(Long eventId) {
        Event event = eventRepository.getEventById(eventId);

        if (event == null) {
            logWarning("Event not found. eventId=" + eventId);
            throw new IllegalArgumentException("Event not found");
        }

        return event;
    }

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
    
        private ActiveOrder getExistingOrder(String token, Long eventId) {
        ActiveOrder order = findActiveOrder(token, eventId);

        if (order == null) {
            throw new IllegalArgumentException("Active order not found");
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

        orderRepository.updateOrder(order);
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