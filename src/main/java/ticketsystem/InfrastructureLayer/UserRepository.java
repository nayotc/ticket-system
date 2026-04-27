package ticketsystem.InfrastructureLayer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ticketsystem.DomainLayer.user.Member;
import ticketsystem.DomainLayer.user.User;

public class UserRepository implements ticketsystem.DomainLayer.IRepository.IUserRepository {
    private Map<String, User> ActiveSessionsMap;
    private Map<Long, User> RegisteredMembersMap;

    public UserRepository() {
        this.ActiveSessionsMap = new ConcurrentHashMap<>();
        this.RegisteredMembersMap = new ConcurrentHashMap<>();
    }
    @Override
    public boolean addRegisteredMember(long id, User user) {
        if (RegisteredMembersMap.containsKey(id)) {
            return false; // Member ID already exists, cannot add member
        }
        RegisteredMembersMap.put(id, user);
        return true; // Member added successfully
    }
    @Override
    public boolean removeRegisteredMember(long id) {
        if (!RegisteredMembersMap.containsKey(id)) {
            return false; // Member ID does not exist, cannot remove member
        }
        RegisteredMembersMap.remove(id);
        return true; // Member removed successfully
    }

    @Override
    public boolean isIDTaken(long id) {
        return RegisteredMembersMap.containsKey(id);
    }

    @Override
    public boolean isUsernameTaken(String username) {
        return RegisteredMembersMap.values().stream()
                .filter(user -> user instanceof Member)
                .map(user -> (Member) user)
                .anyMatch(member -> member.getUserName().equals(username));
    }

    @Override
    public boolean addActiveSession(String sessionToken, User user) {
        if (ActiveSessionsMap.containsKey(sessionToken)) {
            return false; // Session token already exists, cannot add guest
        }
        ActiveSessionsMap.put(sessionToken, user);
        return true;
    }

    @Override
    public boolean isActiveSession(String sessionToken) {
        return ActiveSessionsMap.containsKey(sessionToken);
    }

    @Override
    public int getTotalActiveSessions() {
        return ActiveSessionsMap.size();
    }

    @Override
    public void removeActiveSession(String sessionToken) {
        ActiveSessionsMap.remove(sessionToken);
    }
    public int getAllRegisteredMembersCount() {
        return RegisteredMembersMap.size();
    }

}
