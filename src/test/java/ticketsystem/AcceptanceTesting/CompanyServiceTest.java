package ticketsystem.AcceptanceTesting;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
import ticketsystem.ApplicationLayer.UserAccessService;
import ticketsystem.ApplicationLayer.UserService;
import ticketsystem.DTO.CompanyDTO;
import ticketsystem.DTO.PurchasePolicyDTO;
import ticketsystem.DTO.PurchaseRuleType;
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

import ticketsystem.DTO.PurchaseRuleDTO;

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;

import ticketsystem.ApplicationLayer.INotifier;


public class CompanyServiceTest {

    private CompanyService companyService;
    private UserService userService;
    private ITokenService tokenService;
    private ISystemLogger testLogger;
    private FakeNotifier fakeNotifier;
    private String founderToken;
    private String nonFounderToken;
    private IUserRepository userRepository;
    private MembershipDomainService membershipDomain;
    private ICompanyRepository companyRepository;
    private  UserAccessService userAccessService;

    


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
        fakeNotifier = new FakeNotifier();
        userAccessService=new UserAccessService(userRepository);
        companyService = new CompanyService(companyRepository, tokenService, membershipDomain, testLogger,userAccessService,fakeNotifier);

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
    
    // UC 4.3: Set company purchase policy
    @Test
        void GivenFounder_WhenSetCompanyPurchasePolicyWithMaxTicketsRule_ThenPolicyIsSavedOnCompany() throws Exception {
        CompanyDTO companyDTO = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);

        PurchasePolicyDTO policyDTO = maxTicketsPolicyDTO(5);

        companyService.setCompanyPurchasePolicy(
                founderToken,
                companyDTO.getId(),
                policyDTO
        );

        Company company = companyRepository.findById(companyDTO.getId())
                .orElseThrow(() -> new Exception("Company not found in test"));

        assertDoesNotThrow(() -> company.canPurchase(5, 20));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> company.canPurchase(6, 20)
        );

        assertEquals("Cannot purchase more than 5 tickets.", exception.getMessage());
        }

        @Test
        void GivenFounder_WhenSetCompanyPurchasePolicyWithMinAgeRule_ThenPolicyIsSavedOnCompany() throws Exception {
        CompanyDTO companyDTO = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);

        PurchasePolicyDTO policyDTO = minAgePolicyDTO(18);

        companyService.setCompanyPurchasePolicy(
                founderToken,
                companyDTO.getId(),
                policyDTO
        );

        Company company = companyRepository.findById(companyDTO.getId())
                .orElseThrow(() -> new Exception("Company not found in test"));

        assertDoesNotThrow(() -> company.canPurchase(1, 18));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> company.canPurchase(1, 17)
        );

        assertEquals(
                "Customer does not meet the minimum age requirement of 18",
                exception.getMessage()
        );
        }
    @Test
    void GivenFounder_WhenSetCompanyPurchasePolicyWithNestedRule_ThenNestedPolicyIsSavedOnCompany() throws Exception {
        CompanyDTO companyDTO = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);

        PurchasePolicyDTO policyDTO = new PurchasePolicyDTO(
                new PurchaseRuleDTO(
                        PurchaseRuleType.AND,
                        null,
                        List.of(
                                new PurchaseRuleDTO(PurchaseRuleType.MIN_AGE, 18, null),
                                new PurchaseRuleDTO(
                                        PurchaseRuleType.OR,
                                        null,
                                        List.of(
                                                new PurchaseRuleDTO(PurchaseRuleType.MAX_TICKETS, 2, null),
                                                new PurchaseRuleDTO(PurchaseRuleType.MIN_TICKETS, 100, null)
                                        )
                                )
                        )
                )
        );

        companyService.setCompanyPurchasePolicy(
                founderToken,
                companyDTO.getId(),
                policyDTO
        );

        Company company = companyRepository.findById(companyDTO.getId())
                .orElseThrow(() -> new Exception("Company not found in test"));

        assertDoesNotThrow(() -> company.canPurchase(2, 18));
        assertDoesNotThrow(() -> company.canPurchase(100, 18));

        assertThrows(
                IllegalArgumentException.class,
                () -> company.canPurchase(50, 18)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> company.canPurchase(2, 17)
        );
    }
        @Test
        void GivenUserWithoutPurchasePolicyPermission_WhenSetCompanyPurchasePolicy_ThenThrowsException() throws Exception {
        CompanyDTO companyDTO = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);

        PurchasePolicyDTO policyDTO = maxTicketsPolicyDTO(5);

        assertThrows(
                IllegalArgumentException.class,
                () -> companyService.setCompanyPurchasePolicy(
                        nonFounderToken,
                        companyDTO.getId(),
                        policyDTO
                )
        );
        }

        @Test
        void GivenFounderAndNonExistingCompany_WhenSetCompanyPurchasePolicy_ThenThrowsException() {
        PurchasePolicyDTO policyDTO = maxTicketsPolicyDTO(5);

        assertThrows(
                Exception.class,
                () -> companyService.setCompanyPurchasePolicy(
                        founderToken,
                        999999L,
                        policyDTO
                        )
        );
        }

        @Test
        void GivenFounderAndInvalidPurchasePolicyDTO_WhenSetCompanyPurchasePolicy_ThenThrowsException() throws Exception {
        CompanyDTO companyDTO = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);

        PurchasePolicyDTO invalidPolicyDTO = new PurchasePolicyDTO(
                new PurchaseRuleDTO(PurchaseRuleType.MIN_AGE, null, null)
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> companyService.setCompanyPurchasePolicy(
                        founderToken,
                        companyDTO.getId(),
                        invalidPolicyDTO
                )
        );

        assertEquals("Minimum age is required", exception.getMessage());
        }

        private PurchasePolicyDTO maxTicketsPolicyDTO(int maxTickets) {
        return new PurchasePolicyDTO(
                new PurchaseRuleDTO(PurchaseRuleType.MAX_TICKETS, maxTickets, null)
        );
        }

        private PurchasePolicyDTO minAgePolicyDTO(int minAge) {
        return new PurchasePolicyDTO(
                new PurchaseRuleDTO(PurchaseRuleType.MIN_AGE, minAge, null)
        );
        }
        private static class FakeNotifier implements INotifier {

            private final List<String> messages = new ArrayList<>();

            @Override
            public void notifyMember(Long memberId, String message) {
                messages.add(message);
            }

            @Override
            public void notifyGuest(String guestToken, String message) {
                messages.add(message);
            }

            @Override
            public void notifyMembers(Collection<Long> memberIds, String message) {
                if (memberIds == null) {
                    return;
                }

                for (Long memberId : memberIds) {
                    if (memberId != null) {
                        notifyMember(memberId, message);
                    }
                }
            }

            @Override
            public void notifyGuests(Collection<String> guestTokens, String message) {
                if (guestTokens == null) {
                    return;
                }

                for (String guestToken : guestTokens) {
                    if (guestToken != null && !guestToken.isBlank()) {
                        notifyGuest(guestToken, message);
                    }
                }
            }

            boolean containsMessage(String text) {
                return messages.stream()
                        .anyMatch(message -> message.contains(text));
            }
        }

        }