package ticketsystem.DomainLayer.user;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;

@Entity
@DiscriminatorValue("MANAGER")
public class Manager extends CompanyRole {

    @Column(name = "appointed_by_member_id")
    private Long appointedByMemberId;

    @ElementCollection
    @CollectionTable(name = "manager_permissions", joinColumns = @JoinColumn(name = "role_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "permission")
    private Set<Permission> permissions = new HashSet<>();

    protected Manager() {
    }

    public Manager(Long companyId, Long appointedByMemberId, Set<Permission> permissions) {
        super(companyId);
        this.appointedByMemberId = appointedByMemberId;
        this.status = RoleStatus.PENDING;
        this.permissions = permissions != null ? new HashSet<>(permissions) : new HashSet<>();
    }

    public Manager(Manager other, Long companyId) {
        super(companyId);
        this.status = other.status;
        this.appointedByMemberId = other.appointedByMemberId;
        this.permissions = new HashSet<>(other.permissions);
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

    @Override
    public boolean hasPermission(Permission permission) {
        return this.status == RoleStatus.ACTIVE && this.permissions.contains(permission);
    }

    public void addPermission(Permission permission) {
        this.permissions.add(permission);
    }

    public void setPermissions(Set<Permission> newPermissions) {
        this.permissions = newPermissions != null ? new HashSet<>(newPermissions) : new HashSet<>();
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
