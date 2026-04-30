package ticketsystem.DomainLayer.user;

public abstract class CompanyRole {

<<<<<<< HEAD
<<<<<<< HEAD
    protected Long companyId;
    protected RoleStatus status;

    public CompanyRole(Long companyId) {
=======
    protected long memberId;
    protected long companyId;
=======
    protected Long memberId;
    protected Long companyId;
>>>>>>> 44d970c (Refactor UC 4.7 to use RoleStatus and a unified MembershipRepository)
    protected RoleStatus status;

    public CompanyRole(Long memberId, Long companyId) {
        this.memberId = memberId;
>>>>>>> 0dbf918 (Refactor MembershipRepository and CompanyRole)
        this.companyId = companyId;
        this.status = RoleStatus.PENDING;
    }

<<<<<<< HEAD
<<<<<<< HEAD
    public Long getCompanyId() {
=======
    public long getMemberId() {
        return memberId;
    }
    
    public long getCompanyId() { 
>>>>>>> 0dbf918 (Refactor MembershipRepository and CompanyRole)
=======
    public Long getMemberId() {
        return memberId;
    }

    public Long getCompanyId() {
>>>>>>> 44d970c (Refactor UC 4.7 to use RoleStatus and a unified MembershipRepository)
        return companyId;
    }

    public RoleStatus getStatus() {
        return status;
    }

<<<<<<< HEAD
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
=======
    public void setStatus(RoleStatus status) {
        this.status = status;
    }

    public abstract boolean hasPermission(Permission permission);
}
>>>>>>> 0dbf918 (Refactor MembershipRepository and CompanyRole)
