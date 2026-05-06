package ticketsystem.ApplicationLayer;
<<<<<<< HEAD
import java.util.Optional;
=======
import java.util.HashMap;
import java.util.Map;
>>>>>>> 1d842e6 (Add uc 4.15 View roles and permissions tree)
import java.util.Set;
import ticketsystem.DomainLayer.MembershipDomainService;
import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.IRepository.IMembershipRepository;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.DomainLayer.company.Company;
import ticketsystem.DomainLayer.user.Member;
import ticketsystem.DomainLayer.user.Owner;
import ticketsystem.DomainLayer.user.CompanyRole;
<<<<<<< HEAD
=======
import ticketsystem.DomainLayer.user.Founder;
import ticketsystem.DomainLayer.user.Manager;
>>>>>>> e663313 (implementation of use-case 4.7)
import ticketsystem.DomainLayer.user.Permission;
import ticketsystem.DomainLayer.user.RoleStatus;
import ticketsystem.DomainLayer.user.Founder;
import ticketsystem.DomainLayer.user.Owner;
import ticketsystem.DomainLayer.user.Manager;

public class MembershipService {

    private final ITokenService tokenService;
<<<<<<< HEAD
<<<<<<< HEAD
    private final IUserRepository userRepository;
    private final ICompanyRepository companyRepository;
<<<<<<< HEAD
    private final MembershipDomainService domainService;
    private final NotificationsService notificationsService;

    public MembershipService(ITokenService tokenService, IUserRepository userRepository, ICompanyRepository companyRepository, MembershipDomainService domainService, NotificationsService notificationsService) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.domainService = domainService;
        this.notificationsService = notificationsService;
=======
    //private final IUserRepository userRepository;
    //private final ICompanyRepository companyRepository;
=======
    private final IUserRepository userRepository;
    private final ICompanyRepository companyRepository;
>>>>>>> 1d842e6 (Add uc 4.15 View roles and permissions tree)
    private final IMembershipRepository membershipRepository;
=======
>>>>>>> 8105adc (Deleting Membership Repository and updating Member to save his list of roles in each company)
    private final MembershipDomainService domainService;
    private final INotificationService notificationService;

    public MembershipService(ITokenService tokenService, IUserRepository userRepository, ICompanyRepository companyRepository, MembershipDomainService domainService, INotificationService notificationService) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.domainService = domainService;
<<<<<<< HEAD
>>>>>>> 44d970c (Refactor UC 4.7 to use RoleStatus and a unified MembershipRepository)
=======
        this.notificationService = notificationService;
>>>>>>> 8105adc (Deleting Membership Repository and updating Member to save his list of roles in each company)
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
<<<<<<< HEAD
<<<<<<< HEAD
        
        // 2. Extract user ID from token and retrieve member information
        Long memberId = tokenService.extractUserId(sessionToken);
=======
=======
        
<<<<<<< HEAD
        // 2. Extract the requesting member ID
>>>>>>> 4368f6f (Add comments)
=======
>>>>>>> 8105adc (Deleting Membership Repository and updating Member to save his list of roles in each company)
        // TODO: delete casting to Long after memberId is changed to long in tokenService.extractSubject
        Long memberId = Long.parseLong(tokenService.extractSubject(sessionToken));
        Member member = userRepository.getMemberById(memberId);
        CompanyRole memberRole = member.getRoleInCompany(companyId);
        return domainService.validatePermission(memberRole, requiredPermission);
    }

<<<<<<< HEAD
    public void approveManagerAssignment(String sessionToken, long companyId) throws Exception {
=======
    /**
     * Use Case 4.7: Request to assign a manager to a company (Draft Entity Pattern)
     */
    public void requestManagerAssignment(String sessionToken, Long companyId, Long targetMemberId, Set<Permission> permissions) throws Exception {
        
        // 1. Authenticate session
>>>>>>> 4368f6f (Add comments)
        if (!tokenService.validateToken(sessionToken)) {
            throw new Exception("Session authentication failed.");
        }
        
        // TODO: delete casting to Long after memberId is changed to long in tokenService.extractSubject
        Long appointerId = Long.parseLong(tokenService.extractSubject(sessionToken));
        Member appointer = userRepository.getMemberById(appointerId);
<<<<<<< HEAD
        Company company = companyRepository.findById(companyId);
        
        company.registerNewAppointment(appointer.getUserName(), appointee.getUserName());
    }
<<<<<<< HEAD

    public void rejectManagerAssignment(String sessionToken, long companyId) throws Exception {
=======
    
    /**
     * Use Case 4.7: Reject a pending manager assignment
     */
    public void rejectManagerAssignment(String sessionToken, Long companyId) throws Exception {
<<<<<<< HEAD
>>>>>>> 44d970c (Refactor UC 4.7 to use RoleStatus and a unified MembershipRepository)
=======
        
        // 1. Authenticate session
>>>>>>> 4368f6f (Add comments)
        if (!tokenService.validateToken(sessionToken)) {
            throw new Exception("Session authentication failed.");
        }
        
        // 2. Extract the requesting member ID (The Appointee rejecting the offer)
        // TODO: delete casting to Long after memberId is changed to long in tokenService.extractSubject
        Long appointeeId = Long.parseLong(tokenService.extractSubject(sessionToken));
        
        // 3. Fetch the pending role from the repository
        CompanyRole pendingRole = membershipRepository.findRole(companyId, appointeeId);
        
        // 4. Domain Validation
        domainService.validateRejectManager(pendingRole);
        
        // 5. Retrieve appointer details before deletion for notification purposes
        // Optional: Get appointer ID before we delete the role, so we can notify them.
        Long appointerId = ((Manager) pendingRole).getAppointedByMemberId();
        
        // 6. Execute Deletion! (Clean up the draft entity)
        // Since they were never added to the appointer's tree, simply deleting the role is safe.
        membershipRepository.deleteRole(companyId, appointeeId);
=======
        CompanyRole appointerRole = appointer.getRoleInCompany(companyId);
        Member targetMember = userRepository.getMemberById(targetMemberId);
        CompanyRole targetRole = targetMember.getRoleInCompany(companyId);
        domainService.validateManagerAssignmentRequest(appointerRole, targetRole);           
        targetMember.addManagerRole(companyId, appointerId, permissions);
        userRepository.updateMember(targetMember);
        notificationService.notify(targetMemberId, "You have been assigned to become a manager at " + companyRepository.findById(companyId).getName() + ". Please review and approve or reject this assignment.");
>>>>>>> 8105adc (Deleting Membership Repository and updating Member to save his list of roles in each company)
    }

<<<<<<< HEAD
<<<<<<< HEAD
    public void giveUpOwnership(String sessionToken, Long companyId) throws Exception {
        if (!tokenService.validateToken(sessionToken)) {
            throw new Exception("Session authentication failed.");
        }
        // TODO: delete casting to Long after memberId is changed to long in tokenService.extractSubject
        Long ownerId = Long.parseLong(tokenService.extractSubject(sessionToken));
        Member resigningOwner = userRepository.getMemberById(ownerId);
        Company company = companyRepository.findById(companyId);
        membership.resignOwnershipFromCompany(resigningOwner, company);
    }

    private 

    public boolean validatePermission(String sessionToken, long companyId, Permission requiredPermission) {
=======
    public boolean validatePermission(String sessionToken, Long companyId, Permission requiredPermission) throws Exception {
>>>>>>> 44d970c (Refactor UC 4.7 to use RoleStatus and a unified MembershipRepository)
        if (!tokenService.validateToken(sessionToken)) {
            throw new Exception("Session authentication failed.");
        }
        // TODO: delete casting to Long after memberId is changed to long in tokenService.extractSubject
<<<<<<< HEAD
        long memberId = Long.parseLong(tokenService.extractSubject(sessionToken));
>>>>>>> e7f5697 (starting to implement giveup ownership use case)
        Member member = userRepository.getMemberById(memberId);

        // 3. Retrieve the member's role in the specified company and validate the required permission
        CompanyRole memberRole = member.getRoleInCompany(companyId);
        return domainService.validatePermission(memberRole, requiredPermission);
    }

=======
>>>>>>> 4368f6f (Add comments)
    /**
<<<<<<< HEAD
<<<<<<< HEAD
     * Use Case 4.7: Request to assign a manager to a company
     */
    public String requestManagerAssignment(String sessionToken, Long companyId, Long targetMemberId, Set<Permission> permissions) throws Exception {
        
=======
=======
     * Approve a pending assignment (Manager or Owner)
     */
    public void approveAssignment(String sessionToken, Long companyId) throws Exception {
        
        // 1. Authenticate session
        if (!tokenService.validateToken(sessionToken)) {
            throw new Exception("Session authentication failed.");
        }
        
        // TODO: delete casting to Long after memberId is changed to long in tokenService.extractSubject
        Long appointeeId = Long.parseLong(tokenService.extractSubject(sessionToken));
        Member appointee = userRepository.getMemberById(appointeeId);
        CompanyRole approvedRole = appointee.getRoleInCompany(companyId);
        
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
        
        domainService.validateAndApproveAssignment(approvedRole, appointerRole, appointeeId);
        
        userRepository.updateRole(approvedRole);
        userRepository.updateRole(appointerRole);
        
        Company company = companyRepository.findById(companyId);
        company.registerNewAppointment(appointer, appointee);
    }

    /**
     * Reject a pending assignment (Manager or Owner)
     */
    public void rejectAssignment(String sessionToken, Long companyId) throws Exception {
        
        // 1. Authenticate session
        if (!tokenService.validateToken(sessionToken)) {
            throw new Exception("Session authentication failed.");
        }
        
        // TODO: delete casting to Long after memberId is changed to long in tokenService.extractSubject
        Long memberId = Long.parseLong(tokenService.extractSubject(sessionToken));
        Member member = userRepository.getMemberById(memberId);
        CompanyRole rejectedRole = member.getRoleInCompany(companyId);
        
        domainService.validateRejectAssignment(rejectedRole);
        
        Long appointerId = null;
        if (rejectedRole instanceof Manager) {
            appointerId = ((Manager) rejectedRole).getAppointedByMemberId();
        } else if (rejectedRole instanceof Owner) {
            appointerId = ((Owner) rejectedRole).getAppointedByMemberId();
        }
        
        Member appointer = userRepository.getMemberById(appointerId);
        CompanyRole appointerRole = appointer.getRoleInCompany(companyId);

        if (appointerRole instanceof Owner) {
            ((Owner) appointerRole).deleteAppointee(memberId);
            userRepository.updateRole(appointerRole);
        }
        else if (appointerRole instanceof Founder) {
            ((Founder) appointerRole).deleteAppointee(memberId);
            userRepository.updateRole(appointerRole);
        }
        else {
            throw new Exception("Appointer's role is not valid for this operation.");
        }
        
        member.deleteRoleInCompany(companyId);
    }

    /**
>>>>>>> e663313 (implementation of use-case 4.7)
     * Use Case 4.15: View roles and permissions tree
     */
    public String viewRolesAndPermissionsTree(String sessionToken, long companyId) throws Exception {
>>>>>>> 1d842e6 (Add uc 4.15 View roles and permissions tree)
        // 1. Authenticate session
        if (!tokenService.validateToken(sessionToken)) {
            throw new Exception("Session authentication failed.");
        }
        
<<<<<<< HEAD
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
=======
        Long memberId = Long.parseLong(tokenService.extractSubject(sessionToken));
        CompanyRole memberRole = membershipRepository.findRole(companyId, memberId);
        return domainService.validatePermission(memberRole, requiredPermission);
>>>>>>> 44d970c (Refactor UC 4.7 to use RoleStatus and a unified MembershipRepository)
=======
        // 2. Extract the requesting member
        // TODO: delete casting to Long after memberId is changed to long in tokenService.extractSubject
        long memberId = Long.parseLong(tokenService.extractSubject(sessionToken));
        Member requester = userRepository.getMemberById(memberId);
        
        // Note for teammate: Make sure the Member class has a getUserName() method
        String requestingUsername = requester.getUserName(); 

        // 3. Fetch the company
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new Exception("Error: Company not found."));

        // 4. Build the permissions map (Username -> Permissions String)
        Map<String, String> permissionsMap = new HashMap<>();

        // Add all owners to the map (Owners have full permissions implicitly)
        for (String ownerUsername : company.getOwners()) {
            permissionsMap.put(ownerUsername, "Role: OWNER");
        }

        // Add all managers and their specific permissions to the map
        for (String managerUsername : company.getManagers()) {
            try {
                // Note for teammate: Make sure IUserRepository has getMemberByUsername(String username)
                Member managerMember = userRepository.getMemberByUsername(managerUsername);
                
                if (managerMember != null) {
                    CompanyRole role = managerMember.getRole(companyId);
                    
                    if (role instanceof Manager) {
                        Set<String> perms = ((Manager) role).getPermissionKeys();
                        String permString = perms.isEmpty() ? "No specific permissions" : "Permissions: " + String.join(", ", perms);
                        permissionsMap.put(managerUsername, "Role: MANAGER, " + permString);
                    }
                }
            } catch (Exception e) {
                // Ignore if user is not found to prevent the whole tree from failing
            }
        }

        // 5. Request the tree representation from the Company domain object
        return company.getRolesTreeRepresentation(requestingUsername, permissionsMap);
>>>>>>> 1d842e6 (Add uc 4.15 View roles and permissions tree)
    }

}