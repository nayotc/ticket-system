package ticketsystem.AcceptanceTesting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.ApplicationLayer.CompanyService;
import ticketsystem.ApplicationLayer.SystemAdminService;
import ticketsystem.ApplicationLayer.TokenService;
import ticketsystem.DTO.CompanyDTO;
import ticketsystem.DTO.OrderDTO;
import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.company.Company;
import ticketsystem.DomainLayer.history.Purchase;
import ticketsystem.DomainLayer.history.PurchasedTicket;
import ticketsystem.DomainLayer.systemAdmin.SystemAdmin;
import ticketsystem.DomainLayer.user.Member;
import ticketsystem.DomainLayer.user.User;
import ticketsystem.InfrastructureLayer.CompanyRepository;
import ticketsystem.InfrastructureLayer.HistoryRepository;
import ticketsystem.InfrastructureLayer.LogbackSystemLogger;
import ticketsystem.InfrastructureLayer.OrderRepository;
import ticketsystem.InfrastructureLayer.PaymentServiceProxy;
import ticketsystem.InfrastructureLayer.SecureBarcodeProxy;
import ticketsystem.InfrastructureLayer.SystemAdminRepository;
import ticketsystem.InfrastructureLayer.TokenRepository;
import ticketsystem.InfrastructureLayer.UserRepository;

public class SystemAdminServiceTest {

    private SystemAdminService systemAdminService;
    private SystemAdminRepository realAdminRepo;
    private TokenService tokenService;
    private UserRepository userRepo = new UserRepository();
    private CompanyService companyService;
    private SystemAdmin admin = new SystemAdmin("1", "Admin123", true);
    ICompanyRepository companyRepo;
    HistoryRepository historyRepo;
    OrderRepository orderRepo;
    LogbackSystemLogger logger = new LogbackSystemLogger();

    @BeforeEach
    public void setUp() {
        realAdminRepo = new SystemAdminRepository();
        PaymentServiceProxy paymentProxy = new PaymentServiceProxy();
        SecureBarcodeProxy barcodeProxy = new SecureBarcodeProxy();
        PaymentServiceProxy.isConnectionSuccessful = true;
        PaymentServiceProxy.wasConnectCalled = false;
        SecureBarcodeProxy.isConnectionSuccessful = true;
        userRepo = new UserRepository();
        companyRepo = new CompanyRepository();
        TokenRepository tokenRepository = new TokenRepository();
        tokenService = new TokenService("manual_test_secret_32_chars_long", tokenRepository);
        companyService = new CompanyService(companyRepo, tokenService);
        orderRepo = new OrderRepository();
        historyRepo = new HistoryRepository();
        systemAdminService = new SystemAdminService(
                realAdminRepo,
                paymentProxy,
                barcodeProxy,
                userRepo,
                orderRepo,
                tokenService,
                companyRepo, logger, historyRepo

        );
    }

    // Use Case: System Initialization
    @Test
    public void givenSystemAdminExists_whenInitSystem_thenSystemInitializesWithProxies() {

        realAdminRepo.addAdmin(admin);
        PaymentServiceProxy.isConnectionSuccessful = true;
        SecureBarcodeProxy.isConnectionSuccessful = true;

        boolean result = systemAdminService.initSystem();
        assertTrue(result, "Acceptance Test Failed: System should initialize successfully using infrastructure proxies.");
        assertEquals(1, realAdminRepo.countAdmins());
    }

    @Test
    public void givenEmptyRepository_whenInitSystem_thenInitializationFails() {
        PaymentServiceProxy.isConnectionSuccessful = true;

        // Act
        boolean result = systemAdminService.initSystem();

        // Assert
        assertFalse(result, "Acceptance Test Failed: System allowed initialization without a System Admin.");
        assertFalse(PaymentServiceProxy.wasConnectCalled, "Payment service should not be contacted if no admin exists.");
    }

    @Test
    public void givenPaymentServiceIsDown_whenInitSystem_thenInitializationFails() {
        // Arrange
        realAdminRepo.addAdmin(admin);

        PaymentServiceProxy.isConnectionSuccessful = false;
        SecureBarcodeProxy.isConnectionSuccessful = true;

        // Act
        boolean result = systemAdminService.initSystem();

        // Assert
        assertFalse(result, "Acceptance Test Failed: System should fail to initialize if payment service is down.");
    }

    @Test
    public void givenBarcodeServiceIsDown_whenInitSystem_thenInitializationFails() {
        // Arrange
        realAdminRepo.addAdmin(admin);

        PaymentServiceProxy.isConnectionSuccessful = true;
        SecureBarcodeProxy.isConnectionSuccessful = false;

        // Act
        boolean result = systemAdminService.initSystem();

        // Assert
        assertFalse(result, "Acceptance Test Failed: System should fail to initialize if barcode service is down.");
    }

    // Use case: delete member by admin
    @Test
    public void givenInvalidAdminId_whenDeleteMember_thenReturnsUnauthorizedError() {
        String result = systemAdminService.deleteMemberByAdmin(-5L, 1L);
        assertTrue(result.startsWith("ERROR: Unauthorized access"), "Should reject invalid token.");
    }

    @Test
    public void givenNonExistentMember_whenDeleteMember_thenReturnsNotFoundError() {
        // Arrange
        SystemAdmin admin = new SystemAdmin("1", "Admin123", true);
        realAdminRepo.addAdmin(admin);
        long nonExistentMemberId = 99L;

        // Act
        String result = systemAdminService.deleteMemberByAdmin(1L, nonExistentMemberId);

        // Assert
        assertEquals("ERROR: Member with ID 99 was not found.", result, "Should return not found error.");
    }

    @Test
    public void givenValidRequest_whenDeleteMember_thenMemberIsDeletedAndCleanupPerformed() {
        // Arrange
        SystemAdmin admin = new SystemAdmin("1", "Admin123", true);
        realAdminRepo.addAdmin(admin);
        long memberId = 1L;

        Member member = new Member(memberId, "TestUser");
        userRepo.addRegisteredMember(memberId, member, "hashedPassword123");

        // Act
        String result = systemAdminService.deleteMemberByAdmin(1L, memberId);

        // Assert
        assertEquals("SUCCESS: Member deactivated and associated records cleaned up.", result);

        User deletedUser = userRepo.getMemberById(memberId);
        assertTrue(deletedUser == null, "Member should be removed from UserRepository.");
    }

    // Use case: close production company by admin
    @Test
    void GivenActiveSystemAdminAndActiveCompany_WhenCloseProductionCompanyByAdmin_ThenCompanyIsClosedAndAppointmentsAreCancelled() throws Exception {
        // Arrange
        long adminId = 1L;
        long founderId = 2L;
        long ownerId = 3L;
        long managerId = 4L;
        // Add active system admin to admin repository
        realAdminRepo.addAdmin(new SystemAdmin(String.valueOf(adminId), "admin", true));

        // Create active company by founder
        String founderSessionId = tokenService.addActiveSession(new Member(founderId, "founder"));
        CompanyDTO createdCompany = companyService.createProductionCompany(founderSessionId, "Test Company");

        // Add owner and manager appointments to the company
        Company company = companyRepo.findById(createdCompany.getId())
                .orElseThrow(() -> new Exception("Company was not created"));

        company.registerNewAppointment(founderId, ownerId, "OWNER");
        company.registerNewAppointment(founderId, managerId, "MANAGER");
        companyRepo.save(company);

        // Act
        CompanyDTO closedCompany = systemAdminService.closeProductionCompanyByAdmin(
                adminId,
                createdCompany.getId()
        );
        // Assert
        assertNotNull(closedCompany);
        Company savedCompany = companyRepo.findById(createdCompany.getId())
                .orElseThrow(() -> new Exception("Company was not found after closing"));
        assertFalse(savedCompany.isActive());
        assertTrue(savedCompany.getOwners().isEmpty());
        assertTrue(savedCompany.getManagers().isEmpty());
    }

    @Test
    void GivenNonAdminMember_WhenCloseProductionCompanyByAdmin_ThenThrowsExceptionAndCompanyRemainsActive() throws Exception {
        // Arrange
        long nonAdminId = 10L;
        long founderId = 20L;
        String founderSessionId = tokenService.addActiveSession(new Member(founderId, "founder"));
        CompanyDTO createdCompany = companyService.createProductionCompany(founderSessionId, "Test Company");

        // Act + Assert
        Exception exception = assertThrows(Exception.class, ()
                -> systemAdminService.closeProductionCompanyByAdmin(nonAdminId, createdCompany.getId())
        );

        assertTrue(exception.getMessage().contains("Unauthorized access"),
                "Should throw unauthorized access exception.");

        Company savedCompany = companyRepo.findById(createdCompany.getId())
                .orElseThrow(() -> new Exception("Company was not found"));

        assertTrue(savedCompany.isActive(), "Company should remain active after a failed closure attempt.");
    }

// Use Case 6.4: View Purchase History by Buyer
    @Test
    void AcceptanceTest_ViewPurchaseHistoryByBuyer_Successful() {
        // --- 1. Preparation (Admin is created, logged in, and is system admin) ---
        long adminId = 1L;
        realAdminRepo.addAdmin(admin); 

        // --- 2. Create Purchase History ---

        long buyer1_Id = 801L;
        long buyer2_Id = 802L;
  
      
        Purchase purchase1 = new Purchase(
            10L, 
            Arrays.asList(new PurchasedTicket(401, 60, 1, 1, 200.0, "BARCODE_A")), 
            "Jazz Festival", 
            "Shuni", 
            buyer1_Id, 
            50L
        );
        
        Purchase purchase2 = new Purchase(
            11L, 
            Arrays.asList(new PurchasedTicket(402, 60, 1, 2, 200.0, "BARCODE_B")), 
            "Jazz Festival", 
            "Shuni", 
            buyer1_Id, 
            50L
        );
        
        Purchase purchase3 = new Purchase(
            12L, 
            Arrays.asList(new PurchasedTicket(403, 61, 5, 5, 150.0, "BARCODE_C")), 
            "Rock Concert", 
            "Barby", 
            buyer2_Id, 
            50L
        );

        historyRepo.addPurchase(purchase1);
        historyRepo.addPurchase(purchase2);
        historyRepo.addPurchase(purchase3);

        // --- 3. Action (Admin requests to view purchase history by buyer) ---
       
        Map<Long, List<OrderDTO>> historyResult = systemAdminService.getPurchaseHistoryByBuyer(adminId);

        // --- 4. Assertions (System displays the purchase history grouped by buyer) ---
        assertNotNull(historyResult, "History result should not be null");
        assertFalse(historyResult.isEmpty(), "History result should not be empty");
        
        
        assertEquals(2, historyResult.size(), "Result should contain exactly 2 distinct buyers");
        
        // buyer 1
        assertTrue(historyResult.containsKey(buyer1_Id), "Result should contain buyer 1");
        List<OrderDTO> buyer1Purchases = historyResult.get(buyer1_Id);
        assertEquals(2, buyer1Purchases.size(), "Buyer 1 should have exactly 2 purchases");
        
        //buyer 2
        assertTrue(historyResult.containsKey(buyer2_Id), "Result should contain buyer 2");
        List<OrderDTO> buyer2Purchases = historyResult.get(buyer2_Id);
        assertEquals(1, buyer2Purchases.size(), "Buyer 2 should have exactly 1 purchase");
    }

    // Use Case 6.4: View Purchase History by Buyer - Failure Scenarios
    @Test
    void AcceptanceTest_ViewPurchaseHistoryByBuyer_Failure_NoHistory() {
        // --- 1. Preparation (Admin is created, logged in, and is system admin) ---
        long adminId = 1L;
        realAdminRepo.addAdmin(admin); 

        // --- 2. NO PURCHASE HISTORY EXISTS ---
        // empty historyRepo, no purchases added

        // --- 3 & 4. Action & Assertions (System displays message / throws exception) ---
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            systemAdminService.getPurchaseHistoryByBuyer(adminId);
        });

        assertTrue(exception.getMessage().contains("No purchases have been made yet."), 
            "Exception message should indicate that no history is available");
    }
    
    // Use Case 6.4: View Purchase History by Buyer - Failure Scenarios
    @Test
    void AcceptanceTest_ViewPurchaseHistoryByBuyer_Failure_UnauthorizedAccess() {
        // --- 1. Preparation (Simulating an unauthorized request) ---
        // נשתמש ב-ID של אדמין שלא קיים במערכת (או שלא הוספנו ל-repo)
        long unauthorizedAdminId = 999L; 

        // --- 2 & 3 & 4. Action & Assertions ---
        Exception exception = assertThrows(SecurityException.class, () -> {
            systemAdminService.getPurchaseHistoryByBuyer(unauthorizedAdminId);
        });

        assertTrue(exception.getMessage().contains("Unauthorized access"), 
            "Exception message should indicate unauthorized access");
    }

    // Use Case 6.4: View Global Purchase History (By Company and Event)
    @Test
    void AcceptanceTest_ViewHistoryByCompanyAndEvent_Successful() {
        // --- 1. Preparation (Admin is logged in) ---
        long adminId = 1L;
        realAdminRepo.addAdmin(admin);

        // --- 2. Create Purchase History ---
        long companyId = 40L;
        String event1_Name = "Tomorrowland Israel";
        String event2_Name = "Winter Festival";
  
        //create purchases for the same company but different events, to check the grouping by both company and event name
        Purchase purchase1 = new Purchase(
            4L, 
            Arrays.asList(new PurchasedTicket(301, 52, 10, 5, 400.0, "VIP_BARCODE")), 
            event1_Name, 
            "Expo TLV", 
            666L, 
            companyId
        );
        Purchase purchase2 = new Purchase(
            5L, 
            Arrays.asList(new PurchasedTicket(302, 52, 10, 6, 400.0, "VIP_BARCODE")), 
            event1_Name, 
            "Expo TLV", 
            777L, 
            companyId
        );
        //one purchase for a different event but same company, to check the grouping by event name as well
        Purchase purchase3 = new Purchase(
            6L, 
            Arrays.asList(new PurchasedTicket(303, 53, 1, 1, 200.0, "REGULAR_BARCODE")), 
            event2_Name, 
            "Expo TLV", 
            888L, 
            companyId 
        );

        historyRepo.addPurchase(purchase1);
        historyRepo.addPurchase(purchase2);
        historyRepo.addPurchase(purchase3);

        // --- 3. Action ---
        Map<Long, Map<String, List<OrderDTO>>> historyResult = 
            systemAdminService.getPurchaseHistoryByCompanyAndEvent(adminId);

        // --- 4. Assertions ---
        assertNotNull(historyResult, "History result should not be null");
        assertFalse(historyResult.isEmpty(), "History result should not be empty");
        
        assertTrue(historyResult.containsKey(companyId), "Result should group by company ID (40)");
        
        Map<String, List<OrderDTO>> eventsForCompany = historyResult.get(companyId);
        assertNotNull(eventsForCompany, "The inner map for the company should not be null");
        assertTrue(eventsForCompany.containsKey(event1_Name), "Result should contain " + event1_Name);
        assertEquals(2, eventsForCompany.get(event1_Name).size(), "There should be exactly 2 purchases for Tomorrowland");
    
        assertTrue(eventsForCompany.containsKey(event2_Name), "Result should contain " + event2_Name);
        assertEquals(1, eventsForCompany.get(event2_Name).size(), "There should be exactly 1 purchase for Winter Festival");
    }

    // Use Case 6.4: View Global Purchase History (By Company and Event) - Failure Scenarios
    @Test
    void AcceptanceTest_ViewHistoryByCompanyAndEvent_Failure_NoHistory() {
        // --- 1. Preparation ---
        long adminId = 1L;
        realAdminRepo.addAdmin(admin); 

        // --- 2. NO PURCHASE HISTORY EXISTS ---
        // empty historyRepo, no purchases added

        // --- 3 & 4. Action & Assertions ---
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            systemAdminService.getPurchaseHistoryByCompanyAndEvent(adminId);
        });

        assertTrue(exception.getMessage().contains("No purchase history"), 
            "Exception message should indicate that no history is available");
    }
    
    // Use Case 6.4: View Global Purchase History (By Company and Event) - Failure Scenarios
    @Test
    void AcceptanceTest_ViewHistoryByCompanyAndEvent_Failure_UnauthorizedAccess() {
        // --- 1. Preparation ---
        long unauthorizedAdminId = 999L; // ID that does not correspond to any real admin in the repository

        // --- 2 & 3 & 4. Action & Assertions ---
        Exception exception = assertThrows(SecurityException.class, () -> {
            systemAdminService.getPurchaseHistoryByCompanyAndEvent(unauthorizedAdminId);
        });

        assertTrue(exception.getMessage().contains("Unauthorized access"), 
            "Exception message should indicate unauthorized access for invalid admin");
    }
}