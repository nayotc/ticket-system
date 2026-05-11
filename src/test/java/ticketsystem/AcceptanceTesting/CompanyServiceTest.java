package ticketsystem.AcceptanceTesting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.ApplicationLayer.CompanyService;
import ticketsystem.ApplicationLayer.UserService;
import ticketsystem.DTO.CompanyDTO;

public class CompanyServiceTest {
private CompanyService companyService;
    private UserService userService;
    
    private String founderToken;
    private String nonFounderToken;
    
    private final String VALID_COMPANY_NAME = "BGU Productions";

    @BeforeEach
    void setUp() throws Exception {
        // Setup (Arrange) 

        founderToken = userService.visitSystem();
        userService.signUp(founderToken, "noa_user", "password123");
        userService.logIn(founderToken, "noa_user", "password123");

        nonFounderToken = userService.visitSystem();
        userService.signUp(nonFounderToken, "other_user", "password123");
        userService.logIn(nonFounderToken, "other_user", "password123");
    }

    // --- Create a production company (UC 3.2) ---

    @Test
    void GivenLoggedInUserAndAvailableName_WhenCreateCompany_ThenCompanyCreatedAndUserIsFounder() throws Exception {
        // Act
        CompanyDTO newCompany = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);

        // Assert
        assertNotNull(newCompany, "A production company should be created.");
        assertEquals(VALID_COMPANY_NAME, newCompany.getName(), "Company name should match the requested name.");
        assertTrue(newCompany.getFounderId() != 0, "The creator should be assigned a valid founder ID.");
        assertTrue(newCompany.isActive(), "The new company should be active.");
    }

    @Test
    void GivenLoggedInUserAndTakenName_WhenCreateCompany_ThenThrowsException() throws Exception {
        companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);

        Exception exception = assertThrows(Exception.class, () -> 
            companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME)
        );
        assertTrue(exception.getMessage().contains("already exist"), "System should reject creation if name is taken.");
    }

    // --- Close or suspend production company (UC 4.13) ---

    @Test
    void GivenActiveCompanyAndFounder_WhenCloseCompany_ThenStatusIsInactive() throws Exception {
        CompanyDTO company = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);

        // Act
        companyService.closeProductionCompany(founderToken, company.getId());

        // Assert
        CompanyDTO updatedCompany = companyService.getCompanyDetails(founderToken, company.getId());
        assertFalse(updatedCompany.isActive(), "Company status should be Inactive.");
    }

    @Test
    void GivenActiveCompanyAndNonFounder_WhenCloseCompany_ThenThrowsException() throws Exception {
        CompanyDTO company = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);

        Exception exception = assertThrows(Exception.class, () -> 
            companyService.closeProductionCompany(nonFounderToken, company.getId())
        );
        assertTrue(exception.getMessage().contains("lack of permissions"), "System should reject close request from non-founder.");
    }

    // --- Reopen production company (UC 4.14) ---

    @Test
    void GivenInactiveCompanyAndFounder_WhenReopenCompany_ThenStatusIsActive() throws Exception {
        CompanyDTO company = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);
        companyService.closeProductionCompany(founderToken, company.getId());

        // Act
        companyService.reopenProductionCompany(founderToken, company.getId());

        // Assert
        CompanyDTO updatedCompany = companyService.getCompanyDetails(founderToken, company.getId());
        assertTrue(updatedCompany.isActive(), "Company status should be Active again.");
    }

    @Test
    void GivenInactiveCompanyAndNonFounder_WhenReopenCompany_ThenThrowsException() throws Exception {
        CompanyDTO company = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);
        companyService.closeProductionCompany(founderToken, company.getId());

        Exception exception = assertThrows(Exception.class, () -> 
            companyService.reopenProductionCompany(nonFounderToken, company.getId())
        );
        assertTrue(exception.getMessage().contains("lack of permissions"), "System should reject reopen request from non-founder.");
    }

    @Test
    void GivenActiveCompanyAndFounder_WhenReopenCompany_ThenThrowsException() throws Exception {
        CompanyDTO company = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);

        Exception exception = assertThrows(Exception.class, () -> 
            companyService.reopenProductionCompany(founderToken, company.getId())
        );
        assertTrue(exception.getMessage().contains("no action is needed") || exception.getMessage().toLowerCase().contains("already active"), 
            "System should notify that the company is already active.");
    }

    // --- View roles and permissions tree (UC 4.15) ---

    // @Test
    // void GivenCompanyAndOwner_WhenViewRolesTree_ThenReturnsTreeString() throws Exception {
    //     CompanyDTO company = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);

    //     // Act
    //     String rolesTree = companyService.viewRolesAndPermissionsTree(founderToken, company.getId());

    //     // Assert
    //     assertNotNull(rolesTree, "Roles tree representation should not be null.");
    //     assertTrue(rolesTree.contains("FOUNDER"), "The tree should display the founder role.");
    // }
}

