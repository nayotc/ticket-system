package ticketsystem.DomainLayer.lottery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Lottery {
    private long lotteryId;
    private long eventId;
    private int winnersNumber;
    private LotteryStatus status;
    private Map<Long, LotteryRegistration> registrations = new HashMap<>();
    
    public Lottery(long lotteryId, long eventId, int winnersNumber) {
        this.lotteryId = lotteryId;
        this.eventId = eventId;
        this.winnersNumber = winnersNumber;
        this.status = LotteryStatus.OPEN;
        registrations = new HashMap<>();
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
        return new ArrayList<>(registrations.values());
    }

    public List<Long> getWinners() {
    return registrations.values().stream()           
            .filter(LotteryRegistration::isWinner)    
            .map(LotteryRegistration::getMemberId)    
            .collect(Collectors.toList());           
    }

    // Method to register a member for the lottery
    public synchronized void registerMember(long memberId) {
        if (this.status != LotteryStatus.OPEN) {
            throw new IllegalStateException("Registration is closed for this lottery.");
        }
        boolean alreadyRegistered = registrations.values().stream()
                .anyMatch(reg -> reg.getMemberId() == memberId);
        if (alreadyRegistered) {
            throw new IllegalStateException("Member is already registered for this lottery.");
        }
        LotteryRegistration newRegistration = new LotteryRegistration(memberId);
        this.registrations.put(memberId, newRegistration);
    }

    public List<Long> getRegisteredMemberIds() {
        return new ArrayList<>(registrations.keySet());
    }

    // Method to mark a member as a winner and generate an authentication codev
    public synchronized void setWinner(long memberId, String authCode) {
        LotteryRegistration registration = registrations.get(memberId);
        if (registration != null) {
            registration.markAsWinner(authCode);
        } else {
            throw new IllegalArgumentException("Member ID not found in registrations.");
        }
    }

    // Method to validate a winner's authentication code
    public synchronized boolean validateWinnerCode(long memberId, String inputCode) {
        LotteryRegistration reg = registrations.get(memberId);
        // If the member is not registered or not a winner, return false
        if (reg == null || !reg.isWinner()) {
            return false;
        }
        
        return inputCode.equals(reg.getAuthCode());
    }

    
}
