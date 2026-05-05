package ticketsystem.DomainLayer.user;
import java.util.Set;
import java.util.stream.Collectors;

public class Manager extends CompanyRole {

    private Long appointedByMemberId;
    private Set<Permission> permissions;

<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
    public Manager(Long companyId, Long appointedByMemberId, Set<Permission> permissions) {
        super(companyId);
=======
    public Manager(Member memberMock, Long companyId, Set<Permission> permissions, Long appointedByMemberId) {
        super(memberMock, companyId);
>>>>>>> 2d153d5 (Add unit tests for Member and CompanyRole classes)
=======
    public Manager(Long memberId, Long companyId, Set<Permission> permissions, Long appointedByMemberId) {
        super(memberId, companyId);
>>>>>>> 44d970c (Refactor UC 4.7 to use RoleStatus and a unified MembershipRepository)
=======
    public Manager(Long companyId, Set<Permission> permissions, Long appointedByMemberId) {
        super(companyId);
>>>>>>> 8105adc (Deleting Membership Repository and updating Member to save his list of roles in each company)
        this.appointedByMemberId = appointedByMemberId;
        this.status = RoleStatus.PENDING;
        this.permissions = permissions;
    }

    public Long getAppointedByMemberId() {
        return this.appointedByMemberId; 
    }

<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> e7f5697 (starting to implement giveup ownership use case)
    public void setAppointer(Long newAppointedByMemberId) {
        this.appointedByMemberId = newAppointedByMemberId;
    }

<<<<<<< HEAD
    public Set<Permission> getPermissions() {
        return this.permissions;
=======
=======
>>>>>>> e7f5697 (starting to implement giveup ownership use case)
    public void activate() {
        this.status = RoleStatus.ACTIVE;
>>>>>>> 2d153d5 (Add unit tests for Member and CompanyRole classes)
    }

    public boolean hasPermission(Permission permission) {
<<<<<<< HEAD
        return this.status == RoleStatus.ACTIVE && this.permissions.contains(permission); // Managers have specific permissions when active
=======
        return status == RoleStatus.ACTIVE && permissions.contains(permission); // Managers have specific permissions when active
>>>>>>> 44d970c (Refactor UC 4.7 to use RoleStatus and a unified MembershipRepository)
    }

    public void addPermission(Permission permission) {
        this.permissions.add(permission);
    }

    public void setPermissions(Set<Permission> newPermissions) {
        this.permissions = newPermissions;
    }

    public void deletePermission(Permission permission) {
        this.permissions.remove(permission);
    }

    public Set<String> getPermissionKeys() {
        return this.permissions.stream()
                .map(Permission::getKey)
                .collect(Collectors.toSet());
    }
}
