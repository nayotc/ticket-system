package ticketsystem.ApplicationLayer;

public interface INotifier {

    void notifyUser(String sessionId, String message);
}
