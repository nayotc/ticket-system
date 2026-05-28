package ticketsystem.AcceptanceTesting;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ticketsystem.ApplicationLayer.INotifier;
import ticketsystem.ApplicationLayer.IPaymentService;
import ticketsystem.ApplicationLayer.ISecureBarcode;
import ticketsystem.ApplicationLayer.ISystemLogger;
import ticketsystem.ApplicationLayer.ReservationService;
import ticketsystem.ApplicationLayer.TokenService;
import ticketsystem.ApplicationLayer.UserAccessService;
import ticketsystem.ApplicationLayer.UserService;
import ticketsystem.DTO.ActiveOrderDTO;
import ticketsystem.DTO.OrderDTO;
import ticketsystem.DTO.PaymentDetails;
import ticketsystem.DTO.seatPositionDTO;
import ticketsystem.DomainLayer.EventCatalogDomainService;
import ticketsystem.DomainLayer.MembershipDomainService;
import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.IRepository.ILotteryRepository;
import ticketsystem.DomainLayer.IRepository.IOrderRepository;
import ticketsystem.DomainLayer.IRepository.ITokenRepository;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.DomainLayer.company.Company;
import ticketsystem.DomainLayer.discount.DiscountCompositionType;
import ticketsystem.DomainLayer.discount.DiscountPolicy;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.event.EventCategory;
import ticketsystem.DomainLayer.event.EventLocation;
import ticketsystem.DomainLayer.event.EventMap;
import ticketsystem.DomainLayer.event.Pair;
import ticketsystem.DomainLayer.event.SeatingArea;
import ticketsystem.DomainLayer.event.StandingArea;
import ticketsystem.DomainLayer.lottery.Lottery;
import ticketsystem.DomainLayer.order.ActiveOrder;
import ticketsystem.InfrastructureLayer.CompanyRepository;
import ticketsystem.InfrastructureLayer.EventRepository;
import ticketsystem.InfrastructureLayer.LotteryRepository;
import ticketsystem.DomainLayer.order.Ticket;
import ticketsystem.InfrastructureLayer.OrderRepository;
import ticketsystem.InfrastructureLayer.PaymentServiceProxy;
import ticketsystem.InfrastructureLayer.TokenRepository;
import ticketsystem.InfrastructureLayer.UserRepository;
import ticketsystem.DomainLayer.policy.PurchasePolicy;

public class ReservationServiceTest {
        private ReservationService reservationService;

        private IOrderRepository orderRepository;
        private IEventRepository eventRepository;
        private ILotteryRepository lotteryRepository;
        private ICompanyRepository companyRepository;
        private IUserRepository userRepository;

        private TokenService tokenService;
        private UserService userService;
        private EventCatalogDomainService eventCatalogDomainService;
        private IPaymentService paymentService;
        private TestSecureBarcode secureBarcode;
        private ISystemLogger logger;
        private FakeNotifier fakeNotifier;
        private MembershipDomainService membershipDomain;
        private UserAccessService userAccessService;
        private String memberToken;
        private String guestToken;
        private Long memberId;

        private static final Long COMPANY_ID = 1L;
        private static final Long COMPANY_FOUNDER_ID = 1L;

        @BeforeEach
        void setUp() {
                orderRepository = new OrderRepository();
                eventRepository = new EventRepository();
                lotteryRepository = new LotteryRepository();
                companyRepository = new CompanyRepository();
                userRepository = new UserRepository();

                membershipDomain = new MembershipDomainService(userRepository);

                paymentService = new PaymentServiceProxy();
                secureBarcode = new TestSecureBarcode();
                logger = new NoOpSystemLogger();
                fakeNotifier = new FakeNotifier();
                userAccessService = new UserAccessService(userRepository);

                ITokenRepository tokenRepository = new TokenRepository();

                tokenService = new TokenService(
                                "manual_test_secret_32_chars_long_for_tests",
                                tokenRepository);

                userService = new UserService(userRepository, tokenService, logger);

                memberToken = createLoggedInMember("member_user", "password123");
                memberId = tokenService.extractUserId(memberToken);
                guestToken = userService.visitSystem();

                Company company = new Company(
                                "BGU Productions",
                                COMPANY_FOUNDER_ID,
                                PurchasePolicy.noRestrictions(),
                                new DiscountPolicy(DiscountCompositionType.MAX));

                company.setId(COMPANY_ID);
                companyRepository.save(company);

                eventCatalogDomainService = new EventCatalogDomainService(
                                (CompanyRepository) companyRepository);

                resetPaymentProxy();

                reservationService = new ReservationService(
                                orderRepository,
                                eventRepository,
                                companyRepository,
                                membershipDomain,
                                tokenService,
                                paymentService,
                                secureBarcode,
                                lotteryRepository,
                                eventCatalogDomainService,
                                logger,
                                fakeNotifier, userAccessService);
        }

        private String createLoggedInMember(String username, String password) {
                String guest = userService.visitSystem();

                boolean signedUp = userService.signUp(
                                guest,
                                username,
                                password,
                                "Test User",
                                "0500000000");
                assertTrue(signedUp);

                String token = userService.login(guest, username, password);
                assertNotNull(token);

                return token;
        }

        private void resetPaymentProxy() {
                PaymentServiceProxy.isConnectionSuccessful = true;
                PaymentServiceProxy.isPaymentSuccessful = true;
                PaymentServiceProxy.isRefundSuccessful = true;

                PaymentServiceProxy.wasConnectCalled = false;
                PaymentServiceProxy.wasPayCalled = false;
                PaymentServiceProxy.wasRefundCalled = false;
        }

        @Test
        void AcceptanceTest_SelectStandingTicket_WhenEventIsRegular_ThenTicketIsSelectedWithoutLotteryCode() {
                Long eventId = 10L;
                Long areaId = 1L;

                Event event = createActiveEvent(eventId);
                eventRepository.addEvent(event);

                boolean result = reservationService.selectStandingTicket(
                                memberToken,
                                eventId,
                                areaId,
                                1,
                                null);

                assertTrue(result);
        }

        @Test
        void AcceptanceTest_SelectStandingTicket_WhenLotteryEventAndValidCode_ThenTicketIsSelected() {
                Long eventId = 11L;
                Long areaId = 1L;
                String lotteryCode = "ABC12345";

                Event event = createActiveEvent(eventId);
                eventRepository.addEvent(event);

                Lottery lottery = new Lottery(1L, eventId, 1);
                lottery.registerMember(memberId);
                lottery.setWinner(memberId, lotteryCode);
                lotteryRepository.addLottery(lottery);

                boolean result = reservationService.selectStandingTicket(
                                memberToken,
                                eventId,
                                areaId,
                                1,
                                lotteryCode);

                assertTrue(result);
        }

        @Test
        void AcceptanceTest_SelectStandingTicket_WhenLotteryEventAndMissingCode_ThenSelectionFails() {
                Long eventId = 12L;
                Long areaId = 1L;

                Event event = createActiveEvent(eventId);
                eventRepository.addEvent(event);

                Lottery lottery = new Lottery(1L, eventId, 1);
                lottery.registerMember(memberId);
                lottery.setWinner(memberId, "ABC12345");
                lotteryRepository.addLottery(lottery);

                assertThrows(IllegalArgumentException.class, () -> reservationService.selectStandingTicket(
                                memberToken,
                                eventId,
                                areaId,
                                1,
                                null));
        }

        @Test
        void AcceptanceTest_SelectStandingTicket_WhenLotteryEventAndWrongCode_ThenSelectionFails() {
                Long eventId = 13L;
                Long areaId = 1L;

                Event event = createActiveEvent(eventId);
                eventRepository.addEvent(event);

                Lottery lottery = new Lottery(1L, eventId, 1);
                lottery.registerMember(memberId);
                lottery.setWinner(memberId, "ABC12345");
                lotteryRepository.addLottery(lottery);

                assertThrows(IllegalArgumentException.class, () -> reservationService.selectStandingTicket(
                                memberToken,
                                eventId,
                                areaId,
                                1,
                                "WRONGCODE"));
        }

        @Test
        void AcceptanceTest_SelectStandingTicket_WhenLotteryEventAndUserDidNotWin_ThenSelectionFails() {
                Long eventId = 14L;
                Long areaId = 1L;
                Long otherUserId = 999L;

                Event event = createActiveEvent(eventId);
                eventRepository.addEvent(event);

                Lottery lottery = new Lottery(1L, eventId, 1);
                lottery.registerMember(otherUserId);
                lottery.setWinner(otherUserId, "ABC12345");
                lotteryRepository.addLottery(lottery);

                assertThrows(IllegalArgumentException.class, () -> reservationService.selectStandingTicket(
                                memberToken,
                                eventId,
                                areaId,
                                1,
                                "ABC12345"));
        }

        @Test
        void AcceptanceTest_SelectSeatTicket_WhenEventIsRegular_ThenTicketIsSelectedWithoutLotteryCode() {
                Long eventId = 15L;
                Long areaId = 1L;

                Event event = createActiveEventWithSeatingArea(eventId);
                eventRepository.addEvent(event);

                boolean result = reservationService.selectSeatTicket(
                                memberToken,
                                eventId,
                                areaId,
                                new seatPositionDTO(1, 1),
                                null);

                assertTrue(result);
        }

        @Test
        void AcceptanceTest_SelectSeatTicket_WhenLotteryEventAndValidCode_ThenTicketIsSelected() {
                Long eventId = 16L;
                Long areaId = 1L;
                String lotteryCode = "ABC12345";

                Event event = createActiveEventWithSeatingArea(eventId);
                eventRepository.addEvent(event);

                Lottery lottery = new Lottery(1L, eventId, 1);
                lottery.registerMember(memberId);
                lottery.setWinner(memberId, lotteryCode);
                lotteryRepository.addLottery(lottery);

                boolean result = reservationService.selectSeatTicket(
                                memberToken,
                                eventId,
                                areaId,
                                new seatPositionDTO(1, 1),
                                lotteryCode);

                assertTrue(result);
        }

        @Test
        void AcceptanceTest_SelectSeatTicket_WhenLotteryEventAndMissingCode_ThenSelectionFails() {
                Long eventId = 17L;
                Long areaId = 1L;

                Event event = createActiveEventWithSeatingArea(eventId);
                eventRepository.addEvent(event);

                Lottery lottery = new Lottery(1L, eventId, 1);
                lottery.registerMember(memberId);
                lottery.setWinner(memberId, "ABC12345");
                lotteryRepository.addLottery(lottery);

                assertThrows(IllegalArgumentException.class, () -> reservationService.selectSeatTicket(
                                memberToken,
                                eventId,
                                areaId,
                                new seatPositionDTO(1, 1),
                                null));
        }

        @Test
        void AcceptanceTest_SelectSeatTicket_WhenLotteryEventAndWrongCode_ThenSelectionFails() {
                Long eventId = 18L;
                Long areaId = 1L;

                Event event = createActiveEventWithSeatingArea(eventId);
                eventRepository.addEvent(event);

                Lottery lottery = new Lottery(1L, eventId, 1);
                lottery.registerMember(memberId);
                lottery.setWinner(memberId, "ABC12345");
                lotteryRepository.addLottery(lottery);

                assertThrows(IllegalArgumentException.class, () -> reservationService.selectSeatTicket(
                                memberToken,
                                eventId,
                                areaId,
                                new seatPositionDTO(1, 1),
                                "WRONGCODE"));
        }

        @Test
        void AcceptanceTest_SelectSeatTicket_WhenLotteryEventAndUserDidNotWin_ThenSelectionFails() {
                Long eventId = 19L;
                Long areaId = 1L;
                Long otherUserId = 999L;

                Event event = createActiveEventWithSeatingArea(eventId);
                eventRepository.addEvent(event);

                Lottery lottery = new Lottery(1L, eventId, 1);
                lottery.registerMember(otherUserId);
                lottery.setWinner(otherUserId, "ABC12345");
                lotteryRepository.addLottery(lottery);

                assertThrows(IllegalArgumentException.class, () -> reservationService.selectSeatTicket(
                                memberToken,
                                eventId,
                                areaId,
                                new seatPositionDTO(1, 1),
                                "ABC12345"));
        }

        @Test
        void AcceptanceTest_RemoveTicketFromActiveOrder_WhenSeatTicketExists_ThenTicketIsRemoved() {
                Long eventId = 20L;
                Long areaId = 1L;

                Event event = createActiveEventWithSeatingArea(eventId);
                eventRepository.addEvent(event);

                reservationService.selectSeatTicket(
                                memberToken,
                                eventId,
                                areaId,
                                new seatPositionDTO(1, 1),
                                null);

                ActiveOrder order = orderRepository.getActiveOrderByUserId(memberId);
                Long ticketId = order.getTickets().get(0).getTicketId();

                boolean result = reservationService.removeTicketFromActiveOrder(
                                memberToken,
                                eventId,
                                ticketId);

                ActiveOrder updatedOrder = orderRepository.getActiveOrderByUserId(memberId);

                assertTrue(result);
                assertTrue(updatedOrder.getTickets().isEmpty());
        }

        @Test
        void AcceptanceTest_RemoveTicketFromActiveOrder_WhenNoActiveOrderExists_ThenThrowException() {
                Long eventId = 21L;
                Long ticketId = 1L;

                Event event = createActiveEventWithSeatingArea(eventId);
                eventRepository.addEvent(event);

                IllegalStateException exception = assertThrows(
                                IllegalStateException.class,
                                () -> reservationService.removeTicketFromActiveOrder(
                                                memberToken,
                                                eventId,
                                                ticketId));

                assertEquals("No active order found for this event", exception.getMessage());
        }

        @Test
        void AcceptanceTest_RemoveStandingTicketsFromActiveOrder_WhenEnoughTicketsExist_ThenRequestedQuantityIsRemoved() {
                Long eventId = 22L;
                Long areaId = 1L;

                Event event = createActiveEvent(eventId);
                eventRepository.addEvent(event);

                reservationService.selectStandingTicket(
                                memberToken,
                                eventId,
                                areaId,
                                3,
                                null);

                boolean result = reservationService.removeStandingTicketsFromActiveOrder(
                                memberToken,
                                eventId,
                                areaId,
                                2);

                ActiveOrder updatedOrder = orderRepository.getActiveOrderByUserId(memberId);

                assertTrue(result);
                assertEquals(1, updatedOrder.getTickets().size());
        }

        @Test
        void AcceptanceTest_RemoveStandingTicketsFromActiveOrder_WhenNotEnoughTicketsExist_ThenThrowException() {
                Long eventId = 23L;
                Long areaId = 1L;

                Event event = createActiveEvent(eventId);
                eventRepository.addEvent(event);

                reservationService.selectStandingTicket(
                                memberToken,
                                eventId,
                                areaId,
                                1,
                                null);

                ActiveOrder order = orderRepository.getActiveOrderByUserId(memberId);

                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> reservationService.removeStandingTicketsFromActiveOrder(
                                                memberToken,
                                                eventId,
                                                areaId,
                                                2));

                assertEquals("Not enough standing tickets in the order to remove", exception.getMessage());
                assertEquals(1, order.getTickets().size());
        }

        @Test
        void AcceptanceTest_ViewActiveOrder_WhenActiveOrderExists_ThenReturnActiveOrderDTO() {
                Long eventId = 24L;
                Long areaId = 1L;

                Event event = createActiveEvent(eventId);
                eventRepository.addEvent(event);

                reservationService.selectStandingTicket(
                        memberToken,
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

        Event event = createActiveEventWithSeatingArea(eventId);
        eventRepository.addEvent(event);

        boolean result = reservationService.selectSeatTicket(
                memberToken,
                eventId,
                areaId,
                new seatPositionDTO(1, 1),
                null
        );

        assertTrue(result);
    }

    @Test
    void AcceptanceTest_SelectSeatTicket_WhenLotteryEventAndValidCode_ThenTicketIsSelected() {
        Long eventId = 16L;
        Long areaId = 1L;
        String lotteryCode = "ABC12345";

        Event event = createActiveEventWithSeatingArea(eventId);
        eventRepository.addEvent(event);

        Lottery lottery = new Lottery(1L, eventId, 1);
        lottery.registerMember(memberId);
        lottery.setWinner(memberId, lotteryCode);
        lotteryRepository.addLottery(lottery);

        boolean result = reservationService.selectSeatTicket(
                memberToken,
                eventId,
                areaId,
                new seatPositionDTO(1, 1),
                lotteryCode
        );

        assertTrue(result);
    }

    @Test
    void AcceptanceTest_SelectSeatTicket_WhenLotteryEventAndMissingCode_ThenSelectionFails() {
        Long eventId = 17L;
        Long areaId = 1L;

        Event event = createActiveEventWithSeatingArea(eventId);
        eventRepository.addEvent(event);

        Lottery lottery = new Lottery(1L, eventId, 1);
        lottery.registerMember(memberId);
        lottery.setWinner(memberId, "ABC12345");
        lotteryRepository.addLottery(lottery);

        assertThrows(IllegalArgumentException.class, () ->
                reservationService.selectSeatTicket(
                        memberToken,
                        eventId,
                        areaId,
                        new seatPositionDTO(1, 1),
                        null
                )
        );
    }

    @Test
    void AcceptanceTest_SelectSeatTicket_WhenLotteryEventAndWrongCode_ThenSelectionFails() {
        Long eventId = 18L;
        Long areaId = 1L;

        Event event = createActiveEventWithSeatingArea(eventId);
        eventRepository.addEvent(event);

        Lottery lottery = new Lottery(1L, eventId, 1);
        lottery.registerMember(memberId);
        lottery.setWinner(memberId, "ABC12345");
        lotteryRepository.addLottery(lottery);

        assertThrows(IllegalArgumentException.class, () ->
                reservationService.selectSeatTicket(
                        memberToken,
                        eventId,
                        areaId,
                        new seatPositionDTO(1, 1),
                        "WRONGCODE"
                )
        );
    }

    @Test
    void AcceptanceTest_SelectSeatTicket_WhenLotteryEventAndUserDidNotWin_ThenSelectionFails() {
        Long eventId = 19L;
        Long areaId = 1L;
        Long otherUserId = 999L;

        Event event = createActiveEventWithSeatingArea(eventId);
        eventRepository.addEvent(event);

        Lottery lottery = new Lottery(1L, eventId, 1);
        lottery.registerMember(otherUserId);
        lottery.setWinner(otherUserId, "ABC12345");
        lotteryRepository.addLottery(lottery);

        assertThrows(IllegalArgumentException.class, () ->
                reservationService.selectSeatTicket(
                        memberToken,
                        eventId,
                        areaId,
                        new seatPositionDTO(1, 1),
                        "ABC12345"
                )
        );
    }

    @Test
    void AcceptanceTest_RemoveTicketFromActiveOrder_WhenSeatTicketExists_ThenTicketIsRemoved() {
        Long eventId = 20L;
        Long areaId = 1L;

        Event event = createActiveEventWithSeatingArea(eventId);
        eventRepository.addEvent(event);

        reservationService.selectSeatTicket(
                memberToken,
                eventId,
                areaId,
                new seatPositionDTO(1, 1),
                null
        );

        ActiveOrder order = orderRepository.getActiveOrderByUserId(memberId);
        Long ticketId = order.getTickets().get(0).getTicketId();

        boolean result = reservationService.removeTicketFromActiveOrder(
                memberToken,
                eventId,
                ticketId
        );

        ActiveOrder updatedOrder = orderRepository.getActiveOrderByUserId(memberId);

        assertTrue(result);
        assertTrue(updatedOrder.getTickets().isEmpty());
    }

    @Test
    void AcceptanceTest_RemoveTicketFromActiveOrder_WhenNoActiveOrderExists_ThenThrowException() {
        Long eventId = 21L;
        Long ticketId = 1L;

        Event event = createActiveEventWithSeatingArea(eventId);
        eventRepository.addEvent(event);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> reservationService.removeTicketFromActiveOrder(
                        memberToken,
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

        Event event = createActiveEvent(eventId);
        eventRepository.addEvent(event);

        reservationService.selectStandingTicket(
                memberToken,
                eventId,
                areaId,
                3,
                null
        );

        boolean result = reservationService.removeStandingTicketsFromActiveOrder(
                memberToken,
                eventId,
                areaId,
                2
        );

        ActiveOrder updatedOrder = orderRepository.getActiveOrderByUserId(memberId);

        assertTrue(result);
        assertEquals(1, updatedOrder.getTickets().size());
    }

    @Test
    void AcceptanceTest_RemoveStandingTicketsFromActiveOrder_WhenNotEnoughTicketsExist_ThenThrowException() {
        Long eventId = 23L;
        Long areaId = 1L;

        Event event = createActiveEvent(eventId);
        eventRepository.addEvent(event);

        reservationService.selectStandingTicket(
                memberToken,
                eventId,
                areaId,
                1,
                null
        );

        ActiveOrder order = orderRepository.getActiveOrderByUserId(memberId);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reservationService.removeStandingTicketsFromActiveOrder(
                        memberToken,
                        eventId,
                        areaId,
                        2
                )
        );

        assertEquals("Not enough standing tickets in the order to remove", exception.getMessage());
        assertEquals(1, order.getTickets().size());
    }

    @Test
    void AcceptanceTest_ViewActiveOrder_WhenActiveOrderExists_ThenReturnActiveOrderDTO() {
        Long eventId = 24L;
        Long areaId = 1L;

        Event event = createActiveEvent(eventId);
        eventRepository.addEvent(event);

        reservationService.selectStandingTicket(
                memberToken,
                eventId,
                areaId,
                2,
                null
        );

        ActiveOrder order = orderRepository.getActiveOrderByUserId(memberId);

        ActiveOrderDTO dto = reservationService.viewActiveOrder(
                memberToken,
                order.getOrderId()
        );

        assertNotNull(dto);
        assertEquals(order.getOrderId(), dto.getOrderId());
        assertEquals(eventId, dto.getEventId());
        assertEquals(2, dto.getTickets().size());
    }

    @Test
    void AcceptanceTest_ViewActiveOrder_WhenOrderDoesNotExist_ThenThrowException() {
        Long nonExistingOrderId = 999L;

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> reservationService.viewActiveOrder(memberToken, nonExistingOrderId)
        );

        assertEquals("No active order found", exception.getMessage());
    }

    @Test
    void AcceptanceTest_Checkout_WhenPaymentAndTicketIssuingSucceed_ThenOrderIsCompletedAndBarcodeIssued() {
        Long eventId = 1L;
        Long areaId = 1L;

        Event event = createActiveEvent(eventId);
        eventRepository.addEvent(event);

        AtomicReference<OrderDTO> completedOrder = new AtomicReference<>();
        reservationService.addOrderListener(completedOrder::set);

        boolean selected = reservationService.selectStandingTicket(
                memberToken,
                eventId,
                areaId,
                1,
                null
        );

        assertTrue(selected);

        boolean checkoutResult = reservationService.checkout(
                memberToken,
                eventId,
                createPaymentDetails(),
                null
        );

        assertTrue(checkoutResult);

        assertTrue(PaymentServiceProxy.wasPayCalled);
        assertFalse(PaymentServiceProxy.wasRefundCalled);
        assertTrue(secureBarcode.wasGenerateCalled.get());

        assertNotNull(completedOrder.get());
        assertFalse(completedOrder.get().getTickets().isEmpty());
        assertNotNull(completedOrder.get().getTickets().get(0).getSecureBarcode());
    }

    @Test
    void AcceptanceTest_Checkout_WhenPaymentFails_ThenCheckoutThrowsAndNoBarcodeIssued() {
        Long eventId = 2L;
        Long areaId = 1L;

        Event event = createActiveEvent(eventId);
        eventRepository.addEvent(event);

        reservationService.selectStandingTicket(
                memberToken,
                eventId,
                areaId,
                1,
                null
        );

        PaymentServiceProxy.isPaymentSuccessful = false;

        assertThrows(
                IllegalStateException.class,
                () -> reservationService.checkout(
                        memberToken,
                        eventId,
                        createPaymentDetails(),
                        null
                )
        );

        assertTrue(PaymentServiceProxy.wasPayCalled);
        assertFalse(PaymentServiceProxy.wasRefundCalled);
        assertFalse(secureBarcode.wasGenerateCalled.get());
    }

    @Test
    void AcceptanceTest_Checkout_WhenTicketIssuingFailsAfterPayment_ThenRefundIsCalled() {
        Long eventId = 3L;
        Long areaId = 1L;

        Event event = createActiveEvent(eventId);
        eventRepository.addEvent(event);

        reservationService.selectStandingTicket(
                memberToken,
                eventId,
                areaId,
                1,
                null
        );

        secureBarcode.shouldGenerateSucceed = false;

        assertThrows(
                IllegalStateException.class,
                () -> reservationService.checkout(
                        memberToken,
                        eventId,
                        createPaymentDetails(),
                        null
                )
        );

        assertTrue(PaymentServiceProxy.wasPayCalled);
        assertTrue(PaymentServiceProxy.wasRefundCalled);
        assertTrue(secureBarcode.wasGenerateCalled.get());
    }
    @Test
    void AcceptanceTest_GuestCheckout_WhenPaymentAndTicketIssuingSucceed_ThenOrderIsCompletedAndBarcodeIssued() {
        Long eventId = 4L;
        Long areaId = 1L;

        Event event = createActiveEvent(eventId);
        eventRepository.addEvent(event);

        AtomicReference<OrderDTO> completedOrder = new AtomicReference<>();
        reservationService.addOrderListener(completedOrder::set);

        boolean selected = reservationService.selectStandingTicket(
                guestToken,
                eventId,
                areaId,
                1,
                null
        );

        assertTrue(selected);

        boolean checkoutResult = reservationService.checkout(
                guestToken,
                eventId,
                createPaymentDetails(),
                null
        );

        assertTrue(checkoutResult);

        assertTrue(PaymentServiceProxy.wasPayCalled);
        assertFalse(PaymentServiceProxy.wasRefundCalled);
        assertTrue(secureBarcode.wasGenerateCalled.get());

        assertNotNull(completedOrder.get());
        assertFalse(completedOrder.get().getTickets().isEmpty());
        assertNotNull(completedOrder.get().getTickets().get(0).getSecureBarcode());
    }

    @Test
    void GivenExpiredOrder_WhenSelectSeatTicket_ThenExpiredOrderIsCancelledAndNewTicketIsSelected() {
        Long eventId = 30L;
        Long areaId = 1L;

        Event event = createActiveEventWithSeatingArea(eventId);
        eventRepository.addEvent(event);

        reservationService.selectSeatTicket(
                memberToken,
                eventId,
                areaId,
                new seatPositionDTO(1, 1),
                null
        );

        ActiveOrder expiredOrder = orderRepository.getActiveOrderByUserId(memberId);
        Long expiredOrderId = expiredOrder.getOrderId();

        expiredOrder.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        orderRepository.updateOrder(expiredOrder);

        boolean result = reservationService.selectSeatTicket(
                memberToken,
                eventId,
                areaId,
                new seatPositionDTO(1, 2),
                null
        );

        assertTrue(result);
        assertNull(orderRepository.findOrderById(expiredOrderId));

        ActiveOrder newActiveOrder = orderRepository.getActiveOrderByUserId(memberId);

        assertNotNull(newActiveOrder);
        assertEquals(ActiveOrder.OrderStatus.ACTIVE, newActiveOrder.getStatus());
        assertEquals(1, newActiveOrder.getTickets().size());
        assertNotEquals(expiredOrderId, newActiveOrder.getOrderId());
    }

    private Event createActiveEvent(Long eventId) {
        Event event = new Event(
                eventId,
                LocalDateTime.now().plusDays(10),
                "Checkout Test Event",
                COMPANY_ID,
                COMPANY_FOUNDER_ID,
                EventLocation.TEL_AVIV,
                100L,
                EventCategory.CONCERT,
                "Test Artist",
                new BigDecimal("100.00"),
                new Pair<>(10, 10)
        );

        event.setStatus(Event.eventStatus.ACTIVE);

        EventMap map = new EventMap(new Pair<>(10, 10));

        StandingArea standingArea = new StandingArea(
                1L,
                "Main Standing Area",
                new Pair<>(0, 0),
                new Pair<>(5, 5),
                100
        );
                                memberToken,
                                eventId,
                                areaId,
                                2,
                                null);

                ActiveOrder order = orderRepository.getActiveOrderByUserId(memberId);

                ActiveOrderDTO dto = reservationService.viewActiveOrder(
                                memberToken,
                                order.getOrderId());

                assertNotNull(dto);
                assertEquals(order.getOrderId(), dto.getOrderId());
                assertEquals(eventId, dto.getEventId());
                assertEquals(2, dto.getTickets().size());
        }

        @Test
        void AcceptanceTest_ViewActiveOrder_WhenOrderDoesNotExist_ThenThrowException() {
                Long nonExistingOrderId = 999L;

                IllegalStateException exception = assertThrows(
                                IllegalStateException.class,
                                () -> reservationService.viewActiveOrder(memberToken, nonExistingOrderId));

                assertEquals("No active order found", exception.getMessage());
        }

        @Test
        void AcceptanceTest_Checkout_WhenPaymentAndTicketIssuingSucceed_ThenOrderIsCompletedAndBarcodeIssued() {
                Long eventId = 1L;
                Long areaId = 1L;

                Event event = createActiveEvent(eventId);
                eventRepository.addEvent(event);

                AtomicReference<OrderDTO> completedOrder = new AtomicReference<>();
                reservationService.addOrderListener(completedOrder::set);

                boolean selected = reservationService.selectStandingTicket(
                                memberToken,
                                eventId,
                                areaId,
                                1,
                                null);

                assertTrue(selected);

                boolean checkoutResult = reservationService.checkout(
                                memberToken,
                                eventId,
                                createPaymentDetails(),
                                null);

                assertTrue(checkoutResult);

                assertTrue(PaymentServiceProxy.wasPayCalled);
                assertFalse(PaymentServiceProxy.wasRefundCalled);
                assertTrue(secureBarcode.wasGenerateCalled.get());

                assertNotNull(completedOrder.get());
                assertFalse(completedOrder.get().getTickets().isEmpty());
                assertNotNull(completedOrder.get().getTickets().get(0).getSecureBarcode());
        }

        @Test
        void AcceptanceTest_Checkout_WhenPaymentFails_ThenCheckoutThrowsAndNoBarcodeIssued() {
                Long eventId = 2L;
                Long areaId = 1L;

                Event event = createActiveEvent(eventId);
                eventRepository.addEvent(event);

                reservationService.selectStandingTicket(
                                memberToken,
                                eventId,
                                areaId,
                                1,
                                null);

                PaymentServiceProxy.isPaymentSuccessful = false;

                assertThrows(
                                IllegalStateException.class,
                                () -> reservationService.checkout(
                                                memberToken,
                                                eventId,
                                                createPaymentDetails(),
                                                null));

                assertTrue(PaymentServiceProxy.wasPayCalled);
                assertFalse(PaymentServiceProxy.wasRefundCalled);
                assertFalse(secureBarcode.wasGenerateCalled.get());
        }

        @Test
        void AcceptanceTest_Checkout_WhenTicketIssuingFailsAfterPayment_ThenRefundIsCalled() {
                Long eventId = 3L;
                Long areaId = 1L;

                Event event = createActiveEvent(eventId);
                eventRepository.addEvent(event);

                reservationService.selectStandingTicket(
                                memberToken,
                                eventId,
                                areaId,
                                1,
                                null);

                secureBarcode.shouldGenerateSucceed = false;

                assertThrows(
                                IllegalStateException.class,
                                () -> reservationService.checkout(
                                                memberToken,
                                                eventId,
                                                createPaymentDetails(),
                                                null));

                assertTrue(PaymentServiceProxy.wasPayCalled);
                assertTrue(PaymentServiceProxy.wasRefundCalled);
                assertTrue(secureBarcode.wasGenerateCalled.get());
        }

        @Test
        void AcceptanceTest_GuestCheckout_WhenPaymentAndTicketIssuingSucceed_ThenOrderIsCompletedAndBarcodeIssued() {
                Long eventId = 4L;
                Long areaId = 1L;

                Event event = createActiveEvent(eventId);
                eventRepository.addEvent(event);

                AtomicReference<OrderDTO> completedOrder = new AtomicReference<>();
                reservationService.addOrderListener(completedOrder::set);

                boolean selected = reservationService.selectStandingTicket(
                                guestToken,
                                eventId,
                                areaId,
                                1,
                                null);

                assertTrue(selected);

                boolean checkoutResult = reservationService.checkout(
                                guestToken,
                                eventId,
                                createPaymentDetails(),
                                null);

                assertTrue(checkoutResult);

                assertTrue(PaymentServiceProxy.wasPayCalled);
                assertFalse(PaymentServiceProxy.wasRefundCalled);
                assertTrue(secureBarcode.wasGenerateCalled.get());

                assertNotNull(completedOrder.get());
                assertFalse(completedOrder.get().getTickets().isEmpty());
                assertNotNull(completedOrder.get().getTickets().get(0).getSecureBarcode());
        }

        @Test
        void GivenExpiredOrder_WhenSelectSeatTicket_ThenExpiredOrderIsCancelledAndNewTicketIsSelected() {
                Long eventId = 30L;
                Long areaId = 1L;

                Event event = createActiveEventWithSeatingArea(eventId);
                eventRepository.addEvent(event);

                reservationService.selectSeatTicket(
                                memberToken,
                                eventId,
                                areaId,
                                new seatPositionDTO(1, 1),
                                null);

                ActiveOrder expiredOrder = orderRepository.getActiveOrderByUserId(memberId);
                Long expiredOrderId = expiredOrder.getOrderId();

                expiredOrder.setExpiresAt(LocalDateTime.now().minusMinutes(1));
                orderRepository.updateOrder(expiredOrder);

                boolean result = reservationService.selectSeatTicket(
                                memberToken,
                                eventId,
                                areaId,
                                new seatPositionDTO(1, 2),
                                null);

                assertTrue(result);
                assertNull(orderRepository.findOrderById(expiredOrderId));

                ActiveOrder newActiveOrder = orderRepository.getActiveOrderByUserId(memberId);

                assertNotNull(newActiveOrder);
                assertEquals(ActiveOrder.OrderStatus.ACTIVE, newActiveOrder.getStatus());
                assertEquals(1, newActiveOrder.getTickets().size());
                assertNotEquals(expiredOrderId, newActiveOrder.getOrderId());
        }

        @Test
        void AcceptanceTest_ViewActiveOrder_WhenOrderBelongsToAnotherMember_ThenThrowsSecurityException() {
                Long eventId = 40L;
                Long areaId = 1L;

                Event event = createActiveEvent(eventId);
                eventRepository.addEvent(event);

                reservationService.selectStandingTicket(
                                memberToken,
                                eventId,
                                areaId,
                                1,
                                null);

                ActiveOrder order = orderRepository.getActiveOrderByUserId(memberId);

                SecurityException exception = assertThrows(
                                SecurityException.class,
                                () -> reservationService.viewActiveOrder(
                                                guestToken,
                                                order.getOrderId()));

                assertEquals("User is not allowed to view this order", exception.getMessage());
        }

        @Test
        void AcceptanceTest_SelectStandingTicket_WhenQuantityIsZero_ThenThrowsException() {
                Long eventId = 41L;
                Long areaId = 1L;

                Event event = createActiveEvent(eventId);
                eventRepository.addEvent(event);

                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> reservationService.selectStandingTicket(
                                                memberToken,
                                                eventId,
                                                areaId,
                                                0,
                                                null));

                assertEquals("Quantity must be greater than zero", exception.getMessage());
        }

        @Test
        void AcceptanceTest_SelectStandingTicket_WhenEventDoesNotExist_ThenThrowsException() {
                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> reservationService.selectStandingTicket(
                                                memberToken,
                                                9999L,
                                                1L,
                                                1,
                                                null));

                assertEquals("Event not found", exception.getMessage());
        }

        @Test
        void AcceptanceTest_Checkout_WhenPaymentDetailsMissing_ThenThrowsException() {
                Long eventId = 42L;
                Long areaId = 1L;

                Event event = createActiveEvent(eventId);
                eventRepository.addEvent(event);

                reservationService.selectStandingTicket(
                                memberToken,
                                eventId,
                                areaId,
                                1,
                                null);

                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> reservationService.checkout(
                                                memberToken,
                                                eventId,
                                                null,
                                                null));

                assertEquals("Payment details are incomplete", exception.getMessage());
        }

        @Test
        void AcceptanceTest_Checkout_WhenBirthDateMissing_ThenThrowsException() {
                Long eventId = 43L;
                Long areaId = 1L;

                Event event = createActiveEvent(eventId);
                eventRepository.addEvent(event);

                reservationService.selectStandingTicket(
                                memberToken,
                                eventId,
                                areaId,
                                1,
                                null);

                PaymentDetails invalidDetails = new PaymentDetails("VISA", "Yosi", null);

                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> reservationService.checkout(
                                                memberToken,
                                                eventId,
                                                invalidDetails,
                                                null));

                assertEquals("Payment details are incomplete", exception.getMessage());
        }

        @Test
        void AcceptanceTest_Checkout_WhenRefundFails_ThenThrowsRefundFailureException() {
                Long eventId = 44L;
                Long areaId = 1L;

                Event event = createActiveEvent(eventId);
                eventRepository.addEvent(event);

                reservationService.selectStandingTicket(
                                memberToken,
                                eventId,
                                areaId,
                                1,
                                null);

                secureBarcode.shouldGenerateSucceed = false;
                PaymentServiceProxy.isRefundSuccessful = false;

                IllegalStateException exception = assertThrows(
                                IllegalStateException.class,
                                () -> reservationService.checkout(
                                                memberToken,
                                                eventId,
                                                createPaymentDetails(),
                                                null));

                assertEquals(
                                "Ticket issuing failed and refund failed.",
                                exception.getMessage());
        }

        @Test
        void AcceptanceTest_Checkout_WhenListenerThrowsException_ThenCheckoutStillSucceeds() {
                Long eventId = 45L;
                Long areaId = 1L;

                Event event = createActiveEvent(eventId);
                eventRepository.addEvent(event);

                reservationService.addOrderListener(order -> {
                        throw new RuntimeException("Listener failed");
                });

                reservationService.selectStandingTicket(
                                memberToken,
                                eventId,
                                areaId,
                                1,
                                null);

                boolean result = reservationService.checkout(
                                memberToken,
                                eventId,
                                createPaymentDetails(),
                                null);

                assertTrue(result);
        }

        @Test
        void GivenAboutToExpireOrder_WhenAnyActionOccurs_ThenExpirationWarningNotificationIsSent() {
                Long eventId = 46L;
                Long areaId = 1L;

                Event event = createActiveEvent(eventId);
                eventRepository.addEvent(event);

                reservationService.selectStandingTicket(
                                memberToken,
                                eventId,
                                areaId,
                                1,
                                null);

                ActiveOrder order = orderRepository.getActiveOrderByUserId(memberId);

                order.setExpiresAt(LocalDateTime.now().plusMinutes(2));
                orderRepository.updateOrder(order);

                reservationService.viewActiveOrder(
                                memberToken,
                                order.getOrderId());

                assertTrue(
                                fakeNotifier.containsMessage("about to expire"));
        }

        private Event createActiveEvent(Long eventId) {
                Event event = new Event(
                                eventId,
                                LocalDateTime.now().plusDays(10),
                                "Checkout Test Event",
                                COMPANY_ID,
                                COMPANY_FOUNDER_ID,
                                EventLocation.TEL_AVIV,
                                100L,
                                EventCategory.CONCERT,
                                "Test Artist",
                                new BigDecimal("100.00"),
                                new Pair<>(10, 10));

                event.setStatus(Event.eventStatus.ACTIVE);

                EventMap map = new EventMap(new Pair<>(10, 10));

                StandingArea standingArea = new StandingArea(
                                1L,
                                "Main Standing Area",
                                new Pair<>(0, 0),
                                new Pair<>(5, 5),
                                100);

                map.addElement(standingArea);
                event.setMap(map);

                return event;
        }

        private Event createActiveEventWithSeatingArea(Long eventId) {
                Event event = new Event(
                                eventId,
                                LocalDateTime.now().plusDays(10),
                                "Seat Test Event",
                                COMPANY_ID,
                                COMPANY_FOUNDER_ID,
                                EventLocation.TEL_AVIV,
                                100L,
                                EventCategory.CONCERT,
                                "Test Artist",
                                new BigDecimal("100.00"),
                                new Pair<>(10, 10));

                event.setStatus(Event.eventStatus.ACTIVE);

                EventMap map = new EventMap(new Pair<>(10, 10));

                SeatingArea seatingArea = new SeatingArea(
                                1L,
                                "Main Seating Area",
                                new Pair<>(0, 0),
                                new Pair<>(5, 5),
                                5,
                                5);

                map.addElement(seatingArea);
                event.setMap(map);

                return event;
        }

        private PaymentDetails createPaymentDetails() {
                return new PaymentDetails("VISA", "Yosi", LocalDate.now());

        }

        private static class TestSecureBarcode implements ISecureBarcode {

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

        private static class NoOpSystemLogger implements ISystemLogger {

                @Override
                public void logEvent(String message, LogLevel level) {
                }

                @Override
                public void logError(String errorMessage, Throwable exception) {
                }
        }

        private void useGuestTokenService() {
                tokenService = new TokenService(
                                "manual_test_secret_32_chars_long",
                                new TokenRepository()) {
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

                userService = new UserService(userRepository, tokenService, logger);

                reservationService = new ReservationService(
                                orderRepository,
                                eventRepository,
                                companyRepository,
                                membershipDomain,
                                tokenService,
                                paymentService,
                                secureBarcode,
                                lotteryRepository,
                                eventCatalogDomainService,
                                logger,
                                fakeNotifier, userAccessService);
        }

        private static class FakeNotifier implements INotifier {

                private final List<String> messages = new ArrayList<>();

                @Override
                public void notifyMember(Long memberId, String message) {
                        messages.add(message);
                }

                @Override
                public void notifyGuest(String guestToken, String message) {
                        messages.add(message);
                }

                @Override
                public void notifyMembers(Collection<Long> memberIds, String message) {
                        if (memberIds == null) {
                                return;
                        }

                        for (Long memberId : memberIds) {
                                if (memberId != null) {
                                        notifyMember(memberId, message);
                                }
                        }
                }

                @Override
                public void notifyGuests(Collection<String> guestTokens, String message) {
                        if (guestTokens == null) {
                                return;
                        }

                        for (String guestToken : guestTokens) {
                                if (guestToken != null && !guestToken.isBlank()) {
                                        notifyGuest(guestToken, message);
                                }
                        }
                }

                boolean containsMessage(String text) {
                        return messages.stream()
                                        .anyMatch(message -> message.contains(text));
                }
        }
}