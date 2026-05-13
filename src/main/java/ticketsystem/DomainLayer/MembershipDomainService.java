package ticketsystem.DomainLayer;
import java.util.HashSet;
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

        // 1. Check existence
        if (memberRole == null) {
            return false;
        }
        
        // 2. Check role status - Only ACTIVE roles can perform actions
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
            return null; // No role found, cannot determine appointer
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

    public boolean managerAssignmentRequest(Member appointer, Member targetMember, Long companyId, Set<Permission> permissions) throws Exception {
        CompanyRole appointerRole = appointer.getRoleInCompany(companyId);
        CompanyRole targetRole = targetMember.getRoleInCompany(companyId);

        // 1. Validate the appointer exists
        if (appointerRole == null) {
            throw new Exception("You do not have a role in this company.");
        }
        
        // 2. Validate the appointer's role status
        if (appointerRole.getStatus() != RoleStatus.ACTIVE) {
            throw new Exception("Your role is not active yet. You cannot appoint others.");
        }

        // 3. Validate the appointer's role type
        if (!(appointerRole instanceof Owner) && !(appointerRole instanceof Founder)) {
            throw new Exception("Only Owners and Founders can appoint others.");
        }

        // 4. Validate the target is free
        if (targetRole != null) {
            throw new Exception("This user already has an active or pending role in this company.");
        }

        // 5. If all validations pass, add a pending Manager role
        return targetMember.addManagerRole(companyId, appointer.getId(), permissions) == true;
    }

    public boolean ownerAssignmentRequest(Member appointer, Member targetMember, Long companyId) throws Exception {
        CompanyRole appointerRole = appointer.getRoleInCompany(companyId);
        CompanyRole targetRole = targetMember.getRoleInCompany(companyId);

        // 1. Validate the appointer exists
        if (appointerRole == null) {
            throw new Exception("You do not have a role in this company.");
        }
        
        // 2. Validate the appointer's role status
        if (appointerRole.getStatus() != RoleStatus.ACTIVE) {
            throw new Exception("Your role is not active yet. You cannot appoint others.");
        }

        // 3. Validate the appointer's role type
        if (!(appointerRole instanceof Owner) && !(appointerRole instanceof Founder)) {
            throw new Exception("Only Owners and Founders can appoint others.");
        }

        // 4. Validate the target is free
        if (targetRole != null) {
            throw new Exception("This user already has an active or pending role in this company.");
        }

        // 5. If all validations pass, add a pending Owner role
        return targetMember.addOwnerRole(companyId, appointer.getId());
    }

    public void addNewAppointeeToAppointer(CompanyRole appointerRole, Long appointeeId) throws Exception {
        if (appointerRole instanceof Owner) {
            ((Owner) appointerRole).addAppointee(appointeeId);
        }

        else if (appointerRole instanceof Founder) {
            ((Founder) appointerRole).addAppointee(appointeeId);
        }

        else {
            // This prevents data corruption if a Manager somehow tries to approve
            throw new Exception("Managers cannot appoint others.");
        }
    }

    public boolean approveAssignment(Member appointer, Member appointee, Company company) throws Exception {
        Long companyId = company.getId();
        CompanyRole approvedRole = appointee.getRoleInCompany(companyId);
        CompanyRole appointerRole = appointer.getRoleInCompany(companyId);

        // 1. Validate the approved role
        if (approvedRole == null) {
            throw new Exception("No pending role invitation found.");
        }

        // 2. Ensure the role is in a PENDING state before allowing approval
        if (approvedRole.getStatus() == RoleStatus.ACTIVE) {
            throw new Exception("This role is already active.");
        }
        
        // 3. Validate the appointer still exists and is capable of having appointees
        if (appointerRole == null) {
            throw new Exception("Appointer does not have a role in this company anymore.");
        }
        
        // 4. Block the approval if the appointer is no longer ACTIVE
        if (appointerRole.getStatus() != RoleStatus.ACTIVE) {
            throw new Exception("The user who appointed you is no longer active. Invitation is void.");
        }
        
        // 5. If all validations pass, activate the pending role and update the appointer's list of appointees and the company's records
        approvedRole.setStatus(RoleStatus.ACTIVE);
        addNewAppointeeToAppointer(appointerRole, appointee.getId());
        company.registerNewAppointment(appointer.getId(), appointee.getId(), approvedRole instanceof Manager ? "Manager" : "Owner");
        return true;
    }

    public void deleteAppointeeFromAppointer(CompanyRole appointerRole, Long appointeeId) throws Exception {
        if (appointerRole instanceof Owner) {
            ((Owner) appointerRole).deleteAppointee(appointeeId);
        }

        else if (appointerRole instanceof Founder) {
            ((Founder) appointerRole).deleteAppointee(appointeeId);
        }

        else {
            throw new Exception("Appointer's role is not valid for this operation.");
        }
    }

    public boolean rejectAssignment(Member appointer, Member appointee, Long companyId) throws Exception {
        CompanyRole rejectedRole = appointee.getRoleInCompany(companyId);
        CompanyRole appointerRole = appointer.getRoleInCompany(companyId);

        // 1. Validate the rejected role
        if (rejectedRole == null) {
            throw new Exception("No pending role invitation found.");
        }

        // 2. Ensure the role is in a PENDING state before allowing rejection
        if (rejectedRole.getStatus() == RoleStatus.ACTIVE) {
            throw new Exception("This role is already active and cannot be rejected.");
        }

        // 3. If validation passes, remove the pending role and update the appointer's list of appointees
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

    public void transferAppointees(Member resigningMember, Member newAppointer, Long companyId) throws Exception {
        CompanyRole resigningRole = resigningMember.getRoleInCompany(companyId);
        CompanyRole newAppointerRole = newAppointer.getRoleInCompany(companyId);

        if (resigningRole instanceof Owner) {
            Owner resigningOwner = (Owner) resigningRole;
            // Use a copy to avoid ConcurrentModificationException during iteration
            Set<Long> appointeesIds = new java.util.HashSet<>(resigningOwner.getAppointeesMemberIds());

            for (Long appointeeId : appointeesIds) {
                Member appointee = userRepository.getMemberById(appointeeId);
                if (appointee != null) {
                    CompanyRole appointeeRole = appointee.getRoleInCompany(companyId);
                    
                    // Update the subordinate's record to point to the new appointer
                    if (appointeeRole instanceof Manager) {
                        ((Manager) appointeeRole).setAppointer(newAppointer.getId());
                    } else if (appointeeRole instanceof Owner) {
                        ((Owner) appointeeRole).setAppointer(newAppointer.getId());
                    }
                    
                    // Add the subordinate to the new appointer's list using existing domain logic
                    addNewAppointeeToAppointer(newAppointerRole, appointeeId);
                    userRepository.updateMember(appointee);
                }
            }
            resigningOwner.getAppointeesMemberIds().clear();
        }
        else {
            throw new Exception("Only Owner's appointees can be transfer.");
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
        
        // Removed the company tree update. Only transferring appointees and deleting the role.
        transferAppointees(appointee, appointer, companyId);
        company.removeUserFromAllRoles(appointee.getId());
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

}