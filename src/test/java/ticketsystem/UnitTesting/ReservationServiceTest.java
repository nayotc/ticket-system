package ticketsystem.UnitTesting;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.IRepository.IOrderRepository;
import ticketsystem.DomainLayer.IRepository.IReservationRepository;
import ticketsystem.ApplicationLayer.ReservationService;
import ticketsystem.ApplicationLayer.TokenService;
import ticketsystem.DomainLayer.Reservation;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.order.ActiveOrder;

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

    @Test
    void shouldSelectSeat_ForRegisteredUser() {
        String token = "token";

        when(tokenService.validateToken(token)).thenReturn(true);
        when(tokenService.isGuestToken(token)).thenReturn(false);
        when(tokenService.extractSubject(token)).thenReturn("1");

        ActiveOrder order = mock(ActiveOrder.class);
        Event event = mock(Event.class);
        Reservation reservation = mock(Reservation.class);

        when(orderRepository.getActiveOrderByUserIdAndEventId(1, 10)).thenReturn(order);
        when(eventRepository.getEventById(10)).thenReturn(event);
        when(reservationRepository.getReservationByOrderId(any())).thenReturn(reservation);

        service.selectSeatTicket(token, 10, 5, 7);

        verify(reservation).selectSeatTicket(10, 5, 7);
        verify(reservationRepository).saveReservation(reservation);
        verify(orderRepository).updateOrder(order);
        verify(eventRepository).updateEvent(event);
    }

    @Test
    void shouldSelectSeat_ForGuest() {
        String token = "guest";

        when(tokenService.validateToken(token)).thenReturn(true);
        when(tokenService.isGuestToken(token)).thenReturn(true);
        when(tokenService.extractSubject(token)).thenReturn("GUEST_123");

        ActiveOrder order = mock(ActiveOrder.class);
        Event event = mock(Event.class);
        Reservation reservation = mock(Reservation.class);

        when(orderRepository.getActiveOrderBySessionTokenAndEventId("GUEST_123", 10))
                .thenReturn(order);
        when(eventRepository.getEventById(10)).thenReturn(event);
        when(reservationRepository.getReservationByOrderId(any())).thenReturn(reservation);

        service.selectSeatTicket(token, 10, 1, 1);

        verify(reservation).selectSeatTicket(10, 1, 1);
    }

    @Test
    void shouldCreateReservation_IfNotExists() {
        String token = "token";

        when(tokenService.validateToken(token)).thenReturn(true);
        when(tokenService.isGuestToken(token)).thenReturn(false);
        when(tokenService.extractSubject(token)).thenReturn("1");

        ActiveOrder order = mock(ActiveOrder.class);
        Event event = mock(Event.class);

        when(orderRepository.getActiveOrderByUserIdAndEventId(1, 10)).thenReturn(order);
        when(eventRepository.getEventById(10)).thenReturn(event);
        when(reservationRepository.getReservationByOrderId(any())).thenReturn(null);
        when(reservationRepository.generateNextId()).thenReturn(99);

        service.selectSeatTicket(token, 10, 2, 3);

        verify(reservationRepository).saveReservation(any(Reservation.class));
    }

    @Test
    void shouldThrowException_WhenEventNotFound() {
        String token = "token";

        when(tokenService.validateToken(token)).thenReturn(true);
        when(tokenService.isGuestToken(token)).thenReturn(false);
        when(tokenService.extractSubject(token)).thenReturn("1");

        when(orderRepository.getActiveOrderByUserIdAndEventId(1, 10))
                .thenReturn(mock(ActiveOrder.class));
        when(eventRepository.getEventById(10)).thenReturn(null);

        try {
            service.selectSeatTicket(token, 10, 1, 1);
        } catch (Exception e) {
            assert e instanceof IllegalArgumentException;
        }
    }
}