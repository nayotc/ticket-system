package ticketsystem.AcceptanceTesting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ticketsystem.ApplicationLayer.CompanyService;
import ticketsystem.ApplicationLayer.INotifier;
import ticketsystem.ApplicationLayer.ISystemLogger;
import ticketsystem.ApplicationLayer.SystemAdminService;
import ticketsystem.ApplicationLayer.TokenService;
import ticketsystem.ApplicationLayer.UserAccessService;
import ticketsystem.DTO.CompanyDTO;
import ticketsystem.DTO.OrderDTO;
import ticketsystem.DTO.SuspentionUserDTO;
import ticketsystem.DomainLayer.IRepository.ISystemAdminRepository;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.DomainLayer.MembershipDomainService;
import ticketsystem.DomainLayer.company.Company;
import ticketsystem.DomainLayer.history.PaymentDetails;
import ticketsystem.DomainLayer.history.Purchase;
import ticketsystem.DomainLayer.history.PurchasedTicket;
import ticketsystem.DomainLayer.systemAdmin.SystemAdmin;
import ticketsystem.DomainLayer.user.CompanyRole;
import ticketsystem.DomainLayer.user.Founder;
import ticketsystem.DomainLayer.user.Member;
import ticketsystem.DomainLayer.user.Permission;
import ticketsystem.DomainLayer.user.RoleStatus;
import ticketsystem.DomainLayer.user.User;
import ticketsystem.InfrastructureLayer.CompanyRepository;
import ticketsystem.InfrastructureLayer.HistoryRepository;
import ticketsystem.InfrastructureLayer.LogbackSystemLogger;
import ticketsystem.InfrastructureLayer.InMemoryNotificationsRepository;
import ticketsystem.DomainLayer.IRepository.IOrderRepository;
import ticketsystem.InfrastructureLayer.InMemoryOrderRepository;
import ticketsystem.InfrastructureLayer.InMemoryUserRepository;
import ticketsystem.InfrastructureLayer.PaymentServiceProxy;
import ticketsystem.InfrastructureLayer.SecureBarcodeProxy;
import ticketsystem.InfrastructureLayer.SystemAdminRepository;
import ticketsystem.InfrastructureLayer.TokenRepository;
import ticketsystem.testutil.RecordingNotifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * Acceptance tests for system-administrator use cases.
 *
 * <p>Company and purchase-history persistence use the production repository
 * adapters with an embedded H2 database.</p>
 */
@DataJpaTest(
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop"
        }
)
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.ANY
)
@Import({
        CompanyRepository.class,
        HistoryRepository.class
})
public class SystemAdminServiceTest {
    private SystemAdminService systemAdminService;
    private ISystemAdminRepository realAdminRepo;
    private TokenService tokenService;
    private IUserRepository userRepo = new InMemoryUserRepository();
    private CompanyService companyService;
    private SystemAdmin admin = new SystemAdmin("1", "Admin123", true);
    @Autowired
    private CompanyRepository companyRepo;
    @Autowired
    private HistoryRepository historyRepo;
    IOrderRepository orderRepo;
    ISystemLogger logger = new LogbackSystemLogger();
    private MembershipDomainService membershipDomain;
    private RecordingNotifier recordingNotifier;
    private INotifier notifier;
    private InMemoryNotificationsRepository notificationRepo;

    private UserAccessService userAccessService;

    @BeforeEach
    public void setUp() {
        realAdminRepo = new SystemAdminRepository();
        PaymentServiceProxy paymentProxy = new PaymentServiceProxy();
        SecureBarcodeProxy barcodeProxy = new SecureBarcodeProxy();
        PaymentServiceProxy.isConnectionSuccessful = true;
        PaymentServiceProxy.wasConnectCalled = false;
        SecureBarcodeProxy.isConnectionSuccessful = true;
        userRepo = new InMemoryUserRepository();
        TokenRepository tokenRepository = new TokenRepository();
        tokenService = new TokenService("manual_test_secret_32_chars_long", tokenRepository, logger);
        membershipDomain = new MembershipDomainService(userRepo);
        notificationRepo = new InMemoryNotificationsRepository();
        recordingNotifier = new RecordingNotifier();
        notifier = recordingNotifier;
        userAccessService = new UserAccessService(userRepo);
        companyService = new CompanyService(companyRepo, tokenService, membershipDomain, logger, userAccessService, notifier);
        orderRepo = new InMemoryOrderRepository();
        systemAdminService = new SystemAdminService(
                realAdminRepo,
                paymentProxy,
                barcodeProxy,
                userRepo,
                orderRepo,
                tokenService,
                companyRepo, logger, historyRepo,
                membershipDomain, notifier
        );
    }

    // Use Case: System Initialization
    @Test
    public void givenSystemAdminExists_whenInitSystem_thenSystemInitializesWithProxies() {

        realAdminRepo.addAdmin(admin);
        PaymentServiceProxy.isConnectionSuccessful = true;
        SecureBarcodeProxy.isConnectionSuccessful = true;

        boolean result = systemAdminService.initSystem();
        assertTrue(result, "Acceptance Test Failed: System should initialize successfully using infrastructure proxies.");
        assertEquals(1, realAdminRepo.countAdmins());
    }

    @Test
    public void givenEmptyRepository_whenInitSystem_thenInitializationFails() {
        PaymentServiceProxy.isConnectionSuccessful = true;
        FailureStateSnapshot beforeState = captureStateSnapshot();

        // Act
        boolean result = systemAdminService.initSystem();
        FailureStateSnapshot afterState = captureStateSnapshot();

        // Assert
        assertFalse(result, "Acceptance Test Failed: System allowed initialization without a System Admin.");
        assertFalse(PaymentServiceProxy.wasConnectCalled, "Payment service should not be contacted if no admin exists.");
        assertStateUnchanged(beforeState, afterState, "Init system without admin");
    }

    @Test
    public void givenPaymentServiceIsDown_whenInitSystem_thenInitializationFails() {
        // Arrange
        realAdminRepo.addAdmin(admin);

        PaymentServiceProxy.isConnectionSuccessful = false;
        SecureBarcodeProxy.isConnectionSuccessful = true;
        FailureStateSnapshot beforeState = captureStateSnapshot();

        // Act
        boolean result = systemAdminService.initSystem();
        FailureStateSnapshot afterState = captureStateSnapshot();

        // Assert
        assertFalse(result, "Acceptance Test Failed: System should fail to initialize if payment service is down.");
        assertStateUnchanged(beforeState, afterState, "Init system when payment service is down");
    }

    @Test
    public void givenBarcodeServiceIsDown_whenInitSystem_thenInitializationFails() {
        // Arrange
        realAdminRepo.addAdmin(admin);

        PaymentServiceProxy.isConnectionSuccessful = true;
        SecureBarcodeProxy.isConnectionSuccessful = false;
        FailureStateSnapshot beforeState = captureStateSnapshot();

        // Act
        boolean result = systemAdminService.initSystem();
        FailureStateSnapshot afterState = captureStateSnapshot();

        // Assert
        assertFalse(result, "Acceptance Test Failed: System should fail to initialize if barcode service is down.");
        assertStateUnchanged(beforeState, afterState, "Init system when barcode service is down");
    }

    // Use case: delete member by admin
    @Test
    public void givenInvalidAdminId_whenDeleteMember_thenReturnsUnauthorizedError() {
        FailureStateSnapshot beforeState = captureStateSnapshot();

        String result = systemAdminService.deleteMemberByAdmin(-5L, 1L);
        FailureStateSnapshot afterState = captureStateSnapshot();

        assertTrue(result.startsWith("ERROR: Unauthorized access"), "Should reject invalid token.");
        assertStateUnchanged(beforeState, afterState, "Delete member with invalid admin");
    }

    @Test
    public void givenNonExistentMember_whenDeleteMember_thenReturnsNotFoundError() {
        // Arrange
        SystemAdmin admin = new SystemAdmin("1", "Admin123", true);
        realAdminRepo.addAdmin(admin);
        long nonExistentMemberId = 99L;

        FailureStateSnapshot beforeState = captureStateSnapshot();

        // Act
        String result = systemAdminService.deleteMemberByAdmin(1L, nonExistentMemberId);
        FailureStateSnapshot afterState = captureStateSnapshot();

        // Assert
        assertEquals("ERROR: Member with ID 99 was not found.", result, "Should return not found error.");
        assertStateUnchanged(beforeState, afterState, "Delete non-existent member");
    }

    @Test
    public void givenValidRequest_whenDeleteMember_thenMemberIsDeletedAndCleanupPerformed() {
        // Arrange
        SystemAdmin admin = new SystemAdmin("1", "Admin123", true);
        realAdminRepo.addAdmin(admin);
        long memberId = 1L;

        Member member = new Member(memberId, "TestUser", "Test User", "0500000001", LocalDate.of(2001, 1, 1));
        userRepo.addRegisteredMember(memberId, member, "hashedPassword123");

        // Act
        String result = systemAdminService.deleteMemberByAdmin(1L, memberId);

        // Assert
        assertEquals("SUCCESS: Member deactivated and associated records cleaned up.", result);

        User deletedUser = userRepo.getMemberById(memberId);
        assertTrue(deletedUser == null, "Member should be removed from UserRepository.");
    }

   @Test
        void GivenActiveSystemAdminAndActiveCompany_WhenCloseProductionCompanyByAdmin_ThenCompanyIsClosedAndRolesAreCancelled()
                throws Exception {
        // Arrange
        long adminId = 1L;
        long founderId = 2L;
        long ownerId = 3L;
        long managerId = 4L;

        realAdminRepo.addAdmin(new SystemAdmin(String.valueOf(adminId), "admin", true));

        Member founder = new Member(
                founderId,
                "founder",
                "Founder User",
                "0500000002",
                LocalDate.of(2001, 1, 1)
        );
        userRepo.addRegisteredMember(founderId, founder, "password123");

        String founderSessionId = tokenService.addActiveSession(founder);

        CompanyDTO createdCompany = companyService.createProductionCompany(
                founderSessionId,
                "Test Company"
        );
        long companyId = createdCompany.getId();

        Member owner = new Member(
                ownerId,
                "owner",
                "Owner User",
                "0500000003",
                LocalDate.of(2001, 1, 1)
        );
        owner.addOwnerRole(companyId, founderId);
        owner.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        userRepo.addRegisteredMember(ownerId, owner, "password123");

        Set<Permission> managerPermissions = new HashSet<>();
        managerPermissions.add(Permission.MANAGE_EVENT_INVENTORY);

        Member manager = new Member(
                managerId,
                "manager",
                "Manager User",
                "0500000004",
                LocalDate.of(2001, 1, 1)
        );
        manager.addManagerRole(companyId, founderId, managerPermissions);
        manager.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        userRepo.addRegisteredMember(managerId, manager, "password123");

        Member savedFounder = userRepo.getMemberById(founderId);
        Founder founderRole = (Founder) savedFounder.getRoleInCompany(companyId);

        founderRole.addAppointee(ownerId);
        founderRole.addAppointee(managerId);

        userRepo.updateMember(savedFounder);

        assertTrue(
                membershipDomain.getManagementSubTreeMemberIds(founderId, companyId).contains(ownerId),
                "Owner should be part of the founder management subtree before admin closure."
        );
        assertTrue(
                membershipDomain.getManagementSubTreeMemberIds(founderId, companyId).contains(managerId),
                "Manager should be part of the founder management subtree before admin closure."
        );

        // Act
        CompanyDTO closedCompany = systemAdminService.closeProductionCompanyByAdmin(adminId, companyId);

        // Assert
        assertNotNull(closedCompany);

        Company savedCompany = companyRepo.findById(companyId)
                .orElseThrow(() -> new Exception("Company was not found after closing"));

        assertFalse(savedCompany.isActive(), "Company should become inactive.");

        CompanyRole savedFounderRole = userRepo.getMemberById(founderId).getRoleInCompany(companyId);
        CompanyRole savedOwnerRole = userRepo.getMemberById(ownerId).getRoleInCompany(companyId);
        CompanyRole savedManagerRole = userRepo.getMemberById(managerId).getRoleInCompany(companyId);

        assertEquals(
                RoleStatus.CANCELLED,
                savedFounderRole.getStatus(),
                "Founder role should be cancelled by admin company closure."
        );
        assertEquals(
                RoleStatus.CANCELLED,
                savedOwnerRole.getStatus(),
                "Owner role should be cancelled by admin company closure."
        );
        assertEquals(
                RoleStatus.CANCELLED,
                savedManagerRole.getStatus(),
                "Manager role should be cancelled by admin company closure."
        );

        recordingNotifier.assertNotifiedMember(ownerId, "closed");
        recordingNotifier.assertNotifiedMember(managerId, "closed");
        }

    @Test
    void GivenNonAdminMember_WhenCloseProductionCompanyByAdmin_ThenThrowsExceptionAndCompanyRemainsActive() throws Exception {
        // Arrange
        long nonAdminId = 10L;
        long founderId = 20L;
        Member founder = new Member(founderId, "founder", "Founder User", "0500000005", LocalDate.of(2001, 1, 1));
        userRepo.addRegisteredMember(founderId, founder, "password123");
        String founderSessionId = tokenService.addActiveSession(founder);

        CompanyDTO createdCompany = companyService.createProductionCompany(founderSessionId, "Test Company");
        FailureStateSnapshot beforeState = captureStateSnapshot();
        // Act + Assert
        Exception exception = assertThrows(Exception.class, ()
                -> systemAdminService.closeProductionCompanyByAdmin(nonAdminId, createdCompany.getId())
        );

        FailureStateSnapshot afterState = captureStateSnapshot();

        assertTrue(exception.getMessage().contains("Unauthorized access"),
                "Should throw unauthorized access exception.");

        Company savedCompany = companyRepo.findById(createdCompany.getId())
                .orElseThrow(() -> new Exception("Company was not found"));

        assertTrue(savedCompany.isActive(), "Company should remain active after a failed closure attempt.");
        assertStateUnchanged(beforeState, afterState, "Close company by non-admin");
    }

        /**
         * Verifies that the system administrator can view purchase history grouped
         * by the identifier of each buyer.
         *
         * <p>Purchase identifiers are generated by the database and therefore are not
         * assigned manually by the test.</p>
         */
        @Test
        void AcceptanceTest_ViewPurchaseHistoryByBuyer_Successful() {
        // --- 1. Preparation: create an active system administrator ---
        long adminId = 1L;
        realAdminRepo.addAdmin(admin);

        // --- 2. Create purchase history for two different buyers ---
        long buyer1Id = 801L;
        long buyer2Id = 802L;

        Purchase purchase1 = new Purchase(
                List.of(
                        new PurchasedTicket(
                                401L,
                                1,
                                1,
                                new BigDecimal("200.00"),
                                "BARCODE_A"
                        )
                ),
                "Jazz Festival",
                "Shuni",
                buyer1Id,
                50L,
                4L,
                60L,
                new BigDecimal("100.00"),
                111111
        );

        Purchase purchase2 = new Purchase(
                List.of(
                        new PurchasedTicket(
                                402L,
                                1,
                                2,
                                new BigDecimal("200.00"),
                                "BARCODE_B"
                        )
                ),
                "Jazz Festival",
                "Shuni",
                buyer1Id,
                50L,
                4L,
                60L,
                new BigDecimal("100.00"),
                111111
        );

        Purchase purchase3 = new Purchase(
                List.of(
                        new PurchasedTicket(
                                403L,
                                5,
                                5,
                                new BigDecimal("150.00"),
                                "BARCODE_C"
                        )
                ),
                "Rock Concert",
                "Barby",
                buyer2Id,
                50L,
                4L,
                61L,
                new BigDecimal("100.00"),
                111111
        );

        historyRepo.addPurchase(purchase1);
        historyRepo.addPurchase(purchase2);
        historyRepo.addPurchase(purchase3);

        // The database must assign identifiers to all new purchases.
        assertNotNull(purchase1.getPurchaseId());
        assertNotNull(purchase2.getPurchaseId());
        assertNotNull(purchase3.getPurchaseId());

        // --- 3. Action ---
        Map<Long, List<OrderDTO>> historyResult =
                systemAdminService.getPurchaseHistoryByBuyer(adminId);

        // --- 4. Assertions ---
        assertNotNull(
                historyResult,
                "History result should not be null"
        );

        assertFalse(
                historyResult.isEmpty(),
                "History result should not be empty"
        );

        assertEquals(
                2,
                historyResult.size(),
                "Result should contain exactly two distinct buyers"
        );

        assertTrue(
                historyResult.containsKey(buyer1Id),
                "Result should contain buyer 1"
        );

        List<OrderDTO> buyer1Purchases =
                historyResult.get(buyer1Id);

        assertEquals(
                2,
                buyer1Purchases.size(),
                "Buyer 1 should have exactly two purchases"
        );

        assertTrue(
                historyResult.containsKey(buyer2Id),
                "Result should contain buyer 2"
        );

        List<OrderDTO> buyer2Purchases =
                historyResult.get(buyer2Id);

        assertEquals(
                1,
                buyer2Purchases.size(),
                "Buyer 2 should have exactly one purchase"
        );
        }

    // Use Case 6.4: View Purchase History by Buyer - Failure Scenarios
    @Test
    void AcceptanceTest_ViewPurchaseHistoryByBuyer_Failure_UnauthorizedAccess() {
        // --- 1. Preparation (Simulating an unauthorized request) ---
        long unauthorizedAdminId = 999L;

        // --- 2 & 3 & 4. Action & Assertions ---
        FailureStateSnapshot beforeState = captureStateSnapshot();

        Exception exception = assertThrows(SecurityException.class, () -> {
            systemAdminService.getPurchaseHistoryByBuyer(unauthorizedAdminId);
        });
        FailureStateSnapshot afterState = captureStateSnapshot();

        assertTrue(exception.getMessage().contains("Unauthorized access"),
                "Exception message should indicate unauthorized access");
        assertStateUnchanged(beforeState, afterState, "View purchase history with unauthorized admin");
    }

        /**
         * Verifies that global purchase history is grouped first by company and then
         * by event name.
         *
         * <p>All purchase identifiers are generated by the database.</p>
         */
        @Test
        void AcceptanceTest_ViewHistoryByCompanyAndEvent_Successful() {
        // --- 1. Preparation: create an active system administrator ---
        long adminId = 1L;
        realAdminRepo.addAdmin(admin);

        // --- 2. Create history for one company and two different events ---
        long companyId = 40L;
        String event1Name = "Tomorrowland Israel";
        String event2Name = "Winter Festival";

        Purchase purchase1 = new Purchase(
                List.of(
                        new PurchasedTicket(
                                301L,
                                10,
                                5,
                                new BigDecimal("400.00"),
                                "VIP_BARCODE"
                        )
                ),
                event1Name,
                "Expo TLV",
                666L,
                companyId,
                4L,
                52L,
                new BigDecimal("100.00"),
                111111
        );

        Purchase purchase2 = new Purchase(
                List.of(
                        new PurchasedTicket(
                                302L,
                                10,
                                6,
                                new BigDecimal("400.00"),
                                "VIP_BARCODE"
                        )
                ),
                event1Name,
                "Expo TLV",
                777L,
                companyId,
                4L,
                52L,
                new BigDecimal("100.00"),
                111111
        );

        Purchase purchase3 = new Purchase(
                List.of(
                        new PurchasedTicket(
                                303L,
                                1,
                                1,
                                new BigDecimal("200.00"),
                                "REGULAR_BARCODE"
                        )
                ),
                event2Name,
                "Expo TLV",
                888L,
                companyId,
                4L,
                53L,
                new BigDecimal("100.00"),
                111111
        );

        historyRepo.addPurchase(purchase1);
        historyRepo.addPurchase(purchase2);
        historyRepo.addPurchase(purchase3);

        // The database must assign identifiers to all new purchases.
        assertNotNull(purchase1.getPurchaseId());
        assertNotNull(purchase2.getPurchaseId());
        assertNotNull(purchase3.getPurchaseId());

        // --- 3. Action ---
        Map<Long, Map<String, List<OrderDTO>>> historyResult =
                systemAdminService
                        .getPurchaseHistoryByCompanyAndEvent(adminId);

        // --- 4. Assertions ---
        assertNotNull(
                historyResult,
                "History result should not be null"
        );

        assertFalse(
                historyResult.isEmpty(),
                "History result should not be empty"
        );

        assertTrue(
                historyResult.containsKey(companyId),
                "Result should group purchases by company ID"
        );

        Map<String, List<OrderDTO>> eventsForCompany =
                historyResult.get(companyId);

        assertNotNull(
                eventsForCompany,
                "The event map for the company should not be null"
        );

        assertTrue(
                eventsForCompany.containsKey(event1Name),
                "Result should contain Tomorrowland Israel"
        );

        assertEquals(
                2,
                eventsForCompany.get(event1Name).size(),
                "Tomorrowland Israel should contain two purchases"
        );

        assertTrue(
                eventsForCompany.containsKey(event2Name),
                "Result should contain Winter Festival"
        );

        assertEquals(
                1,
                eventsForCompany.get(event2Name).size(),
                "Winter Festival should contain one purchase"
        );
        }

    // Use Case 6.4: View Global Purchase History (By Company and Event) - Failure Scenarios
    @Test
    void AcceptanceTest_ViewHistoryByCompanyAndEvent_Failure_NoHistory() {
        // --- 1. Preparation ---
        long adminId = 1L;
        realAdminRepo.addAdmin(admin);

        // --- 2. NO PURCHASE HISTORY EXISTS ---
        // empty historyRepo, no purchases added
        // --- 3 & 4. Action & Assertions ---
        FailureStateSnapshot beforeState = captureStateSnapshot();

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            systemAdminService.getPurchaseHistoryByCompanyAndEvent(adminId);
        });
        FailureStateSnapshot afterState = captureStateSnapshot();

        assertTrue(exception.getMessage().contains("No purchase history"),
                "Exception message should indicate that no history is available");
        assertStateUnchanged(beforeState, afterState, "View company history with no history");
    }

    // Use Case 6.4: View Global Purchase History (By Company and Event) - Failure Scenarios
    @Test
    void AcceptanceTest_ViewHistoryByCompanyAndEvent_Failure_UnauthorizedAccess() {
        // --- 1. Preparation ---
        long unauthorizedAdminId = 999L; // ID that does not correspond to any real admin in the repository

        // --- 2 & 3 & 4. Action & Assertions ---
        FailureStateSnapshot beforeState = captureStateSnapshot();

        Exception exception = assertThrows(SecurityException.class, () -> {
            systemAdminService.getPurchaseHistoryByCompanyAndEvent(unauthorizedAdminId);
        });
        FailureStateSnapshot afterState = captureStateSnapshot();

        assertTrue(exception.getMessage().contains("Unauthorized access"),
                "Exception message should indicate unauthorized access for invalid admin");
        assertStateUnchanged(beforeState, afterState, "View company history with unauthorized admin");
    }

    // -------------------- UC 6.7: Suspend Member by System Admin -------------------
    @Test
    void GivenActiveSystemAdminAndExistingMember_WhenSuspendMemberTemporarily_ThenMemberIsSuspendedAndSaved() {
        long adminId = 1L;
        long memberId = 100L;

        realAdminRepo.addAdmin(admin);

        Member member = new Member(memberId, "baduser", "Bad User", "0501112222", LocalDate.of(2001, 1, 1));
        userRepo.addRegisteredMember(memberId, member, "password123");

        LocalDateTime start = LocalDateTime.now().minusMinutes(1);
        LocalDateTime end = LocalDateTime.now().plusDays(30);
        String reason = "Violation of terms";

        boolean result = systemAdminService.suspendMemberByAdmin(
                adminId,
                memberId,
                start,
                end,
                reason
        );

        Member savedMember = userRepo.getMemberById(memberId);

        assertTrue(result);
        assertNotNull(savedMember);
        assertTrue(savedMember.isSuspended());
        assertNotNull(savedMember.getSuspension());
        assertEquals(adminId, savedMember.getSuspension().getSuspendedByAdminId());
        assertEquals(reason, savedMember.getSuspension().getReason());
        assertEquals(start, savedMember.getSuspension().getStartDate());
        assertEquals(end, savedMember.getSuspension().getEndDate());
        assertFalse(savedMember.getSuspension().isPermanent());
        recordingNotifier.assertNotifiedMember(memberId, "suspended");
        recordingNotifier.assertNotificationCount(1);
    }

    @Test
    void GivenActiveSystemAdminAndExistingMember_WhenSuspendMemberPermanently_ThenMemberHasPermanentSuspension() {
        long adminId = 1L;
        long memberId = 101L;

        realAdminRepo.addAdmin(admin);

        Member member = new Member(memberId, "permanentuser", "Permanent User", "0502223333", LocalDate.of(2001, 1, 1));
        userRepo.addRegisteredMember(memberId, member, "password123");

        LocalDateTime start = LocalDateTime.now().minusMinutes(1);
        String reason = "Permanent suspension";

        boolean result = systemAdminService.suspendMemberByAdmin(
                adminId,
                memberId,
                start,
                null,
                reason
        );

        Member savedMember = userRepo.getMemberById(memberId);

        assertTrue(result);
        assertNotNull(savedMember);
        assertTrue(savedMember.isSuspended());
        assertNotNull(savedMember.getSuspension());
        assertTrue(savedMember.getSuspension().isPermanent());
        assertEquals(reason, savedMember.getSuspension().getReason());
        recordingNotifier.assertNotifiedMember(memberId, "suspended");
        recordingNotifier.assertNotificationCount(1);
    }

    @Test
    void GivenInvalidAdmin_WhenSuspendMember_ThenThrowsUnauthorizedAndMemberIsNotSuspended() {
        long invalidAdminId = 999L;
        long memberId = 102L;

        Member member = new Member(memberId, "regularuser", "Regular User", "0503334444", LocalDate.of(2001, 1, 1));
        userRepo.addRegisteredMember(memberId, member, "password123");

        FailureStateSnapshot beforeState = captureStateSnapshot();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> systemAdminService.suspendMemberByAdmin(
                        invalidAdminId,
                        memberId,
                        LocalDateTime.now(),
                        LocalDateTime.now().plusDays(7),
                        "Unauthorized attempt"
                )
        );

        FailureStateSnapshot afterState = captureStateSnapshot();

        Member savedMember = userRepo.getMemberById(memberId);

        assertTrue(exception.getMessage().contains("Unauthorized access"));
        assertNotNull(savedMember);
        assertFalse(savedMember.isSuspended());

        assertStateUnchanged(beforeState, afterState, "Suspend member with invalid admin");
        recordingNotifier.assertNoNotifications();
    }

    @Test
    void GivenActiveSystemAdminAndMissingMember_WhenSuspendMember_ThenThrowsMemberNotFound() {
        long adminId = 1L;
        long missingMemberId = 999L;

        realAdminRepo.addAdmin(admin);
        FailureStateSnapshot beforeState = captureStateSnapshot();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> systemAdminService.suspendMemberByAdmin(
                        adminId,
                        missingMemberId,
                        LocalDateTime.now(),
                        LocalDateTime.now().plusDays(7),
                        "Missing member"
                )
        );
        FailureStateSnapshot afterState = captureStateSnapshot();

        assertTrue(exception.getMessage().contains("was not found"));
        assertStateUnchanged(beforeState, afterState, "Revoke suspension for missing member");
        recordingNotifier.assertNoNotifications();
    }

    @Test
    void GivenActiveSystemAdminAndInvalidSuspensionDates_WhenSuspendMember_ThenThrowsAndMemberIsNotSuspended() {
        long adminId = 1L;
        long memberId = 103L;

        realAdminRepo.addAdmin(admin);

        Member member = new Member(memberId, "dateuser", "Date User", "0504445555", LocalDate.of(2001, 1, 1));
        userRepo.addRegisteredMember(memberId, member, "password123");

        LocalDateTime start = LocalDateTime.now().plusDays(10);
        LocalDateTime end = LocalDateTime.now().plusDays(1);

        FailureStateSnapshot beforeState = captureStateSnapshot();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> systemAdminService.suspendMemberByAdmin(
                        adminId,
                        memberId,
                        start,
                        end,
                        "Invalid dates"
                )
        );

        FailureStateSnapshot afterState = captureStateSnapshot();
        Member savedMember = userRepo.getMemberById(memberId);

        assertTrue(exception.getMessage().contains("End date cannot be before start date"));
        assertNotNull(savedMember);
        assertFalse(savedMember.isSuspended());
        assertStateUnchanged(beforeState, afterState, "Suspend member with invalid dates");
        recordingNotifier.assertNoNotifications();
    }

    @Test
    void GivenAlreadySuspendedMember_WhenSuspendMemberAgain_ThenThrowsAndOriginalSuspensionRemains() {
        long adminId = 1L;
        long memberId = 104L;

        realAdminRepo.addAdmin(admin);

        Member member = new Member(memberId, "alreadySuspended", "Already Suspended", "0505556666", LocalDate.of(2001, 1, 1));
        userRepo.addRegisteredMember(memberId, member, "password123");

        systemAdminService.suspendMemberByAdmin(
                adminId,
                memberId,
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusDays(5),
                "First reason"
        );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> systemAdminService.suspendMemberByAdmin(
                        adminId,
                        memberId,
                        LocalDateTime.now(),
                        LocalDateTime.now().plusDays(10),
                        "Second reason"
                )
        );

        Member savedMember = userRepo.getMemberById(memberId);

        assertTrue(exception.getMessage().contains("already suspended"));
        assertTrue(savedMember.isSuspended());
        assertEquals("First reason", savedMember.getSuspension().getReason());
        recordingNotifier.assertNotificationCount(1);
    }

    @Test
    void GivenActiveSystemAdminAndSuspendedMember_WhenRevokeSuspension_ThenMemberIsNoLongerSuspended() {
        long adminId = 1L;
        long memberId = 200L;

        realAdminRepo.addAdmin(admin);

        Member member = new Member(memberId, "suspendeduser", "Suspended User", "0506667777", LocalDate.of(2001, 1, 1));
        userRepo.addRegisteredMember(memberId, member, "password123");

        systemAdminService.suspendMemberByAdmin(
                adminId,
                memberId,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(7),
                "Temporary suspension"
        );

        boolean result = systemAdminService.revokeMemberByAdmin(adminId, memberId);

        Member savedMember = userRepo.getMemberById(memberId);

        assertTrue(result);
        assertNotNull(savedMember);
        assertFalse(savedMember.isSuspended());
        assertNotNull(savedMember.getSuspension());
        assertTrue(savedMember.getSuspension().isRevoked());
        recordingNotifier.assertNotifiedMember(memberId, "revoked");
        recordingNotifier.assertNotificationCount(2);
    }

    @Test
    void GivenInvalidAdmin_WhenRevokeSuspension_ThenThrowsUnauthorizedAndSuspensionRemainsActive() {
        long adminId = 1L;
        long invalidAdminId = 999L;
        long memberId = 201L;

        realAdminRepo.addAdmin(admin);

        Member member = new Member(memberId, "blockeduser", "Blocked User", "0507778888", LocalDate.of(2001, 1, 1));
        userRepo.addRegisteredMember(memberId, member, "password123");

        FailureStateSnapshot beforeState = captureStateSnapshot();
        systemAdminService.suspendMemberByAdmin(
                adminId,
                memberId,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(7),
                "Temporary suspension"
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> systemAdminService.revokeMemberByAdmin(invalidAdminId, memberId)
        );
        FailureStateSnapshot afterState = captureStateSnapshot();

        Member savedMember = userRepo.getMemberById(memberId);

        assertTrue(exception.getMessage().contains("Unauthorized access"));
        assertTrue(savedMember.isSuspended());
        assertStateUnchanged(beforeState, afterState, "Revoke suspension with invalid admin");
    }

    @Test
    void GivenActiveSystemAdminAndMissingMember_WhenRevokeSuspension_ThenThrowsMemberNotFound() {
        long adminId = 1L;
        long missingMemberId = 999L;

        realAdminRepo.addAdmin(admin);
        FailureStateSnapshot beforeState = captureStateSnapshot();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> systemAdminService.revokeMemberByAdmin(adminId, missingMemberId)
        );
        FailureStateSnapshot afterState = captureStateSnapshot();

        assertTrue(exception.getMessage().contains("was not found"));
        assertStateUnchanged(beforeState, afterState, "Revoke suspension for missing member");
    }

    @Test
    void GivenActiveSystemAdminAndMemberIsNotSuspended_WhenRevokeSuspension_ThenThrowsMemberIsNotSuspended() {
        long adminId = 1L;
        long memberId = 202L;

        realAdminRepo.addAdmin(admin);

        Member member = new Member(memberId, "regularmember", "Regular Member", "0508889999", LocalDate.of(2001, 1, 1));
        userRepo.addRegisteredMember(memberId, member, "password123");

        FailureStateSnapshot beforeState = captureStateSnapshot();
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> systemAdminService.revokeMemberByAdmin(adminId, memberId)
        );
        FailureStateSnapshot afterState = captureStateSnapshot();

        Member savedMember = userRepo.getMemberById(memberId);

        assertTrue(exception.getMessage().contains("Member is not suspended"));
        assertFalse(savedMember.isSuspended());
        assertStateUnchanged(beforeState, afterState, "Revoke suspension for member that is not suspended");
    }

    @Test
    void GivenActiveSystemAdminAndSuspendedMembers_WhenViewSuspendedMembers_ThenOnlyActiveSuspensionsAreReturned() {
        long adminId = 1L;

        realAdminRepo.addAdmin(admin);

        Member normalMember = new Member(300L, "normal", "Normal User", "0500000001", LocalDate.of(2001, 1, 1));
        userRepo.addRegisteredMember(300L, normalMember, "password123");

        Member temporarySuspendedMember = new Member(301L, "temporary", "Temporary Suspended", "0500000002", LocalDate.of(2001, 1, 1));
        userRepo.addRegisteredMember(301L, temporarySuspendedMember, "password123");

        Member permanentSuspendedMember = new Member(302L, "permanent", "Permanent Suspended", "0500000003", LocalDate.of(2001, 1, 1));
        userRepo.addRegisteredMember(302L, permanentSuspendedMember, "password123");

        Member revokedSuspensionMember = new Member(303L, "revoked", "Revoked User", "0500000004", LocalDate.of(2001, 1, 1));
        userRepo.addRegisteredMember(303L, revokedSuspensionMember, "password123");

        LocalDateTime start = LocalDateTime.now().minusDays(1);

        systemAdminService.suspendMemberByAdmin(
                adminId,
                301L,
                start,
                start.plusDays(10),
                "Temporary reason"
        );

        systemAdminService.suspendMemberByAdmin(
                adminId,
                302L,
                start,
                null,
                "Permanent reason"
        );

        systemAdminService.suspendMemberByAdmin(
                adminId,
                303L,
                start,
                start.plusDays(10),
                "Revoked reason"
        );
        systemAdminService.revokeMemberByAdmin(adminId, 303L);

        List<SuspentionUserDTO> result = systemAdminService.viewSuspendedMembersByAdmin(adminId);

        assertNotNull(result);
        assertEquals(2, result.size());

        boolean containsTemporaryMember = result.stream()
                .anyMatch(dto
                        -> dto.getMemberId() == 301L
                && dto.getReason().equals("Temporary reason")
                && dto.getDuration() != null
                && dto.getDuration() == 10L
                );

        boolean containsPermanentMember = result.stream()
                .anyMatch(dto
                        -> dto.getMemberId() == 302L
                && dto.getReason().equals("Permanent reason")
                && dto.getDuration() == null
                );

        boolean containsNormalMember = result.stream()
                .anyMatch(dto -> dto.getMemberId() == 300L);

        boolean containsRevokedMember = result.stream()
                .anyMatch(dto -> dto.getMemberId() == 303L);

        assertTrue(containsTemporaryMember);
        assertTrue(containsPermanentMember);
        assertFalse(containsNormalMember);
        assertFalse(containsRevokedMember);
    }

    @Test
    void GivenActiveSystemAdminAndNoSuspendedMembers_WhenViewSuspendedMembers_ThenThrowsNoSuspendedMembersFound() {
        long adminId = 1L;

        realAdminRepo.addAdmin(admin);

        Member normalMember = new Member(304L, "happy", "Happy User", "0500000005", LocalDate.of(2001, 1, 1));
        userRepo.addRegisteredMember(304L, normalMember, "password123");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> systemAdminService.viewSuspendedMembersByAdmin(adminId)
        );

        assertTrue(exception.getMessage().contains("No suspended members found"));
    }

    @Test
    void GivenInvalidAdmin_WhenViewSuspendedMembers_ThenThrowsUnauthorizedAccess() {
        long invalidAdminId = 999L;

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> systemAdminService.viewSuspendedMembersByAdmin(invalidAdminId)
        );

        assertTrue(exception.getMessage().contains("Unauthorized access"));
    }

    @Test
    void GivenActiveSystemAdminAndExpiredSuspension_WhenViewSuspendedMembers_ThenExpiredSuspensionIsNotReturned() {
        long adminId = 1L;
        long memberId = 305L;

        realAdminRepo.addAdmin(admin);

        Member member = new Member(memberId, "expired", "Expired Suspension", "0500000006", LocalDate.of(2001, 1, 1));
        userRepo.addRegisteredMember(memberId, member, "password123");

        FailureStateSnapshot beforeState = captureStateSnapshot();
        systemAdminService.suspendMemberByAdmin(
                adminId,
                memberId,
                LocalDateTime.now().minusDays(10),
                LocalDateTime.now().minusDays(1),
                "Expired suspension"
        );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> systemAdminService.viewSuspendedMembersByAdmin(adminId)
        );
        FailureStateSnapshot afterState = captureStateSnapshot();

        assertTrue(exception.getMessage().contains("No suspended members found"));
        assertStateUnchanged(beforeState, afterState, "View suspended members with only expired suspensions");
    }

    // for checking invariants before and after failure scenarios
    private record FailureStateSnapshot(int adminCount, int registeredMemberCount, int companyCount, int purchaseCount) {

    }

    private FailureStateSnapshot captureStateSnapshot() {
        return new FailureStateSnapshot(
                realAdminRepo.countAdmins(),
                userRepo.getAllMembers().size(),
                companyRepo.findAll().size(),
                historyRepo.getAllPurchases().size()
        );
    }

    private void assertStateUnchanged(FailureStateSnapshot before, FailureStateSnapshot after, String scenario) {
        assertEquals(before.adminCount, after.adminCount, scenario + ": admin count should not change on failure");
        assertEquals(before.registeredMemberCount, after.registeredMemberCount, scenario + ": registered member count should not change on failure");
        assertEquals(before.companyCount, after.companyCount, scenario + ": company count should not change on failure");
        assertEquals(before.purchaseCount, after.purchaseCount, scenario + ": purchase count should not change on failure");
    }
}
