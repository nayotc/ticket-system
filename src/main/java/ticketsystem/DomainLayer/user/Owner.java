package ticketsystem.DomainLayer.user;
import java.util.ArrayList;
import java.util.List;

public class Owner extends CompanyRole {

    private List<Long> appointeesMemberIds;
    private Long appointedByMemberId;

<<<<<<< HEAD
<<<<<<< HEAD
    public Owner(Long companyId, Long appointedByMemberId) {
        super(companyId);
=======
    public Owner(Long memberId, Long companyId, Long appointedByMemberId) {
        super(memberId, companyId);
>>>>>>> 44d970c (Refactor UC 4.7 to use RoleStatus and a unified MembershipRepository)
=======
    public Owner(Long companyId, Long appointedByMemberId) {
        super(companyId);
>>>>>>> 8105adc (Deleting Membership Repository and updating Member to save his list of roles in each company)
        this.appointedByMemberId = appointedByMemberId;
        this.appointeesMemberIds = new ArrayList<>();
        this.status = RoleStatus.PENDING;
    }

<<<<<<< HEAD
=======
    public void activate() {
        this.status = RoleStatus.ACTIVE;
    }

>>>>>>> 2d153d5 (Add unit tests for Member and CompanyRole classes)
    public Long getAppointedByMemberId() {
        return this.appointedByMemberId;        
    }

    public void setAppointer(Long newAppointedByMemberId) {
        this.appointedByMemberId = newAppointedByMemberId;
    }

    public void setAppointer(Long newAppointedByMemberId) {
        this.appointedByMemberId = newAppointedByMemberId;
    }

    public List<Long> getAppointeesMemberIds() {
        return this.appointeesMemberIds; 
    }

    public void addAppointee(Long memberId) {
<<<<<<< HEAD
        this.appointeesMemberIds.add(memberId);
    }

    public void deleteAppointee(Long memberId) {
        this.appointeesMemberIds.remove(memberId);
=======
        appointeesMemberIds.add(memberId);
    }

    public void deleteAppointee(Long memberId) {
        appointeesMemberIds.remove(memberId);
>>>>>>> 44d970c (Refactor UC 4.7 to use RoleStatus and a unified MembershipRepository)
    }

    public boolean hasPermission(Permission permission) {
<<<<<<< HEAD
        return this.status == RoleStatus.ACTIVE; // Owners have all permissions when active
    }
}
=======
        return status == RoleStatus.ACTIVE; // Owners have all permissions when active
    }
}
>>>>>>> 2d153d5 (Add unit tests for Member and CompanyRole classes)
