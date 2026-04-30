package ticketsystem.ApplicationLayer;
import java.util.Set;
import ticketsystem.DomainLayer.MembershipDomainService;
import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.IRepository.IMembershipRepository;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.DomainLayer.company.Company;
import ticketsystem.DomainLayer.user.Member;
import ticketsystem.DomainLayer.user.Permission;

public class MembershipService {

    private final ITokenService tokenService;
<<<<<<< HEAD
    private final IUserRepository userRepository;
    private final ICompanyRepository companyRepository;
    private final MembershipDomainService membershipDomain;
    private final NotificationsService notificationsService;

    public MembershipService(ITokenService tokenService, IUserRepository userRepository, ICompanyRepository companyRepository, MembershipDomainService membershipDomain, NotificationsService notificationsService) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.membershipDomain = membershipDomain;
        this.notificationsService = notificationsService;
=======
    //private final IUserRepository userRepository;
    //private final ICompanyRepository companyRepository;
    private final IMembershipRepository membershipRepository;
    private final MembershipDomainService domainService;

    public MembershipService(ITokenService tokenService, IMembershipRepository membershipRepository, MembershipDomainService domainService) {
        this.tokenService = tokenService;
        this.membershipRepository = membershipRepository;
        this.domainService = domainService;
>>>>>>> 44d970c (Refactor UC 4.7 to use RoleStatus and a unified MembershipRepository)
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
<<<<<<< HEAD
        
        // Extract user ID from token
        Long memberId = tokenService.extractUserId(sessionToken);
        
        // Use the domain service to validate the permission based on the member's role
        return membershipDomain.validatePermission(memberId, companyId, requiredPermission);
=======
        // TODO: delete casting to Long after memberId is changed to long in tokenService.extractSubject
        Long memberId = Long.parseLong(tokenService.extractSubject(sessionToken));
        CompanyRole appointerRole = membershipRepository.findRole(companyId, memberId);
        CompanyRole targetRole = membershipRepository.findRole(companyId, targetMemberId);
        domainService.validateManagerAssignmentRequest(appointerRole, targetRole);        
        Manager newManager = new Manager(targetMemberId, companyId, permissions, memberId);
        membershipRepository.addRole(newManager);
>>>>>>> 44d970c (Refactor UC 4.7 to use RoleStatus and a unified MembershipRepository)
    }

    /**
     * Use Case 4.7: Request to assign a manager to a company
     */
    public boolean requestManagerAssignment(String sessionToken, Long companyId, Long targetMemberId, Set<Permission> permissions) throws Exception {
        
        // Authenticate session
        if (!tokenService.validateToken(sessionToken)) {
            throw new Exception("Session authentication failed.");
        }
<<<<<<< HEAD
        
        // Extract user ID from token and retrieve member information
        Long appointerId = tokenService.extractUserId(sessionToken);
=======
        // TODO: delete casting to Long after memberId is changed to long in tokenService.extractSubject
        Long appointeeId = Long.parseLong(tokenService.extractSubject(sessionToken));
        CompanyRole approvedRole = membershipRepository.findRole(companyId, appointeeId);
        if (approvedRole == null) {
            throw new Exception("No pending invitation found.");
        }
        if (!(approvedRole instanceof Manager)) {
            throw new Exception("The pending role found is not a manager role.");
        }
        Long appointerId = ((Manager) approvedRole).getAppointedByMemberId();
        CompanyRole parentRole = membershipRepository.findRole(companyId, appointerId);
        domainService.validateAndApproveManager(approvedRole, parentRole, appointeeId);
        membershipRepository.updateRole(approvedRole);
        membershipRepository.updateRole(parentRole);
        // TODO: Update and Notify to the Company on the New Manager
        Member appointee = userRepository.getMemberById(appointeeId);
>>>>>>> 44d970c (Refactor UC 4.7 to use RoleStatus and a unified MembershipRepository)
        Member appointer = userRepository.getMemberById(appointerId);
        if (appointer == null) {
            throw new Exception("Appointer not found.");
        }

        // Retrieve target member information
        Member targetMember = userRepository.getMemberById(targetMemberId);
        if (targetMember == null) {
            throw new Exception("Target Member not found.");
        }

        // Call the domain service to handle the business logic of creating the manager role
        membershipDomain.managerAssignmentRequest(appointer, targetMember, companyId, permissions);

        // Update the repository with the changes to the target member
        userRepository.updateMember(targetMember);
        
        // TODO: add notification to the target member about the pending assignment
        
        // Return true if the request was successfully processed
        return true;
    }
<<<<<<< HEAD

/**
     * Use Case 4.8: Request to assign an owner to a company
     */
    public boolean requestOwnerAssignment(String sessionToken, Long companyId, Long targetMemberId) throws Exception {

        // Authenticate session
        if (!tokenService.validateToken(sessionToken)) {
            throw new Exception("Session authentication failed.");
        }
        
        // Extract user ID from token and retrieve member information
        Long appointerId = tokenService.extractUserId(sessionToken);
        Member appointer = userRepository.getMemberById(appointerId);
        if (appointer == null) {
            throw new Exception("Appointer not found.");
        }

        // Retrieve target member information
        Member targetMember = userRepository.getMemberById(targetMemberId);
        if (targetMember == null) {
            throw new Exception("Target Member not found.");
        }

        // Call the domain service to handle the business logic of creating the owner role
        if (!membershipDomain.ownerAssignmentRequest(appointer, targetMember, companyId)) {
            return false;
        }

        // Update the repository with the changes to the target member ONLY if successful
        userRepository.updateMember(targetMember);
        
        // TODO: add notification to the target member about the pending assignment
        
        // Return the actual result from the domain layer
        return true;
    }

    /**
     * Use-case 4.9: Remove owner assignment
     */
    public boolean removeOwnerAssignment(String sessionToken, Long companyId, Long targetMemberId) throws Exception {
        if (!tokenService.validateToken(sessionToken)) {
            throw new Exception("Session authentication failed.");
        }
        
        Long appointerId = tokenService.extractUserId(sessionToken);
        Member appointer = userRepository.getMemberById(appointerId);
        if (appointer == null) {
            throw new Exception("Appointer not found.");
        }

        Member targetMember = userRepository.getMemberById(targetMemberId);
        if (targetMember == null) {
            throw new Exception("Target Member not found.");
        }

        Company company = companyRepository.findById(companyId).orElseThrow(() -> new Exception("Company not found."));
        
        // Passing companyId directly instead of querying the Company object
        membershipDomain.validateRemoveOwnerAssignment(appointer, targetMember, company);

        userRepository.updateMember(appointer);
        userRepository.updateMember(targetMember);
        companyRepository.save(company);
        
        return true;
    }

    /**
     * Use Case 4.11: Update manager permissions
     */
    public boolean updateManagerPermissions(String sessionToken, Long companyId, Long managerId, Set<Permission> permissions) throws Exception {
        
        // Authenticate session
        if (!tokenService.validateToken(sessionToken)) {
            throw new Exception("Session authentication failed.");
        }
        
        // Extract user ID from token and retrieve member information
        Long appointerId = tokenService.extractUserId(sessionToken);
        Member appointer = userRepository.getMemberById(appointerId);
        if (appointer == null) {
            throw new Exception("Appointer not found.");
        }

        // Retrieve target member information
        Member targetManager = userRepository.getMemberById(managerId);
        if (targetManager == null) {
            throw new Exception("Target Manager not found.");
        }

        // Call the domain service to handle the business logic of updating manager's permissions
        membershipDomain.setPermissionsToManager(appointer, targetManager, companyId, permissions);

        // Update the repository with the changes to the target member
        userRepository.updateMember(targetManager);
        
        // TODO: add notification to the target member about the pending assignment
        
        // Return true if the request was successfully processed
        return true;
    }

    /**
     * Approve a pending assignment (Manager or Owner)
     */
    public boolean approveAssignment(String sessionToken, Long companyId) throws Exception {
        
        // Authenticate session
        if (!tokenService.validateToken(sessionToken)) {
            throw new Exception("Session authentication failed.");
        }
        
        // Extract user ID from token and retrieve member information
        Long appointeeId = tokenService.extractUserId(sessionToken);
        Member appointee = userRepository.getMemberById(appointeeId);
        if (appointee == null) {
            throw new Exception("Appointee not found.");
        }
        
        Long appointerId = membershipDomain.getAppointerId(appointee, companyId);
        if (appointerId == null) {
            throw new Exception("The appointer ID could not be determined.");
        }

        Member appointer = userRepository.getMemberById(appointerId);
        if (appointer == null) {
            throw new Exception("Appointer not found.");
        }

        Company company = companyRepository.findById(companyId).orElseThrow(() -> new Exception("Company not found."));
        membershipDomain.approveAssignment(appointer, appointee, company);

        // Update the repository with the changes to both the appointee, appointer and company        
        userRepository.updateMember(appointee);
        userRepository.updateMember(appointer);
        companyRepository.save(company);

        // Return a success message or status
        return true;
    }

    /**
     * Reject a pending assignment (Manager or Owner)
     */
    public boolean rejectAssignment(String sessionToken, Long companyId) throws Exception {
        
        //  Authenticate session
        if (!tokenService.validateToken(sessionToken)) {
            throw new Exception("Session authentication failed.");
        }
        
        // Extract user ID from token and retrieve member information
        Long memberId = tokenService.extractUserId(sessionToken);
        Member appointee = userRepository.getMemberById(memberId);
        if (appointee == null) {
            throw new Exception("Appointee not found.");
        }

        // Extract user ID from token and retrieve member information
        Long appointerId = membershipDomain.getAppointerId(appointee, companyId);
        if (appointerId == null) {
            throw new Exception("The appointer ID could not be determined.");
        }
        
        Member appointer = userRepository.getMemberById(appointerId);
        if (appointer == null) {
            throw new Exception("Appointer not found.");
        }

        membershipDomain.rejectAssignment(appointer, appointee, companyId);

        // Update the repository with the changes to the appointee, appointer
        userRepository.updateMember(appointee);
        userRepository.updateMember(appointer);

        // 6. Return a success message or status
        return true;
    }

    
=======
    
    public void rejectManagerAssignment(String sessionToken, Long companyId) throws Exception {
        if (!tokenService.validateToken(sessionToken)) {
            throw new Exception("Session authentication failed.");
        }
        // TODO: delete casting to Long after memberId is changed to long in tokenService.extractSubject
        Long appointeeId = Long.parseLong(tokenService.extractSubject(sessionToken));
        CompanyRole pendingRole = membershipRepository.findRole(companyId, appointeeId);
        domainService.validateRejectManager(pendingRole);
        // Optional: Get appointer ID before we delete the role, so we can notify them.
        Long appointerId = ((Manager) pendingRole).getAppointedByMemberId();
        membershipRepository.deleteRole(companyId, appointeeId);
    }

    public boolean validatePermission(String sessionToken, Long companyId, Permission requiredPermission) throws Exception {
        if (!tokenService.validateToken(sessionToken)) {
            throw new Exception("Session authentication failed.");
        }
        // TODO: delete casting to Long after memberId is changed to long in tokenService.extractSubject
        Long memberId = Long.parseLong(tokenService.extractSubject(sessionToken));
        CompanyRole memberRole = membershipRepository.findRole(companyId, memberId);
        return domainService.validatePermission(memberRole, requiredPermission);
    }
>>>>>>> 44d970c (Refactor UC 4.7 to use RoleStatus and a unified MembershipRepository)

}