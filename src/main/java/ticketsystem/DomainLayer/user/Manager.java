package ticketsystem.DomainLayer.user;
import java.util.Set;
import java.util.stream.Collectors;

public class Manager extends CompanyRole {

    private Long appointedByMemberId;
    private Set<Permission> permissions;

    public Manager(Long companyId, Long appointedByMemberId, Set<Permission> permissions) {
        super(companyId);
        this.appointedByMemberId = appointedByMemberId;
        this.status = RoleStatus.PENDING;
        this.permissions = permissions;
    }

    // Copy Constructor for Deep Copying
    public Manager(Manager other, Long companyId) {
        super(companyId);
        this.status = other.status;
        this.appointedByMemberId = other.appointedByMemberId;
        this.permissions = new java.util.HashSet<>(other.permissions);
    }

    public Long getAppointedByMemberId() {
        return this.appointedByMemberId; 
    }

    public void setAppointer(Long newAppointedByMemberId) {
        this.appointedByMemberId = newAppointedByMemberId;
    }

    public Set<Permission> getPermissions() {
        return this.permissions;
    }

    public boolean hasPermission(Permission permission) {
        return this.status == RoleStatus.ACTIVE && this.permissions.contains(permission); // Managers have specific permissions when active
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
