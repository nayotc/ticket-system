package ticketsystem.UnitTesting;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ticketsystem.DomainLayer.company.Company;
import ticketsystem.DomainLayer.company.DiscountPolicy;
import ticketsystem.DomainLayer.company.PurchasePolicy;

class CompanyTest {

    private Company company;
    // We now use string-based IDs since TokenService uses user IDs, not usernames
    private final String FOUNDER_ID = "100";
    private final String SECOND_OWNER_ID = "101";
    private final String MANAGER_ID = "102";

    @BeforeEach
    void setUp() {
        // Create a new company before each test
        company = new Company("BGU Productions", FOUNDER_ID, new PurchasePolicy(), new DiscountPolicy());
        
        // Add an additional owner and a manager to test permissions
        company.addOwner(SECOND_OWNER_ID);
        company.addManager(MANAGER_ID);
    }

    // --- Tests for UC 3.2 (Create Company) & ID Generation ---

    @Test
    void testCompanyCreation_FounderIsAlsoOwner() {
        // Verify that the founder is automatically added to the owners list (Requirement 3.2)
        assertTrue(company.getOwners().contains(FOUNDER_ID), "Founder should be automatically added to owners list.");
        assertTrue(company.isActive(), "A new company should be active by default.");
    }

    @Test
    void testCompanyCreation_GeneratesValidAndUniqueId() {
        // Verify that the company receives a valid positive long ID
        assertTrue(company.getId() > 0, "Company ID should be a positive number.");
        
        // Create a second company to verify the auto-increment behavior
        Company secondCompany = new Company("Another Company", FOUNDER_ID, new PurchasePolicy(), new DiscountPolicy());
        
        assertTrue(secondCompany.getId() > company.getId(), "New company should receive a higher incremented ID.");
        assertNotEquals(company.getId(), secondCompany.getId(), "Every company should have a unique ID.");
    }

    // --- Tests for UC 13 (Close/Suspend Company) ---

    @Test
    void testCloseCompany_ByFounder_Success() throws Exception {
        // Main Scenario: Founder successfully closes the company
        company.closeOrSuspend(FOUNDER_ID);
        assertFalse(company.isActive(), "Company should be inactive after founder closes it.");
    }

    @Test
    void testCloseCompany_ByOtherOwner_ThrowsException() {
        // Alternative flow: An owner who is not the founder attempts to close the company
        Exception exception = assertThrows(Exception.class, () -> company.closeOrSuspend(SECOND_OWNER_ID));
        assertTrue(exception.getMessage().contains("Only the Founder can close"));
    }

    @Test
    void testCloseCompany_AlreadyInactive_ThrowsException() throws Exception {
        // Founder closes the company first
        company.closeOrSuspend(FOUNDER_ID);
        
        // Founder attempts to close an already inactive company
        Exception exception = assertThrows(Exception.class, () -> company.closeOrSuspend(FOUNDER_ID));
        assertTrue(exception.getMessage().contains("already inactive"));
    }

    // --- Tests for UC 14 (Reopen Company) ---

    @Test
    void testReopenCompany_ByFounder_Success() throws Exception {
        // Close the company first
        company.closeOrSuspend(FOUNDER_ID);
        
        // Founder successfully reopens the company
        company.reopenCompany(FOUNDER_ID);
        assertTrue(company.isActive(), "Company should be active after founder reopens it.");
    }

    @Test
    void testReopenCompany_ByManager_ThrowsException() throws Exception {
        // Close the company first
        company.closeOrSuspend(FOUNDER_ID);
        
        // Manager attempts to reopen the company and is rejected
        Exception exception = assertThrows(Exception.class, () -> company.reopenCompany(MANAGER_ID));
        assertTrue(exception.getMessage().contains("Only the Founder can reopen"));
    }

    @Test
    void testReopenCompany_AlreadyActive_ThrowsException() {
        // Attempt to reopen a company that is already active
        Exception exception = assertThrows(Exception.class, () -> company.reopenCompany(FOUNDER_ID));
        assertTrue(exception.getMessage().contains("already Active"));
    }

    // --- Tests for UC 15 (View Roles Tree) ---

    @Test
    void testViewRolesTree_ByOwner_Success() throws Exception {
        // Create a mock permissions map for testing
        Map<String, String> mockPermissions = new HashMap<>();
        mockPermissions.put(FOUNDER_ID, "Role: OWNER");
        mockPermissions.put(SECOND_OWNER_ID, "Role: OWNER");
        mockPermissions.put(MANAGER_ID, "Role: MANAGER, Permissions: inventory:event:manage");

        // Both the founder and the additional owner are permitted to view the roles tree
        String treeForFounder = company.getRolesTreeRepresentation(FOUNDER_ID, mockPermissions);
        String treeForOwner = company.getRolesTreeRepresentation(SECOND_OWNER_ID, mockPermissions);
        
        assertNotNull(treeForFounder);
        assertNotNull(treeForOwner);
        
        // Additional check to verify the map data is integrated into the string output
        assertTrue(treeForFounder.contains("Role: OWNER"), "Tree should contain the provided permissions string.");
    }

    @Test
    void testViewRolesTree_ByManager_ThrowsException() {
        Map<String, String> mockPermissions = new HashMap<>(); // Empty map is fine for this test
        
        // A manager attempts to view the roles tree and receives an error
        Exception exception = assertThrows(Exception.class, () -> company.getRolesTreeRepresentation(MANAGER_ID, mockPermissions));
        assertTrue(exception.getMessage().contains("Only Owners can view"));
    }
}
