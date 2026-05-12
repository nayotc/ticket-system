package ticketsystem.ApplicationLayer;

import java.util.List;
import java.util.Optional;

import ticketsystem.DTO.CompanyDTO;
import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.company.*;

public class CompanyService {
    private final ICompanyRepository companyRepository;
    private final ITokenService tokenService;

    public CompanyService(ICompanyRepository repo, ITokenService tokenService) {
        this.companyRepository = repo;
        this.tokenService = tokenService;
    }

    private long getRegisteredMemberId(String token) throws Exception {
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
    }

    /**
     * Use Case 3.2: Create a production company.
     */
    public CompanyDTO createProductionCompany(String sessionId, String companyName) throws Exception {
        try {
            long memberId = getRegisteredMemberId(sessionId);

            Company newCompany = new Company(
                companyName, 
                memberId, 
                new PurchasePolicy(), 
                new DiscountPolicy()
            );

            companyRepository.save(newCompany); 

            return new CompanyDTO(newCompany);
        } catch (Exception e) {
            throw e; 
        }
    }

    /**
     * Use Case 4.13: Close or suspend production company
     */
    public CompanyDTO closeProductionCompany(String sessionId, long companyId) throws Exception {
        try {
            long memberId = getRegisteredMemberId(sessionId);

            Company company = companyRepository.findById(companyId) // gets the company from the CompanyRepository
                    .orElseThrow(() -> new Exception("Error: Company not found."));

            company.closeOrSuspend(memberId);
            companyRepository.save(company);
            return new CompanyDTO(company);
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Use Case 4.14: Reopen production company
     */
    public CompanyDTO reopenProductionCompany(String sessionId, long companyId) throws Exception {
        try {
            long memberId = getRegisteredMemberId(sessionId);

            Company company = companyRepository.findById(companyId) // gets the company from the CompanyRepository
                    .orElseThrow(() -> new Exception("Error: Company not found."));

            company.reopenCompany(memberId);
            companyRepository.save(company);
            return new CompanyDTO(company);

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

public void removeUserFromAllCompanies(long memberIdToDelete) throws Exception {
try {
            // Phase 1: Validation - Efficiently check if the user is a Founder anywhere directly in the DB
            boolean isFounderAnywhere = companyRepository.existsByFounderId(memberIdToDelete);
            
            if (isFounderAnywhere) {
                // If the user is a founder, throw an exception and completely stop the deletion process
                throw new Exception("Cannot delete user: The user is a Founder of one or more companies.");
            }

            // Phase 2: Fetch ONLY the companies where the user is an owner or a manager
            // We pass 'memberIdToDelete' twice because we check both the 'owners' and 'managers' lists
            List<Company> relevantCompanies = companyRepository.findByOwnersContainingOrManagersContaining(memberIdToDelete, memberIdToDelete);

            // Phase 3: Remove from roles and save
            for (Company company : relevantCompanies) {
                // Remove the user and handle reassigning their appointees
                company.removeUserFromAllRoles(memberIdToDelete);
                
                // We don't need a 'needsSaving' flag anymore, because we only fetched companies that actually need updating
                companyRepository.save(company);
            }
            
        } catch (Exception e) {
            // Propagate the exception so the calling layer can return an appropriate message
            throw new Exception("Failed to remove user from companies: " + e.getMessage());
        }
    }

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
}