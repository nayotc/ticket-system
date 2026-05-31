package ticketsystem.InfrastructureLayer.Bootstrap;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;

import ticketsystem.ApplicationLayer.CompanyService;
import ticketsystem.ApplicationLayer.HistoryService;
import ticketsystem.ApplicationLayer.UserService;
import ticketsystem.DTO.OrderDTO;
import ticketsystem.DTO.PurchaseDTO;
import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.DomainLayer.company.Company;
import ticketsystem.DomainLayer.policy.PurchasePolicy;
import ticketsystem.DomainLayer.discount.DiscountCompositionType;
import ticketsystem.DomainLayer.discount.DiscountPolicy;
import ticketsystem.DomainLayer.user.Member;

@Component
@Profile("dev")
@Order(1)
public class DevDataInitializer implements CommandLineRunner {

    private static final String TEST_USERNAME = "test@test.com";
    private static final String TEST_PASSWORD = "123456";

    private static final String FOUNDER_USERNAME = "founder@test.com";
    private static final String FOUNDER_PASSWORD = "123456";
    
    private static final long TEST_COMPANY_ID = 1L;
    private static final String COMPANY_NAME = "TixNow Productions"; 
    
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

    public void run(String... args) {
        createTestMember();               // 1. Create the regular buyer member
        createTestFounder();              // 2. Create the company founder member
        createTestCompany();              // 3. Create the production company owned by the founder
        createTestSalesData();            // 4. Generate transactions where test user is the buyer
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
            System.out.println("Dev founder already exists: " + FOUNDER_USERNAME);
            return;
        }

        String guestToken = userService.visitSystem();
        userService.signUp(guestToken, FOUNDER_USERNAME, FOUNDER_PASSWORD, "Test Founder", "0500000001");

        System.out.println("Dev founder created:");
        System.out.println("username: " + FOUNDER_USERNAME);
        System.out.println("password: " + FOUNDER_PASSWORD);
    }

    /**
     * Creates the actual production company in the system owned by the founder.
     */
    private void createTestCompany() {
        System.out.println("Creating test production company...");
        
        var founder = userRepository.getMemberByUsername(FOUNDER_USERNAME);
        founder.addFounderRole(TEST_COMPANY_ID);
        userRepository.updateMember(founder);

        Company company = new Company(COMPANY_NAME, founder.getId(), PurchasePolicy.noRestrictions(), new DiscountPolicy(DiscountCompositionType.MAX));
        company.setId(TEST_COMPANY_ID); 
        companyRepository.save(company); 
        
        System.out.println("Test company created: " + company.getName() + " [ID: " + company.getId() + "] owned by Founder ID: " + founder.getId());
    }

    /**
     * Generates test transactions where the test user is the buyer and the founder is the manager.
     */
    private void createTestSalesData() {
        System.out.println("Generating test sales data for HistoryService...");
        
        // Fetch the regular member ID (test@test.com) who acts as the buyer
        var buyer = userRepository.getMemberByUsername(TEST_USERNAME);
        if (!(buyer instanceof Member)) return;
        long buyerId = ((Member) buyer).getId();
        
        // Fetch the Founder ID (founder@test.com) who manages the event
        var founder = userRepository.getMemberByUsername(FOUNDER_USERNAME);
        if (!(founder instanceof Member)) return;
        long founderId = ((Member) founder).getId();
        
        // Transaction 1: 2 Tickets bought by the regular test member, managed by the Founder
        PurchaseDTO ticket1 = new PurchaseDTO(100L, 1, 12, BigDecimal.valueOf(180), "ACTIVE", "BARCODE-123");
        PurchaseDTO ticket2 = new PurchaseDTO(101L, 1, 13, BigDecimal.valueOf(180), "ACTIVE", "BARCODE-124");
        
        OrderDTO order1 = new OrderDTO(8492L, List.of(ticket1, ticket2), "פסטיבל אורות הלילה", "תל אביב", buyerId, TEST_COMPANY_ID, founderId, 91L);
        historyService.onOrderCompleted(order1);

        // Transaction 2: 1 Ticket bought by the regular test member, managed by the Founder
        PurchaseDTO ticket3 = new PurchaseDTO(102L, 2, 5, BigDecimal.valueOf(120), "ACTIVE", "BARCODE-125");
        OrderDTO order2 = new OrderDTO(8491L, List.of(ticket3), "הופעת רוק במדבר", "באר שבע", buyerId, TEST_COMPANY_ID, founderId, 92L);
        historyService.onOrderCompleted(order2);
        
        System.out.println("Test sales data generated successfully. Buyer: " + TEST_USERNAME + ", Founder: " + FOUNDER_USERNAME);
        
        // Printing a detailed console summary of the loaded mock transactions
        System.out.println("=========================================================================");
        System.out.println("DEVELOPMENT MOCK SALES DATA SUMMARY FOR COMPANY ID: #" + TEST_COMPANY_ID);
        System.out.println("-------------------------------------------------------------------------");
        System.out.println(" -> Order #8492: 'Night Lights Festival' (Tel Aviv)");
        System.out.println("    Tickets: 2 | Price: 180.00 NIS each | Status: ACTIVE | Buyer ID: " + buyerId);
        System.out.println(" -> Order #8491: 'Rock Concert in the Desert' (Beer Sheva)");
        System.out.println("    Tickets: 1 | Price: 120.00 NIS | Status: ACTIVE | Buyer ID: " + buyerId);
        System.out.println("-------------------------------------------------------------------------");
        System.out.println(" -> EXPECTED REPORT TOTALS: 3 Tickets Sold | Total Revenue: 480.00 NIS");
        System.out.println("=========================================================================");
        System.out.println();
    }

}