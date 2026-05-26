package ticketsystem.AcceptanceTesting;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import ticketsystem.ApplicationLayer.ISystemLogger;
import ticketsystem.ApplicationLayer.ReservationService;
import ticketsystem.ApplicationLayer.TokenService;
import ticketsystem.DTO.ActiveOrderDTO;
import ticketsystem.DTO.OrderDTO;
import ticketsystem.DTO.PaymentDetails;
import ticketsystem.DTO.seatPositionDTO;
import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.IRepository.ILotteryRepository;
import ticketsystem.DomainLayer.IRepository.IOrderRepository;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.event.EventCategory;
import ticketsystem.DomainLayer.event.EventLocation;
import ticketsystem.DomainLayer.event.EventMap;
import ticketsystem.DomainLayer.event.Pair;
import ticketsystem.DomainLayer.event.SeatPosition;
import ticketsystem.DomainLayer.event.SeatingArea;
import ticketsystem.DomainLayer.event.StandingArea;
import ticketsystem.DomainLayer.lottery.Lottery;
import ticketsystem.DomainLayer.order.ActiveOrder;
import ticketsystem.DomainLayer.order.Ticket;
import ticketsystem.InfrastructureLayer.CompanyRepository;
import ticketsystem.InfrastructureLayer.OrderRepository;
import ticketsystem.InfrastructureLayer.TokenRepository;

public class ReservationServiceTest {

    private ReservationService reservationService;
    private IOrderRepository orderRepository;
    private IEventRepository eventRepository;
    private ILotteryRepository lotteryRepository;
    private FakePaymentService paymentService;
    private FakeSecureBarcode secureBarcode;
    private TokenService tokenService;
    private ISystemLogger logger;
    private CompanyRepository companyRepository;

    @BeforeEach
    void setUp() {
        orderRepository = new OrderRepository();
        eventRepository = new FakeEventRepository();
        lotteryRepository = new FakeLotteryRepository();
        paymentService = new FakePaymentService();
        secureBarcode = new FakeSecureBarcode();
        logger=new FakeSystemLogger();
        companyRepository = new CompanyRepository();

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
                secureBarcode,lotteryRepository,logger,companyRepository

        );
    }
    @Test
    void AcceptanceTest_SelectStandingTicket_WhenEventIsRegular_ThenTicketIsSelectedWithoutLotteryCode() {
        Long eventId = 10L;
        Long areaId = 1L;
        String token = "member-token-1";

        Event event = createActiveEvent(eventId);
        eventRepository.addEvent(event);

        boolean result = reservationService.selectStandingTicket(
                token,
                eventId,
                areaId,
                1,
                null
        );

        assertTrue(result);
    }

    @Test
    void AcceptanceTest_SelectStandingTicket_WhenLotteryEventAndValidCode_ThenTicketIsSelected() {
        Long eventId = 11L;
        Long areaId = 1L;
        String token = "member-token-1";
        String lotteryCode = "ABC12345";

        Event event = createActiveEvent(eventId);
        eventRepository.addEvent(event);

        Lottery lottery = new Lottery(1L, eventId, 1);
        lottery.registerMember(1L);
        lottery.setWinner(1L, lotteryCode);
        lotteryRepository.addLottery(lottery);

        boolean result = reservationService.selectStandingTicket(
                token,
                eventId,
                areaId,
                1,
                lotteryCode
        );

        assertTrue(result);
    }

    @Test
void AcceptanceTest_SelectStandingTicket_WhenLotteryEventAndMissingCode_ThenSelectionFails() {
    Long eventId = 12L;
    Long areaId = 1L;
    String token = "member-token-1";

    Event event = createActiveEvent(eventId);
    eventRepository.addEvent(event);

    Lottery lottery = new Lottery(1L, eventId, 1);
    lottery.registerMember(1L);
    lottery.setWinner(1L, "ABC12345");
    lotteryRepository.addLottery(lottery);

    assertThrows(
            IllegalArgumentException.class,
            () -> reservationService.selectStandingTicket(
                    token,
                    eventId,
                    areaId,
                    1,
                    null
            )
    );
}

@Test
void AcceptanceTest_SelectStandingTicket_WhenLotteryEventAndWrongCode_ThenSelectionFails() {
    Long eventId = 13L;
    Long areaId = 1L;
    String token = "member-token-1";

    Event event = createActiveEvent(eventId);
    eventRepository.addEvent(event);

    Lottery lottery = new Lottery(1L, eventId, 1);
    lottery.registerMember(1L);
    lottery.setWinner(1L, "ABC12345");
    lotteryRepository.addLottery(lottery);

    assertThrows(
            IllegalArgumentException.class,
            () -> reservationService.selectStandingTicket(
                    token,
                    eventId,
                    areaId,
                    1,
                    "WRONGCODE"
            )
    );
}
@Test
void AcceptanceTest_SelectStandingTicket_WhenLotteryEventAndUserDidNotWin_ThenSelectionFails() {
    Long eventId = 14L;
    Long areaId = 1L;
    String token = "member-token-1";

    Event event = createActiveEvent(eventId);
    eventRepository.addEvent(event);

    Lottery lottery = new Lottery(1L, eventId, 1);
    lottery.registerMember(999L);
    lottery.setWinner(999L, "ABC12345");
    lotteryRepository.addLottery(lottery);

    assertThrows(
            IllegalArgumentException.class,
            () -> reservationService.selectStandingTicket(
                    token,
                    eventId,
                    areaId,
                    1,
                    "ABC12345"
            )
    );
}

@Test
void AcceptanceTest_SelectSeatTicket_WhenEventIsRegular_ThenTicketIsSelectedWithoutLotteryCode() {
    Long eventId = 15L;
    Long areaId = 1L;
    String token = "member-token-1";

    Event event = createActiveEventWithSeatingArea(eventId);
    eventRepository.addEvent(event);

    seatPositionDTO position = new seatPositionDTO(1, 1);

    boolean result = reservationService.selectSeatTicket(
            token,
            eventId,
            areaId,
            position,
            null
    );

    assertTrue(result);
}
@Test
void AcceptanceTest_SelectSeatTicket_WhenLotteryEventAndValidCode_ThenTicketIsSelected() {
    Long eventId = 16L;
    Long areaId = 1L;
    String token = "member-token-1";
    String lotteryCode = "ABC12345";

    Event event = createActiveEventWithSeatingArea(eventId);
    eventRepository.addEvent(event);

    Lottery lottery = new Lottery(1L, eventId, 1);
    lottery.registerMember(1L);
    lottery.setWinner(1L, lotteryCode);
    lotteryRepository.addLottery(lottery);

    seatPositionDTO position = new seatPositionDTO(1, 1);

    boolean result = reservationService.selectSeatTicket(
            token,
            eventId,
            areaId,
            position,
            lotteryCode
    );

    assertTrue(result);
}

@Test
void AcceptanceTest_SelectSeatTicket_WhenLotteryEventAndMissingCode_ThenSelectionFails() {
    Long eventId = 17L;
    Long areaId = 1L;
    String token = "member-token-1";

    Event event = createActiveEventWithSeatingArea(eventId);
    eventRepository.addEvent(event);

    Lottery lottery = new Lottery(1L, eventId, 1);
    lottery.registerMember(1L);
    lottery.setWinner(1L, "ABC12345");
    lotteryRepository.addLottery(lottery);

    seatPositionDTO position = new seatPositionDTO(1, 1);

    assertThrows(
            IllegalArgumentException.class,
            () -> reservationService.selectSeatTicket(
                    token,
                    eventId,
                    areaId,
                    position,
                    null
            )
    );
}

@Test
void AcceptanceTest_SelectSeatTicket_WhenLotteryEventAndWrongCode_ThenSelectionFails() {
    Long eventId = 18L;
    Long areaId = 1L;
    String token = "member-token-1";

    Event event = createActiveEventWithSeatingArea(eventId);
    eventRepository.addEvent(event);

    Lottery lottery = new Lottery(1L, eventId, 1);
    lottery.registerMember(1L);
    lottery.setWinner(1L, "ABC12345");
    lotteryRepository.addLottery(lottery);

    seatPositionDTO position = new seatPositionDTO(1, 1);

    assertThrows(
            IllegalArgumentException.class,
            () -> reservationService.selectSeatTicket(
                    token,
                    eventId,
                    areaId,
                    position,
                    "WRONGCODE"
            )
    );
}

@Test
void AcceptanceTest_SelectSeatTicket_WhenLotteryEventAndUserDidNotWin_ThenSelectionFails() {
    Long eventId = 19L;
    Long areaId = 1L;
    String token = "member-token-1";

    Event event = createActiveEventWithSeatingArea(eventId);
    eventRepository.addEvent(event);

    Lottery lottery = new Lottery(1L, eventId, 1);
    lottery.registerMember(999L);
    lottery.setWinner(999L, "ABC12345");
    lotteryRepository.addLottery(lottery);

    seatPositionDTO position = new seatPositionDTO(1, 1);

    assertThrows(
            IllegalArgumentException.class,
            () -> reservationService.selectSeatTicket(
                    token,
                    eventId,
                    areaId,
                    position,
                    "ABC12345"
            )
    );
}
@Test
void AcceptanceTest_RemoveTicketFromActiveOrder_WhenSeatTicketExists_ThenTicketIsRemoved() {
    Long eventId = 20L;
    Long areaId = 1L;
    String token = "member-token-1";

    Event event = createActiveEventWithSeatingArea(eventId);
    eventRepository.addEvent(event);

    reservationService.selectSeatTicket(
            token,
            eventId,
            areaId,
            new seatPositionDTO(1, 1),
            null
    );

    ActiveOrder order = orderRepository.getActiveOrderByUserId(1L);
    Long ticketId = order.getTickets().get(0).getTicketId();

    boolean result = reservationService.removeTicketFromActiveOrder(
            token,
            eventId,
            ticketId
    );
    order = orderRepository.getActiveOrderByUserId(1L);
    assertTrue(result);
    assertTrue(order.getTickets().isEmpty());
}

    @Test
    void AcceptanceTest_RemoveTicketFromActiveOrder_WhenNoActiveOrderExists_ThenThrowException() {
        Long eventId = 21L;
        Long ticketId = 1L;
        String token = "member-token-1";

        Event event = createActiveEventWithSeatingArea(eventId);
        eventRepository.addEvent(event);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> reservationService.removeTicketFromActiveOrder(
                        token,
                        eventId,
                        ticketId
                )
        );

        assertEquals("No active order found for this event", exception.getMessage());
    }

    @Test
    void AcceptanceTest_RemoveStandingTicketsFromActiveOrder_WhenEnoughTicketsExist_ThenRequestedQuantityIsRemoved() {
        Long eventId = 22L;
        Long areaId = 1L;
        String token = "member-token-1";

        Event event = createActiveEvent(eventId);
        eventRepository.addEvent(event);

        reservationService.selectStandingTicket(
                token,
                eventId,
                areaId,
                3,
                null
        );

        ActiveOrder order = orderRepository.getActiveOrderByUserId(1L);

        boolean result = reservationService.removeStandingTicketsFromActiveOrder(
                token,
                eventId,
                areaId,
                2
        );

        ActiveOrder updatedOrder = orderRepository.getActiveOrderByUserId(1L);
        assertTrue(result);
        assertEquals(1, updatedOrder.getTickets().size());
    }

    @Test
    void AcceptanceTest_RemoveStandingTicketsFromActiveOrder_WhenNotEnoughTicketsExist_ThenThrowException() {
        Long eventId = 23L;
        Long areaId = 1L;
        String token = "member-token-1";

        Event event = createActiveEvent(eventId);
        eventRepository.addEvent(event);

        reservationService.selectStandingTicket(
                token,
                eventId,
                areaId,
                1,
                null
        );

        ActiveOrder order = orderRepository.getActiveOrderByUserId(1L);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reservationService.removeStandingTicketsFromActiveOrder(
                        token,
                        eventId,
                        areaId,
                        2
                )
        );

        assertEquals(
                "Not enough standing tickets in the order to remove",
                exception.getMessage()
        );

        assertEquals(1, order.getTickets().size());
    }
    @Test
    void AcceptanceTest_ViewActiveOrder_WhenActiveOrderExists_ThenReturnActiveOrderDTO() {
        Long eventId = 24L;
        Long areaId = 1L;
        String token = "member-token-1";

        Event event = createActiveEvent(eventId);
        eventRepository.addEvent(event);

        reservationService.selectStandingTicket(
                token,
                eventId,
                areaId,
                2,
                null
        );

        ActiveOrder order = orderRepository.getActiveOrderByUserId(1L);

        ActiveOrderDTO dto = reservationService.viewActiveOrder(
                token,
                order.getOrderId()
        );

        assertNotNull(dto);
        assertEquals(order.getOrderId(), dto.getOrderId());
        assertEquals(eventId, dto.getEventId());
        assertEquals(2, dto.getTickets().size());
    }

    @Test
    void AcceptanceTest_ViewActiveOrder_WhenOrderDoesNotExist_ThenThrowException() {
        String token = "member-token-1";
        Long nonExistingOrderId = 999L;

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> reservationService.viewActiveOrder(token, nonExistingOrderId)
        );

        assertEquals("No active order found", exception.getMessage());
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
                1,null
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
                1,null
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
                1,null
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
                1,"null"
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


    @Test
    void GivenExpiredOrder_WhenSelectSeatTicket_ThenExpiredOrderIsCancelledAndNewTicketIsSelected() {
        // Arrange
        Long eventId = 30L;
        Long areaId = 1L;
        String token = "member-token-1";

        Event event = createActiveEventWithSeatingArea(eventId);
        eventRepository.addEvent(event);

        reservationService.selectSeatTicket(
                token,
                eventId,
                areaId,
                new seatPositionDTO(1, 1),
                null
        );

        ActiveOrder expiredOrder = orderRepository.getActiveOrderByUserId(1L);
        Long expiredOrderId = expiredOrder.getOrderId();

        expiredOrder.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        orderRepository.updateOrder(expiredOrder);

        // Act
        boolean result = reservationService.selectSeatTicket(
                token,
                eventId,
                areaId,
                new seatPositionDTO(1, 2),
                null
        );

       assertTrue(result);

        assertNull(orderRepository.findOrderById(expiredOrderId));

        ActiveOrder newActiveOrder = orderRepository.getActiveOrderByUserId(1L);

        assertNotNull(newActiveOrder);
        assertEquals(ActiveOrder.OrderStatus.ACTIVE, newActiveOrder.getStatus());
        assertEquals(1, newActiveOrder.getTickets().size());
        assertNotEquals(expiredOrderId, newActiveOrder.getOrderId());
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

private Event createActiveEventWithSeatingArea(Long eventId) {
    Event event = new Event(
            eventId,
            LocalDateTime.now().plusDays(10),
            "Seat Test Event",
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

    SeatingArea seatingArea = new SeatingArea(
            1L,
            "Main Seating Area",
            new Pair<>(0, 0),   // location
            new Pair<>(5, 5),   // size
            5,                  // rows
            5                   // columns
    );

    realMap.addElement(seatingArea);

    event.setMap(realMap);

    return event;
}

    private PaymentDetails createPaymentDetails() {
        return new PaymentDetails("VISA","Yosi", LocalDate.now());
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

    private static class FakeLotteryRepository implements ILotteryRepository {

        private final ConcurrentHashMap<Long, Lottery> lotteries = new ConcurrentHashMap<>();

        @Override
        public void addLottery(Lottery lottery) {
            lotteries.put(lottery.getLotteryId(), lottery);
        }

        @Override
        public Lottery findById(long lotteryId) {
            return lotteries.get(lotteryId);
        }

        @Override
        public void update(Lottery lottery) {
            lotteries.put(lottery.getLotteryId(), lottery);
        }

        @Override
        public Lottery findByEventId(long eventId) {
            for (Lottery lottery : lotteries.values()) {
                if (lottery.getEventId() == eventId) {
                    return lottery;
                }
            }
            return null;
        }

        @Override
        public long generateNextLotteryId() {
            return lotteries.size() + 1L;
        }
    }

    private static class FakeSystemLogger implements ISystemLogger {

        private final List<String> messages = new ArrayList<>();
        private final List<LogLevel> levels = new ArrayList<>();

        @Override
        public void logEvent(String message, LogLevel level) {
            messages.add(message);
            levels.add(level);
         }

        @Override
        public void logError(String errorMessage, Throwable exception) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'logError'");
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
                secureBarcode,lotteryRepository,logger,companyRepository
        );
}
}