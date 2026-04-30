package ticketsystem.UnitTesting;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.ApplicationLayer.ReservationService;
import ticketsystem.ApplicationLayer.TokenService;
import ticketsystem.DomainLayer.Reservation;
import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.IRepository.IOrderRepository;
import ticketsystem.DomainLayer.IRepository.IReservationRepository;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.order.ActiveOrder;

public class ReservationServiceTest {

    private IOrderRepository orderRepoMock;
    private IEventRepository eventRepoMock;
    private IReservationRepository reservationRepoMock;
    private TokenService tokenServiceMock;
    private ReservationService reservationService;

    @BeforeEach
    public void setUp() {
        orderRepoMock = mock(IOrderRepository.class);
        eventRepoMock = mock(IEventRepository.class);
        reservationRepoMock = mock(IReservationRepository.class);
        tokenServiceMock = mock(TokenService.class);

        reservationService = new ReservationService(
                orderRepoMock,
                eventRepoMock,
                reservationRepoMock,
                tokenServiceMock
        );
    }

    @Test
    public void givenValidTokenExistingOrderAndReservation_whenReserveSeatTicket_thenReservationAndRepositoriesShouldBeUpdated() {
        String token = "validToken";
        int userId = 1;
        int eventId = 10;
        int orderId = 100;
        int row = 4;
        int chair = 12;

        ActiveOrder orderMock = mock(ActiveOrder.class);
        Event eventMock = mock(Event.class);
        Reservation reservationMock = mock(Reservation.class);

        when(tokenServiceMock.validateToken(token)).thenReturn(true);
        when(tokenServiceMock.extractSubject(token)).thenReturn(String.valueOf(userId));

        when(orderRepoMock.getActiveOrderByUserIdAndEventId(userId, eventId)).thenReturn(orderMock);
        when(orderMock.getOrderId()).thenReturn(orderId);

        when(eventRepoMock.getEventById(eventId)).thenReturn(eventMock);

        when(reservationRepoMock.getReservationByOrderId(orderId)).thenReturn(reservationMock);
        when(reservationMock.isExpired()).thenReturn(false);

        reservationService.reserveSeatTicket(token, eventId, row, chair);

        verify(reservationMock).reserveSeatTicket(eventId, row, chair);
        verify(reservationRepoMock).saveReservation(reservationMock);
        verify(orderRepoMock).updateOrder(orderMock);
        verify(eventRepoMock).updateEvent(eventMock);
    }

    @Test
    public void givenValidTokenExistingOrderAndReservation_whenReserveStandingTicket_thenReservationAndRepositoriesShouldBeUpdated() {
        String token = "validToken";
        int userId = 1;
        int eventId = 10;
        int orderId = 100;
        double price = 80.0;

        ActiveOrder orderMock = mock(ActiveOrder.class);
        Event eventMock = mock(Event.class, RETURNS_DEEP_STUBS);
        Reservation reservationMock = mock(Reservation.class);

        when(tokenServiceMock.validateToken(token)).thenReturn(true);
        when(tokenServiceMock.extractSubject(token)).thenReturn(String.valueOf(userId));

        when(orderRepoMock.getActiveOrderByUserIdAndEventId(userId, eventId)).thenReturn(orderMock);
        when(orderMock.getOrderId()).thenReturn(orderId);

        when(eventRepoMock.getEventById(eventId)).thenReturn(eventMock);
        when(eventMock.getStandingArea().getPrice()).thenReturn(price);

        when(reservationRepoMock.getReservationByOrderId(orderId)).thenReturn(reservationMock);
        when(reservationMock.isExpired()).thenReturn(false);

        reservationService.reserveStandingTicket(token, eventId);

        verify(reservationMock).reserveStandingTicket(eventId, price);
        verify(reservationRepoMock).saveReservation(reservationMock);
        verify(orderRepoMock).updateOrder(orderMock);
        verify(eventRepoMock).updateEvent(eventMock);
    }

    @Test
    public void givenInvalidToken_whenReserveSeatTicket_thenShouldThrowExceptionAndNotAccessRepositories() {
        String token = "invalidToken";

        when(tokenServiceMock.validateToken(token)).thenReturn(false);

        assertThrows(
                IllegalArgumentException.class,
                () -> reservationService.reserveSeatTicket(token, 10, 4, 12)
        );

        verify(orderRepoMock, never()).getActiveOrderByUserIdAndEventId(1, 10);
        verify(eventRepoMock, never()).getEventById(10);
        verify(reservationRepoMock, never()).saveReservation(mock(Reservation.class));
    }

    @Test
    public void givenGuestToken_whenReserveSeatTicket_thenShouldThrowException() {
        String token = "guestToken";

        when(tokenServiceMock.validateToken(token)).thenReturn(true);
        when(tokenServiceMock.extractSubject(token)).thenReturn("GUEST_123");

        assertThrows(
                IllegalArgumentException.class,
                () -> reservationService.reserveSeatTicket(token, 10, 4, 12)
        );

        verify(eventRepoMock, never()).getEventById(10);
    }

    @Test
    public void givenEventDoesNotExist_whenReserveSeatTicket_thenShouldThrowException() {
        String token = "validToken";
        int userId = 1;
        int eventId = 10;

        ActiveOrder orderMock = mock(ActiveOrder.class);

        when(tokenServiceMock.validateToken(token)).thenReturn(true);
        when(tokenServiceMock.extractSubject(token)).thenReturn(String.valueOf(userId));

        when(orderRepoMock.getActiveOrderByUserIdAndEventId(userId, eventId)).thenReturn(orderMock);
        when(eventRepoMock.getEventById(eventId)).thenReturn(null);

        assertThrows(
                IllegalArgumentException.class,
                () -> reservationService.reserveSeatTicket(token, eventId, 4, 12)
        );

        verify(reservationRepoMock, never()).saveReservation(mock(Reservation.class));
        verify(eventRepoMock, never()).updateEvent(mock(Event.class));
    }

    @Test
    public void givenExistingReservation_whenExpireReservation_thenReservationShouldExpireAndBeDeleted() {
        String token = "validToken";
        int userId = 1;
        int eventId = 10;
        int orderId = 100;

        ActiveOrder orderMock = mock(ActiveOrder.class);
        Event eventMock = mock(Event.class);
        Reservation reservationMock = mock(Reservation.class);

        when(tokenServiceMock.validateToken(token)).thenReturn(true);
        when(tokenServiceMock.extractSubject(token)).thenReturn(String.valueOf(userId));

        when(orderRepoMock.getActiveOrderByUserIdAndEventId(userId, eventId)).thenReturn(orderMock);
        when(orderMock.getOrderId()).thenReturn(orderId);

        when(eventRepoMock.getEventById(eventId)).thenReturn(eventMock);
        when(reservationRepoMock.getReservationByOrderId(orderId)).thenReturn(reservationMock);

        reservationService.expireReservation(token, eventId);

        verify(reservationMock).expire();
        verify(reservationRepoMock).deleteReservationByOrderId(orderId);
        verify(orderRepoMock).updateOrder(orderMock);
        verify(eventRepoMock).updateEvent(eventMock);
    }

    @Test
    public void givenNoReservation_whenExpireReservation_thenShouldReturnWithoutUpdates() {
        String token = "validToken";
        int userId = 1;
        int eventId = 10;
        int orderId = 100;

        ActiveOrder orderMock = mock(ActiveOrder.class);
        Event eventMock = mock(Event.class);

        when(tokenServiceMock.validateToken(token)).thenReturn(true);
        when(tokenServiceMock.extractSubject(token)).thenReturn(String.valueOf(userId));

        when(orderRepoMock.getActiveOrderByUserIdAndEventId(userId, eventId)).thenReturn(orderMock);
        when(orderMock.getOrderId()).thenReturn(orderId);

        when(eventRepoMock.getEventById(eventId)).thenReturn(eventMock);
        when(reservationRepoMock.getReservationByOrderId(orderId)).thenReturn(null);

        reservationService.expireReservation(token, eventId);

        verify(reservationRepoMock, never()).deleteReservationByOrderId(orderId);
        verify(orderRepoMock, never()).updateOrder(orderMock);
        verify(eventRepoMock, never()).updateEvent(eventMock);
    }
}