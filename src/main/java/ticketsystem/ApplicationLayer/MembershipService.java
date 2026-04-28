package ticketsystem.ApplicationLayer;
import java.util.Optional;
import java.util.Set;
import ticketsystem.DomainLayer.MembershipDomainService;
import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.DomainLayer.company.Company;
import ticketsystem.DomainLayer.user.Member;
import ticketsystem.DomainLayer.user.CompanyRole;
import ticketsystem.DomainLayer.user.Permission;
import ticketsystem.DomainLayer.user.RoleStatus;
import ticketsystem.DomainLayer.user.Founder;
import ticketsystem.DomainLayer.user.Owner;
import ticketsystem.DomainLayer.user.Manager;

public class MembershipService {

    private final ITokenService tokenService;
    private final IUserRepository userRepository;
    private final ICompanyRepository companyRepository;
    private final MembershipDomainService domainService;
    private final NotificationsService notificationsService;

    public MembershipService(ITokenService tokenService, IUserRepository userRepository, ICompanyRepository companyRepository, MembershipDomainService domainService, NotificationsService notificationsService) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.domainService = domainService;
        this.notificationsService = notificationsService;
    }

    /**
     * Utility Method: Validate if a member has a specific permission within a company.
     * Used as a precondition check for various restricted Use Cases.
     */
    public boolean validatePermission(String sessionToken, Long companyId, Permission requiredPermission) throws Exception {
        
        // 1. Authenticate session
        if (!tokenService.validateToken(sessionToken)) {
            throw new Exception("Session authentication failed.");
        }
        
        // 2. Extract user ID from token and retrieve member information
        Long memberId = tokenService.extractUserId(sessionToken);
        Member member = userRepository.getMemberById(memberId);

        // 3. Retrieve the member's role in the specified company and validate the required permission
        CompanyRole memberRole = member.getRoleInCompany(companyId);
        return domainService.validatePermission(memberRole, requiredPermission);
    }

    /**
     * Use Case 4.7: Request to assign a manager to a company
     */
    public String requestManagerAssignment(String sessionToken, Long companyId, Long targetMemberId, Set<Permission> permissions) throws Exception {
        
        // 1. Authenticate session
        if (!tokenService.validateToken(sessionToken)) {
            throw new Exception("Session authentication failed.");
        }
        
        // 2. Extract user ID from token and retrieve member information
        Long appointerId = tokenService.extractUserId(sessionToken);
        Member appointer = userRepository.getMemberById(appointerId);
        CompanyRole appointerRole = appointer.getRoleInCompany(companyId);

        // 3. Retrieve target member information
        Member targetMember = userRepository.getMemberById(targetMemberId);
        CompanyRole targetRole = targetMember.getRoleInCompany(companyId);

        // 4. Validate the assignment request using the domain service
        domainService.validateAssignmentRequest(appointerRole, targetRole);
        
        // 5. If validation passes, add a pending Manager role to the target member and notify them
        targetMember.addManagerRole(companyId, appointerId, permissions);
        userRepository.updateMember(targetMember);
        // TODO: Consider what information to include in the notification (e.g., company name, permissions assigned) and implement the notification content accordingly.
        // notificationsService.notifyUser(targetMemberId, "You have been assigned to become a manager at " + companyRepository.findById(companyId).getName() + ". Please review and approve or reject this assignment.");
        
        // 6. Return a success message or status
        return "Manager assignment request sent successfully.";
    }

    /**
     * Approve a pending assignment (Manager or Owner)
     */
    public String approveAssignment(String sessionToken, Long companyId) throws Exception {
        
        // 1. Authenticate session
        if (!tokenService.validateToken(sessionToken)) {
            throw new Exception("Session authentication failed.");
        }
        
        // 2. Extract user ID from token and retrieve member information
        Long appointeeId = tokenService.extractUserId(sessionToken);
        Member appointee = userRepository.getMemberById(appointeeId);
        CompanyRole approvedRole = appointee.getRoleInCompany(companyId);
        
        // 3. Extract the appointer's ID from the pending role and retrieve their information
        Long appointerId;
        if (approvedRole == null) {
            throw new Exception("No pending role invitation found.");
        }
        else if (approvedRole instanceof Manager) {
            appointerId = ((Manager) approvedRole).getAppointedByMemberId();
        }
        else if (approvedRole instanceof Owner) {
            appointerId = ((Owner) approvedRole).getAppointedByMemberId();
        }
        else {
            // A Founder or unexpected role type cannot be "approved" this way.
            throw new Exception("The role found is not eligible for approval.");
        }

        Member appointer = userRepository.getMemberById(appointerId);
        CompanyRole appointerRole = appointer.getRoleInCompany(companyId);
        
        // 4. Validate the approval action using the domain service
        domainService.validateApproveAssignment(approvedRole, appointerRole, appointeeId);
        
        // 5. If validation passes, update the repository with the changes to both the appointee and appointer
        approvedRole.setStatus(RoleStatus.ACTIVE);
        userRepository.updateMember(appointee);
        userRepository.updateMember(appointer);
        
        // 6. Update the company's tree
        Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new Exception("Error: Company not found."));
        company.registerNewAppointment(appointerId, appointeeId, approvedRole instanceof Manager ? "Manager" : "Owner");

        // 7. Return a success message or status
        return "Assignment approved successfully.";
    }

    /**
     * Reject a pending assignment (Manager or Owner)
     */
    public String rejectAssignment(String sessionToken, Long companyId) throws Exception {
        
        // 1. Authenticate session
        if (!tokenService.validateToken(sessionToken)) {
            throw new Exception("Session authentication failed.");
        }
        
        // 2. Extract user ID from token and retrieve member information
        Long memberId = tokenService.extractUserId(sessionToken);
        Member member = userRepository.getMemberById(memberId);
        CompanyRole rejectedRole = member.getRoleInCompany(companyId);
        
        // 3. Validate the rejection action using the domain service
        domainService.validateRejectAssignment(rejectedRole);
        
        // 3. If validation passes, extract the appointer's ID from the pending role and retrieve their information
        Long appointerId = null;
        if (rejectedRole instanceof Manager) {
            appointerId = ((Manager) rejectedRole).getAppointedByMemberId();
        } else if (rejectedRole instanceof Owner) {
            appointerId = ((Owner) rejectedRole).getAppointedByMemberId();
        }
        else {
            throw new Exception("The role found is not eligible for rejection.");
        }
        
        Member appointer = userRepository.getMemberById(appointerId);
        CompanyRole appointerRole = appointer.getRoleInCompany(companyId);

        // 4. Update the appointer's list of appointees to remove the rejected member and update the repository
        if (appointerRole instanceof Owner) {
            ((Owner) appointerRole).deleteAppointee(memberId);
            userRepository.updateMember(appointer);
        }
        else if (appointerRole instanceof Founder) {
            ((Founder) appointerRole).deleteAppointee(memberId);
            userRepository.updateMember(appointer);
        }
        else {
            throw new Exception("Appointer's role is not valid for this operation.");
        }
        
        // 5. Remove the pending role from the member and update the repository
        member.deleteRoleInCompany(companyId);
        userRepository.updateMember(member);
        
        // 6. Return a success message or status
        return "Assignment rejected successfully.";
    }

}