package ticketsystem.DomainLayer;
import ticketsystem.DomainLayer.user.Permission;
import ticketsystem.DomainLayer.user.RoleStatus;

import java.util.Set;

import ticketsystem.DomainLayer.user.CompanyRole;
import ticketsystem.DomainLayer.user.Founder;
import ticketsystem.DomainLayer.user.Owner;
import ticketsystem.DomainLayer.user.Manager;
import ticketsystem.DomainLayer.user.Member;

public class MembershipDomainService {

    public boolean validatePermission(Member member, Long companyId, Permission permission) {
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

    public void ManagerAssignmentRequest(Member appointer, Member targetMember, Long companyId, Set<Permission> permissions) throws Exception {
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
        targetMember.addManagerRole(companyId, appointer.getId(), permissions);
    }

    public void ApproveAssignment(Member appointer, Member appointee, Long companyId) throws Exception {
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
        
        // 5. Update the appointer's list of appointees if applicable
        addNewAppointeeToAppointer(appointerRole, appointee.getId());

        // 6. If validation passes, the service will handle the activation of the pending role
    }

    private void addNewAppointeeToAppointer(CompanyRole appointerRole, Long appointeeId) throws Exception {
        
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

    public void RejectAssignment(Long companyId, Member member) throws Exception {
        CompanyRole rejectedRole = member.getRoleInCompany(companyId);

        // 1. Validate the rejected role
        if (rejectedRole == null) {
            throw new Exception("No pending role invitation found.");
        }

        // 2. Ensure the role is in a PENDING state before allowing rejection
        if (rejectedRole.getStatus() == RoleStatus.ACTIVE) {
            throw new Exception("This role is already active and cannot be rejected.");
        }

        // 3. If validation passes, the service will handle the removal of the pending role
    }

    // This method abstracts the logic of determining the appointer ID based on the role type  
    public Long getAppointerId(CompanyRole role) {

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


}