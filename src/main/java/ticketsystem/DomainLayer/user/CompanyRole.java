package ticketsystem.DomainLayer.user;

public abstract class CompanyRole {

    protected Long companyId;
    protected RoleStatus status;

    public CompanyRole(Long companyId) {
        this.companyId = companyId;
        this.status = RoleStatus.PENDING;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public RoleStatus getStatus() {
        return status;
    }

    public void setStatus(RoleStatus status) {
        this.status = status;
    }

    public abstract boolean hasPermission(Permission permission);
}