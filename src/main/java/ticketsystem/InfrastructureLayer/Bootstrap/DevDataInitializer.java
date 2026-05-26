package ticketsystem.InfrastructureLayer.Bootstrap;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import ticketsystem.ApplicationLayer.CompanyService;

import ticketsystem.ApplicationLayer.HistoryService;
import ticketsystem.ApplicationLayer.UserService;
import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DTO.OrderDTO;
import ticketsystem.DTO.PurchaseDTO;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.DomainLayer.user.CompanyRole;
import ticketsystem.DomainLayer.user.Member;
import ticketsystem.DomainLayer.user.Permission;

@Component
@Profile("dev")
public class DevDataInitializer implements CommandLineRunner {

    private static final String TEST_USERNAME = "test@test.com";
    private static final String TEST_PASSWORD = "123456";

    private static final String FOUNDER_USERNAME = "founder@test.com";
    private static final String FOUNDER_PASSWORD = "123456";
    private static final long TEST_COMPANY_ID = 1L; // The ID of the company for which we will generate the report
    
    private final UserService userService;
    private final IUserRepository userRepository;
    private final CompanyService companyService;
    private final ICompanyRepository companyRepository;
    private final HistoryService historyService;

    public DevDataInitializer(UserService userService, IUserRepository userRepository, CompanyService companyService, ICompanyRepository companyRepository, HistoryService historyService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.companyService = companyService;
        this.companyRepository = companyRepository;
        this.historyService = historyService;
    }

    @Override
    public void run(String... args) {
        createTestMember();
        createTestFounder();
        assignPermissionsToTestMember(); // Calling the new function that sets up permissions
        createTestSalesData();           // Calling the function that generates the transactions
    }

    private void createTestMember() {
        if (userRepository.isUsernameTaken(TEST_USERNAME)) {
            System.out.println("Dev user already exists: " + TEST_USERNAME);
            return;
        }

        String guestToken = userService.visitSystem();
        userService.signUp(guestToken, TEST_USERNAME, TEST_PASSWORD, "Test User", "0500000000");

        System.out.println("Dev user created:");
        System.out.println("username: " + TEST_USERNAME);
        System.out.println("password: " + TEST_PASSWORD);
    }

    private void createTestFounder() {
        if (userRepository.isUsernameTaken(FOUNDER_USERNAME)) {
            System.out.println("Dev user already exists: " + FOUNDER_USERNAME);
            return;
        }

        String guestToken = userService.visitSystem();
        userService.signUp(guestToken, FOUNDER_USERNAME, FOUNDER_PASSWORD, "Test Founder", "0500000000");

        System.out.println("Dev user created:");
        System.out.println("username: " + FOUNDER_USERNAME);
        System.out.println("password: " + FOUNDER_PASSWORD);
    }

    /**
     * New function: Configures the test user as the Founder of the company.
     * This is critical so that HistoryService allows them to generate the sales report.
     */
    private void assignPermissionsToTestMember() {
        System.out.println("Assigning Founder permissions to the test user...");

        var user = userRepository.getMemberByUsername(TEST_USERNAME); 
        
        // Ensuring the user is of type Member as defined in the domain
        if (user instanceof Member) {
            Member member = (Member) user;
            
            // 1. Adding the founder role to the test company (ID = 1)
            member.addFounderRole(TEST_COMPANY_ID);
            
            // 2. Fetching the role we just added
            CompanyRole role = member.getRoleInCompany(TEST_COMPANY_ID);
            
            if (role != null && role.isPending()) {
                // 3. Activating the role so it receives all permissions (including GENERATE_SALES_REPORT)
                role.activate();
            }
            
            // Saving the updated user in the database
            userRepository.updateMember(member); 
            
            System.out.println("Founder permissions assigned successfully.");
            System.out.println("Founder has permission to GENERATE_SALES_REPORT: " + member.hasPermission(TEST_COMPANY_ID, Permission.GENERATE_SALES_REPORT));
        }
    }

/**
     * New function: Generates test transactions directly into the HistoryService
     */
    private void createTestSalesData() {
        System.out.println("Generating test sales data for HistoryService...");
        
        // We need the user's ID to associate the sales with them
        var user = userRepository.getMemberByUsername(TEST_USERNAME);
        if (!(user instanceof Member)) return;
        long memberId = ((Member) user).getId();
        
        // Creating transaction 1: Night Lights Festival (2 tickets)
        PurchaseDTO ticket1 = new PurchaseDTO(100L, 1, 12, BigDecimal.valueOf(180), "ACTIVE", "BARCODE-123");
        PurchaseDTO ticket2 = new PurchaseDTO(101L, 1, 13, BigDecimal.valueOf(180), "ACTIVE", "BARCODE-124");
        
        OrderDTO order1 = new OrderDTO(8492L, List.of(ticket1, ticket2), "פסטיבל אורות הלילה", "תל אביב", memberId, TEST_COMPANY_ID, memberId, 91L);
        historyService.onOrderCompleted(order1);

        // Creating transaction 2: Rock Concert in the Desert (1 ticket)
        PurchaseDTO ticket3 = new PurchaseDTO(102L, 2, 5, BigDecimal.valueOf(120), "ACTIVE", "BARCODE-125");
        OrderDTO order2 = new OrderDTO(8491L, List.of(ticket3), "הופעת רוק במדבר", "באר שבע", memberId, TEST_COMPANY_ID, memberId, 92L);
        historyService.onOrderCompleted(order2);
        
        System.out.println("Test sales data generated successfully.");
        
        // Printing a detailed console summary of the loaded mock transactions
        System.out.println("=========================================================================");
        System.out.println("DEVELOPMENT MOCK SALES DATA SUMMARY FOR COMPANY ID: #" + TEST_COMPANY_ID);
        System.out.println("-------------------------------------------------------------------------");
        System.out.println(" -> Order #8492: 'Night Lights Festival' (Tel Aviv)");
        System.out.println("    Tickets: 2 | Price: 180.00 NIS each | Status: ACTIVE | Subtotal: 360.00 NIS");
        System.out.println(" -> Order #8491: 'Rock Concert in the Desert' (Beer Sheva)");
        System.out.println("    Tickets: 1 | Price: 120.00 NIS | Status: ACTIVE | Subtotal: 120.00 NIS");
        System.out.println("-------------------------------------------------------------------------");
        System.out.println(" -> EXPECTED REPORT TOTALS: 3 Tickets Sold | Total Revenue: 480.00 NIS");
        System.out.println("=========================================================================");
        System.out.println();
    }
}