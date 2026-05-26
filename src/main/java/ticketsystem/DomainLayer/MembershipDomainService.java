package ticketsystem.DomainLayer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.DomainLayer.company.Company;
import ticketsystem.DomainLayer.user.CompanyRole;
import ticketsystem.DomainLayer.user.Permission;
import ticketsystem.DomainLayer.user.RoleStatus;
import ticketsystem.DomainLayer.user.Founder;
import ticketsystem.DomainLayer.user.Owner;
import ticketsystem.DomainLayer.user.Manager;
import ticketsystem.DomainLayer.user.Member;

public class MembershipDomainService {

    private final IUserRepository userRepository;

    public MembershipDomainService(IUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean validatePermission(String sessionId, Long companyId, String permission) {
        return false;
    }

    public boolean validatePermission(Long memberId, Long companyId, Permission permission) {
        Member member = userRepository.getMemberById(memberId);
        CompanyRole memberRole = member.getRoleInCompany(companyId);

        if (memberRole == null) {
            return false;
        }
        
        if (memberRole.getStatus() != RoleStatus.ACTIVE) {
            return false;
        }
        
        // 3. Polymorphic permission evaluation
        return memberRole.hasPermission(permission); 
    }

    // This method abstracts the logic of determining the appointer ID based on the role type  
    public Long getAppointerId(Member appointee, Long companyId) {
        CompanyRole role = appointee.getRoleInCompany(companyId);

        if (role == null) {
            return null;
        }

        if (role instanceof Manager) {
            return ((Manager) role).getAppointedByMemberId();
        }

        else if (role instanceof Owner) {
            return ((Owner) role).getAppointedByMemberId();
        }

        else if (role instanceof Founder) {
            return null; // Founders not have an appointer
        }

        return null; // Default case
    }

    /**
     * Adds a new appointee to the appointer's list of subordinates based on their role.
     */
    public void addNewAppointeeToAppointer(CompanyRole appointerRole, Long appointeeId) throws Exception {
        if (appointerRole instanceof Owner) {
            ((Owner) appointerRole).addAppointee(appointeeId);
        }

        else if (appointerRole instanceof Founder) {
            ((Founder) appointerRole).addAppointee(appointeeId);
        }

        else {
            throw new Exception("Cannot add appointee: Managers do not have appointees.");
        }
    }

    /**
     * Removes an appointee from the appointer's list of subordinates based on their role.
     */
    public void deleteAppointeeFromAppointer(CompanyRole appointerRole, Long appointeeId) throws Exception {
        if (appointerRole instanceof Owner) {
            ((Owner) appointerRole).deleteAppointee(appointeeId);
        }

        else if (appointerRole instanceof Founder) {
            ((Founder) appointerRole).deleteAppointee(appointeeId);
        }

        else {
            throw new Exception("Cannot delete appointee: Managers do not have appointees.");
        }
    }

    public boolean managerAssignmentRequest(Member appointer, Member targetMember, Long companyId, Set<Permission> permissions) throws Exception {
        CompanyRole appointerRole = appointer.getRoleInCompany(companyId);
        CompanyRole targetRole = targetMember.getRoleInCompany(companyId);

        if (appointerRole == null) {
            throw new Exception("You do not have a role in this company.");
        }
        
        if (appointerRole.getStatus() != RoleStatus.ACTIVE) {
            throw new Exception("Your role is not active yet. You cannot appoint others.");
        }

        if (!(appointerRole instanceof Owner) && !(appointerRole instanceof Founder)) {
            throw new Exception("Only Owners and Founders can appoint others.");
        }

        if (targetRole != null && targetRole.getStatus() != RoleStatus.CANCELLED) {
            throw new Exception("This user already has an active or pending role in this company.");
        }

        if (targetRole != null && targetRole.getStatus() == RoleStatus.CANCELLED) {
            targetMember.deleteRoleInCompany(companyId);
        }

        return targetMember.addManagerRole(companyId, appointer.getId(), permissions) == true;
    }

    public boolean ownerAssignmentRequest(Member appointer, Member targetMember, Long companyId) throws Exception {
        CompanyRole appointerRole = appointer.getRoleInCompany(companyId);
        CompanyRole targetRole = targetMember.getRoleInCompany(companyId);

        if (appointerRole == null) {
            throw new Exception("You do not have a role in this company.");
        }
        
        if (appointerRole.getStatus() != RoleStatus.ACTIVE) {
            throw new Exception("Your role is not active yet. You cannot appoint others.");
        }

        if (!(appointerRole instanceof Owner) && !(appointerRole instanceof Founder)) {
            throw new Exception("Only Owners and Founders can appoint others.");
        }

        if (targetRole != null && targetRole.getStatus() != RoleStatus.CANCELLED) {
            throw new Exception("This user already has an active or pending role in this company.");
        }

        if (targetRole != null && targetRole.getStatus() == RoleStatus.CANCELLED) {
            targetMember.deleteRoleInCompany(companyId);
        }

        return targetMember.addOwnerRole(companyId, appointer.getId());
    }

    public boolean approveAssignment(Member appointer, Member appointee, Company company) throws Exception {
        Long companyId = company.getId();
        CompanyRole approvedRole = appointee.getRoleInCompany(companyId);
        CompanyRole appointerRole = appointer.getRoleInCompany(companyId);

        // 1. Validate the approved role
        if (approvedRole == null) {
            throw new Exception("No pending role invitation found.");
        }

        // 2. Validate the role is not in a ACTIVE state before allowing approval
        if (approvedRole.getStatus() == RoleStatus.ACTIVE) {
            throw new Exception("This role is already active.");
        }

        // 3. Validate the role is not in a PENDING state before allowing approval
        if (approvedRole.getStatus() != RoleStatus.PENDING) {
            throw new Exception("Only pending role invitations can be approved.");
        }
        
        // 4. Validate the appointer still exists and is capable of having appointees
        if (appointerRole == null) {
            throw new Exception("Appointer does not have a role in this company anymore.");
        }
        
        // 5. Validate the appointerws status is ACTIVE
        if (appointerRole.getStatus() != RoleStatus.ACTIVE) {
            throw new Exception("The user who appointed you is no longer active. Invitation is void.");
        }
        
        // 6. If all validations pass, activate the pending role and update the appointer's list of appointees
        approvedRole.setStatus(RoleStatus.ACTIVE);
        addNewAppointeeToAppointer(appointerRole, appointee.getId());
        return true;
    }

    public boolean rejectAssignment(Member appointer, Member appointee, Long companyId) throws Exception {
        CompanyRole rejectedRole = appointee.getRoleInCompany(companyId);
        CompanyRole appointerRole = appointer.getRoleInCompany(companyId);

        // 1. Validate the rejected role
        if (rejectedRole == null) {
            throw new Exception("No pending role invitation found.");
        }

        // 2. Validate the role is not in a ACTIVE state before allowing rejection
        if (rejectedRole.getStatus() == RoleStatus.ACTIVE) {
            throw new Exception("This role is already active and cannot be rejected.");
        }

        // 3. Validate the role is not in a PENDING state before allowing rejection
        if (rejectedRole.getStatus() != RoleStatus.PENDING) {
            throw new Exception("Only pending role invitations can be rejected.");
        }

        // 4. If validation passes, remove the pending role and update the appointer's list of appointees
        deleteAppointeeFromAppointer(appointerRole, appointee.getId());
        return appointee.deleteRoleInCompany(companyId) == true;
    }

    public boolean setPermissionsToManager(Member appointer, Member targetManager, Long companyId, Set<Permission> permissions) throws Exception {
        CompanyRole appointerRole = appointer.getRoleInCompany(companyId);
        CompanyRole managerRole = targetManager.getRoleInCompany(companyId);

        // 1. Validate the appointer exists
        if (appointerRole == null) {
            throw new Exception("You do not have a role in this company.");
        }
        
        // 2. Validate the appointer's role status
        if (appointerRole.getStatus() != RoleStatus.ACTIVE) {
            throw new Exception("Your role is not active yet. You cannot update others permissions.");
        }

        // 3. Validate the appointer's role type
        if (!(appointerRole instanceof Owner) && !(appointerRole instanceof Founder)) {
            throw new Exception("Only Owners and Founders can update manager's permissions.");
        }

        // 4. Validate the target has a Manager role
        if (managerRole == null || !(managerRole instanceof Manager)) {
            throw new Exception("The specified user does not hold a Manager role");
        }

        // 5. Validate that the target manager is ACTIVE (prevent changing permissions of a pending invitation)
        if (managerRole.getStatus() == RoleStatus.PENDING) {
            throw new Exception("Cannot update permissions for a pending manager.");
        }

        // 6. Validate permissions set is not null and does not contain null elements
        if (permissions == null || permissions.contains(null)) {
            throw new Exception("Permissions set cannot be null or contain null values.");
        }

        // 7. Validate the actor is the appointer of target user
        if (!java.util.Objects.equals(getAppointerId(targetManager, companyId), appointer.getId())) {
            throw new Exception("You are not the appointer of the specified user");
        }

        // 8. If all validations pass, update manager's permissions
        ((Manager) managerRole).setPermissions(permissions);
        return true;
    }

    public Set<Long> getManagementSubTreeMemberIds(Long rootMemberId, Long companyId) {
        Set<Long> result = new HashSet<>();
        collectManagementSubTree(rootMemberId, companyId, result);
        return result;
    }

    private void collectManagementSubTree(Long currentMemberId, Long companyId, Set<Long> result) {
        if (currentMemberId == null || result.contains(currentMemberId)) {
            return;
        }

        result.add(currentMemberId);

        Member member = userRepository.getMemberById(currentMemberId);
        if (member == null) {
            return;
        }

        CompanyRole role = member.getRoleInCompany(companyId);
        if (role == null || role.getStatus() != RoleStatus.ACTIVE) {
            return;
        }

        if (role instanceof Founder) {
            for (Long appointeeId : ((Founder) role).getAppointeesMemberIds()) {
                collectManagementSubTree(appointeeId, companyId, result);
            }
        }

        if (role instanceof Owner) {
            for (Long appointeeId : ((Owner) role).getAppointeesMemberIds()) {
                collectManagementSubTree(appointeeId, companyId, result);
            }
        }
    }

    /**
     * Checks if a member has an active role in a specific company.
     */
    public boolean isMemberInCompany(Long memberId, Long companyId) {
        Member member = userRepository.getMemberById(memberId);
        
        // If the member does not exist in the system
        if (member == null) {
            return false;
        }

        CompanyRole role = member.getRoleInCompany(companyId);
        
        // The member is considered part of the company only if they hold a valid role 
        // and its status is strictly ACTIVE (disallowing PENDING or CANCELLED statuses).
        return role != null && role.getStatus() == RoleStatus.ACTIVE;
    }

    /**
     * Transfers all appointees (subordinates) from a resigning member to a new appointer.
     * This ensures the management hierarchy remains intact when an Owner resigns or is removed.
     * * @param resigningMember The member who is leaving their role and transferring their appointees.
     * @param newAppointer The member who will inherit the appointees (usually the appointer of the resigning member).
     * @param companyId The ID of the company where this transfer is taking place.
     * @throws Exception if the resigning member is not an Owner, or if adding appointees fails.
     */
    public void transferAppointees(Member resigningMember, Member newAppointer, Long companyId) throws Exception {
        CompanyRole resigningRole = resigningMember.getRoleInCompany(companyId);
        CompanyRole newAppointerRole = newAppointer.getRoleInCompany(companyId);

        // We only allow transferring appointees from an Owner, as Managers do not have appointees 
        // and Founders cannot resign.
        if (resigningRole instanceof Owner) {
            Owner resigningOwner = (Owner) resigningRole;
            
            // Use a copy of the appointees set to avoid ConcurrentModificationException 
            // since we are iterating over the list and modifying relationships simultaneously.
            Set<Long> appointeesIds = new java.util.HashSet<>(resigningOwner.getAppointeesMemberIds());

            for (Long appointeeId : appointeesIds) {
                Member appointee = userRepository.getMemberById(appointeeId);
                if (appointee != null) {
                    CompanyRole appointeeRole = appointee.getRoleInCompany(companyId);
                    
                    // Update the subordinate's record to point to the new appointer's ID.
                    // We must check the specific role type (Manager or Owner) to cast it appropriately.
                    if (appointeeRole instanceof Manager) {
                        ((Manager) appointeeRole).setAppointer(newAppointer.getId());
                    } else if (appointeeRole instanceof Owner) {
                        ((Owner) appointeeRole).setAppointer(newAppointer.getId());
                    }
                    
                    // Add the subordinate to the new appointer's list using existing domain logic.
                    // This updates the new appointer's internal state.
                    addNewAppointeeToAppointer(newAppointerRole, appointeeId);
                    
                    // Persist the changes made to the subordinate (appointee) in the repository.
                    userRepository.updateMember(appointee);
                }
            }
            // Clear the resigning owner's list of appointees now that they have been successfully transferred.
            resigningOwner.getAppointeesMemberIds().clear();
        }
        else {
            throw new Exception("Only Owner's appointees can be transferred.");
        }
    }

    public boolean validateRemoveOwnerAssignment(Member appointer, Member appointee, Company company) throws Exception {
        Long companyId = company.getId();
        CompanyRole removedRole = appointee.getRoleInCompany(companyId);
        CompanyRole appointerRole = appointer.getRoleInCompany(companyId);

        if (appointerRole == null) {
            throw new Exception("You do not have a role in this company.");
        }
        if (appointerRole.getStatus() != RoleStatus.ACTIVE) {
            throw new Exception("Your role is not active yet. You cannot update others permissions.");
        }
        if (!(appointerRole instanceof Owner) && !(appointerRole instanceof Founder)) {
            throw new Exception("Only Owners and Founders can remove owner assignment.");
        }
        if (removedRole == null) {
            throw new Exception("The target user does not have a role in this company.");
        }
        if (!(removedRole instanceof Owner)) {
            throw new Exception("The target user is not an Owner.");
        }
        if (!java.util.Objects.equals(getAppointerId(appointee, companyId), appointer.getId())) {
            throw new Exception("You are not the appointer of the specified user");
        }
        
        transferAppointees(appointee, appointer, companyId);
        deleteAppointeeFromAppointer(appointerRole, appointee.getId());
        return appointee.deleteRoleInCompany(companyId);       
    }

    public boolean validateRemoveManagerAssignment(Member appointer, Member appointee, Long companyId) throws Exception {
        CompanyRole removedRole = appointee.getRoleInCompany(companyId);
        CompanyRole appointerRole = appointer.getRoleInCompany(companyId);

        if (appointerRole == null) {
            throw new Exception("You do not have a role in this company.");
        }
        if (appointerRole.getStatus() != RoleStatus.ACTIVE) {
            throw new Exception("Your role is not active yet. You cannot update others permissions.");
        }
        if (!(appointerRole instanceof Owner) && !(appointerRole instanceof Founder)) {
            throw new Exception("Only Owners and Founders can remove manager assignment.");
        }
        if (removedRole == null) {
            throw new Exception("The target user does not have a role in this company.");
        }
        if (removedRole.getStatus() != RoleStatus.ACTIVE) {
            throw new Exception("Your role is not active yet. You cannot remove it.");
        }
        if (!(removedRole instanceof Manager)) {
            throw new Exception("The target user is not a Manager.");
        }
        if (!java.util.Objects.equals(getAppointerId(appointee, companyId), appointer.getId())) {
            throw new Exception("You are not the appointer of the specified user");
        }

        deleteAppointeeFromAppointer(appointerRole, appointee.getId());
        return appointee.deleteRoleInCompany(companyId);        
    }

    public boolean validateOwnerResignation(CompanyRole role) throws Exception {
        if (role == null) {
            throw new Exception("You do not have a role in this company.");
        }
        if (role.getStatus() != RoleStatus.ACTIVE) {
            throw new Exception("Your role is not active yet. You cannot update others permissions.");
        }
        if (role instanceof Founder) {
            throw new Exception("A Founder cannot resign from the company.");
        }
        if (!(role instanceof Owner)) {
            throw new Exception("Only Owners can use this resignation process.");
        }
        return true;
    }

    public void assignFounderRole(Long memberId, Long companyId) throws Exception {
        Member member = userRepository.getMemberById(memberId);
        if (member == null) {
            throw new Exception("Member not found.");
        }

        CompanyRole existingRole = member.getRoleInCompany(companyId);
        if (existingRole != null && existingRole.getStatus() != RoleStatus.CANCELLED) {
            throw new Exception("Member already has a role in this company.");
        }

        if (existingRole != null && existingRole.getStatus() == RoleStatus.CANCELLED) {
            member.deleteRoleInCompany(companyId);
        }

        boolean added = member.addFounderRole(companyId);
        if (!added) {
            throw new Exception("Failed to assign founder role.");
        }

        if (!userRepository.updateMember(member)) {
            throw new Exception("Failed to persist founder role.");
        }
    }

    public void validateFounder(Long memberId, Long companyId) throws Exception {
        Member member = userRepository.getMemberById(memberId);
        if (member == null) {
            throw new Exception("Member not found.");
        }

        CompanyRole role = member.getRoleInCompany(companyId);

        if (!(role instanceof Founder) || role.getStatus() != RoleStatus.ACTIVE) {
            throw new Exception("Only the active Founder can perform this action.");
        }
    }

    public void validateOwnerOrFounder(Long memberId, Long companyId) throws Exception {
        Member member = userRepository.getMemberById(memberId);
        if (member == null) {
            throw new Exception("Member not found.");
        }

        CompanyRole role = member.getRoleInCompany(companyId);

        if (role == null || role.getStatus() != RoleStatus.ACTIVE) {
            throw new Exception("Member does not have an active role in this company.");
        }

        if (!(role instanceof Owner) && !(role instanceof Founder)) {
            throw new Exception("Only Owners or Founder can perform this action.");
        }
    }

    public boolean hasActiveRoleInCompany(Long memberId, Long companyId) {
        Member member = userRepository.getMemberById(memberId);
        if (member == null) {
            return false;
        }

        CompanyRole role = member.getRoleInCompany(companyId);
        return role != null && role.getStatus() == RoleStatus.ACTIVE;
    }

    public void cancelAllRolesForCompany(Long companyId) {
        for (Member member : userRepository.getAllMembers()) {
            CompanyRole role = member.getRoleInCompany(companyId);

            if (role != null && role.getStatus() != RoleStatus.CANCELLED) {
                role.cancel();
                userRepository.updateMember(member);
            }
        }
    }

    public String buildRolesAndPermissionsTree(Long requesterId, Long companyId, Long founderId) throws Exception {
        validateOwnerOrFounder(requesterId, companyId);

        StringBuilder sb = new StringBuilder();
        buildRoleTreeString(founderId, companyId, 0, sb, new HashSet<>());

        return sb.toString();
    }

    private void buildRoleTreeString(Long currentMemberId, Long companyId, int depth, StringBuilder sb, Set<Long> visited) {
        if (currentMemberId == null || visited.contains(currentMemberId)) {
            return;
        }

        visited.add(currentMemberId);

        Member member = userRepository.getMemberById(currentMemberId);
        if (member == null) {
            return;
        }

        CompanyRole role = member.getRoleInCompany(companyId);
        if (role == null || role.getStatus() != RoleStatus.ACTIVE) {
            return;
        }

        for (int i = 0; i < depth; i++) {
            sb.append("  ");
        }

        sb.append("- ID: ")
                .append(currentMemberId)
                .append(" (Role: ")
                .append(getRoleName(role))
                .append(")");

        String permissions = getPermissionString(role);
        if (permissions != null && !permissions.isBlank()) {
            sb.append(" [Permissions: ").append(permissions).append("]");
        }

        sb.append("\n");

        for (Long appointeeId : getAppointees(role)) {
            buildRoleTreeString(appointeeId, companyId, depth + 1, sb, visited);
        }
    }

    private String getRoleName(CompanyRole role) {
        if (role instanceof Founder) {
            return "FOUNDER";
        }

        if (role instanceof Owner) {
            return "OWNER";
        }

        if (role instanceof Manager) {
            return "MANAGER";
        }

        return "UNKNOWN";
    }

    private String getPermissionString(CompanyRole role) {
        if (role instanceof Founder || role instanceof Owner) {
            return "All Permissions";
        }

        if (role instanceof Manager) {
            Set<String> permissions = ((Manager) role).getPermissionKeys();
            return permissions.isEmpty() ? "None" : String.join(", ", permissions);
        }

        return "";
    }

    private List<Long> getAppointees(CompanyRole role) {
        if (role instanceof Founder) {
            return ((Founder) role).getAppointeesMemberIds();
        }

        if (role instanceof Owner) {
            return ((Owner) role).getAppointeesMemberIds();
        }

        return new java.util.ArrayList<>();
    }

    /**
     * Cancels all company roles held by a member.
     * Used by SystemAdminService as part of deleting a registered member.
     *
     * Founder roles are not cancelled here, because deleting a founder may violate
     * the invariant that an active company must have at least one owner/founder.
     * Such a case should be handled by a dedicated company/admin flow.
     *
     * @param memberIdToDelete id of the member whose roles should be cancelled
     * @throws Exception if the member does not exist or is a founder of any company
     */
    public void cancelAllRolesForMember(long memberIdToDelete) throws Exception {
        Member memberToDelete = userRepository.getMemberById(memberIdToDelete);
        if (memberToDelete == null) {
            throw new Exception("Member not found.");
        }

        // Work on the copy returned by the repository
        for (CompanyRole roleToCancel : memberToDelete.getAllRoles()) {
            if (roleToCancel.getStatus() == RoleStatus.CANCELLED) {
                continue;
            }

            Long companyId = roleToCancel.getCompanyId();
            if (roleToCancel instanceof Founder) {
                throw new Exception("Cannot delete user: The user is a Founder...");
            }

            removeCancelledMemberFromAppointer(memberToDelete, roleToCancel, companyId);
            if (roleToCancel instanceof Owner) {
                transferOwnerAppointeesBeforeCancellation(memberToDelete, companyId);
            }
            
            // Change status on the object inside the member
            roleToCancel.setStatus(RoleStatus.CANCELLED);
        }

        // MUST call update after modifying the member object
        userRepository.updateMember(memberToDelete);
    }

    /**
     * Removes the cancelled member from the appointer's appointees list.
     */
    private void removeCancelledMemberFromAppointer(Member memberToDelete,
                                                    CompanyRole roleToCancel,
                                                    Long companyId) throws Exception {
        Long appointerId = getAppointerId(memberToDelete, companyId);

        if (appointerId == null) {
            return;
        }

        Member appointer = userRepository.getMemberById(appointerId);
        if (appointer == null) {
            throw new Exception("Appointer not found.");
        }

        CompanyRole appointerRole = appointer.getRoleInCompany(companyId);
        if (appointerRole == null) {
            throw new Exception("Appointer does not have a role in this company.");
        }

        deleteAppointeeFromAppointer(appointerRole, memberToDelete.getId());
        userRepository.updateMember(appointer);
    }

    /**
     * Before cancelling an Owner role, transfers the owner's appointees to the
     * owner's appointer, so the management tree does not keep pointing to a
     * cancelled role.
     */
    private void transferOwnerAppointeesBeforeCancellation(Member ownerToCancel,
                                                        Long companyId) throws Exception {
        Long appointerId = getAppointerId(ownerToCancel, companyId);

        if (appointerId == null) {
            return;
        }

        Member appointer = userRepository.getMemberById(appointerId);
        if (appointer == null) {
            throw new Exception("Appointer not found.");
        }

        transferAppointees(ownerToCancel, appointer, companyId);
        userRepository.updateMember(appointer);
    }
}