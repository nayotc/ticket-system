package ticketsystem.UnitTesting;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.DomainLayer.lottery.Lottery;
import ticketsystem.DomainLayer.lottery.LotteryStatus;

/**
 * Unit tests for the Lottery aggregate.
 *
 * <p>These tests verify domain behavior only. They intentionally do not start
 * Spring or access a database. Database-generated identifiers and persistence
 * mappings are verified separately by repository integration tests.</p>
 */
public class LotteryTest {

    private static final long EVENT_ID = 100L;
    private static final int WINNERS_COUNT = 5;

    private Lottery lottery;

    /**
     * Creates a fresh, non-persisted lottery before every test.
     *
     * <p>The lottery does not receive an ID here because IDs are now generated
     * by the database when the aggregate is persisted.</p>
     */
    @BeforeEach
    void setUp() {
        lottery = new Lottery(EVENT_ID, WINNERS_COUNT);
    }

    /**
     * Verifies that registering a new member adds the member to the
     * participants collection.
     */
    @Test
    void GivenNewMember_WhenRegisterMember_ThenAddedToParticipants() {
        lottery.registerMember(10L);

        assertTrue(
                lottery.getRegisteredMemberIds().contains(10L),
                "Member should be in the participants list"
        );

        assertEquals(
                1,
                lottery.getRegisteredMemberIds().size(),
                "There should be exactly one participant"
        );
    }

    /**
     * Verifies that a member cannot register more than once to the same
     * lottery.
     */
    @Test
    void GivenAlreadyRegisteredMember_WhenRegisterMember_ThenThrowsException() {
        lottery.registerMember(10L);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> lottery.registerMember(10L)
        );

        assertEquals(
                "Member is already registered for this lottery.",
                exception.getMessage()
        );
    }

    /**
     * Verifies that a registered member may be marked as a winner.
     */
    @Test
    void GivenRegisteredMember_WhenSetWinner_ThenNoExceptionThrown() {
        lottery.registerMember(20L);

        assertDoesNotThrow(
                () -> lottery.setWinner(20L, "WINNER123")
        );
    }

    /**
     * Verifies that an unregistered member cannot be marked as a winner.
     */
    @Test
    void GivenUnregisteredMember_WhenSetWinner_ThenThrowsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> lottery.setWinner(99L, "WINNER123")
        );

        assertEquals(
                "Member ID not found in registrations.",
                exception.getMessage()
        );
    }

    /**
     * Verifies the initial business state of a newly created lottery.
     */
    @Test
    void GivenNewLottery_WhenGettersCalled_ThenReturnCorrectValues() {
        assertEquals(EVENT_ID, lottery.getEventId());
        assertEquals(WINNERS_COUNT, lottery.getWinnersNumber());
        assertEquals(LotteryStatus.OPEN, lottery.getStatus());
        assertTrue(lottery.getRegistrations().isEmpty());
        assertTrue(lottery.getRegisteredMemberIds().isEmpty());
        assertTrue(lottery.getWinners().isEmpty());
    }

    /**
     * Verifies that persistence-related values remain unset before the entity
     * is stored by JPA.
     */
    @Test
    void GivenNewLottery_WhenNotPersisted_ThenIdAndVersionAreNull() {
        assertNull(
                lottery.getLotteryId(),
                "A new lottery should not have an ID before persistence"
        );

        assertNull(
                lottery.getVersion(),
                "A new lottery should not have a version before persistence"
        );
    }

    /**
     * Verifies that the associated event identifier may be updated.
     */
    @Test
    void GivenLottery_WhenSetEventId_ThenEventIdIsUpdated() {
        lottery.setEventId(200L);

        assertEquals(200L, lottery.getEventId());
    }

    /**
     * Verifies that the requested number of winners may be updated.
     */
    @Test
    void GivenLottery_WhenSetWinnersNumber_ThenWinnersNumberIsUpdated() {
        lottery.setWinnersNumber(10);

        assertEquals(10, lottery.getWinnersNumber());
    }

    /**
     * Verifies that the lottery lifecycle status may be changed.
     */
    @Test
    void GivenLottery_WhenSetStatus_ThenStatusIsUpdated() {
        lottery.setStatus(LotteryStatus.CLOSED);

        assertEquals(LotteryStatus.CLOSED, lottery.getStatus());
    }

    /**
     * Verifies that several distinct members may register successfully.
     */
    @Test
    void GivenOpenLottery_WhenRegisterMultipleMembers_ThenAllMembersAreRegistered() {
        lottery.registerMember(10L);
        lottery.registerMember(20L);

        assertEquals(2, lottery.getRegisteredMemberIds().size());
        assertTrue(lottery.getRegisteredMemberIds().contains(10L));
        assertTrue(lottery.getRegisteredMemberIds().contains(20L));
    }

    /**
     * Verifies that registration is rejected after the lottery is closed.
     */
    @Test
    void GivenClosedLottery_WhenRegisterMember_ThenThrowsException() {
        lottery.setStatus(LotteryStatus.CLOSED);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> lottery.registerMember(10L)
        );

        assertEquals(
                "Registration is closed for this lottery.",
                exception.getMessage()
        );
    }

    /**
     * Verifies that the correct winner code is accepted.
     */
    @Test
    void GivenRegisteredWinner_WhenValidateWinnerCodeWithCorrectCode_ThenReturnTrue() {
        lottery.registerMember(20L);
        lottery.setWinner(20L, "WINNER123");

        assertTrue(
                lottery.validateWinnerCode(20L, "WINNER123")
        );
    }

    /**
     * Verifies that an incorrect winner code is rejected.
     */
    @Test
    void GivenRegisteredWinner_WhenValidateWinnerCodeWithWrongCode_ThenReturnFalse() {
        lottery.registerMember(20L);
        lottery.setWinner(20L, "WINNER123");

        assertFalse(
                lottery.validateWinnerCode(20L, "WRONG_CODE")
        );
    }

    /**
     * Verifies that a registered participant who did not win cannot use a
     * lottery code.
     */
    @Test
    void GivenRegisteredNonWinner_WhenValidateWinnerCode_ThenReturnFalse() {
        lottery.registerMember(20L);

        assertFalse(
                lottery.validateWinnerCode(20L, "WINNER123")
        );
    }

    /**
     * Verifies that an unregistered member cannot use a lottery code.
     */
    @Test
    void GivenUnregisteredMember_WhenValidateWinnerCode_ThenReturnFalse() {
        assertFalse(
                lottery.validateWinnerCode(99L, "WINNER123")
        );
    }

    /**
     * Verifies that only members marked as winners appear in the winners list.
     */
    @Test
    void GivenRegisteredWinner_WhenGetWinners_ThenReturnWinnerMemberId() {
        lottery.registerMember(20L);
        lottery.registerMember(30L);

        lottery.setWinner(20L, "WINNER123");

        assertEquals(1, lottery.getWinners().size());
        assertTrue(lottery.getWinners().contains(20L));
        assertFalse(lottery.getWinners().contains(30L));
    }

    /**
     * Verifies that a lottery without selected winners returns an empty list.
     */
    @Test
    void GivenNoWinners_WhenGetWinners_ThenReturnEmptyList() {
        lottery.registerMember(20L);

        assertTrue(lottery.getWinners().isEmpty());
    }

    /**
     * Verifies that all created registrations are exposed through the
     * aggregate's defensive registration snapshot.
     */
    @Test
    void GivenRegisteredMembers_WhenGetRegistrations_ThenReturnRegistrationsList() {
        lottery.registerMember(10L);
        lottery.registerMember(20L);

        assertEquals(2, lottery.getRegistrations().size());
    }
}