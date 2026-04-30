package ticketsystem.DomainLayer.IRepository;
import ticketsystem.DomainLayer.user.CompanyRole;
import java.util.List;

public interface IMembershipRepository {

    void addRole(CompanyRole role);
    
    CompanyRole findRole(long companyId, long memberId);
    
    void updateRole(CompanyRole role);
    
    void deleteRole(long companyId, long memberId);
    
    List<CompanyRole> getAllRolesInCompany(long companyId);
}