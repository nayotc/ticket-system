package DomainLayer.user;

import java.util.ArrayList;
import java.util.List;

public class Owner extends CompanyRole {
    private List<Long> appointeesMemberIds;
    private Long appointedByMemberId;

    public Owner(Member member, long companyId, Long appointedByMemberId) {
        super(member, companyId);
        this.appointedByMemberId = appointedByMemberId;
        this.appointeesMemberIds = new ArrayList<>(); // חובה לאתחל את הרשימה!
    }

    public Long getAppointedByMemberId() { return appointedByMemberId; }
    public List<Long> getAppointeesMemberIds() { return appointeesMemberIds; }
}
