package ticketsystem.ApplicationLayer;
import java.util.Set;

import ticketsystem.ApplicationLayer.ISystemLogger.LogLevel;
import ticketsystem.DomainLayer.MembershipDomainService;
import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.DomainLayer.company.Company;
import ticketsystem.DomainLayer.user.CompanyRole;
import ticketsystem.DomainLayer.user.Member;
import ticketsystem.DomainLayer.user.Permission;
import ticketsystem.ApplicationLayer.INotifier;

public class MembershipService {

    private final ITokenService tokenService;
    private final IUserRepository userRepository;
    private final ICompanyRepository companyRepository;
    private final MembershipDomainService membershipDomain;
    private final INotifier notificationsService;
    private final ISystemLogger logger;

    public MembershipService(ITokenService tokenService, IUserRepository userRepository, ICompanyRepository companyRepository, MembershipDomainService membershipDomain, INotifier notificationsService, ISystemLogger logger) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.membershipDomain = membershipDomain;
        this.notificationsService = notificationsService;
        this.logger = logger;
    }

    /**
     * Utility Method: Validate if a member has a specific permission within a company.
     * Used as a precondition check for various restricted Use Cases.
     */
    public boolean validatePermission(String sessionToken, Long companyId, Permission requiredPermission) throws Exception {
        
        String context = "companyId=" + companyId + ", requiredPermission=" + requiredPermission;
        logger.logEvent("started - validatePermission. " + context, LogLevel.INFO);

        try {
            // Authenticate session
            if (!tokenService.validateToken(sessionToken)) {
                throw new IllegalArgumentException("Session authentication failed.");
            }
            
            // Extract user ID from token
            Long memberId = tokenService.extractUserId(sessionToken);
            
            logger.logEvent("Loaded data - validatePermission. memberId=" + memberId, LogLevel.DEBUG);
            
            // Use the domain service to validate the permission based on the member's role
            boolean hasPermission = membershipDomain.validatePermission(memberId, companyId, requiredPermission);
            
            logger.logEvent(
                    "Completed - validatePermission. hasPermission=" + hasPermission, 
                    LogLevel.INFO);
            
            return hasPermission;
            
        } 
        catch (IllegalArgumentException e) {
            // Expected validation / use-case errors
            logger.logEvent("Invalid validatePermission criteria: " + e.getMessage(), LogLevel.WARN);
            throw e;
        } 
        catch (Exception e) {
            // Unexpected system errors
            logger.logError("Unexpected system error in validatePermission. " + context + ". reason=" + e.getMessage(), e);
            throw new RuntimeException(
                "An error occurred while validating permission: " + e.getMessage(), e);
        }
    }

/**
 * Use Case 4.7: Request to assign a manager to a company
 */
public boolean requestManagerAssignment(String sessionToken, Long companyId, Long targetMemberId, Set<Permission> permissions) throws Exception {

    String context = "companyId=" + companyId + ", targetMemberId=" + targetMemberId +
            ", permissionsCount=" + (permissions != null ? permissions.size() : 0);

    logger.logEvent("started - requestManagerAssignment. " + context, LogLevel.INFO);

    try {
        // Authenticate session
        if (!tokenService.validateToken(sessionToken)) {
            throw new IllegalArgumentException("Session authentication failed.");
        }

        // Extract user ID from token and retrieve member information
        Long appointerId = tokenService.extractUserId(sessionToken);
        Member appointer = userRepository.getMemberById(appointerId);
        if (appointer == null) {
            throw new IllegalArgumentException("Appointer not found.");
        }

        // Retrieve target member information
        Member targetMember = userRepository.getMemberById(targetMemberId);
        if (targetMember == null) {
            throw new IllegalArgumentException("Target Member not found.");
        }

        logger.logEvent(
                "Loaded data - requestManagerAssignment. appointerId=" + appointerId,
                LogLevel.DEBUG
        );

        // Call the domain service to handle the business logic of creating the manager role
        membershipDomain.managerAssignmentRequest(appointer, targetMember, companyId, permissions);

        // Update the repository with the changes to the target member
        userRepository.updateMember(targetMember);

        // Notify the target member about the pending assignment
        notificationsService.notifyMember(
                targetMemberId,
                "You received a request to become a manager of the production company \""
                        + getCompanyName(companyId) + "\"."
        );

        logger.logEvent(
                "Completed - requestManagerAssignment. pending role created for targetMemberId=" + targetMemberId,
                LogLevel.INFO
        );

        return true;

    } catch (IllegalArgumentException e) {
        // Expected validation / use-case errors
        logger.logEvent("Invalid requestManagerAssignment criteria: " + e.getMessage(), LogLevel.WARN);
        throw e;

    } catch (Exception e) {
        // Unexpected system errors
        logger.logError(
                "Unexpected system error in requestManagerAssignment. " + context + ". reason=" + e.getMessage(),
                e
        );
        throw new RuntimeException(
                "An error occurred while requesting manager assignment: " + e.getMessage(),
                e
        );
    }
}

    /**
     * Use Case 4.8: Request to assign an owner to a company
     */
    public boolean requestOwnerAssignment(String sessionToken, Long companyId, Long targetMemberId) throws Exception {

        String context = "companyId=" + companyId + ", targetMemberId=" + targetMemberId;
        logger.logEvent("started - requestOwnerAssignment. " + context, LogLevel.INFO);

        try {
            // Authenticate session
            if (!tokenService.validateToken(sessionToken)) {
                throw new IllegalArgumentException("Session authentication failed.");
            }
            
            // Extract user ID from token and retrieve member information
            Long appointerId = tokenService.extractUserId(sessionToken);
            Member appointer = userRepository.getMemberById(appointerId);
            if (appointer == null) {
                throw new IllegalArgumentException("Appointer not found.");
            }

            // Retrieve target member information
            Member targetMember = userRepository.getMemberById(targetMemberId);
            if (targetMember == null) {
                throw new IllegalArgumentException("Target Member not found.");
            }

            logger.logEvent(
                    "Loaded data - requestOwnerAssignment. appointerId=" + appointerId,
                    LogLevel.DEBUG
            );

            // Call the domain service to handle the business logic of creating the owner role
            if (!membershipDomain.ownerAssignmentRequest(appointer, targetMember, companyId)) {
                logger.logEvent(
                        "Failed - requestOwnerAssignment. Domain logic rejected the request. " + context,
                        LogLevel.WARN
                );
                return false;
            }

            // Update the repository with the changes to the target member ONLY if successful
            userRepository.updateMember(targetMember);

            // Notify the target member about the pending assignment
            if (notificationsService != null && targetMemberId != null) {
                notificationsService.notifyMember(
                        targetMemberId,
                        "You received a request to become an owner of the production company \""
                                + getCompanyName(companyId) + "\"."
                );
            }

            logger.logEvent(
                    "Completed - requestOwnerAssignment. pending owner role created for targetMemberId=" + targetMemberId,
                    LogLevel.INFO
            );

            // Return the actual result from the domain layer
            return true;

            } catch (IllegalArgumentException e) {
                // Expected validation / use-case errors
                logger.logEvent("Invalid requestOwnerAssignment criteria: " + e.getMessage(), LogLevel.WARN);
                throw e;

            } catch (Exception e) {
                // Unexpected system errors
                logger.logError(
                        "Unexpected system error in requestOwnerAssignment. " + context + ". reason=" + e.getMessage(),
                        e
                );
                throw new RuntimeException(
                        "An error occurred while requesting owner assignment: " + e.getMessage(),
                        e
                );
                }
            }
    /**
     * Use-case 4.9: Remove owner assignment
     */
    public boolean removeOwnerAssignment(String sessionToken, Long companyId, Long targetMemberId) throws Exception {
        
        String context = "companyId=" + companyId + ", targetMemberId=" + targetMemberId;
        logger.logEvent("started - removeOwnerAssignment. " + context, LogLevel.INFO);

        try {
            if (!tokenService.validateToken(sessionToken)) {
                throw new IllegalArgumentException("Session authentication failed.");
            }
            
            Long appointerId = tokenService.extractUserId(sessionToken);
            Member appointer = userRepository.getMemberById(appointerId);
            if (appointer == null) {
                throw new IllegalArgumentException("Appointer not found.");
            }

            Member targetMember = userRepository.getMemberById(targetMemberId);
            if (targetMember == null) {
                throw new IllegalArgumentException("Target Member not found.");
            }

            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new IllegalArgumentException("Company not found."));
            
            logger.logEvent(
                    "Loaded data - removeOwnerAssignment. appointerId=" + appointerId, 
                    LogLevel.DEBUG);

            // Calling domain logic
            membershipDomain.validateRemoveOwnerAssignment(appointer, targetMember, company);

            userRepository.updateMember(appointer);
            userRepository.updateMember(targetMember);
            companyRepository.save(company);
            
            logger.logEvent(
                    "Completed - removeOwnerAssignment. targetMemberId=" + targetMemberId + " removed as owner.", 
                    LogLevel.INFO);
            if (notificationsService != null && targetMemberId != null) {
                notificationsService.notifyMember(
                        targetMemberId,
                        "Your owner role in the production company \""
                                + company.getName() + "\" was removed."
                );
            }
            return true;
            
        } 
        catch (IllegalArgumentException e) {
            // Expected validation / use-case errors
            logger.logEvent("Invalid removeOwnerAssignment criteria: " + e.getMessage(), LogLevel.WARN);
            throw e;
        } 
        catch (Exception e) {
            // Unexpected system errors
            logger.logError("Unexpected system error in removeOwnerAssignment. " + context + ". reason=" + e.getMessage(), e);
            throw new RuntimeException(
                "An error occurred while removing owner assignment: " + e.getMessage(), e);
        }
    }

    /**
     * Use Case 4.10: Give Up ownership
     */
    public boolean giveUpOwnership(String sessionToken, Long companyId) throws Exception {
        
        String context = "companyId=" + companyId;
        logger.logEvent("started - giveUpOwnership. " + context, LogLevel.INFO);

        try {
            // Authenticate session
            if (!tokenService.validateToken(sessionToken)) {
                throw new IllegalArgumentException("Session authentication failed.");
            }
            
            // Extract user ID from token and retrieve member information
            Long memberId = tokenService.extractUserId(sessionToken);
            Member targetMember = userRepository.getMemberById(memberId);
            if (targetMember == null) {
                throw new IllegalArgumentException("Member not found.");
            }
            
            logger.logEvent("Loaded data - giveUpOwnership. resigning memberId=" + memberId, LogLevel.DEBUG);
            
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
                        throw new IllegalArgumentException("Appointer do not have a role in this company.");
                    }
                    
                    targetMember.deleteRoleInCompany(companyId);
                    userRepository.updateMember(targetMember);
                    userRepository.updateMember(appointer);
                    if (notificationsService != null && appointerId != null) {
                    notificationsService.notifyMember(
                            appointerId,
                            targetMember.getUserName()
                                    + " resigned from the owner role in the production company \""
                                    + getCompanyName(companyId) + "\"."
                    );
                }
                }
                else {
                    throw new IllegalArgumentException("Appointer not found.");
                }
            }

            logger.logEvent("Completed - giveUpOwnership. memberId=" + memberId + " successfully gave up ownership.", LogLevel.INFO);
            return true;

        } 
        catch (IllegalArgumentException e) {
            // Expected validation / use-case errors
            logger.logEvent("Invalid giveUpOwnership criteria: " + e.getMessage(), LogLevel.WARN);
            throw e;
        } 
        catch (Exception e) {
            // Unexpected system errors
            logger.logError("Unexpected system error in giveUpOwnership. " + context + ". reason=" + e.getMessage(), e);
            throw new RuntimeException(
                "An error occurred while giving up ownership: " + e.getMessage(), e);
        }
    }

    /**
     * Use Case 4.11: Update manager permissions
     */
    public boolean updateManagerPermissions(String sessionToken, Long companyId, Long managerId, Set<Permission> permissions) throws Exception {
        
        String context = "companyId=" + companyId + ", managerId=" + managerId + 
                         ", permissionsCount=" + (permissions != null ? permissions.size() : 0);
        logger.logEvent("started - updateManagerPermissions. " + context, LogLevel.INFO);

        try {
            // Authenticate session
            if (!tokenService.validateToken(sessionToken)) {
                throw new IllegalArgumentException("Session authentication failed.");
            }
            
            // Extract user ID from token and retrieve member information
            Long appointerId = tokenService.extractUserId(sessionToken);
            Member appointer = userRepository.getMemberById(appointerId);
            if (appointer == null) {
                throw new IllegalArgumentException("Appointer not found.");
            }

            // Retrieve target member information
            Member targetManager = userRepository.getMemberById(managerId);
            if (targetManager == null) {
                throw new IllegalArgumentException("Target Manager not found.");
            }

            logger.logEvent(
                    "Loaded data - updateManagerPermissions. appointerId=" + appointerId, 
                    LogLevel.DEBUG);

            // Call the domain service to handle the business logic of updating manager's permissions
            membershipDomain.setPermissionsToManager(appointer, targetManager, companyId, permissions);

            // Update the repository with the changes to the target member
            userRepository.updateMember(targetManager);
            
            if (notificationsService != null && managerId != null) {
                notificationsService.notifyMember(
                        managerId,
                        "Your management permissions in the production company \""
                                + getCompanyName(companyId) + "\" were updated."
                );
            }            
            logger.logEvent(
                    "Completed - updateManagerPermissions. permissions updated for managerId=" + managerId, 
                    LogLevel.INFO);
            
            // Return true if the request was successfully processed
            return true;

        } 
        catch (IllegalArgumentException e) {
            // Expected validation / use-case errors
            logger.logEvent("Invalid updateManagerPermissions criteria: " + e.getMessage(), LogLevel.WARN);
            throw e;
        } 
        catch (Exception e) {
            // Unexpected system errors
            logger.logError("Unexpected system error in updateManagerPermissions. " + context + ". reason=" + e.getMessage(), e);
            throw new RuntimeException(
                "An error occurred while updating manager permissions: " + e.getMessage(), e);
        }
    }

    /**
     * Use Case 4.12: Remove manager assignment
     */
    public boolean removeManagerAssignment(String sessionToken, Long companyId, Long targetMemberId) throws Exception {
        
        String context = "companyId=" + companyId + ", targetMemberId=" + targetMemberId;
        logger.logEvent("started - removeManagerAssignment. " + context, LogLevel.INFO);

        try {
            // 1. Authenticate session
            if (!tokenService.validateToken(sessionToken)) {
                throw new IllegalArgumentException("Session authentication failed.");
            }
            
            // 2. Retrieve appointer and target member information
            Long appointerId = tokenService.extractUserId(sessionToken);
            Member appointer = userRepository.getMemberById(appointerId);
            if (appointer == null) {
                throw new IllegalArgumentException("Appointer not found.");
            }

            Member targetMember = userRepository.getMemberById(targetMemberId);
            if (targetMember == null) {
                throw new IllegalArgumentException("Target Member not found.");
            }
            
            logger.logEvent("Loaded data - removeManagerAssignment. appointerId=" + appointerId, LogLevel.DEBUG);

            // 3. Execute domain logic (passing companyId directly)
            membershipDomain.validateRemoveManagerAssignment(appointer, targetMember, companyId);

            // 4. Persist changes in the repository
            userRepository.updateMember(appointer);
            userRepository.updateMember(targetMember);
            if (notificationsService != null && targetMemberId != null) {
            notificationsService.notifyMember(
                    targetMemberId,
                    "Your manager role in the production company \""
                            + getCompanyName(companyId) + "\" was removed."
            );
        }
            logger.logEvent("Completed - removeManagerAssignment. targetMemberId=" + targetMemberId + " removed as manager.", LogLevel.INFO);
            
            return true;

        } 
        catch (IllegalArgumentException e) {
            // Expected validation / use-case errors
            logger.logEvent("Invalid removeManagerAssignment criteria: " + e.getMessage(), LogLevel.WARN);
            throw e;
        } 
        catch (Exception e) {
            // Unexpected system errors
            logger.logError("Unexpected system error in removeManagerAssignment. " + context + ". reason=" + e.getMessage(), e);
            throw new RuntimeException(
                "An error occurred while removing manager assignment: " + e.getMessage(), e);
        }

}
    /**
/**
 * Approve a pending assignment (Manager or Owner)
 */
public boolean approveAssignment(String sessionToken, Long companyId) throws Exception {

    String context = "companyId=" + companyId;
    logger.logEvent("started - approveAssignment. " + context, LogLevel.INFO);

    try {
        // Authenticate session
        if (!tokenService.validateToken(sessionToken)) {
            throw new IllegalArgumentException("Session authentication failed.");
        }

        // Extract user ID from token and retrieve member information
        Long appointeeId = tokenService.extractUserId(sessionToken);
        Member appointee = userRepository.getMemberById(appointeeId);
        if (appointee == null) {
            throw new IllegalArgumentException("Appointee not found.");
        }

        Long appointerId = membershipDomain.getAppointerId(appointee, companyId);
        if (appointerId == null) {
            throw new IllegalArgumentException("The appointer ID could not be determined.");
        }

        Member appointer = userRepository.getMemberById(appointerId);
        if (appointer == null) {
            throw new IllegalArgumentException("Appointer not found.");
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found."));

        logger.logEvent(
                "Loaded data - approveAssignment. appointeeId=" + appointeeId + ", appointerId=" + appointerId,
                LogLevel.DEBUG
        );

        membershipDomain.approveAssignment(appointer, appointee, company);

        // Update the repository with the changes to both the appointee, appointer and company
        userRepository.updateMember(appointee);
        userRepository.updateMember(appointer);
        companyRepository.save(company);

        // Notify the appointer that the assignment was approved
        if (notificationsService != null && appointerId != null) {
            notificationsService.notifyMember(
                    appointerId,
                    appointee.getUserName() + " approved the assignment request for the production company \""
                            + company.getName() + "\"."
            );
        }

        logger.logEvent(
                "Completed - approveAssignment. Assignment approved for appointeeId=" + appointeeId,
                LogLevel.INFO
        );

        return true;

    } catch (IllegalArgumentException e) {
        // Expected validation / use-case errors
        logger.logEvent("Invalid approveAssignment criteria: " + e.getMessage(), LogLevel.WARN);
        throw e;

    } catch (Exception e) {
        // Unexpected system errors
        logger.logError(
                "Unexpected system error in approveAssignment. " + context + ". reason=" + e.getMessage(),
                e
        );
        throw new RuntimeException(
                "An error occurred while approving assignment: " + e.getMessage(),
                e
        );
    }
}

/**
 * Reject a pending assignment (Manager or Owner)
 */
public boolean rejectAssignment(String sessionToken, Long companyId) throws Exception {

    String context = "companyId=" + companyId;
    logger.logEvent("started - rejectAssignment. " + context, LogLevel.INFO);

    try {
        // Authenticate session
        if (!tokenService.validateToken(sessionToken)) {
            throw new IllegalArgumentException("Session authentication failed.");
        }

        // Extract user ID from token and retrieve member information
        Long memberId = tokenService.extractUserId(sessionToken);
        Member appointee = userRepository.getMemberById(memberId);
        if (appointee == null) {
            throw new IllegalArgumentException("Appointee not found.");
        }

        Long appointerId = membershipDomain.getAppointerId(appointee, companyId);
        if (appointerId == null) {
            throw new IllegalArgumentException("The appointer ID could not be determined.");
        }

        Member appointer = userRepository.getMemberById(appointerId);
        if (appointer == null) {
            throw new IllegalArgumentException("Appointer not found.");
        }

        logger.logEvent(
                "Loaded data - rejectAssignment. appointeeId=" + memberId + ", appointerId=" + appointerId,
                LogLevel.DEBUG
        );

        membershipDomain.rejectAssignment(appointer, appointee, companyId);

        // Update the repository with the changes to the appointee and appointer
        userRepository.updateMember(appointee);
        userRepository.updateMember(appointer);

        // Notify the appointer that the assignment was rejected
        if (notificationsService != null && appointerId != null) {
            notificationsService.notifyMember(
                    appointerId,
                    appointee.getUserName() + " rejected the assignment request for the production company \""
                            + getCompanyName(companyId) + "\"."
            );
        }

        logger.logEvent(
                "Completed - rejectAssignment. Assignment rejected for appointeeId=" + memberId,
                LogLevel.INFO
        );

        return true;

    } catch (IllegalArgumentException e) {
        // Expected validation / use-case errors
        logger.logEvent("Invalid rejectAssignment criteria: " + e.getMessage(), LogLevel.WARN);
        throw e;

    } catch (Exception e) {
        // Unexpected system errors
        logger.logError(
                "Unexpected system error in rejectAssignment. " + context + ". reason=" + e.getMessage(),
                e
        );
        throw new RuntimeException(
                "An error occurred while rejecting assignment: " + e.getMessage(),
                e
        );
    }
}

    /**
     * Use Case 4.15: View roles and permissions tree
     */
    public String viewRolesAndPermissionsTree(String sessionToken, long companyId) throws Exception {
        String context = "companyId=" + companyId;
        logger.logEvent("started - viewRolesAndPermissionsTree. " + context, LogLevel.INFO);

        try {
            if (!tokenService.validateToken(sessionToken)) {
                throw new IllegalArgumentException("Session authentication failed.");
            }

            Long memberId = tokenService.extractUserId(sessionToken);
            if (memberId == null) {
                throw new IllegalArgumentException("Member ID not found in token.");
            }

            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new IllegalArgumentException("Error: Company not found."));

            logger.logEvent(
                    "Loaded data - viewRolesAndPermissionsTree. requesterId=" + memberId + ", founderId=" + company.getFounderId(), 
                    LogLevel.DEBUG);

            String tree = membershipDomain.buildRolesAndPermissionsTree(
                    memberId,
                    companyId,
                    company.getFounderId()
            );

            logger.logEvent(
                    "Completed - viewRolesAndPermissionsTree. Tree generated for requesterId=" + memberId, 
                    LogLevel.INFO);

            return tree;

        } 
        catch (IllegalArgumentException e) {
            // Expected validation / use-case errors
            logger.logEvent("Invalid viewRolesAndPermissionsTree criteria: " + e.getMessage(), LogLevel.WARN);
            throw e;
        } 
        catch (Exception e) {
            // Unexpected system errors
            logger.logError("Unexpected system error in viewRolesAndPermissionsTree. " + context + ". reason=" + e.getMessage(), e);
            throw new RuntimeException(
                "An error occurred while viewing roles and permissions tree: " + e.getMessage(), e);
        }
    }


    private String getCompanyName(Long companyId) {
        try {
            Company company = companyRepository.findById(companyId).orElse(null);
            return company != null ? company.getName() : "the company";
        } catch (Exception e) {
            return "the company";
        }
    }
}
