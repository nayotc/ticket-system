package ticketsystem.DTO;

import ticketsystem.DomainLayer.user.*;
import java.util.Set;
import java.util.stream.Collectors;

public class CompanyRoleDTO {
    
    private Long companyId;
    private String roleType; // "FOUNDER", "OWNER", "MANAGER"
    private String status;   // "ACTIVE", "PENDING", "CANCELLED"
    private Set<String> permissions;
    private Long appointedByMemberId;

    public CompanyRoleDTO() {}

    public CompanyRoleDTO(Long companyId, String roleType, String status, Set<String> permissions, Long appointedByMemberId) {
        this.companyId = companyId;
        this.roleType = roleType;
        this.status = status;
        this.permissions = permissions;
        this.appointedByMemberId = appointedByMemberId;
    }

    public static CompanyRoleDTO fromDomain(CompanyRole role) {
        String roleType = "UNKNOWN";
        Long appointerId = null;
        Set<String> permissions = Set.of();

        if (role instanceof Founder) {
            roleType = "FOUNDER";
            permissions = Permission.getAllPermissions().stream().map(Permission::name).collect(Collectors.toSet());
        } else if (role instanceof Owner) {
            roleType = "OWNER";
            appointerId = ((Owner) role).getAppointedByMemberId();
            permissions = Permission.getAllPermissions().stream().map(Permission::name).collect(Collectors.toSet());
        } else if (role instanceof Manager) {
            roleType = "MANAGER";
            appointerId = ((Manager) role).getAppointedByMemberId();
            permissions = ((Manager) role).getPermissionKeys();
        }

        return new CompanyRoleDTO(
                role.getCompanyId(),
                roleType,
                role.getStatus().name(),
                permissions,
                appointerId
        );
    }

    public Long getCompanyId() { return companyId; }
    public String getRoleType() { return roleType; }
    public String getStatus() { return status; }
    public Set<String> getPermissions() { return permissions; }
    public Long getAppointedByMemberId() { return appointedByMemberId; }

}