package ticketsystem.UnitTesting;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.DomainLayer.MembershipDomainService;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.InfrastructureLayer.UserRepository;
import ticketsystem.DomainLayer.company.Company;
import ticketsystem.DomainLayer.company.DiscountPolicy;
import ticketsystem.DomainLayer.company.PurchasePolicy;
import ticketsystem.DomainLayer.user.*;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class MembershipDomainServiceTest {

    private MembershipDomainService domainService;
    private IUserRepository userRepository;
    
    // Using REAL Domain Objects!
    private Member appointer;
    private Member appointee;
    private Company company;
    private final Long companyId = 1L;

    private Member existingOwner;
    private final Long existingOwnerId = 300L;

    @BeforeEach
    public void setUp() {
        userRepository = new UserRepository();
        domainService = new MembershipDomainService(userRepository);
        
        appointer = new Member(100L, "AppointerUser");
        appointee = new Member(200L, "AppointeeUser"); // Kept blank for UC 4.7 & 4.8
        
        userRepository.addRegisteredMember(100L, appointer, "password123");
        userRepository.addRegisteredMember(200L, appointee, "password123");
        
        company = new Company("BGU Productions", 100L, new PurchasePolicy(), new DiscountPolicy());
        try { company.setId(companyId); } catch (Exception e) {}

        // Setup a pre-existing Owner specifically for UC 4.9 & 4.10 tests
        existingOwner = new Member(existingOwnerId, "ExistingOwner");
        existingOwner.addOwnerRole(companyId, 100L); // Appointed by appointer
        existingOwner.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        userRepository.addRegisteredMember(existingOwnerId, existingOwner, "password123");
    }

    // --- Validate Permission ---

    @Test
    public void GivenNullRole_WhenValidatePermission_ThenReturnFalse() {
        assertFalse(domainService.validatePermission(100L, companyId, Permission.MANAGE_EVENT_INVENTORY));
    }

    @Test
    public void GivenPendingRole_WhenValidatePermission_ThenReturnFalse() {
        appointer.addOwnerRole(companyId, 999L); 
        assertFalse(domainService.validatePermission(100L, companyId, Permission.MANAGE_EVENT_INVENTORY));
    }

    @Test
    public void GivenActiveRoleWithPermission_WhenValidatePermission_ThenReturnTrue() {
        appointer.addFounderRole(companyId); 
        assertTrue(domainService.validatePermission(100L, companyId, Permission.MANAGE_EVENT_INVENTORY));
    }

    // --- Manager Assignment Request ---

    @Test
    public void GivenAppointerHasNoRole_WhenManagerAssignmentRequest_ThenThrowException() {
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.managerAssignmentRequest(appointer, appointee, companyId, new HashSet<>());
        });
        assertEquals("You do not have a role in this company.", exception.getMessage());
    }

    @Test
    public void GivenAppointerIsPending_WhenManagerAssignmentRequest_ThenThrowException() {
        appointer.addOwnerRole(companyId, 999L); 
        
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.managerAssignmentRequest(appointer, appointee, companyId, new HashSet<>());
        });
        assertEquals("Your role is not active yet. You cannot appoint others.", exception.getMessage());
    }

    @Test
    public void GivenAppointerIsManager_WhenManagerAssignmentRequest_ThenThrowException() {
        appointer.addManagerRole(companyId, 999L, new HashSet<>());
        appointer.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.managerAssignmentRequest(appointer, appointee, companyId, new HashSet<>());
        });
        assertEquals("Only Owners and Founders can appoint others.", exception.getMessage());
    }

    @Test
    public void GivenAppointeeHasRole_WhenManagerAssignmentRequest_ThenThrowException() {
        appointer.addFounderRole(companyId);
        appointee.addManagerRole(companyId, 100L, new HashSet<>()); 
        
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.managerAssignmentRequest(appointer, appointee, companyId, new HashSet<>());
        });
        assertEquals("This user already has an active or pending role in this company.", exception.getMessage());
    }

    @Test
    public void GivenValidOwner_WhenManagerAssignmentRequest_ThenAddRole() throws Exception {
        appointer.addFounderRole(companyId);
        Set<Permission> perms = new HashSet<>();
        
        boolean result = domainService.managerAssignmentRequest(appointer, appointee, companyId, perms);
        
        assertTrue(result);
        CompanyRole assignedRole = appointee.getRoleInCompany(companyId);
        assertNotNull(assignedRole);
        assertTrue(assignedRole instanceof Manager);
        assertEquals(RoleStatus.PENDING, assignedRole.getStatus());
        assertEquals(100L, ((Manager) assignedRole).getAppointedByMemberId());
    }

    // --- Approve Assignment ---

    @Test
    public void GivenNoPendingRole_WhenApproveAssignment_ThenThrowException() {
        appointer.addFounderRole(companyId);
        
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.approveAssignment(appointer, appointee, company);
        });
        assertEquals("No pending role invitation found.", exception.getMessage());
    }

    @Test
    public void GivenRoleAlreadyActive_WhenApproveAssignment_ThenThrowException() {
        appointer.addFounderRole(companyId);
        appointee.addManagerRole(companyId, 100L, new HashSet<>());
        appointee.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.approveAssignment(appointer, appointee, company);
        });
        assertEquals("This role is already active.", exception.getMessage());
    }
    
    @Test
    public void GivenValidApproval_WhenApproveAssignment_ThenStatusBecomesActive() throws Exception {
        appointer.addFounderRole(companyId);
        appointee.addManagerRole(companyId, 100L, new HashSet<>());

        domainService.approveAssignment(appointer, appointee, company);

        assertEquals(RoleStatus.ACTIVE, appointee.getRoleInCompany(companyId).getStatus());
        Founder founderRole = (Founder) appointer.getRoleInCompany(companyId);
        assertTrue(founderRole.getAppointeesMemberIds().contains(200L));
    }

    // --- Reject Assignment ---

    @Test
    public void GivenNoPendingRole_WhenRejectAssignment_ThenThrowException() {
        appointer.addFounderRole(companyId);
        
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.rejectAssignment(appointer, appointee, companyId);
        });
        assertEquals("No pending role invitation found.", exception.getMessage());
    }

    @Test
    public void GivenValidRejection_WhenRejectAssignment_ThenRoleDeletedAndAppointeeRemoved() throws Exception {
        appointer.addFounderRole(companyId);
        Founder founderRole = (Founder) appointer.getRoleInCompany(companyId);
        founderRole.addAppointee(200L); 

        appointee.addManagerRole(companyId, 100L, new HashSet<>());

        domainService.rejectAssignment(appointer, appointee, companyId);

        assertNull(appointee.getRoleInCompany(companyId));
        assertFalse(founderRole.getAppointeesMemberIds().contains(200L));
    }

    // --- Update Manager Permissions ---

    @Test
    public void GivenValidAppointer_WhenSetPermissions_ThenPermissionsAreUpdated() throws Exception {
        appointer.addFounderRole(companyId); 
        
        Set<Permission> initialPerms = new HashSet<>();
        appointee.addManagerRole(companyId, 100L, initialPerms); // Appointed by appointer (ID 100L)
        appointee.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        
        Set<Permission> newPerms = new HashSet<>();
        newPerms.add(Permission.MANAGE_EVENT_INVENTORY);
        
        // Act
        boolean result = domainService.setPermissionsToManager(appointer, appointee, companyId, newPerms);
        
        // Assert
        assertTrue(result);
        Manager managerRole = (Manager) appointee.getRoleInCompany(companyId);
        assertTrue(managerRole.getPermissionKeys().contains(Permission.MANAGE_EVENT_INVENTORY.getKey()));
    }

    @Test
    public void GivenAppointerHasNoRole_WhenSetPermissions_ThenThrowsException() {
        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.setPermissionsToManager(appointer, appointee, companyId, new HashSet<>());
        });
        assertEquals("You do not have a role in this company.", exception.getMessage());
    }

    @Test
    public void GivenAppointerIsNotActive_WhenSetPermissions_ThenThrowsException() {
        // Arrange
        appointer.addOwnerRole(companyId, 999L); // Owner starts as PENDING
        
        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.setPermissionsToManager(appointer, appointee, companyId, new HashSet<>());
        });
        assertEquals("Your role is not active yet. You cannot update others permissions.", exception.getMessage());
    }

    @Test
    public void GivenAppointerIsManager_WhenSetPermissions_ThenThrowsException() {
        // Arrange
        appointer.addManagerRole(companyId, 999L, new HashSet<>());
        appointer.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        
        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.setPermissionsToManager(appointer, appointee, companyId, new HashSet<>());
        });
        assertEquals("Only Owners and Founders can update manager's permissions.", exception.getMessage());
    }

    @Test
    public void GivenTargetIsNotManager_WhenSetPermissions_ThenThrowsException() {
        // Arrange
        appointer.addFounderRole(companyId);
        appointee.addOwnerRole(companyId, 100L); // Target is Owner, not Manager
        
        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.setPermissionsToManager(appointer, appointee, companyId, new HashSet<>());
        });
        assertEquals("The specified user does not hold a Manager role", exception.getMessage());
    }

    @Test
    public void GivenActorIsNotTheAppointer_WhenSetPermissions_ThenThrowsException() {
        // Arrange
        appointer.addFounderRole(companyId);
        appointee.addManagerRole(companyId, 999L, new HashSet<>()); // Appointed by 999L, not 100L
        appointee.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        
        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.setPermissionsToManager(appointer, appointee, companyId, new HashSet<>());
        });
        assertEquals("You are not the appointer of the specified user", exception.getMessage());
    }

    @Test
    public void GivenTargetIsPending_WhenSetPermissions_ThenThrowsException() {
        // Arrange
        appointer.addFounderRole(companyId);
        
        // Target is appointed but stays in default PENDING status
        appointee.addManagerRole(companyId, 100L, new HashSet<>()); 
        
        Set<Permission> newPerms = new HashSet<>();
        newPerms.add(Permission.MANAGE_INQUIRIES);
        
        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.setPermissionsToManager(appointer, appointee, companyId, newPerms);
        });
        assertEquals("Cannot update permissions for a pending manager.", exception.getMessage());
    }

    @Test
    public void GivenNullPermissionsSet_WhenSetPermissions_ThenThrowsException() {
        // Arrange
        appointer.addFounderRole(companyId);
        appointee.addManagerRole(companyId, 100L, new HashSet<>());
        appointee.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        
        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.setPermissionsToManager(appointer, appointee, companyId, null);
        });
        assertEquals("Permissions set cannot be null or contain null values.", exception.getMessage());
    }

    @Test
    public void GivenPermissionsSetWithNullElement_WhenSetPermissions_ThenThrowsException() {
        // Arrange
        appointer.addFounderRole(companyId);
        appointee.addManagerRole(companyId, 100L, new HashSet<>());
        appointee.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        
        Set<Permission> invalidPerms = new HashSet<>();
        invalidPerms.add(Permission.MANAGE_EVENT_INVENTORY);
        invalidPerms.add(null); // Injecting a null value
        
        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.setPermissionsToManager(appointer, appointee, companyId, invalidPerms);
        });
        assertEquals("Permissions set cannot be null or contain null values.", exception.getMessage());
    }

    // --- Owner Assignment Request ---   

    @Test
    public void GivenValidOwner_WhenOwnerAssignmentRequest_ThenAddRole() throws Exception {
        // Arrange
        appointer.addFounderRole(companyId); // Founder is active and valid to appoint
        
        // Act
        boolean result = domainService.ownerAssignmentRequest(appointer, appointee, companyId);
        
        // Assert: State verification on real objects
        assertTrue(result);
        CompanyRole assignedRole = appointee.getRoleInCompany(companyId);
        assertNotNull(assignedRole);
        assertTrue(assignedRole instanceof Owner);
        assertEquals(RoleStatus.PENDING, assignedRole.getStatus());
        assertEquals(100L, ((Owner) assignedRole).getAppointedByMemberId());
    }

    @Test
    public void GivenAppointerHasNoRole_WhenOwnerAssignmentRequest_ThenThrowException() {
        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.ownerAssignmentRequest(appointer, appointee, companyId);
        });
        assertEquals("You do not have a role in this company.", exception.getMessage());
    }

    @Test
    public void GivenAppointerIsPending_WhenOwnerAssignmentRequest_ThenThrowException() {
        // Arrange
        appointer.addOwnerRole(companyId, 999L); // Owner starts as PENDING by default
        
        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.ownerAssignmentRequest(appointer, appointee, companyId);
        });
        assertEquals("Your role is not active yet. You cannot appoint others.", exception.getMessage());
    }

    @Test
    public void GivenAppointerIsManager_WhenOwnerAssignmentRequest_ThenThrowException() {
        // Arrange
        appointer.addManagerRole(companyId, 999L, new HashSet<>());
        appointer.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        
        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.ownerAssignmentRequest(appointer, appointee, companyId);
        });
        assertEquals("Only Owners and Founders can appoint others.", exception.getMessage());
    }

    @Test
    public void GivenAppointeeHasRole_WhenOwnerAssignmentRequest_ThenThrowException() {
        // Arrange
        appointer.addFounderRole(companyId);
        appointee.addManagerRole(companyId, 100L, new HashSet<>()); // Appointee already has a role
        
        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.ownerAssignmentRequest(appointer, appointee, companyId);
        });
        assertEquals("This user already has an active or pending role in this company.", exception.getMessage());
    }

     @Test
    public void GivenValidRequest_WhenApproveOwnerAssignment_ThenRoleBecomesActive() throws Exception {
        // Arrange: Appointer is an active Founder
        appointer.addFounderRole(companyId);
        
        // Act 1: Appointer requests the Owner assignment for the appointee
        domainService.ownerAssignmentRequest(appointer, appointee, companyId);
        
        // Verify intermediate state: Role exists and is PENDING
        assertEquals(RoleStatus.PENDING, appointee.getRoleInCompany(companyId).getStatus());
        
        // Act 2: Appointee approves the assignment
        domainService.approveAssignment(appointer, appointee, company);
        
        // Assert: Role is now ACTIVE
        CompanyRole assignedRole = appointee.getRoleInCompany(companyId);
        assertNotNull(assignedRole, "Role should exist.");
        assertTrue(assignedRole instanceof Owner, "Role should be of type Owner.");
        assertEquals(RoleStatus.ACTIVE, assignedRole.getStatus(), "Role status should transition to ACTIVE.");
        
        // Assert: Appointer's tree was correctly updated with the new subordinate
        Founder founderRole = (Founder) appointer.getRoleInCompany(companyId);
        assertTrue(founderRole.getAppointeesMemberIds().contains(200L), "Founder should have the new Owner in their appointees list.");
    }

    @Test
    public void GivenValidRequest_WhenRejectOwnerAssignment_ThenRoleIsDeleted() throws Exception {
        // Arrange: Appointer is an active Founder
        appointer.addFounderRole(companyId);
        
        // Act 1: Appointer requests the Owner assignment
        domainService.ownerAssignmentRequest(appointer, appointee, companyId);
        
        // Verify intermediate state: Role exists and is PENDING
        assertNotNull(appointee.getRoleInCompany(companyId));
        
        // Act 2: Appointee rejects the assignment
        domainService.rejectAssignment(appointer, appointee, companyId);
        
        // Assert: Role is completely removed from the appointee's profile
        assertNull(appointee.getRoleInCompany(companyId), "Role should be removed after rejection.");
        
        // Assert: Appointer's tree remains clean
        Founder founderRole = (Founder) appointer.getRoleInCompany(companyId);
        assertFalse(founderRole.getAppointeesMemberIds().contains(200L), "Founder's appointee list should not contain the rejected member.");
    }

    // --- Remove Owner Assignment ---
    @Test
    public void GivenValidOwner_WhenValidateRemoveOwnerAssignment_ThenReturnsTrueAndCleansUp() throws Exception {
        // Arrange
        appointer.addFounderRole(companyId);

        Founder founderRole = (Founder) appointer.getRoleInCompany(companyId);
        founderRole.addAppointee(existingOwnerId);

        // Act
        boolean result = domainService.validateRemoveOwnerAssignment(appointer, existingOwner, company);

        // Assert
        assertTrue(result, "Should return true on successful validation and removal.");
        assertNull(existingOwner.getRoleInCompany(companyId), "The Owner role should be deleted.");
        assertFalse(founderRole.getAppointeesMemberIds().contains(existingOwnerId),
                "Removed owner should also be removed from founder appointees list.");
    }

    @Test
    public void GivenAppointerHasNoRole_WhenValidateRemoveOwnerAssignment_ThenThrowsException() {
        Exception ex = assertThrows(Exception.class, () -> {
            domainService.validateRemoveOwnerAssignment(appointer, existingOwner, company);
        });
        assertEquals("You do not have a role in this company.", ex.getMessage());
    }

    @Test
    public void GivenTargetHasNoRole_WhenValidateRemoveOwnerAssignment_ThenThrowsException() {
        appointer.addFounderRole(companyId);
        // target 'appointee' (200L) currently has no role in setup
        
        Exception ex = assertThrows(Exception.class, () -> {
            domainService.validateRemoveOwnerAssignment(appointer, appointee, company);
        });
        assertEquals("The target user does not have a role in this company.", ex.getMessage());
    }

    @Test
    public void GivenTargetIsNotOwner_WhenValidateRemoveOwnerAssignment_ThenThrowsException() {
        appointer.addFounderRole(companyId);

        appointee.addManagerRole(companyId, 100L, new HashSet<>());

        Exception ex = assertThrows(Exception.class, () -> {
            domainService.validateRemoveOwnerAssignment(appointer, appointee, company);
        });

        assertEquals("The target user is not an Owner.", ex.getMessage());
    }

    @Test
    public void GivenActorIsNotAppointer_WhenValidateRemoveOwnerAssignment_ThenThrowsException() {
        appointer.addFounderRole(companyId);
        
        // existingOwner was appointed by 100L. Let's make an actor with ID 999L
        Member fakeAppointer = new Member(999L, "FakeAppointer");
        fakeAppointer.addFounderRole(companyId);
        
        Exception ex = assertThrows(Exception.class, () -> {
            domainService.validateRemoveOwnerAssignment(fakeAppointer, existingOwner, company);
        });
        assertEquals("You are not the appointer of the specified user", ex.getMessage());
    }

    // --- Remove Manager Assignment ---

    @Test
    public void GivenAuthorizedAppointer_WhenValidateRemoveManagerAssignment_ThenReturnsTrue() throws Exception {
        // Arrange
        appointer.addFounderRole(companyId);
        appointer.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE); // ווידוא שהממנה פעיל
        Founder founderRole = (Founder) appointer.getRoleInCompany(companyId);
        
        appointee.addManagerRole(companyId, 100L, new HashSet<>());
        appointee.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE); // ווידוא שהמנהל פעיל
        founderRole.addAppointee(200L);

        // Act
        boolean result = domainService.validateRemoveManagerAssignment(appointer, appointee, companyId);

        // Assert
        assertTrue(result);
        assertNull(appointee.getRoleInCompany(companyId), "Manager role should be deleted.");
        assertFalse(founderRole.getAppointeesMemberIds().contains(200L), "Target should be removed from appointer's list.");
    }

    @Test
    public void GivenUnauthorizedActor_WhenValidateRemoveManagerAssignment_ThenThrowsException() {
        // Arrange
        appointer.addFounderRole(companyId);
        appointer.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE); // ווידוא שהממנה פעיל
        
        // Appointee was appointed by someone else (999L)
        appointee.addManagerRole(companyId, 999L, new HashSet<>()); 
        appointee.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE); // ווידוא שהמנהל פעיל

        // Act & Assert
        Exception ex = assertThrows(Exception.class, () -> {
            domainService.validateRemoveManagerAssignment(appointer, appointee, companyId);
        });
        assertEquals("You are not the appointer of the specified user", ex.getMessage());
    }

    // =========================================================================================
    // Use Case 4.10: Resignation & Transfer (Unit Tests)
    // =========================================================================================

@Test
    public void GivenOwnerWithSubordinates_WhenTransferAppointees_ThenSubordinatesMoveToFounder() throws Exception {
        // Arrange: Founder(100) -> Owner(200) -> Manager(400)
        appointer.addFounderRole(companyId); 
        appointee.addOwnerRole(companyId, 100L);
        appointee.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        
        // --- התיקון: שימוש במזהה 400 כדי לא להתנגש עם הבעלים מה-setup ---
        Member sub = new Member(400L, "SubManager");
        sub.addManagerRole(companyId, 200L, new java.util.HashSet<>());
        userRepository.addRegisteredMember(400L, sub, "pass");
        ((Owner)appointee.getRoleInCompany(companyId)).addAppointee(400L);

        // Act
        domainService.transferAppointees(appointee, appointer, companyId);

        // Assert
        Founder founderRole = (Founder) appointer.getRoleInCompany(companyId);
        assertTrue(founderRole.getAppointeesMemberIds().contains(400L), "Founder should inherit the subordinate.");
        assertEquals(0, ((Owner)appointee.getRoleInCompany(companyId)).getAppointeesMemberIds().size(), "Resigning owner's appointee list should be empty.");
        
        Member updatedSub = userRepository.getMemberById(400L);
        Manager transferredSubRole = (Manager) updatedSub.getRoleInCompany(companyId);
        
        assertEquals(100L, transferredSubRole.getAppointedByMemberId(), "The transferred subordinate's appointedByMemberId should be updated to the Founder's ID.");
    }

    @Test
    public void GivenFounderRole_WhenValidateResignation_ThenThrowsException() {
        appointer.addFounderRole(companyId);
        assertThrows(Exception.class, () -> {
            domainService.validateOwnerResignation(appointer.getRoleInCompany(companyId));
        });
    }

    @Test
    public void GivenMemberWithoutRole_WhenAssignFounderRole_ThenFounderRoleIsActive() throws Exception {
        // Act
        domainService.assignFounderRole(100L, companyId);

        // Assert: MUST fetch fresh instance
        Member updatedAppointer = userRepository.getMemberById(100L);
        CompanyRole role = updatedAppointer.getRoleInCompany(companyId);

        assertNotNull(role, "Founder role should be assigned.");
        assertTrue(role instanceof Founder, "Assigned role should be Founder.");
        assertEquals(RoleStatus.ACTIVE, role.getStatus(), "Founder role should be active immediately.");
    }

    @Test
    public void GivenActiveFounder_WhenValidateFounder_ThenDoesNotThrow() {
        // Arrange
        appointer.addFounderRole(companyId);

        // Act + Assert
        assertDoesNotThrow(() -> domainService.validateFounder(100L, companyId));
    }

    @Test
    public void GivenNonFounder_WhenValidateFounder_ThenThrowsException() {
        // Arrange
        appointer.addOwnerRole(companyId, 999L);
        appointer.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);

        // Act + Assert
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.validateFounder(100L, companyId);
        });

        assertEquals("Only the active Founder can perform this action.", exception.getMessage());
    }

    @Test
    public void GivenActiveOwner_WhenValidateOwnerOrFounder_ThenDoesNotThrow() {
        // Arrange
        appointer.addOwnerRole(companyId, 999L);
        appointer.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);

        // Act + Assert
        assertDoesNotThrow(() -> domainService.validateOwnerOrFounder(100L, companyId));
    }

    @Test
    public void GivenManager_WhenValidateOwnerOrFounder_ThenThrowsException() {
        // Arrange
        appointer.addManagerRole(companyId, 999L, new HashSet<>());
        appointer.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);

        // Act + Assert
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.validateOwnerOrFounder(100L, companyId);
        });

        assertEquals("Only Owners or Founder can perform this action.", exception.getMessage());
    }

    @Test
    public void GivenActiveRole_WhenHasActiveRoleInCompany_ThenReturnsTrue() {
        // Arrange
        appointer.addOwnerRole(companyId, 999L);
        appointer.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);

        // Act + Assert
        assertTrue(domainService.hasActiveRoleInCompany(100L, companyId));
    }

    @Test
    public void GivenNoRole_WhenHasActiveRoleInCompany_ThenReturnsFalse() {
        assertFalse(domainService.hasActiveRoleInCompany(100L, companyId));
    }

    @Test
    public void GivenMemberWithNonFounderRoles_WhenCancelAllRolesForMember_ThenRolesAreCancelled() throws Exception {
        // Arrange
        Long secondCompanyId = 2L;

        // Company 1: appointer is Founder, appointee is Owner
        appointer.addFounderRole(companyId);
        Founder founderRole = (Founder) appointer.getRoleInCompany(companyId);

        appointee.addOwnerRole(companyId, 100L);
        appointee.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        founderRole.addAppointee(200L);

        // Company 2: appointer must also have an active role there,
        // because appointee's manager role says appointerId = 100L.
        appointer.addFounderRole(secondCompanyId);
        Founder secondCompanyFounderRole = (Founder) appointer.getRoleInCompany(secondCompanyId);

        appointee.addManagerRole(secondCompanyId, 100L, new HashSet<>());
        appointee.getRoleInCompany(secondCompanyId).setStatus(RoleStatus.ACTIVE);
        secondCompanyFounderRole.addAppointee(200L);

        userRepository.updateMember(appointer);
        userRepository.updateMember(appointee);

        // Act
        domainService.cancelAllRolesForMember(200L);

        // Assert
        Member updatedAppointee = userRepository.getMemberById(200L);
        Member updatedAppointer = userRepository.getMemberById(100L);
        
        assertEquals(RoleStatus.CANCELLED, updatedAppointee.getRoleInCompany(companyId).getStatus());
        assertEquals(RoleStatus.CANCELLED, updatedAppointee.getRoleInCompany(secondCompanyId).getStatus());

        founderRole = (Founder) updatedAppointer.getRoleInCompany(companyId);
        assertFalse(founderRole.getAppointeesMemberIds().contains(200L),
                "Cancelled member should be removed from the first appointer's appointees list.");

        assertFalse(founderRole.getAppointeesMemberIds().contains(200L),
                "Cancelled member should be removed from the first appointer's appointees list.");

        assertFalse(secondCompanyFounderRole.getAppointeesMemberIds().contains(200L),
                "Cancelled member should be removed from the second appointer's appointees list.");
    }

    @Test
    public void GivenMemberIsFounder_WhenCancelAllRolesForMember_ThenThrowsException() {
        // Arrange
        appointer.addFounderRole(companyId);

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.cancelAllRolesForMember(100L);
        });

        // Use contains to be safe from exception wrappers
        assertTrue(exception.getMessage().contains("Cannot delete user: The user is a Founder"));
    }

    @Test
    public void GivenCompanyHasActiveRoles_WhenCancelAllRolesForCompany_ThenAllCompanyRolesAreCancelled() {
        // Arrange
        appointer.addFounderRole(companyId);

        appointee.addManagerRole(companyId, 100L, new HashSet<>());
        appointee.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);

        existingOwner.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);

        // Act
        domainService.cancelAllRolesForCompany(companyId);

        // Assert
        assertEquals(RoleStatus.CANCELLED, appointer.getRoleInCompany(companyId).getStatus());
        assertEquals(RoleStatus.CANCELLED, appointee.getRoleInCompany(companyId).getStatus());
        assertEquals(RoleStatus.CANCELLED, existingOwner.getRoleInCompany(companyId).getStatus());
    }

    // =========================================================================================
// Use Case 4.15: Roles and permissions tree - Domain logic
// =========================================================================================

    @Test
    public void GivenFounderWithOwnerAndManager_WhenBuildRolesAndPermissionsTree_ThenTreeContainsRolesAndPermissions() throws Exception {
        // Arrange
        appointer.addFounderRole(companyId);

        Founder founderRole = (Founder) appointer.getRoleInCompany(companyId);

        existingOwner.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        founderRole.addAppointee(existingOwnerId);

        Set<Permission> managerPermissions = new HashSet<>();
        managerPermissions.add(Permission.MANAGE_INQUIRIES);

        appointee.addManagerRole(companyId, 100L, managerPermissions);
        appointee.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        founderRole.addAppointee(200L);

        userRepository.updateMember(appointer);
        userRepository.updateMember(appointee);
        userRepository.updateMember(existingOwner);

        // Act
        String tree = domainService.buildRolesAndPermissionsTree(100L, companyId, 100L);

        // Assert
        assertNotNull(tree, "Tree should not be null.");

        assertTrue(tree.contains("FOUNDER"), "Tree should include the founder role.");
        assertTrue(tree.contains("100"), "Tree should include the founder id.");

        assertTrue(tree.contains("OWNER"), "Tree should include the owner role.");
        assertTrue(tree.contains(String.valueOf(existingOwnerId)), "Tree should include the owner id.");

        assertTrue(tree.contains("MANAGER"), "Tree should include the manager role.");
        assertTrue(tree.contains("200"), "Tree should include the manager id.");

        assertTrue(tree.contains(Permission.MANAGE_INQUIRIES.getKey()),
                "Tree should include manager permissions.");
    }

    @Test
    public void GivenManager_WhenBuildRolesAndPermissionsTree_ThenThrowsException() {
        // Arrange
        appointer.addFounderRole(companyId);

        Set<Permission> managerPermissions = new HashSet<>();
        managerPermissions.add(Permission.MANAGE_INQUIRIES);

        appointee.addManagerRole(companyId, 100L, managerPermissions);
        appointee.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);

        // Act + Assert
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.buildRolesAndPermissionsTree(200L, companyId, 100L);
        });

        assertEquals("Only Owners or Founder can perform this action.", exception.getMessage());
    }
}
