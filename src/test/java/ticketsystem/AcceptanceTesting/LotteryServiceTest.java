package ticketsystem.AcceptanceTesting;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.ApplicationLayer.INotifier;
import ticketsystem.ApplicationLayer.ITokenService;
import ticketsystem.ApplicationLayer.LotteryService;
import ticketsystem.ApplicationLayer.TokenService;
import ticketsystem.ApplicationLayer.UserAccessService;
import ticketsystem.ApplicationLayer.UserService;
import ticketsystem.DomainLayer.IRepository.ILotteryRepository;
import ticketsystem.DomainLayer.IRepository.ITokenRepository;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.DomainLayer.lottery.Lottery;
import ticketsystem.DomainLayer.user.Guest;
import ticketsystem.InfrastructureLayer.LotteryRepository;
import ticketsystem.InfrastructureLayer.TokenRepository;
import ticketsystem.InfrastructureLayer.UserRepository; // בהנחה שיש מחלקה כזו
import ticketsystem.InfrastructureLayer.LogbackSystemLogger;

public class LotteryServiceTest {

    private ILotteryRepository lotteryRepository;
    private ITokenService tokenService;
    private LotteryService lotteryService;
    private ITokenRepository tokenRepository;
    private IUserRepository userRepository;
    private UserService userService;
    private FakeNotifier fakeNotifier;
    private UserAccessService userAccessService;
    @BeforeEach
    void setUp() {
        tokenRepository = new TokenRepository();
        LotteryRepository repo = new LotteryRepository();
        repo.clearForTests(); 
        lotteryRepository = repo; 
        userRepository = new UserRepository();
        userAccessService=new UserAccessService(userRepository);
        tokenService = new TokenService("manual_test_secret_32_chars_long", tokenRepository);
        fakeNotifier = new FakeNotifier(); 
        lotteryService = new LotteryService(lotteryRepository, tokenService,fakeNotifier,userAccessService);
        
        userService = new UserService(userRepository, tokenService, new LogbackSystemLogger());
    }

    /**
     * Helper method to simulate a full user registration and login flow.
     * This ensures the token generated is completely valid and bypasses security checks.
     */
    private String getValidMemberToken(String username, String password) {
        String guestToken = userService.visitSystem();
        userService.signUp(
                guestToken,
                username,
                password,
                "Test User",
                "0500000000"
        );
        return userService.login(guestToken, username, password);
    }

    @Test
    void AcceptanceTest_RegisterForPurchaseLottery_Selected() {
        String user1Token = getValidMemberToken("winner", "Pass123!");
        long user1Id = tokenService.extractUserId(user1Token); 

        String validTokenLotteryActions = tokenService.addActiveSession(new Guest()); 

        long eventId = 100L;
        lotteryService.addLottery(validTokenLotteryActions, eventId, 1); //only 1 winner allowed
        long lotteryId = 1L; 

        // --- 2. Action (requests to register) ---
        boolean isRegistered = lotteryService.registerMemberToLottery(user1Token, lotteryId);
        assertTrue(isRegistered, "Registration process should complete successfully");

        // --- 3. Verify Registration ---
        Lottery lotteryBeforeDraw = lotteryRepository.findById(lotteryId);
        assertTrue(lotteryBeforeDraw.getRegisteredMemberIds().contains(user1Id), 
            "User1 should be in the participants list before the draw");

        // --- 4. Conduct Draw ---
        lotteryService.closeLotteryRegistration(validTokenLotteryActions, lotteryId); 
        lotteryService.conductLotteryDraw(validTokenLotteryActions, lotteryId);

        // --- 5. Verify Selection (user1 is selected) ---
        Lottery lotteryAfterDraw = lotteryRepository.findById(lotteryId);
    
        assertTrue(lotteryAfterDraw.getWinners().contains(user1Id), 
            "User1 MUST be selected as the winner because he is the only participant");
    }

    @Test
    void AcceptanceTest_RegisterForPurchaseLottery_NotSelected() {
        // --- 1. Preparation ---
        String user1Token = getValidMemberToken("user1", "Pass123!");
        long user1Id = tokenService.extractUserId(user1Token);

        String user2Token = getValidMemberToken("user2", "Pass456!");
        long user2Id = tokenService.extractUserId(user2Token);

        String validTokenLotteryActions = tokenService.addActiveSession(new Guest());

        long eventId = 200L;
        lotteryService.addLottery(validTokenLotteryActions, eventId, 1); // only 1 winner allowed
        long lotteryId = 1L;

        // --- 2. Action (both users register) ---
        lotteryService.registerMemberToLottery(user1Token, lotteryId);
        lotteryService.registerMemberToLottery(user2Token, lotteryId);

        // --- 3. Conduct Draw ---
        lotteryService.closeLotteryRegistration(validTokenLotteryActions, lotteryId);
        lotteryService.conductLotteryDraw(validTokenLotteryActions, lotteryId);

        // --- 4. Verify Not Selected status ---
        Lottery lotteryAfterDraw = lotteryRepository.findById(lotteryId);
        
        // בודקים מי הפסיד בהגרלה
        long loserId = lotteryAfterDraw.getWinners().contains(user1Id) ? user2Id : user1Id;

        assertFalse(lotteryAfterDraw.getWinners().contains(loserId), 
            "The loser should NOT be in the winners list");
            
        // still should be in the registered participants list, because they registered successfully
        assertTrue(lotteryAfterDraw.getRegisteredMemberIds().contains(loserId), 
            "The loser should still be recorded as a participant who registered");
    }

    @Test
    void GivenValidEventAndWinners_WhenAddLottery_ThenRepositorySavesLottery() {
        long eventId = 100L;
        int winners = 5;
        
        String validTokenLotteryActions = tokenService.addActiveSession(new Guest());
        
        lotteryService.addLottery(validTokenLotteryActions, eventId, winners);

        Lottery savedLottery = lotteryRepository.findById(1L);
        assertNotNull(savedLottery, "Lottery should be saved in the repository");
        assertEquals(eventId, savedLottery.getEventId(), "Event ID should match");
        assertEquals(winners, savedLottery.getWinnersNumber(), "Winners number should match");
    }

    @Test
    void GivenInvalidWinnersCount_WhenAddLottery_ThenThrowsException() {
        String validTokenLotteryActions = tokenService.addActiveSession(new Guest());
        
        assertThrows(IllegalArgumentException.class, () -> {
            lotteryService.addLottery(validTokenLotteryActions, 100L, 0);
        });
        
        Lottery notSavedLottery = lotteryRepository.findById(1L);
        assertNull(notSavedLottery, "Lottery should not be saved due to exception");
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