package ticketsystem.AcceptanceTesting;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.ApplicationLayer.INotifier;
import ticketsystem.ApplicationLayer.ISystemLogger;
import ticketsystem.ApplicationLayer.OrderService;
import ticketsystem.ApplicationLayer.TokenService;
import ticketsystem.DomainLayer.IRepository.IOrderRepository;
import ticketsystem.DomainLayer.order.ActiveOrder;
import ticketsystem.DomainLayer.order.ActiveOrder.OrderStatus;
import ticketsystem.DomainLayer.order.Ticket;
import ticketsystem.InfrastructureLayer.InMemoryNotificationsRepository;
import ticketsystem.InfrastructureLayer.InMemoryOrderRepository;
import ticketsystem.InfrastructureLayer.LogbackSystemLogger;
import ticketsystem.InfrastructureLayer.TokenRepository;
import ticketsystem.InfrastructureLayer.VaadinNotifier;

public class OrderServiceAcceptanceTest {

    private OrderService orderService;
    private IOrderRepository orderRepository;
    private TokenService tokenService;
    private ISystemLogger logger;
    private INotifier notification;

    private String guestToken;
    private String memberToken;
    private final Long userId = 1L;

    @BeforeEach
    void setUp() {
        orderRepository = new InMemoryOrderRepository();
        logger = new LogbackSystemLogger();
        notification = new VaadinNotifier(new InMemoryNotificationsRepository());

        // use real token repository + token service
        TokenRepository tokenRepository = new TokenRepository();
        tokenService = new TokenService(
                "manual_test_secret_32_chars_long",
                tokenRepository,
                logger);

        // create real user sessions
        ticketsystem.DomainLayer.user.Guest guest = new ticketsystem.DomainLayer.user.Guest();
        ticketsystem.DomainLayer.user.Member member = new ticketsystem.DomainLayer.user.Member(userId, "user", "full", "phone", LocalDate.of(2001, 1, 1));

        guestToken = tokenService.addActiveSession(guest);
        memberToken = tokenService.addActiveSession(member);

        orderService = new OrderService(
                orderRepository,
                tokenService,
                logger, notification);
    }

    @Test
    void AcceptanceTest_OnUserLogin_WhenGuestHasNoActiveOrder_ThenNothingChanges() {
        orderService.onUserLogin(guestToken, memberToken);

        ActiveOrder memberOrder = orderRepository.getActiveOrderByUserId(userId);
        ActiveOrder guestOrder = orderRepository.getActiveOrderBySessionToken(guestToken);

        assertNull(memberOrder);
        assertNull(guestOrder);
    }

    @Test
    void AcceptanceTest_OnUserLogin_WhenGuestHasOrderAndMemberHasNoOrder_ThenGuestOrderBecomesMemberOrder() {
        Long eventId = 100L;

        ActiveOrder guestOrder = new ActiveOrder(
                1L,
                guestToken,
                null,
                eventId);
        guestOrder.addTicket(new Ticket(1L, eventId, 10L, 0, 0, new BigDecimal(10))); // add a ticket to guest order to differentiate it from member order
        orderRepository.addOrder(guestOrder);

        orderService.onUserLogin(guestToken, memberToken);

        ActiveOrder updatedOrder = orderRepository.getActiveOrderByUserId(userId);

        assertNotNull(updatedOrder);
        assertEquals(userId, updatedOrder.getUserId());
        assertEquals(memberToken, updatedOrder.getSessionToken());
        assertEquals(eventId, updatedOrder.getEventId());

        assertNull(orderRepository.getActiveOrderBySessionToken(guestToken));
    }

    @Test
    void AcceptanceTest_OnUserLogin_WhenGuestAndMemberHaveOrdersForSameEvent_ThenOrdersAreMergedAndGuestOrderIsDeleted() {
        Long eventId = 200L;
        Long areaId = 10L;

        ActiveOrder guestOrder = new ActiveOrder(
                1L,
                guestToken,
                null,
                eventId);

        ActiveOrder memberOrder = new ActiveOrder(
                2L,
                memberToken,
                userId,
                eventId);
        memberOrder.addTicket(new Ticket(1L, eventId, areaId, 0, 0, new BigDecimal(10))); // add a ticket to member order to differentiate it from guest order
        orderRepository.addOrder(guestOrder);
        orderRepository.addOrder(memberOrder);

        orderService.onUserLogin(guestToken, memberToken);

        ActiveOrder updatedMemberOrder = orderRepository.getActiveOrderByUserId(userId);
        ActiveOrder deletedGuestOrder = orderRepository.getActiveOrderBySessionToken(guestToken);

        assertNotNull(updatedMemberOrder);
        assertEquals(2L, updatedMemberOrder.getOrderId());
        assertEquals(userId, updatedMemberOrder.getUserId());
        assertEquals(eventId, updatedMemberOrder.getEventId());

        assertNull(deletedGuestOrder);
    }

    @Test
    void AcceptanceTest_OnUserLogin_WhenMemberHasOrderForDifferentEvent_ThenGuestOrderIsDeletedAndMemberOrderRemains() {
        Long guestEventId = 300L;
        Long memberEventId = 400L;

        ActiveOrder guestOrder = new ActiveOrder(
                1L,
                guestToken,
                null,
                guestEventId);

        ActiveOrder memberOrder = new ActiveOrder(
                2L,
                memberToken,
                userId,
                memberEventId);
        guestOrder.addTicket(new Ticket(1L, guestEventId, 10L, 0, 0, new BigDecimal(10))); // add a ticket to guest order to differentiate it from member order
        memberOrder.addTicket(new Ticket(2L, memberEventId, 20L, 0, 0, new BigDecimal(20))); // add a ticket to member order to differentiate it from guest order
        orderRepository.addOrder(guestOrder);
        orderRepository.addOrder(memberOrder);

        orderService.onUserLogin(guestToken, memberToken);

        ActiveOrder remainingMemberOrder = orderRepository.getActiveOrderByUserId(userId);
        ActiveOrder deletedGuestOrder = orderRepository.getActiveOrderBySessionToken(guestToken);

        assertNotNull(remainingMemberOrder);
        assertEquals(2L, remainingMemberOrder.getOrderId());
        assertEquals(memberEventId, remainingMemberOrder.getEventId());
        assertEquals(OrderStatus.CANCELLED, deletedGuestOrder.getStatus());
    }

    @Test
    void AcceptanceTest_OnUserLogin_WhenTokenExtractionFails_ThenWarningIsLoggedAndExceptionIsThrown() {
        tokenService = new TokenService(
                "manual_test_secret_32_chars_long",
                new TokenRepository(), logger) {
            @Override
            public Long extractUserId(String token) {
                throw new IllegalArgumentException("Invalid member token");
            }
        };

        orderService = new OrderService(
                orderRepository,
                tokenService,
                logger,
                notification);

        ActiveOrder guestOrder = new ActiveOrder(
                1L,
                guestToken,
                null,
                500L);

        orderRepository.addOrder(guestOrder);

        assertThrows(
                IllegalArgumentException.class,
                () -> orderService.onUserLogin(guestToken, memberToken));
    }

}
