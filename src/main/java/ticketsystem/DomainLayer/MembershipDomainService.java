package ticketsystem.DomainLayer;
import ticketsystem.DomainLayer.user.Permission;
import ticketsystem.DomainLayer.user.RoleStatus;
import ticketsystem.DomainLayer.user.CompanyRole;
import ticketsystem.DomainLayer.user.Founder;
import ticketsystem.DomainLayer.user.Owner;
import ticketsystem.DomainLayer.user.Manager;

public class MembershipDomainService {
<<<<<<< HEAD
    
    public CompanyRole assignManagerToCompany(long companyId, Member appointer, Member appointee, Set<Permission> permissions) throws Exception {
        if (!(appointer.getRole(companyId) instanceof Owner)) {
            throw new Exception("Only owners can assign managers.");
=======
=======
import ticketsystem.DomainLayer.user.Manager;

public class MembershipDomainService {
>>>>>>> 5c34fef (implementation of use-case 4.7)

    public boolean validatePermission(CompanyRole role, Permission permission) {
        
        // 1. Check existence
        if (role == null) {
            return false;
        }
        
        // 2. Check role status - Only ACTIVE roles can perform actions
        if (role.getStatus() != RoleStatus.ACTIVE) {
            return false;
        }
        
        // 3. Polymorphic permission evaluation
        return role.hasPermission(permission); 
    }

    public void validateManagerAssignmentRequest(CompanyRole appointerRole, CompanyRole targetRole) throws Exception {
        
        // 1. Validate the appointer exists
        if (appointerRole == null) {
            throw new Exception("You do not have a role in this company.");
<<<<<<< HEAD
>>>>>>> 44d970c (Refactor UC 4.7 to use RoleStatus and a unified MembershipRepository)
=======
>>>>>>> 5c34fef (implementation of use-case 4.7)
        }
        
        // 2. Validate the appointer's role status
        if (appointerRole.getStatus() != RoleStatus.ACTIVE) {
            throw new Exception("Your role is not active yet. You cannot appoint managers.");
        }

        // 3. Validate the appointer's role type
        if (!(appointerRole instanceof Owner) && !(appointerRole instanceof Founder)) {
            throw new Exception("Only Owners and Founders can appoint managers.");
        }

        // 4. Validate the target is free
        if (targetRole != null) {
            throw new Exception("This user already has an active or pending role in this company.");
        }
    }

    public void validateAndApproveAssignment(CompanyRole approvedRole, CompanyRole appointerRole, Long appointeeId) throws Exception {
        
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
        if (appointerRole instanceof Owner) {
            ((Owner) appointerRole).addAppointee(appointeeId);
        } else if (appointerRole instanceof Founder) {
            ((Founder) appointerRole).addAppointee(appointeeId);
        } else {
            // This prevents data corruption if a Manager somehow tries to approve
            throw new Exception("Managers cannot appoint others.");
        }

        // 6. Finally, set the approved role to ACTIVE
        approvedRole.setStatus(RoleStatus.ACTIVE);
    }

    public void validateRejectAssignment(CompanyRole rejectedRole) throws Exception {
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


}