package ticketsystem.DomainLayer.user;
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 8105adc (Deleting Membership Repository and updating Member to save his list of roles in each company)
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
<<<<<<< HEAD
=======
>>>>>>> 0dbf918 (Refactor MembershipRepository and CompanyRole)

public class Member extends User {

<<<<<<< HEAD
    private final Long memberId;
=======
    private final Long id;
>>>>>>> 44d970c (Refactor UC 4.7 to use RoleStatus and a unified MembershipRepository)
=======

public class Member extends User {

    private final Long memberId;
>>>>>>> 8105adc (Deleting Membership Repository and updating Member to save his list of roles in each company)
    private String userName;
<<<<<<< HEAD
    private ConcurrentMap<Long, CompanyRole> myRoles; // Key: companyId, Value: Role in that company
=======
    private String password;
<<<<<<< HEAD
>>>>>>> 0dbf918 (Refactor MembershipRepository and CompanyRole)

<<<<<<< HEAD
    public Member(Long memberId, String userName) {
        this.memberId = memberId;
=======
    public Member(Long id, String userName, String password) {
        this.id = id;
>>>>>>> 44d970c (Refactor UC 4.7 to use RoleStatus and a unified MembershipRepository)
=======
    private ConcurrentMap<Long, CompanyRole> myRoles; // Key: companyId, Value: Role in that company

    public Member(Long memberId, String userName, String password) {
        this.memberId = memberId;
>>>>>>> 8105adc (Deleting Membership Repository and updating Member to save his list of roles in each company)
        this.userName = userName;
<<<<<<< HEAD
        this.myRoles = new ConcurrentHashMap<Long, CompanyRole>();
    }

    public Long getId() {
        return this.memberId;
=======
        this.password = password;
        this.myRoles = new ConcurrentHashMap<Long, CompanyRole>();
    }

<<<<<<< HEAD
    public Long getId() {
        return this.id;
>>>>>>> 0dbf918 (Refactor MembershipRepository and CompanyRole)
=======
    public Long getMemberId() {
        return this.memberId;
>>>>>>> 8105adc (Deleting Membership Repository and updating Member to save his list of roles in each company)
    }

    public String getUserName() {
        return userName;
    }
    protected void setUserName(String userName) {
        this.userName = userName;
    }

<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 8105adc (Deleting Membership Repository and updating Member to save his list of roles in each company)
    public List<CompanyRole> getAllRoles() {
        return myRoles.values().stream().collect(Collectors.toList());
    }

    public CompanyRole getRoleInCompany(Long companyId) {
        return myRoles.get(companyId);
    }

<<<<<<< HEAD
    public boolean hasPermission(Long companyId, Permission permission) {
        CompanyRole role = myRoles.get(companyId);
        return role != null && role.hasPermission(permission);
    }

    public void addManagerRole(Long companyId, Long memberId, Set<Permission> permissions) {
        CompanyRole newRole = new Manager(companyId, memberId, permissions);
=======
    public void addManagerRole(Long companyId, Set<Permission> permissions, Long memberId) {
        CompanyRole newRole = new Manager(companyId, permissions, memberId);
>>>>>>> 8105adc (Deleting Membership Repository and updating Member to save his list of roles in each company)
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

<<<<<<< HEAD
    public void deleteRoleInCompany(Long companyId) {
=======
    public void deleteRole(Long companyId, Long memberId) {
>>>>>>> 8105adc (Deleting Membership Repository and updating Member to save his list of roles in each company)
        myRoles.remove(companyId);
    }

    public void updateManagerPermissions(Long companyId, Set<Permission> newPermissions) {
        CompanyRole role = myRoles.get(companyId);
        if (role != null && role instanceof Manager) {
            ((Manager) role).setPermissions(newPermissions);
        }
    }

<<<<<<< HEAD
=======
>>>>>>> 0dbf918 (Refactor MembershipRepository and CompanyRole)
=======
>>>>>>> 8105adc (Deleting Membership Repository and updating Member to save his list of roles in each company)
}
