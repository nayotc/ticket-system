package ticketsystem.InfrastructureLayer;

import java.util.HashMap;
import java.util.Map;

import ticketsystem.DomainLayer.user.Member;

public class UserRepository implements ticketsystem.DomainLayer.IRepository.IUserRepository {

    private Map<Long, Member> registeredMembersMap;
    private Map<String, String> hashedPasswordsMap;
    private Map<String, Long> usernameToIdMap;

    public UserRepository() {
        this.registeredMembersMap = new HashMap<>();
        this.usernameToIdMap = new HashMap<>();
        this.hashedPasswordsMap = new HashMap<>();
    }

    @Override
    public synchronized boolean addRegisteredMember(long id, Member member, String hashedPassword) {
        if (registeredMembersMap.containsKey(id) || usernameToIdMap.containsKey(member.getUserName())) {
            return false; // ID or username already exists, cannot add member
        }
        registeredMembersMap.put(id, member);
        usernameToIdMap.put(member.getUserName(), id);
        hashedPasswordsMap.put(member.getUserName(), hashedPassword);
        return true;
    }

    @Override
    public synchronized boolean removeRegisteredMember(long id) {
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
    public synchronized boolean isIDTaken(long id) {
        return registeredMembersMap.containsKey(id);
    }

    @Override
    public synchronized boolean isUsernameTaken(String username) {
        return usernameToIdMap.containsKey(username);
    }

    @Override
    public synchronized int getAllRegisteredMembersCount() {
        return registeredMembersMap.size();
    }

    @Override
    public synchronized Member getMemberByUsername(String username) {
        return registeredMembersMap.get(usernameToIdMap.get(username));
    }

    @Override
    public synchronized Member getMemberById(long id) {
        return registeredMembersMap.get(id);
    }

    @Override
    public synchronized String getHashedPasswordByUsername(String username) {
        return hashedPasswordsMap.get(username);
    }

    @Override
    public synchronized boolean updateRegisteredMemberUsername(String username, String newUsername) {
        if (!usernameToIdMap.containsKey(username)) {
            return false;
        }
        if (newUsername == null || newUsername.isBlank()) {
            return false;
        }
        if (!username.equals(newUsername) && usernameToIdMap.containsKey(newUsername)) {
            return false;
        }
        long id = usernameToIdMap.get(username);
        Member member = registeredMembersMap.get(id);
        if (member == null) {
            return false;
        }
        if (username.equals(newUsername)) {
            return true;
        }
        String hashedPassword = hashedPasswordsMap.remove(username);
        member.setUserName(newUsername);
        usernameToIdMap.remove(username);
        usernameToIdMap.put(newUsername, id);
        hashedPasswordsMap.put(newUsername, hashedPassword);
        return true;
    }

    @Override
    public synchronized boolean updateRegisteredMemberPassword(String username, String newHashedPassword) {
        if (!usernameToIdMap.containsKey(username)) {
            return false;
        }
        if (newHashedPassword == null || newHashedPassword.isBlank()) {
            return false;
        }
        if (!hashedPasswordsMap.containsKey(username)) {
            return false;
        }
        hashedPasswordsMap.put(username, newHashedPassword);
        return true;
    }

}
