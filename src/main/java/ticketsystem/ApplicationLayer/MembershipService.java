package ticketsystem.ApplicationLayer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import ticketsystem.DomainLayer.MembershipDomainService;
import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.DomainLayer.company.Company;
import ticketsystem.DomainLayer.user.CompanyRole;
import ticketsystem.DomainLayer.user.Manager;
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
        membershipDomain.managerAssignmentRequest(appointer, targetMember, companyId, permissions);

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
    
    /**
     * Use Case 4.12: Remove manager assignment
     */
    public boolean removeManagerAssignment(String sessionToken, Long companyId, Long targetMemberId) throws Exception {
        
        // 1. Authenticate session
        if (!tokenService.validateToken(sessionToken)) {
            throw new Exception("Session authentication failed.");
        }
        
        // 2. Retrieve appointer and target member information
        Long appointerId = tokenService.extractUserId(sessionToken);
        Member appointer = userRepository.getMemberById(appointerId);
        if (appointer == null) throw new Exception("Appointer not found.");

        Member targetMember = userRepository.getMemberById(targetMemberId);
        if (targetMember == null) throw new Exception("Target Member not found.");
        
        // 3. Execute domain logic (passing companyId directly)
        membershipDomain.validateRemoveManagerAssignment(appointer, targetMember, companyId);

        // 4. Persist changes in the repository
        userRepository.updateMember(appointer);
        userRepository.updateMember(targetMember);
        
        return true;
    }

    /**
     * Use Case 4.15: View roles and permissions tree
     */
    public String viewRolesAndPermissionsTree(String sessionToken, long companyId) throws Exception {
        // 1. Authenticate session
        if (!tokenService.validateToken(sessionToken)) {
            throw new Exception("Session authentication failed.");
        }
        
        // 2. Extract the requesting member ID
        long memberId = tokenService.extractUserId(sessionToken);

        // 3. Fetch the company
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new Exception("Error: Company not found."));

        // 4. PRE-AUTHORIZATION CHECK (Fail-Fast)

        if (!company.getOwners().contains(memberId)) {
            throw new Exception("The system rejects the request due to lack of permissions. Only Owners can view the roles tree.");
        }

        // 5. Build the permissions map (MemberID -> Permissions String)
        Map<Long, String> permissionsMap = new HashMap<>();

        // Add all owners to the map (Owners have full permissions implicitly)
        for (long ownerId : company.getOwners()) {
            permissionsMap.put(ownerId, "All Permissions");
        }

        // Add all managers and their specific permissions to the map
        for (long managerId : company.getManagers()) {
            try {
                // Fetch member by ID directly
                Member managerMember = userRepository.getMemberById(managerId);
                
                if (managerMember != null) {
                    CompanyRole role = managerMember.getRoleInCompany(companyId);
                    
                    if (role instanceof Manager) {
                        Set<String> perms = ((Manager) role).getPermissionKeys();
                        String permString = perms.isEmpty() ? "None" : String.join(", ", perms);
                        permissionsMap.put(managerId, permString);
                    }
                }
            } catch (Exception e) {
                // Ignore if user is not found to prevent the whole tree from failing
            }
        }

        // 6. Request the tree representation from the Company domain object
        return company.getRolesTreeRepresentation(memberId, permissionsMap);
    }

    /**
     * Use Case 4.10: Give Up ownership
     */
    public boolean giveUpOwnership(String sessionToken, Long companyId) throws Exception {

        //  Authenticate session
        if (!tokenService.validateToken(sessionToken)) {
            throw new Exception("Session authentication failed.");
        }
        
        // Extract user ID from token and retrieve member information
        Long memberId = tokenService.extractUserId(sessionToken);
        Member targetMember = userRepository.getMemberById(memberId);
        if (targetMember == null) {
            throw new Exception("Member not found.");
        }
        
        membershipDomain.validateOwnerResignation(targetMember.getRoleInCompany(companyId));
  
        // Hierarchy Transfer: Find the appointer of the resigning member
        Long appointerId = membershipDomain.getAppointerId(targetMember, companyId);
        if (appointerId != null) {
            Member appointer = userRepository.getMemberById(appointerId);
            if (appointer != null) {
                CompanyRole appointerRole = appointer.getRoleInCompany(companyId);
                membershipDomain.transferAppointees(targetMember, appointer, companyId);
                if (appointerRole != null) {
                    membershipDomain.deleteAppointeeFromAppointer(appointerRole, memberId);
                }
                else {
                    throw new Exception("Appointer do not have a role in this company.");
                }
                targetMember.deleteRoleInCompany(companyId);
                userRepository.updateMember(targetMember);
                userRepository.updateMember(appointer);
            }
            else {
                throw new Exception("Appointer not found.");
            }
        }

        return true;
    }

}