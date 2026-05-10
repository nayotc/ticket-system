package ticketsystem.DomainLayer.IRepository;

import ticketsystem.DomainLayer.lottery.Lottery;

public interface ILotteryRepository {
    Lottery findById(long lotteryId);
    void addLottery(Lottery lottery);
    long generateNextLotteryId();
    void update(Lottery lottery);

}
