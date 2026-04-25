package ticketsystem.DomainLayer.IRepository;

import ticketsystem.DomainLayer.user.User;

public interface IUserRepository {
    boolean addGuest(String sessionToken, User user);
    void removeGuest(String sessionToken);
    boolean isActiveGuest(String sessionToken);
    int getTotalActiveSessions();
}
