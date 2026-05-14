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

    public void activate() {
        this.status = RoleStatus.ACTIVE;
    }

    public abstract boolean hasPermission(Permission permission);
    public boolean isActive() {
    return this.status == RoleStatus.ACTIVE;
    }

    public boolean isPending() {
        return this.status == RoleStatus.PENDING;
    }

    public boolean isCancelled() {
        return this.status == RoleStatus.CANCELLED;
    }

    public void cancel() {
        this.status = RoleStatus.CANCELLED;
    }

}