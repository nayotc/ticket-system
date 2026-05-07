package ticketsystem.InfrastructureLayer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ticketsystem.DomainLayer.user.Member;

public class UserRepository implements ticketsystem.DomainLayer.IRepository.IUserRepository {

    private Map<Long, Member> registeredMembersMap;
    private Map<String, String> hashedPasswordsMap;
    private Map<String, Long> usernameToIdMap;

    public UserRepository() {
        this.registeredMembersMap = new ConcurrentHashMap<>();
        this.usernameToIdMap = new ConcurrentHashMap<>();
        this.hashedPasswordsMap = new ConcurrentHashMap<>();
    }

    @Override
    public boolean addRegisteredMember(long id, Member member, String hashedPassword) {
        if (usernameToIdMap.putIfAbsent(member.getUserName(), id) != null) {
            return false; // Username already exists, cannot add member
        }
        if (registeredMembersMap.putIfAbsent(id, member) != null) {
            // Rollback: Remove the username mapping if the ID is already taken
            usernameToIdMap.remove(member.getUserName());
            return false;
        }

        // Store the hashed password
        hashedPasswordsMap.put(member.getUserName(), hashedPassword);

        return true;
    }

    @Override
    public boolean removeRegisteredMember(long id) {
        Member removedMember = registeredMembersMap.remove(id);

        if (removedMember == null) {
            return false; // No member with the given ID was found, nothing to remove
        }
        String username = removedMember.getUserName();
        usernameToIdMap.remove(username);
        hashedPasswordsMap.remove(username);

        return true;
    }

    @Override
    public boolean isIDTaken(long id) {
        return registeredMembersMap.containsKey(id);
    }

    @Override
    public boolean isUsernameTaken(String username) {
        return usernameToIdMap.containsKey(username);
    }

    @Override
    public int getAllRegisteredMembersCount() {
        return registeredMembersMap.size();
    }

    @Override
    public Member getMemberByUsername(String username) {
        return registeredMembersMap.get(usernameToIdMap.get(username));
    }

    @Override
    public Member getMemberById(long id) {
        return registeredMembersMap.get(id);
    }

    @Override
    public String getHashedPasswordByUsername(String username) {
        return hashedPasswordsMap.get(username);
    }

}
