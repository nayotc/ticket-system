package ticketsystem.DomainLayer.IRepository;

public interface IUserRepository {
    void addGuest(String sessionToken);
    void removeGuest(String sessionToken);
    boolean isActiveGuest(String sessionToken);
    int getTotalActiveSessions();
}
