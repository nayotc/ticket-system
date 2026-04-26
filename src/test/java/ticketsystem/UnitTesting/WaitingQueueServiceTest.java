package ticketsystem.UnitTesting;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ticketsystem.ApplicationLayer.NotificationsService;
import ticketsystem.ApplicationLayer.WaitingQueueService;
import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.IRepository.IWaitingQueueRepository;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.ApplicationLayer.TokenService;

public class WaitingQueueServiceTest {

    private IEventRepository eventRepoMock;
    private IWaitingQueueRepository queueRepoMock;
    private NotificationsService notificationsMock;
    private WaitingQueueService waitingQueueService;
    private TokenService tokenServiceMock;

    @BeforeEach
    public void setUp() {
        eventRepoMock = mock(IEventRepository.class);
        queueRepoMock = mock(IWaitingQueueRepository.class);
        notificationsMock = mock(NotificationsService.class);
        tokenServiceMock = mock(TokenService.class);

        waitingQueueService = new WaitingQueueService(eventRepoMock, queueRepoMock, notificationsMock, tokenServiceMock);
    }

    @Test
    public void givenEventHasCapacity_whenTryReserve_thenUserIsApproved() {
        //arrange
        Event event = new Event(1L, "Music Festival", 100L);
        when(eventRepoMock.getEventById(1)).thenReturn(event);
        when(tokenServiceMock.validate("session-123")).thenReturn(true);

        //act
        String result = waitingQueueService.tryReserve(1, "session-123");

        //assert
        assertEquals("APPROVED", result, "User should be approved instantly.");
        assertEquals(1, event.getActiveReservationsCount(), "Active reservations should be 1.");
        verify(queueRepoMock, never()).enqueueUser(anyInt(), anyString());
    }

    @Test
    public void givenEventIsFull_whenTryReserve_thenUserIsQueued() {
        //arrange
        Event event = new Event(2L, "Art Expo", 1L);
        event.incrementActiveReservations();
        when(eventRepoMock.getEventById(2)).thenReturn(event);
        when(queueRepoMock.getQueueSize(2)).thenReturn(1);
        when(tokenServiceMock.validate("session-456")).thenReturn(true);

        //act
        String result = waitingQueueService.tryReserve(2, "session-456");

        //assert
        assertEquals("QUEUED", result, "User should be queued because event is full.");
        verify(queueRepoMock, times(1)).enqueueUser(2, "session-456");
        assertEquals(1, event.getActiveReservationsCount(), "Active reservations should not increase.");
    }

    @Test
    public void givenUserInQueue_whenSpotReleased_thenNextUserIsProcessedAndNotified() {
        //arrange
        Event event = new Event(3L, "Rock Concert", 2L);
        event.incrementActiveReservations();
        event.incrementActiveReservations();
        when(eventRepoMock.getEventById(3)).thenReturn(event);
        when(queueRepoMock.dequeueBatch(3, 1)).thenReturn(Arrays.asList("session-789"));
        when(tokenServiceMock.validate("session-111")).thenReturn(true);

        //act
        waitingQueueService.releaseSpot(3, "session-111");

        //assert
        verify(notificationsMock, times(1)).notifyUser(eq("session-789"), anyString());
        assertEquals(2, event.getActiveReservationsCount(), "Capacity should be full again after pulling from queue.");
    }

    @Test
    public void givenEmptyQueue_whenSpotReleased_thenCapacityDrops() {
        //arrange
        Event event = new Event(4L, "Jazz Night", 2L);
        event.incrementActiveReservations();
        event.incrementActiveReservations();
        when(eventRepoMock.getEventById(4)).thenReturn(event);
        when(queueRepoMock.dequeueBatch(4, 1)).thenReturn(Collections.emptyList());
        when(tokenServiceMock.validate("session-222")).thenReturn(true);

        // act
        waitingQueueService.releaseSpot(4, "session-222");

        // assert
        assertEquals(1, event.getActiveReservationsCount(), "Capacity should drop to 1.");
        verify(notificationsMock, never()).notifyUser(anyString(), anyString());
    }
}
