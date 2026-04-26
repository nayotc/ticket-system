package ticketsystem.ApplicationLayer;
import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.company.*;
import java.util.logging.Logger;
public class CompanyService {
private final ICompanyRepository companyRepository;
    private final AuthService authService;
 //   private static final Logger logger = Logger.getLogger(CompanyService.class.getName());

    public CompanyService(ICompanyRepository repo, AuthService auth) {
        this.companyRepository = repo;
        this.authService = auth;
    }

    
     // Use Case 3.2: Create a production company.

    public void createProductionCompany(String sessionId, String companyName) throws Exception {
        try {
            // 1. Precondition: Member is logged-in
            String username = authService.getUsernameBySession(sessionId);
            if (username == null) {
          //      logger.warning("Attempt to create company without login. Session: " + sessionId); // דרישת מעקב 
                throw new Exception("Error: Member must be logged in.");
            }

            // 2. Alternative flow: Company already exists
            if (companyRepository.existsByName(companyName)) {
                throw new Exception("Error: A company with this name already exists.");
            }

            // 3. Main Scenario: Register new company [cite: 114]
            Company newCompany = new Company(
                companyName, 
                username, 
                new PurchasePolicy(), 
                new DiscountPolicy()
            );

            companyRepository.save(newCompany); // שמירה ב-Repository

            // 4. System: logs event
          //  logger.info("User " + username + " successfully created company: " + companyName);

        } catch (Exception e) {
            // יומן שגיאות 
            //logger.severe("Operation failed: " + e.getMessage());
            throw e; 
        }
    }
}

