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

    public void selectStandingTicket(String token, int eventId) {
    try {
        OrderOwner owner = getOrderOwnerFromToken(token);

        ActiveOrder order = getOrCreateOrder(owner, eventId);
        Event event = getEvent(eventId);
        Reservation reservation = getOrCreateReservation(order, event);

        double price = event.getStandingArea().getPrice();
        reservation.selectStandingTicket(eventId, price);

        saveAll(reservation, order, event);

    } catch (Exception e) {
        System.err.println("Error in selectStandingTicket: " + e.getMessage());
        throw e;
        }
    }

    public void expireReservation(String token, int eventId) {
        try {
            OrderOwner owner = getOrderOwnerFromToken(token);

            ActiveOrder order = getExistingOrder(owner, eventId);
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

        } catch (Exception e) {
            System.err.println("Error in expireReservation: " + e.getMessage());
            throw e;
        }
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
                if (reservation == null) {
                    reservation = new Reservation(
                            reservationRepository.generateNextId(),
                            order,
                            event
                    );
                }
                return reservation;
    }

    private void saveAll(Reservation reservation, ActiveOrder order, Event event) {
        reservationRepository.saveReservation(reservation);
        orderRepository.updateOrder(order);
        eventRepository.updateEvent(event);
    }


    private ActiveOrder getOrCreateOrder(OrderOwner owner, int eventId) {
    ActiveOrder order;

        if (owner.isGuest()) {
            order = orderRepository.getActiveOrderBySessionTokenAndEventId(
                    owner.getSessionToken(),
                    eventId
            );
        } else {
            order = orderRepository.getActiveOrderByUserIdAndEventId(
                    owner.getUserId(),
                    eventId
            );
        }

        if (order == null) {
            int orderId = orderRepository.getNextId();

            order = new ActiveOrder(
                    orderId,
                    owner.getUserId(),
                    owner.getSessionToken(),
                    eventId
            );

            orderRepository.updateOrder(order);
        }

        return order;
    }

    private ActiveOrder getExistingOrder(OrderOwner owner, int eventId) {
        ActiveOrder order;

        if (owner.isGuest()) {
            order = orderRepository.getActiveOrderBySessionTokenAndEventId(
                    owner.getSessionToken(),
                    eventId
            );
        } else {
            order = orderRepository.getActiveOrderByUserIdAndEventId(
                    owner.getUserId(),
                    eventId
            );
        }

        if (order == null) {
            throw new IllegalArgumentException("Active order not found");
        }

        return order;
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

        