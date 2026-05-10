package ticketsystem.ApplicationLayer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import ticketsystem.ApplicationLayer.ReservationService;
import ticketsystem.ApplicationLayer.TokenService;
import ticketsystem.ApplicationLayer.IPaymentService;
import ticketsystem.DTO.OrderDTO;
import ticketsystem.DTO.seatPositionDTO;
import ticketsystem.DomainLayer.Reservation;
import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.IRepository.IOrderRepository;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.order.ActiveOrder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class ReservationServiceTest {

    private IOrderRepository orderRepository;
    private IEventRepository eventRepository;
    private TokenService tokenService;
    private Reservation reservation;

    private ReservationService service;

    private final String token = "valid-token";
    private final int eventId = 1;

    private ActiveOrder order;
    private Event event;

    @BeforeEach
    void setUp() throws Exception {
        orderRepository = mock(IOrderRepository.class);
        eventRepository = mock(IEventRepository.class);
        tokenService = mock(TokenService.class);
        reservation = mock(Reservation.class);

        order = mock(ActiveOrder.class);
        event = mock(Event.class);

        service = new ReservationService(
                orderRepository,
                eventRepository,
                tokenService
                
        );

        injectReservationMock();

        when(tokenService.validateToken(token)).thenReturn(true);
        when(tokenService.isGuestToken(token)).thenReturn(false);
        when(tokenService.isMemberToken(token)).thenReturn(true);
        when(tokenService.extractUserId(token)).thenReturn(7L);

        when(orderRepository.getActiveOrderByUserIdAndEventId(7L, eventId))
                .thenReturn(order);

        when(eventRepository.getEventById(eventId)).thenReturn(event);
    }

    private void injectReservationMock() throws Exception {
        Field field = ReservationService.class.getDeclaredField("reservation");
        field.setAccessible(true);
        field.set(service, reservation);
    }

    @Test
    void selectSeatTicket_success_updatesOrderAndEvent() {
        seatPositionDTO position = new seatPositionDTO(2, 5);
        service.selectSeatTicket(token, eventId, 2L, position);

        verify(reservation).selectSeatTicket(order, event, 2L, position);
        verify(orderRepository).updateOrder(order);
        verify(eventRepository).updateEvent(event);
    }

    @Test
    void selectStandingTicket_success_updatesOrderAndEvent() {
        service.selectStandingTicket(token, eventId, 3L, 2);

        verify(reservation).selectStandingTicket(order, event, 3L, 2);
        verify(orderRepository).updateOrder(order);
        verify(eventRepository).updateEvent(event);
    }
}