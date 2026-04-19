package DomainLayer.user;

public class Manager extends CompanyRole{
    private Long appointedByMemberId;

    public Manager(Member member, long companyId, Long appointedByMemberId) {
        super(member, companyId); // קריאה לבנאי של מחלקת האב
        this.appointedByMemberId = appointedByMemberId;
    }

    public Long getAppointedByMemberId() { return appointedByMemberId; }
}
