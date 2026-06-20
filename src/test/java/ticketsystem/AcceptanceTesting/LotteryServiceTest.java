package ticketsystem.AcceptanceTesting;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.ApplicationLayer.INotifier;
import ticketsystem.ApplicationLayer.ISystemLogger;
import ticketsystem.ApplicationLayer.LotteryService;
import ticketsystem.ApplicationLayer.TokenService;
import ticketsystem.ApplicationLayer.UserAccessService;
import ticketsystem.ApplicationLayer.UserService;
import ticketsystem.DomainLayer.MembershipDomainService;
import ticketsystem.DomainLayer.lottery.Lottery;
import ticketsystem.DomainLayer.lottery.LotteryStatus;
import ticketsystem.DomainLayer.user.Member;
import ticketsystem.DomainLayer.user.Permission;
import ticketsystem.DomainLayer.user.RoleStatus;
import ticketsystem.InfrastructureLayer.LogbackSystemLogger;
import ticketsystem.InfrastructureLayer.LotteryRepository;
import ticketsystem.InfrastructureLayer.InMemoryNotificationsRepository;
import ticketsystem.InfrastructureLayer.TokenRepository;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.InfrastructureLayer.InMemoryUserRepository;
import ticketsystem.testutil.RecordingNotifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * Acceptance tests for LotteryService using the production JPA-backed
 * LotteryRepository with the embedded test database.
 */
@DataJpaTest
@Import(LotteryRepository.class)
public class LotteryServiceTest {

    private IUserRepository userRepo;
    @Autowired
    private LotteryRepository lotteryRepo;
    private TokenService tokenService;
    private InMemoryNotificationsRepository notificationRepository;
    private RecordingNotifier recordingNotifier;
    private INotifier notifier;
    private UserAccessService userAccessService;
    private MembershipDomainService membershipDomain;
    private ISystemLogger logger;
    private TokenRepository tokenRepository;
    private UserService userService;

    /**
     * Helper method to simulate a full user registration and login flow. This
     * ensures the token generated is completely valid and bypasses security
     * checks.
     */
    private String getValidMemberToken(String username, String password) {
        String guestToken = userService.visitSystem();
        userService.signUp(
                guestToken,
                username,
                password,
                "Test User",
                "0500000000", LocalDate.of(2001, 1, 1)
        );
        return userService.login(guestToken, username, password);
    }

    private LotteryService lotteryService;

    private long companyId = 100L;
    private long eventId = 500L;
    private long managerId = 10L;
    private String managerToken;

    @BeforeEach
    public void setUp() {
        userRepo = new InMemoryUserRepository();
        TokenRepository tokenRepository = new TokenRepository();
        logger = new LogbackSystemLogger();
        tokenService = new TokenService("manual_test_secret_32_chars_long", tokenRepository, logger);
        notificationRepository = new InMemoryNotificationsRepository();
        recordingNotifier = new RecordingNotifier();
        notifier = recordingNotifier;
        userAccessService = new UserAccessService(userRepo);
        membershipDomain = new MembershipDomainService(userRepo);

        lotteryService = new LotteryService(
                lotteryRepo,
                tokenService,
                notifier,
                userAccessService,
                membershipDomain,
                logger
        );

        Member manager = new Member(managerId, "manager", "Lottery Manager", "0501234567", LocalDate.of(2001, 1, 1));
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
    public void AcceptanceTest_RegisterForPurchaseLottery_Selected() {
        // --- 1. Setup ---      
        long user1Id = 55L;
        Member user1 = new Member(user1Id, "winner", "Winner User", "0501231231", LocalDate.of(2001, 1, 1));
        userRepo.addRegisteredMember(user1Id, user1, "Pass123!");
        String user1Token = tokenService.addActiveSession(user1);
        int winnersAmount = 1;
        long eventId = 100L;
        long lotteryId = lotteryService.addLottery(managerToken, eventId, companyId, winnersAmount);

        // --- 2. Action (requests to register) ---
        boolean isRegistered = lotteryService.registerMemberToLottery(user1Token, lotteryId);
        assertTrue(isRegistered, "Registration process should complete successfully");

        // --- 3. Verify Registration ---
        Lottery lotteryBeforeDraw = lotteryRepo.findById(lotteryId);
        assertTrue(lotteryBeforeDraw.getRegisteredMemberIds().contains(user1Id),
                "User1 should be in the participants list before the draw");

        // --- 4. Conduct Draw ---
        lotteryService.closeLotteryRegistration(managerToken, lotteryId, companyId);
        lotteryService.conductLotteryDraw(managerToken, lotteryId, companyId);

        // --- 5. Verify Selection (user1 is selected) ---
        Lottery lotteryAfterDraw = lotteryRepo.findById(lotteryId);
        assertTrue(lotteryAfterDraw.getWinners().contains(user1Id),
                "User1 should be selected as a winner because they are the only participant");
        recordingNotifier.assertNotifiedMember(user1Id, "registered");
        recordingNotifier.assertNotifiedMember(user1Id, "Congratulations");
        recordingNotifier.assertNotificationCount(2);
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
        Member normalUser = new Member(normalUserId, "normal", "Normal User", "0509999999", LocalDate.of(2001, 1, 1));
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
        Member participant = new Member(participantId, "part1", "Participant 1", "0501112222", LocalDate.of(2001, 1, 1));
        userRepo.addRegisteredMember(participantId, participant, "pass");
        String partToken = tokenService.addActiveSession(participant);

        boolean result = lotteryService.registerMemberToLottery(partToken, lotteryId);

        assertTrue(result, "Registration should succeed");
        Lottery updatedLottery = lotteryRepo.findById(lotteryId);
        assertTrue(updatedLottery.getRegisteredMemberIds().contains(participantId), "Participant should be in the registered list");
        recordingNotifier.assertNotifiedMember(participantId, "registered");
        recordingNotifier.assertNotificationCount(1);
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
            Member p = new Member(pId, "user" + i, "User " + i, "050000000" + i, LocalDate.of(2001, 1, 1));
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

        long winnerId = completedLottery.getWinners().iterator().next();
        long loserId = List.of(201L, 202L, 203L).stream()
                .filter(id -> !completedLottery.getWinners().contains(id))
                .findFirst()
                .orElseThrow();

        recordingNotifier.assertNotifiedMember(winnerId, "Congratulations");
        recordingNotifier.assertNotifiedMember(loserId, "did not win");
        recordingNotifier.assertNotificationCount(6);
    }

    @Test
    public void AcceptanceTest_ConductDraw_Failure_LotteryStillOpen() {
        long lotteryId = lotteryService.addLottery(managerToken, eventId, companyId, 2);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            lotteryService.conductLotteryDraw(managerToken, lotteryId, companyId);
        });
        assertTrue(exception.getMessage().contains("is not closed yet"), "Should not allow draw on an OPEN lottery");
    }

    @Test
    public void GivenExistingLotteryForEvent_WhenHasLotteryForEvent_ThenReturnsTrue() {
        lotteryService.addLottery(managerToken, eventId, companyId, 2);

        boolean result = lotteryService.hasLotteryForEvent(managerToken, eventId);

        assertTrue(result);
    }

    @Test
    public void GivenNoLotteryForEvent_WhenHasLotteryForEvent_ThenReturnsFalse() {
        boolean result = lotteryService.hasLotteryForEvent(managerToken, 999L);

        assertFalse(result);
    }

    @Test
    public void GivenInvalidEventId_WhenHasLotteryForEvent_ThenReturnsFalse() {
        boolean result = lotteryService.hasLotteryForEvent(managerToken, 0L);

        assertFalse(result);
    }

    @Test
    public void GivenExistingLotteryForEvent_WhenGetLotteryIdByEventId_ThenReturnsLotteryId() {
        long lotteryId = lotteryService.addLottery(managerToken, eventId, companyId, 2);

        long result = lotteryService.getLotteryIdByEventId(eventId);

        assertEquals(lotteryId, result);
    }

    @Test
    public void GivenInvalidEventId_WhenGetLotteryIdByEventId_ThenThrowsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> lotteryService.getLotteryIdByEventId(0L)
        );

        assertEquals("Event ID is invalid.", exception.getMessage());
    }

    @Test
    public void GivenEventWithoutLottery_WhenGetLotteryIdByEventId_ThenThrowsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> lotteryService.getLotteryIdByEventId(999L)
        );

        assertEquals("Lottery for event not found.", exception.getMessage());
    }

    @Test
    public void GivenExistingLotteryForEvent_WhenRegisterMemberToLotteryByEventId_ThenMemberIsRegistered() {
        long lotteryId = lotteryService.addLottery(managerToken, eventId, companyId, 2);

        long participantId = 301L;
        Member participant = new Member(
                participantId,
                "eventParticipant",
                "Event Participant",
                "0503010301",
                LocalDate.of(2001, 1, 1)
        );
        userRepo.addRegisteredMember(participantId, participant, "pass");
        String participantToken = tokenService.addActiveSession(participant);

        boolean result = lotteryService.registerMemberToLotteryByEventId(participantToken, eventId);

        Lottery lottery = lotteryRepo.findById(lotteryId);

        assertTrue(result);
        assertTrue(lottery.getRegisteredMemberIds().contains(participantId));
    }

    @Test
    public void GivenInvalidEventId_WhenRegisterMemberToLotteryByEventId_ThenThrowsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> lotteryService.registerMemberToLotteryByEventId(managerToken, 0L)
        );

        assertEquals("Event ID is invalid.", exception.getMessage());
    }

    @Test
    public void GivenEventWithoutLottery_WhenRegisterMemberToLotteryByEventId_ThenThrowsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> lotteryService.registerMemberToLotteryByEventId(managerToken, 999L)
        );

        assertEquals("Lottery for event not found.", exception.getMessage());
    }

    @Test
    public void GivenCompletedLottery_WhenGetWinners_ThenReturnsWinnerIds() {
        long participantId = 401L;
        Member participant = new Member(
                participantId,
                "singleWinner",
                "Single Winner",
                "0504010401",
                LocalDate.of(2001, 1, 1)
        );
        userRepo.addRegisteredMember(participantId, participant, "pass");
        String participantToken = tokenService.addActiveSession(participant);

        long lotteryId = lotteryService.addLottery(managerToken, eventId, companyId, 1);

        lotteryService.registerMemberToLottery(participantToken, lotteryId);
        lotteryService.closeLotteryRegistration(managerToken, lotteryId, companyId);
        lotteryService.conductLotteryDraw(managerToken, lotteryId, companyId);

        List<Long> winners = lotteryService.getWinners(lotteryId);

        assertEquals(1, winners.size());
        assertTrue(winners.contains(participantId));
    }

    @Test
    public void GivenMissingLottery_WhenGetWinners_ThenThrowsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> lotteryService.getWinners(999L)
        );

        assertEquals("Lottery with ID 999 does not exist.", exception.getMessage());
    }

    @Test
    public void GivenUserWithoutPermission_WhenCloseLotteryRegistration_ThenThrowsException() {
        long lotteryId = lotteryService.addLottery(managerToken, eventId, companyId, 2);

        long normalUserId = 501L;
        Member normalUser = new Member(
                normalUserId,
                "closeNoPermission",
                "Close No Permission",
                "0505010501",
                LocalDate.of(2001, 1, 1)
        );
        userRepo.addRegisteredMember(normalUserId, normalUser, "pass");
        String normalToken = tokenService.addActiveSession(normalUser);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> lotteryService.closeLotteryRegistration(normalToken, lotteryId, companyId)
        );

        assertEquals("Insufficient permissions to close lottery registration", exception.getMessage());
    }

    @Test
    public void GivenMissingLottery_WhenCloseLotteryRegistration_ThenThrowsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> lotteryService.closeLotteryRegistration(managerToken, 999L, companyId)
        );

        assertEquals("Lottery with ID 999 not found.", exception.getMessage());
    }

    @Test
    public void GivenUserWithoutPermission_WhenConductLotteryDraw_ThenThrowsException() {
        long lotteryId = lotteryService.addLottery(managerToken, eventId, companyId, 2);

        long normalUserId = 601L;
        Member normalUser = new Member(
                normalUserId,
                "drawNoPermission",
                "Draw No Permission",
                "0506010601",
                LocalDate.of(2001, 1, 1)
        );
        userRepo.addRegisteredMember(normalUserId, normalUser, "pass");
        String normalToken = tokenService.addActiveSession(normalUser);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> lotteryService.conductLotteryDraw(normalToken, lotteryId, companyId)
        );

        assertEquals("Insufficient permissions to conduct lottery draw", exception.getMessage());
    }

    @Test
    public void GivenMissingLottery_WhenConductLotteryDraw_ThenThrowsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> lotteryService.conductLotteryDraw(managerToken, 999L, companyId)
        );

        assertEquals("Lottery with ID 999 not found.", exception.getMessage());
    }

    @Test
    public void GivenWinnerWithWrongCode_WhenValidateWinnerCodeForEvent_ThenReturnsFalse() {
        long participantId = 702L;
        Member participant = new Member(
                participantId,
                "wrongCodeWinner",
                "Wrong Code Winner",
                "0507020702",
                LocalDate.of(2001, 1, 1)
        );
        userRepo.addRegisteredMember(participantId, participant, "pass");
        String participantToken = tokenService.addActiveSession(participant);

        long lotteryId = lotteryService.addLottery(managerToken, eventId, companyId, 1);

        lotteryService.registerMemberToLottery(participantToken, lotteryId);
        lotteryService.closeLotteryRegistration(managerToken, lotteryId, companyId);
        lotteryService.conductLotteryDraw(managerToken, lotteryId, companyId);

        boolean result = lotteryService.validateWinnerCodeForEvent(
                participantToken,
                eventId,
                "WRONG123"
        );

        assertFalse(result);
    }

    @Test
    public void GivenBlankCode_WhenValidateWinnerCodeForEvent_ThenReturnsFalse() {
        long participantId = 703L;
        Member participant = new Member(
                participantId,
                "blankCodeUser",
                "Blank Code User",
                "0507030703",
                LocalDate.of(2001, 1, 1)
        );
        userRepo.addRegisteredMember(participantId, participant, "pass");
        String participantToken = tokenService.addActiveSession(participant);

        boolean result = lotteryService.validateWinnerCodeForEvent(
                participantToken,
                eventId,
                "   "
        );

        assertFalse(result);
    }

    @Test
    public void GivenInvalidEventId_WhenValidateWinnerCodeForEvent_ThenThrowsException() {
        long participantId = 704L;
        Member participant = new Member(
                participantId,
                "invalidEventCodeUser",
                "Invalid Event Code User",
                "0507040704",
                LocalDate.of(2001, 1, 1)
        );
        userRepo.addRegisteredMember(participantId, participant, "pass");
        String participantToken = tokenService.addActiveSession(participant);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> lotteryService.validateWinnerCodeForEvent(participantToken, 0L, "ABC123")
        );

        assertEquals("Event ID is invalid.", exception.getMessage());
    }

    @Test
    public void GivenEventWithoutLottery_WhenValidateWinnerCodeForEvent_ThenThrowsException() {
        long participantId = 705L;
        Member participant = new Member(
                participantId,
                "noLotteryCodeUser",
                "No Lottery Code User",
                "0507050705",
                LocalDate.of(2001, 1, 1)
        );
        userRepo.addRegisteredMember(participantId, participant, "pass");
        String participantToken = tokenService.addActiveSession(participant);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> lotteryService.validateWinnerCodeForEvent(participantToken, 999L, "ABC123")
        );

        assertEquals("Lottery for event not found.", exception.getMessage());
    }

}
