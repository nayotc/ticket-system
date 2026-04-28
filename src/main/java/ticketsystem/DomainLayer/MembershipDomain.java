package ticketsystem.DomainLayer;
import ticketsystem.DomainLayer.user.*;

public class PermissionDomain {

    public void validateAppointManager(CompanyRole member, CompanyRole targetMember) {
        if (!member.hasPermission(Permission.APPOINT_MANAGER)) {
            throw new RuntimeException("Member lacks permission to appoint a manager.");
        }

        if (targetMember != null && isManager(targetMember)) {
            throw new RuntimeException("Member is already has a Manager role in this company.");
        }
    }

    private boolean isManager(CompanyRole role) {
        return role instanceof Manager;
    }
    

}