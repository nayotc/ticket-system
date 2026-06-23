package ticketsystem.AcceptanceTesting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import ticketsystem.ApplicationLayer.INotifier;
import ticketsystem.ApplicationLayer.IPaymentService;
import ticketsystem.ApplicationLayer.ISystemLogger;
import ticketsystem.ApplicationLayer.ITicketIssuingService;
import ticketsystem.ApplicationLayer.ReservationService;
import ticketsystem.ApplicationLayer.TokenService;
import ticketsystem.ApplicationLayer.UserAccessService;
import ticketsystem.ApplicationLayer.UserService;
import ticketsystem.DTO.ActiveOrderDTO;
import ticketsystem.DTO.OrderDTO;
import ticketsystem.DTO.PaymentDetails;
import ticketsystem.DTO.TicketIssueRequest;
import ticketsystem.DTO.seatPositionDTO;
import ticketsystem.DomainLayer.EventCatalogDomainService;
import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.IRepository.IOrderRepository;
import ticketsystem.DomainLayer.IRepository.ITokenRepository;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.DomainLayer.MembershipDomainService;
import ticketsystem.DomainLayer.company.Company;
import ticketsystem.DomainLayer.discount.DiscountCompositionType;
import ticketsystem.DomainLayer.discount.DiscountPolicy;
import ticketsystem.DomainLayer.event.*;
import ticketsystem.DomainLayer.lottery.Lottery;
import ticketsystem.DomainLayer.order.ActiveOrder;
import ticketsystem.DomainLayer.policy.PurchasePolicy;
import ticketsystem.DomainLayer.user.Founder;
import ticketsystem.DomainLayer.user.Member;
import ticketsystem.DomainLayer.user.Permission;
import ticketsystem.DomainLayer.user.RoleStatus;
import ticketsystem.DomainLayer.event.Seat.SeatStatus;
import ticketsystem.InfrastructureLayer.CompanyRepository;
import ticketsystem.InfrastructureLayer.InMemoryOrderRepository;
import ticketsystem.InfrastructureLayer.InMemoryUserRepository;
import ticketsystem.InfrastructureLayer.LogbackSystemLogger;
import ticketsystem.InfrastructureLayer.LotteryRepository;
import ticketsystem.InfrastructureLayer.PaymentServiceProxy;
import ticketsystem.InfrastructureLayer.TokenRepository;
import ticketsystem.testutil.RecordingNotifier;

@SpringBootTest
@Transactional
public class ReservationServiceTest {
    private ReservationService reservationService;
    private IOrderRepository orderRepository;

    @Autowired
    private IEventRepository eventRepository;
    @Autowired
    private LotteryRepository lotteryRepository;

    @Autowired
    private CompanyRepository companyRepository;
    private IUserRepository userRepository;
    private TokenService tokenService;
    private UserService userService;
    private EventCatalogDomainService eventCatalogDomainService;
    private IPaymentService paymentService;
    private TestSecureBarcode secureBarcode;
    private ISystemLogger logger;
    private RecordingNotifier recordingNotifier;
    private INotifier notifier;
    private MembershipDomainService membershipDomain;
    private UserAccessService userAccessService;
    private String memberToken;
    private String guestToken;
    private Long memberId;

    private Long companyId;

    private static final Long COMPANY_FOUNDER_ID = 1L;

    @BeforeEach
    void setUp() {
        orderRepository = new InMemoryOrderRepository();
        userRepository = new InMemoryUserRepository();

        membershipDomain = new MembershipDomainService(userRepository);

        paymentService = new PaymentServiceProxy();
        secureBarcode = new TestSecureBarcode();
        logger = new LogbackSystemLogger();
        recordingNotifier = new RecordingNotifier();
        notifier = recordingNotifier;
        userAccessService = new UserAccessService(userRepository);

        ITokenRepository tokenRepository = new TokenRepository();

        tokenService = new TokenService(
                "manual_test_secret_32_chars_long_for_tests",
                tokenRepository,
                logger
        );

        userService = new UserService(userRepository, tokenService, logger);

        memberToken = createLoggedInMember("member_user", "password123");
        memberId = tokenService.extractUserId(memberToken);
        guestToken = userService.visitSystem();

        Company company = new Company(
                "BGU Productions",
                COMPANY_FOUNDER_ID,
                PurchasePolicy.noRestrictions(),
                new DiscountPolicy(DiscountCompositionType.MAX)
        );

        companyRepository.save(company);

        companyId = company.getId();

        assertNotNull(
                companyId,
                "The database should assign an identifier to the saved company."
        );

        assertTrue(
                companyId > 0,
                "The database-generated company identifier should be positive."
        );

        eventCatalogDomainService = new EventCatalogDomainService(
                companyRepository);

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
                notifier,
                userAccessService
        );
    }

    private String createLoggedInMember(String username, String password) {
        String guest = userService.visitSystem();

        boolean signedUp = userService.signUp(
                guest,
                username,
                password,
                "Test User",
                "0500000000",
                LocalDate.of(2001, 1, 1)
        );
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
    void GivenCheckoutCompletesLastAvailableTicket_WhenEventBecomesSoldOut_ThenOwnerAndManagerAreNotified() {
        Long ownerId = 2001L;
        Long managerId = 2002L;

        addActiveOwnerAndManagerUnderCompanyFounder(ownerId, managerId);

        Event event = createActiveEventWithSingleStandingTicket();
        eventRepository.addEvent(event);
        Long eventId = event.getId();
        Long areaId = getStandingAreaId(eventId);

        reservationService.selectStandingTicket(
                memberToken,
                eventId,
                areaId,
                1,
                null
        );

        boolean checkoutResult = reservationService.checkout(
                memberToken,
                eventId,
                createPaymentDetails(),
                null
        );

        assertTrue(checkoutResult);
        recordingNotifier.assertNotifiedMember(ownerId, "sold");
        recordingNotifier.assertNotifiedMember(managerId, "sold");
    }

    @Test
    void AcceptanceTest_SelectStandingTicket_WhenEventIsRegular_ThenTicketIsSelectedWithoutLotteryCode() {
        Event event = createActiveEvent();
        eventRepository.addEvent(event);

        Long eventId = event.getId();
        Long areaId = getStandingAreaId(eventId);

        boolean result = reservationService.selectStandingTicket(
                memberToken,
                eventId,
                areaId,
                1,
                null
        );

        assertTrue(result);
        assertStandingSelectionStored(eventId, areaId, 1);
    }

    @Test
    void AcceptanceTest_SelectStandingTicket_WhenLotteryEventAndValidCode_ThenTicketIsSelected() {
        String lotteryCode = "ABC12345";

        Event event = createActiveEvent();
        event.setSaleStatus(SaleStatus.PRE_SALE);
        eventRepository.addEvent(event);

        Long eventId = event.getId();
        Long areaId = getStandingAreaId(eventId);

        Lottery lottery = new Lottery(eventId, 1);
        lottery.registerMember(memberId);
        lottery.setWinner(memberId, lotteryCode);
        lotteryRepository.addLottery(lottery);

        boolean result = reservationService.selectStandingTicket(
                memberToken,
                eventId,
                areaId,
                1,
                lotteryCode
        );

        assertTrue(result);
        assertStandingSelectionStored(eventId, areaId, 1);
    }

    @Test
    void AcceptanceTest_SelectStandingTicket_WhenLotteryEventAndMissingCode_ThenSelectionFails() {
        Event event = createActiveEvent();
        event.setSaleStatus(SaleStatus.PRE_SALE);
        eventRepository.addEvent(event);

        Long eventId = event.getId();
        Long areaId = getStandingAreaId(eventId);

        Lottery lottery = new Lottery(eventId, 1);
        lottery.registerMember(memberId);
        lottery.setWinner(memberId, "ABC12345");
        lotteryRepository.addLottery(lottery);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reservationService.selectStandingTicket(
                        memberToken,
                        eventId,
                        areaId,
                        1,
                        null
                )
        );

        assertEquals(
                "Lottery code is required for this event",
                exception.getMessage()
        );

        assertNoMemberOrderAndStandingInventoryUnchanged(
                eventId,
                areaId
        );
    }

    @Test
    void AcceptanceTest_SelectStandingTicket_WhenLotteryEventAndWrongCode_ThenSelectionFails() {
        Event event = createActiveEvent();
        event.setSaleStatus(SaleStatus.PRE_SALE);
        eventRepository.addEvent(event);

        Long eventId = event.getId();
        Long areaId = getStandingAreaId(eventId);

        Lottery lottery = new Lottery(eventId, 1);
        lottery.registerMember(memberId);
        lottery.setWinner(memberId, "ABC12345");
        lotteryRepository.addLottery(lottery);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reservationService.selectStandingTicket(
                        memberToken,
                        eventId,
                        areaId,
                        1,
                        "WRONGCODE"
                )
        );

        assertEquals(
                "Invalid lottery code",
                exception.getMessage()
        );

        assertNoMemberOrderAndStandingInventoryUnchanged(
                eventId,
                areaId
        );
    }

    @Test
    void AcceptanceTest_SelectStandingTicket_WhenLotteryEventAndUserDidNotWin_ThenSelectionFails() {
        Long winningUserId = 999L;
        String winnerCode = "ABC12345";

        Event event = createActiveEvent();
        event.setSaleStatus(SaleStatus.PRE_SALE);
        eventRepository.addEvent(event);

        Long eventId = event.getId();
        Long areaId = getStandingAreaId(eventId);

        Lottery lottery = new Lottery(eventId, 1);

        /*
         * The current member must be registered so the test reaches
         * the winner validation instead of failing because the user
         * was not registered.
         */
        lottery.registerMember(memberId);
        lottery.registerMember(winningUserId);
        lottery.setWinner(winningUserId, winnerCode);
        lotteryRepository.addLottery(lottery);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reservationService.selectStandingTicket(
                        memberToken,
                        eventId,
                        areaId,
                        1,
                        winnerCode
                )
        );

        assertEquals(
                "Invalid lottery code",
                exception.getMessage()
        );

        assertNoMemberOrderAndStandingInventoryUnchanged(
                eventId,
                areaId
        );
    }

    @Test
    void AcceptanceTest_SelectSeatTicket_WhenEventIsRegular_ThenTicketIsSelectedWithoutLotteryCode() {
        Event event = createActiveEventWithSeatingArea();
        eventRepository.addEvent(event);

        Long eventId = event.getId();
        Long areaId = getSeatingAreaId(eventId);

        boolean result = reservationService.selectSeatTicket(
                memberToken,
                eventId,
                areaId,
                new seatPositionDTO(1, 1),
                null
        );

        assertTrue(result);
        assertSeatSelectionStored(eventId, areaId, 1, 1);
    }

    @Test
    void AcceptanceTest_SelectSeatTicket_WhenLotteryEventAndValidCode_ThenTicketIsSelected() {
        String lotteryCode = "ABC12345";

        Event event = createActiveEventWithSeatingArea();
        event.setSaleStatus(SaleStatus.PRE_SALE);
        eventRepository.addEvent(event);

        Long eventId = event.getId();
        Long areaId = getSeatingAreaId(eventId);

        Lottery lottery = new Lottery(eventId, 1);
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
        assertSeatSelectionStored(eventId, areaId, 1, 1);
    }

    @Test
    void AcceptanceTest_SelectSeatTicket_WhenLotteryEventAndMissingCode_ThenSelectionFails() {
        Event event = createActiveEventWithSeatingArea();
        event.setSaleStatus(SaleStatus.PRE_SALE);
        eventRepository.addEvent(event);

        Long eventId = event.getId();
        Long areaId = getSeatingAreaId(eventId);

        Lottery lottery = new Lottery(eventId, 1);
        lottery.registerMember(memberId);
        lottery.setWinner(memberId, "ABC12345");
        lotteryRepository.addLottery(lottery);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reservationService.selectSeatTicket(
                        memberToken,
                        eventId,
                        areaId,
                        new seatPositionDTO(1, 1),
                        null
                )
        );

        assertEquals(
                "Lottery code is required for this event",
                exception.getMessage()
        );

        assertNoMemberOrderAndSeatAvailable(
                eventId,
                areaId,
                1,
                1
        );
    }

    @Test
    void AcceptanceTest_SelectSeatTicket_WhenLotteryEventAndWrongCode_ThenSelectionFails() {
        Event event = createActiveEventWithSeatingArea();
        event.setSaleStatus(SaleStatus.PRE_SALE);
        eventRepository.addEvent(event);

        Long eventId = event.getId();
        Long areaId = getSeatingAreaId(eventId);

        Lottery lottery = new Lottery(eventId, 1);
        lottery.registerMember(memberId);
        lottery.setWinner(memberId, "ABC12345");
        lotteryRepository.addLottery(lottery);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reservationService.selectSeatTicket(
                        memberToken,
                        eventId,
                        areaId,
                        new seatPositionDTO(1, 1),
                        "WRONGCODE"
                )
        );

        assertEquals(
                "Invalid lottery code",
                exception.getMessage()
        );

        assertNoMemberOrderAndSeatAvailable(
                eventId,
                areaId,
                1,
                1
        );
    }

    @Test
    void AcceptanceTest_SelectSeatTicket_WhenLotteryEventAndUserDidNotWin_ThenSelectionFails() {
        Long winningUserId = 999L;
        String winnerCode = "ABC12345";

        Event event = createActiveEventWithSeatingArea();
        event.setSaleStatus(SaleStatus.PRE_SALE);
        eventRepository.addEvent(event);

        Long eventId = event.getId();
        Long areaId = getSeatingAreaId(eventId);

        Lottery lottery = new Lottery(eventId, 1);
        lottery.registerMember(memberId);
        lottery.registerMember(winningUserId);
        lottery.setWinner(winningUserId, winnerCode);
        lotteryRepository.addLottery(lottery);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reservationService.selectSeatTicket(
                        memberToken,
                        eventId,
                        areaId,
                        new seatPositionDTO(1, 1),
                        winnerCode
                )
        );

        assertEquals(
                "Invalid lottery code",
                exception.getMessage()
        );

        assertNoMemberOrderAndSeatAvailable(
                eventId,
                areaId,
                1,
                1
        );
    }

    @Test
    void AcceptanceTest_RemoveTicketFromActiveOrder_WhenSeatTicketExists_ThenTicketIsRemoved() {
        Event event = createActiveEventWithSeatingArea();
        eventRepository.addEvent(event);
        Long eventId = event.getId();
        Long areaId = getSeatingAreaId(eventId);

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
        assertNull(updatedOrder);
    }

    @Test
    void AcceptanceTest_RemoveTicketFromActiveOrder_WhenNoActiveOrderExists_ThenThrowException() {
        Long ticketId = 1L;

        Event event = createActiveEventWithSeatingArea();
        eventRepository.addEvent(event);
        Long eventId = event.getId();

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
        Event event = createActiveEvent();
        eventRepository.addEvent(event);
        Long eventId = event.getId();
        Long areaId = getStandingAreaId(eventId);

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
        Event event = createActiveEvent();
        eventRepository.addEvent(event);
        Long eventId = event.getId();
        Long areaId = getStandingAreaId(eventId);

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
        Event event = createActiveEvent();
        eventRepository.addEvent(event);
        Long eventId = event.getId();
        Long areaId = getStandingAreaId(eventId);

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
        Event event = createActiveEvent();
        eventRepository.addEvent(event);

        Long eventId = event.getId();
        Long areaId = getStandingAreaId(eventId);

        AtomicReference<OrderDTO> completedOrder =
                new AtomicReference<>();

        reservationService.addOrderListener(
                completedOrder::set
        );

        boolean selected =
                reservationService.selectStandingTicket(
                        memberToken,
                        eventId,
                        areaId,
                        1,
                        null
                );

        assertTrue(selected);

        ActiveOrder orderBeforeCheckout =
                orderRepository.getActiveOrderByUserId(memberId);

        assertNotNull(orderBeforeCheckout);
        Long orderId = orderBeforeCheckout.getOrderId();

        reservationService.validateActiveOrderPolicy(
                memberToken,
                eventId,
                createPaymentDetails(),
                null
        );

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
        assertNotNull(
                completedOrder.get()
                        .getTickets()
                        .get(0)
                        .getSecureBarcode()
        );

        assertNull(
                orderRepository.findOrderById(orderId),
                "Completed active order should be removed"
        );

        assertNull(
                orderRepository.getActiveOrderByUserId(memberId),
                "The member should no longer have an active order"
        );

        StandingArea storedArea =
                getStoredStandingArea(eventId, areaId);

        assertEquals(
                0,
                storedArea.getReserved(),
                "The ticket should no longer be reserved after checkout"
        );

        assertEquals(
                1,
                storedArea.getSold(),
                "The ticket should be marked as sold"
        );
    }

    @Test
    void AcceptanceTest_Checkout_WhenPaymentFails_ThenCheckoutThrowsAndNoBarcodeIssued() {
        Event event = createActiveEvent();
        eventRepository.addEvent(event);
        Long eventId = event.getId();
        Long areaId = getStandingAreaId(eventId);

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
        recordingNotifier.assertNotifiedMember(memberId, "Payment failed");
        recordingNotifier.assertNotificationCount(1);
    }

    @Test
    void AcceptanceTest_Checkout_WhenTicketIssuingFailsAfterPayment_ThenRefundIsCalled() {
        Event event = createActiveEvent();
        eventRepository.addEvent(event);
        Long eventId = event.getId();
        Long areaId = getStandingAreaId(eventId);

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
        recordingNotifier.assertNotifiedMember(memberId, "refund was issued");
        recordingNotifier.assertNotificationCount(1);
    }

    @Test
    void AcceptanceTest_GuestCheckout_WhenPaymentAndTicketIssuingSucceed_ThenOrderIsCompletedAndBarcodeIssued() {
        Event event = createActiveEvent();
        eventRepository.addEvent(event);

        Long eventId = event.getId();
        Long areaId = getStandingAreaId(eventId);

        AtomicReference<OrderDTO> completedOrder =
                new AtomicReference<>();

        reservationService.addOrderListener(
                completedOrder::set
        );

        boolean selected =
                reservationService.selectStandingTicket(
                        guestToken,
                        eventId,
                        areaId,
                        1,
                        null
                );

        assertTrue(selected);

        ActiveOrder orderBeforeCheckout =
                orderRepository.getActiveOrderBySessionToken(guestToken);

        assertNotNull(orderBeforeCheckout);
        Long orderId = orderBeforeCheckout.getOrderId();

        reservationService.validateActiveOrderPolicy(
                guestToken,
                eventId,
                createPaymentDetails(),
                null
        );

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
        assertNotNull(
                completedOrder.get()
                        .getTickets()
                        .get(0)
                        .getSecureBarcode()
        );

        assertNull(
                orderRepository.findOrderById(orderId),
                "Completed active order should be removed"
        );

        assertNull(
                orderRepository.getActiveOrderBySessionToken(guestToken),
                "The guest should no longer have an active order"
        );

        StandingArea storedArea =
                getStoredStandingArea(eventId, areaId);

        assertEquals(0, storedArea.getReserved());
        assertEquals(1, storedArea.getSold());
    }

    @Test
    void AcceptanceTest_GuestCheckout_WhenPaymentFails_ThenUserIsNotified() {
        Event event = createActiveEvent();
        eventRepository.addEvent(event);
        Long eventId = event.getId();
        Long areaId = getStandingAreaId(eventId);

        reservationService.selectStandingTicket(
                guestToken,
                eventId,
                areaId,
                1,
                null
        );

        PaymentServiceProxy.isPaymentSuccessful = false;

        assertThrows(
                IllegalStateException.class,
                () -> reservationService.checkout(
                        guestToken,
                        eventId,
                        createPaymentDetails(),
                        null
                )
        );

        recordingNotifier.assertNotifiedGuest(guestToken, "Payment failed");
        recordingNotifier.assertNotificationCount(1);
    }

    @Test
    void GivenExpiredOrder_WhenSelectSeatTicket_ThenExpiredOrderIsCancelledAndNewTicketIsSelected() {
        Event event = createActiveEventWithSeatingArea();
        eventRepository.addEvent(event);
        Long eventId = event.getId();
        Long areaId = getSeatingAreaId(eventId);

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

        reservationService.sweepExpiredAndExpiringOrders();

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

    @Test
    void AcceptanceTest_ViewActiveOrder_WhenOrderBelongsToAnotherMember_ThenThrowsSecurityException() {
        Event event = createActiveEvent();
        eventRepository.addEvent(event);
        Long eventId = event.getId();
        Long areaId = getStandingAreaId(eventId);

        reservationService.selectStandingTicket(
                memberToken,
                eventId,
                areaId,
                1,
                null
        );

        ActiveOrder order = orderRepository.getActiveOrderByUserId(memberId);

        SecurityException exception = assertThrows(
                SecurityException.class,
                () -> reservationService.viewActiveOrder(
                        guestToken,
                        order.getOrderId()
                )
        );

        assertEquals("User is not allowed to view this order", exception.getMessage());
    }

    @Test
    void AcceptanceTest_SelectStandingTicket_WhenQuantityIsZero_ThenThrowsException() {
        Event event = createActiveEvent();
        eventRepository.addEvent(event);

        Long eventId = event.getId();
        Long areaId = getStandingAreaId(eventId);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reservationService.selectStandingTicket(
                        memberToken,
                        eventId,
                        areaId,
                        0,
                        null
                )
        );

        assertEquals(
                "Quantity must be greater than zero",
                exception.getMessage()
        );

        assertNoMemberOrderAndStandingInventoryUnchanged(
                eventId,
                areaId
        );
    }

    @Test
    void AcceptanceTest_SelectStandingTicket_WhenEventDoesNotExist_ThenThrowsException() {
        Long nonexistentEventId = Long.MAX_VALUE;

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reservationService.selectStandingTicket(
                        memberToken,
                        nonexistentEventId,
                        1L,
                        1,
                        null
                )
        );

        assertEquals("Event not found", exception.getMessage());

        assertNull(
                orderRepository.getActiveOrderByUserId(memberId),
                "A missing event must not create an active order"
        );
    }

    @Test
    void AcceptanceTest_Checkout_WhenPaymentDetailsMissing_ThenThrowsException() {
        Event event = createActiveEvent();
        eventRepository.addEvent(event);
        Long eventId = event.getId();
        Long areaId = getStandingAreaId(eventId);

        reservationService.selectStandingTicket(
                memberToken,
                eventId,
                areaId,
                1,
                null
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reservationService.checkout(
                        memberToken,
                        eventId,
                        null,
                        null
                )
        );

        assertEquals("Payment details are incomplete", exception.getMessage());
    }

    @Test
    void AcceptanceTest_Checkout_WhenBirthDateMissing_ThenThrowsException() {
        Event event = createActiveEvent();
        eventRepository.addEvent(event);
        Long eventId = event.getId();
        Long areaId = getStandingAreaId(eventId);

        reservationService.selectStandingTicket(
                memberToken,
                eventId,
                areaId,
                1,
                null
        );

        PaymentDetails invalidDetails = new PaymentDetails(
                "VISA",
                "Yosi Cohen",
                null,
                null,
                12,
                2030,
                null,
                "123456789",
                "ILS"
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reservationService.checkout(
                        memberToken,
                        eventId,
                        invalidDetails,
                        null
                )
        );

        assertEquals("Payment details are incomplete", exception.getMessage());
    }

    @Test
    void AcceptanceTest_Checkout_WhenRefundFails_ThenThrowsRefundFailureException() {
        Event event = createActiveEvent();
        eventRepository.addEvent(event);
        Long eventId = event.getId();
        Long areaId = getStandingAreaId(eventId);

        reservationService.selectStandingTicket(
                memberToken,
                eventId,
                areaId,
                1,
                null
        );

        secureBarcode.shouldGenerateSucceed = false;
        PaymentServiceProxy.isRefundSuccessful = false;

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> reservationService.checkout(
                        memberToken,
                        eventId,
                        createPaymentDetails(),
                        null
                )
        );

        assertEquals(
                "Ticket issuing failed and refund failed.",
                exception.getMessage()
        );
    }

    @Test
    void AcceptanceTest_Checkout_WhenListenerThrowsException_ThenCheckoutStillSucceeds() {
        Event event = createActiveEvent();
        eventRepository.addEvent(event);
        Long eventId = event.getId();
        Long areaId = getStandingAreaId(eventId);

        reservationService.addOrderListener(order -> {
            throw new RuntimeException("Listener failed");
        });

        reservationService.selectStandingTicket(
                memberToken,
                eventId,
                areaId,
                1,
                null
        );

        reservationService.validateActiveOrderPolicy(
                memberToken,
                eventId,
                createPaymentDetails(),
                null
        );

        boolean result = reservationService.checkout(
                memberToken,
                eventId,
                createPaymentDetails(),
                null
        );

        assertTrue(result);
    }

    @Test
    void GivenAboutToExpireOrder_WhenAnyActionOccurs_ThenExpirationWarningNotificationIsSent() {
        Event event = createActiveEvent();
        eventRepository.addEvent(event);
        Long eventId = event.getId();
        Long areaId = getStandingAreaId(eventId);

        reservationService.selectStandingTicket(
                memberToken,
                eventId,
                areaId,
                1,
                null
        );

        ActiveOrder order = orderRepository.getActiveOrderByUserId(memberId);
        order.setExpiresAt(LocalDateTime.now().plusMinutes(2));
       
        orderRepository.updateOrder(order);
        reservationService.sweepExpiredAndExpiringOrders();
        reservationService.viewActiveOrder(
                memberToken,
                order.getOrderId()
        );

        recordingNotifier.assertNotifiedMember(memberId, "about to expire");
        recordingNotifier.assertNotificationCount(1);
    }

    @Test
    void TestGuestOrderAboutToExpire_ThenUserIsNotified() {
        Event event = createActiveEvent();
        eventRepository.addEvent(event);
        Long eventId = event.getId();
        Long areaId = getStandingAreaId(eventId);

        reservationService.selectStandingTicket(
                guestToken,
                eventId,
                areaId,
                1,
                null
        );

        ActiveOrder order = orderRepository.getActiveOrderBySessionToken(guestToken);
        order.setExpiresAt(LocalDateTime.now().plusMinutes(2));
        orderRepository.updateOrder(order);

        reservationService.viewActiveOrder(
                guestToken,
                order.getOrderId()
        );
        reservationService.sweepExpiredAndExpiringOrders();
        recordingNotifier.assertNotifiedGuest(guestToken, "about to expire");
        recordingNotifier.assertNotificationCount(1);
    }

    private void addActiveOwnerAndManagerUnderCompanyFounder(Long ownerId, Long managerId) {
        Member founder = userRepository.getMemberById(COMPANY_FOUNDER_ID);
        assertNotNull(founder, "Founder member must exist in test setup");

        if (founder.getRoleInCompany(companyId) == null) {
                founder.addFounderRole(companyId);
        }

        Founder founderRole = (Founder) founder.getRoleInCompany(companyId);

        Member owner = new Member(
                ownerId,
                "soldOutOwner",
                "Sold Out Owner",
                "0501111111",
                LocalDate.of(2001, 1, 1)
        );

        owner.addOwnerRole(companyId, COMPANY_FOUNDER_ID);
        owner.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        userRepository.addRegisteredMember(ownerId, owner, "password123");

        Set<Permission> managerPermissions = new HashSet<>();
        managerPermissions.add(Permission.MANAGE_EVENT_INVENTORY);

        Member manager = new Member(
                managerId,
                "soldOutManager",
                "Sold Out Manager",
                "0502222222",
                LocalDate.of(2001, 1, 1)
        );

        manager.addManagerRole(companyId, COMPANY_FOUNDER_ID, managerPermissions);
        manager.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        userRepository.addRegisteredMember(managerId, manager, "password123");

        founderRole.addAppointee(ownerId);
        founderRole.addAppointee(managerId);
        userRepository.updateMember(founder);
        }
        private Event createActiveEventWithSingleStandingTicket() {
            Event event = new Event(
            LocalDateTime.now().plusDays(10),
            "Sold Out Test Event",
            companyId,
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
                "Single Ticket Standing Area",
                new Pair<>(0, 0),
                new Pair<>(5, 5),
                1
        );

        map.addElement(standingArea);
        event.setMap(map);

        return event;
    }

    private Event createActiveEvent() {
        Event event = new Event(
            LocalDateTime.now().plusDays(10),
            "Checkout Test Event",
            companyId,
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
                "Main Standing Area",
                new Pair<>(0, 0),
                new Pair<>(5, 5),
                100
        );

        map.addElement(standingArea);
        event.setMap(map);

        return event;
    }

    private Event createActiveEventWithSeatingArea() {
        Event event = new Event(
                LocalDateTime.now().plusDays(10),
                "Seat Test Event",
                companyId,
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
        SeatingArea seatingArea = new SeatingArea(
                "Main Seating Area",
                new Pair<>(0, 0),
                new Pair<>(5, 5),
                5,
                5
        );

        map.addElement(seatingArea);
        event.setMap(map);

        return event;
    }

    private Long getStandingAreaId(Long eventId) {
        Event savedEvent = eventRepository.getEventById(eventId);

        StandingArea area = savedEvent.getMap()
                .getElements()
                .stream()
                .filter(StandingArea.class::isInstance)
                .map(StandingArea.class::cast)
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException(
                                "Standing area was not found"
                        )
                );

        assertNotNull(
                area.getId(),
                "Persisted standing area must have a generated ID"
        );

        return area.getId();
    }

    private Long getSeatingAreaId(Long eventId) {
        Event savedEvent = eventRepository.getEventById(eventId);

        SeatingArea area = savedEvent.getMap()
                .getElements()
                .stream()
                .filter(SeatingArea.class::isInstance)
                .map(SeatingArea.class::cast)
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException(
                                "Seating area was not found"
                        )
                );

        assertNotNull(
                area.getId(),
                "Persisted seating area must have a generated ID"
        );

        return area.getId();
    }

    private StandingArea getStoredStandingArea(Long eventId, Long areaId) {
        Event storedEvent = eventRepository.getEventById(eventId);

        assertNotNull(storedEvent, "Event should exist in the repository");

        return storedEvent.getMap()
                .getElements()
                .stream()
                .filter(StandingArea.class::isInstance)
                .map(StandingArea.class::cast)
                .filter(area -> areaId.equals(area.getId()))
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException(
                                "Standing area was not found"
                        )
                );
    }

    private void assertStandingSelectionStored(
            Long eventId,
            Long areaId,
            int expectedQuantity
    ) {
        ActiveOrder order =
                orderRepository.getActiveOrderByUserId(memberId);

        assertNotNull(order, "An active order should be created");
        assertEquals(eventId, order.getEventId());
        assertEquals(expectedQuantity, order.getTickets().size());

        assertTrue(
                order.getTickets().stream().allMatch(ticket ->
                        eventId.equals(ticket.getEventId())
                                && areaId.equals(ticket.getAreaId())
                                && ticket.getRow() == 0
                                && ticket.getChair() == 0
                ),
                "All stored tickets should belong to the selected standing area"
        );

        StandingArea storedArea =
                getStoredStandingArea(eventId, areaId);

        assertEquals(
                expectedQuantity,
                storedArea.getReserved(),
                "The selected standing tickets should be reserved"
        );
    }

    private void assertSeatSelectionStored(
            Long eventId,
            Long areaId,
            int row,
            int chair
    ) {
        ActiveOrder order =
                orderRepository.getActiveOrderByUserId(memberId);

        assertNotNull(order, "An active order should be created");
        assertEquals(eventId, order.getEventId());
        assertEquals(1, order.getTickets().size());

        var selectedTicket = order.getTickets().get(0);

        assertEquals(eventId, selectedTicket.getEventId());
        assertEquals(areaId, selectedTicket.getAreaId());
        assertEquals(row, selectedTicket.getRow());
        assertEquals(chair, selectedTicket.getChair());

        Event storedEvent = eventRepository.getEventById(eventId);

        assertNotNull(storedEvent);
        assertEquals(
                SeatStatus.RESERVED,
                storedEvent.getSeatStatus(
                        areaId,
                        new SeatPosition(row, chair)
                ),
                "The selected seat should be reserved"
        );
    }

    private void assertNoMemberOrderAndStandingInventoryUnchanged(
            Long eventId,
            Long areaId
    ) {
        assertNull(
                orderRepository.getActiveOrderByUserId(memberId),
                "A failed selection must not create an active order"
        );

        StandingArea storedArea =
                getStoredStandingArea(eventId, areaId);

        assertEquals(
                0,
                storedArea.getReserved(),
                "A failed selection must not reserve standing tickets"
        );
    }

    private void assertNoMemberOrderAndSeatAvailable(
            Long eventId,
            Long areaId,
            int row,
            int chair
    ) {
        assertNull(
                orderRepository.getActiveOrderByUserId(memberId),
                "A failed selection must not create an active order"
        );

        Event storedEvent = eventRepository.getEventById(eventId);

        assertNotNull(storedEvent);
        assertEquals(
                SeatStatus.AVAILABLE,
                storedEvent.getSeatStatus(
                        areaId,
                        new SeatPosition(row, chair)
                ),
                "A failed selection must not reserve the seat"
        );
    }

    private PaymentDetails createPaymentDetails() {
        return new PaymentDetails(
            "VISA",
            "Yosi Cohen",
            LocalDate.of(2001, 1, 1),
            "4580458045804580",
            12,
            2030,
            "123",
            "123456789",
            "ILS"
        );
    }

    private static class TestSecureBarcode implements ITicketIssuingService {

        boolean shouldGenerateSucceed = true;
        AtomicBoolean wasGenerateCalled = new AtomicBoolean(false);

        @Override
        public boolean handshake() {
            return true;
        }

        @Override
        public String issueTicket(TicketIssueRequest request) {
            wasGenerateCalled.set(true);

            if (!shouldGenerateSucceed) {
                throw new IllegalStateException("Ticket issuing failed");
            }

            return "SECURE_BARCODE_" + request.getCustomerId() + "_" + request.getEventId();
        }

        @Override
        public boolean cancelTicket(String ticketId) {
            return true;
        }
    }

    private void useGuestTokenService() {
        tokenService = new TokenService(
                "manual_test_secret_32_chars_long",
                new TokenRepository(),
                logger
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
                notifier,
                userAccessService
        );
    }
}
