package ticketsystem.AcceptanceTesting;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.ApplicationLayer.ITokenService;
import ticketsystem.ApplicationLayer.LotteryService;
import ticketsystem.ApplicationLayer.TokenService;
import ticketsystem.DomainLayer.IRepository.ILotteryRepository;
import ticketsystem.DomainLayer.IRepository.ITokenRepository;
import ticketsystem.DomainLayer.lottery.Lottery;
import ticketsystem.InfrastructureLayer.LotteryRepository;
import ticketsystem.InfrastructureLayer.TokenRepository;

public class LotteryServiceTest {

    private ILotteryRepository lotteryRepository;
    private ITokenService tokenService;
    private LotteryService lotteryService;
    private ITokenRepository tokenRepository;

    @BeforeEach
    void setUp() {
        // אנחנו מאתחלים סביבה אמיתית "נקייה" לפני כל טסט
        tokenRepository = new TokenRepository();
        
        // מושכים את הסינגלטון ומאפסים אותו כדי שכל טסט יתחיל מ-ID מספר 1
        LotteryRepository repo = LotteryRepository.getInstance();
        repo.clearForTests(); 
        lotteryRepository = repo; 
        
        tokenService = new TokenService("manual_test_secret_32_chars_long", tokenRepository); 
        lotteryService = new LotteryService(lotteryRepository, tokenService);
    }

    @Test
    void AcceptanceTest_RegisterForPurchaseLottery_Selected() {
        // --- 1. Preparation (user created & logged in, event exists) ---
        long user1Id = 1L;
        String user1Token = tokenService.generateNewMemberToken(user1Id);

        long eventId = 100L;
        lotteryService.addLottery(eventId, 1); // הגרלה על מקום 1 בלבד
        long lotteryId = 1L; 

        // --- 2. Action (requests to register) ---
        boolean isRegistered = lotteryService.registerMemberToLottery(user1Token, lotteryId);
        assertTrue(isRegistered, "Registration process should complete successfully");

        // --- 3. Verify Registration ---
        Lottery lotteryBeforeDraw = lotteryRepository.findById(lotteryId);
        assertTrue(lotteryBeforeDraw.getRegisteredMemberIds().contains(user1Id), 
            "User1 should be in the participants list before the draw");

        // --- 4. Conduct Draw ---
        lotteryService.conductLotteryDraw(lotteryId, 1);

        // --- 5. Verify Selection (user1 is selected) ---
        Lottery lotteryAfterDraw = lotteryRepository.findById(lotteryId);
    
        // בדיקה שהמשתמש אכן נמצא ברשימת המנצחים 
        assertTrue(lotteryAfterDraw.getWinners().contains(user1Id), 
            "User1 MUST be selected as the winner because he is the only participant");
    }

    @Test
    void AcceptanceTest_RegisterForPurchaseLottery_NotSelected() {
        // --- 1. Preparation ---
        long user1Id = 1L;
        long user2Id = 2L;
        String user1Token = tokenService.generateNewMemberToken(user1Id);
        String user2Token = tokenService.generateNewMemberToken(user2Id);

        long eventId = 200L;
        lotteryService.addLottery(eventId, 1); // רק מקום אחד לזכייה
        long lotteryId = 1L;

        // --- 2. Action (both users register) ---
        lotteryService.registerMemberToLottery(user1Token, lotteryId);
        lotteryService.registerMemberToLottery(user2Token, lotteryId);

        // --- 3. Conduct Draw ---
        lotteryService.conductLotteryDraw(lotteryId, 1);

        // --- 4. Verify Not Selected status ---
        Lottery lotteryAfterDraw = lotteryRepository.findById(lotteryId);
        
        // בודקים מי הפסיד בהגרלה
        long loserId = lotteryAfterDraw.getWinners().contains(user1Id) ? user2Id : user1Id;

        assertFalse(lotteryAfterDraw.getWinners().contains(loserId), 
            "The loser should NOT be in the winners list");
            
        // מוודאים שהוא עדיין רשום כמשתתף היסטורי (לצורך תיעוד), גם אם לא זכה
        assertTrue(lotteryAfterDraw.getRegisteredMemberIds().contains(loserId), 
            "The loser should still be recorded as a participant who registered");
    }

    @Test
    void GivenValidEventAndWinners_WhenAddLottery_ThenRepositorySavesLottery() {
        long eventId = 100L;
        int winners = 5;
        lotteryService.addLottery(eventId, winners);

        Lottery savedLottery = lotteryRepository.findById(1L);
        assertNotNull(savedLottery, "Lottery should be saved in the repository");
        assertEquals(eventId, savedLottery.getEventId(), "Event ID should match");
        assertEquals(winners, savedLottery.getWinnersNumber(), "Winners number should match");
    }

    @Test
    void GivenInvalidWinnersCount_WhenAddLottery_ThenThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            lotteryService.addLottery(100L, 0);
        });
        
        // כיוון שהריפוסיטורי אופס, ID 1 באמת לא אמור להיות קיים
        Lottery notSavedLottery = lotteryRepository.findById(1L);
        assertNull(notSavedLottery, "Lottery should not be saved due to exception");
    }
}