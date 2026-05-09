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
    private final long FOUNDER_ID = 100L;
    private final long SECOND_OWNER_ID = 101L;
    private final long MANAGER_ID = 102L;

    @BeforeEach
    void setUp() {
        // Arrange - Setup the base environment for each test 
        company = new Company("BGU Productions", FOUNDER_ID, new PurchasePolicy(), new DiscountPolicy());
        
        // Initialize some roles to test hierarchy and permissions
        company.registerNewAppointment(FOUNDER_ID, SECOND_OWNER_ID, "OWNER");
        company.registerNewAppointment(FOUNDER_ID, MANAGER_ID, "MANAGER");
    }

    // --- UC 3.2: Create Company & ID Generation ---

    @Test
    void GivenNewCompany_WhenCreated_ThenFounderIsAddedToOwners() {
        assertTrue(company.getOwners().contains(FOUNDER_ID), "Founder should be automatically added to owners list.");
        assertTrue(company.isActive(), "A new company should be active by default.");
    }

    @Test
    void GivenNewCompanies_WhenCreated_ThenIdsAreUniqueAndIncremented() {
        Company secondCompany = new Company("Another Company", FOUNDER_ID, new PurchasePolicy(), new DiscountPolicy());
        
        // Assert
        assertTrue(company.getId() > 0, "Company ID should be a positive number.");
        assertTrue(secondCompany.getId() > company.getId(), "New company should receive a higher incremented ID.");
        assertNotEquals(company.getId(), secondCompany.getId(), "Every company should have a unique ID.");
    }

    // --- UC 4.13: Close/Suspend Company ---

    @Test
    void GivenActiveCompany_WhenFounderCloses_ThenStatusIsInactive() throws Exception {
        // Act
        company.closeOrSuspend(FOUNDER_ID);
        
        // Assert
        assertFalse(company.isActive(), "Company should be inactive after founder closes it.");
    }

    @Test
    void GivenActiveCompany_WhenNonFounderCloses_ThenExceptionIsThrown() {
        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> company.closeOrSuspend(SECOND_OWNER_ID));
        assertTrue(exception.getMessage().contains("Only the Founder can close"), "Should reject non-founder requests.");
    }

    @Test
    void GivenInactiveCompany_WhenFounderCloses_ThenExceptionIsThrown() throws Exception {
        // Arrange
        company.closeOrSuspend(FOUNDER_ID);
        
        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> company.closeOrSuspend(FOUNDER_ID));
        assertTrue(exception.getMessage().contains("already inactive"));
    }

    // --- UC 4.14: Reopen Company ---

    @Test
    void GivenInactiveCompany_WhenFounderReopens_ThenStatusIsActive() throws Exception {
        // Arrange
        company.closeOrSuspend(FOUNDER_ID);
        
        // Act
        company.reopenCompany(FOUNDER_ID);
        
        // Assert
        assertTrue(company.isActive(), "Company should be active after founder reopens it.");
    }

    @Test
    void GivenInactiveCompany_WhenManagerReopens_ThenExceptionIsThrown() throws Exception {
        // Arrange
        company.closeOrSuspend(FOUNDER_ID);
        
        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> company.reopenCompany(MANAGER_ID));
        assertTrue(exception.getMessage().contains("Only the Founder can reopen"));
    }

    @Test
    void GivenActiveCompany_WhenFounderReopens_ThenExceptionIsThrown() {
        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> company.reopenCompany(FOUNDER_ID));
        assertTrue(exception.getMessage().contains("already Active"));
    }

    // --- UC 4.15: View Roles Tree ---

    @Test
    void GivenOwnersAndManagers_WhenOwnerRequestsTree_ThenRepresentationIsCorrect() throws Exception {
        // Arrange
        Map<Long, String> mockPermissions = new HashMap<>();
        mockPermissions.put(MANAGER_ID, "inventory:event:manage");

        // Act
        String treeOutput = company.getRolesTreeRepresentation(FOUNDER_ID, mockPermissions);
        
        // Assert
        assertNotNull(treeOutput);
        assertTrue(treeOutput.contains("Role: FOUNDER"), "Tree must display the founder role.");
        assertTrue(treeOutput.contains("Role: MANAGER"), "Tree must display assigned manager role.");
        assertTrue(treeOutput.contains("inventory:event:manage"), "Tree should include external permissions.");
    }

    @Test
    void GivenManager_WhenRequestsTree_ThenExceptionIsThrown() {
        // Arrange
        Map<Long, String> mockPermissions = new HashMap<>();
        
        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> company.getRolesTreeRepresentation(MANAGER_ID, mockPermissions));
        assertTrue(exception.getMessage().contains("Only Owners can view"), "Only owners should have access to the tree.");
    }

    // --- Additional Logic: Remove User & Concurrency ---

    @Test
    void GivenUserIsManager_WhenRemoveUserFromAllRoles_ThenUserIsRemoved() throws Exception {
        // Act
        company.removeUserFromAllRoles(MANAGER_ID);
        
        // Assert
        assertFalse(company.getManagers().contains(MANAGER_ID), "User should be removed from the manager list.");
        
        Map<Long, String> emptyPerms = new HashMap<>();
        String tree = company.getRolesTreeRepresentation(FOUNDER_ID, emptyPerms);
        assertFalse(tree.contains("ID: " + MANAGER_ID), "User should be removed from the hierarchy tree.");
    }

    @Test
    void GivenCompany_WhenCopyConstructorCalled_ThenDetachedCopyIsCreated() {
        // Arrange - Setup for optimistic locking check [cite: 747]
        company.setVersion(5);
        
        // Act
        Company copy = new Company(company);
        
        // Assert
        assertEquals(company.getId(), copy.getId());
        assertEquals(5, copy.getVersion(), "Version should be copied correctly.");
        assertNotSame(company.getOwners(), copy.getOwners(), "Deep copy check: owners lists must be different objects.");
        
        copy.setVersion(6);
        assertNotEquals(company.getVersion(), copy.getVersion(), "Modifying detached copy should not affect the original.");
    }
}