package ticketsystem.DomainLayer.user;
import java.util.ArrayList;
import java.util.List;

public class Owner extends CompanyRole {

    private List<Long> appointeesMemberIds;
    private Long appointedByMemberId;

    public Owner(Long companyId, Long appointedByMemberId) {
        super(companyId);
        this.appointedByMemberId = appointedByMemberId;
        this.appointeesMemberIds = new ArrayList<>();
        this.status = RoleStatus.PENDING;
    }

    public RoleStatus getStatus() {
        return status;
    }

    public void activate() {
        this.status = RoleStatus.ACTIVE;
    }

    public Long getAppointedByMemberId() {
        return this.appointedByMemberId;        
    }

    public void setAppointer(Long newAppointedByMemberId) {
        this.appointedByMemberId = newAppointedByMemberId;
    }

    public List<Long> getAppointeesMemberIds() {
        return this.appointeesMemberIds; 
    }

    public void addAppointee(Long memberId) {
        this.appointeesMemberIds.add(memberId);
    }

    public void deleteAppointee(Long memberId) {
        this.appointeesMemberIds.remove(memberId);
    }

    public boolean hasPermission(Permission permission) {
        return this.status == RoleStatus.ACTIVE; // Owners have all permissions when active
    }
}