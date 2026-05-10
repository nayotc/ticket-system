package ticketsystem.DomainLayer.lottery;

//@Entity
public class LotteryRegistration {
    private long memberId;
    private boolean isWinner; 
    private String authCode; 

    public LotteryRegistration(long memberId) {
        this.memberId = memberId;
        this.isWinner = false;
        this.authCode = null;
    }

    public long getMemberId() {
        return memberId;
    }

    public void setMemberId(long memberId) {
        this.memberId = memberId;
    }
    public boolean isWinner() {
        return isWinner;
    }
    public String getAuthCode() {
        return authCode;
    }
    // Method to mark this registration as a winner and generate an authentication code
    public void markAsWinner(String generatedCode) {
        this.isWinner = true;
        this.authCode = generatedCode;
    }
    

}
