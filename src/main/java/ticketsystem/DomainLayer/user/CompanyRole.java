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

<<<<<<< HEAD
    public void setStatus(RoleStatus status) {
        this.status = status;
    }

    public abstract boolean hasPermission(Permission permission);
}
=======
    public abstract boolean hasPermission(Permission permission);

}
>>>>>>> 2d153d5 (Add unit tests for Member and CompanyRole classes)
