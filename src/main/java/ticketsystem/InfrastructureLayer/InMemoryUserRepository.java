package ticketsystem.InfrastructureLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.DomainLayer.user.CompanyRole;
import ticketsystem.DomainLayer.user.Member;
import ticketsystem.DomainLayer.user.Owner;
import ticketsystem.DomainLayer.user.RoleStatus;

/**
 * In-memory implementation used by acceptance/unit tests that construct repositories manually.
 */
public class InMemoryUserRepository implements IUserRepository {

    private final ConcurrentHashMap<Long, Member> registeredMembersMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> hashedPasswordsMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> usernameToIdMap = new ConcurrentHashMap<>();
    private final AtomicLong nextGeneratedId = new AtomicLong(1L);

    @Override
    public synchronized boolean addRegisteredMember(long id, Member member, String hashedPassword) {
        long memberId = resolveMemberId(id, member);
        if (registeredMembersMap.containsKey(memberId) || usernameToIdMap.containsKey(member.getUserName())) {
            return false;
        }
        Member persisted = new Member(member);
        persisted.setId(memberId);
        persisted.setHashedPassword(hashedPassword);
        registeredMembersMap.put(memberId, persisted);
        usernameToIdMap.put(member.getUserName(), memberId);
        hashedPasswordsMap.put(member.getUserName(), hashedPassword);
        return true;
    }

    @Override
    public synchronized boolean removeRegisteredMember(long id) {
        Member removedMember = registeredMembersMap.remove(id);
        if (removedMember == null) {
            return false;
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
        if (id == null) {
            return null;
        }
        return getMemberById(id);
    }

    @Override
    public Member getMemberByUsernameIgnoreCase(String username) {
        if (username == null) {
            return null;
        }
        for (var entry : usernameToIdMap.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(username)) {
                return getMemberById(entry.getValue());
            }
        }
        return null;
    }

    @Override
    public Member getMemberById(long id) {
        Member dbMember = registeredMembersMap.get(id);
        if (dbMember != null) {
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
        long memberId = usernameToIdMap.get(username);
        Member member = registeredMembersMap.get(memberId);
        if (member == null) {
            return false;
        }
        if (username.equals(newUsername)) {
            return true;
        }
        String hashedPassword = hashedPasswordsMap.remove(username);
        member.setUserName(newUsername);
        usernameToIdMap.remove(username);
        usernameToIdMap.put(newUsername, memberId);
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
        long memberId = usernameToIdMap.get(username);
        Member member = registeredMembersMap.get(memberId);
        if (member != null) {
            member.setHashedPassword(newHashedPassword);
        }
        return true;
    }

    @Override
    public boolean updateMember(Member targetMember) {
        long id = targetMember.getId();
        Member currentDbMember = registeredMembersMap.get(id);
        if (currentDbMember == null) {
            return false;
        }
        if (currentDbMember.getVersion() != targetMember.getVersion()) {
            throw new RuntimeException("OptimisticLockingFailureException: Concurrent modification detected for member "
                    + id);
        }
        Member updatedMember = new Member(targetMember);
        updatedMember.setVersion(currentDbMember.getVersion() + 1);
        boolean replaced = registeredMembersMap.replace(id, currentDbMember, updatedMember);
        if (!replaced) {
            throw new RuntimeException("OptimisticLockingFailureException: Member " + id
                    + " was modified concurrently during replace.");
        }
        return true;
    }

    @Override
    public List<Member> getAllMembers() {
        List<Member> membersCopy = new ArrayList<>();
        for (Member member : registeredMembersMap.values()) {
            membersCopy.add(new Member(member));
        }
        return membersCopy;
    }

    @Override
    public int countPendingRolesByCompanyId(Long companyId) {
        int count = 0;
        for (Member member : registeredMembersMap.values()) {
            CompanyRole role = member.getRoleInCompany(companyId);
            if (role != null && role.getStatus() == RoleStatus.PENDING) {
                count++;
            }
        }
        return count;
    }

    @Override
    public int countActiveOwnersByCompanyId(Long companyId) {
        int count = 0;
        for (Member member : registeredMembersMap.values()) {
            CompanyRole role = member.getRoleInCompany(companyId);
            if (role instanceof Owner && role.getStatus() == RoleStatus.ACTIVE) {
                count++;
            }
        }
        return count;
    }

    @Override
    public List<Member> findMembersWithRolesInCompany(Long companyId) {
        List<Member> members = new ArrayList<>();
        for (Member member : registeredMembersMap.values()) {
            if (member.getRoleInCompany(companyId) != null) {
                members.add(new Member(member));
            }
        }
        return members;
    }

    @Override
    public List<Member> findSuspendedMembers() {
        List<Member> members = new ArrayList<>();
        for (Member member : registeredMembersMap.values()) {
            if (member.isSuspended()) {
                members.add(new Member(member));
            }
        }
        return members;
    }

    private long resolveMemberId(long id, Member member) {
        if (member.getId() != null) {
            return member.getId();
        }
        if (id != 0L) {
            return id;
        }
        return nextGeneratedId.getAndIncrement();
    }
}
