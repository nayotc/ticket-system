package ticketsystem.AcceptanceTesting;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.ApplicationLayer.CompanyService;
import ticketsystem.ApplicationLayer.ISystemLogger;
import ticketsystem.ApplicationLayer.ISystemLogger.LogLevel;
import ticketsystem.ApplicationLayer.ITokenService;
import ticketsystem.ApplicationLayer.TokenService;
import ticketsystem.ApplicationLayer.UserService;
import ticketsystem.DTO.CompanyDTO;
import ticketsystem.DomainLayer.MembershipDomainService;
import ticketsystem.DomainLayer.MembershipDomainService;
import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.IRepository.ITokenRepository;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.InfrastructureLayer.CompanyRepository;
import ticketsystem.InfrastructureLayer.TokenRepository;
import ticketsystem.InfrastructureLayer.UserRepository;
import ticketsystem.DomainLayer.MembershipDomainService;
import ticketsystem.DomainLayer.user.CompanyRole;
import ticketsystem.DomainLayer.user.Founder;
import ticketsystem.DomainLayer.user.RoleStatus;

public class CompanyServiceTest {

    private CompanyService companyService;
    private UserService userService;
    private ITokenService tokenService;
    private ISystemLogger testLogger;

    private String founderToken;
    private String nonFounderToken;
    private IUserRepository userRepository;
    private MembershipDomainService membershipDomain;

    private static final String VALID_COMPANY_NAME = "BGU Productions";

    @BeforeEach
    void setUp() throws Exception {
        ICompanyRepository companyRepository = new CompanyRepository();
        userRepository = new UserRepository();
        ITokenRepository tokenRepository = new TokenRepository();

        tokenService = new TokenService(
                "default_secret_key_for_development_purposes_only_32_chars",
                tokenRepository
        );

        testLogger = new ISystemLogger() {
            @Override
            public void logEvent(String message, LogLevel level) {
                // No-op logger for acceptance tests
            }

            @Override
            public void logError(String errorMessage, Throwable exception) {
                // No-op logger for acceptance tests
            }
        };

        userService = new UserService(userRepository, tokenService, testLogger);
        membershipDomain = new MembershipDomainService(userRepository);
        userService = new UserService(userRepository, tokenService, testLogger);
        companyService = new CompanyService(companyRepository, tokenService, membershipDomain, testLogger);

        founderToken = createLoggedInMember("noa_user", "password123");
        nonFounderToken = createLoggedInMember("other_user", "password123");
    }

    private String createLoggedInMember(String username, String password) {
        String guestToken = userService.visitSystem();

        boolean signedUp = userService.signUp(guestToken, username, password);
        assertTrue(signedUp, "Member signup should succeed during test setup.");

        String memberToken = userService.login(guestToken, username, password);
        assertNotNull(memberToken, "Login should return a member session token.");

        return memberToken;
    }

    // UC 3.2: Create a production company

    @Test
    void GivenLoggedInMember_WhenCreateProductionCompany_ThenCompanyCreatedActiveAndMemberIsFounder() throws Exception {
        // Act
        CompanyDTO company = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);

        // Assert
        assertNotNull(company, "Created company DTO should not be null.");
        assertEquals(VALID_COMPANY_NAME, company.getName(), "Company name should match the requested name.");
        assertTrue(company.isActive(), "New production company should be active.");

        Long founderId = tokenService.extractUserId(founderToken);
        assertEquals(founderId, company.getFounderId(), "The logged-in member should become the company founder.");
        CompanyRole founderRole = userRepository.getMemberById(founderId).getRoleInCompany(company.getId());

        assertNotNull(founderRole, "Founder role should be assigned to the creating member.");
        assertTrue(founderRole instanceof Founder, "The creating member should receive a Founder role.");
        assertEquals(RoleStatus.ACTIVE, founderRole.getStatus(), "Founder role should be active immediately.");
    }

    @Test
    void GivenGuestUser_WhenCreateProductionCompany_ThenThrowsException() {
        // Arrange
        String guestToken = userService.visitSystem();

        // Act + Assert
        assertThrows(Exception.class, () ->
                companyService.createProductionCompany(guestToken, "Guest Company")
        );
    }

    // UC 4.13: Close or suspend production company by Founder

    @Test
    void GivenActiveCompanyAndFounder_WhenCloseProductionCompany_ThenCompanyBecomesInactive() throws Exception {
        // Arrange
        CompanyDTO company = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);

        // Act
        CompanyDTO closedCompany = companyService.closeProductionCompany(founderToken, company.getId());

        // Assert
        assertNotNull(closedCompany, "Closed company DTO should not be null.");
        assertFalse(closedCompany.isActive(), "Company should become inactive after founder closes it.");
    }

    @Test
    void GivenActiveCompanyAndNonFounder_WhenCloseProductionCompany_ThenThrowsException() throws Exception {
        // Arrange
        CompanyDTO company = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);

        // Act + Assert
        assertThrows(Exception.class, () ->
                companyService.closeProductionCompany(nonFounderToken, company.getId())
        );
    }

    @Test
    void GivenInactiveCompanyAndFounder_WhenCloseProductionCompanyAgain_ThenThrowsException() throws Exception {
        // Arrange
        CompanyDTO company = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);
        companyService.closeProductionCompany(founderToken, company.getId());

        // Act + Assert
        assertThrows(Exception.class, () ->
                companyService.closeProductionCompany(founderToken, company.getId())
        );
    }

    // UC 4.14: Reopen production company by Founder

    @Test
    void GivenInactiveCompanyAndFounder_WhenReopenProductionCompany_ThenCompanyBecomesActive() throws Exception {
        // Arrange
        CompanyDTO company = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);
        companyService.closeProductionCompany(founderToken, company.getId());

        // Act
        CompanyDTO reopenedCompany = companyService.reopenProductionCompany(founderToken, company.getId());

        // Assert
        assertNotNull(reopenedCompany, "Reopened company DTO should not be null.");
        assertTrue(reopenedCompany.isActive(), "Company should become active again after founder reopens it.");
    }

    @Test
    void GivenInactiveCompanyAndNonFounder_WhenReopenProductionCompany_ThenThrowsException() throws Exception {
        // Arrange
        CompanyDTO company = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);
        companyService.closeProductionCompany(founderToken, company.getId());

        // Act + Assert
        assertThrows(Exception.class, () ->
                companyService.reopenProductionCompany(nonFounderToken, company.getId())
        );
    }

    @Test
    void GivenActiveCompanyAndFounder_WhenReopenProductionCompany_ThenThrowsException() throws Exception {
        // Arrange
        CompanyDTO company = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);

        // Act + Assert
        assertThrows(Exception.class, () ->
                companyService.reopenProductionCompany(founderToken, company.getId())
        );
    }

    // UC 4.15: View roles and permissions tree
    // Keep these tests commented out until viewRolesAndPermissionsTree is available
    // in this branch's CompanyService API.

    // @Test
    // void GivenCompanyAndFounder_WhenViewRolesAndPermissionsTree_ThenReturnsTreeWithFounder() throws Exception {
    //     // Arrange
    //     CompanyDTO company = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);
    //
    //     // Act
    //     String tree = companyService.viewRolesAndPermissionsTree(founderToken, company.getId());
    //
    //     // Assert
    //     assertNotNull(tree, "Roles and permissions tree should not be null.");
    //     assertTrue(tree.contains("FOUNDER"), "Roles tree should include the founder role.");
    //     assertTrue(tree.contains(String.valueOf(tokenService.extractUserId(founderToken)),
    //             "Roles tree should include the founder member id.");
    // }

    // @Test
    // void GivenCompanyAndNonOwner_WhenViewRolesAndPermissionsTree_ThenThrowsException() throws Exception {
    //     // Arrange
    //     CompanyDTO company = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);
    //
    //     // Act + Assert
    //     assertThrows(Exception.class, () ->
    //             companyService.viewRolesAndPermissionsTree(nonFounderToken, company.getId())
    //     );
    // }
}