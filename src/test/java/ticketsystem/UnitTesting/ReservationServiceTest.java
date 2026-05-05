package ticketsystem.UnitTesting;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.ApplicationLayer.IPaymentService;
import ticketsystem.ApplicationLayer.ISecureBarcode;
import ticketsystem.ApplicationLayer.ReservationService;
import ticketsystem.ApplicationLayer.TokenService;
import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.IRepository.IOrderRepository;
import ticketsystem.DomainLayer.IRepository.IReservationRepository;
import ticketsystem.DomainLayer.Reservation;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.order.ActiveOrder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ReservationServiceTest {

    private IOrderRepository orderRepository;
    private IEventRepository eventRepository;
    private IReservationRepository reservationRepository;
    private TokenService tokenService;
    private IPaymentService paymentService;
    private ISecureBarcode secureBarcode;

    private ReservationService service;

    private final String MEMBER_TOKEN = "member-token";
    private final String GUEST_TOKEN = "guest-token";

    @BeforeEach
    void setUp() {
        orderRepository = mock(IOrderRepository.class);
        eventRepository = mock(IEventRepository.class);
        reservationRepository = mock(IReservationRepository.class);
        tokenService = mock(TokenService.class);
        paymentService = mock(IPaymentService.class);
        secureBarcode = mock(ISecureBarcode.class);

        service = new ReservationService(
                orderRepository,
                eventRepository,
                reservationRepository,
                tokenService,
                paymentService,
                secureBarcode
        );
    }

    @Test
    void selectSeatTicket_memberToken_existingOrderAndReservation_success() {
        int eventId = 10;
        int row = 2;
        int chair = 5;
        Long userId = 100L;

        Event event = mock(Event.class);
        ActiveOrder order = mock(ActiveOrder.class);
        Reservation reservation = mock(Reservation.class);

        when(tokenService.validateToken(MEMBER_TOKEN)).thenReturn(true);
        when(tokenService.isGuestToken(MEMBER_TOKEN)).thenReturn(false);
        when(tokenService.extractUserId(MEMBER_TOKEN)).thenReturn(userId);

        when(orderRepository.getActiveOrderByUserIdAndEventId(userId, eventId))
                .thenReturn(order);

        when(eventRepository.getEventById(eventId)).thenReturn(event);
        when(order.getOrderId()).thenReturn(1);
        when(reservationRepository.getReservationByOrderId(1)).thenReturn(reservation);
        when(reservation.isExpired()).thenReturn(false);

        service.selectSeatTicket(MEMBER_TOKEN, eventId, row, chair);

        verify(reservation).selectSeatTicket(eventId, row, chair);
        verify(reservationRepository).saveReservation(reservation);
        verify(orderRepository).updateOrder(order);
        verify(eventRepository).updateEvent(event);
    }

    @Test
    void selectStandingTicket_guestToken_createsNewOrderAndReservation_success() {
        int eventId = 20;
        int quantity = 3;

        Event event = mock(Event.class);

        when(tokenService.validateToken(GUEST_TOKEN)).thenReturn(true);
        when(tokenService.isGuestToken(GUEST_TOKEN)).thenReturn(true);

        when(orderRepository.getActiveOrderBySessionTokenAndEventId(GUEST_TOKEN, eventId))
                .thenReturn(null);

        when(orderRepository.getNextId()).thenReturn(1);
        when(eventRepository.getEventById(eventId)).thenReturn(event);

        when(reservationRepository.getReservationByOrderId(1)).thenReturn(null);
        when(reservationRepository.generateNextId()).thenReturn(100);

        service.selectStandingTicket(GUEST_TOKEN, eventId, quantity);

        verify(orderRepository, atLeastOnce()).updateOrder(any(ActiveOrder.class));
        verify(reservationRepository, atLeastOnce()).saveReservation(any(Reservation.class));
        verify(eventRepository, atLeastOnce()).updateEvent(event);
    }

    @Test
    void selectSeatTicket_invalidToken_throwsException() {
        int eventId = 10;

        when(tokenService.validateToken(MEMBER_TOKEN)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.selectSeatTicket(MEMBER_TOKEN, eventId, 1, 1)
        );

        assertEquals("Invalid or expired token", exception.getMessage());

        verifyNoInteractions(orderRepository);
        verifyNoInteractions(eventRepository);
        verifyNoInteractions(reservationRepository);
    }

    @Test
    void viewActiveOrder_existingOrderAndReservation_returnsOrderString() {
        int eventId = 10;
        Long userId = 100L;

        ActiveOrder order = mock(ActiveOrder.class);
        Reservation reservation = mock(Reservation.class);
        Event event = mock(Event.class);

        when(tokenService.validateToken(MEMBER_TOKEN)).thenReturn(true);
        when(tokenService.isGuestToken(MEMBER_TOKEN)).thenReturn(false);
        when(tokenService.extractUserId(MEMBER_TOKEN)).thenReturn(userId);

        when(orderRepository.getActiveOrderByUserIdAndEventId(userId, eventId))
                .thenReturn(order);

        when(order.getOrderId()).thenReturn(1);
        when(order.getEventId()).thenReturn(eventId);
        when(order.toString()).thenReturn("active order details");

        when(reservationRepository.getReservationByOrderId(1)).thenReturn(reservation);
        when(eventRepository.getEventById(eventId)).thenReturn(event);
        when(reservation.isExpired()).thenReturn(false);

        String result = service.viewActiveOrder(MEMBER_TOKEN, eventId);

        assertEquals("active order details", result);
        verify(reservation).viewActiveOrder(order);
    }

    @Test
    void removeTicketFromActiveOrder_existingOrder_success() {
        int eventId = 10;
        int ticketId = 55;
        Long userId = 100L;

        ActiveOrder order = mock(ActiveOrder.class);
        Reservation reservation = mock(Reservation.class);
        Event event = mock(Event.class);

        when(tokenService.validateToken(MEMBER_TOKEN)).thenReturn(true);
        when(tokenService.isGuestToken(MEMBER_TOKEN)).thenReturn(false);
        when(tokenService.extractUserId(MEMBER_TOKEN)).thenReturn(userId);

        when(orderRepository.getActiveOrderByUserIdAndEventId(userId, eventId))
                .thenReturn(order);

        when(order.getOrderId()).thenReturn(1);
        when(order.getEventId()).thenReturn(eventId);

        when(reservationRepository.getReservationByOrderId(1)).thenReturn(reservation);
        when(eventRepository.getEventById(eventId)).thenReturn(event);
        when(reservation.isExpired()).thenReturn(false);

        service.removeTicketFromActiveOrder(MEMBER_TOKEN, eventId, ticketId);

        verify(reservation).removeTicketFromActiveOrder(order, ticketId);
        verify(reservationRepository).saveReservation(reservation);
        verify(orderRepository).updateOrder(order);
    }

    @Test
    void submitActiveOrderForCheckout_existingOrder_success() {
        int orderId = 1;
        int eventId = 10;
        Long userId = 100L;

        ActiveOrder order = mock(ActiveOrder.class);
        Reservation reservation = mock(Reservation.class);
        Event event = mock(Event.class);

        when(tokenService.validateToken(MEMBER_TOKEN)).thenReturn(true);
        when(tokenService.isGuestToken(MEMBER_TOKEN)).thenReturn(false);
        when(tokenService.extractUserId(MEMBER_TOKEN)).thenReturn(userId);

        when(orderRepository.getActiveOrderByUserIdAndEventId(userId, eventId))
                .thenReturn(order);

        when(order.getOrderId()).thenReturn(orderId);
        when(order.getEventId()).thenReturn(eventId);

        when(reservationRepository.getReservationByOrderId(orderId)).thenReturn(reservation);
        when(eventRepository.getEventById(eventId)).thenReturn(event);
        when(reservation.isExpired()).thenReturn(false);

        service.submitActiveOrderForCheckout(MEMBER_TOKEN, orderId, eventId);

        verify(reservation).submitActiveOrderForCheckout(order);
        verify(reservationRepository).saveReservation(reservation);
        verify(orderRepository).updateOrder(order);
    }

    @Test
    void getExistingReservation_expiredReservation_deletesReservationAndThrows() {
        int eventId = 10;
        Long userId = 100L;

        ActiveOrder order = mock(ActiveOrder.class);
        Reservation reservation = mock(Reservation.class);
        Event event = mock(Event.class);

        when(tokenService.validateToken(MEMBER_TOKEN)).thenReturn(true);
        when(tokenService.isGuestToken(MEMBER_TOKEN)).thenReturn(false);
        when(tokenService.extractUserId(MEMBER_TOKEN)).thenReturn(userId);

        when(orderRepository.getActiveOrderByUserIdAndEventId(userId, eventId))
                .thenReturn(order);

        when(order.getOrderId()).thenReturn(1);
        when(order.getEventId()).thenReturn(eventId);

        when(reservationRepository.getReservationByOrderId(1)).thenReturn(reservation);
        when(eventRepository.getEventById(eventId)).thenReturn(event);
        when(reservation.isExpired()).thenReturn(true);

        assertThrows(
                IllegalStateException.class,
                () -> service.viewActiveOrder(MEMBER_TOKEN, eventId)
        );

        verify(reservation).expire();
        verify(reservationRepository).deleteReservationByOrderId(1);
        verify(orderRepository).updateOrder(order);
        verify(eventRepository).updateEvent(event);
    }

    @Test
    void viewActiveOrder_orderNotFound_throwsException() {
        int eventId = 10;
        Long userId = 100L;

        when(tokenService.validateToken(MEMBER_TOKEN)).thenReturn(true);
        when(tokenService.isGuestToken(MEMBER_TOKEN)).thenReturn(false);
        when(tokenService.extractUserId(MEMBER_TOKEN)).thenReturn(userId);

        when(orderRepository.getActiveOrderByUserIdAndEventId(userId, eventId))
                .thenReturn(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.viewActiveOrder(MEMBER_TOKEN, eventId)
        );

        assertEquals("Active order not found", exception.getMessage());
    }

    @Test
    void selectSeatTicket_eventNotFound_throwsException() {
        int eventId = 10;
        Long userId = 100L;

        ActiveOrder order = mock(ActiveOrder.class);

        when(tokenService.validateToken(MEMBER_TOKEN)).thenReturn(true);
        when(tokenService.isGuestToken(MEMBER_TOKEN)).thenReturn(false);
        when(tokenService.extractUserId(MEMBER_TOKEN)).thenReturn(userId);

        when(orderRepository.getActiveOrderByUserIdAndEventId(userId, eventId))
                .thenReturn(order);

        when(eventRepository.getEventById(eventId)).thenReturn(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.selectSeatTicket(MEMBER_TOKEN, eventId, 1, 1)
        );

        assertEquals("Event not found", exception.getMessage());
    }
}