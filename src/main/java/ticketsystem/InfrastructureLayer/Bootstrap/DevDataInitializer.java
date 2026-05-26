package ticketsystem.InfrastructureLayer.Bootstrap;

import java.math.BigDecimal;
import java.util.List;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import ticketsystem.ApplicationLayer.CompanyService;
import ticketsystem.ApplicationLayer.EventService;
import ticketsystem.ApplicationLayer.HistoryService;
import ticketsystem.ApplicationLayer.UserService;
import ticketsystem.DTO.OrderDTO;
import ticketsystem.DTO.PurchaseDTO;
import ticketsystem.DTO.OrderDTO;
import ticketsystem.DTO.PurchaseDTO;
import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.DomainLayer.user.CompanyRole;
import ticketsystem.DomainLayer.user.Member;
import ticketsystem.DomainLayer.user.Permission;
import ticketsystem.DomainLayer.user.Member;
import ticketsystem.DomainLayer.company.Company;
import ticketsystem.DomainLayer.policy.PurchasePolicy;
import ticketsystem.DomainLayer.discount.DiscountCompositionType;
import ticketsystem.DomainLayer.discount.DiscountPolicy;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.event.EventCategory;
import ticketsystem.DomainLayer.event.EventLocation;
import ticketsystem.DomainLayer.user.CompanyRole;
import ticketsystem.DomainLayer.user.Founder;
import ticketsystem.DomainLayer.user.Member;
import ticketsystem.DomainLayer.user.Permission;
import ticketsystem.DomainLayer.event.Pair;

@Component
@Profile("dev")
public class DevDataInitializer implements CommandLineRunner {

    private static final String TEST_USERNAME = "test@test.com";
    private static final String TEST_PASSWORD = "123456";

    private static final String FOUNDER_USERNAME = "founder@test.com";
    private static final String FOUNDER_PASSWORD = "123456";

    private static final String MANAGER_USERNAME = "manager@test.com";
    private static final String OWNER_USERNAME = "owner@test.com";
    
    private static final long TEST_COMPANY_ID = 1L;
    private static final String COMPANY_NAME = "TixNow Productions"; 
    
    private final UserService userService;
    private final IUserRepository userRepository;
    private final CompanyService companyService;
    private final ICompanyRepository companyRepository;
    private final HistoryService historyService;
    private final EventService eventService;
    private final IEventRepository eventRepository;


    public DevDataInitializer(UserService userService, IUserRepository userRepository, CompanyService companyService, ICompanyRepository companyRepository, HistoryService historyService, EventService eventService, IEventRepository eventRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.companyService = companyService;
        this.companyRepository = companyRepository;
        this.historyService = historyService;
        this.eventService = eventService;
        this.eventRepository = eventRepository;
    }

    public void run(String... args) {
        createTestMember();               // 1. Create the regular buyer member
        createTestFounder();              // 2. Create the company founder member
        createAdditionalTeamMembers();    // 3. Create extra members for management testing
        createTestCompany();              // 4. Create the main production company 
        //createSecondCompany();            // 5. Create a second company to test UI Company Selector
        assignTeamRoles();                // 6. Assign Owner, Manager, and Pending roles to members
        createTestEvents();               // 7. Create actual Events in the system (Matching the mock sales)
        createTestSalesData();            // 8. Generate transactions where test user is the buyer
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
     * Creates additional members to populate the team management UI table.
     */
    private void createAdditionalTeamMembers() {
        if (!userRepository.isUsernameTaken(MANAGER_USERNAME)) {
            String guestToken = userService.visitSystem();
            userService.signUp(guestToken, MANAGER_USERNAME, "123456", "Test Manager", "0500000002");
        }
        if (!userRepository.isUsernameTaken(OWNER_USERNAME)) {
            String guestToken = userService.visitSystem();
            userService.signUp(guestToken, OWNER_USERNAME, "123456", "Test Owner", "0500000003");
        }
    }

    /**
     * New function: Configures the test user as the Founder of the company.
     * This is critical so that HistoryService allows them to generate the sales report.
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

    // /**
    //  * Creates a secondary company so the ComboBox (company selector) in the UI appears.
    //  */
    // private void createSecondCompany() {
    //     System.out.println("Creating second test production company...");
    //     long secondCompanyId = 2L;
        
    //     var owner = (Member) userRepository.getMemberByUsername(OWNER_USERNAME);
    //     owner.addFounderRole(secondCompanyId);
    //     userRepository.updateMember(owner);

    //     Company company2 = new Company("Festivals Israel", owner.getId(), PurchasePolicy.noRestrictions(), new DiscountPolicy(DiscountCompositionType.SUM));
    //     company2.setId(secondCompanyId);
    //     companyRepository.save(company2);
    // }

    /**
     * Assigns Manager and Owner roles to the additional members for the main company,
     * including a PENDING role to test the pending assignments counter in the UI.
     */
    private void assignTeamRoles() {
        System.out.println("Assigning team roles for UI testing...");
        
        Member founder = (Member) userRepository.getMemberByUsername(FOUNDER_USERNAME);
        Member manager = (Member) userRepository.getMemberByUsername(MANAGER_USERNAME);
        Member owner = (Member) userRepository.getMemberByUsername(OWNER_USERNAME);
        Member pendingUser = (Member) userRepository.getMemberByUsername(TEST_USERNAME);

        Founder founderRole = (Founder) founder.getRoleInCompany(TEST_COMPANY_ID);

        // 1. Assign an active Owner role
        owner.addOwnerRole(TEST_COMPANY_ID, founder.getId());
        CompanyRole ownerRole = owner.getRoleInCompany(TEST_COMPANY_ID);
        ownerRole.activate(); // Force activate for testing purposes
        founderRole.addAppointee(owner.getId());
        userRepository.updateMember(owner);

        // 2. Assign an active Manager role with specific permissions
        Set<Permission> managerPerms = EnumSet.of(Permission.MANAGE_EVENT_INVENTORY, Permission.MANAGE_INQUIRIES);
        manager.addManagerRole(TEST_COMPANY_ID, founder.getId(), managerPerms);
        CompanyRole managerRole = manager.getRoleInCompany(TEST_COMPANY_ID);
        managerRole.activate(); // Force activate for testing purposes
        founderRole.addAppointee(manager.getId());
        userRepository.updateMember(manager);

        // 3. Assign a Pending Manager role (Do NOT activate)
        // This ensures state.stats().pendingAssignments() returns > 0 in the UI
        Set<Permission> pendingPerms = EnumSet.of(Permission.VIEW_PURCHASE_HISTORY);
        pendingUser.addManagerRole(TEST_COMPANY_ID, founder.getId(), pendingPerms);
        userRepository.updateMember(pendingUser);

        // Update the founder to persist the new appointees list
        userRepository.updateMember(founder);
        
        System.out.println("Team roles assigned successfully.");
    }

        /**
     * Creates real Event domain entities that match the mock sales data below.
     * This ensures the UI can pull real event details from the repository.
     */
    private void createTestEvents() {
        System.out.println("Creating test events for company ID " + TEST_COMPANY_ID + "...");

        Member founder = (Member) userRepository.getMemberByUsername(FOUNDER_USERNAME);
        
        // 1. Event: Night Lights Festival (Matches Order #8492)
        Event event1 = new Event(
                91L, 
                LocalDateTime.now().plusDays(30), 
                "פסטיבל אורות הלילה", 
                TEST_COMPANY_ID, 
                founder.getId(), 
                EventLocation.JERUSALEM,
                1000L, 
                EventCategory.CONCERT,
                "אומנים שונים", 
                BigDecimal.valueOf(180), 
                new Pair<>(10, 20)
        );
        event1.setStatus(Event.eventStatus.ACTIVE);
        // If you have an OPEN status in SaleStatus enum, you can uncomment and adjust the line below:
        // event1.setSaleStatus(SaleStatus.OPEN);
        eventRepository.addEvent(event1);

        // 2. Event: Rock Concert (Matches Order #8491)
        Event event2 = new Event(
                92L, 
                LocalDateTime.now().plusDays(45), 
                "הופעת רוק במדבר", 
                TEST_COMPANY_ID, 
                founder.getId(), 
                EventLocation.BEER_SHEVA,
                500L, 
                EventCategory.OTHER,
                "להקת המדבר", 
                BigDecimal.valueOf(120), 
                new Pair<>(10, 10)
        );
        event2.setStatus(Event.eventStatus.ACTIVE);
        // If you have an OPEN status in SaleStatus enum, you can uncomment and adjust the line below:
        // event2.setSaleStatus(SaleStatus.OPEN);
        eventRepository.addEvent(event2);
        
        System.out.println("Test events generated successfully.");
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