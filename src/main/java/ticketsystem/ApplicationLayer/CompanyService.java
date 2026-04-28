package ticketsystem.ApplicationLayer;

import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.company.*;

public class CompanyService {
    private final ICompanyRepository companyRepository;
    private final ITokenService tokenService;

    public CompanyService(ICompanyRepository repo, ITokenService tokenService) {
        this.companyRepository = repo;
        this.tokenService = tokenService;
    }

    private String getRegisteredUserId(String token) throws Exception {
        if (!tokenService.validateToken(token)) {
            throw new Exception("Error: Invalid or expired session token.");
        }
        
        String subject = tokenService.extractSubject(token);
        
        if (subject != null && subject.startsWith("GUEST_")) {
            throw new Exception("Error: Member must be logged in. Guests are not allowed.");
        }
        
        return subject;
    }

    /**
     * Use Case 3.2: Create a production company.
     * כעת מחזיר long (את ה-ID המספרי של החברה שנוצרה)
     */
    public long createProductionCompany(String sessionId, String companyName) throws Exception {
        try {
            String userId = getRegisteredUserId(sessionId);

            if (companyRepository.existsByName(companyName)) {
                throw new Exception("Error: A company with this name already exists.");
            }

            Company newCompany = new Company(
                companyName, 
                userId, 
                new PurchasePolicy(), 
                new DiscountPolicy()
            );

            companyRepository.save(newCompany); 

            return newCompany.getId();

        } catch (Exception e) {
            throw e; 
        }
    }

    /**
     * Use Case 4.13: Close or suspend production company
     * שונה: מקבל long companyId
     */
    public void closeProductionCompany(String sessionId, long companyId) throws Exception {
        try {
            String userId = getRegisteredUserId(sessionId);

            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new Exception("Error: Company not found."));

            company.closeOrSuspend(userId);
            companyRepository.save(company);

        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Use Case 4.14: Reopen production company
     * שונה: מקבל long companyId
     */
    public void reopenProductionCompany(String sessionId, long companyId) throws Exception {
        try {
            String userId = getRegisteredUserId(sessionId);

            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new Exception("Error: Company not found."));

            company.reopenCompany(userId);
            companyRepository.save(company);

        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Use Case 4.15: View roles and permissions tree
     * שונה: מקבל long companyId
     */
    public String viewRolesAndPermissionsTree(String sessionId, long companyId) throws Exception {
        try {
            String userId = getRegisteredUserId(sessionId);

            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new Exception("Error: Company not found."));

            return company.getRolesTreeRepresentation(userId);

        } catch (Exception e) {
            throw e;
        }
    }
}