package ticketsystem.IntegrationTesting;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.ApplicationLayer.INotifier;
import ticketsystem.ApplicationLayer.OrderService;
import ticketsystem.ApplicationLayer.TokenService;
import ticketsystem.ApplicationLayer.UserService;
import ticketsystem.DomainLayer.IRepository.IOrderRepository;
import ticketsystem.DomainLayer.IRepository.ITokenRepository;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.DomainLayer.order.ActiveOrder;
import ticketsystem.DomainLayer.order.ActiveOrder.OrderStatus;
import ticketsystem.DomainLayer.order.Ticket;
import ticketsystem.DomainLayer.user.Member;
import ticketsystem.InfrastructureLayer.InMemoryOrderRepository;
import ticketsystem.InfrastructureLayer.InMemoryUserRepository;
import ticketsystem.InfrastructureLayer.LogbackSystemLogger;
import ticketsystem.InfrastructureLayer.TokenRepository;

public class UserOrderIntegrationTests {

    private IUserRepository userRepository;
    private UserService userService;
    private TokenService tokenService;
    private ITokenRepository tokenRepository;
    private LogbackSystemLogger logger;
    private IOrderRepository orderRepository;
    private OrderService orderService;
    private INotifier notification;

    @BeforeEach
    public void setup() {
        logger = new LogbackSystemLogger();
        userRepository = new InMemoryUserRepository();
        tokenRepository = new TokenRepository();
        tokenService = new TokenService("manual_test_secret_32_chars_long", tokenRepository, logger);
        userService = new UserService(userRepository, tokenService, logger);
        orderRepository = new InMemoryOrderRepository();
        orderService = new OrderService(orderRepository, tokenService, logger, notification);
        userService.addUserLoginListener(orderService);
    }

    @Test
    public void testUserLogin_WhenNeitherGuestNorMemberHasOrder_ThenNoActiveOrder() {
        // Arrange - visit → sign up
        String guestToken = userService.visitSystem();
        userService.signUp(
                guestToken,
                "member",
                "password",
                "Test User",
                "0500000000",LocalDate.of(2001, 1, 1)
        );
        String memberToken = userService.login(guestToken, "member", "password");
        // doesn't have any orders
        guestToken = userService.logOut(memberToken);
        // Act - login again
        userService.login(guestToken, "member", "password");
        // Assert - no active orders
        assertNull(orderRepository.getActiveOrderByUserId(userRepository.getMemberByUsername("member").getId()));
        assertNull(orderRepository.getActiveOrderBySessionToken(guestToken));
    }

    @Test
    public void testUserLogin_WhenGuestHasOrderAndMemberHasNoOrder_ThenGuestOrderBecomesMemberOrder() {
        // Arrange - visit → sign up
        String guestToken = userService.visitSystem();
        userService.signUp(
                guestToken,
                "member",
                "password",
                "Test User",
                "0500000000",LocalDate.of(2001, 1, 1)
        );
        String memberToken = userService.login(guestToken, "member", "password");
        Member member = userRepository.getMemberByUsername("member");
        // doesn't have any orders
        guestToken = userService.logOut(memberToken);
        ActiveOrder GuestOrder = new ActiveOrder(
                orderRepository.getNextId(),
                guestToken,
                null,
                100L);
        Ticket ticket = new Ticket(
        1L,
        100L,
        10L,
        1, // row
        1, // chair
        BigDecimal.valueOf(100)
);

GuestOrder.addTicket(ticket);
        orderRepository.addOrder(GuestOrder);
        // Act - login again
        userService.login(guestToken, "member", "password");
        // Assert - guest order becomes member order
        assertNotNull(orderRepository.getActiveOrderByUserId(member.getId()));
        assertEquals(GuestOrder.getOrderId(), orderRepository.getActiveOrderByUserId(member.getId()).getOrderId());
        assertNull(orderRepository.getActiveOrderBySessionToken(guestToken));
    }

    @Test
    public void testUserLogin_WhenGuestHasNoOrderAndMemberHasOrder_ThenMemberOrderUnchanged() {
        // Arrange - visit → sign up
        String guestToken = userService.visitSystem();
        userService.signUp(
                guestToken,
                "member",
                "password",
                "Test User",
                "0500000000",LocalDate.of(2001, 1, 1)
        );
        String memberToken = userService.login(guestToken, "member", "password");
        Member member = userRepository.getMemberByUsername("member");
        ActiveOrder memberOrder = new ActiveOrder(
                orderRepository.getNextId(),
                memberToken,
                member.getId(),
                200L);
        orderRepository.addOrder(memberOrder);
        guestToken = userService.logOut(memberToken);
        // Act - login again
        userService.login(guestToken, "member", "password");
        // Assert - member order remains unchanged
        assertNotNull(orderRepository.getActiveOrderByUserId(member.getId()));
        assertEquals(memberOrder.getOrderId(), orderRepository.getActiveOrderByUserId(member.getId()).getOrderId());
        assertNull(orderRepository.getActiveOrderBySessionToken(guestToken));
    }

    @Test
    public void testUserLogin_WhenGuestAndMemberHaveOrdersForSameEvent_ThenOrdersMergedAndGuestRemoved() {
        // Arrange - visit → sign up
        String guestToken = userService.visitSystem();
        userService.signUp(
                guestToken,
                "member",
                "password",
                "Test User",
                "0500000000",LocalDate.of(2001, 1, 1)
        );
        String memberToken = userService.login(guestToken, "member", "password");
        Member member = userRepository.getMemberByUsername("member");

        ActiveOrder memberOrder = new ActiveOrder(
                orderRepository.getNextId(),
                memberToken,
                member.getId(),
                100L);
        memberOrder.addTicket(new Ticket(1L, 100L, 1L, 0, 0, BigDecimal.TEN));
        orderRepository.addOrder(memberOrder);
        guestToken = userService.logOut(memberToken);
        ActiveOrder guestOrder = new ActiveOrder(
                orderRepository.getNextId(),
                guestToken,
                null, 100L);
        guestOrder.addTicket(new Ticket(2L, 100L, 1L, 0, 0, BigDecimal.TEN));
        orderRepository.addOrder(guestOrder);
        // Act - login again
        userService.login(guestToken, "member", "password");
        // Assert - orders merged and guest removed
        assertNotNull(orderRepository.getActiveOrderByUserId(member.getId()));
        assertNull(orderRepository.getActiveOrderBySessionToken(guestToken));
        assertNull(orderRepository.findOrderById(guestOrder.getOrderId()));
        assertEquals(2, orderRepository.getActiveOrderByUserId(member.getId()).getTickets().size());
    }

  @Test
public void testUserLogin_WhenGuestAndMemberHaveOrdersForDifferentEvents_ThenGuestOrderDeleted() {
    String guestToken = userService.visitSystem();

    userService.signUp(
            guestToken,
            "member",
            "password",
            "Test User",
            "0500000000",
            LocalDate.of(2001, 1, 1)
    );

    String memberToken = userService.login(guestToken, "member", "password");
    Member member = userRepository.getMemberByUsername("member");

    Long memberEventId = 200L;
    Long guestEventId = 300L;
    Long areaId = 1L;

    ActiveOrder memberOrder = new ActiveOrder(
            orderRepository.getNextId(),
            memberToken,
            member.getId(),
            memberEventId
    );

    memberOrder.addTicket(new Ticket(
            1L,
            memberEventId,
            areaId,
            1,
            1,
            BigDecimal.valueOf(100)
    ));

    orderRepository.addOrder(memberOrder);

    guestToken = userService.logOut(memberToken);

    ActiveOrder guestOrder = new ActiveOrder(
            orderRepository.getNextId(),
            guestToken,
            null,
            guestEventId
    );

    guestOrder.addTicket(new Ticket(
            2L,
            guestEventId,
            areaId,
            1,
            2,
            BigDecimal.valueOf(100)
    ));

    orderRepository.addOrder(guestOrder);

    userService.login(guestToken, "member", "password");

    ActiveOrder updatedMemberOrder = orderRepository.getActiveOrderByUserId(member.getId());

    assertNotNull(updatedMemberOrder);
    assertEquals(memberOrder.getOrderId(), updatedMemberOrder.getOrderId());

    ActiveOrder deletedGuestOrder =
        orderRepository.getActiveOrderBySessionToken(guestToken);

assertNotNull(deletedGuestOrder);
assertEquals(OrderStatus.CANCELLED, deletedGuestOrder.getStatus());

ActiveOrder guestOrderById =
        orderRepository.findOrderById(guestOrder.getOrderId());

assertNotNull(guestOrderById);
assertEquals(OrderStatus.CANCELLED, guestOrderById.getStatus());
}
}