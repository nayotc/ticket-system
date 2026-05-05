package ticketsystem.DomainLayer.IRepository;

import ticketsystem.DomainLayer.user.User;

public interface ITokenRepository {
    boolean addActiveSession(String sessionToken, User user);
    boolean isActiveSession(String sessionToken);
    int getTotalActiveSessions();
    void removeActiveSession(String sessionToken);
}
