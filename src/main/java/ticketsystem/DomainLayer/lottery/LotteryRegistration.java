package ticketsystem.DomainLayer.lottery;

//@Entity
public class LotteryRegistration {
   // @Id
   // @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private int memberId;

    public LotteryRegistration(int memberId) {
        this.memberId = memberId;
        this.id   = 0 ; // need to write more code logic here
    }

    public int getMemberId() {
        return memberId;
    }

    public void setMemberId(int memberId) {
        this.memberId = memberId;
    }

    public int getLotteryId() {
        return id;
    }

    public void setLotteryId(int lotteryId) {
        this.id = lotteryId;
    }

}
