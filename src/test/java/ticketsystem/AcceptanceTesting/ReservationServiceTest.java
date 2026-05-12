package ticketsystem.AcceptanceTesting;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.ApplicationLayer.IPaymentService;
import ticketsystem.ApplicationLayer.ISecureBarcode;
import ticketsystem.ApplicationLayer.ReservationService;
import ticketsystem.ApplicationLayer.TokenService;
import ticketsystem.DTO.OrderDTO;
import ticketsystem.DTO.PaymentDetails;
import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.IRepository.IOrderRepository;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.event.EventCategory;
import ticketsystem.DomainLayer.event.EventLocation;
import ticketsystem.DomainLayer.event.EventMap;
import ticketsystem.DomainLayer.event.Pair;
import ticketsystem.DomainLayer.event.StandingArea;
import ticketsystem.InfrastructureLayer.OrderRepository;
import ticketsystem.InfrastructureLayer.TokenRepository;

public class ReservationServiceTest {

    private ReservationService reservationService;
    private IOrderRepository orderRepository;
    private IEventRepository eventRepository;
    private FakePaymentService paymentService;
    private FakeSecureBarcode secureBarcode;
    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        orderRepository = new OrderRepository();
        eventRepository = new FakeEventRepository();
        paymentService = new FakePaymentService();
        secureBarcode = new FakeSecureBarcode();

        tokenService = new TokenService(
                "manual_test_secret_32_chars_long",
                new TokenRepository()
        ) {
            @Override
            public boolean validateToken(String token) {
                return true;
            }

            @Override
            public boolean isGuestToken(String token) {
                return false;
            }

            @Override
            public boolean isMemberToken(String token) {
                return true;
            }

            @Override
            public Long extractUserId(String token) {
                return 1L;
            }
        };
    

        reservationService = new ReservationService(
                orderRepository,
                eventRepository,
                tokenService,
                paymentService,
                secureBarcode
        );
    }

    @Test
    void AcceptanceTest_Checkout_WhenPaymentAndTicketIssuingSucceed_ThenOrderIsCompletedAndBarcodeIssued() {
        Long eventId = 1L;
        Long areaId = 1L;
        String token = "member-token-1";

        Event event = createActiveEvent(eventId);
        eventRepository.addEvent(event);

        AtomicReference<OrderDTO> completedOrder = new AtomicReference<>();
        reservationService.addOrderListener(completedOrder::set);

        boolean selected = reservationService.selectStandingTicket(
                token,
                eventId,
                areaId,
                1
        );

        assertTrue(selected);

        PaymentDetails details = createPaymentDetails();

        boolean checkoutResult = reservationService.checkout(
                token,
                eventId,
                details
        );

        assertTrue(checkoutResult);

        assertTrue(paymentService.wasPayCalled.get());
        assertFalse(paymentService.wasRefundCalled.get());

        assertTrue(secureBarcode.wasGenerateCalled.get());

        assertNotNull(completedOrder.get(), "Completed order should be sent to listeners");
        assertFalse(completedOrder.get().getTickets().isEmpty(), "Order should contain purchased tickets");

        assertNotNull(
                completedOrder.get().getTickets().get(0).getSecureBarcode(),
                "Purchased ticket should contain secure barcode"
        );
    }

    @Test
    void AcceptanceTest_Checkout_WhenPaymentFails_ThenCheckoutThrowsAndNoBarcodeIssued() {
        Long eventId = 2L;
        Long areaId = 1L;
        String token = "member-token-1";

        Event event = createActiveEvent(eventId);
        eventRepository.addEvent(event);

        reservationService.selectStandingTicket(
                token,
                eventId,
                areaId,
                1
        );

        paymentService.shouldPaymentSucceed = false;

        assertThrows(
                IllegalStateException.class,
                () -> reservationService.checkout(token, eventId, createPaymentDetails())
        );

        assertTrue(paymentService.wasPayCalled.get());
        assertFalse(paymentService.wasRefundCalled.get());
        assertFalse(secureBarcode.wasGenerateCalled.get());
    }

    @Test
    void AcceptanceTest_Checkout_WhenTicketIssuingFailsAfterPayment_ThenRefundIsCalled() {
        Long eventId = 3L;
        Long areaId = 1L;
        String token = "member-token-1";

        Event event = createActiveEvent(eventId);
        eventRepository.addEvent(event);

        reservationService.selectStandingTicket(
                token,
                eventId,
                areaId,
                1
        );

        secureBarcode.shouldGenerateSucceed = false;

        assertThrows(
                IllegalStateException.class,
                () -> reservationService.checkout(token, eventId, createPaymentDetails())
        );

        assertTrue(paymentService.wasPayCalled.get());
        assertTrue(paymentService.wasRefundCalled.get());
        assertTrue(secureBarcode.wasGenerateCalled.get());
    }

    @Test
    void AcceptanceTest_GuestCheckout_WhenPaymentAndTicketIssuingSucceed_ThenOrderIsCompletedAndBarcodeIssued() {
        useGuestTokenService();

        Long eventId = 4L;
        Long areaId = 1L;
        String token = "guest-token-1";

        Event event = createActiveEvent(eventId);
        eventRepository.addEvent(event);

        AtomicReference<OrderDTO> completedOrder = new AtomicReference<>();
        reservationService.addOrderListener(completedOrder::set);

        boolean selected = reservationService.selectStandingTicket(
                token,
                eventId,
                areaId,
                1
        );

        assertTrue(selected);

        boolean checkoutResult = reservationService.checkout(
                token,
                eventId,
                createPaymentDetails()
        );

        assertTrue(checkoutResult);

        assertTrue(paymentService.wasPayCalled.get());
        assertFalse(paymentService.wasRefundCalled.get());
        assertTrue(secureBarcode.wasGenerateCalled.get());

        assertNotNull(completedOrder.get());
        assertFalse(completedOrder.get().getTickets().isEmpty());
        assertNotNull(completedOrder.get().getTickets().get(0).getSecureBarcode());
    }

   //create event
   private Event createActiveEvent(Long eventId) {
    Event event = new Event(
            eventId,
            LocalDateTime.now().plusDays(10),
            "Checkout Test Event",
            1L,
            1L,
            EventLocation.TEL_AVIV,
            100L,
            EventCategory.CONCERT,
            "Test Artist",
            new BigDecimal("100.00"),
            new Pair<>(10, 10)
    );

    event.setStatus(Event.eventStatus.ACTIVE);

    EventMap realMap = new EventMap(new Pair<>(10, 10));

    StandingArea standingArea = new StandingArea(
            1L,
            "Main Standing Area",
            new Pair<>(0, 0),     // location
            new Pair<>(5, 5),     // size
            100                   // capacity
    );

    realMap.addElement(standingArea);

    event.setMap(realMap);

    return event;
}


    private PaymentDetails createPaymentDetails() {
        return new PaymentDetails("VISA","Yosi");
    }

    private static class FakePaymentService implements IPaymentService {

        boolean shouldPaymentSucceed = true;
        boolean shouldRefundSucceed = true;

        AtomicBoolean wasConnectCalled = new AtomicBoolean(false);
        AtomicBoolean wasPayCalled = new AtomicBoolean(false);
        AtomicBoolean wasRefundCalled = new AtomicBoolean(false);

        @Override
        public boolean connect() {
            wasConnectCalled.set(true);
            return true;
        }

        @Override
        public boolean pay(BigDecimal amount, PaymentDetails details) {
            wasPayCalled.set(true);
            return shouldPaymentSucceed;
        }

        @Override
        public boolean refund(BigDecimal amount, PaymentDetails details) {
            wasRefundCalled.set(true);
            return shouldRefundSucceed;
        }
    }

    private static class FakeSecureBarcode implements ISecureBarcode {

        boolean shouldGenerateSucceed = true;
        AtomicBoolean wasGenerateCalled = new AtomicBoolean(false);

        @Override
        public boolean connect() {
            return true;
        }

        @Override
        public String generateSecureBarcode(Long ticketId, Long eventId, Long userId) {
            wasGenerateCalled.set(true);

            if (!shouldGenerateSucceed) {
                throw new IllegalStateException("Ticket issuing failed");
            }

            return "SECURE_BARCODE_" + ticketId + "_" + eventId;
        }
    }

    private static class FakeEventRepository implements IEventRepository {

        private final ConcurrentHashMap<Long, Event> events = new ConcurrentHashMap<>();

        @Override
        public void addEvent(Event event) {
            events.put(event.getId(), event);
        }

        @Override
        public Event getEventById(Long eventId) {
            return events.get(eventId);
        }

        @Override
        public void updateEvent(Event event) {
            events.put(event.getId(), event);
        }

        @Override
        public void deleteEvent(Long eventId, long expectedVersion) {
            events.remove(eventId);
        }

        @Override
        public long getNextId() {
            return events.size() + 1L;
        }

        @Override
        public List<Event> getEventsByCompanyId(Long companyId) {
            List<Event> result = new ArrayList<>();

            for (Event event : events.values()) {
                if (event.getCompanyId().equals(companyId)) {
                    result.add(event);
                }
            }

            return result;
        }

        @Override
        public List<Event> getAllEvents() {
            return new ArrayList<>(events.values());
        }
    }

    private void useGuestTokenService() {
    tokenService = new TokenService(
            "manual_test_secret_32_chars_long",
            new TokenRepository()
    ) {
        @Override
        public boolean validateToken(String token) {
            return true;
        }

        @Override
        public boolean isGuestToken(String token) {
            return true;
        }

        @Override
        public boolean isMemberToken(String token) {
            return false;
        }

        @Override
        public Long extractUserId(String token) {
            return null;
        }
    };

    reservationService = new ReservationService(
            orderRepository,
            eventRepository,
            tokenService,
            paymentService,
            secureBarcode
    );
}
}