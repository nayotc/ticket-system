package ticketsystem.DomainLayer;
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

    public void ApproveAssignment(Member appointer, Member appointee, Company company) throws Exception {
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

    public void RejectAssignment(Member appointer, Member appointee, Long companyId) throws Exception {
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
        appointee.deleteRoleInCompany(companyId);
    }

}