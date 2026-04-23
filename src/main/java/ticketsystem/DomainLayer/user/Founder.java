package ticketsystem.DomainLayer.user;

import java.util.ArrayList;
import java.util.List;

public class Founder extends CompanyRole{
    private List<Long> appointeesMemberIds;

    public Founder(Member member, long companyId, Long appointedByMemberId) {
        super(member, companyId);
        this.appointeesMemberIds = new ArrayList<>();
    }
}
