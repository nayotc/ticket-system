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

    }