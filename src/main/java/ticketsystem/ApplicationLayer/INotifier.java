package ticketsystem.ApplicationLayer;

public interface INotifier {

    void notifyMember(Long memberId, String message);

    void notifyGuest(String guestToken, String message);
}
