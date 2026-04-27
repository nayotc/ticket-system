package ticketsystem.DomainLayer.IRepository;

import java.util.List;

import ticketsystem.DomainLayer.lottery.Lottery;

public interface ILotteryRepository {
    void addLottery(Lottery lottery);
    boolean isMemberRegistered(int memberId, int lotteryId);
    void addRegistration(int memberId, int lotteryId);
    List<Integer> getAllRegisteredMembers(int lotteryId);
    int findMaxLotteryId();
}
