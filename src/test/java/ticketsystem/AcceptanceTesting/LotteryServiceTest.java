package ticketsystem.AcceptanceTesting;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.ApplicationLayer.INotifier;
import ticketsystem.ApplicationLayer.LotteryService;
import ticketsystem.ApplicationLayer.TokenService;
import ticketsystem.ApplicationLayer.UserAccessService;
import ticketsystem.DomainLayer.MembershipDomainService;
import ticketsystem.DomainLayer.lottery.Lottery;
import ticketsystem.DomainLayer.lottery.LotteryStatus;
import ticketsystem.DomainLayer.user.Member;
import ticketsystem.DomainLayer.user.Permission;
import ticketsystem.DomainLayer.user.RoleStatus;
import ticketsystem.InfrastructureLayer.LogbackSystemLogger;
import ticketsystem.InfrastructureLayer.LotteryRepository;
import ticketsystem.InfrastructureLayer.TokenRepository;
import ticketsystem.InfrastructureLayer.UserRepository;


public class LotteryServiceTest {

    private UserRepository userRepo;
    private LotteryRepository lotteryRepo;
    private TokenService tokenService;
    private FakeNotifier fakeNotifier;
    private UserAccessService userAccessService;
    private MembershipDomainService membershipDomain;
    private LogbackSystemLogger logger;
    
    private LotteryService lotteryService;

    private long companyId = 100L;
    private long eventId = 500L;
    private long managerId = 10L;
    private String managerToken;

    @BeforeEach
    public void setUp() {
        userRepo = new UserRepository();
        lotteryRepo = new LotteryRepository(); 
        TokenRepository tokenRepository = new TokenRepository();
        tokenService = new TokenService("manual_test_secret_32_chars_long", tokenRepository);
        fakeNotifier = new FakeNotifier();
        userAccessService = new UserAccessService(userRepo);
        membershipDomain = new MembershipDomainService(userRepo);
        logger = new LogbackSystemLogger();

        lotteryService = new LotteryService(
                lotteryRepo, 
                tokenService, 
                fakeNotifier, 
                userAccessService, 
                membershipDomain, 
                logger
        );

        Member manager = new Member(managerId, "manager", "Lottery Manager", "0501234567");
        Set<Permission> managerPermissions = new HashSet<>();
        managerPermissions.add(Permission.MANAGE_EVENT_INVENTORY);
        
        manager.addManagerRole(companyId, 1L, managerPermissions);
        manager.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        userRepo.addRegisteredMember(managerId, manager, "password123");
        
        managerToken = tokenService.addActiveSession(manager);
    }

    @Test
    public void AcceptanceTest_AddLottery_Successful() {
        long lotteryId = lotteryService.addLottery(managerToken, eventId, companyId, 5);

        assertTrue(lotteryId > 0, "Lottery ID should be generated and positive");
        Lottery savedLottery = lotteryRepo.findById(lotteryId);
        assertNotNull(savedLottery, "Lottery should be saved in the repository");
        assertEquals(5, savedLottery.getWinnersNumber(), "Winners number should match");
        assertEquals(eventId, savedLottery.getEventId(), "Event ID should match");
    }

    @Test
    public void AcceptanceTest_AddLottery_Failure_ZeroWinners() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            lotteryService.addLottery(managerToken, eventId, companyId, 0);
        });
        assertTrue(exception.getMessage().contains("greater than zero"), "Should not allow creating lottery with 0 winners");
    }

    @Test
    public void AcceptanceTest_AddLottery_Failure_NoPermission() {
        long normalUserId = 20L;
        Member normalUser = new Member(normalUserId, "normal", "Normal User", "0509999999");
        userRepo.addRegisteredMember(normalUserId, normalUser, "pass");
        String normalToken = tokenService.addActiveSession(normalUser);

        Exception exception = assertThrows(Exception.class, () -> { 
            lotteryService.addLottery(normalToken, eventId, companyId, 5);
        });
        assertNotNull(exception); 
    }

    @Test
    public void AcceptanceTest_RegisterToLottery_Successful() {
        long lotteryId = lotteryService.addLottery(managerToken, eventId, companyId, 5);
        
        long participantId = 11L;
        Member participant = new Member(participantId, "part1", "Participant 1", "0501112222");
        userRepo.addRegisteredMember(participantId, participant, "pass");
        String partToken = tokenService.addActiveSession(participant);

        boolean result = lotteryService.registerMemberToLottery(partToken, lotteryId);

        assertTrue(result, "Registration should succeed");
        Lottery updatedLottery = lotteryRepo.findById(lotteryId);
        assertTrue(updatedLottery.getRegisteredMemberIds().contains(participantId), "Participant should be in the registered list");
    }

    @Test
    public void AcceptanceTest_RegisterToLottery_Failure_LotteryNotFound() {
        long invalidLotteryId = 999L;
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            lotteryService.registerMemberToLottery(managerToken, invalidLotteryId);
        });
        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    public void AcceptanceTest_CloseLottery_Successful() {
        long lotteryId = lotteryService.addLottery(managerToken, eventId, companyId, 2);

        boolean result = lotteryService.closeLotteryRegistration(managerToken, lotteryId, companyId);

        assertTrue(result);
        Lottery updatedLottery = lotteryRepo.findById(lotteryId);
        assertEquals(LotteryStatus.CLOSED, updatedLottery.getStatus(), "Lottery status should be changed to CLOSED");
    }

    @Test
    public void AcceptanceTest_ConductDraw_Successful() {
        int winnersAmount = 2;
        long lotteryId = lotteryService.addLottery(managerToken, eventId, companyId, winnersAmount);
        
        for (long i = 1; i <= 3; i++) {
            long pId = 200L + i;
            Member p = new Member(pId, "user" + i, "User " + i, "050000000" + i);
            userRepo.addRegisteredMember(pId, p, "pass");
            String pToken = tokenService.addActiveSession(p);
            lotteryService.registerMemberToLottery(pToken, lotteryId);
        }

        lotteryService.closeLotteryRegistration(managerToken, lotteryId, companyId);

        boolean result = lotteryService.conductLotteryDraw(managerToken, lotteryId, companyId);

        assertTrue(result, "Draw should execute successfully");
        
        Lottery completedLottery = lotteryRepo.findById(lotteryId);
        assertEquals(LotteryStatus.COMPLETED, completedLottery.getStatus(), "Status should be COMPLETED");
        assertEquals(winnersAmount, completedLottery.getWinners().size(), "Should have exactly 2 winners");
        
        assertTrue(fakeNotifier.containsMessage("Congratulations!"), "Winners should receive a notification");
    }

    @Test
    public void AcceptanceTest_ConductDraw_Failure_LotteryStillOpen() {
        long lotteryId = lotteryService.addLottery(managerToken, eventId, companyId, 2);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            lotteryService.conductLotteryDraw(managerToken, lotteryId, companyId);
        });
        assertTrue(exception.getMessage().contains("is not closed yet"), "Should not allow draw on an OPEN lottery");
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
            return messages.stream().anyMatch(message -> message.contains(text));
        }
    }
}