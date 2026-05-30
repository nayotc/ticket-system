package ticketsystem.PresentationLayer.Presenters;

import org.springframework.stereotype.Component;
import ticketsystem.ApplicationLayer.CompanyService;
import ticketsystem.ApplicationLayer.ITokenService;
import ticketsystem.ApplicationLayer.SystemAdminService;
import ticketsystem.ApplicationLayer.UserService;
import ticketsystem.DTO.CompanyDTO;
import ticketsystem.DTO.OrderDTO;
import ticketsystem.PresentationLayer.Views.SystemAdminDashboard;
import ticketsystem.PresentationLayer.Views.SystemAdminDashboard.AdminUserRow;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Presenter implementation for the System Admin Dashboard.
 * Bridges the UI with the SystemAdminService, UserService, and CompanyService.
 */
@Component
public class SystemAdminPresenter {

    private final SystemAdminService systemAdminService;
    private final UserService userService;
    private final CompanyService companyService;

    // Injecting all necessary services to aggregate data for the admin dashboard
    public SystemAdminPresenter(SystemAdminService systemAdminService, UserService userService, CompanyService companyService, ITokenService tokenService) {
        this.systemAdminService = systemAdminService;
        this.userService = userService;
        this.companyService = companyService;
    }

    public List<AdminUserRow> loadActiveUsers(String sessionToken) throws Exception {
        // Validate admin access first
        validateAndGetAdminId(sessionToken);

        // Fetch all users from UserService and map them to the View's record structure
        // NOTE: Adjust 'getAllUsers' to match your UserService method (e.g., getAllMembers)
        return userService.getAllUsers().stream()
                .map(user -> new AdminUserRow(
                        user.getId(),
                        user.getEmail(),
                        user.getFirstName() + " " + user.getLastName(),
                        "פעיל", // Default status assuming they are active
                        "לא זמין" // Replace with actual last seen data if available in your DTO
                ))
                .collect(Collectors.toList());
    }

    public List<CompanyDTO> loadActiveCompanies(String sessionToken) throws Exception {
        // Validate admin access
        validateAndGetAdminId(sessionToken);

        // NOTE: Adjust 'getAllCompanies' to match your CompanyService method
        return companyService.getAllCompanies();
    }

    public Map<Long, Map<String, List<OrderDTO>>> loadPurchaseHistoryByCompanyAndEvent(String sessionToken) throws Exception {
        long adminId = validateAndGetAdminId(sessionToken);
        
        try {
            return systemAdminService.getPurchaseHistoryByCompanyAndEvent(adminId);
        } catch (IllegalStateException e) {
            // The service throws an IllegalStateException if the history is empty.
            // Instead of crashing the UI, we catch it and return an empty map so the grid simply shows 0 rows.
            return Map.of();
        } catch (Exception e) {
            throw new PresentationException(e.getMessage());
        }
    }

    public Map<Long, List<OrderDTO>> loadPurchaseHistoryByBuyer(String sessionToken) throws Exception {
        long adminId = validateAndGetAdminId(sessionToken);
        
        try {
            return systemAdminService.getPurchaseHistoryByBuyer(adminId);
        } catch (IllegalStateException e) {
            // Gracefully handle empty history
            return Map.of();
        } catch (Exception e) {
            throw new PresentationException(e.getMessage());
        }
    }

    public void deleteUser(String sessionToken, long memberId) throws Exception {
        long adminId = validateAndGetAdminId(sessionToken);
        
        // systemAdminService returns a String message starting with "ERROR" or "SUCCESS"
        String result = systemAdminService.deleteMemberByAdmin(adminId, memberId);
        
        if (result != null && result.startsWith("ERROR")) {
            // Strip the "ERROR: " prefix and throw it so the View can show it in a red notification
            throw new PresentationException(result.replace("ERROR: ", "").trim());
        }
    }

    public void removeUserFromAllCompanies(String sessionToken, long memberId) throws Exception {
        // Validate token to ensure caller is an admin
        validateAndGetAdminId(sessionToken);
        
        try {
            systemAdminService.removeUserFromAllCompanies(memberId);
        } catch (Exception e) {
            throw new PresentationException("Failed to remove user from companies: " + e.getMessage());
        }
    }

    public void closeCompany(String sessionToken, long companyId) throws Exception {
        long adminId = validateAndGetAdminId(sessionToken);
        
        try {
            systemAdminService.closeProductionCompanyByAdmin(adminId, companyId);
        } catch (Exception e) {
            throw new PresentationException(e.getMessage());
        }
    }
}