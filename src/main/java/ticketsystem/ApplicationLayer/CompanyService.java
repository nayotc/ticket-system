package ticketsystem.ApplicationLayer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ticketsystem.DomainLayer.discount.DiscountPolicy;
import ticketsystem.DomainLayer.discount.DiscountTypes;
import ticketsystem.DomainLayer.discount.DiscountCompositionType;
import ticketsystem.DomainLayer.user.Permission;
import ticketsystem.DTO.CompanyDTO;
import ticketsystem.DTO.DiscountDTO;
import ticketsystem.DTO.DiscountPolicyDTO;
import ticketsystem.DTO.PurchasePolicyDTO;
import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.company.Company;
import ticketsystem.DomainLayer.policy.PurchasePolicy;
import ticketsystem.DomainLayer.MembershipDomainService;
import ticketsystem.DomainLayer.discount.ConditionalDiscount;
import ticketsystem.DomainLayer.discount.ConditionalDiscount.Condition;

@Service
public class CompanyService {

    private final ICompanyRepository companyRepository;
    private final ITokenService tokenService;
    private final ISystemLogger logger;
    private final MembershipDomainService membershipDomain;
    private final PurchasePolicyMapper mapper = new PurchasePolicyMapper();
    private final INotifier notificationsService;
    private final UserAccessService userAccessService; 
    
    /**
     * Constructor without logger. Kept for backward compatibility with existing
     * tests and code.
     */
    public CompanyService(ICompanyRepository repo,
                        ITokenService tokenService,
                        MembershipDomainService membershipDomain,UserAccessService userAccessService, INotifier notifier) {
        this(repo, tokenService, membershipDomain, null,userAccessService,notifier);
    }

    @Autowired
    public CompanyService(ICompanyRepository repo,
                        ITokenService tokenService,
                        MembershipDomainService membershipDomain,
                        ISystemLogger logger,UserAccessService userAccessService, INotifier notifier) {
        this.companyRepository = repo;
        this.tokenService = tokenService;
        this.membershipDomain = membershipDomain;
        this.logger = logger;
        this.notificationsService=notifier;
        this.userAccessService=userAccessService;
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
            userAccessService.validateCanPerformNonViewAction(memberId);
            logEvent("UC 3.2 validated member, memberId=" + memberId,
                    ISystemLogger.LogLevel.DEBUG);

        Company newCompany = new Company(
                companyName,
                memberId,
                PurchasePolicy.noRestrictions(),
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
            userAccessService.validateCanPerformNonViewAction(memberId);

            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new Exception("Error: Company not found."));

            membershipDomain.validateFounder(memberId, companyId);
            company.closeOrSuspend();
            companyRepository.save(company);
            notifyCompanyStaff(
            company,
            "The production company \"" + company.getName() + "\" has been closed and is no longer active."
            );
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
            userAccessService.validateCanPerformNonViewAction(memberId);
            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new Exception("Error: Company not found."));

            membershipDomain.validateFounder(memberId, companyId);
            company.reopenCompany();
            companyRepository.save(company);
            notifyCompanyStaff(
            company,
            "The production company \"" + company.getName() + "\" has been reopened and is now active."
            );
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
    //for header presenter
    public Long getFirstManagedCompanyId(String sessionToken) throws Exception {
        long memberId = getRegisteredMemberId(sessionToken);

        return membershipDomain.getFirstManagedCompanyId(memberId);
    }
    private void notifyCompanyStaff(Company company, String message) {
        if (notificationsService == null || company == null || message == null || message.isBlank()) {
            return;
        }

        Set<Long> recipients = membershipDomain.getManagementSubTreeMemberIds(
                company.getFounderId(),
                company.getId()
        );

        notificationsService.notifyMembers(recipients, message);
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
    //add discount

    // add visible discount
    public void addVisibleDiscountToCompany(String token, Long companyId,
            String name, BigDecimal percentage) throws Exception {

        try {
            Company company = canEditDiscount(token, companyId);

            company.addVisibleDiscountToCompany(name, percentage);

            companyRepository.save(company);
                logger.logEvent(
                "Visible discount added successfully to company id: " + companyId,
                ISystemLogger.LogLevel.INFO);

        } catch (Exception e) {

            logger.logEvent("Failed to add visible discount to company",
                    ISystemLogger.LogLevel.WARN);

            throw e;
        }
    }
    // add coupon discount
    public void addCouponDiscountToCompany(String token, Long companyId,
            String name, String couponCode,
            BigDecimal percentage,LocalDateTime endTime) throws Exception {

        try {
            Company company = canEditDiscount(token, companyId);

            company.addCouponDiscountToCompany(name, couponCode, percentage,endTime);

            companyRepository.save(company);
                    logger.logEvent(
                "Coupon discount added successfully to company id: " + companyId,
                ISystemLogger.LogLevel.INFO
        );

        } catch (Exception e) {

            logger.logEvent("Failed to add coupon discount to company",
                    ISystemLogger.LogLevel.WARN);

            throw e;
        }
    }// add conditional discount
    public void addConditionalDiscountToCompany(String token, Long companyId,
            String name, LocalDateTime startTime,
            LocalDateTime endTime, BigDecimal percentage,
            Condition condition,
            Integer ticketThreshold) throws Exception {

        try {
            Company company = canEditDiscount(token, companyId);

            company.addConditionalDiscountToCompany(
                    name,
                    startTime,
                    endTime,
                    percentage,
                    condition,
                    ticketThreshold
            );

            companyRepository.save(company);
                    logger.logEvent(
                "Conditional discount added successfully to company id: " + companyId,
                ISystemLogger.LogLevel.INFO
        );

        } catch (Exception e) {

            logger.logEvent("Failed to add conditional discount to company",
                    ISystemLogger.LogLevel.WARN);

            throw e;
        }
    }
    //remove discount
    public void removeDiscountFromCompany(String token,Long companyId,Long discountId ) throws Exception{
        try{
        Company company = canEditDiscount(token,companyId);
        company.removeDiscountFromCompany(discountId);
        companyRepository.save(company);
                logger.logEvent(
                "Discount removed successfully from company id: "
                        + companyId + ", discount id: " + discountId,
                ISystemLogger.LogLevel.INFO
        );
        } catch (Exception e){
              logger.logEvent( "Failed to remove discount, id:"+discountId ,ISystemLogger.LogLevel.WARN);
            throw e;
        }
    }

    //set composition type
    public void setCompositionType(String token,Long companyId,DiscountCompositionType compositionType)
    throws Exception{
        try{
        Company company = canEditDiscount(token,companyId);
        company.setDiscountCompositionType(compositionType);
        companyRepository.save(company);
                logger.logEvent(
                "Discount composition type updated successfully for company id: "
                        + companyId,
                ISystemLogger.LogLevel.INFO
        );
        }catch(Exception e){
            logger.logEvent( "Failed to set composition Type discount to company",ISystemLogger.LogLevel.WARN);
            throw e;
        }
    }

    private Company canEditDiscount(String token,Long companyId) throws Exception{
        tokenService.validateToken(token);

            Long memberId = tokenService.extractUserId(token);
            userAccessService.validateCanPerformNonViewAction(memberId);

            Company company = companyRepository.findById(companyId)
                            .orElseThrow(() -> new Exception("Error: Company not found."));;

        if (!membershipDomain.validatePermission(memberId,companyId,Permission.SET_DISCOUNT_POLICY)){
            throw new IllegalArgumentException(
                "User does not have permission to manage company discount policy");
        }
            return company;
    }

    public void setCompanyPurchasePolicy(String token, Long companyId, PurchasePolicyDTO policyDTO) throws Exception {
        try {
            Company company = canEditPurchasePolicy(token, companyId);

            PurchasePolicy policy = mapper.toDomain(policyDTO);

            company.setPurchasePolicy(policy);

            companyRepository.save(company);

        } catch (Exception e) {
            logger.logEvent(
                    "Failed to set purchase policy for company, id: " + companyId,
                    ISystemLogger.LogLevel.WARN
            );
            throw e;
        }
    }
    private Company canEditPurchasePolicy(String token,Long companyId) throws Exception{
        tokenService.validateToken(token);

            Long memberId = tokenService.extractUserId(token);
            userAccessService.validateCanPerformNonViewAction(memberId);
            Company company = companyRepository.findById(companyId)
                            .orElseThrow(() -> new Exception("Error: Company not found."));;

        if (!membershipDomain.validatePermission(memberId,companyId,Permission.SET_PURCHASING_POLICY)){
            throw new IllegalArgumentException(
                "User does not have permission to manage company purchasing policy");
        }
            return company;
    }

/**
     * Retrieves the purchase policy of a specified company and maps it to a DTO.
     * Validates the user's session and permissions before allowing access.
     *
     * @param token     The authentication token of the current user.
     * @param companyId The unique identifier of the target company.
     * @return PurchasePolicyDTO representing the current purchase policy of the company.
     * @throws Exception If the company is not found, the user lacks permission to view it,
     * or an internal retrieval error occurs.
     */
    public PurchasePolicyDTO getCompanyPurchasePolicy(String token, Long companyId) throws Exception {
        try {
            Company company = canViewCompanyDetails(token, companyRepository.findById(companyId)
                    .orElseThrow(() -> new Exception("Error: Company not found."))) ?
                    companyRepository.findById(companyId).get() : null;

            if (company == null) {
                throw new Exception("Error: User does not have permission to view this company's policies.");
            }

            return mapper.toDTO(company.getPurchasePolicy());

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new Exception(e.getMessage());
        } catch (Exception e) {
            throw new Exception("An error occurred while retrieving the purchase policy.");
        }
    }

/**
     * Retrieves the discount policy of a specified company and maps it to a DTO.
     * Validates the user's session and permissions before allowing access.
     * Automatically maps Domain discount types (Visible, Coupon, Conditional) back to DTOs,
     * including translating internal Enums into readable text for the UI.
     *
     * @param token     The authentication token of the current user.
     * @param companyId The unique identifier of the target company.
     * @return DiscountPolicyDTO representing the current discount policy of the company.
     * @throws Exception If the company is not found, the user lacks permission, or a retrieval error occurs.
     */
    public DiscountPolicyDTO getCompanyDiscountPolicy(String token, Long companyId) throws Exception {
        try {
            Company company = canViewCompanyDetails(token, companyRepository.findById(companyId)
                    .orElseThrow(() -> new Exception("Error: Company not found."))) ?
                    companyRepository.findById(companyId).get() : null;

            if (company == null) {
                throw new Exception("Error: User does not have permission to view this company's policies.");
            }

            DiscountPolicy domainPolicy = company.getDiscountPolicy();

            if (domainPolicy == null) {
                return new DiscountPolicyDTO();
            }

            DiscountPolicyDTO dto = new DiscountPolicyDTO();
            dto.setCompositionType(domainPolicy.getDiscountCompositionType());

            List<DiscountDTO> discountDTOs = new java.util.ArrayList<>();

            if (domainPolicy.getDiscounts() != null) {
                for (DiscountTypes discount : domainPolicy.getDiscounts()) {
                    DiscountDTO dDTO = new DiscountDTO();

                    dDTO.setName(discount.getName());
                    dDTO.setPercentage(discount.getPercentage());

                    // בדיקת סוג ההנחה הספציפי והמרתו
                    if (discount instanceof ticketsystem.DomainLayer.discount.CouponDiscount) {
                        ticketsystem.DomainLayer.discount.CouponDiscount coupon =
                                (ticketsystem.DomainLayer.discount.CouponDiscount) discount;

                        dDTO.setType("COUPON");
                        dDTO.setCouponCode(coupon.getCouponCode());
                        dDTO.setEndTime(coupon.getEndTime());

                    } else if (discount instanceof ConditionalDiscount) {
                        ConditionalDiscount cond = (ConditionalDiscount) discount;

                        dDTO.setType("CONDITIONAL");
                        dDTO.setEndTime(cond.getEndTime());

                        String threshold = cond.getTicketThreshold() != null ? cond.getTicketThreshold().toString() : "1";

                        if (cond.getCondition() == ticketsystem.DomainLayer.discount.ConditionalDiscount.Condition.MAX_TICKET) {
                            dDTO.setConditionText("מקסימום " + threshold + " כרטיסים");
                        } else if (cond.getCondition() == ticketsystem.DomainLayer.discount.ConditionalDiscount.Condition.DATE) {
                            dDTO.setConditionText("הנחת תאריך");
                        } else {
                            dDTO.setConditionText("מינימום " + threshold + " כרטיסים");
                        }

                    } else if (discount instanceof ticketsystem.DomainLayer.discount.VisibleDiscount) {
                        dDTO.setType("VISIBLE");
                    }

                    discountDTOs.add(dDTO);
                }
            }

            dto.setDiscounts(discountDTOs);
            return dto;

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new Exception(e.getMessage());
        } catch (Exception e) {
            throw new Exception("An error occurred while retrieving the discount policy.");
        }
    }

    /**
     * Sets or updates the discount policy for a specified company based on the provided DTO.
     * Validates user permissions to ensure only authorized personnel can modify policies.
     * Maps different discount types (Visible, Coupon, Conditional) from the DTO to Domain entities.
     * Note: For conditional discounts, it attempts to parse the condition type and threshold
     * dynamically from the free-text condition string provided by the UI.
     *
     * @param token     The authentication token of the current user.
     * @param companyId The unique identifier of the target company.
     * @param policyDTO The DiscountPolicyDTO containing the new discount rules and composition type.
     * @throws Exception If validation fails, the company is not found, or the database save operation fails.
     */
    public void setCompanyDiscountPolicy(String token, Long companyId, ticketsystem.DTO.DiscountPolicyDTO policyDTO) throws Exception {
        try {
            tokenService.validateToken(token);
            Long memberId = tokenService.extractUserId(token);
            userAccessService.validateCanPerformNonViewAction(memberId);

            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new Exception("Error: Company not found."));

            if (!membershipDomain.validatePermission(memberId, companyId, Permission.SET_DISCOUNT_POLICY)) {
                throw new IllegalArgumentException("User does not have permission to manage company discount policy");
            }

            if (policyDTO.getDiscounts() != null) {
                for (ticketsystem.DTO.DiscountDTO discountDTO : policyDTO.getDiscounts()) {

                    if ("VISIBLE".equals(discountDTO.getType()) || "SIMPLE".equals(discountDTO.getType())) {
                        company.addVisibleDiscountToCompany(
                                discountDTO.getName(),
                                discountDTO.getPercentage()
                        );
                    }
                    else if ("COUPON".equals(discountDTO.getType())) {
                        company.addCouponDiscountToCompany(
                                discountDTO.getName(),
                                discountDTO.getCouponCode(),
                                discountDTO.getPercentage(),
                                discountDTO.getEndTime()
                        );
                    }
                    else if ("CONDITIONAL".equals(discountDTO.getType())) {

                        ticketsystem.DomainLayer.discount.ConditionalDiscount.Condition actualCondition =
                                ticketsystem.DomainLayer.discount.ConditionalDiscount.Condition.MIN_TICKET;

                        int threshold = 1;
                        String conditionStr = discountDTO.getConditionText();

                        if (conditionStr != null && !conditionStr.isBlank()) {
                            try {
                                if (conditionStr.contains("<") || conditionStr.toLowerCase().contains("max") || conditionStr.contains("מקסימום")) {
                                    actualCondition = ticketsystem.DomainLayer.discount.ConditionalDiscount.Condition.MAX_TICKET;
                                } else if (conditionStr.contains("date") || conditionStr.contains("תאריך")) {
                                    actualCondition = ticketsystem.DomainLayer.discount.ConditionalDiscount.Condition.DATE;
                                }

                                String numberOnly = conditionStr.replaceAll("[^0-9]", "");
                                if (!numberOnly.isEmpty()) {
                                    threshold = Integer.parseInt(numberOnly);
                                }
                            } catch (Exception e) {
                                threshold = 1;
                            }
                        }

                        company.addConditionalDiscountToCompany(
                                discountDTO.getName(),
                                java.time.LocalDateTime.now(),
                                discountDTO.getEndTime(),
                                discountDTO.getPercentage(),
                                actualCondition,
                                threshold
                        );
                    }
                }
            }

            companyRepository.save(company);
            logger.logEvent("Discount policy updated successfully for company: " + companyId, ISystemLogger.LogLevel.INFO);

        } catch (Exception e) {
            logger.logEvent("Failed to set discount policy for company id: " + companyId, ISystemLogger.LogLevel.WARN);
            throw e;
        }
    }

    public boolean hasPermission(String sessionToken, long companyId, Permission permission) throws Exception {
        long memberId = getRegisteredMemberId(sessionToken);

        if (permission == null) {
            throw new IllegalArgumentException("Permission cannot be null");
        }

        return membershipDomain.validatePermission(memberId, companyId, permission);
    }

    public String getPurchasePolicySummary(Long companyId) {
        try {
            Company company = companyRepository.findById(companyId).orElse(null);

            if (company == null || company.getPurchasePolicy() == null) {
                return "לא הוגדרה מדיניות רכישה";
            }

            PurchasePolicy policy = company.getPurchasePolicy();

            if (policy.getRootRule() != null && policy.getRootRule().getClass().getSimpleName().equals("AlwaysAllowRule")) {
                return "ללא הגבלות רכישה מיוחדות";
            }

            return "מוגדרת חוקיות רכישה מותאמת אישית";

        } catch (Exception e) {
            logError("Failed to get purchase policy summary for companyId=" + companyId, e);
            return "שגיאה בשליפת מדיניות הרכישה";
        }
    }

    public String getDiscountPolicySummary(Long companyId) {
        try {
            Company company = companyRepository.findById(companyId).orElse(null);

            if (company == null || company.getDiscountPolicy() == null) {
                return "אין הנחות פעילות";
            }

            DiscountPolicy policy = company.getDiscountPolicy();

            if (policy.getDiscounts() == null || policy.getDiscounts().isEmpty()) {
                return "אין הנחות פעילות";
            }

            int discountsCount = policy.getDiscounts().size();

            String compositionMethod = "";
            if (policy.getDiscountCompositionType() != null) {
                switch (policy.getDiscountCompositionType().name()) {
                    case "SUM":
                        compositionMethod = " (כפל מבצעים)";
                        break;
                    case "MAX":
                        compositionMethod = " (הנחה מקסימלית)";
                        break;
                }
            }

            return discountsCount + " הנחות מוגדרות במערכת" + compositionMethod;

        } catch (Exception e) {
            logError("Failed to get discount policy summary for companyId=" + companyId, e);
            return "שגיאה בשליפת מדיניות ההנחות";
        }
    }

}
