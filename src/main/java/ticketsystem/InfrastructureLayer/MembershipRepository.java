package ticketsystem.InfrastructureLayer;
import ticketsystem.DomainLayer.IRepository.IMembershipRepository;
import ticketsystem.DomainLayer.user.CompanyRole;
import ticketsystem.DomainLayer.user.Permission;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MembershipRepository implements IMembershipRepository {

    private MembershipRepository instance;
    private final ConcurrentHashMap<String, CompanyRole> roles;
    private final ConcurrentHashMap<String, PendingInviteData> pendingInvites;

    public static class PendingInviteData {
        public final long ownerId;
        public final Set<Permission> permissions;

        public PendingInviteData(long ownerId, Set<Permission> permissions) {
            this.ownerId = ownerId;
            this.permissions = permissions;
        }
    }

    private MembershipRepository() {
        this.roles = new ConcurrentHashMap<String, CompanyRole>();
        this.pendingInvites = new ConcurrentHashMap<String, PendingInviteData>();
    }

    public MembershipRepository getInstance() {
        if (instance == null) {
            instance = new MembershipRepository();
        }
        return instance;
    }

    private String generateKey(Long companyId, Long memberId) {
        return companyId + "_" + memberId;
    }

    @Override
    public synchronized void addRole(CompanyRole role) {
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }

        String key = generateKey(role.getCompanyId(), role.getMemberId());
        
        if (roles.putIfAbsent(key, role) != null) {
            throw new IllegalArgumentException("Role already exists for this member in this company.");
        }
    }

    @Override
    public CompanyRole findRole(long companyId, long memberId) {
        String key = generateKey(companyId, memberId);
        return roles.get(key);
    }

    @Override
    public void updateRole(CompanyRole role) {
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }

        String key = generateKey(role.getCompanyId(), role.getMemberId());
        
        if (roles.replace(key, role) == null) {
            throw new IllegalArgumentException("Role does not exist to update.");
        }
    }

    @Override
    public synchronized void deleteRole(long companyId, long memberId) {
        String key = generateKey(companyId, memberId);
        roles.remove(key);
    }

    @Override
    public List<CompanyRole> getAll() {
        return new ArrayList<>(roles.values());
    }
    
    public void savePendingInvite(long companyId, long targetMemberId, long ownerId, Set<Permission> permissions) {
        String key = generateKey(companyId, targetMemberId);
        if (pendingInvites.putIfAbsent(key, new PendingInviteData(ownerId, permissions)) != null) {
            throw new IllegalArgumentException("A pending invite already exists for this member.");
        }
    }

    public PendingInviteData getPendingInvite(long companyId, long targetMemberId) {
        return pendingInvites.get(generateKey(companyId, targetMemberId));
    }

    public void deletePendingInvite(long companyId, long targetMemberId) {
        pendingInvites.remove(generateKey(companyId, targetMemberId));
    }
    
}
