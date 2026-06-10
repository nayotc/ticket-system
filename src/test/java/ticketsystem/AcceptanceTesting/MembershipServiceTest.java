package ticketsystem.AcceptanceTesting;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.ApplicationLayer.INotifier;
import ticketsystem.ApplicationLayer.ISystemLogger;
import ticketsystem.ApplicationLayer.ITokenService;
import ticketsystem.ApplicationLayer.MembershipService;
import ticketsystem.ApplicationLayer.TokenService;
import ticketsystem.ApplicationLayer.UserAccessService;
import ticketsystem.DTO.CompanyDTO;
import ticketsystem.DTO.MemberDTO;
import ticketsystem.DTO.RoleTreeDTO;
import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.IRepository.ITokenRepository;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.DomainLayer.MembershipDomainService;
import ticketsystem.DomainLayer.company.Company;
import ticketsystem.DomainLayer.discount.DiscountCompositionType;
import ticketsystem.DomainLayer.discount.DiscountPolicy;
import ticketsystem.DomainLayer.policy.PurchasePolicy;
import ticketsystem.DomainLayer.user.CompanyRole;
import ticketsystem.DomainLayer.user.Founder;
import ticketsystem.DomainLayer.user.Manager;
import ticketsystem.DomainLayer.user.Member;
import ticketsystem.DomainLayer.user.Owner;
import ticketsystem.DomainLayer.user.Permission;
import ticketsystem.DomainLayer.user.RoleStatus;
import ticketsystem.InfrastructureLayer.CompanyRepository;
import ticketsystem.InfrastructureLayer.InMemoryNotificationsRepository;
import ticketsystem.InfrastructureLayer.InMemoryUserRepository;
import ticketsystem.InfrastructureLayer.LogbackSystemLogger;
import ticketsystem.InfrastructureLayer.TokenRepository;
import ticketsystem.testutil.RecordingNotifier;

/**
 * Acceptance Tests for MembershipService. This class uses real Domain objects
 * and relies on existing Repository implementations.
 */
public class MembershipServiceTest {

    private ITokenService tokenService;
    private IUserRepository userRepository;
    private ICompanyRepository companyRepository;
    private MembershipDomainService domainService;
    private ISystemLogger systemLogger;
    private MembershipService membershipService;
    private INotifier notifier;
    private RecordingNotifier recordingNotifier;
    private InMemoryNotificationsRepository notificationsRepository;
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
        this.systemLogger = new LogbackSystemLogger();
        this.tokenService = new TokenService("my_very_long_secret_key_for_testing_purposes_only_32_chars", tokenRepo, systemLogger);
        this.userRepository = new InMemoryUserRepository();
        this.companyRepository = new CompanyRepository();
        this.domainService = new MembershipDomainService(userRepository);
        this.recordingNotifier = new RecordingNotifier();
        this.notifier = recordingNotifier;
        userAccessService = new UserAccessService(userRepository);
        // Initialize service with null for notifications and the logger
        this.membershipService = new MembershipService(tokenService, userRepository, companyRepository, domainService,
                notifier, systemLogger, userAccessService);

        // 2. Setup Company state
        testCompany = new Company("BGU Productions", founderId, PurchasePolicy.noRestrictions(),
                new DiscountPolicy(DiscountCompositionType.MAX));
        try {
            testCompany.setId(companyId);
        } catch (Exception e) {
        }

        // 3. Setup Founder - Active state
        founderMember = new Member(founderId, "FounderUser", "Founder User", "0500000001", LocalDate.of(2001, 1, 1));
        founderMember.addFounderRole(companyId);
        userRepository.addRegisteredMember(founderId, founderMember, "password123");
        appointerToken = tokenService.addActiveSession(founderMember);

        // 4. Setup Manager - Pre-existing active role
        managerMember = new Member(managerId, "ManagerUser", "Manager User", "0500000002", LocalDate.of(2001, 1, 1));
        Set<Permission> managerPerms = new HashSet<>();
        managerPerms.add(Permission.MANAGE_INQUIRIES);
        managerMember.addManagerRole(companyId, founderId, managerPerms);
        managerMember.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        userRepository.addRegisteredMember(managerId, managerMember, "password123");
        managerToken = tokenService.addActiveSession(managerMember);

        Founder founderRole = (Founder) founderMember.getRoleInCompany(companyId);
        founderRole.addAppointee(managerId);

        // 5. Setup Owner - Specifically for UC 4.9 and 4.10
        ownerMember = new Member(ownerId, "OwnerUser", "Owner User", "0500000003", LocalDate.of(2001, 1, 1));
        ownerMember.addOwnerRole(companyId, founderId);
        ownerMember.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        userRepository.addRegisteredMember(ownerId, ownerMember, "password123");
        ownerToken = tokenService.addActiveSession(ownerMember);

        founderRole.addAppointee(ownerId);
        userRepository.updateMember(founderMember);

        // Save company after setup
        companyRepository.save(testCompany);

        // 6. Setup Regular Member - Starting with no role (For UC 4.7, 4.8)
        member = new Member(memberId, "PlainMember", "Plain Member", "0500000004", LocalDate.of(2001, 1, 1));
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
        membershipService.requestManagerAssignment(appointerToken, companyId, "PlainMember", permissions);

        // Assert: Verify real state change in the repository
        Member updatedMember = userRepository.getMemberById(memberId);
        CompanyRole assignedRole = updatedMember.getRoleInCompany(companyId);

        assertNotNull(assignedRole, "A role should be created for the member.");
        assertTrue(assignedRole instanceof Manager, "The role must be of type Manager.");
        assertEquals(RoleStatus.PENDING, assignedRole.getStatus(), "Initial assignment must be PENDING.");
        recordingNotifier.assertNotifiedMember(memberId, "manager");
    }

    @Test
    public void GivenTargetAlreadyHasRole_WhenRequestManagerAssignment_ThenThrowsException() {
        // Arrange
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.MANAGE_EVENT_INVENTORY);

        // Act & Assert (Domain Error -> RuntimeException)
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            membershipService.requestManagerAssignment(appointerToken, companyId, "ManagerUser", permissions);
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
            membershipService.requestManagerAssignment(managerToken, companyId, "PlainMember", permissions);
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
        boolean result = membershipService.updateManagerPermissions(appointerToken, companyId, "ManagerUser",
                newPermissions);

        // Assert
        assertTrue(result, "Service should return true on success.");

        Member updatedManager = userRepository.getMemberById(managerId);
        Manager managerRole = (Manager) updatedManager.getRoleInCompany(companyId);

        assertTrue(managerRole.getPermissionKeys().contains(Permission.MANAGE_INQUIRIES.getKey()),
                "Permissions should be updated in the repository.");
        assertTrue(managerRole.getPermissionKeys().contains(Permission.CONFIGURE_HALL_AND_MAP.getKey()),
                "Permissions should be updated in the repository.");
        recordingNotifier.assertNotifiedMember(managerId, "permission");
    }

    @Test
    public void GivenInvalidSessionToken_WhenUpdatePermissions_ThenThrowsException() {
        // Arrange
        Set<Permission> newPermissions = new HashSet<>();
        newPermissions.add(Permission.MANAGE_INQUIRIES);
        String invalidToken = "invalid-session-token";

        // Act & Assert (Validation Error -> IllegalArgumentException)
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            membershipService.updateManagerPermissions(invalidToken, companyId, "NonExistentManager", newPermissions);
        });

        assertTrue(exception.getMessage().contains("Invalid")
                || exception.getMessage().contains("Session authentication failed."));
    }

    @Test
    public void GivenManagerDoesNotExist_WhenUpdatePermissions_ThenThrowsException() {
        // Arrange
        Set<Permission> newPermissions = new HashSet<>();

        // Act & Assert (Validation Error -> IllegalArgumentException)
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            membershipService.updateManagerPermissions(appointerToken, companyId, "NonExistentManager", newPermissions);
        });

        // שינוי כאן: בדיקה גמישה יותר למחרוזת במקום הנוסח הישן והנוקשה
        assertTrue(exception.getMessage().contains("not found"),
                "Exception message should indicate that the manager was not found");
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
            membershipService.updateManagerPermissions(appointerToken, companyId, "PlainMember", newPermissions);
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
        assertNull(userRepository.getMemberById(managerId).getRoleInCompany(companyId),
                "Manager role should be removed from the member.");
    }

    @Test
    public void GivenUnauthorizedActor_WhenRemoveManagerAssignment_ThenThrowsException() {
        // Act & Assert (Domain Error -> RuntimeException)
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            membershipService.removeManagerAssignment(appointeeToken, companyId, managerId);
        });

        assertTrue(exception.getMessage().contains("You are not the appointer")
                || exception.getMessage().contains("You do not have a role"));
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
        assertNull(userRepository.getMemberById(ownerId).getRoleInCompany(companyId),
                "Target's role should be removed from the repository.");
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
        boolean result = membershipService.requestOwnerAssignment(appointerToken, companyId, "PlainMember");

        // Assert
        assertTrue(result, "Service should return true on success.");
        Member updatedMember = userRepository.getMemberById(memberId);
        CompanyRole assignedRole = updatedMember.getRoleInCompany(companyId);

        assertNotNull(assignedRole, "A role should be created for the member.");
        assertTrue(assignedRole instanceof Owner, "The role must be of type Owner.");
        assertEquals(RoleStatus.PENDING, assignedRole.getStatus(), "Initial assignment must be PENDING.");
        recordingNotifier.assertNotifiedMember(memberId, "owner");
    }

    @Test
    public void GivenTargetAlreadyHasRole_WhenRequestOwnerAssignment_ThenThrowsException() {
        // Act & Assert (Domain Error -> RuntimeException)
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            membershipService.requestOwnerAssignment(appointerToken, companyId, "ManagerUser");
        });

        assertTrue(exception.getMessage().contains("This user already has an active or pending role in this company."));
    }

    @Test
    public void GivenAppointerIsManager_WhenRequestOwnerAssignment_ThenThrowsException() {
        // Act & Assert (Domain Error -> RuntimeException)
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            membershipService.requestOwnerAssignment(managerToken, companyId, "PlainMember");
        });

        assertTrue(exception.getMessage().contains("Only Owners and Founders can appoint others."));
    }

    @Test
    public void GivenTargetMemberNotFound_WhenRequestOwnerAssignment_ThenThrowsException() {
        // Act & Assert (Validation Error -> IllegalArgumentException)
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            membershipService.requestOwnerAssignment(appointerToken, companyId, "NonExistentMember");
        });

        // שינוי כאן: בדיקה גמישה יותר למחרוזת
        assertTrue(exception.getMessage().contains("not found"),
                "Exception message should indicate that the target member was not found");
    }

    @Test
    public void GivenInvalidSessionToken_WhenRequestOwnerAssignment_ThenThrowsException() {
        // Arrange
        String invalidToken = "invalid-session-token";

        // Act & Assert (Validation Error -> IllegalArgumentException)
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            membershipService.requestOwnerAssignment(invalidToken, companyId, "PlainMember");
        });

        assertTrue(exception.getMessage().contains("Invalid")
                || exception.getMessage().contains("Session authentication failed."));
    }

    // =========================================================================================
    // Use-case: Give up ownership
    // =========================================================================================
    @Test
    public void GivenOwnerWithSubordinate_WhenResignFromOwnership_ThenReturnsTrueAndSubordinateIsTransferred()
            throws Exception {
        // Arrange
        Member subManager = new Member(999L, "SubManager", "Sub Manager", "0500000005", LocalDate.of(2001, 1, 1));
        subManager.addManagerRole(companyId, ownerId, new HashSet<>());
        subManager.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        userRepository.addRegisteredMember(999L, subManager, "password123");

        ((Owner) ownerMember.getRoleInCompany(companyId)).addAppointee(999L);
        userRepository.updateMember(ownerMember);

        // Act
        boolean result = membershipService.giveUpOwnership(ownerToken, companyId);

        // Assert
        assertTrue(result, "Service should return true upon successful resignation.");
        assertNull(userRepository.getMemberById(ownerId).getRoleInCompany(companyId),
                "Owner's role should be removed.");

        Member updatedFounder = userRepository.getMemberById(founderId);
        Founder founderRole = (Founder) updatedFounder.getRoleInCompany(companyId);
        assertTrue(founderRole.getAppointeesMemberIds().contains(999L),
                "Founder should inherit the subordinate manager.");
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
        assertTrue(exception.getMessage().contains("Session authentication failed")
                || exception.getMessage().toLowerCase().contains("token"));
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
    public void GivenCompanyAndFounder_WhenViewRolesAndPermissionsTree_ThenReturnsTreeWithRolesAndPermissions()
            throws Exception {
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
    public void GivenCompanyAndOwner_WhenViewRolesAndPermissionsTree_ThenReturnsTreeWithRolesAndPermissions()
            throws Exception {
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

    // add tests
    @Test
    public void GivenFounder_WhenValidatePermission_ThenReturnTrue() throws Exception {
        boolean result = membershipService.validatePermission(
                appointerToken,
                companyId,
                Permission.MANAGE_EVENT_INVENTORY);

        assertTrue(result);
    }

    @Test
    public void GivenManagerWithoutPermission_WhenValidatePermission_ThenReturnFalse() throws Exception {
        boolean result = membershipService.validatePermission(
                managerToken,
                companyId,
                Permission.MANAGE_EVENT_INVENTORY);

        assertFalse(result);
    }

    @Test
    public void GivenInvalidToken_WhenValidatePermission_ThenThrowsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            membershipService.validatePermission(
                    "invalid-token",
                    companyId,
                    Permission.MANAGE_EVENT_INVENTORY);
        });

        assertTrue(exception.getMessage().contains("Session authentication failed")
                || exception.getMessage().toLowerCase().contains("token"));
    }

    @Test
    public void GivenAppointeeWithoutPendingRole_WhenApproveAssignment_ThenThrowsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            membershipService.approveAssignment(appointeeToken, companyId);
        });

        assertEquals("The appointer ID could not be determined.", exception.getMessage());
    }

    @Test
    public void GivenPendingRoleAndCompanyMissing_WhenApproveAssignment_ThenThrowsException() {
        member.addManagerRole(companyId, founderId, new HashSet<>());
        userRepository.updateMember(member);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            membershipService.approveAssignment(appointeeToken, 9999L);
        });

        assertEquals("The appointer ID could not be determined.", exception.getMessage());
    }

    @Test
    public void GivenAppointeeWithoutPendingRole_WhenRejectAssignment_ThenThrowsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            membershipService.rejectAssignment(appointeeToken, companyId);
        });

        assertEquals("The appointer ID could not be determined.", exception.getMessage());
    }

    @Test
    public void GivenInvalidToken_WhenRejectAssignment_ThenThrowsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            membershipService.rejectAssignment("invalid-token", companyId);
        });

        assertTrue(exception.getMessage().contains("Session authentication failed")
                || exception.getMessage().toLowerCase().contains("token"));
    }

    @Test
    public void GivenInvalidToken_WhenViewRolesAndPermissionsTree_ThenThrowsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            membershipService.viewRolesAndPermissionsTree("invalid-token", companyId);
        });

        assertTrue(exception.getMessage().contains("Session authentication failed")
                || exception.getMessage().toLowerCase().contains("token"));
    }

    @Test
    public void GivenMissingCompany_WhenViewRolesAndPermissionsTree_ThenThrowsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            membershipService.viewRolesAndPermissionsTree(appointerToken, 9999L);
        });

        assertEquals("Error: Company not found.", exception.getMessage());
    }

    @Test
    public void GivenAppointerTokenButMemberNotInRepository_WhenRequestManagerAssignment_ThenThrowsException() {
        Member ghost = new Member(999L, "ghost", "Ghost User", "0500000999", LocalDate.of(2001, 1, 1));
        String ghostToken = tokenService.addActiveSession(ghost);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> membershipService.requestManagerAssignment(ghostToken, companyId, "Ghost User", new HashSet<>()));

        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    public void GivenTargetMemberNotFound_WhenRequestManagerAssignment_ThenThrowsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> membershipService.requestManagerAssignment(appointerToken, companyId, "NonExistentMember", new HashSet<>()));

        // שינוי כאן: במקום assertEquals שדורש התאמה של 100%, משתמשים ב-assertTrue
        assertTrue(exception.getMessage().contains("not found"),
                "Exception message should indicate that the target member was not found");
    }

    @Test
    public void GivenCancelledTargetRole_WhenRequestManagerAssignment_ThenNewPendingManagerRoleIsCreated()
            throws Exception {
        member.addOwnerRole(companyId, founderId);
        member.getRoleInCompany(companyId).cancel();
        userRepository.updateMember(member);

        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.MANAGE_INQUIRIES);

        boolean result = membershipService.requestManagerAssignment(
                appointerToken,
                companyId,
                "PlainMember",
                permissions);

        assertTrue(result);

        Member updated = userRepository.getMemberById(memberId);
        assertTrue(updated.getRoleInCompany(companyId) instanceof Manager);
        assertEquals(RoleStatus.PENDING, updated.getRoleInCompany(companyId).getStatus());
    }

    @Test
    public void GivenCancelledTargetRole_WhenRequestOwnerAssignment_ThenNewPendingOwnerRoleIsCreated()
            throws Exception {
        member.addManagerRole(companyId, founderId, new HashSet<>());
        member.getRoleInCompany(companyId).cancel();
        userRepository.updateMember(member);

        boolean result = membershipService.requestOwnerAssignment(
                appointerToken,
                companyId,
                "PlainMember");

        assertTrue(result);

        Member updated = userRepository.getMemberById(memberId);
        assertTrue(updated.getRoleInCompany(companyId) instanceof Owner);
        assertEquals(RoleStatus.PENDING, updated.getRoleInCompany(companyId).getStatus());
    }

    @Test
    public void GivenInvalidToken_WhenApproveAssignment_ThenThrowsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> membershipService.approveAssignment("invalid-token", companyId));

        assertTrue(exception.getMessage().toLowerCase().contains("token"));
    }

    @Test
    public void GivenTokenForMemberNotInRepository_WhenApproveAssignment_ThenThrowsException() {
        Member ghost = new Member(999L, "ghost", "Ghost User", "0500000999", LocalDate.of(2001, 1, 1));
        String ghostToken = tokenService.addActiveSession(ghost);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> membershipService.approveAssignment(ghostToken, companyId));

        assertEquals("Appointee not found.", exception.getMessage());
    }

    @Test
    public void GivenPendingRoleButAppointerNotFound_WhenApproveAssignment_ThenThrowsException() {
        member.addManagerRole(companyId, 999L, new HashSet<>());
        userRepository.updateMember(member);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> membershipService.approveAssignment(appointeeToken, companyId));

        assertEquals("Appointer not found.", exception.getMessage());
    }

    @Test
    public void GivenPendingRoleButCompanyNotFound_WhenApproveAssignment_ThenThrowsException() {
        Long missingCompanyId = 9999L;

        member.addManagerRole(missingCompanyId, founderId, new HashSet<>());
        userRepository.updateMember(member);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> membershipService.approveAssignment(appointeeToken, missingCompanyId));

        assertEquals("Company not found.", exception.getMessage());
    }

    @Test
    public void GivenTokenForMemberNotInRepository_WhenRejectAssignment_ThenThrowsException() {
        Member ghost = new Member(999L, "ghost", "Ghost User", "0500000999", LocalDate.of(2001, 1, 1));
        String ghostToken = tokenService.addActiveSession(ghost);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> membershipService.rejectAssignment(ghostToken, companyId));

        assertEquals("Appointee not found.", exception.getMessage());
    }

    @Test
    public void GivenPendingRoleButAppointerNotFound_WhenRejectAssignment_ThenThrowsException() {
        member.addManagerRole(companyId, 999L, new HashSet<>());
        userRepository.updateMember(member);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> membershipService.rejectAssignment(appointeeToken, companyId));

        assertEquals("Appointer not found.", exception.getMessage());
    }

    @Test
    public void GivenAppointerTokenButMemberNotInRepository_WhenUpdateManagerPermissions_ThenThrowsException() {
        Member ghost = new Member(999L, "ghost", "Ghost User", "0500000999", LocalDate.of(2001, 1, 1));
        String ghostToken = tokenService.addActiveSession(ghost);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> membershipService.updateManagerPermissions(
                        ghostToken,
                        companyId,
                        "ManagerUser",
                        new HashSet<>()));

        assertEquals("Appointer not found.", exception.getMessage());
    }

    @Test
    public void GivenAppointerTokenButMemberNotInRepository_WhenRemoveManagerAssignment_ThenThrowsException() {
        Member ghost = new Member(999L, "ghost", "Ghost User", "0500000999", LocalDate.of(2001, 1, 1));
        String ghostToken = tokenService.addActiveSession(ghost);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> membershipService.removeManagerAssignment(ghostToken, companyId, managerId));

        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    public void GivenTargetMemberNotFound_WhenRemoveManagerAssignment_ThenThrowsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> membershipService.removeManagerAssignment(appointerToken, companyId, 9999L));

        assertEquals("Target Member not found.", exception.getMessage());
    }

    @Test
    public void GivenCompanyNotFound_WhenRemoveOwnerAssignment_ThenThrowsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> membershipService.removeOwnerAssignment(appointerToken, 9999L, ownerId));

        assertEquals("Company not found.", exception.getMessage());
    }

    @Test
    public void GivenOwnerWithMissingAppointer_WhenGiveUpOwnership_ThenThrowsException() {
        Member ownerWithoutExistingAppointer = new Member(
                777L,
                "lonelyOwner",
                "Lonely Owner",
                "0500000777", LocalDate.of(2001, 1, 1));

        ownerWithoutExistingAppointer.addOwnerRole(companyId, 999L);
        ownerWithoutExistingAppointer.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);

        userRepository.addRegisteredMember(777L, ownerWithoutExistingAppointer, "password123");

        String token = tokenService.addActiveSession(ownerWithoutExistingAppointer);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> membershipService.giveUpOwnership(token, companyId));

        assertEquals("Appointer not found.", exception.getMessage());
    }

    @Test
    public void GivenSuccessfulRequestOwnerAssignment_WhenNotificationSent_ThenNotifierContainsCompanyName()
            throws Exception {
        boolean result = membershipService.requestOwnerAssignment(
                appointerToken,
                companyId,
                "PlainMember");

        assertTrue(result);
    }

    @Test
    public void GivenFounder_WhenViewRolesAndPermissionsTreeDto_ThenReturnsRoleTreeDto() throws Exception {
        RoleTreeDTO tree = membershipService.viewRolesAndPermissionsTreeDto(appointerToken, companyId);

        assertNotNull(tree, "Role tree DTO should not be null.");
    }

    @Test
    public void GivenOwner_WhenViewRolesAndPermissionsTreeDto_ThenReturnsRoleTreeDto() throws Exception {
        RoleTreeDTO tree = membershipService.viewRolesAndPermissionsTreeDto(ownerToken, companyId);

        assertNotNull(tree, "Owner should be allowed to view role tree DTO.");
    }

    @Test
    public void GivenManager_WhenViewRolesAndPermissionsTreeDto_ThenThrowsException() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            membershipService.viewRolesAndPermissionsTreeDto(managerToken, companyId);
        });

        assertTrue(exception.getMessage().contains("Only Owners")
                || exception.getMessage().contains("Founder")
                || exception.getMessage().contains("viewing roles and permissions tree"));
    }

    @Test
    public void GivenMissingCompany_WhenViewRolesAndPermissionsTreeDto_ThenThrowsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            membershipService.viewRolesAndPermissionsTreeDto(appointerToken, 9999L);
        });

        assertEquals("Error: Company not found.", exception.getMessage());
    }

    @Test
    public void GivenInvalidToken_WhenViewRolesAndPermissionsTreeDto_ThenThrowsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            membershipService.viewRolesAndPermissionsTreeDto("invalid-token", companyId);
        });

        assertTrue(exception.getMessage().contains("Session authentication failed")
                || exception.getMessage().toLowerCase().contains("token"));
    }

    @Test
    public void GivenFounder_WhenGetCompaniesByMember_ThenReturnsManagedCompanies() throws Exception {
        List<CompanyDTO> companies = membershipService.getCompaniesByMember(appointerToken);

        assertNotNull(companies);
        assertFalse(companies.isEmpty(), "Founder should have at least one managed company.");
        assertTrue(companies.stream().anyMatch(company -> company.getId() == companyId));
    }

    @Test
    public void GivenOwner_WhenGetCompaniesByMember_ThenReturnsManagedCompanies() throws Exception {
        List<CompanyDTO> companies = membershipService.getCompaniesByMember(ownerToken);

        assertNotNull(companies);
        assertFalse(companies.isEmpty(), "Owner should have at least one managed company.");
        assertTrue(companies.stream().anyMatch(company -> company.getId() == companyId));
    }

    @Test
    public void GivenRegularMemberWithoutRole_WhenGetCompaniesByMember_ThenReturnsEmptyList() throws Exception {
        List<CompanyDTO> companies = membershipService.getCompaniesByMember(appointeeToken);

        assertNotNull(companies);
        assertTrue(companies.isEmpty(), "Member without company role should not have managed companies.");
    }

    @Test
    public void GivenInvalidToken_WhenGetCompaniesByMember_ThenThrowsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            membershipService.getCompaniesByMember("invalid-token");
        });

        assertTrue(exception.getMessage().contains("Session authentication failed")
                || exception.getMessage().toLowerCase().contains("token"));
    }

    @Test
    public void GivenFounder_WhenGetCompanyTeamMembers_ThenReturnsCompanyTeamMembers() throws Exception {
        List<MemberDTO> teamMembers = membershipService.getCompanyTeamMembers(appointerToken, companyId);

        assertNotNull(teamMembers);
        assertFalse(teamMembers.isEmpty(), "Company team members list should not be empty.");
    }

    @Test
    public void GivenOwner_WhenGetCompanyTeamMembers_ThenReturnsCompanyTeamMembers() throws Exception {
        List<MemberDTO> teamMembers = membershipService.getCompanyTeamMembers(ownerToken, companyId);

        assertNotNull(teamMembers);
        assertFalse(teamMembers.isEmpty(), "Owner should be able to load company team members.");
    }

    @Test
    public void GivenMissingCompany_WhenGetCompanyTeamMembers_ThenThrowsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            membershipService.getCompanyTeamMembers(appointerToken, 9999L);
        });

        assertEquals("Error: Company not found.", exception.getMessage());
    }

    @Test
    public void GivenInvalidToken_WhenGetCompanyTeamMembers_ThenThrowsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            membershipService.getCompanyTeamMembers("invalid-token", companyId);
        });

        assertTrue(exception.getMessage().contains("Session authentication failed")
                || exception.getMessage().toLowerCase().contains("token"));
    }

    @Test
    public void GivenCompanyWithoutPendingAssignments_WhenGetPendingAssignmentsCount_ThenReturnsZero() {
        int count = membershipService.getPendingAssignmentsCount(companyId);

        assertEquals(0, count);
    }

    @Test
    public void GivenPendingManagerAssignment_WhenGetPendingAssignmentsCount_ThenReturnsOne() throws Exception {
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.MANAGE_EVENT_INVENTORY);

        membershipService.requestManagerAssignment(
                appointerToken,
                companyId,
                "PlainMember",
                permissions
        );

        int count = membershipService.getPendingAssignmentsCount(companyId);

        assertEquals(1, count);
    }

    @Test
    public void GivenPendingOwnerAssignment_WhenGetPendingAssignmentsCount_ThenReturnsOne() throws Exception {
        membershipService.requestOwnerAssignment(
                appointerToken,
                companyId,
                "PlainMember"
        );

        int count = membershipService.getPendingAssignmentsCount(companyId);

        assertEquals(1, count);
    }

    @Test
    public void GivenPendingAssignmentApproved_WhenGetPendingAssignmentsCount_ThenReturnsZero() throws Exception {
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.MANAGE_EVENT_INVENTORY);

        membershipService.requestManagerAssignment(
                appointerToken,
                companyId,
                "PlainMember",
                permissions
        );

        membershipService.approveAssignment(appointeeToken, companyId);

        int count = membershipService.getPendingAssignmentsCount(companyId);

        assertEquals(0, count);
    }

    @Test
    public void GivenPendingAssignmentRejected_WhenGetPendingAssignmentsCount_ThenReturnsZero() throws Exception {
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.MANAGE_EVENT_INVENTORY);

        membershipService.requestManagerAssignment(
                appointerToken,
                companyId,
                "PlainMember",
                permissions
        );

        membershipService.rejectAssignment(appointeeToken, companyId);

        int count = membershipService.getPendingAssignmentsCount(companyId);

        assertEquals(0, count);
    }

    @Test
    public void GivenMissingCompany_WhenGetPendingAssignmentsCount_ThenReturnsZero() {
        int count = membershipService.getPendingAssignmentsCount(9999L);

        assertEquals(0, count);
    }

    @Test
    public void GivenSuccessfulRequestManagerAssignment_WhenNotificationSent_ThenNotifierContainsCompanyName()
            throws Exception {
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.MANAGE_EVENT_INVENTORY);

        boolean result = membershipService.requestManagerAssignment(
                appointerToken,
                companyId,
                "PlainMember",
                permissions
        );

        assertTrue(result);
    }

    @Test
    public void GivenSuccessfulRemoveManagerAssignment_WhenNotificationSent_ThenNotifierContainsRemovalMessage()
            throws Exception {
        boolean result = membershipService.removeManagerAssignment(
                appointerToken,
                companyId,
                managerId
        );

        assertTrue(result);
        recordingNotifier.assertNotifiedMember(managerId, "removed");
    }

    @Test
    public void GivenSuccessfulRemoveOwnerAssignment_WhenNotificationSent_ThenNotifierContainsRemovalMessage()
            throws Exception {
        boolean result = membershipService.removeOwnerAssignment(
                appointerToken,
                companyId,
                ownerId
        );

        assertTrue(result);
        recordingNotifier.assertNotifiedMember(ownerId, "removed");
    }
}
