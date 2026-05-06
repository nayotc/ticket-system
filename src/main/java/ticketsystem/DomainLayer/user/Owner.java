package ticketsystem.DomainLayer.user;
import java.util.ArrayList;
import java.util.List;
import ticketsystem.DomainLayer.user.Permission;


public class Owner extends CompanyRole {

    private List<Long> appointeesMemberIds;
    private Long appointedByMemberId;

<<<<<<< HEAD
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
=======
    public Owner(Long companyId, Long appointedByMemberId) {
        super(companyId);
>>>>>>> 5c34fef (implementation of use-case 4.7)
        this.appointedByMemberId = appointedByMemberId;
        this.appointeesMemberIds = new ArrayList<>();
        this.status = RoleStatus.PENDING;
    }

<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
=======
    public void activate() {
        this.status = RoleStatus.ACTIVE;
    }

>>>>>>> 2d153d5 (Add unit tests for Member and CompanyRole classes)
=======
>>>>>>> e663313 (implementation of use-case 4.7)
=======
>>>>>>> 5c34fef (implementation of use-case 4.7)
    public Long getAppointedByMemberId() {
        return this.appointedByMemberId;        
    }

    public void setAppointer(Long newAppointedByMemberId) {
        this.appointedByMemberId = newAppointedByMemberId;
    }

<<<<<<< HEAD
    public void setAppointer(Long newAppointedByMemberId) {
        this.appointedByMemberId = newAppointedByMemberId;
    }

=======
>>>>>>> 5c34fef (implementation of use-case 4.7)
    public List<Long> getAppointeesMemberIds() {
        return this.appointeesMemberIds; 
    }

    public void addAppointee(Long memberId) {
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 5c34fef (implementation of use-case 4.7)
        this.appointeesMemberIds.add(memberId);
    }

    public void deleteAppointee(Long memberId) {
        this.appointeesMemberIds.remove(memberId);
<<<<<<< HEAD
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
=======
    }

    public boolean hasPermission(Permission permission) {
        return this.status == RoleStatus.ACTIVE; // Owners have all permissions when active
    }
}
>>>>>>> 5c34fef (implementation of use-case 4.7)
