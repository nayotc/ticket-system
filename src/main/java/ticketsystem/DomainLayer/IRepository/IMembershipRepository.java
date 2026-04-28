package ticketsystem.DomainLayer.IRepository;
import java.util.List;
import ticketsystem.DomainLayer.user.CompanyRole;

public interface IMembershipRepository {

    void addRole(CompanyRole role);
    CompanyRole findRole(long companyId, long memberId);
    void updateRole(CompanyRole role);
    void deleteRole(long companyId, long memberId);
    List<CompanyRole> getAll();
    
}
