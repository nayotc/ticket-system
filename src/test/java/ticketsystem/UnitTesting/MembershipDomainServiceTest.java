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
    
    private Member founder;
    private Member targetMember;
    private Member existingOwner;
    
    private Company company;
    private final Long companyId = 1L;
    private final Long existingOwnerId = 300L;

    @BeforeEach
    public void setUp() {
        userRepository = new UserRepository();
        domainService = new MembershipDomainService(userRepository);
        
        // Members start clean (Version 0)
        founder = new Member(100L, "FounderUser");
        targetMember = new Member(200L, "TargetUser"); 
        
        userRepository.addRegisteredMember(100L, founder, "password123");
        userRepository.addRegisteredMember(200L, targetMember, "password123");
        
        company = new Company("BGU Productions", 100L, new PurchasePolicy(), new DiscountPolicy(DiscountCompositionType.MAX));
        try { company.setId(companyId); } catch (Exception e) {}

        existingOwner = new Member(existingOwnerId, "ExistingOwner");
        userRepository.addRegisteredMember(existingOwnerId, existingOwner, "password123");
        
        // Setup a pre-existing Owner specifically for UC 4.9 & 4.10 tests
        makeActiveOwner(existingOwner, 100L);
    }

    // =========================================================================================
    // HELPER METHODS (To manage DB saves properly and avoid Optimistic Locking issues)
    // =========================================================================================

    private void makeActiveFounder(Member member) {
        member.addFounderRole(companyId);
        userRepository.updateMember(member);
    }

    private void makeActiveOwner(Member member, Long appointedBy) {
        member.addOwnerRole(companyId, appointedBy);
        member.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        userRepository.updateMember(member);
    }

    private void makePendingOwner(Member member, Long appointedBy) {
        member.addOwnerRole(companyId, appointedBy); 
        userRepository.updateMember(member);
    }

    private void makeActiveManager(Member member, Long appointedBy, Set<Permission> perms) {
        member.addManagerRole(companyId, appointedBy, perms);
        member.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        userRepository.updateMember(member);
    }

    private void makePendingManager(Member member, Long appointedBy, Set<Permission> perms) {
        member.addManagerRole(companyId, appointedBy, perms); 
        userRepository.updateMember(member);
    }

    private void makeCancelledRole(Member member, Long appointedBy) {
        member.addManagerRole(companyId, appointedBy, new HashSet<>());
        member.getRoleInCompany(companyId).setStatus(RoleStatus.CANCELLED);
        userRepository.updateMember(member);
    }

    // =========================================================================================
    // Validate Permission Tests
    // =========================================================================================

    @Test
    public void GivenNullRole_WhenValidatePermission_ThenReturnFalse() {
        assertFalse(domainService.validatePermission(100L, companyId, Permission.MANAGE_EVENT_INVENTORY));
    }

    @Test
    public void GivenPendingRole_WhenValidatePermission_ThenReturnFalse() {
        makePendingOwner(founder, 999L);
        assertFalse(domainService.validatePermission(100L, companyId, Permission.MANAGE_EVENT_INVENTORY));
    }

    @Test
    public void GivenActiveRoleWithPermission_WhenValidatePermission_ThenReturnTrue() {
        makeActiveFounder(founder); 
        assertTrue(domainService.validatePermission(100L, companyId, Permission.MANAGE_EVENT_INVENTORY));
    }

    @Test
    public void GivenCancelledRole_WhenValidatePermission_ThenReturnFalse() {
        makeCancelledRole(founder, 999L);
        assertFalse(domainService.validatePermission(100L, companyId, Permission.MANAGE_EVENT_INVENTORY));
    }

    // =========================================================================================
    // Manager Assignment Request Tests
    // =========================================================================================

    @Test
    public void GivenFounderHasNoRole_WhenManagerAssignmentRequest_ThenThrowException() {
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.managerAssignmentRequest(founder, targetMember, companyId, new HashSet<>());
        });
        assertEquals("You do not have a role in this company.", exception.getMessage());
    }

    @Test
    public void GivenFounderIsPending_WhenManagerAssignmentRequest_ThenThrowException() {
        makePendingOwner(founder, 999L);
        Member currentFounder = userRepository.getMemberById(100L);
        
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.managerAssignmentRequest(currentFounder, targetMember, companyId, new HashSet<>());
        });
        assertEquals("Your role is not active yet. You cannot appoint others.", exception.getMessage());
    }

    @Test
    public void GivenFounderIsManager_WhenManagerAssignmentRequest_ThenThrowException() {
        makeActiveManager(founder, 999L, new HashSet<>());
        Member currentFounder = userRepository.getMemberById(100L);
        
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.managerAssignmentRequest(currentFounder, targetMember, companyId, new HashSet<>());
        });
        assertEquals("Only Owners and Founders can appoint others.", exception.getMessage());
    }

    @Test
    public void GivenTargetMemberHasRole_WhenManagerAssignmentRequest_ThenThrowException() {
        makeActiveFounder(founder);
        makePendingManager(targetMember, 100L, new HashSet<>());
        
        Member currentFounder = userRepository.getMemberById(100L);
        Member currentTarget = userRepository.getMemberById(200L);

        Exception exception = assertThrows(Exception.class, () -> {
            domainService.managerAssignmentRequest(currentFounder, currentTarget, companyId, new HashSet<>());
        });
        assertEquals("This user already has an active or pending role in this company.", exception.getMessage());
    }

    @Test
    public void GivenValidOwner_WhenManagerAssignmentRequest_ThenAddRoleLocally() throws Exception {
        makeActiveFounder(founder);
        Member currentFounder = userRepository.getMemberById(100L);
        
        boolean result = domainService.managerAssignmentRequest(currentFounder, targetMember, companyId, new HashSet<>());
        assertTrue(result);
        
        // Asserting on the local object since domainService does not save this specific action to DB
        CompanyRole managerRole = targetMember.getRoleInCompany(companyId);
        assertNotNull(managerRole);
        assertTrue(managerRole instanceof Manager);
        assertEquals(RoleStatus.PENDING, managerRole.getStatus());
        assertEquals(100L, ((Manager) managerRole).getAppointedByMemberId());
    }

    @Test
    public void GivenTargetHasCancelledRole_WhenManagerAssignmentRequest_ThenRoleIsReplacedLocally() throws Exception {
        makeActiveFounder(founder);
        makeCancelledRole(targetMember, 999L);
        
        Member currentFounder = userRepository.getMemberById(100L);
        Member currentTarget = userRepository.getMemberById(200L);

        boolean result = domainService.managerAssignmentRequest(currentFounder, currentTarget, companyId, new HashSet<>());
        assertTrue(result);

        assertEquals(RoleStatus.PENDING, currentTarget.getRoleInCompany(companyId).getStatus());
    }

    // =========================================================================================
    // Approve & Reject Assignment Tests
    // =========================================================================================

    @Test
    public void GivenNoPendingRole_WhenApproveAssignment_ThenThrowException() {
        makeActiveFounder(founder);
        Member currentFounder = userRepository.getMemberById(100L);
        
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.approveAssignment(currentFounder, targetMember, company);
        });
        assertEquals("No pending role invitation found.", exception.getMessage());
    }

    @Test
    public void GivenRoleAlreadyActive_WhenApproveAssignment_ThenThrowException() {
        makeActiveFounder(founder);
        makeActiveManager(targetMember, 100L, new HashSet<>());
        
        Member currentFounder = userRepository.getMemberById(100L);
        Member currentTarget = userRepository.getMemberById(200L);

        Exception exception = assertThrows(Exception.class, () -> {
            domainService.approveAssignment(currentFounder, currentTarget, company);
        });
        assertEquals("This role is already active.", exception.getMessage());
    }
    
    @Test
    public void GivenValidApproval_WhenApproveAssignment_ThenStatusBecomesActiveLocally() throws Exception {
        makeActiveFounder(founder);
        makePendingManager(targetMember, 100L, new HashSet<>());

        Member currentFounder = userRepository.getMemberById(100L);
        Member currentTarget = userRepository.getMemberById(200L);

        domainService.approveAssignment(currentFounder, currentTarget, company);

        assertEquals(RoleStatus.ACTIVE, currentTarget.getRoleInCompany(companyId).getStatus());
        
        Founder founderRole = (Founder) currentFounder.getRoleInCompany(companyId);
        assertTrue(founderRole.getAppointeesMemberIds().contains(200L));
    }

    @Test
    public void GivenNoPendingRole_WhenRejectAssignment_ThenThrowException() {
        makeActiveFounder(founder);
        Member currentFounder = userRepository.getMemberById(100L);
        
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.rejectAssignment(currentFounder, targetMember, companyId);
        });
        assertEquals("No pending role invitation found.", exception.getMessage());
    }

    @Test
    public void GivenValidRejection_WhenRejectAssignment_ThenRoleDeletedLocally() throws Exception {
        makeActiveFounder(founder);
        
        Member currentFounder = userRepository.getMemberById(100L);
        Founder founderRole = (Founder) currentFounder.getRoleInCompany(companyId);
        founderRole.addAppointee(200L); 
        userRepository.updateMember(currentFounder);
        
        makePendingManager(targetMember, 100L, new HashSet<>());
        
        currentFounder = userRepository.getMemberById(100L);
        Member currentTarget = userRepository.getMemberById(200L);

        domainService.rejectAssignment(currentFounder, currentTarget, companyId);

        assertNull(currentTarget.getRoleInCompany(companyId));
        
        Founder updatedFounderRole = (Founder) currentFounder.getRoleInCompany(companyId);
        assertFalse(updatedFounderRole.getAppointeesMemberIds().contains(200L));
    }

    // =========================================================================================
    // Update Manager Permissions Tests
    // =========================================================================================

    @Test
    public void GivenValidFounder_WhenSetPermissions_ThenPermissionsAreUpdatedLocally() throws Exception {
        makeActiveFounder(founder); 
        makeActiveManager(targetMember, 100L, new HashSet<>());
        
        Member currentFounder = userRepository.getMemberById(100L);
        Member currentTarget = userRepository.getMemberById(200L);
        
        Set<Permission> newPerms = new HashSet<>();
        newPerms.add(Permission.MANAGE_EVENT_INVENTORY);
        
        boolean result = domainService.setPermissionsToManager(currentFounder, currentTarget, companyId, newPerms);
        
        assertTrue(result);
        Manager managerRole = (Manager) currentTarget.getRoleInCompany(companyId);
        assertTrue(managerRole.getPermissionKeys().contains(Permission.MANAGE_EVENT_INVENTORY.getKey()));
    }

    @Test
    public void GivenTargetIsPending_WhenSetPermissions_ThenThrowsException() {
        makeActiveFounder(founder);
        makePendingManager(targetMember, 100L, new HashSet<>()); 
        
        Member currentFounder = userRepository.getMemberById(100L);
        Member currentTarget = userRepository.getMemberById(200L);
        
        Set<Permission> newPerms = new HashSet<>();
        newPerms.add(Permission.MANAGE_INQUIRIES);
        
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.setPermissionsToManager(currentFounder, currentTarget, companyId, newPerms);
        });
        assertEquals("Cannot update permissions for a pending manager.", exception.getMessage());
    }

    // =========================================================================================
    // Owner Assignment Request Tests
    // =========================================================================================

    @Test
    public void GivenValidOwner_WhenOwnerAssignmentRequest_ThenAddRoleLocally() throws Exception {
        makeActiveFounder(founder);
        Member currentFounder = userRepository.getMemberById(100L);
        
        boolean result = domainService.ownerAssignmentRequest(currentFounder, targetMember, companyId);
        
        assertTrue(result);
        CompanyRole ownerRole = targetMember.getRoleInCompany(companyId);
        assertNotNull(ownerRole);
        assertTrue(ownerRole instanceof Owner);
        assertEquals(RoleStatus.PENDING, ownerRole.getStatus());
        assertEquals(100L, ((Owner) ownerRole).getAppointedByMemberId());
    }

    // =========================================================================================
    // Resignation, Transfer & Removal Tests
    // =========================================================================================

    @Test
    public void GivenOwnerWithSubordinates_WhenTransferAppointees_ThenSubordinatesMoveToFounderInDB() throws Exception {
        // Arrange
        makeActiveFounder(founder); 
        makeActiveOwner(targetMember, 100L);
        
        // Create subordinate manager
        Member subManager = new Member(400L, "SubManager");
        userRepository.addRegisteredMember(400L, subManager, "pass");
        makeActiveManager(subManager, 200L, new HashSet<>());
        
        // Link subordinate to owner and save to DB
        Member currentTarget = userRepository.getMemberById(200L);
        Owner ownerRole = (Owner) currentTarget.getRoleInCompany(companyId);
        ownerRole.addAppointee(400L);
        userRepository.updateMember(currentTarget);

        Member currentFounder = userRepository.getMemberById(100L);
        currentTarget = userRepository.getMemberById(200L); 
        
        // Act
        domainService.transferAppointees(currentTarget, currentFounder, companyId);
        
        // --- FIX: Explicitly save the resigning owner and new appointer to DB ---
        // Since transferAppointees only updates the subordinates in DB, 
        // we must save the changes made to currentTarget (list cleared) 
        // and currentFounder (list expanded).
        userRepository.updateMember(currentTarget);
        userRepository.updateMember(currentFounder);
        // ------------------------------------------------------------------------

        // Assert: Verify changes were persisted to DB
        Member dbFounder = userRepository.getMemberById(100L);
        Member dbTarget = userRepository.getMemberById(200L);
        Member dbSubManager = userRepository.getMemberById(400L);

        Founder dbFounderRole = (Founder) dbFounder.getRoleInCompany(companyId);
        Owner dbOwnerRole = (Owner) dbTarget.getRoleInCompany(companyId);
        Manager dbSubManagerRole = (Manager) dbSubManager.getRoleInCompany(companyId);

        assertTrue(dbFounderRole.getAppointeesMemberIds().contains(400L), "Founder should inherit the subordinate in DB.");
        assertEquals(0, dbOwnerRole.getAppointeesMemberIds().size(), "Resigning owner's list should be empty.");
        assertEquals(100L, dbSubManagerRole.getAppointedByMemberId(), "Transferred manager appointer should be updated to Founder ID.");
    }

    @Test
    public void GivenValidOwner_WhenValidateRemoveOwnerAssignment_ThenReturnsTrueAndCleansUpLocally() throws Exception {
        makeActiveFounder(founder);
        Member currentFounder = userRepository.getMemberById(100L);

        Founder founderRole = (Founder) currentFounder.getRoleInCompany(companyId);
        founderRole.addAppointee(existingOwnerId);
        userRepository.updateMember(currentFounder); 
        
        currentFounder = userRepository.getMemberById(100L);
        Member currentExistingOwner = userRepository.getMemberById(existingOwnerId);

        boolean result = domainService.validateRemoveOwnerAssignment(currentFounder, currentExistingOwner, company);

        assertTrue(result);
        assertNull(currentExistingOwner.getRoleInCompany(companyId), "Owner role should be deleted in memory.");
        
        Founder updatedFounderRole = (Founder) currentFounder.getRoleInCompany(companyId);
        assertFalse(updatedFounderRole.getAppointeesMemberIds().contains(existingOwnerId));
    }

    @Test
    public void GivenAuthorizedAppointer_WhenValidateRemoveManagerAssignment_ThenReturnsTrueLocally() throws Exception {
        makeActiveFounder(founder);
        Member currentFounder = userRepository.getMemberById(100L);
        
        Founder founderRole = (Founder) currentFounder.getRoleInCompany(companyId);
        founderRole.addAppointee(200L);
        userRepository.updateMember(currentFounder);

        makeActiveManager(targetMember, 100L, new HashSet<>());
        
        currentFounder = userRepository.getMemberById(100L);
        Member currentTarget = userRepository.getMemberById(200L);

        boolean result = domainService.validateRemoveManagerAssignment(currentFounder, currentTarget, companyId);

        assertTrue(result);
        assertNull(currentTarget.getRoleInCompany(companyId), "Manager role should be deleted locally.");
    }

    // =========================================================================================
    // Cancel Roles Tests (DB Driven)
    // =========================================================================================

    @Test
    public void GivenMemberWithNonFounderRoles_WhenCancelAllRolesForMember_ThenRolesAreCancelledInDB() throws Exception {
        Long secondCompanyId = 2L;

        makeActiveFounder(founder);
        makeActiveOwner(targetMember, 100L);
        
        // Link targetMember to founder
        Member currentFounder = userRepository.getMemberById(100L);
        Founder founderRole = (Founder) currentFounder.getRoleInCompany(companyId);
        founderRole.addAppointee(200L);
        userRepository.updateMember(currentFounder);

        // Setup second company roles directly for test
        currentFounder = userRepository.getMemberById(100L);
        currentFounder.addFounderRole(secondCompanyId);
        userRepository.updateMember(currentFounder);

        Member currentTarget = userRepository.getMemberById(200L);
        currentTarget.addManagerRole(secondCompanyId, 100L, new HashSet<>());
        currentTarget.getRoleInCompany(secondCompanyId).setStatus(RoleStatus.ACTIVE);
        userRepository.updateMember(currentTarget);

        currentFounder = userRepository.getMemberById(100L);
        Founder secondFounderRole = (Founder) currentFounder.getRoleInCompany(secondCompanyId);
        secondFounderRole.addAppointee(200L);
        userRepository.updateMember(currentFounder);

        // Act: domainService.cancelAllRolesForMember saves to DB internally
        domainService.cancelAllRolesForMember(200L);

        // Assert from DB
        Member dbTarget = userRepository.getMemberById(200L);
        assertEquals(RoleStatus.CANCELLED, dbTarget.getRoleInCompany(companyId).getStatus());
        assertEquals(RoleStatus.CANCELLED, dbTarget.getRoleInCompany(secondCompanyId).getStatus());
    }

    @Test
    public void GivenMemberIsFounder_WhenCancelAllRolesForMember_ThenThrowsException() {
        makeActiveFounder(founder);

        Exception exception = assertThrows(Exception.class, () -> {
            domainService.cancelAllRolesForMember(100L);
        });

        assertEquals("Cannot delete user: The user is a Founder of one or more companies.", exception.getMessage());
    }

    @Test
    public void GivenCompanyHasActiveRoles_WhenCancelAllRolesForCompany_ThenAllCompanyRolesAreCancelledInDB() {
        makeActiveFounder(founder);
        makeActiveManager(targetMember, 100L, new HashSet<>());

        // Act: Loops and saves to DB internally
        domainService.cancelAllRolesForCompany(companyId);

        // Assert from DB
        Member dbFounder = userRepository.getMemberById(100L);
        Member dbTarget = userRepository.getMemberById(200L);
        Member dbExistingOwner = userRepository.getMemberById(existingOwnerId);

        assertEquals(RoleStatus.CANCELLED, dbFounder.getRoleInCompany(companyId).getStatus());
        assertEquals(RoleStatus.CANCELLED, dbTarget.getRoleInCompany(companyId).getStatus());
        assertEquals(RoleStatus.CANCELLED, dbExistingOwner.getRoleInCompany(companyId).getStatus());
    }

    
    @Test
    public void GivenMemberWithoutRole_WhenAssignFounderRole_ThenFounderRoleIsActiveInDB() throws Exception {
        // Act: domainService.assignFounderRole saves to DB internally
        domainService.assignFounderRole(100L, companyId);

        // Assert from DB
        Member dbFounder = userRepository.getMemberById(100L);
        CompanyRole role = dbFounder.getRoleInCompany(companyId);

        assertNotNull(role, "Founder role should be assigned.");
        assertTrue(role instanceof Founder, "Assigned role should be Founder.");
        assertEquals(RoleStatus.ACTIVE, role.getStatus(), "Founder role should be active immediately.");
    }

    // =========================================================================================
    // Roles and permissions tree - Domain logic
    // =========================================================================================

    @Test
    public void GivenFounderWithOwnerAndManager_WhenBuildRolesAndPermissionsTree_ThenTreeContainsRolesAndPermissions() throws Exception {
        makeActiveFounder(founder);
        
        Member currentFounder = userRepository.getMemberById(100L);
        Founder founderRole = (Founder) currentFounder.getRoleInCompany(companyId);
        founderRole.addAppointee(existingOwnerId);
        userRepository.updateMember(currentFounder);

        Set<Permission> managerPermissions = new HashSet<>();
        managerPermissions.add(Permission.MANAGE_INQUIRIES);
        makeActiveManager(targetMember, 100L, managerPermissions);

        currentFounder = userRepository.getMemberById(100L);
        founderRole = (Founder) currentFounder.getRoleInCompany(companyId);
        founderRole.addAppointee(200L);
        userRepository.updateMember(currentFounder);

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
        assertTrue(tree.contains(Permission.MANAGE_INQUIRIES.getKey()), "Tree should include manager permissions.");
    }
}