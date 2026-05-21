package ticketsystem.AcceptanceTesting;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import javax.management.Notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.ApplicationLayer.ISystemLogger;
import ticketsystem.ApplicationLayer.INotifier;
import ticketsystem.ApplicationLayer.ISystemLogger.LogLevel;
import ticketsystem.ApplicationLayer.OrderService;
import ticketsystem.ApplicationLayer.TokenService;
import ticketsystem.DomainLayer.IRepository.IOrderRepository;
import ticketsystem.DomainLayer.order.ActiveOrder;
import ticketsystem.DomainLayer.order.ActiveOrder.OrderStatus;
import ticketsystem.InfrastructureLayer.OrderRepository;
import ticketsystem.InfrastructureLayer.TokenRepository;

public class OrderServiceAcceptanceTest {

    private OrderService orderService;
    private IOrderRepository orderRepository;
    private TokenService tokenService;
    private FakeSystemLogger logger;
    private INotifier notification;

    private final String guestToken = "guest-token-1";
    private final String memberToken = "member-token-1";
    private final Long userId = 1L;

    @BeforeEach
    void setUp() {
        orderRepository = new OrderRepository();
        logger = new FakeSystemLogger();

        tokenService = new TokenService(
                "manual_test_secret_32_chars_long",
                new TokenRepository()) {
            @Override
            public boolean validateToken(String token) {
                return true;
            }

            @Override
            public Long extractUserId(String token) {
                return userId;
            }

            @Override
            public boolean isGuestToken(String token) {
                return token.startsWith("guest");
            }

            @Override
            public boolean isMemberToken(String token) {
                return token.startsWith("member");
            }
        };

        orderService = new OrderService(
                orderRepository,
                tokenService,
                logger,notification);
    }

    @Test
    void AcceptanceTest_OnUserLogin_WhenGuestHasNoActiveOrder_ThenNothingChanges() {
        orderService.onUserLogin(guestToken, memberToken);

        ActiveOrder memberOrder = orderRepository.getActiveOrderByUserId(userId);
        ActiveOrder guestOrder = orderRepository.getActiveOrderBySessionToken(guestToken);

        assertNull(memberOrder);
        assertNull(guestOrder);
        assertTrue(logger.messages.isEmpty());
    }

    @Test
    void AcceptanceTest_OnUserLogin_WhenGuestHasOrderAndMemberHasNoOrder_ThenGuestOrderBecomesMemberOrder() {
        Long eventId = 100L;

        ActiveOrder guestOrder = new ActiveOrder(
                1L,
                guestToken,
                null,
                eventId);

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
                new TokenRepository()) {
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

        assertFalse(logger.messages.isEmpty());
        assertTrue(logger.messages.get(0).contains("mergeGuestOrderIntoMemberOrders failed"));
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
            messages.add(errorMessage);
            levels.add(LogLevel.WARN);
        }
    }
}