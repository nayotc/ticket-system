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
        
        // Authenticate session
        if (!tokenService.validateToken(sessionToken)) {
            throw new Exception("Session authentication failed.");
        }
        
        // Extract user ID from token and retrieve member information
        Long memberId = tokenService.extractUserId(sessionToken);
        Member member = userRepository.getMemberById(memberId);
        
        // Use the domain service to validate the permission based on the member's role
        return domainService.validatePermission(member, companyId, requiredPermission);
    }

    /**
     * Use Case 4.7: Request to assign a manager to a company
     */
    public boolean requestManagerAssignment(String sessionToken, Long companyId, Long targetMemberId, Set<Permission> permissions) throws Exception {
        
        // Authenticate session
        if (!tokenService.validateToken(sessionToken)) {
            throw new Exception("Session authentication failed.");
        }
        
        // Extract user ID from token and retrieve member information
        Long appointerId = tokenService.extractUserId(sessionToken);
        Member appointer = userRepository.getMemberById(appointerId);

        // Retrieve target member information
        Member targetMember = userRepository.getMemberById(targetMemberId);

        // Call the domain service to handle the business logic of creating the manager role
        domainService.ManagerAssignmentRequest(companyId, appointer, targetMember, permissions);

        // Update the repository with the changes to the target member
        userRepository.updateMember(targetMember);
        
        // TODO: will be implemented in the future when the notifications service is ready
        
        // Return true if the request was successfully processed
        return true;
    }

    /**
     * Approve a pending assignment (Manager or Owner)
     */
    public boolean approveAssignment(String sessionToken, Long companyId) throws Exception {
        
        // 1. Authenticate session
        if (!tokenService.validateToken(sessionToken)) {
            throw new Exception("Session authentication failed.");
        }
        
        // 2. Extract user ID from token and retrieve member information
        Long appointeeId = tokenService.extractUserId(sessionToken);
        Member appointee = userRepository.getMemberById(appointeeId);
        CompanyRole approvedRole = appointee.getRoleInCompany(companyId);
        
        // 3. Extract the appointer's ID from the pending role and retrieve their information
        // TODO: check appointerId is not null, if null throw exception
        Long appointerId = domainService.getAppointerId(approvedRole);
        if (appointerId == null) {
            throw new Exception("The appointer ID could not be determined.");
        }
        Member appointer = userRepository.getMemberById(appointerId);
        CompanyRole appointerRole = appointer.getRoleInCompany(companyId);
        
        Optional<Company> company = companyRepository.findById(companyId.longValue());

        // TODO: move logic buisness to domain service
        // 4. Validate the approval action using the domain service
        domainService.validateApproveAssignment(approvedRole, appointerRole, appointeeId, company);
        approvedRole.setStatus(RoleStatus.ACTIVE);
        company.ifPresent(c -> c.registerNewAppointment(appointerId.longValue(), appointeeId.longValue(), 
                                                        approvedRole instanceof Manager ? "Manager" : "Owner"));
        // TODO

        // 5. Update the repository with the changes to both the appointee and appointer        
        userRepository.updateMember(appointee);
        userRepository.updateMember(appointer);

        // 7. Return a success message or status
        return true;
    }

    /**
     * Reject a pending assignment (Manager or Owner)
     */
    public boolean rejectAssignment(String sessionToken, Long companyId) throws Exception {
        
        // 1. Authenticate session
        if (!tokenService.validateToken(sessionToken)) {
            throw new Exception("Session authentication failed.");
        }
        
        // 2. Extract user ID from token and retrieve member information
        Long memberId = tokenService.extractUserId(sessionToken);
        Member member = userRepository.getMemberById(memberId);
        CompanyRole rejectedRole = member.getRoleInCompany(companyId);
        
        // 3. Extract the appointer's ID from the pending role and retrieve their information
        // TODO: check appointerId is not null, if null throw exception
        Long appointerId = domainService.getAppointerId(rejectedRole);
        if (appointerId == null) {
            throw new Exception("The appointer ID could not be determined.");
        }
        
        Member appointer = userRepository.getMemberById(appointerId);
        CompanyRole appointerRole = appointer.getRoleInCompany(companyId);

        // TODO: move logic buisness to domain service
        // 3. Validate the rejection action using the domain service
        domainService.validateRejectAssignment(rejectedRole);
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
        // TODO

        // Update the repository with the changes to the member
        userRepository.updateMember(member);
        
        // 6. Return a success message or status
        return true;
    }

    

}