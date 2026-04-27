package ticketsystem.DomainLayer.IRepository;

import ticketsystem.DomainLayer.user.User;

public interface IUserRepository {
    boolean addActiveSession(String sessionToken, User user);

    void removeActiveSession(String sessionToken);

    boolean isActiveSession(String sessionToken);

    int getTotalActiveSessions();

    boolean addRegisteredMember(long id, User user);

    boolean removeRegisteredMember(long id);

    boolean isIDTaken(long id);

    boolean isUsernameTaken(String username);
    

}
