package ticketsystem.AcceptanceTesting;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.DomainLayer.discount.ConditionalDiscount;
import ticketsystem.DomainLayer.discount.CouponDiscount;
import ticketsystem.DomainLayer.discount.VisibleDiscount;
import ticketsystem.DomainLayer.discount.ConditionalDiscount.Condition;
import ticketsystem.DomainLayer.discount.DiscountCompositionType;
import ticketsystem.DomainLayer.discount.DiscountKind;
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
import ticketsystem.DomainLayer.company.Company;
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
    private ICompanyRepository companyRepository;
    


    private static final String VALID_COMPANY_NAME = "BGU Productions";

    @BeforeEach
    void setUp() throws Exception {
        companyRepository = new CompanyRepository();
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

        boolean signedUp = userService.signUp(guestToken, username, password, "Test User", "0500000000");
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

    // UC 4.3: Add discount policy to company

    @Test
    void GivenFounder_WhenAddVisibleDiscountToCompany_ThenDiscountIsAddedToCompany() throws Exception {
        CompanyDTO companyDTO = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);

        companyService.addVisibleDiscountToCompany(
                founderToken,
                companyDTO.getId(),
                "Visible Discount",
                new BigDecimal(10)
        );

        Company company = companyRepository.findById(companyDTO.getId())
                .orElseThrow(() -> new Exception("Company not found in test"));

        assertEquals(1, company.getDiscountPolicy().getDiscounts().size());
        assertTrue(company.getDiscountPolicy().getDiscounts().get(0) instanceof VisibleDiscount);
    }

    @Test
    void GivenFounder_WhenAddConditionalDiscountToCompany_ThenDiscountIsAddedToCompany() throws Exception {
        CompanyDTO companyDTO = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);

        companyService.addConditionalDiscountToCompany(
                founderToken,
                companyDTO.getId(),
                "Conditional Discount",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(7),
                new BigDecimal(15),
                Condition.MIN_TICKET,
                2
        );

        Company company = companyRepository.findById(companyDTO.getId())
                .orElseThrow(() -> new Exception("Company not found in test"));

        assertEquals(1, company.getDiscountPolicy().getDiscounts().size());
        assertTrue(company.getDiscountPolicy().getDiscounts().get(0) instanceof ConditionalDiscount);
    }

    @Test
    void GivenFounder_WhenAddCouponDiscountToCompany_ThenDiscountIsAddedToCompany() throws Exception {
        CompanyDTO companyDTO = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);

        companyService.addCouponDiscountToCompany(
                founderToken,
                companyDTO.getId(),
                "Coupon Discount",
                "BGU10",
                new BigDecimal(10),
                LocalDateTime.now().plusDays(7)
        );

        Company company = companyRepository.findById(companyDTO.getId())
                .orElseThrow(() -> new Exception("Company not found in test"));

        assertEquals(1, company.getDiscountPolicy().getDiscounts().size());
        assertTrue(company.getDiscountPolicy().getDiscounts().get(0) instanceof CouponDiscount);
    }

   @Test
    void GivenUserWithoutDiscountPermission_WhenAddVisibleDiscountToCompany_ThenThrowsException() throws Exception {
        CompanyDTO companyDTO = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);

        assertThrows(IllegalArgumentException.class, () ->
                companyService.addVisibleDiscountToCompany(
                        nonFounderToken,
                        companyDTO.getId(),
                        "Visible Discount",
                        new BigDecimal(10)
                )
        );
    }
    @Test
    void GivenUserWithoutDiscountPermission_WhenRemoveDiscountFromCompany_ThenThrowsException() throws Exception {
        CompanyDTO companyDTO = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);

        companyService.addVisibleDiscountToCompany(
                founderToken,
                companyDTO.getId(),
                "Visible Discount",
                new BigDecimal(10)
        );

        Company company = companyRepository.findById(companyDTO.getId())
                .orElseThrow(() -> new Exception("Company not found in test"));

        Long discountId = company.getDiscountPolicy()
                .getDiscounts()
                .get(0)
                .getDiscountId();

        assertThrows(IllegalArgumentException.class, () ->
                companyService.removeDiscountFromCompany(nonFounderToken, companyDTO.getId(), discountId)
        );
    }

    @Test
    void GivenFounderAndNonExistingCompany_WhenAddVisibleDiscountToCompany_ThenThrowsException() {
        assertThrows(Exception.class, () ->
                companyService.addVisibleDiscountToCompany(
                        founderToken,
                        999999L,
                        "Visible Discount",
                        new BigDecimal(10)
                )
        );
    }

    @Test
    void GivenFounderAndNullDiscountName_WhenAddVisibleDiscountToCompany_ThenThrowsException() throws Exception {
        CompanyDTO companyDTO = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);

        assertThrows(Exception.class, () ->
                companyService.addVisibleDiscountToCompany(
                        founderToken,
                        companyDTO.getId(),
                        null,
                        new BigDecimal(10)
                )
        );
    }

    @Test
    void GivenFounderAndNonExistingCompany_WhenRemoveDiscountFromCompany_ThenThrowsException() {
        // Act + Assert
        assertThrows(Exception.class, () ->
                companyService.removeDiscountFromCompany(founderToken, 999999L, 1L)
        );
    }
    @Test
    void GivenFounderAndNonExistingDiscount_WhenRemoveDiscountFromCompany_ThenThrowsException() throws Exception {
        // Arrange
        CompanyDTO companyDTO = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);

        // Act + Assert
        assertThrows(Exception.class, () ->
                companyService.removeDiscountFromCompany(founderToken, companyDTO.getId(), 999999L)
        );
    }    
}