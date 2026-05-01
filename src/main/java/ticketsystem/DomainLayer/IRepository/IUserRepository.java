package ticketsystem.DomainLayer.IRepository;

import ticketsystem.DomainLayer.user.Member;

public interface IUserRepository {

    boolean addRegisteredMember(long id, Member member, String password);

    boolean removeRegisteredMember(long id);

    boolean isIDTaken(long id);

    boolean isUsernameTaken(String username);

    int getAllRegisteredMembersCount();

    Member getMemberByUsername(String username);

    Member getMemberById(long id);

    boolean isMemberDetailsCorrect(String username, String password);
}
