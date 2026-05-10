package ticketsystem.InfrastructureLayer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import ticketsystem.DomainLayer.IRepository.ILotteryRepository;
import ticketsystem.DomainLayer.lottery.Lottery;

public class LotteryRepository implements ILotteryRepository {
    
    private final AtomicLong counter = new AtomicLong(1);
    private static final LotteryRepository instance = new LotteryRepository();
    private final Map<Long, Lottery> allLotteries = new ConcurrentHashMap<>();

    private LotteryRepository() { }
    
    public static LotteryRepository getInstance() {
        return instance;
    }
    
    @Override
    public Lottery findById(long lotteryId) {   
        return allLotteries.get(lotteryId);
    }
    
    @Override
    public void addLottery(Lottery lottery) {
        allLotteries.put(lottery.getLotteryId(), lottery);
    }

    @Override
    public long generateNextLotteryId() {
        return counter.getAndIncrement(); 
    }

    @Override
    public void update(Lottery lottery) {
        if (!allLotteries.containsKey(lottery.getLotteryId())) {
            throw new IllegalArgumentException("Lottery with ID " + lottery.getLotteryId() + " does not exist.");
        }
        allLotteries.put(lottery.getLotteryId(), lottery);
    }
}