package ticketsystem.InfrastructureLayer;

import java.util.List;
import java.util.Map;

import ticketsystem.DomainLayer.IRepository.ILotteryRepository;
import ticketsystem.DomainLayer.lottery.Lottery;

public class LotteryRepository implements ILotteryRepository {
    private int counter;
    private static LotteryRepository instance;
    private Map<Integer, Lottery> allLotteries;

    public LotteryRepository() {
        this.counter = 1;
        this.allLotteries = new java.util.HashMap<>();
    }
    
    public static LotteryRepository getInstance() {
        if (instance == null) {
            instance = new LotteryRepository();
        }
        return instance;
    }
    
    @Override
    public void addLottery(Lottery lottery) {
        allLotteries.put(lottery.getLotteryId(), lottery);
        counter++;
    }

    @Override
    public int findMaxLotteryId() {
        return counter; 
    }

    @Override
    public boolean isMemberRegistered(int memberId, int lotteryId) {
        return false; // Placeholder return value
    }

    @Override
    public void addRegistration(int memberId, int lotteryId) {
        // Implementation to add a registration for the member in the lottery
    }

    @Override
    public List<Integer> getAllRegisteredMembers(int lotteryId) {
        // Implementation to retrieve all registered members for the lottery
        return null; // Placeholder return value
    }

    
}
