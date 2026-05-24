package ticketsystem.ApplicationLayer;

import java.time.LocalDateTime;

import org.hibernate.validator.constraints.CompositionType;

import ticketsystem.DTO.CompanyDTO;
import ticketsystem.DTO.DiscountRequestDTO;
import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.company.Company;
import ticketsystem.DomainLayer.company.DiscountPolicy;
import ticketsystem.DomainLayer.company.DiscountPolicy.DiscountCompositionType;
import ticketsystem.DomainLayer.company.PurchasePolicy;
import ticketsystem.DomainLayer.MembershipDomainService;

public class CompanyService {

    private final ICompanyRepository companyRepository;
    private final ITokenService tokenService;
    private final ISystemLogger logger;
    private final MembershipDomainService membershipDomain;
    /**
     * Constructor without logger. Kept for backward compatibility with existing
     * tests and code.
     */
    public CompanyService(ICompanyRepository repo,
                        ITokenService tokenService,
                        MembershipDomainService membershipDomain) {
        this(repo, tokenService, membershipDomain, null);
    }

    public CompanyService(ICompanyRepository repo,
                        ITokenService tokenService,
                        MembershipDomainService membershipDomain,
                        ISystemLogger logger) {
        this.companyRepository = repo;
        this.tokenService = tokenService;
        this.membershipDomain = membershipDomain;
        this.logger = logger;
    }

    /**
     * Extracts the logged-in member id from the session token. Guests are
     * rejected because company-management actions require a registered member.
     *
     * Important: this method does not log the token itself, because session
     * tokens are sensitive.
     *
     * @param token active session token
     * @return logged-in member id
     * @throws Exception if the token is invalid, belongs to a guest, or does
     * not contain a member id
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
     * Use Case 3.2: Create a production company. Allows a logged-in member to
     * create a new production company. The creating member becomes the founder
     * of the company.
     *
     * @param sessionId active session token of the logged-in member
     * @param companyName requested production company name
     * @return DTO of the created company
     * @throws Exception if the session is invalid, belongs to a guest, or
     * company creation fails
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
                new DiscountPolicy(DiscountCompositionType.MAX)//defult
        );

        membershipDomain.assignFounderRole(memberId, newCompany.getId());

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
     * Use Case 4.13: Close or suspend production company. Allows only the
     * founder of the company to close or suspend it.
     *
     * @param sessionId active session token of the requesting member
     * @param companyId id of the production company to close
     * @return DTO of the closed company
     * @throws Exception if the company does not exist, the requester is not
     * founder, or the company is already inactive
     */
    public CompanyDTO closeProductionCompany(String sessionId, long companyId) throws Exception {
        logEvent("UC 4.13 started: close production company, companyId=" + companyId,
                ISystemLogger.LogLevel.INFO);

        try {
            long memberId = getRegisteredMemberId(sessionId);

            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new Exception("Error: Company not found."));

            membershipDomain.validateFounder(memberId, companyId);
            company.closeOrSuspend();
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
     * Use Case 4.14: Reopen production company. Allows only the founder to
     * reopen a company that was previously closed.
     *
     * @param sessionId active session token of the requesting member
     * @param companyId id of the production company to reopen
     * @return DTO of the reopened company
     * @throws Exception if the company does not exist, the requester is not
     * founder, or the company is already active
     */
    public CompanyDTO reopenProductionCompany(String sessionId, long companyId) throws Exception {
        logEvent("UC 4.14 started: reopen production company, companyId=" + companyId,
                ISystemLogger.LogLevel.INFO);

        try {
            long memberId = getRegisteredMemberId(sessionId);

            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new Exception("Error: Company not found."));

            membershipDomain.validateFounder(memberId, companyId);
            company.reopenCompany();
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
     * Checks whether the current session is allowed to view the given company
     * details. Active companies can be viewed by any valid active session.
     * Inactive companies can be viewed only by company role holders through
     * CompanyService.
     *
     * @param sessionToken active session token
     * @param company company to view
     * @return true if the requester can view the company details, false
     * otherwise
     */
    private boolean canViewCompanyDetails(String sessionToken, Company company) {
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

        return membershipDomain.hasActiveRoleInCompany(memberId, company.getId());
    }

    /**
     * Returns company details according to viewing permissions. Active
     * companies can be viewed by any valid active session. Inactive companies
     * can be viewed only by company role holders.
     *
     * Important: this method does not log the session token, because session
     * tokens are sensitive.
     *
     * @param sessionToken active session token
     * @param companyId company id
     * @return DTO of the requested company
     * @throws Exception if the session is invalid, the company does not exist,
     * or the requester does not have permission to view it
     */
    public CompanyDTO getCompanyDetails(String sessionToken, long companyId) throws Exception {
        logEvent("Get company details started, companyId=" + companyId,
                ISystemLogger.LogLevel.INFO);

        try {
            if (!tokenService.validateToken(sessionToken)) {
                throw new Exception("Error: Invalid or expired session token.");
            }

            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new Exception("Error: Company not found."));

            if (!canViewCompanyDetails(sessionToken, company)) {
                throw new Exception("Error: User does not have permission to view this company.");
            }

            logEvent("Get company details completed, companyId=" + companyId,
                    ISystemLogger.LogLevel.INFO);

            return new CompanyDTO(company);

        } catch (RuntimeException e) {
            logError("Get company details failed due to an unexpected system error, companyId="
                    + companyId, e);
            throw e;

        } catch (Exception e) {
            logEvent("Get company details rejected, companyId=" + companyId
                    + ", reason=" + e.getMessage(),
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

    public void addDiscountPolicyToCompany(
            String token,
            Long companyId,DiscountRequestDTO discountDTO ) throws Exception{
        try {

            tokenService.validateToken(token);

            Long userId = tokenService.extractUserId(token);

            Company company = companyRepository.findById(companyId)
                            .orElseThrow(() -> new Exception("Error: Company not found."));;


            if (company.getFounderId() != userId) {
                throw new IllegalArgumentException(
                        "User is not allowed to manage company discount policy");
            }
            company.addDiscountToCompany(discountDTO);
            companyRepository.save(company);

        } catch (Exception e) {

            logger.logEvent( "Failed to add visible discount to company",ISystemLogger.LogLevel.WARN);
            throw e;
        }
    }
}
