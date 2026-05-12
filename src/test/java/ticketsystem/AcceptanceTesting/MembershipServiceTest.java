package ticketsystem.AcceptanceTesting;

import ticketsystem.ApplicationLayer.ITokenService;
import ticketsystem.ApplicationLayer.MembershipService;
import ticketsystem.ApplicationLayer.TokenService;
import ticketsystem.DomainLayer.MembershipDomainService;
import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.DomainLayer.IRepository.ITokenRepository;
import ticketsystem.DomainLayer.company.Company;
import ticketsystem.DomainLayer.company.DiscountPolicy;
import ticketsystem.DomainLayer.company.PurchasePolicy;
import ticketsystem.DomainLayer.user.*;
import ticketsystem.InfrastructureLayer.CompanyRepository;
import ticketsystem.InfrastructureLayer.UserRepository;
import ticketsystem.InfrastructureLayer.TokenRepository;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Acceptance Tests for MembershipService.
 * This class uses real Domain objects and relies on existing Repository implementations.
 */
public class MembershipServiceTest {

    private ITokenService tokenService;
    private IUserRepository userRepository;
    private ICompanyRepository companyRepository;
    private MembershipDomainService domainService;
    private MembershipService membershipService;

    // Test Data
    private final Long companyId = 1L;
    private Company testCompany;
    
    private Member founderMember;
    private final Long founderId = 100L;
    
    private Member managerMember;
    private final Long managerId = 102L;
    
    private Member member;
    private final Long memberId = 103L;

    // Tokens will be generated dynamically using the real TokenService
    private String appointerToken;
    private String appointeeToken;
    private String managerToken;

    @BeforeEach
    void setUp() {
        // 1. Initialize Concrete Repositories and Services
        ITokenRepository tokenRepo = new TokenRepository();
        
        // Initialize TokenService with a long dummy secret key required for JWT signing
        this.tokenService = new TokenService("my_very_long_secret_key_for_testing_purposes_only_32_chars", tokenRepo);
        this.userRepository = new UserRepository();
        this.companyRepository = new CompanyRepository();
        
        this.domainService = new MembershipDomainService(userRepository);
        this.membershipService = new MembershipService(tokenService, userRepository, companyRepository, domainService, null);

        // 2. Setup Company state
        testCompany = new Company("BGU Productions", founderId, new PurchasePolicy(), new DiscountPolicy());
        testCompany.setId(companyId);
        companyRepository.save(testCompany);

        // 3. Setup Founder - Active state
        founderMember = new Member(founderId, "FounderUser");
        founderMember.addFounderRole(companyId);
        userRepository.addRegisteredMember(founderId, founderMember, "password123");
        
        // Generate a REAL JWT token using the service
        appointerToken = tokenService.addActiveSession(founderMember);

        // 4. Setup Manager - Pre-existing active role
        managerMember = new Member(managerId, "ManagerUser");
        Set<Permission> managerPerms = new HashSet<>();
        managerPerms.add(Permission.MANAGE_INQUIRIES);
        managerMember.addManagerRole(companyId, founderId, managerPerms);
        
        CompanyRole managerRole = managerMember.getRoleInCompany(companyId);
        if (managerRole instanceof Manager) {
            // Role starts PENDING by default, we must activate it for the test
            managerRole.setStatus(RoleStatus.ACTIVE);
        }
        userRepository.addRegisteredMember(managerId, managerMember, "password123");
        
        // Generate a REAL JWT token for the manager
        managerToken = tokenService.addActiveSession(managerMember);

        // 5. Setup Regular Member - Starting with no role
        member = new Member(memberId, "PlainMember");
        userRepository.addRegisteredMember(memberId, member, "password123");
        
        // Generate a REAL JWT token for the regular member
        appointeeToken = tokenService.addActiveSession(member);
    }
    
    // =========================================================================================
    // Use-case: Request Manager Assignment
    // =========================================================================================

    @Test
    public void GivenValidDetails_WhenRequestManagerAssignment_ThenRoleIsCreatedInPendingStatus() throws Exception {
        // Arrange
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.MANAGE_EVENT_INVENTORY);

        // Act
        membershipService.requestManagerAssignment(appointerToken, companyId, memberId, permissions);

        // Assert: Verify real state change in the repository
        Member updatedMember = userRepository.getMemberById(memberId);
        CompanyRole assignedRole = updatedMember.getRoleInCompany(companyId);
        
        assertNotNull(assignedRole, "A role should be created for the member.");
        assertTrue(assignedRole instanceof Manager, "The role must be of type Manager.");
        assertEquals(RoleStatus.PENDING, assignedRole.getStatus(), "Initial assignment must be PENDING.");
    }

    @Test
    public void GivenTargetAlreadyHasRole_WhenRequestManagerAssignment_ThenThrowsException() {
        // Arrange
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.MANAGE_EVENT_INVENTORY);

        // Act & Assert: Expecting domain validation to prevent duplicate roles
        Exception exception = assertThrows(Exception.class, () -> {
            membershipService.requestManagerAssignment(appointerToken, companyId, managerId, permissions);
        });

        assertEquals("This user already has an active or pending role in this company.", exception.getMessage());
    }

    @Test
    public void GivenAppointerIsManager_WhenRequestManagerAssignment_ThenThrowsException() {
        // Arrange
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.MANAGE_EVENT_INVENTORY);

        // Act & Assert: A Manager cannot appoint another Manager
        Exception exception = assertThrows(Exception.class, () -> {
            membershipService.requestManagerAssignment(managerToken, companyId, memberId, permissions);
        });

        assertEquals("Only Owners and Founders can appoint others.", exception.getMessage());
    }

    // =========================================================================================
    // Use-case: Approve Assignment
    // =========================================================================================

    @Test
    public void GivenPendingRole_WhenApproveAssignment_ThenStatusChangesToActive() throws Exception {
        // Arrange: Manually simulate a pending assignment state
        member.addManagerRole(companyId, founderId, new HashSet<>());
        userRepository.updateMember(member);

        // Act
        membershipService.approveAssignment(appointeeToken, companyId);

        // Assert: Verify status promotion in the repository
        Member updatedMember = userRepository.getMemberById(memberId);
        assertEquals(RoleStatus.ACTIVE, updatedMember.getRoleInCompany(companyId).getStatus());
    }

    @Test
    public void GivenRoleAlreadyActive_WhenApproveAssignment_ThenThrowsException() {
        // Arrange - managerMember is already ACTIVE
        
        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            membershipService.approveAssignment(managerToken, companyId);
        });

        assertEquals("This role is already active.", exception.getMessage());
    }

    // =========================================================================================
    // Use-case: Reject Assignment
    // =========================================================================================

    @Test
    public void GivenPendingRole_WhenRejectAssignment_ThenRoleIsSuccessfullyDeleted() throws Exception {        
        // Arrange: Manually simulate a pending assignment state
        member.addManagerRole(companyId, founderId, new HashSet<>());
        userRepository.updateMember(member);

        // Act
        membershipService.rejectAssignment(appointeeToken, companyId); 

        // Assert: Verify the role is purged from the member's profile
        Member updatedMember = userRepository.getMemberById(memberId);
        assertNull(updatedMember.getRoleInCompany(companyId), "Role should be removed after rejection.");
    }
    
    @Test
    public void GivenRoleAlreadyActive_WhenRejectAssignment_ThenThrowsException() {        
        // Arrange - managerMember is already ACTIVE
        
        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            membershipService.rejectAssignment(managerToken, companyId); 
        });

        assertEquals("This role is already active and cannot be rejected.", exception.getMessage());
    }

    // =========================================================================================
    // Use-case: Update Manager Permissions
    // =========================================================================================

    @Test
    public void GivenValidOwnerAndManager_WhenUpdatePermissions_ThenPermissionsAreSavedSuccessfully() throws Exception {
        // Arrange
        Set<Permission> newPermissions = new HashSet<>();
        newPermissions.add(Permission.MANAGE_INQUIRIES);
        newPermissions.add(Permission.CONFIGURE_HALL_AND_MAP);

        // Act - appointerToken belongs to founderId (100L) who appointed managerId (102L) in setUp()
        boolean result = membershipService.updateManagerPermissions(appointerToken, companyId, managerId, newPermissions);

        // Assert
        assertTrue(result, "Service should return true on success.");
        
        Member updatedManager = userRepository.getMemberById(managerId);
        Manager managerRole = (Manager) updatedManager.getRoleInCompany(companyId);
        
        assertTrue(managerRole.getPermissionKeys().contains(Permission.MANAGE_INQUIRIES.getKey()), "Permissions should be updated in the repository.");
        assertTrue(managerRole.getPermissionKeys().contains(Permission.CONFIGURE_HALL_AND_MAP.getKey()), "Permissions should be updated in the repository.");
    }

    @Test
    public void GivenInvalidSessionToken_WhenUpdatePermissions_ThenThrowsException() {
        // Arrange
        Set<Permission> newPermissions = new HashSet<>();
        newPermissions.add(Permission.MANAGE_INQUIRIES);
        String invalidToken = "invalid-session-token";

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            membershipService.updateManagerPermissions(invalidToken, companyId, managerId, newPermissions);
        });

        // Handles both TokenService direct validation or custom exception messages
        assertTrue(exception.getMessage().contains("Invalid") || exception.getMessage().contains("Session authentication failed."));
    }

    @Test
    public void GivenManagerDoesNotExist_WhenUpdatePermissions_ThenThrowsException() {
        // Arrange
        Long nonExistentManagerId = 999L;
        Set<Permission> newPermissions = new HashSet<>();

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            membershipService.updateManagerPermissions(appointerToken, companyId, nonExistentManagerId, newPermissions);
        });

        assertEquals("Target Manager not found.", exception.getMessage());
    }

    @Test
    public void GivenAppointerDidNotAppointManager_WhenUpdatePermissions_ThenThrowsDomainException() throws Exception {
        // Arrange: Give 'member' (ID 103L) an active Manager role, appointed by someone else (e.g., 500L)
        member.addManagerRole(companyId, 500L, new HashSet<>());
        member.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        userRepository.updateMember(member);

        Set<Permission> newPermissions = new HashSet<>();

        // Act & Assert: Founder (ID 100L) tries to update 'member' (who was appointed by 500L)
        Exception exception = assertThrows(Exception.class, () -> {
            membershipService.updateManagerPermissions(appointerToken, companyId, memberId, newPermissions);
        });

        assertEquals("You are not the appointer of the specified user", exception.getMessage());
    }
}
