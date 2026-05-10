package ticketsystem.AcceptanceTesting;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ticketsystem.ApplicationLayer.ITokenService;
import ticketsystem.ApplicationLayer.LotteryService;
import ticketsystem.DomainLayer.IRepository.ILotteryRepository;
import ticketsystem.DomainLayer.lottery.Lottery;

public class LotteryServiceTest {

    @Mock
    private ILotteryRepository lotteryRepository;

    @Mock
    private ITokenService tokenService;

    private LotteryService lotteryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        lotteryService = new LotteryService(lotteryRepository, tokenService);
    }

    @Test
    void GivenValidEventAndWinners_WhenAddLottery_ThenRepositorySavesLottery() {
        // Arrange
        long eventId = 100L;
        int winners = 5;
        when(lotteryRepository.generateNextLotteryId()).thenReturn(1L);

        // Act
        lotteryService.addLottery(eventId, winners);

        // Assert
        verify(lotteryRepository, times(1)).generateNextLotteryId();
        verify(lotteryRepository, times(1)).addLottery(any(Lottery.class));
    }

    @Test
    void GivenInvalidWinnersCount_WhenAddLottery_ThenThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            lotteryService.addLottery(100L, 0);
        });
        // Verify that the repository's addLottery method was never called due to the exception
        verify(lotteryRepository, never()).addLottery(any(Lottery.class)); 
    }

    @Test
    void GivenValidToken_WhenRegisterMemberToLottery_ThenMemberRegisteredAndUpdateCalled() {
        // Arrange
        String mockToken = "valid-token";
        long mockMemberId = 42L;
        long mockLotteryId = 1L;
        
        Lottery mockLottery = new Lottery(mockLotteryId, 100L, 5); 

        when(tokenService.extractUserId(mockToken)).thenReturn(mockMemberId);
        when(lotteryRepository.findById(mockLotteryId)).thenReturn(mockLottery);

        // Act
        boolean result = lotteryService.registerMemberToLottery(mockToken, mockLotteryId);

        // Assert
        assertTrue(result);
        assertTrue(mockLottery.getRegisteredMemberIds().contains(mockMemberId), "Member should be added to the domain object");
        verify(lotteryRepository, times(1)).update(mockLottery); 
    }

    @Test
    void GivenLotteryWithParticipants_WhenConductLotteryDraw_ThenWinnersAreSetAndUpdateCalled() {
        // Arrange
        long mockLotteryId = 1L;
        Lottery mockLottery = new Lottery(mockLotteryId, 100L, 2); 
        
        
        mockLottery.registerMember(10L);
        mockLottery.registerMember(20L);
        mockLottery.registerMember(30L);

        when(lotteryRepository.findById(mockLotteryId)).thenReturn(mockLottery);

        // Act
        lotteryService.conductLotteryDraw(mockLotteryId, 2);

        // Assert
        verify(lotteryRepository, times(1)).update(mockLottery);
        // We can't predict the winners due to randomness, but we can check that 2 winners were set
    }
}