package ticketsystem.AcceptanceTesting;

import ticketsystem.ApplicationLayer.INotifier;
import ticketsystem.ApplicationLayer.ISystemLogger;
import ticketsystem.ApplicationLayer.ITokenService;
import ticketsystem.ApplicationLayer.MembershipService;
import ticketsystem.ApplicationLayer.TokenService;
import ticketsystem.ApplicationLayer.UserAccessService;
import ticketsystem.DomainLayer.MembershipDomainService;
import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.DomainLayer.company.Company;
import ticketsystem.DomainLayer.IRepository.ITokenRepository;
import ticketsystem.DomainLayer.discount.DiscountCompositionType;
import ticketsystem.DomainLayer.user.*;
import ticketsystem.InfrastructureLayer.CompanyRepository;
import ticketsystem.InfrastructureLayer.LogbackSystemLogger;
import ticketsystem.InfrastructureLayer.UserRepository;
import ticketsystem.InfrastructureLayer.TokenRepository;
import ticketsystem.DomainLayer.policy.PurchasePolicy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.DomainLayer.discount.DiscountPolicy;

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
    private ISystemLogger systemLogger;
    private MembershipService membershipService;
    private FakeNotifier fakeNotifier;
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
    private UserAccessService userAccessService;
    @BeforeEach
    void setUp() {
        // 1. Initialize Concrete Repositories and Services
        ITokenRepository tokenRepo = new TokenRepository();
        this.tokenService = new TokenService("my_very_long_secret_key_for_testing_purposes_only_32_chars", tokenRepo);
        this.userRepository = new UserRepository();
        this.companyRepository = new CompanyRepository();
        this.systemLogger = new LogbackSystemLogger();        
        this.domainService = new MembershipDomainService(userRepository);
        fakeNotifier = new FakeNotifier();
        userAccessService=new UserAccessService(userRepository);
        // Initialize service with null for notifications and the logger
        this.membershipService = new MembershipService(tokenService, userRepository, companyRepository, domainService, fakeNotifier, systemLogger,userAccessService);

        // 2. Setup Company state
        testCompany = new Company("BGU Productions", founderId, PurchasePolicy.noRestrictions(), new DiscountPolicy(DiscountCompositionType.MAX));
        try { testCompany.setId(companyId); } catch (Exception e) {}

        // 3. Setup Founder - Active state
        founderMember = new Member(founderId, "FounderUser", "Founder User", "0500000001");
        founderMember.addFounderRole(companyId);
        userRepository.addRegisteredMember(founderId, founderMember, "password123");
        appointerToken = tokenService.addActiveSession(founderMember);

        // 4. Setup Manager - Pre-existing active role
        managerMember = new Member(managerId, "ManagerUser", "Manager User", "0500000002");
        Set<Permission> managerPerms = new HashSet<>();
        managerPerms.add(Permission.MANAGE_INQUIRIES);
        managerMember.addManagerRole(companyId, founderId, managerPerms);
        managerMember.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        userRepository.addRegisteredMember(managerId, managerMember, "password123");
        managerToken = tokenService.addActiveSession(managerMember);

        Founder founderRole = (Founder) founderMember.getRoleInCompany(companyId);
        founderRole.addAppointee(managerId);

        // 5. Setup Owner - Specifically for UC 4.9 and 4.10
        ownerMember = new Member(ownerId, "OwnerUser", "Owner User", "0500000003");
        ownerMember.addOwnerRole(companyId, founderId);
        ownerMember.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        userRepository.addRegisteredMember(ownerId, ownerMember, "password123");
        ownerToken = tokenService.addActiveSession(ownerMember);

        founderRole.addAppointee(ownerId);
        userRepository.updateMember(founderMember);

        // Save company after setup
        companyRepository.save(testCompany);

        // 6. Setup Regular Member - Starting with no role (For UC 4.7, 4.8)
        member = new Member(memberId, "PlainMember", "Plain Member", "0500000004");
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

        // Act & Assert (Domain Error -> RuntimeException)
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            membershipService.requestManagerAssignment(appointerToken, companyId, managerId, permissions);
        });

        assertTrue(exception.getMessage().contains("This user already has an active or pending role in this company."));
    }

    @Test
    public void GivenAppointerIsManager_WhenRequestManagerAssignment_ThenThrowsException() {
        // Arrange
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.MANAGE_EVENT_INVENTORY);

        // Act & Assert (Domain Error -> RuntimeException)
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            membershipService.requestManagerAssignment(managerToken, companyId, memberId, permissions);
        });

        assertTrue(exception.getMessage().contains("Only Owners and Founders can appoint others."));
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
        // Act & Assert (Domain Error -> RuntimeException)
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            membershipService.approveAssignment(managerToken, companyId);
        });

        assertTrue(exception.getMessage().contains("This role is already active."));
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
        // Act & Assert (Domain Error -> RuntimeException)
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            membershipService.rejectAssignment(managerToken, companyId); 
        });

        assertTrue(exception.getMessage().contains("This role is already active and cannot be rejected."));
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

        // Act
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

        // Act & Assert (Validation Error -> IllegalArgumentException)
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            membershipService.updateManagerPermissions(invalidToken, companyId, managerId, newPermissions);
        });

        assertTrue(exception.getMessage().contains("Invalid") || exception.getMessage().contains("Session authentication failed."));
    }

    @Test
    public void GivenManagerDoesNotExist_WhenUpdatePermissions_ThenThrowsException() {
        // Arrange
        Long nonExistentManagerId = 999L;
        Set<Permission> newPermissions = new HashSet<>();

        // Act & Assert (Validation Error -> IllegalArgumentException)
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            membershipService.updateManagerPermissions(appointerToken, companyId, nonExistentManagerId, newPermissions);
        });

        assertTrue(exception.getMessage().contains("Target Manager not found."));
    }

    @Test
    public void GivenAppointerDidNotAppointManager_WhenUpdatePermissions_ThenThrowsDomainException() throws Exception {
        // Arrange
        member.addManagerRole(companyId, 500L, new HashSet<>());
        member.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        userRepository.updateMember(member);

        Set<Permission> newPermissions = new HashSet<>();

        // Act & Assert (Domain Error -> RuntimeException)
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            membershipService.updateManagerPermissions(appointerToken, companyId, memberId, newPermissions);
        });

        assertTrue(exception.getMessage().contains("You are not the appointer of the specified user"));
    }

    // =========================================================================================
    // Use Case 4.12: Remove Manager Assignment
    // =========================================================================================

    @Test
    public void GivenValidDetails_WhenRemoveManagerAssignment_ThenReturnsTrue() throws Exception {
        // Act
        boolean result = membershipService.removeManagerAssignment(appointerToken, companyId, managerId);

        // Assert
        assertTrue(result, "Manager removal should return true on success.");
        assertNull(userRepository.getMemberById(managerId).getRoleInCompany(companyId), "Manager role should be removed from the member.");
    }

    @Test
    public void GivenUnauthorizedActor_WhenRemoveManagerAssignment_ThenThrowsException() {
        // Act & Assert (Domain Error -> RuntimeException)
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            membershipService.removeManagerAssignment(appointeeToken, companyId, managerId);
        });

        assertTrue(exception.getMessage().contains("You are not the appointer") || 
                   exception.getMessage().contains("You do not have a role"));
    }

    @Test
    public void GivenTargetIsNotAManager_WhenRemoveManagerAssignment_ThenThrowsException() {
        // Act & Assert (Domain Error -> RuntimeException)
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            membershipService.removeManagerAssignment(appointerToken, companyId, founderId);
        });

        assertTrue(exception.getMessage().contains("The target user is not a Manager."));
    }

    // =========================================================================================
    // Use Case 4.9: Remove Owner Assignment
    // =========================================================================================

    @Test
    public void GivenValidDetails_WhenRemoveOwnerAssignment_ThenReturnsTrueAndUpdatesDB() throws Exception {
        // Act
        boolean result = membershipService.removeOwnerAssignment(appointerToken, companyId, ownerId);

        // Assert
        assertTrue(result, "Service should return true upon successful removal.");
        assertNull(userRepository.getMemberById(ownerId).getRoleInCompany(companyId), "Target's role should be removed from the repository.");
    }

    @Test
    public void GivenInvalidToken_WhenRemoveOwnerAssignment_ThenThrowsException() {
        // Act & Assert (Validation Error -> IllegalArgumentException)
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            membershipService.removeOwnerAssignment("invalid-token", companyId, ownerId);
        });
        assertNotNull(ex);
    }

    @Test
    public void GivenTargetMemberNotFound_WhenRemoveOwnerAssignment_ThenThrowsException() {
        // Act & Assert (Validation Error -> IllegalArgumentException)
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            membershipService.removeOwnerAssignment(appointerToken, companyId, 9999L);
        });
        assertTrue(ex.getMessage().contains("Target Member not found."));
    }

    @Test
    public void GivenNotTheAppointer_WhenRemoveOwnerAssignment_ThenThrowsException() throws Exception {
        // Arrange
        member.addOwnerRole(companyId, 888L);
        userRepository.updateMember(member);

        // Act & Assert (Domain Error -> RuntimeException)
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            membershipService.removeOwnerAssignment(appointerToken, companyId, memberId);
        });
        assertTrue(ex.getMessage().contains("You are not the appointer of the specified user"));
    }

    // =========================================================================================
    // Use-case: Request Owner Assignment
    // =========================================================================================

    @Test
    public void GivenValidDetails_WhenRequestOwnerAssignment_ThenRoleIsCreatedInPendingStatus() throws Exception {
        // Act
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
        // Act & Assert (Domain Error -> RuntimeException)
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            membershipService.requestOwnerAssignment(appointerToken, companyId, managerId);
        });

        assertTrue(exception.getMessage().contains("This user already has an active or pending role in this company."));
    }

    @Test
    public void GivenAppointerIsManager_WhenRequestOwnerAssignment_ThenThrowsException() {
        // Act & Assert (Domain Error -> RuntimeException)
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            membershipService.requestOwnerAssignment(managerToken, companyId, memberId);
        });

        assertTrue(exception.getMessage().contains("Only Owners and Founders can appoint others."));
    }

    @Test
    public void GivenTargetMemberNotFound_WhenRequestOwnerAssignment_ThenThrowsException() {
        // Arrange
        Long nonExistentMemberId = 999L;

        // Act & Assert (Validation Error -> IllegalArgumentException)
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            membershipService.requestOwnerAssignment(appointerToken, companyId, nonExistentMemberId);
        });

        assertTrue(exception.getMessage().contains("Target Member not found."));
    }

    @Test
    public void GivenInvalidSessionToken_WhenRequestOwnerAssignment_ThenThrowsException() {
        // Arrange
        String invalidToken = "invalid-session-token";

        // Act & Assert (Validation Error -> IllegalArgumentException)
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            membershipService.requestOwnerAssignment(invalidToken, companyId, memberId);
        });

        assertTrue(exception.getMessage().contains("Invalid") || exception.getMessage().contains("Session authentication failed."));
    }

    // =========================================================================================
    // Use-case: Give up ownership
    // =========================================================================================

    @Test
    public void GivenOwnerWithSubordinate_WhenResignFromOwnership_ThenReturnsTrueAndSubordinateIsTransferred() throws Exception {
        // Arrange
        Member subManager = new Member(999L, "SubManager", "Sub Manager", "0500000005");
        subManager.addManagerRole(companyId, ownerId, new HashSet<>());
        subManager.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        userRepository.addRegisteredMember(999L, subManager, "password123");
        
        ((Owner) ownerMember.getRoleInCompany(companyId)).addAppointee(999L);
        userRepository.updateMember(ownerMember);

        // Act
        boolean result = membershipService.giveUpOwnership(ownerToken, companyId);

        // Assert
        assertTrue(result, "Service should return true upon successful resignation.");
        assertNull(userRepository.getMemberById(ownerId).getRoleInCompany(companyId), "Owner's role should be removed.");
        
        Member updatedFounder = userRepository.getMemberById(founderId);
        Founder founderRole = (Founder) updatedFounder.getRoleInCompany(companyId);
        assertTrue(founderRole.getAppointeesMemberIds().contains(999L), "Founder should inherit the subordinate manager.");
    }
    
    @Test
    public void GivenMemberWithNoRole_WhenResignFromOwnership_ThenThrowsException() {
        // Act & Assert (Domain Error -> RuntimeException)
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            membershipService.giveUpOwnership(appointeeToken, companyId);
        });

        assertTrue(exception.getMessage().contains("You do not have a role in this company."));
    }

    @Test
    public void GivenInvalidSessionToken_WhenResignFromOwnership_ThenThrowsException() {
        // Act & Assert (Validation Error -> IllegalArgumentException)
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            membershipService.giveUpOwnership("fake-or-expired-token", companyId);
        });

        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("Session authentication failed") || 
                   exception.getMessage().toLowerCase().contains("token"));
    }

    @Test
    public void GivenFakeCompanyId_WhenResignFromOwnership_ThenThrowsException() {
        // Arrange
        member.addOwnerRole(companyId, founderId);
        userRepository.updateMember(member);

        // Act & Assert (Domain Error -> RuntimeException)
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            membershipService.giveUpOwnership(appointeeToken, 9999L);
        });

        assertTrue(exception.getMessage().contains("You do not have a role in this company."));
    }
    
    // =========================================================================================
    // Use Case 4.15: View roles and permissions tree
    // =========================================================================================

    @Test
    public void GivenCompanyAndFounder_WhenViewRolesAndPermissionsTree_ThenReturnsTreeWithRolesAndPermissions() throws Exception {
        // Act
        String tree = membershipService.viewRolesAndPermissionsTree(appointerToken, companyId);

        // Assert
        assertNotNull(tree, "Roles and permissions tree should not be null.");

        assertTrue(tree.contains("FOUNDER"), "Tree should include the founder role.");
        assertTrue(tree.contains(String.valueOf(founderId)), "Tree should include the founder member id.");

        assertTrue(tree.contains("OWNER"), "Tree should include the owner role.");
        assertTrue(tree.contains(String.valueOf(ownerId)), "Tree should include the owner member id.");

        assertTrue(tree.contains("MANAGER"), "Tree should include the manager role.");
        assertTrue(tree.contains(String.valueOf(managerId)), "Tree should include the manager member id.");

        assertTrue(tree.contains(Permission.MANAGE_INQUIRIES.getKey()),
                "Tree should include manager permissions.");
    }

    @Test
    public void GivenCompanyAndOwner_WhenViewRolesAndPermissionsTree_ThenReturnsTreeWithRolesAndPermissions() throws Exception {
        // Act
        String tree = membershipService.viewRolesAndPermissionsTree(ownerToken, companyId);

        // Assert
        assertNotNull(tree, "Roles and permissions tree should not be null.");

        assertTrue(tree.contains("FOUNDER"), "Tree should include the founder role.");
        assertTrue(tree.contains(String.valueOf(founderId)), "Tree should include the founder member id.");

        assertTrue(tree.contains("OWNER"), "Tree should include the owner role.");
        assertTrue(tree.contains(String.valueOf(ownerId)), "Tree should include the owner member id.");

        assertTrue(tree.contains("MANAGER"), "Tree should include the manager role.");
        assertTrue(tree.contains(String.valueOf(managerId)), "Tree should include the manager member id.");

        assertTrue(tree.contains(Permission.MANAGE_INQUIRIES.getKey()),
                "Tree should include manager permissions.");
    }

    @Test
    public void GivenManager_WhenViewRolesAndPermissionsTree_ThenThrowsException() {
        // Act + Assert (Domain Error -> RuntimeException)
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            membershipService.viewRolesAndPermissionsTree(managerToken, companyId);
        });

        assertTrue(exception.getMessage().contains("Only Owners or Founder can perform this action.")
                        || exception.getMessage().contains("Only Owners"),
                "Manager should not be allowed to view the roles tree.");
    }
        private static class FakeNotifier implements INotifier {

            private final List<String> messages = new ArrayList<>();

            @Override
            public void notifyMember(Long memberId, String message) {
                messages.add(message);
            }

            @Override
            public void notifyGuest(String guestToken, String message) {
                messages.add(message);
            }

            @Override
            public void notifyMembers(Collection<Long> memberIds, String message) {
                if (memberIds == null) {
                    return;
                }

                for (Long memberId : memberIds) {
                    if (memberId != null) {
                        notifyMember(memberId, message);
                    }
                }
            }

            @Override
            public void notifyGuests(Collection<String> guestTokens, String message) {
                if (guestTokens == null) {
                    return;
                }

                for (String guestToken : guestTokens) {
                    if (guestToken != null && !guestToken.isBlank()) {
                        notifyGuest(guestToken, message);
                    }
                }
            }

            boolean containsMessage(String text) {
                return messages.stream()
                        .anyMatch(message -> message.contains(text));
            }
        }

}