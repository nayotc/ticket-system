package ticketsystem.UnitTesting;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.DomainLayer.lottery.Lottery;
import ticketsystem.DomainLayer.lottery.LotteryStatus;

public class LotteryTest {

    private Lottery lottery;
    private final long lotteryId = 1L;
    private final long eventId = 100L;
    private final int winnersCount = 5;

    @BeforeEach
    void setUp() {
        lottery = new Lottery(lotteryId, eventId, winnersCount);
    }

    @Test
    void GivenNewMember_WhenRegisterMember_ThenAddedToParticipants() {
        // Act
        lottery.registerMember(10L);

        // Assert
        assertTrue(lottery.getRegisteredMemberIds().contains(10L), "Member should be in the participants list");
        assertEquals(1, lottery.getRegisteredMemberIds().size(), "There should be exactly 1 participant");
    }

    @Test
    void GivenAlreadyRegisteredMember_WhenRegisterMember_ThenThrowsException() {
        // Arrange
        lottery.registerMember(10L);

        // Act & Assert
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            lottery.registerMember(10L);
        });
        assertEquals("Member is already registered for this lottery.", exception.getMessage());
    }

    @Test
    void GivenRegisteredMember_WhenSetWinner_ThenNoExceptionThrown() {
        // Arrange
        lottery.registerMember(20L);
        String code = "WINNER123";

        // Act & Assert 
        assertDoesNotThrow(() -> {
            lottery.setWinner(20L, code);
        });
    }

    @Test
    void GivenUnregisteredMember_WhenSetWinner_ThenThrowsException() {
        // Arrange
        String code = "WINNER123";

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            lottery.setWinner(99L, code); //not registered
        });
        assertEquals("Member ID not found in registrations.", exception.getMessage());
    }
    @Test
void GivenNewLottery_WhenGettersCalled_ThenReturnCorrectValues() {
    // Act & Assert
    assertEquals(lotteryId, lottery.getLotteryId());
    assertEquals(eventId, lottery.getEventId());
    assertEquals(winnersCount, lottery.getWinnersNumber());
    assertEquals(LotteryStatus.OPEN, lottery.getStatus());
}

@Test
void GivenLottery_WhenSetLotteryId_ThenLotteryIdIsUpdated() {
    // Act
    lottery.setLotteryId(2L);

    // Assert
    assertEquals(2L, lottery.getLotteryId());
}

@Test
void GivenLottery_WhenSetEventId_ThenEventIdIsUpdated() {
    // Act
    lottery.setEventId(200L);

    // Assert
    assertEquals(200L, lottery.getEventId());
}

@Test
void GivenLottery_WhenSetWinnersNumber_ThenWinnersNumberIsUpdated() {
    // Act
    lottery.setWinnersNumber(10);

    // Assert
    assertEquals(10, lottery.getWinnersNumber());
}

@Test
void GivenLottery_WhenSetStatus_ThenStatusIsUpdated() {
    // Act
    lottery.setStatus(LotteryStatus.CLOSED);

    // Assert
    assertEquals(LotteryStatus.CLOSED, lottery.getStatus());
}

@Test
void GivenOpenLottery_WhenRegisterMultipleMembers_ThenAllMembersAreRegistered() {
    // Act
    lottery.registerMember(10L);
    lottery.registerMember(20L);

    // Assert
    assertEquals(2, lottery.getRegisteredMemberIds().size());
    assertTrue(lottery.getRegisteredMemberIds().contains(10L));
    assertTrue(lottery.getRegisteredMemberIds().contains(20L));
}

@Test
void GivenClosedLottery_WhenRegisterMember_ThenThrowsException() {
    // Arrange
    lottery.setStatus(LotteryStatus.CLOSED);

    // Act & Assert
    Exception exception = assertThrows(IllegalStateException.class, () ->
            lottery.registerMember(10L)
    );

    assertEquals("Registration is closed for this lottery.", exception.getMessage());
}

@Test
void GivenRegisteredWinner_WhenValidateWinnerCodeWithCorrectCode_ThenReturnTrue() {
    // Arrange
    lottery.registerMember(20L);
    lottery.setWinner(20L, "WINNER123");

    // Act & Assert
    assertTrue(lottery.validateWinnerCode(20L, "WINNER123"));
}

@Test
void GivenRegisteredWinner_WhenValidateWinnerCodeWithWrongCode_ThenReturnFalse() {
    // Arrange
    lottery.registerMember(20L);
    lottery.setWinner(20L, "WINNER123");

    // Act & Assert
    assertFalse(lottery.validateWinnerCode(20L, "WRONG_CODE"));
}

@Test
void GivenRegisteredNonWinner_WhenValidateWinnerCode_ThenReturnFalse() {
    // Arrange
    lottery.registerMember(20L);

    // Act & Assert
    assertFalse(lottery.validateWinnerCode(20L, "WINNER123"));
}

@Test
void GivenUnregisteredMember_WhenValidateWinnerCode_ThenReturnFalse() {
    // Act & Assert
    assertFalse(lottery.validateWinnerCode(99L, "WINNER123"));
}

@Test
void GivenRegisteredWinner_WhenGetWinners_ThenReturnWinnerMemberId() {
    // Arrange
    lottery.registerMember(20L);
    lottery.registerMember(30L);

    lottery.setWinner(20L, "WINNER123");

    // Act & Assert
    assertEquals(1, lottery.getWinners().size());
    assertTrue(lottery.getWinners().contains(20L));
    assertFalse(lottery.getWinners().contains(30L));
}

@Test
void GivenNoWinners_WhenGetWinners_ThenReturnEmptyList() {
    // Arrange
    lottery.registerMember(20L);

    // Act & Assert
    assertTrue(lottery.getWinners().isEmpty());
}

@Test
void GivenRegisteredMembers_WhenGetRegistrations_ThenReturnRegistrationsList() {
    // Arrange
    lottery.registerMember(10L);
    lottery.registerMember(20L);

    // Act & Assert
    assertEquals(2, lottery.getRegistrations().size());
}
}