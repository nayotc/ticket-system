package ticketsystem.DomainLayer.IRepository;

import ticketsystem.DomainLayer.user.Member;
import ticketsystem.DomainLayer.user.User;

public interface IUserRepository {

    boolean addRegisteredMember(long id, User user, String password);

    boolean removeRegisteredMember(long id);

    boolean isIDTaken(long id);

    boolean isUsernameTaken(String username);

    int getAllRegisteredMembersCount();

    Member getMemberByUsername(String username);

    Member getMemberById(long id);

    boolean isUserDetailsCorrect(String username, String password);

}
