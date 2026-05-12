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
        
        // SYNC WITH COMPANY TREE
        company.registerNewAppointment(100L, existingOwnerId, "OWNER");
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
            domainService.ManagerAssignmentRequest(appointer, appointee, companyId, new HashSet<>());
        });
        assertEquals("You do not have a role in this company.", exception.getMessage());
    }

    @Test
    public void GivenAppointerIsPending_WhenManagerAssignmentRequest_ThenThrowException() {
        appointer.addOwnerRole(companyId, 999L); 
        
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.ManagerAssignmentRequest(appointer, appointee, companyId, new HashSet<>());
        });
        assertEquals("Your role is not active yet. You cannot appoint others.", exception.getMessage());
    }

    @Test
    public void GivenAppointerIsManager_WhenManagerAssignmentRequest_ThenThrowException() {
        appointer.addManagerRole(companyId, 999L, new HashSet<>());
        appointer.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.ManagerAssignmentRequest(appointer, appointee, companyId, new HashSet<>());
        });
        assertEquals("Only Owners and Founders can appoint others.", exception.getMessage());
    }

    @Test
    public void GivenAppointeeHasRole_WhenManagerAssignmentRequest_ThenThrowException() {
        appointer.addFounderRole(companyId);
        appointee.addManagerRole(companyId, 100L, new HashSet<>()); 
        
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.ManagerAssignmentRequest(appointer, appointee, companyId, new HashSet<>());
        });
        assertEquals("This user already has an active or pending role in this company.", exception.getMessage());
    }

    @Test
    public void GivenValidOwner_WhenManagerAssignmentRequest_ThenAddRole() throws Exception {
        appointer.addFounderRole(companyId);
        Set<Permission> perms = new HashSet<>();
        
        boolean result = domainService.ManagerAssignmentRequest(appointer, appointee, companyId, perms);
        
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
            domainService.ApproveAssignment(appointer, appointee, company);
        });
        assertEquals("No pending role invitation found.", exception.getMessage());
    }

    @Test
    public void GivenRoleAlreadyActive_WhenApproveAssignment_ThenThrowException() {
        appointer.addFounderRole(companyId);
        appointee.addManagerRole(companyId, 100L, new HashSet<>());
        appointee.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.ApproveAssignment(appointer, appointee, company);
        });
        assertEquals("This role is already active.", exception.getMessage());
    }
    
    @Test
    public void GivenValidApproval_WhenApproveAssignment_ThenStatusBecomesActive() throws Exception {
        appointer.addFounderRole(companyId);
        appointee.addManagerRole(companyId, 100L, new HashSet<>());

        domainService.ApproveAssignment(appointer, appointee, company);

        assertEquals(RoleStatus.ACTIVE, appointee.getRoleInCompany(companyId).getStatus());
        Founder founderRole = (Founder) appointer.getRoleInCompany(companyId);
        assertTrue(founderRole.getAppointeesMemberIds().contains(200L));
    }

    // --- Reject Assignment ---

    @Test
    public void GivenNoPendingRole_WhenRejectAssignment_ThenThrowException() {
        appointer.addFounderRole(companyId);
        
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.RejectAssignment(appointer, appointee, companyId);
        });
        assertEquals("No pending role invitation found.", exception.getMessage());
    }

    @Test
    public void GivenValidRejection_WhenRejectAssignment_ThenRoleDeletedAndAppointeeRemoved() throws Exception {
        appointer.addFounderRole(companyId);
        Founder founderRole = (Founder) appointer.getRoleInCompany(companyId);
        founderRole.addAppointee(200L); 

        appointee.addManagerRole(companyId, 100L, new HashSet<>());

        domainService.RejectAssignment(appointer, appointee, companyId);

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
        boolean result = domainService.OwnerAssignmentRequest(appointer, appointee, companyId);
        
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
            domainService.OwnerAssignmentRequest(appointer, appointee, companyId);
        });
        assertEquals("You do not have a role in this company.", exception.getMessage());
    }

    @Test
    public void GivenAppointerIsPending_WhenOwnerAssignmentRequest_ThenThrowException() {
        // Arrange
        appointer.addOwnerRole(companyId, 999L); // Owner starts as PENDING by default
        
        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.OwnerAssignmentRequest(appointer, appointee, companyId);
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
            domainService.OwnerAssignmentRequest(appointer, appointee, companyId);
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
            domainService.OwnerAssignmentRequest(appointer, appointee, companyId);
        });
        assertEquals("This user already has an active or pending role in this company.", exception.getMessage());
    }

    // --- Remove Owner Assignment ---

    @Test
    public void GivenValidOwner_WhenValidateRemoveOwnerAssignment_ThenReturnsTrueAndCleansUp() throws Exception {
        // Arrange: appointer (100L) is Founder, existingOwner (300L) is already synced in Company tree.
        appointer.addFounderRole(companyId);
        
        // Act
        boolean result = domainService.validateRemoveOwnerAssignment(appointer, existingOwner, company);

        // Assert
        assertTrue(result, "Should return true on successful validation and removal.");
        assertNull(existingOwner.getRoleInCompany(companyId), "The Owner role should be deleted.");
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
        
        // Arrange appointee as MANAGER
        appointee.addManagerRole(companyId, 100L, new HashSet<>());
        company.registerNewAppointment(100L, 200L, "MANAGER");
        
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

}
