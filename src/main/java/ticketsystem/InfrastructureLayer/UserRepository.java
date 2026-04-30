package ticketsystem.InfrastructureLayer;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ticketsystem.DomainLayer.user.Member;
import ticketsystem.DomainLayer.user.User;

public class UserRepository implements ticketsystem.DomainLayer.IRepository.IUserRepository {

    private Map<Long, User> RegisteredMembersMap;
    private Map<String, String> HashedPasswordsMap;
    private PasswordEncoder passwordEncoder;

    public UserRepository() {
        this.RegisteredMembersMap = new ConcurrentHashMap<>();
        this.HashedPasswordsMap = new ConcurrentHashMap<>();
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Override
    public boolean addRegisteredMember(long id, User user, String password) {
        if (RegisteredMembersMap.containsKey(id)) {
            return false; // Member ID already exists, cannot add member
        }
        RegisteredMembersMap.put(id, user);
        if (user instanceof Member member) {
            HashedPasswordsMap.put(member.getUserName(), hashPassword(password));
        }
        return true; // Member added successfully
    }

    @Override
    public boolean removeRegisteredMember(long id) {
        if (!RegisteredMembersMap.containsKey(id)) {
            return false; // Member ID does not exist, cannot remove member
        }
        User removed = RegisteredMembersMap.remove(id);
        if (removed instanceof Member member) {
            HashedPasswordsMap.remove(member.getUserName());
        }
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
    public int getAllRegisteredMembersCount() {
        return RegisteredMembersMap.size();
    }

    @Override
    public Member getMemberByUsername(String username) {
        return RegisteredMembersMap.values().stream()
                .filter(Member.class::isInstance)
                .map(Member.class::cast)
                .filter(member -> member.getUserName().equals(username))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Member getMemberById(long id) {
        User user = RegisteredMembersMap.get(id);
        if (user instanceof Member member) {
            return member;
        }
        return null;
    }

    @Override
    public boolean isUserDetailsCorrect(String username, String password) {
        String hashed = HashedPasswordsMap.get(username);
        return hashed != null && verifyPassword(password, hashed);
    }

    private String hashPassword(String password) {
        return passwordEncoder.encode(password);
    }

    private boolean verifyPassword(String password, String hashedPassword) {
        return passwordEncoder.matches(password, hashedPassword);
    }

}
