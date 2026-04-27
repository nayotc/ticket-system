package ticketsystem.DomainLayer.lottery;

import java.util.ArrayList;
import java.util.List;

public class Lottery {
    private int lotteryId;
    private int eventId;
    private int winnersNumber;
    private LotteryStatus status;
    private List<LotteryRegistration> registrations;
    
    public Lottery(int lotteryId, int eventId, int winnersNumber) {
        this.lotteryId = lotteryId;
        this.eventId = eventId;
        this.winnersNumber = winnersNumber;
        this.status = LotteryStatus.OPEN;
        registrations = new ArrayList<>();
    }

    public int getLotteryId() {
        return lotteryId;
    }

    public void setLotteryId(int lotteryId) {
        this.lotteryId = lotteryId;
    }

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public int getWinnersNumber() {
        return winnersNumber;
    }

    public void setWinnersNumber(int winnersNumber) {
        this.winnersNumber = winnersNumber;
    }

    public LotteryStatus getStatus() {
        return status;
    }

    public void setStatus(LotteryStatus status) {
        this.status = status;
    }

    public void registerMember(int memberId) {
        if (this.status != LotteryStatus.OPEN) {
            throw new IllegalStateException("Registration is closed for this lottery.");
        }
        boolean alreadyRegistered = registrations.stream()
                .anyMatch(reg -> reg.getMemberId() == memberId);
        if (alreadyRegistered) {
            throw new IllegalStateException("Member is already registered for this lottery.");
        }
        LotteryRegistration newRegistration = new LotteryRegistration(memberId);
        this.registrations.add(newRegistration);
    }

    
}
