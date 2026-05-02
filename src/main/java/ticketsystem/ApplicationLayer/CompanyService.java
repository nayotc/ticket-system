package ticketsystem.ApplicationLayer;

import java.util.List;

import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.company.*;

public class CompanyService {
    private final ICompanyRepository companyRepository;
    private final ITokenService tokenService;

    public CompanyService(ICompanyRepository repo, ITokenService tokenService) {
        this.companyRepository = repo;
        this.tokenService = tokenService;
    }

    private String getRegisteredUserId(String token) throws Exception { // gets a token and returns user name
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
     */
    public void closeProductionCompany(String sessionId, long companyId) throws Exception {
        try {
            String userId = getRegisteredUserId(sessionId);

            Company company = companyRepository.findById(companyId) // gets the company from the CompanyRepository
                    .orElseThrow(() -> new Exception("Error: Company not found."));

            company.closeOrSuspend(userId);
            companyRepository.save(company);

        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Use Case 4.14: Reopen production company
     */
    public void reopenProductionCompany(String sessionId, long companyId) throws Exception {
        try {
            String userId = getRegisteredUserId(sessionId);

            Company company = companyRepository.findById(companyId) // gets the company from the CompanyRepository
                    .orElseThrow(() -> new Exception("Error: Company not found."));

            company.reopenCompany(userId);
            companyRepository.save(company);

        } catch (Exception e) {
            throw e;
        }
    }

    // /**
    //  * Use Case 4.15: View roles and permissions tree
    //  */
    // public String viewRolesAndPermissionsTree(String sessionId, long companyId) throws Exception {
    //     try {
    //         String userId = getRegisteredUserId(sessionId);

    //         Company company = companyRepository.findById(companyId) // gets the company from the CompanyRepository
    //                 .orElseThrow(() -> new Exception("Error: Company not found."));

    //         return company.getRolesTreeRepresentation(userId);

    //     } catch (Exception e) {
    //         throw e;
    //     }
    // }

public void removeUserFromAllCompanies(String userIdToDelete) throws Exception {
        try {
            // Phase 1: Validation - Efficiently check if the user is a Founder anywhere directly in the DB
            boolean isFounderAnywhere = companyRepository.existsByFounderUsername(userIdToDelete);
            
            if (isFounderAnywhere) {
                // If the user is a founder, throw an exception and completely stop the deletion process
                throw new Exception("Cannot delete user: The user is a Founder of one or more companies.");
            }

            // Phase 2: Fetch ONLY the companies where the user is an owner or a manager
            // We pass 'userIdToDelete' twice because we check both the 'owners' and 'managers' lists
            List<Company> relevantCompanies = companyRepository.findByOwnersContainingOrManagersContaining(userIdToDelete, userIdToDelete);

            // Phase 3: Remove from roles and save
            for (Company company : relevantCompanies) {
                // Remove the user and handle reassigning their appointees
                company.removeUserFromAllRoles(userIdToDelete);
                
                // We don't need a 'needsSaving' flag anymore, because we only fetched companies that actually need updating
                companyRepository.save(company);
            }
            
        } catch (Exception e) {
            // Propagate the exception so the calling layer can return an appropriate message
            throw new Exception("Failed to remove user from companies: " + e.getMessage());
        }
    }
}