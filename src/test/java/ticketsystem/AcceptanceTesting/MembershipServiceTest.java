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

    private Member ownerMember;
    private final Long ownerId = 105L;

    // Tokens will be generated dynamically using the real TokenService
    private String appointerToken;
    private String appointeeToken;
    private String managerToken;
    private String ownerToken;

    @BeforeEach
    void setUp() {
        // 1. Initialize Concrete Repositories and Services
        ITokenRepository tokenRepo = new TokenRepository();
        this.tokenService = new TokenService("my_very_long_secret_key_for_testing_purposes_only_32_chars", tokenRepo);
        this.userRepository = new UserRepository();
        this.companyRepository = new CompanyRepository();
        
        this.domainService = new MembershipDomainService(userRepository);
        this.membershipService = new MembershipService(tokenService, userRepository, companyRepository, domainService, null);

        // 2. Setup Company state
        testCompany = new Company("BGU Productions", founderId, new PurchasePolicy(), new DiscountPolicy());
        try { testCompany.setId(companyId); } catch (Exception e) {}

        // 3. Setup Founder - Active state
        founderMember = new Member(founderId, "FounderUser");
        founderMember.addFounderRole(companyId);
        userRepository.addRegisteredMember(founderId, founderMember, "password123");
        appointerToken = tokenService.addActiveSession(founderMember);

        // 4. Setup Manager - Pre-existing active role
        managerMember = new Member(managerId, "ManagerUser");
        Set<Permission> managerPerms = new HashSet<>();
        managerPerms.add(Permission.MANAGE_INQUIRIES);
        managerMember.addManagerRole(companyId, founderId, managerPerms);
        managerMember.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        userRepository.addRegisteredMember(managerId, managerMember, "password123");
        managerToken = tokenService.addActiveSession(managerMember);
        
        // SYNC WITH COMPANY TREE
        testCompany.registerNewAppointment(founderId, managerId, "MANAGER");

        // 5. Setup Owner - Specifically for UC 4.9 (Remove Owner) and 4.10
        ownerMember = new Member(ownerId, "OwnerUser");
        ownerMember.addOwnerRole(companyId, founderId);
        ownerMember.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        userRepository.addRegisteredMember(ownerId, ownerMember, "password123");
        ownerToken = tokenService.addActiveSession(ownerMember);
        
        // SYNC WITH COMPANY TREE
        testCompany.registerNewAppointment(founderId, ownerId, "OWNER");

        // Save company after all initial appointments
        companyRepository.save(testCompany);

        // 6. Setup Regular Member - Starting with no role (For UC 4.7, 4.8)
        member = new Member(memberId, "PlainMember");
        userRepository.addRegisteredMember(memberId, member, "password123");
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

    // =========================================================================================
    // Use Case 4.9: Remove Owner Assignment
    // =========================================================================================

    @Test
    public void GivenValidDetails_WhenRemoveOwnerAssignment_ThenReturnsTrueAndUpdatesDB() throws Exception {
        // Arrange is fully handled by setUp()! ownerMember (105L) is already an active Owner in the company tree.

        // Act: Founder attempts to remove the Owner
        boolean result = membershipService.removeOwnerAssignment(appointerToken, companyId, ownerId);

        // Assert
        assertTrue(result, "Service should return true upon successful removal.");
        assertNull(userRepository.getMemberById(ownerId).getRoleInCompany(companyId), "Target's role should be removed from the repository.");
    }

    @Test
    public void GivenInvalidToken_WhenRemoveOwnerAssignment_ThenThrowsException() {
        Exception ex = assertThrows(Exception.class, () -> {
            membershipService.removeOwnerAssignment("invalid-token", companyId, ownerId);
        });
        assertNotNull(ex);
    }

    @Test
    public void GivenTargetMemberNotFound_WhenRemoveOwnerAssignment_ThenThrowsException() {
        Exception ex = assertThrows(Exception.class, () -> {
            membershipService.removeOwnerAssignment(appointerToken, companyId, 9999L);
        });
        assertEquals("Target Member not found.", ex.getMessage());
    }

    @Test
    public void GivenNotTheAppointer_WhenRemoveOwnerAssignment_ThenThrowsException() throws Exception {
        // Arrange: Make 'member' an Owner appointed by a DIFFERENT user (888L)
        member.addOwnerRole(companyId, 888L);
        userRepository.updateMember(member);

        // Act & Assert: Founder (100L) tries to remove an Owner appointed by 888L
        Exception ex = assertThrows(Exception.class, () -> {
            membershipService.removeOwnerAssignment(appointerToken, companyId, memberId);
        });
        assertEquals("You are not the appointer of the specified user", ex.getMessage());
    }

    // =========================================================================================
    // Use-case: Request Owner Assignment
    // =========================================================================================

    @Test
    public void GivenValidDetails_WhenRequestOwnerAssignment_ThenRoleIsCreatedInPendingStatus() throws Exception {
        // Act
        // appointerToken belongs to founderMember, memberId is a plain user without a role
        boolean result = membershipService.requestOwnerAssignment(appointerToken, companyId, memberId);

        // Assert
        assertTrue(result, "Service should return true on success.");
        Member updatedMember = userRepository.getMemberById(memberId);
        CompanyRole assignedRole = updatedMember.getRoleInCompany(companyId);
        
        assertNotNull(assignedRole, "A role should be created for the member.");
        assertTrue(assignedRole instanceof Owner, "The role must be of type Owner.");
        assertEquals(RoleStatus.PENDING, assignedRole.getStatus(), "Initial assignment must be PENDING.");
    }

    @Test
    public void GivenTargetAlreadyHasRole_WhenRequestOwnerAssignment_ThenThrowsException() {
        // Act & Assert
        // managerId already has an active Manager role defined in setUp()
        Exception exception = assertThrows(Exception.class, () -> {
            membershipService.requestOwnerAssignment(appointerToken, companyId, managerId);
        });

        assertEquals("This user already has an active or pending role in this company.", exception.getMessage());
    }

    @Test
    public void GivenAppointerIsManager_WhenRequestOwnerAssignment_ThenThrowsException() {
        // Act & Assert
        // A Manager tries to appoint an Owner
        Exception exception = assertThrows(Exception.class, () -> {
            membershipService.requestOwnerAssignment(managerToken, companyId, memberId);
        });

        assertEquals("Only Owners and Founders can appoint others.", exception.getMessage());
    }

    @Test
    public void GivenTargetMemberNotFound_WhenRequestOwnerAssignment_ThenThrowsException() {
        // Arrange
        Long nonExistentMemberId = 999L;

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            membershipService.requestOwnerAssignment(appointerToken, companyId, nonExistentMemberId);
        });

        assertEquals("Target Member not found.", exception.getMessage());
    }

    @Test
    public void GivenInvalidSessionToken_WhenRequestOwnerAssignment_ThenThrowsException() {
        // Arrange
        String invalidToken = "invalid-session-token";

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            membershipService.requestOwnerAssignment(invalidToken, companyId, memberId);
        });

        assertTrue(exception.getMessage().contains("Invalid") || exception.getMessage().contains("Session authentication failed."));
    }

    
}
