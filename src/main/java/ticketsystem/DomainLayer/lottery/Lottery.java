package ticketsystem.DomainLayer.lottery;

import java.util.ArrayList;
import java.util.List;

public class Lottery {
    private long lotteryId;
    private long eventId;
    private int winnersNumber;
    private LotteryStatus status;
    private List<LotteryRegistration> registrations;
    
    public Lottery(long lotteryId, long eventId, int winnersNumber) {
        this.lotteryId = lotteryId;
        this.eventId = eventId;
        this.winnersNumber = winnersNumber;
        this.status = LotteryStatus.OPEN;
        registrations = new ArrayList<>();
    }

    public long getLotteryId() {
        return lotteryId;
    }

    public void setLotteryId(long lotteryId) {
        this.lotteryId = lotteryId;
    }

    public long getEventId() {
        return eventId;
    }

    public void setEventId(long eventId) {
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

    public List<LotteryRegistration> getRegistrations() {
        return registrations;
    }

    // Method to register a member for the lottery
    public synchronized void registerMember(long memberId) {
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

    public List<Long> getRegisteredMemberIds() {
        List<Long> memberIds = new ArrayList<>();
        for (LotteryRegistration registration : registrations) {
            memberIds.add(registration.getMemberId());
        }
        return memberIds;
    }

    // Method to mark a member as a winner and generate an authentication codev
    public synchronized void setWinner(long memberId, String authCode) {
        for (LotteryRegistration registration : registrations) {
            if (registration.getMemberId() == memberId) {
                registration.markAsWinner(authCode);
                return;
            }
        }
        throw new IllegalArgumentException("Member ID not found in registrations.");
    }

    
}
