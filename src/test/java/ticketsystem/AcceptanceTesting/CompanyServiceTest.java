package ticketsystem.AcceptanceTesting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.ApplicationLayer.CompanyService;
import ticketsystem.ApplicationLayer.INotifier;
import ticketsystem.ApplicationLayer.ISystemLogger;
import ticketsystem.ApplicationLayer.ITokenService;
import ticketsystem.ApplicationLayer.TokenService;
import ticketsystem.ApplicationLayer.UserAccessService;
import ticketsystem.ApplicationLayer.UserService;
import ticketsystem.DTO.CompanyDTO;
import ticketsystem.DTO.DiscountDTO;
import ticketsystem.DTO.DiscountPolicyDTO;
import ticketsystem.DTO.PurchasePolicyDTO;
import ticketsystem.DTO.PurchaseRuleDTO;
import ticketsystem.DTO.PurchaseRuleType;
import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.IRepository.INotificationsRepository;
import ticketsystem.DomainLayer.IRepository.ITokenRepository;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.DomainLayer.MembershipDomainService;
import ticketsystem.DomainLayer.company.Company;
import ticketsystem.DomainLayer.discount.ConditionalDiscount;
import ticketsystem.DomainLayer.discount.CouponDiscount;
import ticketsystem.DomainLayer.discount.DiscountCompositionType;
import ticketsystem.DomainLayer.discount.VisibleDiscount;
import ticketsystem.DomainLayer.user.CompanyRole;
import ticketsystem.DomainLayer.user.Founder;
import ticketsystem.DomainLayer.user.RoleStatus;
import ticketsystem.InfrastructureLayer.CompanyRepository;
import ticketsystem.InfrastructureLayer.LogbackSystemLogger;
import ticketsystem.InfrastructureLayer.NotificationsRepository;
import ticketsystem.InfrastructureLayer.TokenRepository;
import ticketsystem.InfrastructureLayer.UserRepository;
import ticketsystem.InfrastructureLayer.VaadinNotifier;

public class CompanyServiceTest {

    private CompanyService companyService;
    private UserService userService;
    private ITokenService tokenService;
    private ISystemLogger testLogger;
    private INotifier Notifier;
    private String founderToken;
    private String nonFounderToken;
    private IUserRepository userRepository;
    private MembershipDomainService membershipDomain;
    private ICompanyRepository companyRepository;
    private UserAccessService userAccessService;
    private INotificationsRepository notificationRepository;

    private static final String VALID_COMPANY_NAME = "BGU Productions";

    @BeforeEach
    void setUp() throws Exception {
        companyRepository = new CompanyRepository();
        userRepository = new UserRepository();
        ITokenRepository tokenRepository = new TokenRepository();
        testLogger = new LogbackSystemLogger();

        tokenService = new TokenService(
                "default_secret_key_for_development_purposes_only_32_chars",
                tokenRepository, testLogger);
        userService = new UserService(userRepository, tokenService, testLogger);
        membershipDomain = new MembershipDomainService(userRepository);
        userService = new UserService(userRepository, tokenService, testLogger);
        notificationRepository = new NotificationsRepository();
        Notifier = new VaadinNotifier(notificationRepository);
        userAccessService = new UserAccessService(userRepository);
        companyService = new CompanyService(companyRepository, tokenService, membershipDomain, testLogger,
                userAccessService, Notifier);

        founderToken = createLoggedInMember("noa_user", "password123");
        nonFounderToken = createLoggedInMember("other_user", "password123");
    }

    private String createLoggedInMember(String username, String password) {
        String guestToken = userService.visitSystem();

        boolean signedUp = userService.signUp(guestToken, username, password, "Test User", "0500000000", LocalDate.of(2001, 1, 1));
        assertTrue(signedUp, "Member signup should succeed during test setup.");

        String memberToken = userService.login(guestToken, username, password);
        assertNotNull(memberToken, "Login should return a member session token.");

        return memberToken;
    }

    // UC 3.2: Create a production company
    @Test
    void GivenLoggedInMember_WhenCreateProductionCompany_ThenCompanyCreatedActiveAndMemberIsFounder()
            throws Exception {
        // Act
        CompanyDTO company = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);

        // Assert
        assertNotNull(company, "Created company DTO should not be null.");
        assertEquals(VALID_COMPANY_NAME, company.getName(), "Company name should match the requested name.");
        assertTrue(company.isActive(), "New production company should be active.");

        Long founderId = tokenService.extractUserId(founderToken);
        assertEquals(founderId, company.getFounderId(),
                "The logged-in member should become the company founder.");
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
        assertThrows(Exception.class,
                () -> companyService.createProductionCompany(guestToken, "Guest Company"));
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
        assertThrows(Exception.class,
                () -> companyService.closeProductionCompany(nonFounderToken, company.getId()));
    }

    @Test
    void GivenInactiveCompanyAndFounder_WhenCloseProductionCompanyAgain_ThenThrowsException() throws Exception {
        // Arrange
        CompanyDTO company = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);
        companyService.closeProductionCompany(founderToken, company.getId());

        // Act + Assert
        assertThrows(Exception.class,
                () -> companyService.closeProductionCompany(founderToken, company.getId()));
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
        assertThrows(Exception.class,
                () -> companyService.reopenProductionCompany(nonFounderToken, company.getId()));
    }

    @Test
    void GivenActiveCompanyAndFounder_WhenReopenProductionCompany_ThenThrowsException() throws Exception {
        // Arrange
        CompanyDTO company = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);

        // Act + Assert
        assertThrows(Exception.class,
                () -> companyService.reopenProductionCompany(founderToken, company.getId()));
    }

    // UC 4.3: Set company purchase policy
    @Test
    void GivenFounder_WhenSetCompanyPurchasePolicyWithMaxTicketsRule_ThenPolicyIsSavedOnCompany() throws Exception {
        CompanyDTO companyDTO = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);

        PurchasePolicyDTO policyDTO = maxTicketsPolicyDTO(5);

        companyService.setCompanyPurchasePolicy(
                founderToken,
                companyDTO.getId(),
                policyDTO);

        Company company = companyRepository.findById(companyDTO.getId())
                .orElseThrow(() -> new Exception("Company not found in test"));

        assertDoesNotThrow(() -> company.canPurchase(5, 20));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> company.canPurchase(6, 20));

        assertEquals("Cannot purchase more than 5 tickets.", exception.getMessage());
    }

    @Test
    void GivenFounder_WhenSetCompanyPurchasePolicyWithMinAgeRule_ThenPolicyIsSavedOnCompany() throws Exception {
        CompanyDTO companyDTO = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);

        PurchasePolicyDTO policyDTO = minAgePolicyDTO(18);

        companyService.setCompanyPurchasePolicy(
                founderToken,
                companyDTO.getId(),
                policyDTO);

        Company company = companyRepository.findById(companyDTO.getId())
                .orElseThrow(() -> new Exception("Company not found in test"));

        assertDoesNotThrow(() -> company.canPurchase(1, 18));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> company.canPurchase(1, 17));

        assertEquals(
                "Customer does not meet the minimum age requirement of 18",
                exception.getMessage());
    }

    @Test
    void GivenFounder_WhenSetCompanyPurchasePolicyWithNestedRule_ThenNestedPolicyIsSavedOnCompany()
            throws Exception {
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
                                                new PurchaseRuleDTO(
                                                        PurchaseRuleType.MAX_TICKETS,
                                                        2,
                                                        null),
                                                new PurchaseRuleDTO(
                                                        PurchaseRuleType.MIN_TICKETS,
                                                        100,
                                                        null))))));

        companyService.setCompanyPurchasePolicy(
                founderToken,
                companyDTO.getId(),
                policyDTO);

        Company company = companyRepository.findById(companyDTO.getId())
                .orElseThrow(() -> new Exception("Company not found in test"));

        assertDoesNotThrow(() -> company.canPurchase(2, 18));
        assertDoesNotThrow(() -> company.canPurchase(100, 18));

        assertThrows(
                IllegalArgumentException.class,
                () -> company.canPurchase(50, 18));

        assertThrows(
                IllegalArgumentException.class,
                () -> company.canPurchase(2, 17));
    }

    @Test
    void GivenUserWithoutPurchasePolicyPermission_WhenSetCompanyPurchasePolicy_ThenThrowsException()
            throws Exception {
        CompanyDTO companyDTO = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);

        PurchasePolicyDTO policyDTO = maxTicketsPolicyDTO(5);

        assertThrows(
                IllegalArgumentException.class,
                () -> companyService.setCompanyPurchasePolicy(
                        nonFounderToken,
                        companyDTO.getId(),
                        policyDTO));
    }

    @Test
    void GivenFounderAndNonExistingCompany_WhenSetCompanyPurchasePolicy_ThenThrowsException() {
        PurchasePolicyDTO policyDTO = maxTicketsPolicyDTO(5);

        assertThrows(
                Exception.class,
                () -> companyService.setCompanyPurchasePolicy(
                        founderToken,
                        999999L,
                        policyDTO));
    }

    @Test
    void GivenFounderAndInvalidPurchasePolicyDTO_WhenSetCompanyPurchasePolicy_ThenThrowsException()
            throws Exception {
        CompanyDTO companyDTO = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);

        PurchasePolicyDTO invalidPolicyDTO = new PurchasePolicyDTO(
                new PurchaseRuleDTO(PurchaseRuleType.MIN_AGE, null, null));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> companyService.setCompanyPurchasePolicy(
                        founderToken,
                        companyDTO.getId(),
                        invalidPolicyDTO));

        assertEquals("Minimum age is required", exception.getMessage());
    }

    @Test
    void GivenActiveCompanyAndGuest_WhenGetCompanyDetails_ThenReturnsCompanyDTO() throws Exception {
        // Arrange
        CompanyDTO companyDTO = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);
        String guestToken = userService.visitSystem();

        // Act
        CompanyDTO result = companyService.getCompanyDetails(guestToken, companyDTO.getId());

        // Assert
        assertNotNull(result);
        assertEquals(companyDTO.getId(), result.getId());
        assertEquals(VALID_COMPANY_NAME, result.getName());
    }

    @Test
    void GivenInvalidToken_WhenGetCompanyDetails_ThenThrowsException() {
        // Act & Assert
        Exception exception = assertThrows(Exception.class,
                () -> companyService.getCompanyDetails("invalid-token", 1L));

        assertTrue(exception.getMessage().contains("Invalid")
                || exception.getMessage().contains("expired"));
    }

    @Test
    void GivenValidTokenAndMissingCompany_WhenGetCompanyDetails_ThenThrowsException() {
        // Act & Assert
        Exception exception = assertThrows(Exception.class,
                () -> companyService.getCompanyDetails(founderToken, 999999L));

        assertEquals("Error: Company not found.", exception.getMessage());
    }

    @Test
    void GivenInactiveCompanyAndGuest_WhenGetCompanyDetails_ThenThrowsException() throws Exception {
        // Arrange
        CompanyDTO companyDTO = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);
        companyService.closeProductionCompany(founderToken, companyDTO.getId());

        String guestToken = userService.visitSystem();

        // Act & Assert
        Exception exception = assertThrows(Exception.class,
                () -> companyService.getCompanyDetails(guestToken, companyDTO.getId()));

        assertEquals("Error: User does not have permission to view this company.", exception.getMessage());
    }

    @Test
    void GivenInactiveCompanyAndFounder_WhenGetCompanyDetails_ThenReturnsCompanyDTO() throws Exception {
        // Arrange
        CompanyDTO companyDTO = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);
        companyService.closeProductionCompany(founderToken, companyDTO.getId());

        // Act
        CompanyDTO result = companyService.getCompanyDetails(founderToken, companyDTO.getId());

        // Assert
        assertNotNull(result);
        assertEquals(companyDTO.getId(), result.getId());
    }

    @Test
    void GivenInactiveCompanyAndRegularMember_WhenGetCompanyDetails_ThenThrowsException() throws Exception {
        // Arrange
        CompanyDTO companyDTO = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);
        companyService.closeProductionCompany(founderToken, companyDTO.getId());

        // Act & Assert
        Exception exception = assertThrows(Exception.class,
                () -> companyService.getCompanyDetails(nonFounderToken, companyDTO.getId()));

        assertEquals("Error: User does not have permission to view this company.", exception.getMessage());
    }

    @Test
    void GivenFounder_WhenSetCompositionTypeToSum_ThenCompositionTypeIsUpdated() throws Exception {
        // Arrange
        CompanyDTO companyDTO = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);

        // Act
        companyService.setCompositionType(
                founderToken,
                companyDTO.getId(),
                DiscountCompositionType.SUM);

        // Assert
        Company company = companyRepository.findById(companyDTO.getId())
                .orElseThrow(() -> new Exception("Company not found in test"));

        assertEquals(DiscountCompositionType.SUM,
                company.getDiscountPolicy().getDiscountCompositionType());
    }

    @Test
    void GivenUserWithoutDiscountPermission_WhenSetCompositionType_ThenThrowsException() throws Exception {
        // Arrange
        CompanyDTO companyDTO = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> companyService.setCompositionType(
                nonFounderToken,
                companyDTO.getId(),
                DiscountCompositionType.SUM));
    }

    @Test
    void GivenFounderAndNonExistingCompany_WhenSetCompositionType_ThenThrowsException() {
        // Act & Assert
        assertThrows(Exception.class, () -> companyService.setCompositionType(
                founderToken,
                999999L,
                DiscountCompositionType.SUM));
    }

    @Test
    void GivenFounderWithCompany_WhenGetFirstManagedCompanyId_ThenReturnsCreatedCompanyId() throws Exception {
        CompanyDTO companyDTO = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);

        Long managedCompanyId = companyService.getFirstManagedCompanyId(founderToken);

        assertNotNull(managedCompanyId);
        assertEquals(companyDTO.getId(), managedCompanyId);
    }

    @Test
    void GivenMemberWithoutManagedCompany_WhenGetFirstManagedCompanyId_ThenReturnsNull() throws Exception {
        Long managedCompanyId = companyService.getFirstManagedCompanyId(nonFounderToken);

        assertEquals(null, managedCompanyId);
    }

    @Test
    void GivenFounder_WhenHasPermissionForDiscountPolicy_ThenReturnsTrue() throws Exception {
        CompanyDTO companyDTO = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);

        boolean result = companyService.hasPermission(
                founderToken,
                companyDTO.getId(),
                ticketsystem.DomainLayer.user.Permission.SET_DISCOUNT_POLICY
        );

        assertTrue(result);
    }

    @Test
    void GivenRegularMember_WhenHasPermissionForDiscountPolicy_ThenReturnsFalse() throws Exception {
        CompanyDTO companyDTO = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);

        boolean result = companyService.hasPermission(
                nonFounderToken,
                companyDTO.getId(),
                ticketsystem.DomainLayer.user.Permission.SET_DISCOUNT_POLICY
        );

        assertFalse(result);
    }

    @Test
    void GivenFounder_WhenHasPermissionWithNullPermission_ThenThrowsException() throws Exception {
        CompanyDTO companyDTO = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> companyService.hasPermission(founderToken, companyDTO.getId(), null)
        );

        assertEquals("Permission cannot be null", exception.getMessage());
    }

    @Test
    void GivenCompanyWithoutPurchasePolicyChange_WhenGetCompanyPurchasePolicy_ThenReturnsDefaultPolicyDTO() throws Exception {
        CompanyDTO companyDTO = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);

        PurchasePolicyDTO result = companyService.getCompanyPurchasePolicy(founderToken, companyDTO.getId());

        assertNotNull(result);
        assertNotNull(result.getRootRule());
        assertEquals(PurchaseRuleType.ALWAYS_ALLOW, result.getRootRule().getType());
    }

    @Test
    void GivenCompanyWithMaxTicketsPolicy_WhenGetCompanyPurchasePolicy_ThenReturnsSavedPolicyDTO() throws Exception {
        CompanyDTO companyDTO = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);

        companyService.setCompanyPurchasePolicy(
                founderToken,
                companyDTO.getId(),
                maxTicketsPolicyDTO(4)
        );

        PurchasePolicyDTO result = companyService.getCompanyPurchasePolicy(founderToken, companyDTO.getId());

        assertNotNull(result);
        assertNotNull(result.getRootRule());
        assertEquals(PurchaseRuleType.MAX_TICKETS, result.getRootRule().getType());
        assertEquals(4, result.getRootRule().getValue());
    }

    @Test
    void GivenMissingCompany_WhenGetCompanyPurchasePolicy_ThenThrowsGenericRetrievalException() {
        Exception exception = assertThrows(
                Exception.class,
                () -> companyService.getCompanyPurchasePolicy(founderToken, 999999L)
        );

        assertEquals("An error occurred while retrieving the purchase policy.", exception.getMessage());
    }

    @Test
    void GivenCompanyWithoutDiscounts_WhenGetCompanyDiscountPolicy_ThenReturnsEmptyDiscountPolicyDTO() throws Exception {
        CompanyDTO companyDTO = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);

        DiscountPolicyDTO result = companyService.getCompanyDiscountPolicy(founderToken, companyDTO.getId());

        assertNotNull(result);
        assertEquals(DiscountCompositionType.MAX, result.getCompositionType());
        assertNotNull(result.getDiscounts());
        assertTrue(result.getDiscounts().isEmpty());
    }

    @Test
    void GivenUserWithoutDiscountPermission_WhenSetCompanyDiscountPolicy_ThenThrowsException() throws Exception {
        CompanyDTO companyDTO = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);

        DiscountDTO visible = new DiscountDTO();
        visible.setType("VISIBLE");
        visible.setName("Visible Discount");
        visible.setPercentage(BigDecimal.valueOf(10));

        DiscountPolicyDTO policyDTO = new DiscountPolicyDTO();
        policyDTO.setCompositionType(DiscountCompositionType.MAX);
        policyDTO.setDiscounts(List.of(visible));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> companyService.setCompanyDiscountPolicy(nonFounderToken, companyDTO.getId(), policyDTO)
        );

        assertEquals("User does not have permission to manage company discount policy", exception.getMessage());
    }

    @Test
    void GivenMissingCompany_WhenSetCompanyDiscountPolicy_ThenThrowsException() {
        DiscountPolicyDTO policyDTO = new DiscountPolicyDTO();
        policyDTO.setCompositionType(DiscountCompositionType.MAX);
        policyDTO.setDiscounts(List.of());

        assertThrows(
                Exception.class,
                () -> companyService.setCompanyDiscountPolicy(founderToken, 999999L, policyDTO)
        );
    }

    @Test
    void GivenMissingCompany_WhenGetPurchasePolicySummary_ThenReturnsNoPurchasePolicyMessage() {
        String summary = companyService.getPurchasePolicySummary(999999L);

        assertEquals("לא הוגדרה מדיניות רכישה", summary);
    }

    @Test
    void GivenCompanyWithDefaultPurchasePolicy_WhenGetPurchasePolicySummary_ThenReturnsNoRestrictionsMessage() throws Exception {
        CompanyDTO companyDTO = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);

        String summary = companyService.getPurchasePolicySummary(companyDTO.getId());

        assertEquals("ללא הגבלות רכישה מיוחדות", summary);
    }

    @Test
    void GivenCompanyWithCustomPurchasePolicy_WhenGetPurchasePolicySummary_ThenReturnsCustomPolicyMessage() throws Exception {
        CompanyDTO companyDTO = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);

        companyService.setCompanyPurchasePolicy(
                founderToken,
                companyDTO.getId(),
                maxTicketsPolicyDTO(5)
        );

        String summary = companyService.getPurchasePolicySummary(companyDTO.getId());

        assertEquals("מוגדרת חוקיות רכישה מותאמת אישית", summary);
    }

    @Test
    void GivenMissingCompany_WhenGetDiscountPolicySummary_ThenReturnsNoDiscountsMessage() {
        String summary = companyService.getDiscountPolicySummary(999999L);

        assertEquals("אין הנחות פעילות", summary);
    }

    @Test
    void GivenCompanyWithoutDiscounts_WhenGetDiscountPolicySummary_ThenReturnsNoDiscountsMessage() throws Exception {
        CompanyDTO companyDTO = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);

        String summary = companyService.getDiscountPolicySummary(companyDTO.getId());

        assertEquals("אין הנחות פעילות", summary);
    }

    @Test
    void GivenCompaniesExist_WhenGetAllCompanies_ThenReturnsAllCompanyDTOs() throws Exception {
        CompanyDTO firstCompany = companyService.createProductionCompany(founderToken, "First Company");
        CompanyDTO secondCompany = companyService.createProductionCompany(founderToken, "Second Company");

        List<CompanyDTO> companies = companyService.getAllCompanies();

        assertNotNull(companies);
        assertTrue(companies.stream().anyMatch(company -> company.getId() == firstCompany.getId()));
        assertTrue(companies.stream().anyMatch(company -> company.getId() == secondCompany.getId()));
    }

    @Test
    void GivenFounder_WhenSetCompanyDiscountPolicyWithVisibleDiscount_ThenPolicyIsSavedOnCompany() throws Exception {
        CompanyDTO companyDTO = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);

        DiscountDTO visible = new DiscountDTO();
        visible.setType("VISIBLE");
        visible.setName("Visible Discount");
        visible.setPercentage(BigDecimal.valueOf(10));

        DiscountPolicyDTO policyDTO = new DiscountPolicyDTO();
        policyDTO.setCompositionType(DiscountCompositionType.MAX);
        policyDTO.setDiscounts(List.of(visible));

        companyService.setCompanyDiscountPolicy(founderToken, companyDTO.getId(), policyDTO);

        Company company = companyRepository.findById(companyDTO.getId())
                .orElseThrow(() -> new Exception("Company not found in test"));

        assertEquals(1, company.getDiscountPolicy().getDiscounts().size());
        assertTrue(company.getDiscountPolicy().getDiscounts().get(0) instanceof VisibleDiscount);
    }

    @Test
    void GivenFounder_WhenSetCompanyDiscountPolicyWithCouponDiscount_ThenPolicyIsSavedOnCompany() throws Exception {
        CompanyDTO companyDTO = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);

        DiscountDTO coupon = new DiscountDTO();
        coupon.setType("COUPON");
        coupon.setName("Coupon Discount");
        coupon.setCouponCode("SAVE10");
        coupon.setPercentage(BigDecimal.valueOf(10));
        coupon.setEndTime(LocalDateTime.now().plusDays(1));

        DiscountPolicyDTO policyDTO = new DiscountPolicyDTO();
        policyDTO.setCompositionType(DiscountCompositionType.MAX);
        policyDTO.setDiscounts(List.of(coupon));

        companyService.setCompanyDiscountPolicy(founderToken, companyDTO.getId(), policyDTO);

        Company company = companyRepository.findById(companyDTO.getId())
                .orElseThrow(() -> new Exception("Company not found in test"));

        assertEquals(1, company.getDiscountPolicy().getDiscounts().size());
        assertTrue(company.getDiscountPolicy().getDiscounts().get(0) instanceof CouponDiscount);
    }

    @Test
    void GivenFounder_WhenSetCompanyDiscountPolicyWithConditionalDiscount_ThenPolicyIsSavedOnCompany() throws Exception {
        CompanyDTO companyDTO = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);

        DiscountDTO conditional = new DiscountDTO();
        conditional.setType("CONDITIONAL");
        conditional.setName("Min Tickets Discount");
        conditional.setPercentage(BigDecimal.valueOf(15));
        conditional.setConditions(List.of(
                new ticketsystem.DTO.DiscountConditionDTO("MIN_TICKET", 3, null, null)
        ));

        DiscountPolicyDTO policyDTO = new DiscountPolicyDTO();
        policyDTO.setCompositionType(DiscountCompositionType.SUM);
        policyDTO.setDiscounts(List.of(conditional));

        companyService.setCompanyDiscountPolicy(founderToken, companyDTO.getId(), policyDTO);

        Company company = companyRepository.findById(companyDTO.getId())
                .orElseThrow(() -> new Exception("Company not found in test"));

        assertEquals(DiscountCompositionType.SUM,
                company.getDiscountPolicy().getDiscountCompositionType());

        assertEquals(1, company.getDiscountPolicy().getDiscounts().size());
        assertTrue(company.getDiscountPolicy().getDiscounts().get(0) instanceof ConditionalDiscount);
    }

    @Test
    void GivenCompanyWithDiscounts_WhenGetDiscountPolicySummary_ThenReturnsDiscountCountAndComposition() throws Exception {
        CompanyDTO companyDTO = companyService.createProductionCompany(founderToken, VALID_COMPANY_NAME);

        DiscountDTO visible = new DiscountDTO();
        visible.setType("VISIBLE");
        visible.setName("Visible Discount");
        visible.setPercentage(BigDecimal.valueOf(10));

        DiscountPolicyDTO policyDTO = new DiscountPolicyDTO();
        policyDTO.setCompositionType(DiscountCompositionType.SUM);
        policyDTO.setDiscounts(List.of(visible));

        companyService.setCompanyDiscountPolicy(founderToken, companyDTO.getId(), policyDTO);

        String summary = companyService.getDiscountPolicySummary(companyDTO.getId());

        assertEquals("1 הנחות מוגדרות במערכת (כפל מבצעים)", summary);
    }

    private PurchasePolicyDTO maxTicketsPolicyDTO(int maxTickets) {
        return new PurchasePolicyDTO(
                new PurchaseRuleDTO(PurchaseRuleType.MAX_TICKETS, maxTickets, null));
    }

    private PurchasePolicyDTO minAgePolicyDTO(int minAge) {
        return new PurchasePolicyDTO(
                new PurchaseRuleDTO(PurchaseRuleType.MIN_AGE, minAge, null));
    }

}
