package ticketsystem.InfrastructureLayer.Bootstrap;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
// Spring Boot imports
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;

//Services
import ticketsystem.ApplicationLayer.CompanyService;
import ticketsystem.ApplicationLayer.EventService;
import ticketsystem.ApplicationLayer.SystemAdminService;
import ticketsystem.ApplicationLayer.UserService;
import ticketsystem.ApplicationLayer.HistoryService;
import ticketsystem.ApplicationLayer.ITokenService;
import ticketsystem.ApplicationLayer.MembershipService;
import ticketsystem.ApplicationLayer.PurchasePolicyMapper;
// Repositories
import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
// DTOs
import ticketsystem.DTO.OrderDTO;
import ticketsystem.DTO.PaymentDetails;
import ticketsystem.DTO.PurchaseDTO;
import ticketsystem.DTO.PurchasePolicyDTO;
import ticketsystem.DTO.PurchaseRuleDTO;
import ticketsystem.DTO.PurchaseRuleType;
// Domain Entities
import ticketsystem.DomainLayer.company.Company;
import ticketsystem.DomainLayer.policy.PurchasePolicy;
import ticketsystem.DomainLayer.discount.DiscountCompositionType;
import ticketsystem.DomainLayer.discount.DiscountPolicy;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.event.EventCategory;
import ticketsystem.DomainLayer.event.EventLocation;
import ticketsystem.DomainLayer.event.Pair;
import ticketsystem.DomainLayer.user.CompanyRole;
import ticketsystem.DomainLayer.user.Founder;
import ticketsystem.DomainLayer.user.Member;
import ticketsystem.DomainLayer.user.Permission;
import ticketsystem.DomainLayer.user.RoleStatus;

@Component
@Profile("dev")
@Order(1)
public class DevDataInitializer implements CommandLineRunner {

    private static final String TEST_USERNAME = "test@test.com";
    private static final String TEST_PASSWORD = "123456";

    private static final String FOUNDER_USERNAME = "founder@test.com";
    private static final String FOUNDER_PASSWORD = "123456";

    private static final String MANAGER_USERNAME = "manager@test.com";
    private static final String OWNER_USERNAME = "owner@test.com";
    
    private static final long TEST_COMPANY_ID = 1L;
    private static final String COMPANY_NAME = "TixNow Productions"; 

    private static final String REPORT_MANAGER_USERNAME = "report@test.com";
    private static final String REPORT_MANAGER_PASSWORD = "123456";
    private static final String SYSTEM_ADMIN_USERNAME = "sysadmin@test.com";
    private static final String SYSTEM_ADMIN_PASSWORD = "123456";
    
    private final ITokenService tokenService;
    private final UserService userService;
    private final IUserRepository userRepository;
    private final SystemAdminService systemAdminService;
    private final MembershipService membershipService;
    private final CompanyService companyService;
    private final ICompanyRepository companyRepository;
    private final HistoryService historyService;
    private final EventService eventService;
    private final IEventRepository eventRepository;


    public DevDataInitializer(ITokenService tokenService, UserService userService, IUserRepository userRepository, SystemAdminService systemAdminService, MembershipService membershipService, CompanyService companyService, ICompanyRepository companyRepository, HistoryService historyService, EventService eventService, IEventRepository eventRepository) {
        this.tokenService = tokenService;
        this.userService = userService;
        this.userRepository = userRepository;
        this.systemAdminService = systemAdminService;
        this.membershipService = membershipService;
        this.companyService = companyService;
        this.companyRepository = companyRepository;
        this.historyService = historyService;
        this.eventService = eventService;
        this.eventRepository = eventRepository;
    }

    public void run(String... args) throws Exception {
        createTestMember();               // 1. Create the regular buyer member
        createTestSystemAdmin();          // 1.1 Create a member and promote to system admin
        createTestFounder();              // 2. Create the company founder member
        createAdditionalTeamMembers();    // 3. Create extra members for management testing
        createTestCompany();              // 4. Create the main production company 
        createReportOnlyManager();  
        assignTeamRoles();                // 5. Assign Owner, Manager, and Pending roles to members
        createTestEvents();               // 6. Create actual Events in the system (Matching the mock sales)
        createTestSalesData();            // 7. Generate transactions where test user is the buyer
    }

    private void createTestMember() {
        if (userRepository.isUsernameTaken(TEST_USERNAME)) {
            System.out.println("Dev user already exists: " + TEST_USERNAME);
            return;
        }

        String guestToken = userService.visitSystem();
        userService.signUp(guestToken, TEST_USERNAME, TEST_PASSWORD, "Test User", "0500000000", LocalDate.of(2001, 1, 1));

        System.out.println("Dev user created:");
        System.out.println("username: " + TEST_USERNAME);
        System.out.println("password: " + TEST_PASSWORD);
    }

    private void createTestSystemAdmin() {
        if (userRepository.isUsernameTaken(SYSTEM_ADMIN_USERNAME)) {
            System.out.println("Dev system admin user already exists: " + SYSTEM_ADMIN_USERNAME);
            try {
                Member existing = (Member) userRepository.getMemberByUsername(SYSTEM_ADMIN_USERNAME);
                if (existing != null) {
                    systemAdminService.promoteMemberToSystemAdmin(existing.getId());
                }
            } catch (Exception e) {
                System.out.println("Failed to promote existing user to system admin: " + e.getMessage());
            }
            return;
        }

        String guestToken = userService.visitSystem();
        userService.signUp(guestToken, SYSTEM_ADMIN_USERNAME, SYSTEM_ADMIN_PASSWORD, "Dev System Admin", "0500000004", LocalDate.of(2001, 1, 1));

        Member adminMember = (Member) userRepository.getMemberByUsername(SYSTEM_ADMIN_USERNAME);
        if (adminMember != null) {
            try {
                systemAdminService.promoteMemberToSystemAdmin(adminMember.getId());
                System.out.println("Dev system admin created and promoted:");
                System.out.println("username: " + SYSTEM_ADMIN_USERNAME);
                System.out.println("password: " + SYSTEM_ADMIN_PASSWORD);
            } catch (Exception e) {
                System.out.println("Failed to promote user to system admin: " + e.getMessage());
            }
        } else {
            System.out.println("Failed to find system admin member after signup: " + SYSTEM_ADMIN_USERNAME);
        }
    }

    private void createTestFounder() {
        if (userRepository.isUsernameTaken(FOUNDER_USERNAME)) {
            System.out.println("Dev founder already exists: " + FOUNDER_USERNAME);
            return;
        }

        String guestToken = userService.visitSystem();
        userService.signUp(guestToken, FOUNDER_USERNAME, FOUNDER_PASSWORD, "Test Founder", "0500000001",LocalDate.of(2001, 1, 1));

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
            userService.signUp(guestToken, MANAGER_USERNAME, "123456", "Test Manager", "0500000002",LocalDate.of(2001, 1, 1));
            System.out.println("Additional team member created: " + MANAGER_USERNAME);
        }
        if (!userRepository.isUsernameTaken(OWNER_USERNAME)) {
            String guestToken = userService.visitSystem();
            userService.signUp(guestToken, OWNER_USERNAME, "123456", "Test Owner", "0500000003",LocalDate.of(2001, 1, 1));
            System.out.println("Additional team member created: " + OWNER_USERNAME);
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
        try {
            company.setId(TEST_COMPANY_ID); 
        } catch (Exception e) {}
        
        // קריאה לפונקציית העזר החדשה שמלבישה את נתוני הדמה של המדיניות
        setupMockPolicies(company);

        // שמירת החברה (עם המדיניות שעודכנה) למסד הנתונים
        companyRepository.save(company); 
        
        System.out.println("Test company created: " + company.getName() + " [ID: " + company.getId() + "] owned by Founder ID: " + founder.getId());
    }

    /**
     * פונקציית עזר להגדרת נתוני דמה של מדיניות רכישה והנחות עבור חברה ספציפית.
     * ניתן לערוך, להוסיף או להסיר חוקים מכאן בחופשיות לצורכי בדיקות ממשק משתמש.
     */
    private void setupMockPolicies(Company company) {
        System.out.println("Setting up mock discount and purchase policies...");
        
        // --- אתחול מדיניות הנחות דמו ---
        try {
            // הנחה רגילה (10%)
            company.addVisibleDiscountToCompany("הנחת השקה", BigDecimal.valueOf(10));
            // הנחת קופון (20%) לחודש הקרוב
            company.addCouponDiscountToCompany("קופון קיץ", "SUMMER26", BigDecimal.valueOf(20), LocalDateTime.now().plusMonths(1));
            // הנחה מותנית (15%) על רכישה של מינימום 4 כרטיסים
            //company.addConditionalDiscountToCompany("הנחת כמות", null, null, BigDecimal.valueOf(15), Condition.MIN_TICKET, 4);
        } catch (Exception e) {
            System.out.println("Failed to setup mock discount policy: " + e.getMessage());
        }

        // --- אתחול מדיניות רכישה דמו ---
        try {
            // חוק 1: גיל מינימלי 18
            PurchaseRuleDTO ageRule = new PurchaseRuleDTO();
            ageRule.setType(PurchaseRuleType.MIN_AGE);
            ageRule.setValue(18);

            // חוק 2: מקסימום 5 כרטיסים לרוכש
            PurchaseRuleDTO limitRule = new PurchaseRuleDTO();
            limitRule.setType(PurchaseRuleType.MAX_TICKETS);
            limitRule.setValue(5);

            // עץ רכישה: חוק 1 "וגם" (AND) חוק 2
            PurchaseRuleDTO rootRule = new PurchaseRuleDTO();
            rootRule.setType(PurchaseRuleType.AND);
            rootRule.setChildren(List.of(ageRule, limitRule));
            rootRule.setValue(0);

            // המרה ושמירה בחברה באמצעות ה-Mapper
            PurchasePolicyDTO policyDTO = new PurchasePolicyDTO(rootRule);
            ticketsystem.ApplicationLayer.PurchasePolicyMapper mapper = new ticketsystem.ApplicationLayer.PurchasePolicyMapper();
            company.setPurchasePolicy(mapper.toDomain(policyDTO));
            
        } catch (Exception e) {
            System.out.println("Failed to setup mock purchase policy: " + e.getMessage());
        }
    }

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

        Member owner = (Member) userRepository.getMemberByUsername("owner@test.com");
        if (owner != null) {
            long ownerId = owner.getId();
            long mainCompanyId = 1L; // או TEST_COMPANY_ID אם זה המשתנה אצלך

            // 1. אירוע ראשון באחריות ה-Owner: פסטיבל שקיעה
            Event ownerEvent1 = new Event(
                    101L, // מזהה אירוע
                    LocalDateTime.now().plusDays(20), // תאריך
                    "פסטיבל שקיעה", // שם האירוע
                    mainCompanyId, // חברת הפקה
                    ownerId, // מנהל האירוע (ה-Owner שלנו)
                    EventLocation.TEL_AVIV, // מיקום (עדכני ל-Enum שקיים אצלך אם צריך)
                    1000L, // תכולה
                    EventCategory.OTHER, // קטגוריה
                    "מסיבת חוף אל תוך הלילה", // תיאור
                    BigDecimal.valueOf(150), // מחיר בסיס
                    new Pair<>(10, 10) // סידור
            );

            // 2. אירוע שני באחריות ה-Owner: הופעת ג'אז
            Event ownerEvent2 = new Event(
                    102L, 
                    LocalDateTime.now().plusDays(25), 
                    "הופעת ג'אז", 
                    mainCompanyId, 
                    ownerId, // מנהל האירוע (ה-Owner שלנו)
                    EventLocation.BEER_SHEVA, // מיקום
                    300L, // תכולה
                    EventCategory.OTHER, // קטגוריה
                    "הופעה חיה", // תיאור
                    BigDecimal.valueOf(200), // מחיר בסיס
                    new Pair<>(10, 10) // סידור
            );
            
            eventRepository.addEvent(ownerEvent1);
            eventRepository.addEvent(ownerEvent2);
            
            System.out.println("Created 2 events managed by Owner [ID: " + ownerId + "]");
        }
    }

    /**
     * Generates test transactions where the test user is the buyer, 
     * and different members (founder, owner) manage the events.
     */
    private void createTestSalesData() {
        System.out.println("Generating test sales data for HistoryService...");
        
        // Fetch the regular member ID (test@test.com) who acts as the buyer
        var buyer = userRepository.getMemberByUsername(TEST_USERNAME);
        if (!(buyer instanceof Member)) return;
        long buyerId = ((Member) buyer).getId();
        
        // Fetch the Founder ID (founder@test.com) who manages the first events
        var founder = userRepository.getMemberByUsername(FOUNDER_USERNAME);
        if (!(founder instanceof Member)) return;
        long founderId = ((Member) founder).getId();

        // Fetch the Owner ID (owner@test.com) who manages the new events
        var owner = userRepository.getMemberByUsername("owner@test.com");
        if (!(owner instanceof Member)) return;
        long ownerId = ((Member) owner).getId();
        
        // ==========================================
        //         TRANSACTIONS FOR FOUNDER
        // ==========================================

        // Transaction 1: 2 Tickets bought by the regular test member, managed by the Founder
        PurchaseDTO ticket1 = new PurchaseDTO(100L, 1, 12, BigDecimal.valueOf(180), "ACTIVE", "BARCODE-123");
        PurchaseDTO ticket2 = new PurchaseDTO(101L, 1, 13, BigDecimal.valueOf(180), "ACTIVE", "BARCODE-124");
        
        OrderDTO order1 = new OrderDTO(8492L, List.of(ticket1, ticket2), "פסטיבל אורות הלילה", "תל אביב", buyerId, TEST_COMPANY_ID, founderId, 91L, new BigDecimal(100), new PaymentDetails("Fake", "Yosi", LocalDate.of(2001, 1, 1)));
        historyService.onOrderCompleted(order1);

        // Transaction 2: 1 Ticket bought by the regular test member, managed by the Founder
        PurchaseDTO ticket3 = new PurchaseDTO(102L, 2, 5, BigDecimal.valueOf(120), "ACTIVE", "BARCODE-125");
        OrderDTO order2 = new OrderDTO(8491L, List.of(ticket3), "הופעת רוק במדבר", "באר שבע", buyerId, TEST_COMPANY_ID, founderId, 92L, new BigDecimal(100), new PaymentDetails("Fake", "Yosi", LocalDate.of(2001, 1, 1)));
        historyService.onOrderCompleted(order2);
        

        // ==========================================
        //          TRANSACTIONS FOR OWNER
        // ==========================================

        // Transaction 3: 2 Tickets for "Sunset Festival" managed by Owner
        PurchaseDTO ticket4 = new PurchaseDTO(103L, 3, 1, BigDecimal.valueOf(150), "ACTIVE", "BARCODE-126");
        PurchaseDTO ticket5 = new PurchaseDTO(104L, 3, 2, BigDecimal.valueOf(150), "ACTIVE", "BARCODE-127");
        
        OrderDTO order3 = new OrderDTO(8493L, List.of(ticket4, ticket5), "פסטיבל שקיעה", "תל אביב", buyerId, TEST_COMPANY_ID, ownerId, 101L, new BigDecimal(100), new PaymentDetails("Fake", "Yosi", LocalDate.of(2001, 1, 1)));
        historyService.onOrderCompleted(order3);

        // Transaction 4: 1 Ticket for "Jazz Concert" managed by Owner
        PurchaseDTO ticket6 = new PurchaseDTO(105L, 4, 1, BigDecimal.valueOf(200), "ACTIVE", "BARCODE-128");
        OrderDTO order4 = new OrderDTO(8494L, List.of(ticket6), "הופעת ג'אז", "באר שבע", buyerId, TEST_COMPANY_ID, ownerId, 102L, new BigDecimal(100), new PaymentDetails("Fake", "Yosi", LocalDate.of(2001, 1, 1)));
        historyService.onOrderCompleted(order4);


        System.out.println("Test sales data generated successfully. Buyer: " + TEST_USERNAME);
        
        // Printing a detailed console summary of the loaded mock transactions
        // System.out.println("=========================================================================");
        // System.out.println("DEVELOPMENT MOCK SALES DATA SUMMARY FOR COMPANY ID: #" + TEST_COMPANY_ID);
        // System.out.println("-------------------------------------------------------------------------");
        // System.out.println(" -> Order #8492: 'Night Lights Festival' (Tel Aviv) [Manager: Founder]");
        // System.out.println("    Tickets: 2 | Price: 180.00 NIS each | Status: ACTIVE | Buyer ID: " + buyerId);
        // System.out.println(" -> Order #8491: 'Rock Concert in the Desert' (Beer Sheva) [Manager: Founder]");
        // System.out.println("    Tickets: 1 | Price: 120.00 NIS | Status: ACTIVE | Buyer ID: " + buyerId);
        // System.out.println(" -> Order #8493: 'Sunset Festival' (Tel Aviv) [Manager: Owner]");
        // System.out.println("    Tickets: 2 | Price: 150.00 NIS each | Status: ACTIVE | Buyer ID: " + buyerId);
        // System.out.println(" -> Order #8494: 'Jazz Concert' (Beer Sheva) [Manager: Owner]");
        // System.out.println("    Tickets: 1 | Price: 200.00 NIS | Status: ACTIVE | Buyer ID: " + buyerId);
        // System.out.println("-------------------------------------------------------------------------");
        // System.out.println(" -> EXPECTED REPORT TOTALS (All Managers): 6 Tickets Sold | Total Revenue: 980.00 NIS");
        // System.out.println("=========================================================================");
        // System.out.println();
    }

    private void createReportOnlyManager() {
        if (userRepository.isUsernameTaken(REPORT_MANAGER_USERNAME)) {
            System.out.println("Dev report manager already exists: " + REPORT_MANAGER_USERNAME);
            return;
        }

        String guestToken = userService.visitSystem();
        userService.signUp(
                guestToken,
                REPORT_MANAGER_USERNAME,
                REPORT_MANAGER_PASSWORD,
                "Report Manager",
                "0500000002",LocalDate.of(2001, 1, 1)
        );

        Member manager = userRepository.getMemberByUsername(REPORT_MANAGER_USERNAME);

        manager.addManagerRole(
                TEST_COMPANY_ID,
                manager.getId(),
                Set.of(Permission.GENERATE_SALES_REPORT)
        );

        manager.getRoleInCompany(TEST_COMPANY_ID).setStatus(RoleStatus.ACTIVE);

        userRepository.updateMember(manager);

        System.out.println("Dev report-only manager created:");
        System.out.println("username: " + REPORT_MANAGER_USERNAME);
        System.out.println("password: " + REPORT_MANAGER_PASSWORD);
    }

}