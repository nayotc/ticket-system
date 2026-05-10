package ticketsystem.AcceptanceTesting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.ApplicationLayer.CompanyService;
import ticketsystem.ApplicationLayer.SystemAdminService;
import ticketsystem.ApplicationLayer.TokenService;
import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.systemAdmin.SystemAdmin;
import ticketsystem.DomainLayer.user.Guest;
import ticketsystem.DomainLayer.user.Member;
import ticketsystem.DomainLayer.user.User;
import ticketsystem.InfrastructureLayer.CompanyRepository;
import ticketsystem.InfrastructureLayer.OrderRepository;
import ticketsystem.InfrastructureLayer.PaymentServiceProxy;
import ticketsystem.InfrastructureLayer.SecureBarcodeProxy;
import ticketsystem.InfrastructureLayer.SystemAdminRepository;
import ticketsystem.InfrastructureLayer.TokenRepository;
import ticketsystem.InfrastructureLayer.UserRepository;

public class SystemAdminServiceTest {

    private SystemAdminService systemAdminService;
    private SystemAdminRepository realAdminRepo;
    private TokenService tokenService;
    private UserRepository userRepo = new UserRepository();
    private CompanyService companyService;
    private SystemAdmin admin = new SystemAdmin("1", "Admin123", true);

    @BeforeEach
    public void setUp() {
        realAdminRepo = new SystemAdminRepository();
        PaymentServiceProxy paymentProxy = new PaymentServiceProxy();
        SecureBarcodeProxy barcodeProxy = new SecureBarcodeProxy();
        PaymentServiceProxy.isConnectionSuccessful = true;
        PaymentServiceProxy.wasConnectCalled = false;
        SecureBarcodeProxy.isConnectionSuccessful = true;
        userRepo = new UserRepository();
        ICompanyRepository companyRepo = new CompanyRepository();
        TokenRepository tokenRepository = new TokenRepository();
        tokenService = new TokenService("manual_test_secret_32_chars_long", tokenRepository);
        companyService = new CompanyService(companyRepo, tokenService);
        OrderRepository orderRepo = OrderRepository.getInstance();

        systemAdminService = new SystemAdminService(
                realAdminRepo,
                paymentProxy,
                barcodeProxy,
                userRepo,
                companyService,
                orderRepo,
                tokenService
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

        // Act
        boolean result = systemAdminService.initSystem();

        // Assert
        assertFalse(result, "Acceptance Test Failed: System allowed initialization without a System Admin.");
        assertFalse(PaymentServiceProxy.wasConnectCalled, "Payment service should not be contacted if no admin exists.");
    }

    @Test
    public void givenPaymentServiceIsDown_whenInitSystem_thenInitializationFails() {
        // Arrange
        realAdminRepo.addAdmin(admin);

        PaymentServiceProxy.isConnectionSuccessful = false;
        SecureBarcodeProxy.isConnectionSuccessful = true;

        // Act
        boolean result = systemAdminService.initSystem();

        // Assert
        assertFalse(result, "Acceptance Test Failed: System should fail to initialize if payment service is down.");
    }

    @Test
    public void givenBarcodeServiceIsDown_whenInitSystem_thenInitializationFails() {
        // Arrange
        realAdminRepo.addAdmin(admin);

        PaymentServiceProxy.isConnectionSuccessful = true;
        SecureBarcodeProxy.isConnectionSuccessful = false;

        // Act
        boolean result = systemAdminService.initSystem();

        // Assert
        assertFalse(result, "Acceptance Test Failed: System should fail to initialize if barcode service is down.");
    }

    // Use case: delete member by admin
    @Test
    public void givenInvalidToken_whenDeleteMember_thenReturnsUnauthorizedError() {
        // Act
        String result = systemAdminService.deleteMemberByAdmin("invalid-token-string", 1L);

        // Assert
        assertTrue(result.startsWith("ERROR: Unauthorized access"), "Should reject invalid token.");
    }

    @Test
    public void givenNonExistentMember_whenDeleteMember_thenReturnsNotFoundError() {
        // Arrange
        String validSession = tokenService.addActiveSession(new Guest());
        long nonExistentMemberId = 99L;

        // Act
        String result = systemAdminService.deleteMemberByAdmin(validSession, nonExistentMemberId);

        // Assert
        assertEquals("ERROR: Member with ID 99 was not found.", result, "Should return not found error.");
    }

    @Test
    public void givenValidRequest_whenDeleteMember_thenMemberIsDeletedAndCleanupPerformed() {
        // Arrange
        String validSession = tokenService.addActiveSession(new Guest());

        long memberId = 1L;

        Member member = new Member(-1L, "TestUser");

        userRepo.addRegisteredMember(memberId, member, "hashedPassword123");

        // Act
        String result = systemAdminService.deleteMemberByAdmin(validSession, memberId);

        // Assert
        assertEquals("SUCCESS: Member deactivated and associated records cleaned up.", result);

        User deletedUser = userRepo.getMemberById(memberId);
        assertTrue(deletedUser == null, "Member should be removed from UserRepository.");
    }

}
