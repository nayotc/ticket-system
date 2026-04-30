package ticketsystem.InfrastructureLayer;
import ticketsystem.DomainLayer.IRepository.IMembershipRepository;
import ticketsystem.DomainLayer.user.CompanyRole;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class MembershipRepository implements IMembershipRepository {

    private static MembershipRepository instance;

    // Key format: "companyId_memberId"
    private final ConcurrentMap<String, CompanyRole> rolesTable;

    private MembershipRepository() {
        this.rolesTable = new ConcurrentHashMap<>();
    }

    public static synchronized MembershipRepository getInstance() {
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
        if (role == null) throw new IllegalArgumentException("Role cannot be null");
        
        String key = generateKey(role.getCompanyId(), role.getMemberId());
        if (rolesTable.putIfAbsent(key, role) != null) {
            throw new IllegalArgumentException("A role (active or pending) already exists for this member.");
        }
    }

    @Override
    public CompanyRole findRole(Long companyId, Long memberId) {
        return rolesTable.get(generateKey(companyId, memberId));
    }

    @Override
    public synchronized void updateRole(CompanyRole role) {
        if (role == null) throw new IllegalArgumentException("Role cannot be null");
        
        String key = generateKey(role.getCompanyId(), role.getMemberId());
        if (rolesTable.replace(key, role) == null) {
            throw new IllegalArgumentException("Role does not exist to update.");
        }
    }

    @Override
    public synchronized void deleteRole(Long companyId, Long memberId) {
        rolesTable.remove(generateKey(companyId, memberId));
    }

    @Override
    public List<CompanyRole> getAllRolesInCompany(Long companyId) {
        return rolesTable.values().stream()
                .filter(role -> role.getCompanyId() == companyId)
                .collect(Collectors.toList());
    }
}