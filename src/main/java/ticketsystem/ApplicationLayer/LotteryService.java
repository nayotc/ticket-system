package ticketsystem.ApplicationLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Collections;
import ticketsystem.DomainLayer.IRepository.ILotteryRepository;
import ticketsystem.DomainLayer.lottery.Lottery;
import ticketsystem.DomainLayer.lottery.LotteryStatus;

public class LotteryService {
    
    private final ILotteryRepository lotteryRepository;
    private final ITokenService tokenService;


    public LotteryService(ILotteryRepository lotteryRepository, ITokenService tokenService) {
        this.lotteryRepository = lotteryRepository;  
        this.tokenService = tokenService; 
    }

    // Method to create a new lottery
    public void addLottery(long eventId, int winnersNumber) {
        try {
            if (winnersNumber <= 0) {
                throw new IllegalArgumentException("Number of winners must be greater than zero.");
            }
            long lotteryId = lotteryRepository.generateNextLotteryId();
            lotteryRepository.addLottery(new Lottery(lotteryId, eventId, winnersNumber));

        } catch (IllegalArgumentException e) {
            throw(e);
        }

    }

    // Method to register a member for a lottery
    public boolean registerMemberToLottery(String token, long lotteryId) {
        try{        
            long memberId = tokenService.extractUserId(token);
            Lottery lottery =lotteryRepository.findById(lotteryId);
            lottery.registerMember(memberId);
            lotteryRepository.update(lottery);
            return true; 
        } catch(IllegalArgumentException e){
            throw(e);
        }

    }

    // Method to close lottery registration
    public void closeLotteryRegistration(long lotteryId) {
        try{
            Lottery lottery = lotteryRepository.findById(lotteryId);
            lottery.setStatus(LotteryStatus.CLOSED);
            lotteryRepository.update(lottery);
        }
        catch(IllegalArgumentException e){
            throw(e);
        }
    }

    // Method to conduct the lottery draw and select winners
    public void conductLotteryDraw(long lotteryId, int numberOfWinners) {
        try{
            Lottery lottery = lotteryRepository.findById(lotteryId);
            List<Long> winningMemberIds = selectRandomWinners(lottery.getRegisteredMemberIds(), numberOfWinners);

            for (long memberId : winningMemberIds) {
                // Generate a unique authentication code for the winner
                String uniqueCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

                lottery.setWinner(memberId, uniqueCode);
                
            }
            lottery.setStatus(LotteryStatus.COMPLETED);
            lotteryRepository.update(lottery);
            
        }
        catch(IllegalArgumentException e){
            throw(e);
        }
    }

    // Helper method to select random winners from the list of registered members
    private List<Long> selectRandomWinners(List<Long> allRegisteredMembers, int numberOfWinners) {
       if (allRegisteredMembers == null || allRegisteredMembers.isEmpty() || numberOfWinners <= 0) {
            return new ArrayList<>(); 
        }

        if (allRegisteredMembers.size() <= numberOfWinners) {
            return new ArrayList<>(allRegisteredMembers);
        }
        // Shuffle the list of registered members and select the top N winners
        List<Long> winnersPool = new ArrayList<>(allRegisteredMembers);
        Collections.shuffle(winnersPool);
        
        return new ArrayList<>(winnersPool.subList(0, numberOfWinners));
    }
}
