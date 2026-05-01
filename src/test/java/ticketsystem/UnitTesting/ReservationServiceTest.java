package ticketsystem.UnitTesting;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    private ReservationService service;

    @BeforeEach
    void setUp() {
        orderRepository = mock(IOrderRepository.class);
        eventRepository = mock(IEventRepository.class);
        reservationRepository = mock(IReservationRepository.class);
        tokenService = mock(TokenService.class);

        service = new ReservationService(
                orderRepository,
                eventRepository,
                reservationRepository,
                tokenService
        );
    }

    private void mockRegisteredUser(String token, int userId) {
        when(tokenService.validateToken(token)).thenReturn(true);
        when(tokenService.isGuestToken(token)).thenReturn(false);
        when(tokenService.extractSubject(token)).thenReturn(String.valueOf(userId));
    }

    private void mockGuestUser(String token, String sessionToken) {
        when(tokenService.validateToken(token)).thenReturn(true);
        when(tokenService.isGuestToken(token)).thenReturn(true);
        when(tokenService.extractSubject(token)).thenReturn(sessionToken);
    }

    @Test
    void shouldSelectSeat_ForRegisteredUser() {
        String token = "token";
        mockRegisteredUser(token, 1);

        ActiveOrder order = mock(ActiveOrder.class);
        Event event = mock(Event.class);
        Reservation reservation = mock(Reservation.class);

        when(order.getOrderId()).thenReturn(100);
        when(orderRepository.getActiveOrderByUserIdAndEventId(1, 10)).thenReturn(order);
        when(eventRepository.getEventById(10)).thenReturn(event);
        when(reservationRepository.getReservationByOrderId(100)).thenReturn(reservation);

        service.selectSeatTicket(token, 10, 5, 7);

        verify(reservation).selectSeatTicket(10, 5, 7);
        verify(reservationRepository).saveReservation(reservation);
        verify(orderRepository).updateOrder(order);
        verify(eventRepository).updateEvent(event);
    }

    @Test
    void shouldSelectSeat_ForGuest() {
        String token = "guest";
        mockGuestUser(token, "GUEST_123");

        ActiveOrder order = mock(ActiveOrder.class);
        Event event = mock(Event.class);
        Reservation reservation = mock(Reservation.class);

        when(order.getOrderId()).thenReturn(100);
        when(orderRepository.getActiveOrderBySessionTokenAndEventId("GUEST_123", 10)).thenReturn(order);
        when(eventRepository.getEventById(10)).thenReturn(event);
        when(reservationRepository.getReservationByOrderId(100)).thenReturn(reservation);

        service.selectSeatTicket(token, 10, 1, 1);

        verify(reservation).selectSeatTicket(10, 1, 1);
        verify(reservationRepository).saveReservation(reservation);
    }

    @Test
    void shouldSelectStandingTicket_ForRegisteredUser() {
        String token = "token";
        mockRegisteredUser(token, 1);

        ActiveOrder order = mock(ActiveOrder.class);
        Event event = mock(Event.class, RETURNS_DEEP_STUBS);
        Reservation reservation = mock(Reservation.class);

        when(order.getOrderId()).thenReturn(100);
        when(orderRepository.getActiveOrderByUserIdAndEventId(1, 10)).thenReturn(order);
        when(eventRepository.getEventById(10)).thenReturn(event);
        when(event.getStandingArea().getPrice()).thenReturn(80.0);
        when(reservationRepository.getReservationByOrderId(100)).thenReturn(reservation);

        service.selectStandingTicket(token, 10, 3);

        verify(reservation).selectStandingTicket(10, 80.0, 3);
        verify(reservationRepository).saveReservation(reservation);
        verify(orderRepository).updateOrder(order);
        verify(eventRepository).updateEvent(event);
    }

    @Test
    void shouldSelectStandingTicket_CreateOrderIfNotExists() {
        String token = "token";
        mockRegisteredUser(token, 1);

        Event event = mock(Event.class, RETURNS_DEEP_STUBS);

        when(orderRepository.getActiveOrderByUserIdAndEventId(1, 10)).thenReturn(null);
        when(orderRepository.getNextId()).thenReturn(55);
        when(eventRepository.getEventById(10)).thenReturn(event);
        when(event.getStandingArea().getPrice()).thenReturn(50.0);
        when(reservationRepository.getReservationByOrderId(55)).thenReturn(null);
        when(reservationRepository.generateNextId()).thenReturn(99);

        service.selectStandingTicket(token, 10, 2);

        verify(orderRepository, atLeastOnce()).updateOrder(any(ActiveOrder.class));
        verify(reservationRepository).saveReservation(any(Reservation.class));
        verify(eventRepository).updateEvent(event);
    }

    @Test
    void shouldViewActiveOrder() {
        String token = "token";
        mockRegisteredUser(token, 1);

        ActiveOrder order = mock(ActiveOrder.class);
        Reservation reservation = mock(Reservation.class);
        Event event = mock(Event.class);

        when(order.getOrderId()).thenReturn(100);
        when(order.getEventId()).thenReturn(10);
        when(order.toString()).thenReturn("order details");

        when(orderRepository.getActiveOrderByUserIdAndEventId(1, 10)).thenReturn(order);
        when(reservationRepository.getReservationByOrderId(100)).thenReturn(reservation);
        when(eventRepository.getEventById(10)).thenReturn(event);

        String result = service.viewActiveOrder(token, 10);

        assertEquals("order details", result);
        verify(reservation).viewActiveOrder(order);
    }

    @Test
    void shouldThrow_WhenViewingOrderThatDoesNotExist() {
        String token = "token";
        mockRegisteredUser(token, 1);

        when(orderRepository.getActiveOrderByUserIdAndEventId(1, 10)).thenReturn(null);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.viewActiveOrder(token, 10)
        );
    }

    @Test
    void shouldRemoveTicketFromActiveOrder() {
        String token = "token";
        mockRegisteredUser(token, 1);

        ActiveOrder order = mock(ActiveOrder.class);
        Reservation reservation = mock(Reservation.class);
        Event event = mock(Event.class);

        when(order.getOrderId()).thenReturn(100);
        when(order.getEventId()).thenReturn(10);

        when(orderRepository.getActiveOrderByUserIdAndEventId(1, 10)).thenReturn(order);
        when(reservationRepository.getReservationByOrderId(100)).thenReturn(reservation);
        when(eventRepository.getEventById(10)).thenReturn(event);

        service.removeTicketFromActiveOrder(token, 10, 777);

        verify(reservation).removeTicketFromActiveOrder(order, 777);
        verify(reservationRepository).saveReservation(reservation);
        verify(orderRepository).updateOrder(order);
        verify(eventRepository, never()).updateEvent(event);
    }

    @Test
    void shouldThrow_WhenRemovingTicketFromExpiredReservation() {
        String token = "token";
        mockRegisteredUser(token, 1);

        ActiveOrder order = mock(ActiveOrder.class);
        Reservation reservation = mock(Reservation.class);
        Event event = mock(Event.class);

        when(order.getOrderId()).thenReturn(100);
        when(order.getEventId()).thenReturn(10);

        when(orderRepository.getActiveOrderByUserIdAndEventId(1, 10)).thenReturn(order);
        when(reservationRepository.getReservationByOrderId(100)).thenReturn(reservation);
        when(eventRepository.getEventById(10)).thenReturn(event);
        when(reservation.isExpired()).thenReturn(true);

        assertThrows(
                IllegalStateException.class,
                () -> service.removeTicketFromActiveOrder(token, 10, 777)
        );

        verify(reservation).expire();
        verify(reservationRepository).deleteReservationByOrderId(100);
        verify(orderRepository).updateOrder(order);
        verify(eventRepository).updateEvent(event);
    }

    @Test
    void shouldThrow_WhenTokenInvalid() {
        String token = "bad-token";

        when(tokenService.validateToken(token)).thenReturn(false);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.selectSeatTicket(token, 10, 1, 1)
        );

        verifyNoInteractions(orderRepository);
        verifyNoInteractions(eventRepository);
        verifyNoInteractions(reservationRepository);
    }

    @Test
    void shouldThrow_WhenEventNotFound() {
        String token = "token";
        mockRegisteredUser(token, 1);

        ActiveOrder order = mock(ActiveOrder.class);

        when(order.getOrderId()).thenReturn(100);
        when(orderRepository.getActiveOrderByUserIdAndEventId(1, 10)).thenReturn(order);
        when(eventRepository.getEventById(10)).thenReturn(null);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.selectSeatTicket(token, 10, 1, 1)
        );

        verify(reservationRepository, never()).saveReservation(any());
    }
}