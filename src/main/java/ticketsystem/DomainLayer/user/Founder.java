package ticketsystem.DomainLayer.user;
import java.util.ArrayList;
import java.util.List;

public class Founder extends CompanyRole{
    
    private List<Long> appointeesMemberIds;

<<<<<<< HEAD
    public Founder(Long companyId) {
        super(companyId);
=======
    public Founder(Long memberId, Long companyId) {
        super(memberId, companyId);
>>>>>>> 44d970c (Refactor UC 4.7 to use RoleStatus and a unified MembershipRepository)
        this.appointeesMemberIds = new ArrayList<>();
        this.status = RoleStatus.ACTIVE; // Founder is active immediately upon creation
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
    }

    public boolean hasPermission(Permission permission) {
<<<<<<< HEAD
        return true; // Founder has all permissions
    }

=======
        // Founder has all permissions active immediately
        return true;
=======
        appointeesMemberIds.add(memberId);
    }

    public void deleteAppointee(Long memberId) {
        appointeesMemberIds.remove(memberId);
    }

    public boolean hasPermission(Permission permission) {
        return true; // Founder has all permissions
>>>>>>> 44d970c (Refactor UC 4.7 to use RoleStatus and a unified MembershipRepository)
    }
>>>>>>> 2d153d5 (Add unit tests for Member and CompanyRole classes)
}
