package ticketsystem.AcceptanceTesting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.ApplicationLayer.HistoryService;
import ticketsystem.ApplicationLayer.INotifier;
import ticketsystem.ApplicationLayer.IPaymentService;
import ticketsystem.ApplicationLayer.ISystemLogger;
import ticketsystem.ApplicationLayer.ITicketIssuingService;
import ticketsystem.ApplicationLayer.ITokenService;
import ticketsystem.ApplicationLayer.TokenService;
import ticketsystem.ApplicationLayer.UserAccessService;
import ticketsystem.ApplicationLayer.UserService;
import ticketsystem.DTO.OrderDTO;
import ticketsystem.DTO.PaymentDetails;
import ticketsystem.DTO.PurchaseDTO;
import ticketsystem.DTO.SalesReportDTO;
import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.IRepository.IHistoryRepository;
import ticketsystem.DomainLayer.IRepository.ITokenRepository;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.DomainLayer.MembershipDomainService;
import ticketsystem.DomainLayer.company.Company;
import ticketsystem.DomainLayer.history.Purchase;
import ticketsystem.DomainLayer.user.CompanyRole;
import ticketsystem.DomainLayer.user.Founder;
import ticketsystem.DomainLayer.user.Member;
import ticketsystem.DomainLayer.user.Permission;
import ticketsystem.DomainLayer.user.RoleStatus;
import ticketsystem.InfrastructureLayer.HistoryRepository;
import ticketsystem.InfrastructureLayer.InMemoryCompanyRepository;
import ticketsystem.InfrastructureLayer.LogbackSystemLogger;
import ticketsystem.InfrastructureLayer.InMemoryNotificationsRepository;
import ticketsystem.InfrastructureLayer.PaymentServiceProxy;
import ticketsystem.InfrastructureLayer.SecureBarcodeProxy;
import ticketsystem.InfrastructureLayer.TokenRepository;
import ticketsystem.InfrastructureLayer.InMemoryUserRepository;
import ticketsystem.testutil.RecordingNotifier;

public class HistoryServiceTest {

    private IHistoryRepository historyRepository;
    private ITokenRepository tokenRepository;
    private IUserRepository userRepository;
    private ITokenService tokenService;
    private UserService userService;
    private HistoryService historyService;
    private ICompanyRepository companyRepository;
    private UserAccessService userAccessService;
    private RecordingNotifier recordingNotifier;
    private INotifier notifier;
    private InMemoryNotificationsRepository notificationRepository;
    private ISystemLogger logger;
    private IPaymentService paymentService;
    private  ITicketIssuingService ticketIssuingService;

    @BeforeEach
    void setUp() {
        // --- Setup Real Repositories (Acceptance Level) ---
        HistoryRepository hRepo = new HistoryRepository();
        this.historyRepository = hRepo;

        this.tokenRepository = new TokenRepository();
        this.userRepository = new InMemoryUserRepository();
        this.logger = new LogbackSystemLogger();
        this.tokenService = new TokenService("manual_test_secret_32_chars_long", tokenRepository, logger);
        this.userService = new UserService(userRepository, tokenService, logger);
        this.companyRepository = new InMemoryCompanyRepository();
        this.userAccessService = new UserAccessService(userRepository);
        this.notificationRepository = new InMemoryNotificationsRepository();
        this.recordingNotifier = new RecordingNotifier();
        this.notifier = recordingNotifier;
        paymentService = new PaymentServiceProxy();
        this.ticketIssuingService=new SecureBarcodeProxy();
        resetPaymentProxy();
        this.historyService = new HistoryService(
                historyRepository,
                tokenService,
                new MembershipDomainService(userRepository),
                logger,
                userAccessService,
                notifier, paymentService,ticketIssuingService);
    }

    /**
     * Helper method to simulate a full user registration and login flow.
     */
    private String getValidMemberToken(String username, String password) {
        String guestToken = userService.visitSystem();
        userService.signUp(
                guestToken,
                username,
                password,
                "Test User",
                "0500000000", LocalDate.of(2001, 1, 1));
        return userService.login(guestToken, username, password);
    }

    private void resetPaymentProxy() {
        PaymentServiceProxy.isConnectionSuccessful = true;
        PaymentServiceProxy.isPaymentSuccessful = true;
        PaymentServiceProxy.isRefundSuccessful = true;

        PaymentServiceProxy.wasConnectCalled = false;
        PaymentServiceProxy.wasPayCalled = false;
        PaymentServiceProxy.wasRefundCalled = false;
    }

    /**
     * 3.5 View personal purchase history - Successful Scenario
     */
    @Test
    void GivenValidTokenAndExistingHistory_WhenGetHistoryForUser_ThenReturnsHistory() {
        // --- Given (Arrange) ---
        String validToken = getValidMemberToken("Yosi_history_user", "Pass123!");
        long userId = tokenService.extractUserId(validToken);

        List<PurchaseDTO> ticketDTOs = new ArrayList<>();
        ticketDTOs.add(new PurchaseDTO(10L, 1, 1, new BigDecimal("150.0"), "ACTIVE", ""));
        OrderDTO orderDto = new OrderDTO(0L, ticketDTOs, "Taylor Swift Tour", "HaYarkon Park", userId, 50L, userId,
                20L, new BigDecimal(100), 111111,false);

        historyService.onOrderCompleted(orderDto);

        // --- When (Act) ---
        List<OrderDTO> result = historyService.getHistoryForUser(validToken);

        // --- Then (Assert) ---
        assertNotNull(result, "The result should not be null");
        assertFalse(result.isEmpty(), "The history list should not be empty");
        assertEquals("Taylor Swift Tour", result.get(0).getEventName(), "Event name should match");
    }

    /**
     * 3.5 View personal purchase history - Failure (Empty) Scenario
     */
    @Test
    void GivenValidTokenAndNoHistory_WhenGetHistoryForUser_ThenReturnsEmptyList() {
        // --- Given (Arrange) ---
        String validToken = getValidMemberToken("new_user_no_history", "Pass123!");

        // --- When (Act) ---
        List<OrderDTO> result = historyService.getHistoryForUser(validToken);

        // --- Then (Assert) ---
        assertNotNull(result, "Result should be an empty list, not null");
        assertTrue(result.isEmpty(), "History should be empty when user has no purchases");
    }

    /**
     * 3.5 View personal purchase history - Unauthorized Scenario
     */
    @Test
    void GivenInvalidToken_WhenGetHistoryForUser_ThenThrowsIllegalArgumentException() {
        // --- Given (Arrange) ---
        String invalidToken = "invalid-token-456";

        // --- When & Then (Act & Assert) ---
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            historyService.getHistoryForUser(invalidToken);
        });

        assertTrue(exception.getMessage().contains("token"), "Error message should mention token");
    }

    /**
     * Add Purchase - Successful Scenario
     */
    @Test
    void GivenOrderDTO_WhenOnOrderCompleted_ThenPurchaseIsAdded() {
        // --- Arrange ---
        String validToken = getValidMemberToken("buyer_user", "Pass123!");
        long userId = tokenService.extractUserId(validToken);

        List<PurchaseDTO> ticketDTOs = new ArrayList<>();
        ticketDTOs.add(new PurchaseDTO(10L, 1, 1, new BigDecimal("150.0"), "ACTIVE", ""));
        OrderDTO orderDto = new OrderDTO(0L, ticketDTOs, "Rock Concert", "Barby", userId, 5L, userId, 20L, new BigDecimal(100), 111111,false);

        // --- Act ---
        historyService.onOrderCompleted(orderDto);

        // --- Assert ---
        List<OrderDTO> history = historyService.getHistoryForUser(validToken);
        assertEquals(1, history.size(), "One purchase should be added to history");
    }

    private Company createCompanyWithFounderRole(long founderId) {
        Company company = new Company(
                "history_company_" + founderId,
                founderId,
                null,
                null);

        companyRepository.save(company);

        Member founder = userRepository.getMemberById(founderId);
        assertNotNull(founder, "Founder member must exist before assigning company role");

        boolean roleAdded = founder.addFounderRole(company.getId());
        assertTrue(roleAdded, "Founder role should be added successfully");

        userRepository.updateMember(founder);

        return company;
    }

    private OrderDTO createOrderDTO(long userId, long companyId, String eventName) {
        List<PurchaseDTO> purchases = new ArrayList<>();
        purchases.add(new PurchaseDTO(
                10L,
                1,
                1,
                new BigDecimal("150.0"),
                "ACTIVE",
                ""));

        return new OrderDTO(
                0L,
                purchases,
                eventName,
                "HaYarkon Park",
                userId,
                companyId,
                userId,
                20L, new BigDecimal(100), 111111,false
        );
    }

    /**
     * 4.5 View purchase and order history - Successful Scenario
     */
    @Test
    void GivenOwnerAndExistingCompanyHistory_WhenGetHistoryForCompany_ThenReturnsCompanyHistory() {
        // --- Given (Arrange) ---
        String ownerToken = getValidMemberToken("company_history_owner", "Pass123!");
        long ownerId = tokenService.extractUserId(ownerToken);

        Company company = createCompanyWithFounderRole(ownerId);

        OrderDTO orderDto = createOrderDTO(
                ownerId,
                company.getId(),
                "Company History Concert");

        historyService.onOrderCompleted(orderDto);

        // --- When (Act) ---
        List<OrderDTO> result = historyService.getHistoryForCompany(ownerToken, company.getId());

        // --- Then (Assert) ---
        assertNotNull(result, "The result should not be null");
        assertFalse(result.isEmpty(), "Company history should not be empty when purchases exist");
        assertEquals(1, result.size(), "Company history should contain exactly one order");
        assertEquals("Company History Concert", result.get(0).getEventName(), "Event name should match");
        assertEquals(company.getId(), result.get(0).getCompanyId(), "Company id should match");
    }

    @Test
    void GivenMemberWithoutCompanyPermission_WhenGetHistoryForCompany_ThenThrowsIllegalArgumentException() {
        // --- Given (Arrange) ---
        String ownerToken = getValidMemberToken("real_company_owner", "Pass123!");
        long ownerId = tokenService.extractUserId(ownerToken);
        Company company = createCompanyWithFounderRole(ownerId);

        String regularMemberToken = getValidMemberToken("regular_member_no_permission", "Pass123!");

        // --- When & Then ---
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            historyService.getHistoryForCompany(regularMemberToken, company.getId());
        });

        assertTrue(
                exception.getMessage().contains("permissions"),
                "Error message should mention insufficient permissions");
    }

    private OrderDTO createSalesReportOrderDTO(Long buyerMemberId, long companyId, Long managedByMemberId,
            String eventName, BigDecimal... ticketPrices) {
        List<PurchaseDTO> purchases = new ArrayList<>();

        long ticketId = 1L;
        for (BigDecimal price : ticketPrices) {
            purchases.add(new PurchaseDTO(
                    ticketId++,
                    1,
                    1,
                    price,
                    "ACTIVE",
                    ""));
        }

        return new OrderDTO(
                0L,
                purchases,
                eventName,
                "HaYarkon Park",
                buyerMemberId,
                companyId,
                managedByMemberId,
                20L, new BigDecimal(100), 111111,false);
    }

    private long createActiveManagerUnderFounder(long founderId, long companyId, String username) {
        String managerToken = getValidMemberToken(username, "Pass123!");
        long managerId = tokenService.extractUserId(managerToken);

        Member founder = userRepository.getMemberById(founderId);
        Member manager = userRepository.getMemberById(managerId);

        assertNotNull(founder, "Founder must exist");
        assertNotNull(manager, "Manager must exist");

        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.GENERATE_SALES_REPORT);

        boolean roleAdded = manager.addManagerRole(companyId, founderId, permissions);
        assertTrue(roleAdded, "Manager role should be added successfully");

        CompanyRole managerRole = manager.getRoleInCompany(companyId);
        assertNotNull(managerRole, "Manager role should exist");
        managerRole.setStatus(RoleStatus.ACTIVE);

        CompanyRole founderRole = founder.getRoleInCompany(companyId);
        assertNotNull(founderRole, "Founder role should exist");

        assertTrue(founderRole instanceof Founder, "Founder role should be Founder");

        ((Founder) founderRole).addAppointee(managerId);

        userRepository.updateMember(manager);

        return managerId;
    }

    /**
     * 4.6 Generate sales report - Successful Scenario
     */
    @Test
    void GivenOwnerAndSalesData_WhenGenerateSalesReport_ThenReturnsTotalRevenueAndTicketsFromManagementTree() {
        // --- Given (Arrange) ---
        String ownerToken = getValidMemberToken("sales_report_owner", "Pass123!");
        long ownerId = tokenService.extractUserId(ownerToken);

        Company company = createCompanyWithFounderRole(ownerId);

        long managerId = createActiveManagerUnderFounder(
                ownerId,
                company.getId(),
                "sales_report_manager");

        // --- FIX: Link the manager to the owner's tree in DB ---
        // The helper method likely creates the manager but fails to add them
        // to the appointer's local list. We fix it here manually to build the tree.
        Member ownerMember = userRepository.getMemberById(ownerId);
        CompanyRole ownerRole = ownerMember.getRoleInCompany(company.getId());

        if (ownerRole instanceof ticketsystem.DomainLayer.user.Founder) {
            ((ticketsystem.DomainLayer.user.Founder) ownerRole).addAppointee(managerId);
        } else if (ownerRole instanceof ticketsystem.DomainLayer.user.Owner) {
            ((ticketsystem.DomainLayer.user.Owner) ownerRole).addAppointee(managerId);
        }
        userRepository.updateMember(ownerMember);
        // --------------------------------------------------------

        String outsideOwnerToken = getValidMemberToken("outside_sales_owner", "Pass123!");
        long outsideOwnerId = tokenService.extractUserId(outsideOwnerToken);

        // Purchase managed directly by owner: 2 tickets, total 250
        OrderDTO ownerManagedOrder = createSalesReportOrderDTO(
                ownerId,
                company.getId(),
                ownerId,
                "Owner Managed Event",
                new BigDecimal("100.0"),
                new BigDecimal("150.0"));

        // Purchase managed by manager under owner's subtree: 1 ticket, total 200
        OrderDTO managerManagedOrder = createSalesReportOrderDTO(
                null, // guest buyer
                company.getId(),
                managerId,
                "Manager Managed Event",
                new BigDecimal("200.0"));

        // Purchase managed by someone outside owner's subtree: should not be counted
        OrderDTO outsideManagedOrder = createSalesReportOrderDTO(
                ownerId,
                company.getId(),
                outsideOwnerId,
                "Outside Managed Event",
                new BigDecimal("999.0"));

        historyService.onOrderCompleted(ownerManagedOrder);
        historyService.onOrderCompleted(managerManagedOrder);
        historyService.onOrderCompleted(outsideManagedOrder);

        // --- When (Act) ---
        SalesReportDTO report = historyService.generateSalesReport(ownerToken, company.getId());

        // --- Then (Assert) ---
        assertNotNull(report, "Sales report should not be null");
        assertEquals(3, report.getTotalTicketsSold(), "Report should count only owner + subtree tickets");
        assertEquals(
                0,
                new BigDecimal("450.0").compareTo(report.getTotalRevenue()),
                "Report revenue should include only owner + subtree sales");
        assertEquals(
                "Sales report generated successfully",
                report.getMessage(),
                "Success message should match");
    }

    /**
     * 4.6 Generate sales report - No sales data available
     */
    @Test
    void GivenOwnerAndNoSalesData_WhenGenerateSalesReport_ThenReturnsEmptyReportSummary() {
        // --- Given (Arrange) ---
        String ownerToken = getValidMemberToken("sales_report_empty_owner", "Pass123!");
        long ownerId = tokenService.extractUserId(ownerToken);

        Company company = createCompanyWithFounderRole(ownerId);

        // --- When (Act) ---
        SalesReportDTO report = historyService.generateSalesReport(ownerToken, company.getId());

        // --- Then (Assert) ---
        assertNotNull(report, "Sales report should not be null");
        assertEquals(0, report.getTotalTicketsSold(), "No tickets should be counted");
        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(report.getTotalRevenue()),
                "Revenue should be zero");
        assertEquals(
                "No sales data was found",
                report.getMessage(),
                "Empty report message should match");
    }

    /**
     * 4.6 Generate sales report - Guest purchases are included in company sales
     * report
     */
    @Test
    void GivenGuestPurchase_WhenGenerateSalesReport_ThenGuestPurchaseIsIncludedInCompanyReport() {
        // --- Given (Arrange) ---
        String ownerToken = getValidMemberToken("sales_report_guest_owner", "Pass123!");
        long ownerId = tokenService.extractUserId(ownerToken);

        Company company = createCompanyWithFounderRole(ownerId);

        OrderDTO guestOrder = createSalesReportOrderDTO(
                null, // guest buyer
                company.getId(),
                ownerId,
                "Guest Buyer Event",
                new BigDecimal("80.0"),
                new BigDecimal("120.0"));

        historyService.onOrderCompleted(guestOrder);

        // --- When (Act) ---
        SalesReportDTO report = historyService.generateSalesReport(ownerToken, company.getId());

        // --- Then (Assert) ---
        assertNotNull(report, "Sales report should not be null");
        assertEquals(2, report.getTotalTicketsSold(), "Guest purchase tickets should be counted");
        assertEquals(
                0,
                new BigDecimal("200.0").compareTo(report.getTotalRevenue()),
                "Guest purchase revenue should be counted");
    }

    /**
     * 4.6 Generate sales report - Verify that order-completed flow stores
     * managedByMemberId correctly
     */
    @Test
    void GivenCompletedOrderWithEventCreator_WhenOnOrderCompleted_ThenPurchaseStoresManagedByMemberIdAndReportIncludesIt() {
        // --- Given (Arrange) ---
        String ownerToken = getValidMemberToken("sales_report_event_creator", "Pass123!");
        long ownerId = tokenService.extractUserId(ownerToken);

        Company company = createCompanyWithFounderRole(ownerId);

        String buyerToken = getValidMemberToken("sales_report_buyer", "Pass123!");
        long buyerId = tokenService.extractUserId(buyerToken);

        List<PurchaseDTO> tickets = new ArrayList<>();
        tickets.add(new PurchaseDTO(
                10L,
                1,
                1,
                new BigDecimal("100.0"),
                "ACTIVE",
                ""));
        tickets.add(new PurchaseDTO(
                11L,
                1,
                2,
                new BigDecimal("150.0"),
                "ACTIVE",
                ""));

        /*
         * This simulates the production flow before onOrderCompleted:
         * the completed order is populated with the event creator / event manager id.
         */
        OrderDTO completedOrder = new OrderDTO(
                0L,
                tickets,
                "Event Created By Owner",
                "HaYarkon Park",
                buyerId,
                company.getId(),
                ownerId, // managedByMemberId - should come from the event creator in the real flow
                20L, new BigDecimal(100), 111111,false);

        // --- When (Act) ---
        historyService.onOrderCompleted(completedOrder);

        // --- Then (Assert) ---
        List<Purchase> purchases = historyRepository.getAllPurchases();

        assertEquals(1, purchases.size(), "Exactly one purchase should be saved");

        Purchase savedPurchase = purchases.get(0);

        assertEquals(
                ownerId,
                savedPurchase.getManagedByMemberId(),
                "Purchase should store the event creator as managedByMemberId");

        SalesReportDTO report = historyService.generateSalesReport(ownerToken, company.getId());

        assertNotNull(report, "Sales report should not be null");
        assertEquals(2, report.getTotalTicketsSold(), "Report should include the completed order tickets");
        assertEquals(
                0,
                new BigDecimal("250.0").compareTo(report.getTotalRevenue()),
                "Report should include the completed order revenue");
    }

    @Test
    void GivenCompletedPurchase_WhenEventCanceled_ThenTicketsBecomeCanceledAndReportDoesNotCountThem() {
        String ownerToken = getValidMemberToken("cancel_event_owner", "Pass123!");
        long ownerId = tokenService.extractUserId(ownerToken);

        Company company = createCompanyWithFounderRole(ownerId);

        OrderDTO order = createSalesReportOrderDTO(
                ownerId,
                company.getId(),
                ownerId,
                "Canceled Event",
                new BigDecimal("100.0"),
                new BigDecimal("150.0"));

        historyService.onOrderCompleted(order);
        historyService.onEventCancellationRequested(20L);
        historyService.onEventCanceled(20L);

        Purchase purchase = historyRepository.getAllPurchases().get(0);

        assertTrue(purchase.getTickets().stream()
                .allMatch(ticket -> ticket.getStatus().name().equals("CANCELED")));

        SalesReportDTO report = historyService.generateSalesReport(ownerToken, company.getId());

        assertEquals(0, report.getTotalTicketsSold());
        assertEquals(0, BigDecimal.ZERO.compareTo(report.getTotalRevenue()));
        assertEquals("No sales data was found", report.getMessage());
        recordingNotifier.assertNotifiedMember(ownerId, "was canceled");
        recordingNotifier.assertNotificationCount(2);
    }

    @Test
    void TestEventUpdated_ThenBuyerIsNotified() {
        String ownerToken = getValidMemberToken("update_event_owner", "Pass123!");
        long ownerId = tokenService.extractUserId(ownerToken);

        Company company = createCompanyWithFounderRole(ownerId);

        OrderDTO order = createSalesReportOrderDTO(
                ownerId,
                company.getId(),
                ownerId,
                "Updated Event",
                new BigDecimal("100.0"));

        historyService.onOrderCompleted(order);

        String updateMessage = "The event has been rescheduled to next week.";
        historyService.onEventUpdated(20L, LocalDateTime.now().plusDays(7), "New Venue", updateMessage);

        recordingNotifier.assertNotifiedMember(ownerId, updateMessage);
        recordingNotifier.assertNotificationCount(1);
    }

    @Test
    void GivenCanceledDifferentEvent_WhenEventCanceled_ThenTicketsRemainActive() {
        String ownerToken = getValidMemberToken("cancel_other_event_owner", "Pass123!");
        long ownerId = tokenService.extractUserId(ownerToken);

        Company company = createCompanyWithFounderRole(ownerId);

        OrderDTO order = createSalesReportOrderDTO(
                ownerId,
                company.getId(),
                ownerId,
                "Active Event",
                new BigDecimal("100.0"));

        historyService.onOrderCompleted(order);

        historyService.onEventCanceled(999L);

        Purchase purchase = historyRepository.getAllPurchases().get(0);

        assertTrue(purchase.getTickets().stream()
                .noneMatch(ticket -> ticket.getStatus().name().equals("CANCELED")));
        recordingNotifier.assertNoNotifications();
    }

    @Test
    void GivenGuestToken_WhenGetHistoryForUser_ThenThrowsException() {
        String guestToken = userService.visitSystem();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> historyService.getHistoryForUser(guestToken));

        assertEquals("Only members can view personal purchase history", exception.getMessage());
    }

    @Test
    void GivenGuestToken_WhenGetHistoryForCompany_ThenThrowsException() {
        String guestToken = userService.visitSystem();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> historyService.getHistoryForCompany(guestToken, 1L));

        assertEquals("Only members can view personal purchase history", exception.getMessage());
    }

    @Test
    void GivenInvalidToken_WhenGenerateSalesReport_ThenThrowsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> historyService.generateSalesReport("invalid-token", 1L));
    }

    @Test
    void GivenGuestToken_WhenGenerateSalesReport_ThenThrowsException() {
        String guestToken = userService.visitSystem();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> historyService.generateSalesReport(guestToken, 1L));

        assertEquals("Only members can generate sales reports", exception.getMessage());
    }

    @Test
    void GivenMemberWithoutSalesReportPermission_WhenGenerateSalesReport_ThenThrowsException() {
        String token = getValidMemberToken("no_sales_permission_user", "Pass123!");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> historyService.generateSalesReport(token, 1L));

        assertEquals("Insufficient permissions to generate sales report", exception.getMessage());
    }

}
