package ticketsystem.InfrastructureLayer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import ticketsystem.DomainLayer.user.Member;

public class UserRepository implements ticketsystem.DomainLayer.IRepository.IUserRepository {

    private Map<Long, Member> registeredMembersMap;
    private Map<String, String> hashedPasswordsMap;
    private PasswordEncoder passwordEncoder;
    private Map<String, Long> usernameToIdMap;

    public UserRepository() {
        this.registeredMembersMap = new ConcurrentHashMap<>();
        this.hashedPasswordsMap = new ConcurrentHashMap<>();
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.usernameToIdMap = new ConcurrentHashMap<>();
    }

    @Override
    public boolean addRegisteredMember(long id, Member member, String password) {
        if (usernameToIdMap.putIfAbsent(member.getUserName(), id) != null) {
            return false; // Username already exists, cannot add member
        }
        if (registeredMembersMap.putIfAbsent(id, member) != null) {
            // Rollback: Remove the username mapping if the ID is already taken
            usernameToIdMap.remove(member.getUserName());
            return false;
        }

        // Store the hashed password
        hashedPasswordsMap.put(member.getUserName(), hashPassword(password));

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
    public boolean isMemberDetailsCorrect(String username, String password) {
        String hashed = hashedPasswordsMap.get(username);
        return hashed != null && verifyPassword(password, hashed);
    }

    private String hashPassword(String password) {
        return passwordEncoder.encode(password);
    }

    private boolean verifyPassword(String password, String hashedPassword) {
        return passwordEncoder.matches(password, hashedPassword);
    }

}
