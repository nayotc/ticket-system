package ticketsystem.DomainLayer.IRepository;

import java.util.List;

import ticketsystem.DomainLayer.user.Member;

public interface IUserRepository {

    boolean addRegisteredMember(long id, Member member, String password);

    boolean removeRegisteredMember(long id);

    boolean isIDTaken(long id);

    boolean isUsernameTaken(String username);

    int getAllRegisteredMembersCount();

    Member getMemberByUsername(String username);

    Member getMemberById(long id);

    String getHashedPasswordByUsername(String username);

    boolean updateRegisteredMemberUsername(String username, String newUsername);

    boolean updateRegisteredMemberPassword(String username, String newHashedPassword);

    boolean updateMember(Member member);

    List<Member> getAllMembers();
}
