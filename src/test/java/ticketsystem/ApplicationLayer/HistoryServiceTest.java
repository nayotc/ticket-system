package ticketsystem.ApplicationLayer;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.DTO.OrderDTO;
import ticketsystem.DTO.PurchaseDTO;
import ticketsystem.DomainLayer.MembershipDomainService;
import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.IRepository.IHistoryRepository;
import ticketsystem.DomainLayer.IRepository.ITokenRepository;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.DomainLayer.company.Company;
import ticketsystem.DomainLayer.user.Member;
import ticketsystem.InfrastructureLayer.CompanyRepository;
import ticketsystem.InfrastructureLayer.HistoryRepository;
import ticketsystem.InfrastructureLayer.TokenRepository;
import ticketsystem.InfrastructureLayer.UserRepository;
import ticketsystem.InfrastructureLayer.LogbackSystemLogger;

public class HistoryServiceTest {

    private IHistoryRepository historyRepository;
    private ITokenRepository tokenRepository;
    private IUserRepository userRepository;
    private ITokenService tokenService;
    private UserService userService;
    private HistoryService historyService;
    private ICompanyRepository companyRepository;

    @BeforeEach
    void setUp() {
        // --- Setup Real Repositories (Acceptance Level) ---
        HistoryRepository hRepo = new HistoryRepository();
        this.historyRepository = hRepo;

        this.tokenRepository = new TokenRepository();
        this.userRepository = new UserRepository(); 
        
        this.tokenService = new TokenService("manual_test_secret_32_chars_long", tokenRepository);
        this.userService = new UserService(userRepository, tokenService, new LogbackSystemLogger());
        this.companyRepository = new CompanyRepository();
        this.historyService = new HistoryService(historyRepository, tokenService, new MembershipDomainService(userRepository), new LogbackSystemLogger()    );
    }

    /**
     * Helper method to simulate a full user registration and login flow.
     */
    private String getValidMemberToken(String username, String password) {
        String guestToken = userService.visitSystem();
        userService.signUp(guestToken, username, password);
        return userService.login(guestToken, username, password);
    }

    /**
     * 3.5 View personal purchase history - Successful Scenario
     */
    @Test
    void GivenValidTokenAndExistingHistory_WhenGetHistoryForUser_ThenReturnsHistory() {
        // --- Given (Arrange) ---
        String validToken = getValidMemberToken("reut_history_user", "Pass123!");
        long userId = tokenService.extractUserId(validToken);
        
        List<PurchaseDTO> ticketDTOs = new ArrayList<>();
        ticketDTOs.add(new PurchaseDTO(10L, 20L, 1, 1, new BigDecimal("150.0"), "ACTIVE", ""));
        OrderDTO orderDto = new OrderDTO(0L, ticketDTOs, "Taylor Swift Tour", "HaYarkon Park", userId, 50L);
        
        historyService.onOrderCompleted(orderDto);

        // --- When (Act) ---
        List<OrderDTO> result = historyService.getHistoryForUser(validToken);

        // --- Then (Assert) ---
        assertNotNull(result, "The result should not be null");
        assertFalse(result.isEmpty(), "The history list should not be empty");
        assertEquals("Taylor Swift Tour", result.get(0).getEventName(), "Event name should match");
    }

    /**
     * 3.5 View personal purchase history - Failure (Empty) Scenario
     */
    @Test
    void GivenValidTokenAndNoHistory_WhenGetHistoryForUser_ThenReturnsEmptyList() {
        // --- Given (Arrange) ---
        String validToken = getValidMemberToken("new_user_no_history", "Pass123!");

        // --- When (Act) ---
        List<OrderDTO> result = historyService.getHistoryForUser(validToken);

        // --- Then (Assert) ---
        assertNotNull(result, "Result should be an empty list, not null");
        assertTrue(result.isEmpty(), "History should be empty when user has no purchases");
    }

    /**
     * 3.5 View personal purchase history - Unauthorized Scenario
     */
    @Test
    void GivenInvalidToken_WhenGetHistoryForUser_ThenThrowsIllegalArgumentException() {
        // --- Given (Arrange) ---
        String invalidToken = "invalid-token-456";

        // --- When & Then (Act & Assert) ---
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            historyService.getHistoryForUser(invalidToken);
        });
        
        assertTrue(exception.getMessage().contains("token"), "Error message should mention token");
    }

    /**
     * Add Purchase - Successful Scenario
     */
    @Test
    void GivenOrderDTO_WhenOnOrderCompleted_ThenPurchaseIsAdded() {
        // --- Arrange ---
        String validToken = getValidMemberToken("buyer_user", "Pass123!");
        long userId = tokenService.extractUserId(validToken);
        
        List<PurchaseDTO> ticketDTOs = new ArrayList<>();
        ticketDTOs.add(new PurchaseDTO(10L, 20L, 1, 1, new BigDecimal("150.0"), "ACTIVE", ""));
        OrderDTO orderDto = new OrderDTO(0L, ticketDTOs, "Rock Concert", "Barby", userId, 5L);

        // --- Act ---
        historyService.onOrderCompleted(orderDto);

        // --- Assert ---
        List<OrderDTO> history = historyService.getHistoryForUser(validToken);
        assertEquals(1, history.size(), "One purchase should be added to history");
    }
        private Company createCompanyWithFounderRole(long founderId) {
        Company company = new Company(
                "history_company_" + founderId,
                founderId,
                null,
                null
        );

        companyRepository.save(company);

        Member founder = userRepository.getMemberById(founderId);
        assertNotNull(founder, "Founder member must exist before assigning company role");

        boolean roleAdded = founder.addFounderRole(company.getId());
        assertTrue(roleAdded, "Founder role should be added successfully");

        return company;
    }

    private OrderDTO createOrderDTO(long userId, long companyId, String eventName) {
        List<PurchaseDTO> purchases = new ArrayList<>();
        purchases.add(new PurchaseDTO(
                10L,
                20L,
                1,
                1,
                new BigDecimal("150.0"),
                "ACTIVE",
                ""
        ));

        return new OrderDTO(
                0L,
                purchases,
                eventName,
                "HaYarkon Park",
                userId,
                companyId
        );
    }
    /**
 * 4.5 View purchase and order history - Successful Scenario
    */
    @Test
    void GivenOwnerAndExistingCompanyHistory_WhenGetHistoryForCompany_ThenReturnsCompanyHistory() {
        // --- Given (Arrange) ---
        String ownerToken = getValidMemberToken("company_history_owner", "Pass123!");
        long ownerId = tokenService.extractUserId(ownerToken);

        Company company = createCompanyWithFounderRole(ownerId);

        OrderDTO orderDto = createOrderDTO(
                ownerId,
                company.getId(),
                "Company History Concert"
        );

        historyService.onOrderCompleted(orderDto);

        // --- When (Act) ---
        List<OrderDTO> result = historyService.getHistoryForCompany(ownerToken, company.getId());

        // --- Then (Assert) ---
        assertNotNull(result, "The result should not be null");
        assertFalse(result.isEmpty(), "Company history should not be empty when purchases exist");
        assertEquals(1, result.size(), "Company history should contain exactly one order");
        assertEquals("Company History Concert", result.get(0).getEventName(), "Event name should match");
        assertEquals(company.getId(), result.get(0).getCompanyId(), "Company id should match");
    }
    @Test
    void GivenMemberWithoutCompanyPermission_WhenGetHistoryForCompany_ThenThrowsIllegalArgumentException() {
        // --- Given (Arrange) ---
        String ownerToken = getValidMemberToken("real_company_owner", "Pass123!");
        long ownerId = tokenService.extractUserId(ownerToken);
        Company company = createCompanyWithFounderRole(ownerId);

        String regularMemberToken = getValidMemberToken("regular_member_no_permission", "Pass123!");

        // --- When & Then ---
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            historyService.getHistoryForCompany(regularMemberToken, company.getId());
        });

        assertTrue(
                exception.getMessage().contains("permissions"),
                "Error message should mention insufficient permissions"
        );
    }
}