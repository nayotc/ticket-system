package ticketsystem.InfrastructureLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

import ticketsystem.DomainLayer.user.Member;

@Repository
public class UserRepository implements ticketsystem.DomainLayer.IRepository.IUserRepository {

    // Using ConcurrentHashMap to allow thread-safe concurrent access without locking the whole map for reads
    private ConcurrentHashMap<Long, Member> registeredMembersMap;
    private ConcurrentHashMap<String, String> hashedPasswordsMap;
    private ConcurrentHashMap<String, Long> usernameToIdMap;

    public UserRepository() {
        this.registeredMembersMap = new ConcurrentHashMap<>();
        this.usernameToIdMap = new ConcurrentHashMap<>();
        this.hashedPasswordsMap = new ConcurrentHashMap<>();
    }

    @Override
    public synchronized boolean addRegisteredMember(long id, Member member, String hashedPassword) {
        if (registeredMembersMap.containsKey(id) || usernameToIdMap.containsKey(member.getUserName())) {
            return false; // ID or username already exists, cannot add member
        }
        // Save a detached copy using the copy constructor to isolate the database state
        registeredMembersMap.put(id, new Member(member));
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
        Long id = usernameToIdMap.get(username);
        if (id == null) return null;
        return getMemberById(id);
    }

    @Override
    public Member getMemberById(long id) {
        Member dbMember = registeredMembersMap.get(id);
        if (dbMember != null) {
            // Return a detached deep copy so modifications don't affect the database until saved
            return new Member(dbMember);
        }
        return null;
    }

    @Override
    public String getHashedPasswordByUsername(String username) {
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

    @Override
    public boolean updateMember(Member targetMember) {
        long id = targetMember.getId();
        
        // Fetch current active member state from the map
        Member currentDbMember = registeredMembersMap.get(id);
        
        if (currentDbMember == null) {
            return false;
        }
        
        // Optimistic locking version check
        if (currentDbMember.getVersion() != targetMember.getVersion()) {
            throw new RuntimeException("OptimisticLockingFailureException: Concurrent modification detected for member " + id + 
                                       ". Expected version " + targetMember.getVersion() + " but found " + currentDbMember.getVersion());
        }
        
        // Prepare updated state and increment version
        Member updatedMember = new Member(targetMember);
        updatedMember.setVersion(currentDbMember.getVersion() + 1);
        
        // Atomic compare-and-swap update using ConcurrentHashMap's replace method
        boolean replaced = registeredMembersMap.replace(id, currentDbMember, updatedMember);
        if (!replaced) {
            throw new RuntimeException("OptimisticLockingFailureException: Member " + id + " was modified concurrently during replace.");
        }
        
        return true;
    }

    @Override
    public List<Member> getAllMembers() {
        List<Member> membersCopy = new ArrayList<>();
        for (Member m : registeredMembersMap.values()) {
            membersCopy.add(new Member(m));
        }
        return membersCopy;
    }
}