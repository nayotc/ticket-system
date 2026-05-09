package ticketsystem.DomainLayer.user;
import java.util.ArrayList;
import java.util.List;

public class Founder extends CompanyRole{
    
    private List<Long> appointeesMemberIds;

    public Founder(Long companyId) {
        super(companyId);
        this.appointeesMemberIds = new ArrayList<>();
        this.status = RoleStatus.ACTIVE; // Founder is active immediately upon creation
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
        return true; // Founder has all permissions
    }

}
