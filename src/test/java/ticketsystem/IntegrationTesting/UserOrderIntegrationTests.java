package ticketsystem.IntegrationTesting;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.ApplicationLayer.OrderService;
import ticketsystem.ApplicationLayer.TokenService;
import ticketsystem.ApplicationLayer.UserService;
import ticketsystem.DomainLayer.IRepository.ITokenRepository;
import ticketsystem.DomainLayer.order.ActiveOrder;
import ticketsystem.DomainLayer.order.ActiveOrder.OrderStatus;
import ticketsystem.DomainLayer.order.Ticket;
import ticketsystem.DomainLayer.user.Member;
import ticketsystem.InfrastructureLayer.LogbackSystemLogger;
import ticketsystem.InfrastructureLayer.OrderRepository;
import ticketsystem.InfrastructureLayer.TokenRepository;
import ticketsystem.InfrastructureLayer.UserRepository;

public class UserOrderIntegrationTests {

    private UserRepository userRepository;
    private UserService userService;
    private TokenService tokenService;
    private ITokenRepository tokenRepository;
    private LogbackSystemLogger logger;
    private OrderRepository orderRepository;
    private OrderService orderService;

    @BeforeEach
    public void setup() {
        logger = new LogbackSystemLogger();
        userRepository = new UserRepository();
        tokenRepository = new TokenRepository();
        tokenService = new TokenService("manual_test_secret_32_chars_long", tokenRepository);
        userService = new UserService(userRepository, tokenService, logger);
        orderRepository = new OrderRepository();
        orderService = new OrderService(orderRepository, tokenService, logger);
        userService.addUserLoginListener(orderService);
    }

    @Test
    public void testUserLogin_WhenNeitherGuestNorMemberHasOrder_ThenNoActiveOrder() {
        // Arrange - visit → sign up
        String guestToken = userService.visitSystem();
        userService.signUp(guestToken, "member", "password");
        String memberToken = userService.login(guestToken, "member", "password");
        // doesn't have any orders
        guestToken= userService.logOut(memberToken);
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
        userService.signUp(guestToken, "member", "password");
        String memberToken = userService.login(guestToken, "member", "password");
        Member member = userRepository.getMemberByUsername("member");
        // doesn't have any orders
        guestToken= userService.logOut(memberToken);
        ActiveOrder GuestOrder = new ActiveOrder(
                orderRepository.getNextId(),
                guestToken,
                null,
                100L);
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
        userService.signUp(guestToken, "member", "password");
        String memberToken = userService.login(guestToken, "member", "password");
        Member member = userRepository.getMemberByUsername("member");
        ActiveOrder memberOrder = new ActiveOrder(
                orderRepository.getNextId(),
                memberToken,
                member.getId(),
                200L);
        orderRepository.addOrder(memberOrder);
        guestToken= userService.logOut(memberToken);        
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
        userService.signUp(guestToken, "member", "password");
        String memberToken = userService.login(guestToken, "member", "password");
        Member member = userRepository.getMemberByUsername("member");
        
        ActiveOrder memberOrder = new ActiveOrder(
                orderRepository.getNextId(),
                memberToken,
                member.getId(),
                100L);
        memberOrder.addTicket(new Ticket(1L, 100L, 1L, 0, 0, BigDecimal.TEN));
        orderRepository.addOrder(memberOrder);
        guestToken= userService.logOut(memberToken);
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
        // Arrange - visit → sign up
        String guestToken = userService.visitSystem();
        userService.signUp(guestToken, "member", "password");
        String memberToken = userService.login(guestToken, "member", "password");
        Member member = userRepository.getMemberByUsername("member");
        ActiveOrder memberOrder = new ActiveOrder(
                orderRepository.getNextId(),
                memberToken,
                member.getId(), 200L);
        orderRepository.addOrder(memberOrder);
        guestToken= userService.logOut(memberToken);
        ActiveOrder guestOrder = new ActiveOrder(
                orderRepository.getNextId(),
                guestToken,
                null, 300L);
        orderRepository.addOrder(guestOrder);
        // Act - login again
        userService.login(guestToken, "member", "password");
        // Assert - guest order deleted and member order remains
        assertNotNull(orderRepository.getActiveOrderByUserId(member.getId()));
        assertEquals(memberOrder.getOrderId(), orderRepository.getActiveOrderByUserId(member.getId()).getOrderId());
        assertEquals(OrderStatus.CANCELLED, orderRepository.getActiveOrderBySessionToken(guestToken).getStatus());
        assertEquals(OrderStatus.CANCELLED, orderRepository.findOrderById(guestOrder.getOrderId()).getStatus());
   
    }
}
