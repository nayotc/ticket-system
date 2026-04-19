package DomainLayer.user;

public abstract class CompanyRole {
    protected Member member;
    protected long companyId;

    public CompanyRole(Member member, long companyId) {
        this.member = member;
        this.companyId = companyId;
    }
    
    public long getCompanyId() { return companyId; }
}
