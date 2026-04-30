package ticketsystem.DomainLayer.user;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Member extends User {

<<<<<<< HEAD
    private final Long memberId;
=======
    private final Long id;
>>>>>>> 44d970c (Refactor UC 4.7 to use RoleStatus and a unified MembershipRepository)
    private String userName;
    private ConcurrentHashMap<Long, CompanyRole> myRoles; // Key: companyId, Value: Role in that company

<<<<<<< HEAD
    public Member(Long memberId, String userName) {
        this.memberId = memberId;
=======
    public Member(Long id, String userName, String password) {
        this.id = id;
>>>>>>> 44d970c (Refactor UC 4.7 to use RoleStatus and a unified MembershipRepository)
        this.userName = userName;
        this.myRoles = new ConcurrentHashMap<Long, CompanyRole>();
    }

    public Long getId() {
<<<<<<< HEAD
        return this.memberId;
=======
        return this.id;
>>>>>>> 44d970c (Refactor UC 4.7 to use RoleStatus and a unified MembershipRepository)
    }

    public String getUserName() {
        return this.userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public List<CompanyRole> getAllRoles() {
        return myRoles.values().stream().collect(Collectors.toList());
    }

    public CompanyRole getRoleInCompany(Long companyId) {
        return myRoles.get(companyId);
    }

    public boolean hasPermission(Long companyId, Permission permission) {
        CompanyRole role = myRoles.get(companyId);
        return role != null && role.hasPermission(permission);
    }

    public boolean addManagerRole(Long companyId, Long memberId, Set<Permission> permissions) {
        CompanyRole newRole = new Manager(companyId, memberId, permissions);
        return myRoles.putIfAbsent(companyId, newRole) == null;
    }

    public boolean addOwnerRole(Long companyId, Long memberId) {
        CompanyRole newRole = new Owner(companyId, memberId);
        return myRoles.putIfAbsent(companyId, newRole) == null;
    }

    public boolean addFounderRole(Long companyId) {
        CompanyRole newRole = new Founder(companyId);
        return myRoles.putIfAbsent(companyId, newRole) == null;
    }

    public boolean deleteRoleInCompany(Long companyId) {
        return myRoles.remove(companyId) != null;
    }

    public void updateManagerPermissions(Long companyId, Set<Permission> newPermissions) {
        CompanyRole role = myRoles.get(companyId);
        if (role != null && role instanceof Manager) {
            ((Manager) role).setPermissions(newPermissions);
        }
    }

}