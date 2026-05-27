package ticketsystem.UnitTesting;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.DomainLayer.discount.DiscountPolicy;
import ticketsystem.DomainLayer.MembershipDomainService;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.InfrastructureLayer.UserRepository;
import ticketsystem.DomainLayer.company.Company;
import ticketsystem.DomainLayer.company.PurchasePolicy;
import ticketsystem.DomainLayer.discount.DiscountCompositionType;
import ticketsystem.DomainLayer.user.*;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class MembershipDomainServiceTest {

    private MembershipDomainService domainService;
    private IUserRepository userRepository;
    
    // Using REAL Domain Objects!
    private Member appointingMember;
    private Member targetMember;
    private Company company;
    private final Long companyId = 1L;

    private Member existingOwner;
    private final Long existingOwnerId = 300L;

    @BeforeEach
    public void setUp() {
        userRepository = new UserRepository();
        domainService = new MembershipDomainService(userRepository);

        appointingMember = new Member(100L, "AppointerUser", "Appointer User", "0500000001");
        targetMember = new Member(200L, "TargetMemberUser", "Target Member User", "0500000002"); // Kept blank for UC 4.7 & 4.8
        
        userRepository.addRegisteredMember(100L, appointingMember, "password123");
        userRepository.addRegisteredMember(200L, targetMember, "password123");
        
        company = new Company("BGU Productions", 100L, new PurchasePolicy(), new DiscountPolicy(DiscountCompositionType.MAX));
        try { company.setId(companyId); } catch (Exception e) {}

        // Setup a pre-existing Owner specifically for UC 4.9 & 4.10 tests
        existingOwner = new Member(existingOwnerId, "ExistingOwner", "Existing Owner", "0500000003");
        existingOwner.addOwnerRole(companyId, 100L); // Appointed by appointingMember
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
        appointingMember.addOwnerRole(companyId, 999L); 
        userRepository.updateMember(appointingMember);
        assertFalse(domainService.validatePermission(100L, companyId, Permission.MANAGE_EVENT_INVENTORY));
    }

    @Test
    public void GivenActiveRoleWithPermission_WhenValidatePermission_ThenReturnTrue() {
        appointingMember.addFounderRole(companyId); 
        userRepository.updateMember(appointingMember);
        assertTrue(domainService.validatePermission(100L, companyId, Permission.MANAGE_EVENT_INVENTORY));
    }

    // --- Manager Assignment Request ---

    @Test
    public void GivenAppointerHasNoRole_WhenManagerAssignmentRequest_ThenThrowException() {
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.managerAssignmentRequest(appointingMember, targetMember, companyId, new HashSet<>());
        });
        assertEquals("You do not have a role in this company.", exception.getMessage());
    }

    @Test
    public void GivenAppointerIsPending_WhenManagerAssignmentRequest_ThenThrowException() {
        appointingMember.addOwnerRole(companyId, 999L); 
        userRepository.updateMember(appointingMember);
        
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.managerAssignmentRequest(appointingMember, targetMember, companyId, new HashSet<>());
        });
        assertEquals("Your role is not active yet. You cannot appoint others.", exception.getMessage());
    }

    @Test
    public void GivenAppointerIsManager_WhenManagerAssignmentRequest_ThenThrowException() {
        appointingMember.addManagerRole(companyId, 999L, new HashSet<>());
        appointingMember.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        userRepository.updateMember(appointingMember);
        
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.managerAssignmentRequest(appointingMember, targetMember, companyId, new HashSet<>());
        });
        assertEquals("Only Owners and Founders can appoint others.", exception.getMessage());
    }

    @Test
    public void GivenAppointeeHasRole_WhenManagerAssignmentRequest_ThenThrowException() {
        appointingMember.addFounderRole(companyId);
        userRepository.updateMember(appointingMember);

        targetMember.addManagerRole(companyId, 100L, new HashSet<>()); 
        userRepository.updateMember(targetMember);
        
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.managerAssignmentRequest(appointingMember, targetMember, companyId, new HashSet<>());
        });
        assertEquals("This user already has an active or pending role in this company.", exception.getMessage());
    }

    @Test
    public void GivenValidOwner_WhenManagerAssignmentRequest_ThenAddRole() throws Exception {
        appointingMember.addFounderRole(companyId);
        userRepository.updateMember(appointingMember);

        Set<Permission> perms = new HashSet<>();
        
        boolean result = domainService.managerAssignmentRequest(appointingMember, targetMember, companyId, perms);
        userRepository.updateMember(targetMember); // Save changes after request
        
        assertTrue(result);
        
        Member freshTarget = userRepository.getMemberById(200L);
        CompanyRole assignedRole = freshTarget.getRoleInCompany(companyId);
        assertNotNull(assignedRole);
        assertTrue(assignedRole instanceof Manager);
        assertEquals(RoleStatus.PENDING, assignedRole.getStatus());
        assertEquals(100L, ((Manager) assignedRole).getAppointedByMemberId());
    }

    // --- Approve Assignment ---

    @Test
    public void GivenNoPendingRole_WhenApproveAssignment_ThenThrowException() {
        appointingMember.addFounderRole(companyId);
        userRepository.updateMember(appointingMember);
        
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.approveAssignment(appointingMember, targetMember, company);
        });
        assertEquals("No pending role invitation found.", exception.getMessage());
    }

    @Test
    public void GivenRoleAlreadyActive_WhenApproveAssignment_ThenThrowException() {
        appointingMember.addFounderRole(companyId);
        userRepository.updateMember(appointingMember);

        targetMember.addManagerRole(companyId, 100L, new HashSet<>());
        targetMember.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        userRepository.updateMember(targetMember);
        
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.approveAssignment(appointingMember, targetMember, company);
        });
        assertEquals("This role is already active.", exception.getMessage());
    }
    
    @Test
    public void GivenValidApproval_WhenApproveAssignment_ThenStatusBecomesActive() throws Exception {
        appointingMember.addFounderRole(companyId);
        userRepository.updateMember(appointingMember);

        targetMember.addManagerRole(companyId, 100L, new HashSet<>());
        userRepository.updateMember(targetMember);

        // שליפת האובייקטים הטריים לאחר ההכנות
        Member freshAppointer = userRepository.getMemberById(100L);
        Member freshTarget = userRepository.getMemberById(200L);

        domainService.approveAssignment(freshAppointer, freshTarget, company);
        
        userRepository.updateMember(freshAppointer);
        userRepository.updateMember(freshTarget);

        Member finalTarget = userRepository.getMemberById(200L);
        Member finalAppointing = userRepository.getMemberById(100L);

        assertEquals(RoleStatus.ACTIVE, finalTarget.getRoleInCompany(companyId).getStatus());
        Founder founderRole = (Founder) finalAppointing.getRoleInCompany(companyId);
        assertTrue(founderRole.getAppointeesMemberIds().contains(200L));
    }

    // --- Reject Assignment ---

    @Test
    public void GivenNoPendingRole_WhenRejectAssignment_ThenThrowException() {
        appointingMember.addFounderRole(companyId);
        userRepository.updateMember(appointingMember);
        
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.rejectAssignment(appointingMember, targetMember, companyId);
        });
        assertEquals("No pending role invitation found.", exception.getMessage());
    }

    @Test
    public void GivenValidRejection_WhenRejectAssignment_ThenRoleDeletedAndAppointeeRemoved() throws Exception {
        appointingMember.addFounderRole(companyId);
        Founder founderRole = (Founder) appointingMember.getRoleInCompany(companyId);
        founderRole.addAppointee(200L); 
        userRepository.updateMember(appointingMember);

        targetMember.addManagerRole(companyId, 100L, new HashSet<>());
        userRepository.updateMember(targetMember);

        // שליפת האובייקטים הטריים לאחר ההכנות
        Member freshAppointer = userRepository.getMemberById(100L);
        Member freshTarget = userRepository.getMemberById(200L);

        domainService.rejectAssignment(freshAppointer, freshTarget, companyId);
        
        userRepository.updateMember(freshAppointer);
        userRepository.updateMember(freshTarget);

        Member finalTarget = userRepository.getMemberById(200L);
        Member finalAppointing = userRepository.getMemberById(100L);

        assertNull(finalTarget.getRoleInCompany(companyId));
        Founder finalFounderRole = (Founder) finalAppointing.getRoleInCompany(companyId);
        assertFalse(finalFounderRole.getAppointeesMemberIds().contains(200L));
    }

    // --- Update Manager Permissions ---

    @Test
    public void GivenValidAppointer_WhenSetPermissions_ThenPermissionsAreUpdated() throws Exception {
        appointingMember.addFounderRole(companyId); 
        userRepository.updateMember(appointingMember);
        
        Set<Permission> initialPerms = new HashSet<>();
        targetMember.addManagerRole(companyId, 100L, initialPerms);
        targetMember.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        userRepository.updateMember(targetMember);
        
        Set<Permission> newPerms = new HashSet<>();
        newPerms.add(Permission.MANAGE_EVENT_INVENTORY);
        
        // שליפת האובייקטים הטריים לפני שינוי ההרשאות
        Member freshAppointer = userRepository.getMemberById(100L);
        Member freshTarget = userRepository.getMemberById(200L);

        boolean result = domainService.setPermissionsToManager(freshAppointer, freshTarget, companyId, newPerms);
        userRepository.updateMember(freshTarget);

        assertTrue(result);
        
        Member finalTarget = userRepository.getMemberById(200L);
        Manager managerRole = (Manager) finalTarget.getRoleInCompany(companyId);
        assertTrue(managerRole.getPermissionKeys().contains(Permission.MANAGE_EVENT_INVENTORY.getKey()));
    }

    @Test
    public void GivenAppointerHasNoRole_WhenSetPermissions_ThenThrowsException() {
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.setPermissionsToManager(appointingMember, targetMember, companyId, new HashSet<>());
        });
        assertEquals("You do not have a role in this company.", exception.getMessage());
    }

    @Test
    public void GivenAppointerIsNotActive_WhenSetPermissions_ThenThrowsException() {
        appointingMember.addOwnerRole(companyId, 999L); 
        userRepository.updateMember(appointingMember);
        
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.setPermissionsToManager(appointingMember, targetMember, companyId, new HashSet<>());
        });
        assertEquals("Your role is not active yet. You cannot update others permissions.", exception.getMessage());
    }

    @Test
    public void GivenAppointerIsManager_WhenSetPermissions_ThenThrowsException() {
        appointingMember.addManagerRole(companyId, 999L, new HashSet<>());
        appointingMember.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        userRepository.updateMember(appointingMember);
        
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.setPermissionsToManager(appointingMember, targetMember, companyId, new HashSet<>());
        });
        assertEquals("Only Owners and Founders can update manager's permissions.", exception.getMessage());
    }

    @Test
    public void GivenTargetIsNotManager_WhenSetPermissions_ThenThrowsException() {
        appointingMember.addFounderRole(companyId);
        userRepository.updateMember(appointingMember);

        targetMember.addOwnerRole(companyId, 100L); 
        userRepository.updateMember(targetMember);
        
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.setPermissionsToManager(appointingMember, targetMember, companyId, new HashSet<>());
        });
        assertEquals("The specified user does not hold a Manager role", exception.getMessage());
    }

    @Test
    public void GivenActorIsNotTheAppointer_WhenSetPermissions_ThenThrowsException() {
        appointingMember.addFounderRole(companyId);
        userRepository.updateMember(appointingMember);

        targetMember.addManagerRole(companyId, 999L, new HashSet<>()); 
        targetMember.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        userRepository.updateMember(targetMember);
        
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.setPermissionsToManager(appointingMember, targetMember, companyId, new HashSet<>());
        });
        assertEquals("You are not the appointer of the specified user", exception.getMessage());
    }

    @Test
    public void GivenTargetIsPending_WhenSetPermissions_ThenThrowsException() {
        appointingMember.addFounderRole(companyId);
        userRepository.updateMember(appointingMember);
        
        targetMember.addManagerRole(companyId, 100L, new HashSet<>()); 
        userRepository.updateMember(targetMember);
        
        Set<Permission> newPerms = new HashSet<>();
        newPerms.add(Permission.MANAGE_INQUIRIES);
        
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.setPermissionsToManager(appointingMember, targetMember, companyId, newPerms);
        });
        assertEquals("Cannot update permissions for a pending manager.", exception.getMessage());
    }

    @Test
    public void GivenNullPermissionsSet_WhenSetPermissions_ThenThrowsException() {
        appointingMember.addFounderRole(companyId);
        userRepository.updateMember(appointingMember);

        targetMember.addManagerRole(companyId, 100L, new HashSet<>());
        targetMember.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        userRepository.updateMember(targetMember);
        
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.setPermissionsToManager(appointingMember, targetMember, companyId, null);
        });
        assertEquals("Permissions set cannot be null or contain null values.", exception.getMessage());
    }

    @Test
    public void GivenPermissionsSetWithNullElement_WhenSetPermissions_ThenThrowsException() {
        appointingMember.addFounderRole(companyId);
        userRepository.updateMember(appointingMember);

        targetMember.addManagerRole(companyId, 100L, new HashSet<>());
        targetMember.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        userRepository.updateMember(targetMember);
        
        Set<Permission> invalidPerms = new HashSet<>();
        invalidPerms.add(Permission.MANAGE_EVENT_INVENTORY);
        invalidPerms.add(null); 
        
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.setPermissionsToManager(appointingMember, targetMember, companyId, invalidPerms);
        });
        assertEquals("Permissions set cannot be null or contain null values.", exception.getMessage());
    }

    // --- Owner Assignment Request ---   

    @Test
    public void GivenValidOwner_WhenOwnerAssignmentRequest_ThenAddRole() throws Exception {
        appointingMember.addFounderRole(companyId); 
        userRepository.updateMember(appointingMember);
        
        boolean result = domainService.ownerAssignmentRequest(appointingMember, targetMember, companyId);
        userRepository.updateMember(targetMember);
        
        assertTrue(result);
        
        Member freshTarget = userRepository.getMemberById(200L);
        CompanyRole assignedRole = freshTarget.getRoleInCompany(companyId);
        assertNotNull(assignedRole);
        assertTrue(assignedRole instanceof Owner);
        assertEquals(RoleStatus.PENDING, assignedRole.getStatus());
        assertEquals(100L, ((Owner) assignedRole).getAppointedByMemberId());
    }

    @Test
    public void GivenAppointerHasNoRole_WhenOwnerAssignmentRequest_ThenThrowException() {
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.ownerAssignmentRequest(appointingMember, targetMember, companyId);
        });
        assertEquals("You do not have a role in this company.", exception.getMessage());
    }

    @Test
    public void GivenAppointerIsPending_WhenOwnerAssignmentRequest_ThenThrowException() {
        appointingMember.addOwnerRole(companyId, 999L); 
        userRepository.updateMember(appointingMember);

        Exception exception = assertThrows(Exception.class, () -> {
            domainService.ownerAssignmentRequest(appointingMember, targetMember, companyId);
        });
        assertEquals("Your role is not active yet. You cannot appoint others.", exception.getMessage());
    }

    @Test
    public void GivenAppointerIsManager_WhenOwnerAssignmentRequest_ThenThrowException() {
        appointingMember.addManagerRole(companyId, 999L, new HashSet<>());
        appointingMember.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        userRepository.updateMember(appointingMember);
        
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.ownerAssignmentRequest(appointingMember, targetMember, companyId);
        });
        assertEquals("Only Owners and Founders can appoint others.", exception.getMessage());
    }

    @Test
    public void GivenAppointeeHasRole_WhenOwnerAssignmentRequest_ThenThrowException() {
        appointingMember.addFounderRole(companyId);
        userRepository.updateMember(appointingMember);

        targetMember.addManagerRole(companyId, 100L, new HashSet<>()); 
        userRepository.updateMember(targetMember);
        
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.ownerAssignmentRequest(appointingMember, targetMember, companyId);
        });
        assertEquals("This user already has an active or pending role in this company.", exception.getMessage());
    }

    @Test
    public void GivenValidRequest_WhenApproveOwnerAssignment_ThenRoleBecomesActive() throws Exception {
        appointingMember.addFounderRole(companyId);
        userRepository.updateMember(appointingMember);
        
        Member freshAppointer1 = userRepository.getMemberById(100L);

        domainService.ownerAssignmentRequest(freshAppointer1, targetMember, companyId);
        userRepository.updateMember(targetMember);
        
        Member intermediateTarget = userRepository.getMemberById(200L);
        assertEquals(RoleStatus.PENDING, intermediateTarget.getRoleInCompany(companyId).getStatus());
        
        // שליפה מחדש כי אנחנו עומדים לשנות אותו שוב ב-approve
        Member freshAppointer2 = userRepository.getMemberById(100L);

        domainService.approveAssignment(freshAppointer2, intermediateTarget, company);
        userRepository.updateMember(freshAppointer2);
        userRepository.updateMember(intermediateTarget);
        
        Member finalTarget = userRepository.getMemberById(200L);
        Member finalAppointing = userRepository.getMemberById(100L);
        
        CompanyRole assignedRole = finalTarget.getRoleInCompany(companyId);
        assertNotNull(assignedRole, "Role should exist.");
        assertTrue(assignedRole instanceof Owner, "Role should be of type Owner.");
        assertEquals(RoleStatus.ACTIVE, assignedRole.getStatus(), "Role status should transition to ACTIVE.");
        
        Founder founderRole = (Founder) finalAppointing.getRoleInCompany(companyId);
        assertTrue(founderRole.getAppointeesMemberIds().contains(200L), "Founder should have the new Owner in their appointees list.");
    }

    @Test
    public void GivenValidRequest_WhenRejectOwnerAssignment_ThenRoleIsDeleted() throws Exception {
        appointingMember.addFounderRole(companyId);
        userRepository.updateMember(appointingMember);
        
        Member freshAppointer1 = userRepository.getMemberById(100L);

        domainService.ownerAssignmentRequest(freshAppointer1, targetMember, companyId);
        userRepository.updateMember(targetMember);
        
        Member intermediateTarget = userRepository.getMemberById(200L);
        assertNotNull(intermediateTarget.getRoleInCompany(companyId));
        
        Member freshAppointer2 = userRepository.getMemberById(100L);

        domainService.rejectAssignment(freshAppointer2, intermediateTarget, companyId);
        userRepository.updateMember(freshAppointer2);
        userRepository.updateMember(intermediateTarget);
        
        Member finalTarget = userRepository.getMemberById(200L);
        Member finalAppointing = userRepository.getMemberById(100L);

        assertNull(finalTarget.getRoleInCompany(companyId), "Role should be removed after rejection.");
        
        Founder founderRole = (Founder) finalAppointing.getRoleInCompany(companyId);
        assertFalse(founderRole.getAppointeesMemberIds().contains(200L), "Founder's appointee list should not contain the rejected member.");
    }

    // --- Remove Owner Assignment ---
    
    @Test
    public void GivenValidOwner_WhenValidateRemoveOwnerAssignment_ThenReturnsTrueAndCleansUp() throws Exception {
        appointingMember.addFounderRole(companyId);
        Founder founderRole = (Founder) appointingMember.getRoleInCompany(companyId);
        founderRole.addAppointee(existingOwnerId);
        userRepository.updateMember(appointingMember);

        // שליפת האובייקטים הטריים לאחר ההכנות
        Member freshAppointer = userRepository.getMemberById(100L);
        Member freshExistingOwner = userRepository.getMemberById(existingOwnerId);

        boolean result = domainService.validateRemoveOwnerAssignment(freshAppointer, freshExistingOwner, company);
        
        userRepository.updateMember(freshAppointer);
        userRepository.updateMember(freshExistingOwner);

        assertTrue(result, "Should return true on successful validation and removal.");
        
        Member finalExistingOwner = userRepository.getMemberById(existingOwnerId);
        Member finalAppointing = userRepository.getMemberById(100L);

        assertNull(finalExistingOwner.getRoleInCompany(companyId), "The Owner role should be deleted.");
        Founder finalFounderRole = (Founder) finalAppointing.getRoleInCompany(companyId);
        assertFalse(finalFounderRole.getAppointeesMemberIds().contains(existingOwnerId),
                "Removed owner should also be removed from founder appointees list.");
    }

    @Test
    public void GivenAppointerHasNoRole_WhenValidateRemoveOwnerAssignment_ThenThrowsException() {
        Exception ex = assertThrows(Exception.class, () -> {
            domainService.validateRemoveOwnerAssignment(appointingMember, existingOwner, company);
        });
        assertEquals("You do not have a role in this company.", ex.getMessage());
    }

    @Test
    public void GivenTargetHasNoRole_WhenValidateRemoveOwnerAssignment_ThenThrowsException() {
        appointingMember.addFounderRole(companyId);
        userRepository.updateMember(appointingMember);
        
        Exception ex = assertThrows(Exception.class, () -> {
            domainService.validateRemoveOwnerAssignment(appointingMember, targetMember, company);
        });
        assertEquals("The target user does not have a role in this company.", ex.getMessage());
    }

    @Test
    public void GivenTargetIsNotOwner_WhenValidateRemoveOwnerAssignment_ThenThrowsException() {
        appointingMember.addFounderRole(companyId);
        userRepository.updateMember(appointingMember);

        targetMember.addManagerRole(companyId, 100L, new HashSet<>());
        userRepository.updateMember(targetMember);

        Exception ex = assertThrows(Exception.class, () -> {
            domainService.validateRemoveOwnerAssignment(appointingMember, targetMember, company);
        });

        assertEquals("The target user is not an Owner.", ex.getMessage());
    }

    @Test
    public void GivenActorIsNotAppointer_WhenValidateRemoveOwnerAssignment_ThenThrowsException() {
        appointingMember.addFounderRole(companyId);
        userRepository.updateMember(appointingMember);

        Member fakeAppointing = new Member(999L, "FakeAppointer", "Fake Appointer", "0500000004");
        fakeAppointing.addFounderRole(companyId);
        userRepository.addRegisteredMember(999L, fakeAppointing, "password123");
        
        Exception ex = assertThrows(Exception.class, () -> {
            domainService.validateRemoveOwnerAssignment(fakeAppointing, existingOwner, company);
        });
        assertEquals("You are not the appointer of the specified user", ex.getMessage());
    }

    // --- Remove Manager Assignment ---

    @Test
    public void GivenAuthorizedAppointer_WhenValidateRemoveManagerAssignment_ThenReturnsTrue() throws Exception {
        appointingMember.addFounderRole(companyId);
        appointingMember.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE); 
        Founder founderRole = (Founder) appointingMember.getRoleInCompany(companyId);
        founderRole.addAppointee(200L);
        userRepository.updateMember(appointingMember);
        
        targetMember.addManagerRole(companyId, 100L, new HashSet<>());
        targetMember.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE); 
        userRepository.updateMember(targetMember);

        // שליפת האובייקטים הטריים לאחר ההכנות
        Member freshAppointer = userRepository.getMemberById(100L);
        Member freshTarget = userRepository.getMemberById(200L);

        boolean result = domainService.validateRemoveManagerAssignment(freshAppointer, freshTarget, companyId);
        
        userRepository.updateMember(freshAppointer);
        userRepository.updateMember(freshTarget);

        assertTrue(result);
        
        Member finalTarget = userRepository.getMemberById(200L);
        Member finalAppointing = userRepository.getMemberById(100L);

        assertNull(finalTarget.getRoleInCompany(companyId), "Manager role should be deleted.");
        Founder finalFounderRole = (Founder) finalAppointing.getRoleInCompany(companyId);
        assertFalse(finalFounderRole.getAppointeesMemberIds().contains(200L), "Target should be removed from appointer's list.");
    }

    @Test
    public void GivenUnauthorizedActor_WhenValidateRemoveManagerAssignment_ThenThrowsException() {
        appointingMember.addFounderRole(companyId);
        appointingMember.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE); 
        userRepository.updateMember(appointingMember);
        
        targetMember.addManagerRole(companyId, 999L, new HashSet<>()); 
        targetMember.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE); 
        userRepository.updateMember(targetMember);

        Exception ex = assertThrows(Exception.class, () -> {
            domainService.validateRemoveManagerAssignment(appointingMember, targetMember, companyId);
        });
        assertEquals("You are not the appointer of the specified user", ex.getMessage());
    }

    // =========================================================================================
    // Use Case 4.10: Resignation & Transfer (Unit Tests)
    // =========================================================================================

    @Test
    public void GivenOwnerWithSubordinates_WhenTransferAppointees_ThenSubordinatesMoveToFounder() throws Exception {
        appointingMember.addFounderRole(companyId); 
        userRepository.updateMember(appointingMember);
        
        targetMember.addOwnerRole(companyId, 100L);
        targetMember.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        userRepository.updateMember(targetMember);

        Member sub = new Member(400L, "SubManager", "Sub Manager", "0500000005");
        sub.addManagerRole(companyId, 200L, new java.util.HashSet<>());
        userRepository.addRegisteredMember(400L, sub, "pass");
        
        Member freshTargetToLink = userRepository.getMemberById(200L);
        ((Owner)freshTargetToLink.getRoleInCompany(companyId)).addAppointee(400L);
        userRepository.updateMember(freshTargetToLink);

        Member freshTargetForAct = userRepository.getMemberById(200L);
        Member freshAppointingForAct = userRepository.getMemberById(100L);

        domainService.transferAppointees(freshTargetForAct, freshAppointingForAct, companyId);
        
        userRepository.updateMember(freshTargetForAct);
        userRepository.updateMember(freshAppointingForAct);

        Member finalAppointing = userRepository.getMemberById(100L);
        Member finalTarget = userRepository.getMemberById(200L);
        Member updatedSub = userRepository.getMemberById(400L);

        Founder founderRole = (Founder) finalAppointing.getRoleInCompany(companyId);
        assertTrue(founderRole.getAppointeesMemberIds().contains(400L), "Founder should inherit the subordinate.");
        assertEquals(0, ((Owner)finalTarget.getRoleInCompany(companyId)).getAppointeesMemberIds().size(), "Resigning owner's appointee list should be empty.");
        
        Manager transferredSubRole = (Manager) updatedSub.getRoleInCompany(companyId);
        assertEquals(100L, transferredSubRole.getAppointedByMemberId(), "The transferred subordinate's appointedByMemberId should be updated to the Founder's ID.");
    }

    @Test
    public void GivenFounderRole_WhenValidateResignation_ThenThrowsException() {
        appointingMember.addFounderRole(companyId);
        userRepository.updateMember(appointingMember);
        
        assertThrows(Exception.class, () -> {
            domainService.validateOwnerResignation(appointingMember.getRoleInCompany(companyId));
        });
    }

    @Test
    public void GivenMemberWithoutRole_WhenAssignFounderRole_ThenFounderRoleIsActive() throws Exception {
        domainService.assignFounderRole(100L, companyId);

        Member updatedAppointing = userRepository.getMemberById(100L);
        CompanyRole role = updatedAppointing.getRoleInCompany(companyId);

        assertNotNull(role, "Founder role should be assigned.");
        assertTrue(role instanceof Founder, "Assigned role should be Founder.");
        assertEquals(RoleStatus.ACTIVE, role.getStatus(), "Founder role should be active immediately.");
    }

    @Test
    public void GivenActiveFounder_WhenValidateFounder_ThenDoesNotThrow() {
        appointingMember.addFounderRole(companyId);
        userRepository.updateMember(appointingMember);
        
        assertDoesNotThrow(() -> domainService.validateFounder(100L, companyId));
    }

    @Test
    public void GivenNonFounder_WhenValidateFounder_ThenThrowsException() {
        appointingMember.addOwnerRole(companyId, 999L);
        appointingMember.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        userRepository.updateMember(appointingMember);

        Exception exception = assertThrows(Exception.class, () -> {
            domainService.validateFounder(100L, companyId);
        });

        assertEquals("Only the active Founder can perform this action.", exception.getMessage());
    }

    @Test
    public void GivenActiveOwner_WhenValidateOwnerOrFounder_ThenDoesNotThrow() {
        appointingMember.addOwnerRole(companyId, 999L);
        appointingMember.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        userRepository.updateMember(appointingMember);

        assertDoesNotThrow(() -> domainService.validateOwnerOrFounder(100L, companyId));
    }

    @Test
    public void GivenManager_WhenValidateOwnerOrFounder_ThenThrowsException() {
        appointingMember.addManagerRole(companyId, 999L, new HashSet<>());
        appointingMember.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        userRepository.updateMember(appointingMember);

        Exception exception = assertThrows(Exception.class, () -> {
            domainService.validateOwnerOrFounder(100L, companyId);
        });

        assertEquals("Only Owners or Founder can perform this action.", exception.getMessage());
    }

    @Test
    public void GivenActiveRole_WhenHasActiveRoleInCompany_ThenReturnsTrue() {
        appointingMember.addOwnerRole(companyId, 999L);
        appointingMember.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        userRepository.updateMember(appointingMember);

        assertTrue(domainService.hasActiveRoleInCompany(100L, companyId));
    }

    @Test
    public void GivenNoRole_WhenHasActiveRoleInCompany_ThenReturnsFalse() {
        assertFalse(domainService.hasActiveRoleInCompany(100L, companyId));
    }

    @Test
    public void GivenMemberWithNonFounderRoles_WhenCancelAllRolesForMember_ThenRolesAreCancelled() throws Exception {
        Long secondCompanyId = 2L;

        appointingMember.addFounderRole(companyId);
        Founder founderRole = (Founder) appointingMember.getRoleInCompany(companyId);

        targetMember.addOwnerRole(companyId, 100L);
        targetMember.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        founderRole.addAppointee(200L);

        appointingMember.addFounderRole(secondCompanyId);
        Founder secondCompanyFounderRole = (Founder) appointingMember.getRoleInCompany(secondCompanyId);

        targetMember.addManagerRole(secondCompanyId, 100L, new HashSet<>());
        targetMember.getRoleInCompany(secondCompanyId).setStatus(RoleStatus.ACTIVE);
        secondCompanyFounderRole.addAppointee(200L);

        userRepository.updateMember(appointingMember);
        userRepository.updateMember(targetMember);

        domainService.cancelAllRolesForMember(200L);

        Member updatedTarget = userRepository.getMemberById(200L);
        Member updatedAppointing = userRepository.getMemberById(100L);
        
        assertEquals(RoleStatus.CANCELLED, updatedTarget.getRoleInCompany(companyId).getStatus());
        assertEquals(RoleStatus.CANCELLED, updatedTarget.getRoleInCompany(secondCompanyId).getStatus());

        Founder freshFounderRole1 = (Founder) updatedAppointing.getRoleInCompany(companyId);
        assertFalse(freshFounderRole1.getAppointeesMemberIds().contains(200L),
                "Cancelled member should be removed from the first appointer's appointees list.");

        Founder freshFounderRole2 = (Founder) updatedAppointing.getRoleInCompany(secondCompanyId);
        assertFalse(freshFounderRole2.getAppointeesMemberIds().contains(200L),
                "Cancelled member should be removed from the second appointer's appointees list.");
    }

    @Test
    public void GivenMemberIsFounder_WhenCancelAllRolesForMember_ThenThrowsException() {
        appointingMember.addFounderRole(companyId);
        userRepository.updateMember(appointingMember);

        Exception exception = assertThrows(Exception.class, () -> {
            domainService.cancelAllRolesForMember(100L);
        });

        assertTrue(exception.getMessage().contains("Cannot delete user: The user is a Founder"));
    }

    @Test
    public void GivenCompanyHasActiveRoles_WhenCancelAllRolesForCompany_ThenAllCompanyRolesAreCancelled() {
        appointingMember.addFounderRole(companyId);

        targetMember.addManagerRole(companyId, 100L, new HashSet<>());
        targetMember.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);

        existingOwner.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);

        userRepository.updateMember(appointingMember);
        userRepository.updateMember(targetMember);
        userRepository.updateMember(existingOwner);

        domainService.cancelAllRolesForCompany(companyId);

        Member freshAppointing = userRepository.getMemberById(100L);
        Member freshTarget = userRepository.getMemberById(200L);
        Member freshExistingOwner = userRepository.getMemberById(existingOwnerId);

        assertEquals(RoleStatus.CANCELLED, freshAppointing.getRoleInCompany(companyId).getStatus());
        assertEquals(RoleStatus.CANCELLED, freshTarget.getRoleInCompany(companyId).getStatus());
        assertEquals(RoleStatus.CANCELLED, freshExistingOwner.getRoleInCompany(companyId).getStatus());
    }

    // =========================================================================================
    // Use Case 4.15: Roles and permissions tree - Domain logic
    // =========================================================================================

    @Test
    public void GivenFounderWithOwnerAndManager_WhenBuildRolesAndPermissionsTree_ThenTreeContainsRolesAndPermissions() throws Exception {
        appointingMember.addFounderRole(companyId);
        Founder founderRole = (Founder) appointingMember.getRoleInCompany(companyId);

        existingOwner.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        founderRole.addAppointee(existingOwnerId);

        Set<Permission> managerPermissions = new HashSet<>();
        managerPermissions.add(Permission.MANAGE_INQUIRIES);

        targetMember.addManagerRole(companyId, 100L, managerPermissions);
        targetMember.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        founderRole.addAppointee(200L);

        userRepository.updateMember(appointingMember);
        userRepository.updateMember(targetMember);
        userRepository.updateMember(existingOwner);

        String tree = domainService.buildRolesAndPermissionsTree(100L, companyId, 100L);

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
        appointingMember.addFounderRole(companyId);

        Set<Permission> managerPermissions = new HashSet<>();
        managerPermissions.add(Permission.MANAGE_INQUIRIES);

        targetMember.addManagerRole(companyId, 100L, managerPermissions);
        targetMember.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);

        userRepository.updateMember(appointingMember);
        userRepository.updateMember(targetMember);

        Exception exception = assertThrows(Exception.class, () -> {
            domainService.buildRolesAndPermissionsTree(200L, companyId, 100L);
        });

        assertEquals("Only Owners or Founder can perform this action.", exception.getMessage());
    }
}