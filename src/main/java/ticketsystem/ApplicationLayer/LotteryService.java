package ticketsystem.ApplicationLayer;

import java.util.List;

import ticketsystem.DomainLayer.IRepository.ILotteryRepository;
import ticketsystem.DomainLayer.lottery.Lottery;

public class LotteryService {
    
    private final ILotteryRepository lotteryRepository;

    public LotteryService(ILotteryRepository lotteryRepository) {
        this.lotteryRepository = lotteryRepository;   
    }

    public void addLottery(int eventId, int winnersNumber) {
        int lotteryId = lotteryRepository.findMaxLotteryId();
        lotteryRepository.addLottery(new Lottery(lotteryId, eventId, winnersNumber));
    }
    public boolean registerMemberToLottery(int memberId, int lotteryId) {
        if (lotteryRepository.isMemberRegistered(memberId, lotteryId)) {
            return false;
        }
        lotteryRepository.addRegistration(memberId, lotteryId);
        return true; 
    }

     public boolean isMemberRegistered(int memberId, int lotteryId) {
        return lotteryRepository.isMemberRegistered(memberId, lotteryId);
    }

     public List<Integer> getAllRegisteredMembers(int lotteryId) {
        return lotteryRepository.getAllRegisteredMembers(lotteryId);
    }

    
}
