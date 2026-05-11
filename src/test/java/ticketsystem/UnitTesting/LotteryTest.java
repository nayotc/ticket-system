package ticketsystem.UnitTesting;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.DomainLayer.lottery.Lottery;

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
}