package ticketsystem.DomainLayer;
<<<<<<< HEAD
import ticketsystem.DomainLayer.user.Permission;
import ticketsystem.DomainLayer.user.RoleStatus;
import ticketsystem.DomainLayer.user.CompanyRole;
<<<<<<< HEAD
import ticketsystem.DomainLayer.user.Founder;
import ticketsystem.DomainLayer.user.Owner;
import ticketsystem.DomainLayer.user.Manager;

public class MembershipDomainService {
=======
=======
import java.util.List;
import java.util.Set;
import ticketsystem.DomainLayer.user.Member;
import ticketsystem.DomainLayer.user.Owner;
import ticketsystem.DomainLayer.user.Permission;
import ticketsystem.DomainLayer.user.RoleStatus;
import ticketsystem.DomainLayer.company.Company;
import ticketsystem.DomainLayer.user.CompanyRole;
import ticketsystem.DomainLayer.user.Founder;
>>>>>>> e7f5697 (starting to implement giveup ownership use case)
import ticketsystem.DomainLayer.user.Manager;

public class MembershipDomainService {
<<<<<<< HEAD
    
    public CompanyRole assignManagerToCompany(long companyId, Member appointer, Member appointee, Set<Permission> permissions) throws Exception {
        if (!(appointer.getRole(companyId) instanceof Owner)) {
            throw new Exception("Only owners can assign managers.");
=======

    /**
     * Domain Validation: Core permission check for any action in the company.
     */
    public boolean validatePermission(CompanyRole role, Permission permission) {
        
        // 1. Check existence
        if (role == null) {
            return false;
        }
        
        // 2. Draft Entity State Check
        if (role.getStatus() != RoleStatus.ACTIVE) {
            return false;
        }
        
        // 3. Polymorphic permission evaluation
        return role.hasPermission(permission); 
    }

    /**
     * Domain Validation: Request phase of assigning a new Manager.
     */
    public void validateManagerAssignmentRequest(CompanyRole appointerRole, CompanyRole targetRole) throws Exception {
        
        // 1. Validate the appointer exists
        if (appointerRole == null) {
            throw new Exception("You do not have a role in this company.");
>>>>>>> 44d970c (Refactor UC 4.7 to use RoleStatus and a unified MembershipRepository)
        }
        
        // Validate that the appointer's role status is ACTIVE
        if (appointerRole.getStatus() != RoleStatus.ACTIVE) {
            throw new Exception("Your role is not active yet. You cannot appoint managers.");
        }

        // 2. Validate the appointer's role type
        if (!(appointerRole instanceof Owner) && !(appointerRole instanceof Founder)) {
            throw new Exception("Only Owners and Founders can appoint managers.");
        }

        // 3. Validate the target is free
        if (targetRole != null) {
            throw new Exception("This user already has an active or pending role in this company.");
        }
    }

    /**
     * Domain Validation & State Mutation: Approve phase of manager assignment.
     */
    public void validateAndApproveManager(CompanyRole approvedRole, CompanyRole parentRole, Long appointeeId) throws Exception {
        
        // 1. Validate the draft role
        if (approvedRole == null || !(approvedRole instanceof Manager)) {
            throw new Exception("No pending manager invitation found.");
        }
        if (approvedRole.getStatus() == RoleStatus.ACTIVE) {
            throw new Exception("This role is already active.");
        }
        
        // 2. Validate the parent (appointer) still exists and is capable of having appointees
        if (parentRole == null) {
            throw new Exception("Appointer does not have a role in this company anymore.");
        }
        
        // Block the approval if the parent is no longer ACTIVE
        if (parentRole.getStatus() != RoleStatus.ACTIVE) {
            throw new Exception("The user who appointed you is no longer active. Invitation is void.");
        }

        // 3. Mutate the state: Change status to ACTIVE
        ((Manager) approvedRole).activate();
        
        // 4. Mutate the state: Add to Parent's Appointees Tree
        if (parentRole instanceof Owner) {
            ((Owner) parentRole).addAppointee(appointeeId);
        } else if (parentRole instanceof Founder) {
            ((Founder) parentRole).addAppointee(appointeeId);
        } else {
            // This prevents data corruption if a Manager somehow tries to approve
            throw new Exception("Managers cannot appoint other managers.");
        }
    }

    /**
     * Domain Validation: Reject phase of manager assignment.
     * Ensures the role is safely in a PENDING state before allowing the Application Layer to delete it.
     */
    public void validateRejectManager(CompanyRole pendingRole) throws Exception {
        
        // 1. Ensure the draft exists
        if (pendingRole == null) {
            throw new Exception("No pending invitation found to reject.");
        }
        
        // 2. Ensure we don't accidentally delete an established, active role
        // Note for teammate: If a user wants to leave an ACTIVE role, they must use a "Resign" Use Case,
        // which handles shifting or deleting their subordinates in the tree. Reject is ONLY for drafts.
        if (pendingRole.getStatus() == RoleStatus.ACTIVE) {
            throw new Exception("You cannot reject a role that is already active. Please use the resign function.");
        }
    }
>>>>>>> 2d153d5 (Add unit tests for Member and CompanyRole classes)

<<<<<<< HEAD
<<<<<<< HEAD
    public boolean validatePermission(CompanyRole role, Permission permission) {
        
        // 1. Check existence
=======
    public void resignOwnershipFromCompany(Member resigningOwner, Company company) {
        CompanyRole role = resigningOwner.getRole(company.getId());
        if (validateResignation(role)) {
            Owner owner = (Owner) role;
            Long appointerId = owner.getAppointedByMemberId();
            List<Long> appointeesIds = owner.getAppointeesMemberIds();
            resigningOwner.deleteRole(company.getId());
            // TODO: delete owner from appointer's appointees list
            // TODO: transfer resiningOwner appointees to be appointed by resiningOwner's appointer
            // TODO: notify appointer and appointees of ownership resignation
            // TODO: save changes to the repository
        }
        else {
            throw new Exception("Invalid ownership resignation.");
        }
    }

<<<<<<< HEAD
    public boolean validatePermission(Member member, Long companyId, Permission permission) {
        CompanyRole role = member.getRole(companyId);
>>>>>>> e7f5697 (starting to implement giveup ownership use case)
        if (role == null) {
            return false;
        }
<<<<<<< HEAD
        
        // 2. Check role status - Only ACTIVE roles can perform actions
        if (role.getStatus() != RoleStatus.ACTIVE) {
            return false;
        }
        
        // 3. Polymorphic permission evaluation
=======
>>>>>>> 2d153d5 (Add unit tests for Member and CompanyRole classes)
        return role.hasPermission(permission); 
    }

<<<<<<< HEAD
    public void validateAssignmentRequest(CompanyRole appointerRole, CompanyRole targetRole) throws Exception {
        
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

        // If all validations pass, the service will handle the creation of the pending role
    }

    public void validateApproveAssignment(CompanyRole approvedRole, CompanyRole appointerRole, Long appointeeId) throws Exception {
        
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

        // 6. If validation passes, the service will handle the activation of the pending role
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
=======
=======
>>>>>>> 44d970c (Refactor UC 4.7 to use RoleStatus and a unified MembershipRepository)
    public boolean validateResignation(CompanyRole role) {
        if (role == null) {
            return false;
        }
        // Alternative Flow: Owner is the Founder
        if (role instanceof Founder) {
            return false;
        }
        if (!(role instanceof Owner)) {
            return false;
        }
        return true;
>>>>>>> e7f5697 (starting to implement giveup ownership use case)
    }

    public boolean validatePermission(CompanyRole role, Permission permission) {
        if (role == null) {
            return false;
        }
        return role.hasPermission(permission); 
    }
=======
>>>>>>> 4368f6f (Add comments)

}