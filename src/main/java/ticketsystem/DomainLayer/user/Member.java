package ticketsystem.DomainLayer.user;
<<<<<<< HEAD
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
=======
>>>>>>> 0dbf918 (Refactor MembershipRepository and CompanyRole)

public class Member extends User {

<<<<<<< HEAD
    private final Long memberId;
=======
    private final Long id;
>>>>>>> 44d970c (Refactor UC 4.7 to use RoleStatus and a unified MembershipRepository)
    private String userName;
<<<<<<< HEAD
    private ConcurrentMap<Long, CompanyRole> myRoles; // Key: companyId, Value: Role in that company
=======
    private String password;
>>>>>>> 0dbf918 (Refactor MembershipRepository and CompanyRole)

<<<<<<< HEAD
    public Member(Long memberId, String userName) {
        this.memberId = memberId;
=======
    public Member(Long id, String userName, String password) {
        this.id = id;
>>>>>>> 44d970c (Refactor UC 4.7 to use RoleStatus and a unified MembershipRepository)
        this.userName = userName;
<<<<<<< HEAD
        this.myRoles = new ConcurrentHashMap<Long, CompanyRole>();
    }

    public Long getId() {
        return this.memberId;
=======
        this.password = password;
    }

    public Long getId() {
        return this.id;
>>>>>>> 0dbf918 (Refactor MembershipRepository and CompanyRole)
    }

    public String getUserName() {
        return userName;
    }
    protected void setUserName(String userName) {
        this.userName = userName;
    }

<<<<<<< HEAD
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

    public void addManagerRole(Long companyId, Long memberId, Set<Permission> permissions) {
        CompanyRole newRole = new Manager(companyId, memberId, permissions);
        myRoles.putIfAbsent(companyId, newRole);
    }

    public void addOwnerRole(Long companyId, Long memberId) {
        CompanyRole newRole = new Owner(companyId, memberId);
        myRoles.putIfAbsent(companyId, newRole);
    }

    public void addFounderRole(Long companyId) {
        CompanyRole newRole = new Founder(companyId);
        myRoles.putIfAbsent(companyId, newRole);
    }

    public void deleteRoleInCompany(Long companyId) {
        myRoles.remove(companyId);
    }

    public void updateManagerPermissions(Long companyId, Set<Permission> newPermissions) {
        CompanyRole role = myRoles.get(companyId);
        if (role != null && role instanceof Manager) {
            ((Manager) role).setPermissions(newPermissions);
        }
    }

=======
>>>>>>> 0dbf918 (Refactor MembershipRepository and CompanyRole)
}
