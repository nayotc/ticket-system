package ticketsystem.DomainLayer.IRepository;
import ticketsystem.DomainLayer.user.CompanyRole;
import java.util.List;

public interface IMembershipRepository {

    void addRole(CompanyRole role);
    
    CompanyRole findRole(Long companyId, Long memberId);
    
    void updateRole(CompanyRole role);
    
    void deleteRole(Long companyId, Long memberId);
    
    List<CompanyRole> getAllRolesInCompany(Long companyId);
}