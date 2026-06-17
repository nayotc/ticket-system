package ticketsystem.IntegrationTesting;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.ApplicationLayer.*;
import ticketsystem.DomainLayer.MembershipDomainService;
import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.IRepository.IHistoryRepository;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.event.Event.eventStatus;
import ticketsystem.DomainLayer.event.EventCategory;
import ticketsystem.DomainLayer.event.EventLocation;
import ticketsystem.DomainLayer.event.Pair;
import ticketsystem.DomainLayer.event.SaleStatus;
import ticketsystem.DomainLayer.history.Purchase;
import ticketsystem.DomainLayer.history.PurchasedTicket;
import ticketsystem.DomainLayer.history.TicketStatus;
import ticketsystem.DomainLayer.user.Permission;
import ticketsystem.DTO.SalesReportDTO;

class EventHistoryIntegrationTests {

    private static final String TOKEN = "token";
    private static final Long USER_ID = 1L;
    private static final Long COMPANY_ID = 10L;
    private static final Long EVENT_ID = 100L;

    private IEventRepository eventRepository;
    private IHistoryRepository historyRepository;
    private ITokenService tokenService;
    private MembershipDomainService membershipDomainService;
    private ISystemLogger logger;
    private UserAccessService userAccessService;
    private INotifier notifier;
    private IPaymentService paymentService;
    private ITicketIssuingService ticketIssuingService;

    private EventService eventService;
    private HistoryService historyService;

    private Event event;

    @BeforeEach
    void setUp() {
        eventRepository = mock(IEventRepository.class);
        historyRepository = mock(IHistoryRepository.class);
        tokenService = mock(ITokenService.class);
        membershipDomainService = mock(MembershipDomainService.class);
        logger = mock(ISystemLogger.class);
        userAccessService = mock(UserAccessService.class);
        notifier = mock(INotifier.class);
        paymentService = mock(IPaymentService.class);
        ticketIssuingService = mock(ITicketIssuingService.class);

        eventService = new EventService(
                eventRepository,
                tokenService,
                membershipDomainService,
                logger,
                userAccessService
        );

        historyService = new HistoryService(
                historyRepository,
                tokenService,
                membershipDomainService,
                logger,
                userAccessService,
                notifier,
                paymentService,
                ticketIssuingService
        );

        eventService.addEventUpdatesListener(historyService);

        event = createActiveEvent();

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USER_ID);
        when(tokenService.isMemberToken(TOKEN)).thenReturn(true);

        when(eventRepository.getEventById(EVENT_ID)).thenReturn(event);

        when(membershipDomainService.validatePermission(
                USER_ID,
                COMPANY_ID,
                Permission.MANAGE_EVENT_INVENTORY
        )).thenReturn(true);

        when(membershipDomainService.validatePermission(
                USER_ID,
                COMPANY_ID,
                Permission.GENERATE_SALES_REPORT
        )).thenReturn(true);

        when(membershipDomainService.getManagementSubTreeMemberIds(USER_ID, COMPANY_ID))
                .thenReturn(Set.of(USER_ID));
    }

    @Test
    void GivenNoPurchases_WhenCancelEvent_ThenEventCanceledWithoutRefunds() {
        when(historyRepository.getPurchasesByEventId(EVENT_ID)).thenReturn(List.of());
        when(paymentService.handshake()).thenReturn(true);
        when(ticketIssuingService.handshake()).thenReturn(true);

        Boolean result = eventService.cancelEvent(TOKEN, EVENT_ID);

        assertTrue(result);
        assertEquals(eventStatus.CANCELLED, event.getStatus());
        assertEquals(SaleStatus.ENDED, event.getSaleStatus());

        verify(paymentService, never()).refund(any());
        verify(eventRepository, times(2)).updateEvent(event);
        verify(notifier, never()).notifyMembers(anyList(), anyString());
    }

    @Test
    void GivenOnePurchase_WhenCancelEventSucceeds_ThenRefundPurchaseCancelTicketsAndNotifyBuyer() {
        PurchasedTicket ticket = mock(PurchasedTicket.class);
        Purchase purchase = purchase(1L, 55L, 12345, List.of(ticket));

        when(historyRepository.getPurchasesByEventId(EVENT_ID)).thenReturn(List.of(purchase));
        when(paymentService.handshake()).thenReturn(true);
        when(ticketIssuingService.handshake()).thenReturn(true);
        when(paymentService.refund(12345)).thenReturn(true);

        Boolean result = eventService.cancelEvent(TOKEN, EVENT_ID);

        assertTrue(result);
        assertEquals(eventStatus.CANCELLED, event.getStatus());
        assertEquals(SaleStatus.ENDED, event.getSaleStatus());
        assertTrue(purchase.isRefunded());

        verify(paymentService).refund(12345);
        verify(ticket).setStatus(TicketStatus.CANCELED);
        verify(eventRepository, times(2)).updateEvent(event);
        verify(notifier, atLeastOnce()).notifyMembers(anyList(), anyString());
    }

    @Test
    void GivenMultiplePurchases_WhenCancelEventSucceeds_ThenRefundAllPurchasesAndCancelAllTickets() {
        PurchasedTicket ticket1 = mock(PurchasedTicket.class);
        PurchasedTicket ticket2 = mock(PurchasedTicket.class);

        Purchase purchase1 = purchase(1L, 55L, 11111, List.of(ticket1));
        Purchase purchase2 = purchase(2L, 66L, 22222, List.of(ticket2));

        when(historyRepository.getPurchasesByEventId(EVENT_ID)).thenReturn(List.of(purchase1, purchase2));
        when(paymentService.handshake()).thenReturn(true);
        when(ticketIssuingService.handshake()).thenReturn(true);
        when(paymentService.refund(11111)).thenReturn(true);
        when(paymentService.refund(22222)).thenReturn(true);

        Boolean result = eventService.cancelEvent(TOKEN, EVENT_ID);

        assertTrue(result);
        assertEquals(eventStatus.CANCELLED, event.getStatus());
        assertTrue(purchase1.isRefunded());
        assertTrue(purchase2.isRefunded());

        verify(paymentService).refund(11111);
        verify(paymentService).refund(22222);
        verify(ticket1).setStatus(TicketStatus.CANCELED);
        verify(ticket2).setStatus(TicketStatus.CANCELED);
    }

    @Test
    void GivenPaymentHandshakeFails_WhenCancelEvent_ThenCancellationFailedAndNoRefund() {
        Purchase purchase = purchase(1L, 55L, 12345, List.of(mock(PurchasedTicket.class)));

        when(historyRepository.getPurchasesByEventId(EVENT_ID)).thenReturn(List.of(purchase));
        when(paymentService.handshake()).thenReturn(false);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.cancelEvent(TOKEN, EVENT_ID)
        );

        assertEquals("Event cancellation failed. Please try again later to complete the cancellation process.", ex.getMessage());
        assertEquals(eventStatus.CANCELLATION_FAILED, event.getStatus());
        assertEquals(SaleStatus.ENDED, event.getSaleStatus());
        assertFalse(purchase.isRefunded());

        verify(paymentService, never()).refund(any());
        verify(ticketIssuingService, never()).handshake();
        verify(eventRepository, times(2)).updateEvent(event);
    }

    @Test
    void GivenTicketIssuingHandshakeFails_WhenCancelEvent_ThenCancellationFailedAndNoRefund() {
        Purchase purchase = purchase(1L, 55L, 12345, List.of(mock(PurchasedTicket.class)));

        when(historyRepository.getPurchasesByEventId(EVENT_ID)).thenReturn(List.of(purchase));
        when(paymentService.handshake()).thenReturn(true);
        when(ticketIssuingService.handshake()).thenReturn(false);

        assertThrows(
                IllegalArgumentException.class,
                () -> eventService.cancelEvent(TOKEN, EVENT_ID)
        );

        assertEquals(eventStatus.CANCELLATION_FAILED, event.getStatus());
        assertEquals(SaleStatus.ENDED, event.getSaleStatus());
        assertFalse(purchase.isRefunded());

        verify(paymentService, never()).refund(any());
        verify(eventRepository, times(2)).updateEvent(event);
    }

    @Test
    void GivenRefundFails_WhenCancelEvent_ThenCancellationFailedAndTicketNotCanceled() {
        PurchasedTicket ticket = mock(PurchasedTicket.class);
        Purchase purchase = purchase(1L, 55L, 12345, List.of(ticket));

        when(historyRepository.getPurchasesByEventId(EVENT_ID)).thenReturn(List.of(purchase));
        when(paymentService.handshake()).thenReturn(true);
        when(ticketIssuingService.handshake()).thenReturn(true);
        when(paymentService.refund(12345)).thenReturn(false);

        assertThrows(
                IllegalArgumentException.class,
                () -> eventService.cancelEvent(TOKEN, EVENT_ID)
        );

        assertEquals(eventStatus.CANCELLATION_FAILED, event.getStatus());
        assertEquals(SaleStatus.ENDED, event.getSaleStatus());
        assertFalse(purchase.isRefunded());

        verify(paymentService).refund(12345);
        verify(ticket, never()).setStatus(TicketStatus.CANCELED);
        verify(eventRepository, times(2)).updateEvent(event);
    }

    @Test
    void GivenOneRefundSucceedsAndOneRefundFails_WhenCancelEvent_ThenFailedStatusButSuccessfulPurchaseStaysRefunded() {
        PurchasedTicket successfulTicket = mock(PurchasedTicket.class);
        PurchasedTicket failedTicket = mock(PurchasedTicket.class);

        Purchase successfulPurchase = purchase(1L, 55L, 11111, List.of(successfulTicket));
        Purchase failedPurchase = purchase(2L, 66L, 22222, List.of(failedTicket));

        when(historyRepository.getPurchasesByEventId(EVENT_ID)).thenReturn(List.of(successfulPurchase, failedPurchase));
        when(paymentService.handshake()).thenReturn(true);
        when(ticketIssuingService.handshake()).thenReturn(true);
        when(paymentService.refund(11111)).thenReturn(true);
        when(paymentService.refund(22222)).thenReturn(false);

        assertThrows(
                IllegalArgumentException.class,
                () -> eventService.cancelEvent(TOKEN, EVENT_ID)
        );

        assertEquals(eventStatus.CANCELLATION_FAILED, event.getStatus());

        assertTrue(successfulPurchase.isRefunded());
        assertFalse(failedPurchase.isRefunded());

        verify(successfulTicket).setStatus(TicketStatus.CANCELED);
        verify(failedTicket, never()).setStatus(TicketStatus.CANCELED);
    }

    @Test
    void GivenAlreadyRefundedPurchase_WhenCancelEvent_ThenRefundIsNotCalledAgain() {
        PurchasedTicket ticket = mock(PurchasedTicket.class);
        Purchase purchase = purchase(1L, 55L, 12345, List.of(ticket));
        purchase.setRefunded(true);

        when(historyRepository.getPurchasesByEventId(EVENT_ID)).thenReturn(List.of(purchase));
        when(paymentService.handshake()).thenReturn(true);
        when(ticketIssuingService.handshake()).thenReturn(true);

        Boolean result = eventService.cancelEvent(TOKEN, EVENT_ID);

        assertTrue(result);
        assertEquals(eventStatus.CANCELLED, event.getStatus());

        verify(paymentService, never()).refund(any());
        verify(ticket, never()).setStatus(TicketStatus.CANCELED);
    }

    @Test
    void GivenInvalidToken_WhenCancelEvent_ThenFailBeforeRepositoryAccess() {
        when(tokenService.validateToken(TOKEN)).thenReturn(false);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.cancelEvent(TOKEN, EVENT_ID)
        );

        assertEquals("Invalid session ID", ex.getMessage());

        verify(eventRepository, never()).getEventById(any());
        verify(eventRepository, never()).updateEvent(any());
        verify(paymentService, never()).refund(any());
    }

    @Test
    void GivenEventDoesNotExist_WhenCancelEvent_ThenFail() {
        when(eventRepository.getEventById(EVENT_ID)).thenReturn(null);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.cancelEvent(TOKEN, EVENT_ID)
        );

        assertEquals("Event does not exist", ex.getMessage());

        verify(eventRepository, never()).updateEvent(any());
        verify(paymentService, never()).refund(any());
    }

    @Test
    void GivenUserWithoutPermission_WhenCancelEvent_ThenFail() {
        when(membershipDomainService.validatePermission(
                USER_ID,
                COMPANY_ID,
                Permission.MANAGE_EVENT_INVENTORY
        )).thenReturn(false);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.cancelEvent(TOKEN, EVENT_ID)
        );

        assertEquals("User does not have permission to cancel an event", ex.getMessage());

        verify(eventRepository, never()).updateEvent(any());
        verify(paymentService, never()).refund(any());
    }

    @Test
    void GivenSuspendedUser_WhenCancelEvent_ThenFailBeforeCancellationPending() {
        doThrow(new IllegalArgumentException("User is suspended"))
                .when(userAccessService)
                .validateCanPerformNonViewAction(USER_ID);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.cancelEvent(TOKEN, EVENT_ID)
        );

        assertEquals("User is suspended", ex.getMessage());

        assertEquals(eventStatus.ACTIVE, event.getStatus());
        verify(eventRepository, never()).updateEvent(any());
        verify(paymentService, never()).refund(any());
    }

    @Test
    void GivenEventAlreadyCanceled_WhenCancelEvent_ThenFail() {
        event.setStatus(eventStatus.CANCELLED);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> eventService.cancelEvent(TOKEN, EVENT_ID)
        );

        assertEquals("Event is already canceled", ex.getMessage());

        verify(eventRepository, never()).updateEvent(any());
        verify(paymentService, never()).refund(any());
    }

    @Test
    void GivenCanceledTickets_WhenGenerateSalesReport_ThenCanceledTicketsAreNotCounted() {
        TicketStatus nonCanceledStatus = firstStatusThatIsNotCanceled();

        PurchasedTicket canceledTicket = mock(PurchasedTicket.class);
        when(canceledTicket.getStatus()).thenReturn(TicketStatus.CANCELED);
        when(canceledTicket.getPrice()).thenReturn(100.0);

        PurchasedTicket validTicket = mock(PurchasedTicket.class);
        when(validTicket.getStatus()).thenReturn(nonCanceledStatus);
        when(validTicket.getPrice()).thenReturn(80.0);

        Purchase purchase = purchase(1L, 55L, 12345, List.of(canceledTicket, validTicket));

        when(historyRepository.getPurchasesByCompanyId(COMPANY_ID)).thenReturn(List.of(purchase));

        SalesReportDTO report = historyService.generateSalesReport(TOKEN, COMPANY_ID);

        assertEquals(1, report.getTotalTicketsSold());
        assertEquals(BigDecimal.valueOf(80.0), report.getTotalRevenue());
        assertEquals("Sales report generated successfully", report.getMessage());
    }

    @Test
    void GivenOnlyCanceledTickets_WhenGenerateSalesReport_ThenReturnNoSalesData() {
        PurchasedTicket canceledTicket = mock(PurchasedTicket.class);
        when(canceledTicket.getStatus()).thenReturn(TicketStatus.CANCELED);
        when(canceledTicket.getPrice()).thenReturn(100.0);

        Purchase purchase = purchase(1L, 55L, 12345, List.of(canceledTicket));

        when(historyRepository.getPurchasesByCompanyId(COMPANY_ID)).thenReturn(List.of(purchase));

        SalesReportDTO report = historyService.generateSalesReport(TOKEN, COMPANY_ID);

        assertEquals(0, report.getTotalTicketsSold());
        assertEquals(BigDecimal.ZERO, report.getTotalRevenue());
        assertEquals("No sales data was found", report.getMessage());
    }

    @Test
    void GivenManagerNotInManagementTree_WhenGenerateSalesReport_ThenPurchaseIsIgnored() {
        TicketStatus nonCanceledStatus = firstStatusThatIsNotCanceled();

        PurchasedTicket ticket = mock(PurchasedTicket.class);
        when(ticket.getStatus()).thenReturn(nonCanceledStatus);
        when(ticket.getPrice()).thenReturn(100.0);

        Purchase purchaseManagedByOtherMember = new Purchase(
                1L,
                List.of(ticket),
                "Test Event",
                "Tel Aviv",
                55L,
                COMPANY_ID,
                999L,
                EVENT_ID,
                BigDecimal.valueOf(100),
                12345
        );

        when(historyRepository.getPurchasesByCompanyId(COMPANY_ID))
                .thenReturn(List.of(purchaseManagedByOtherMember));

        SalesReportDTO report = historyService.generateSalesReport(TOKEN, COMPANY_ID);

        assertEquals(0, report.getTotalTicketsSold());
        assertEquals(BigDecimal.ZERO, report.getTotalRevenue());
        assertEquals("No sales data was found", report.getMessage());
    }

    private Event createActiveEvent() {
        Event createdEvent = new Event(
                EVENT_ID,
                LocalDateTime.now().plusDays(10),
                "Test Event",
                COMPANY_ID,
                USER_ID,
                EventLocation.values()[0],
                100L,
                EventCategory.values()[0],
                "Artist",
                BigDecimal.valueOf(100),
                new Pair<>(10, 10)
        );

        createdEvent.setStatus(eventStatus.ACTIVE);
        return createdEvent;
    }

    private Purchase purchase(Long purchaseId, Long buyerId, Integer transactionId, List<PurchasedTicket> tickets) {
        return new Purchase(
                purchaseId,
                tickets,
                "Test Event",
                "Tel Aviv",
                buyerId,
                COMPANY_ID,
                USER_ID,
                EVENT_ID,
                BigDecimal.valueOf(100),
                transactionId
        );
    }

    private TicketStatus firstStatusThatIsNotCanceled() {
        return Arrays.stream(TicketStatus.values())
                .filter(status -> status != TicketStatus.CANCELED)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("TicketStatus must contain at least one non-canceled status"));
    }
}