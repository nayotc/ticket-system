package ticketsystem.ApplicationLayer;

import ticketsystem.DomainLayer.Reservation;
import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.IRepository.IOrderRepository;
import ticketsystem.DomainLayer.IRepository.IReservationRepository;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.event.Seat;
import ticketsystem.DomainLayer.order.ActiveOrder;

public class ReservationService {

    private final IOrderRepository orderRepository;
    private final IEventRepository eventRepository;
    private final IReservationRepository reservationRepository;
    private final TokenService tokenService;

    public ReservationService(
            IOrderRepository orderRepository,
            IEventRepository eventRepository,
            IReservationRepository reservationRepository,
            TokenService tokenService
    ) {
        this.orderRepository = orderRepository;
        this.eventRepository = eventRepository;
        this.reservationRepository = reservationRepository;
        this.tokenService = tokenService;

    }

    public void reserveSeatTicket(String token, int eventId, int row, int chair) {
        int userId = getUserIdFromToken(token);
        ActiveOrder order = getOrCreateOrder(userId, eventId);
        Event event = getEvent(eventId);        
        Reservation reservation = getOrCreateReservation(order, event);
        reservation.reserveSeatTicket(eventId, row, chair);

        reservationRepository.saveReservation(reservation);
        orderRepository.updateOrder(order);
        eventRepository.updateEvent(event);
    }

    public void reserveStandingTicket(String token, int eventId) {
        int userId = getUserIdFromToken(token);

        ActiveOrder order = getOrCreateOrder(userId, eventId);
        Event event = getEvent(eventId);

        Reservation reservation = getOrCreateReservation(order, event);
        double price = event.getStandingArea().getPrice();

        reservation.reserveStandingTicket(eventId, price);

        reservationRepository.saveReservation(reservation);
        orderRepository.updateOrder(order);
        eventRepository.updateEvent(event);
    }

    public void expireReservation(String token, int eventId) {
        int userId = getUserIdFromToken(token);

        ActiveOrder order = getExistingOrder(userId, eventId);
        Event event = getEvent(eventId);

        Reservation reservation =
                reservationRepository.getReservationByOrderId(order.getOrderId());

        if (reservation == null) {
            return;
        }

        reservation.expire();

        reservationRepository.deleteReservationByOrderId(order.getOrderId());
        orderRepository.updateOrder(order);
        eventRepository.updateEvent(event);
    }

    //get existing reservation and check if it's expired
    private Reservation getExistingReservation(ActiveOrder order, Event event) {
        Reservation reservation =
                reservationRepository.getReservationByOrderId(order.getOrderId());

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


//helper method to extract userId from token and validate it
    private int getUserIdFromToken(String token) {
        if (!tokenService.validateToken(token)) {
            throw new IllegalArgumentException("Invalid or expired token");
        }

        String subject = tokenService.extractSubject(token);

        return Integer.parseInt(subject);
    }

        private Event getEvent(int eventId) {
        Event event = eventRepository.getEventById(eventId);

        if (event == null) {
            throw new IllegalArgumentException("Event not found");
        }

        return event;
    }

    private Reservation getOrCreateReservation(ActiveOrder order, Event event) {
        Reservation reservation =
                reservationRepository.getReservationByOrderId(order.getOrderId());

        if (reservation == null || reservation.isExpired()) {
            reservation = new Reservation(
                    reservationRepository.generateNextId(),order, event);
        }

        return reservation;
    }

        private ActiveOrder getOrCreateOrder(int userId, int eventId) {
        ActiveOrder order =
                orderRepository.getActiveOrderByUserIdAndEventId(userId, eventId);

        if (order == null) {
            int orderId = orderRepository.getNextId();
            order = new ActiveOrder(orderId, userId, eventId);
            orderRepository.updateOrder(order); // upsert
        }

        return order;
    }

    private ActiveOrder getExistingOrder(int userId, int eventId) {
        ActiveOrder order =
                orderRepository.getActiveOrderByUserIdAndEventId(userId, eventId);

        if (order == null) {
            throw new IllegalArgumentException("Active order not found");
        }

        return order;
    }

}

        