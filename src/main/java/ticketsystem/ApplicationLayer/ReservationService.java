package ticketsystem.ApplicationLayer;

import java.util.ArrayList;
import java.util.List;

import ticketsystem.DomainLayer.Reservation;
import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.IRepository.IOrderRepository;
import ticketsystem.DomainLayer.IRepository.IReservationRepository;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.order.ActiveOrder;

public class ReservationService {

    private final IOrderRepository orderRepository;
    private final IEventRepository eventRepository;
    private final IReservationRepository reservationRepository;
    private final IPaymentService paymentService;
    private final ISecureBarcode secureBarcode;
    private final List<OrderCompletedListener> listeners = new ArrayList<>();
    private final TokenService tokenService;


    public ReservationService(
            IOrderRepository orderRepository,
            IEventRepository eventRepository,
            IReservationRepository reservationRepository,
            TokenService tokenService,
            IPaymentService paymentService,
            ISecureBarcode secureBarcode
    ) {
        this.orderRepository = orderRepository;
        this.eventRepository = eventRepository;
        this.reservationRepository = reservationRepository;
        this.tokenService = tokenService;
        this.paymentService = paymentService;
        this.secureBarcode = secureBarcode;
    }

    // UC 2.4, 2.5
    public void selectSeatTicket(String token, int eventId, int row, int chair) {
        try {
            OrderOwner owner = getOrderOwnerFromToken(token);
            ActiveOrder order = getOrCreateOrder(owner, eventId);
            Event event = getEvent(eventId);
            Reservation reservation = getOrCreateReservation(order, event);

            reservation.selectSeatTicket(eventId, row, chair);

            saveAll(reservation, order, event);

        } catch (Exception e) {
            System.err.println("Error in selectSeatTicket: " + e.getMessage());
            throw e;
        }
    }

    public void selectStandingTicket(String token, int eventId, int quantity) {
        try {
            OrderOwner owner = getOrderOwnerFromToken(token);
            ActiveOrder order = getOrCreateOrder(owner, eventId);
            Event event = getEvent(eventId);
            Reservation reservation = getOrCreateReservation(order, event);

            double price = event.getStandingArea().getPrice();
            reservation.selectStandingTicket(eventId, price, quantity);

            saveAll(reservation, order, event);

        } catch (Exception e) {
            System.err.println("Error in selectStandingTicket: " + e.getMessage());
            throw e;
        }
    }

    // UC 2.7
    public String viewActiveOrder(String token, int eventId) {
        try {
            OrderOwner owner = getOrderOwnerFromToken(token);
            ActiveOrder order = getExistingOrder(owner, eventId);
            Reservation reservation = getExistingReservation(order);

            reservation.viewActiveOrder(order);

            return order.toString();

        } catch (Exception e) {
            System.err.println("Error in viewActiveOrder: " + e.getMessage());
            throw e;
        }
    }

    public void removeTicketFromActiveOrder(String token, int eventId, int ticketId) {
        try {
            OrderOwner owner = getOrderOwnerFromToken(token);
            ActiveOrder order = getExistingOrder(owner, eventId);
            Reservation reservation = getExistingReservation(order);

            reservation.removeTicketFromActiveOrder(order, ticketId);

            saveAll(reservation, order);

        } catch (Exception e) {
            System.err.println("Error in removeTicketFromActiveOrder: " + e.getMessage());
            throw e;
        }
    }

    // UC 2.8
    public void submitActiveOrderForCheckout(String token, int orderId) {
        try {
            OrderOwner owner = getOrderOwnerFromToken(token);
            ActiveOrder order = getExistingOrder(owner, orderId);
            Reservation reservation = getExistingReservation(order);

            reservation.submitActiveOrderForCheckout(order);

            saveAll(reservation, order);
           
        } catch (Exception e) {

            System.err.println("Error in submitActiveOrderForCheckout: " + e.getMessage());
            throw e;
        }
    }

    public void checkout(String token, int orderId, PaymentDetails details) {
        try {
            OrderOwner owner = getOrderOwnerFromToken(token);
            ActiveOrder order = getExistingOrder(owner, orderId);
            Reservation reservation = getExistingReservation(order);

            double amount = order.calculateTotalPrice();

            //pay
            paymentService.pay(OrderDTO.from(order),details );

            //update reservation and order status
            reservation.completeCheckout();

            saveAll(reservation, order);

            notifyListeners(OrderDTO.from(order));

        } catch (Exception e) {
            System.err.println("Error in checkout: " + e.getMessage());
            throw e;
        }
    }

    public void addOrderListener(OrderCompletedListener listener) {
            listeners.add(listener);
    }

    private void notifyListeners(OrderDTO order) {
        for (OrderCompletedListener listener : listeners) {
            listener.onOrderCompleted(order);
        }
    }

    private Reservation getExistingReservation(ActiveOrder order) {
        Reservation reservation = reservationRepository.getReservationByOrderId(order.getOrderId());
        Event event = eventRepository.getEventById(order.getEventId());

        if (reservation == null) {
            throw new IllegalArgumentException("Reservation not found");
        }

        if (reservation.isExpired()) {
            reservation.expire();

            reservationRepository.deleteReservationByOrderId(order.getOrderId());
            orderRepository.updateOrder(order);
            eventRepository.updateEvent(event);

            throw new IllegalStateException("Reservation expired");
        }

        return reservation;
    }

    private Reservation getOrCreateReservation(ActiveOrder order, Event event) {
        Reservation reservation = reservationRepository.getReservationByOrderId(order.getOrderId());

        if (reservation == null) {
            return createReservation(order, event);
        }

        if (reservation.isExpired()) {
            reservation.expire();

            reservationRepository.deleteReservationByOrderId(order.getOrderId());
            orderRepository.updateOrder(order);
            eventRepository.updateEvent(event);

            return createReservation(order, event);
        }

        return reservation;
    }

    private Reservation createReservation(ActiveOrder order, Event event) {
        return new Reservation(
                reservationRepository.generateNextId(),
                order,
                event
        );
    }

    private Event getEvent(int eventId) {
        Event event = eventRepository.getEventById(eventId);

        if (event == null) {
            throw new IllegalArgumentException("Event not found");
        }

        return event;
    }

    private ActiveOrder getOrCreateOrder(OrderOwner owner, int eventId) {
        ActiveOrder order = findActiveOrder(owner, eventId);

        if (order == null) {
            order = new ActiveOrder(
                    orderRepository.getNextId(),
                    owner.getUserId(),
                    owner.getSessionToken(),
                    eventId
            );

            orderRepository.updateOrder(order);
        }

        return order;
    }

    private ActiveOrder getExistingOrder(OrderOwner owner, int eventId) {
        ActiveOrder order = findActiveOrder(owner, eventId);

        if (order == null) {
            throw new IllegalArgumentException("Active order not found");
        }

        return order;
    }

    private ActiveOrder findActiveOrder(OrderOwner owner, int eventId) {
        if (owner.isGuest()) {
            return orderRepository.getActiveOrderBySessionTokenAndEventId(
                    owner.getSessionToken(),
                    eventId
            );
        }

        return orderRepository.getActiveOrderByUserIdAndEventId(
                owner.getUserId(),
                eventId
        );
    }

    private void saveAll(Reservation reservation, ActiveOrder order, Event event) {
        reservationRepository.saveReservation(reservation);
        orderRepository.updateOrder(order);
        eventRepository.updateEvent(event);
    }

    private void saveAll(Reservation reservation, ActiveOrder order) {
        reservationRepository.saveReservation(reservation);
        orderRepository.updateOrder(order);
    }

    private OrderOwner getOrderOwnerFromToken(String token) {
        if (!tokenService.validateToken(token)) {
            throw new IllegalArgumentException("Invalid or expired token");
        }

        String subject = tokenService.extractSubject(token);

        if (tokenService.isGuestToken(token)) {
            return new OrderOwner(null, subject);
        }

        return new OrderOwner(Integer.parseInt(subject), token);
    }

    private static class OrderOwner {

        private final Integer userId;
        private final String sessionToken;

        public OrderOwner(Integer userId, String sessionToken) {
            this.userId = userId;
            this.sessionToken = sessionToken;
        }

        public Integer getUserId() {
            return userId;
        }

        public String getSessionToken() {
            return sessionToken;
        }

        public boolean isGuest() {
            return userId == null;
        }
    }
}