package ticketsystem.ApplicationLayer;

import java.util.List;
import java.util.Optional;

import ticketsystem.DTO.CompanyDTO;
import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.company.Company;
import ticketsystem.DomainLayer.company.DiscountPolicy;
import ticketsystem.DomainLayer.company.PurchasePolicy;

public class CompanyService {

    private final ICompanyRepository companyRepository;
    private final ITokenService tokenService;
    private final ISystemLogger logger;

    /**
     * Constructor without logger.
     * Kept for backward compatibility with existing tests and code.
     */
    public CompanyService(ICompanyRepository repo, ITokenService tokenService) {
        this(repo, tokenService, null);
    }

    /**
     * Constructor with logger injection.
     *
     * @param repo company repository
     * @param tokenService token service used for session validation
     * @param logger system logger for event and error logs
     */
    public CompanyService(ICompanyRepository repo, ITokenService tokenService, ISystemLogger logger) {
        this.companyRepository = repo;
        this.tokenService = tokenService;
        this.logger = logger;
    }

    /**
     * Extracts the logged-in member id from the session token.
     * Guests are rejected because company-management actions require a registered member.
     *
     * Important: this method does not log the token itself, because session tokens are sensitive.
     *
     * @param token active session token
     * @return logged-in member id
     * @throws Exception if the token is invalid, belongs to a guest, or does not contain a member id
     */
    private long getRegisteredMemberId(String token) throws Exception {
        try {
            if (!tokenService.validateToken(token)) {
                throw new Exception("Error: Invalid or expired session token.");
            }

            if (tokenService.isGuestToken(token)) {
                throw new Exception("Error: Member must be logged in. Guests are not allowed.");
            }

            Long userId = tokenService.extractUserId(token);

            if (userId == null) {
                throw new Exception("Error: Member ID not found in token.");
            }

            return userId;

        } catch (IllegalArgumentException e) {
            throw new Exception("Error: Invalid or expired session token.", e);
        }
    }

    /**
     * Use Case 3.2: Create a production company.
     * Allows a logged-in member to create a new production company.
     * The creating member becomes the founder of the company.
     *
     * @param sessionId active session token of the logged-in member
     * @param companyName requested production company name
     * @return DTO of the created company
     * @throws Exception if the session is invalid, belongs to a guest, or company creation fails
     */
    public CompanyDTO createProductionCompany(String sessionId, String companyName) throws Exception {
        logEvent("UC 3.2 started: create production company, companyName=" + companyName,
                ISystemLogger.LogLevel.INFO);

        try {
            long memberId = getRegisteredMemberId(sessionId);

            logEvent("UC 3.2 validated member, memberId=" + memberId,
                    ISystemLogger.LogLevel.DEBUG);

            Company newCompany = new Company(
                    companyName,
                    memberId,
                    new PurchasePolicy(),
                    new DiscountPolicy()
            );

            companyRepository.save(newCompany);

            logEvent("UC 3.2 completed: company created, companyId=" + newCompany.getId()
                            + ", founderId=" + memberId,
                    ISystemLogger.LogLevel.INFO);

            return new CompanyDTO(newCompany);

        } catch (RuntimeException e) {
            logError("UC 3.2 failed due to an unexpected system error while creating companyName="
                    + companyName, e);
            throw e;

        } catch (Exception e) {
            logEvent("UC 3.2 rejected: create production company failed, companyName=" + companyName
                            + ", reason=" + e.getMessage(),
                    ISystemLogger.LogLevel.WARN);
            throw e;
        }
    }

    /**
     * Use Case 4.13: Close or suspend production company.
     * Allows only the founder of the company to close or suspend it.
     *
     * @param sessionId active session token of the requesting member
     * @param companyId id of the production company to close
     * @return DTO of the closed company
     * @throws Exception if the company does not exist, the requester is not founder,
     *                   or the company is already inactive
     */
    public CompanyDTO closeProductionCompany(String sessionId, long companyId) throws Exception {
        logEvent("UC 4.13 started: close production company, companyId=" + companyId,
                ISystemLogger.LogLevel.INFO);

        try {
            long memberId = getRegisteredMemberId(sessionId);

            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new Exception("Error: Company not found."));

            company.closeOrSuspend(memberId);
            companyRepository.save(company);

            logEvent("UC 4.13 completed: company closed, companyId=" + companyId
                            + ", founderId=" + memberId,
                    ISystemLogger.LogLevel.INFO);

            return new CompanyDTO(company);

        } catch (RuntimeException e) {
            logError("UC 4.13 failed due to an unexpected system error while closing companyId="
                    + companyId, e);
            throw e;

        } catch (Exception e) {
            logEvent("UC 4.13 rejected: close production company failed, companyId=" + companyId
                            + ", reason=" + e.getMessage(),
                    ISystemLogger.LogLevel.WARN);
            throw e;
        }
    }

    /**
     * Use Case 4.14: Reopen production company.
     * Allows only the founder to reopen a company that was previously closed.
     *
     * @param sessionId active session token of the requesting member
     * @param companyId id of the production company to reopen
     * @return DTO of the reopened company
     * @throws Exception if the company does not exist, the requester is not founder,
     *                   or the company is already active
     */
    public CompanyDTO reopenProductionCompany(String sessionId, long companyId) throws Exception {
        logEvent("UC 4.14 started: reopen production company, companyId=" + companyId,
                ISystemLogger.LogLevel.INFO);

        try {
            long memberId = getRegisteredMemberId(sessionId);

            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new Exception("Error: Company not found."));

            company.reopenCompany(memberId);
            companyRepository.save(company);

            logEvent("UC 4.14 completed: company reopened, companyId=" + companyId
                            + ", founderId=" + memberId,
                    ISystemLogger.LogLevel.INFO);

            return new CompanyDTO(company);

        } catch (RuntimeException e) {
            logError("UC 4.14 failed due to an unexpected system error while reopening companyId="
                    + companyId, e);
            throw e;

        } catch (Exception e) {
            logEvent("UC 4.14 rejected: reopen production company failed, companyId=" + companyId
                            + ", reason=" + e.getMessage(),
                    ISystemLogger.LogLevel.WARN);
            throw e;
        }
    }

    /**
     * Use Case 4.15: View roles and permissions tree.
     * Allows a company owner to view the roles and permissions tree of the company.
     *
     * @param sessionId active session token of the requesting member
     * @param companyId id of the production company
     * @return textual representation of the roles and permissions tree
     * @throws Exception if the company does not exist or the requester is not an owner
     */
    public String viewRolesAndPermissionsTree(String sessionId, long companyId) throws Exception {
        logEvent("UC 4.15 started: view roles and permissions tree, companyId=" + companyId,
                ISystemLogger.LogLevel.INFO);

        try {
            long memberId = getRegisteredMemberId(sessionId);

            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new Exception("Error: Company not found."));

            String tree = company.getRolesTreeRepresentation(memberId, null);

            logEvent("UC 4.15 completed: roles and permissions tree returned, companyId=" + companyId
                            + ", requesterId=" + memberId,
                    ISystemLogger.LogLevel.INFO);

            return tree;

        } catch (RuntimeException e) {
            logError("UC 4.15 failed due to an unexpected system error while viewing roles tree, companyId="
                    + companyId, e);
            throw e;

        } catch (Exception e) {
            logEvent("UC 4.15 rejected: view roles and permissions tree failed, companyId=" + companyId
                            + ", reason=" + e.getMessage(),
                    ISystemLogger.LogLevel.WARN);
            throw e;
        }
    }

    /**
     * Removes a member from all company roles.
     * Used as part of the system-admin member deletion flow.
     *
     * A founder cannot be removed through this flow, because founder removal affects
     * company ownership and should be handled by a dedicated company use case.
     *
     * @param memberIdToDelete id of the member being removed from company roles
     * @throws Exception if the member is a founder or role cleanup fails
     */
    public void removeUserFromAllCompanies(long memberIdToDelete) throws Exception {
        logEvent("Company role cleanup started for memberId=" + memberIdToDelete,
                ISystemLogger.LogLevel.INFO);

        try {
            boolean isFounderAnywhere = companyRepository.existsByFounderId(memberIdToDelete);

            if (isFounderAnywhere) {
                throw new Exception("Cannot delete user: The user is a Founder of one or more companies.");
            }

            List<Company> relevantCompanies =
                    companyRepository.findByOwnersContainingOrManagersContaining(memberIdToDelete, memberIdToDelete);

            logEvent("Company role cleanup found " + relevantCompanies.size()
                            + " relevant companies for memberId=" + memberIdToDelete,
                    ISystemLogger.LogLevel.DEBUG);

            for (Company company : relevantCompanies) {
                company.removeUserFromAllRoles(memberIdToDelete);
                companyRepository.save(company);

                logEvent("Member removed from company roles, memberId=" + memberIdToDelete
                                + ", companyId=" + company.getId(),
                        ISystemLogger.LogLevel.INFO);
            }

            logEvent("Company role cleanup completed for memberId=" + memberIdToDelete,
                    ISystemLogger.LogLevel.INFO);

        } catch (RuntimeException e) {
            logError("Company role cleanup failed due to an unexpected system error, memberId="
                    + memberIdToDelete, e);
            throw e;

        } catch (Exception e) {
            logEvent("Company role cleanup rejected, memberId=" + memberIdToDelete
                            + ", reason=" + e.getMessage(),
                    ISystemLogger.LogLevel.WARN);
            throw new Exception("Failed to remove user from companies: " + e.getMessage(), e);
        }
    }
<<<<<<< HEAD


private boolean canViewCompanyDetails(String sessionToken, Company company) throws Exception {
    if (company.isActive()) {
        return true;
    }

    if (tokenService.isGuestToken(sessionToken)) {
        return false;
    }

    Long memberId = tokenService.extractUserId(sessionToken);
    if (memberId == null) {
        return false;
    }

    return company.getFounderId() == memberId
            || company.getOwners().contains(memberId)
            || company.getManagers().contains(memberId);
}
    public CompanyDTO getCompanyDetails(String sessionToken, long companyId) throws Exception {
    if (!tokenService.validateToken(sessionToken)) {
        throw new Exception("Error: Invalid or expired session token.");
    }

    Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new Exception("Error: Company not found."));

    if (!canViewCompanyDetails(sessionToken, company)) {
        throw new Exception("Error: User does not have permission to view this company.");
    }
        return new CompanyDTO(company);
}


=======

    /**
     * Use Case 6.1: Close production company by system admin.
     * This method is called by SystemAdminService after the requester was validated as a system admin.
     * The company is marked inactive and its company appointments are cancelled by the domain object.
     *
     * @param companyId id of the company to close
     * @param adminId id of the validated system admin
     * @return DTO of the closed company
     * @throws Exception if the company does not exist or cannot be closed
     */
    public CompanyDTO closeProductionCompanyBySystemAdmin(long companyId, long adminId) throws Exception {
        logEvent("UC 6.1 company-side started: close production company by system admin, companyId="
                        + companyId + ", adminId=" + adminId,
                ISystemLogger.LogLevel.INFO);

        try {
            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new Exception("Company not found."));

            company.closeBySystemAdmin(adminId);
            companyRepository.save(company);

            logEvent("UC 6.1 company-side completed: company closed by system admin, companyId="
                            + companyId + ", adminId=" + adminId,
                    ISystemLogger.LogLevel.INFO);

            return new CompanyDTO(company);

        } catch (RuntimeException e) {
            logError("UC 6.1 company-side failed due to an unexpected system error, companyId="
                    + companyId + ", adminId=" + adminId, e);
            throw e;

        } catch (Exception e) {
            logEvent("UC 6.1 company-side rejected: close by system admin failed, companyId="
                            + companyId + ", adminId=" + adminId + ", reason=" + e.getMessage(),
                    ISystemLogger.LogLevel.WARN);
            throw e;
        }
    }

    /**
     * Writes an event log message if a logger was injected.
     *
     * @param message log message
     * @param level event log level
     */
    private void logEvent(String message, ISystemLogger.LogLevel level) {
        if (logger != null) {
            logger.logEvent(message, level);
        }
    }

    /**
     * Writes an error log message if a logger was injected.
     *
     * @param message error message
     * @param exception exception that caused the system error
     */
    private void logError(String message, Throwable exception) {
        if (logger != null) {
            logger.logError(message, exception);
        }
    }
>>>>>>> 5f5bb60 (Add logging and documentation to company service)
}