package ticketsystem.ApplicationLayer;
import java.util.Set;
import ticketsystem.DomainLayer.MembershipDomainService;
import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.DomainLayer.company.Company;
import ticketsystem.DomainLayer.user.Member;
import ticketsystem.DomainLayer.user.Permission;

public class MembershipService {

    private final ITokenService tokenService;
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
        
        // Extract user ID from token
        Long memberId = tokenService.extractUserId(sessionToken);
        
        // Use the domain service to validate the permission based on the member's role
        return membershipDomain.validatePermission(memberId, companyId, requiredPermission);
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
        if (appointer == null) {
            throw new Exception("Appointer not found.");
        }

        // Retrieve target member information
        Member targetMember = userRepository.getMemberById(targetMemberId);
        if (targetMember == null) {
            throw new Exception("Target Member not found.");
        }

        // Call the domain service to handle the business logic of creating the manager role
        membershipDomain.ManagerAssignmentRequest(appointer, targetMember, companyId, permissions);

        // Update the repository with the changes to the target member
        userRepository.updateMember(targetMember);
        
        // TODO: add notification to the target member about the pending assignment
        
        // Return true if the request was successfully processed
        return true;
    }

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
        boolean isAssignmentSuccessful = membershipDomain.OwnerAssignmentRequest(appointer, targetMember, companyId);

        // Update the repository with the changes to the target member ONLY if successful
        if (isAssignmentSuccessful) {
            userRepository.updateMember(targetMember);
        }
        
        // TODO: add notification to the target member about the pending assignment
        
        // Return the actual result from the domain layer
        return isAssignmentSuccessful;
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
        membershipDomain.ApproveAssignment(appointer, appointee, company);

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

        membershipDomain.RejectAssignment(appointer, appointee, companyId);

        // Update the repository with the changes to the appointee, appointer
        userRepository.updateMember(appointee);
        userRepository.updateMember(appointer);

        // 6. Return a success message or status
        return true;
    }

    

}