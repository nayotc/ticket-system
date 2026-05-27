package ticketsystem.ApplicationLayer;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.DTO.OrderDTO;
import ticketsystem.DTO.PurchaseDTO;
import ticketsystem.DTO.SalesReportDTO;
import ticketsystem.DomainLayer.MembershipDomainService;
import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.IRepository.IHistoryRepository;
import ticketsystem.DomainLayer.IRepository.ITokenRepository;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.DomainLayer.company.Company;
import ticketsystem.DomainLayer.history.Purchase;
import ticketsystem.DomainLayer.user.CompanyRole;
import ticketsystem.DomainLayer.user.Founder;
import ticketsystem.DomainLayer.user.Member;
import ticketsystem.DomainLayer.user.Permission;
import ticketsystem.DomainLayer.user.RoleStatus;
import ticketsystem.InfrastructureLayer.CompanyRepository;
import ticketsystem.InfrastructureLayer.HistoryRepository;
import ticketsystem.InfrastructureLayer.TokenRepository;
import ticketsystem.InfrastructureLayer.UserRepository;
import ticketsystem.InfrastructureLayer.LogbackSystemLogger;

public class HistoryServiceTest {

    private IHistoryRepository historyRepository;
    private ITokenRepository tokenRepository;
    private IUserRepository userRepository;
    private ITokenService tokenService;
    private UserService userService;
    private HistoryService historyService;
    private ICompanyRepository companyRepository;
    private UserAccessService userAccessService;

    @BeforeEach
    void setUp() {
        // --- Setup Real Repositories (Acceptance Level) ---
        HistoryRepository hRepo = new HistoryRepository();
        this.historyRepository = hRepo;

        this.tokenRepository = new TokenRepository();
        this.userRepository = new UserRepository(); 
        
        this.tokenService = new TokenService("manual_test_secret_32_chars_long", tokenRepository);
        this.userService = new UserService(userRepository, tokenService, new LogbackSystemLogger());
        this.companyRepository = new CompanyRepository();
        userAccessService=new UserAccessService(userRepository);
        this.historyService = new HistoryService(historyRepository, tokenService, new MembershipDomainService(userRepository), new LogbackSystemLogger(),userAccessService);
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
                "0500000000"
        );
        return userService.login(guestToken, username, password);
    }

    /**
     * 3.5 View personal purchase history - Successful Scenario
     */
    @Test
    void GivenValidTokenAndExistingHistory_WhenGetHistoryForUser_ThenReturnsHistory() {
        // --- Given (Arrange) ---
        String validToken = getValidMemberToken("reut_history_user", "Pass123!");
        long userId = tokenService.extractUserId(validToken);
        
        List<PurchaseDTO> ticketDTOs = new ArrayList<>();
        ticketDTOs.add(new PurchaseDTO(10L, 1, 1, new BigDecimal("150.0"), "ACTIVE", ""));
        OrderDTO orderDto = new OrderDTO(0L, ticketDTOs, "Taylor Swift Tour", "HaYarkon Park", userId, 50L, userId, 20L);
        
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
        OrderDTO orderDto = new OrderDTO(0L, ticketDTOs, "Rock Concert", "Barby", userId, 5L, userId, 20L);

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
                null
        );

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
                ""
        ));

        return new OrderDTO(
                0L,
                purchases,
                eventName,
                "HaYarkon Park",
                userId,
                companyId,
                userId, 
                20L

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
                "Company History Concert"
        );

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
                "Error message should mention insufficient permissions"
        );
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
                    ""
            ));
        }

        return new OrderDTO(
                0L,
                purchases,
                eventName,
                "HaYarkon Park",
                buyerMemberId,
                companyId,
                managedByMemberId,
                20L
        );
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
                "sales_report_manager"
        );

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
                new BigDecimal("150.0")
        );

        // Purchase managed by manager under owner's subtree: 1 ticket, total 200
        OrderDTO managerManagedOrder = createSalesReportOrderDTO(
                null, // guest buyer
                company.getId(),
                managerId,
                "Manager Managed Event",
                new BigDecimal("200.0")
        );

        // Purchase managed by someone outside owner's subtree: should not be counted
        OrderDTO outsideManagedOrder = createSalesReportOrderDTO(
                ownerId,
                company.getId(),
                outsideOwnerId,
                "Outside Managed Event",
                new BigDecimal("999.0")
        );

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
                "Report revenue should include only owner + subtree sales"
        );
        assertEquals(
                "Sales report generated successfully",
                report.getMessage(),
                "Success message should match"
        );
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
                "Revenue should be zero"
        );
        assertEquals(
                "No sales data was found",
                report.getMessage(),
                "Empty report message should match"
        );
    }

    /**
     * 4.6 Generate sales report - Guest purchases are included in company sales report
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
                new BigDecimal("120.0")
        );

        historyService.onOrderCompleted(guestOrder);

        // --- When (Act) ---
        SalesReportDTO report = historyService.generateSalesReport(ownerToken, company.getId());

        // --- Then (Assert) ---
        assertNotNull(report, "Sales report should not be null");
        assertEquals(2, report.getTotalTicketsSold(), "Guest purchase tickets should be counted");
        assertEquals(
                0,
                new BigDecimal("200.0").compareTo(report.getTotalRevenue()),
                "Guest purchase revenue should be counted"
        );
    }
    /**
     * 4.6 Generate sales report -
     * Verify that order-completed flow stores managedByMemberId correctly
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
                ""
        ));
        tickets.add(new PurchaseDTO(
                11L,
                1,
                2,
                new BigDecimal("150.0"),
                "ACTIVE",
                ""
        ));

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
                ownerId,// managedByMemberId - should come from the event creator in the real flow
                20L
            );

        // --- When (Act) ---
        historyService.onOrderCompleted(completedOrder);

        // --- Then (Assert) ---
        List<Purchase> purchases = historyRepository.getAllPurchases();

        assertEquals(1, purchases.size(), "Exactly one purchase should be saved");

        Purchase savedPurchase = purchases.get(0);

        assertEquals(
                ownerId,
                savedPurchase.getManagedByMemberId(),
                "Purchase should store the event creator as managedByMemberId"
        );

        SalesReportDTO report = historyService.generateSalesReport(ownerToken, company.getId());

        assertNotNull(report, "Sales report should not be null");
        assertEquals(2, report.getTotalTicketsSold(), "Report should include the completed order tickets");
        assertEquals(
                0,
                new BigDecimal("250.0").compareTo(report.getTotalRevenue()),
                "Report should include the completed order revenue"
        );
    }

    
}