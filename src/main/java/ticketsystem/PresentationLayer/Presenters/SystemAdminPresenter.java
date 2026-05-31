package ticketsystem.PresentationLayer.Presenters;

import org.springframework.stereotype.Component;
import ticketsystem.ApplicationLayer.CompanyService;
import ticketsystem.ApplicationLayer.ITokenService;
import ticketsystem.ApplicationLayer.SystemAdminService;
import ticketsystem.ApplicationLayer.UserService;
import ticketsystem.DTO.CompanyDTO;
import ticketsystem.DTO.OrderDTO;
import ticketsystem.DTO.SuspentionUserDTO;
import ticketsystem.PresentationLayer.Views.SystemAdminDashboard.AdminUserRow;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Presenter implementation for the System Admin Dashboard.
 * Bridges the UI with the SystemAdminService, UserService, and CompanyService.
 * Exposes all administrative operations including user/company management, history, and system logs.
 */
@Component
public class SystemAdminPresenter {

    private final SystemAdminService systemAdminService;
    private final UserService userService;
    private final CompanyService companyService;
    private final ITokenService tokenService;

    // Injecting all necessary services to aggregate data for the admin dashboard
    public SystemAdminPresenter(SystemAdminService systemAdminService, 
                                UserService userService, 
                                CompanyService companyService, 
                                ITokenService tokenService) {
        this.systemAdminService = systemAdminService;
        this.userService = userService;
        this.companyService = companyService;
        this.tokenService = tokenService;
    }

    /**
     * Helper method to securely extract the admin's user ID from the session token.
     */
    private long validateAndGetAdminId(String sessionToken) throws PresentationException {
        if (sessionToken == null || sessionToken.isBlank()) {
            throw new PresentationException("Session token is missing. Please log in.");
        }
        try {
            return tokenService.extractUserId(sessionToken);
        } catch (Exception e) {
            throw new PresentationException("Invalid or expired session token.");
        }
    }

    // USERS & COMPANIES MANAGEMENT

    public List<AdminUserRow> loadActiveUsers(String sessionToken) throws Exception {
        validateAndGetAdminId(sessionToken);

        // Fetch all users and map them correctly using Member's domain fields
        return userService.getAllUsers().stream()
                .map(user -> new AdminUserRow(
                        user.getId(),
                        user.getUserName(), // Using UserName (serves as email)
                        user.getFullName(), // Using FullName
                        user.isSuspended() ? "מושעה" : "פעיל", // Dynamically set status based on suspension
                        "לא זמין" // Placeholder for last activity
                ))
                .collect(Collectors.toList());
    }

    public List<CompanyDTO> loadActiveCompanies(String sessionToken) throws Exception {
        validateAndGetAdminId(sessionToken);
        return companyService.getAllCompanies();
    }

    public void deleteUser(String sessionToken, long memberId) throws Exception {
        long adminId = validateAndGetAdminId(sessionToken);
        
        // systemAdminService returns a String message starting with "ERROR" or "SUCCESS"
        String result = systemAdminService.deleteMemberByAdmin(adminId, memberId);
        
        if (result != null && result.startsWith("ERROR")) {
            throw new PresentationException(result.replace("ERROR: ", "").trim());
        }
    }

    public void removeUserFromAllCompanies(String sessionToken, long memberId) throws Exception {
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

    // PURCHASE HISTORY

    public Map<Long, Map<String, List<OrderDTO>>> loadPurchaseHistoryByCompanyAndEvent(String sessionToken) throws Exception {
        long adminId = validateAndGetAdminId(sessionToken);
        
        try {
            return systemAdminService.getPurchaseHistoryByCompanyAndEvent(adminId);
        } catch (IllegalStateException e) {
            // The service throws an IllegalStateException if the history is empty.
            // Catch it and return an empty map so the UI grid simply shows 0 rows without crashing.
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
            return Map.of();
        } catch (Exception e) {
            throw new PresentationException(e.getMessage());
        }
    }

    // SUSPENSIONS

    public void suspendMember(String sessionToken, long memberId, LocalDateTime startDate, LocalDateTime endDate, String reason) throws Exception {
        long adminId = validateAndGetAdminId(sessionToken);
        try {
            systemAdminService.suspendMemberByAdmin(adminId, memberId, startDate, endDate, reason);
        } catch (Exception e) {
            throw new PresentationException(e.getMessage());
        }
    }

    public void revokeSuspension(String sessionToken, long memberId) throws Exception {
        long adminId = validateAndGetAdminId(sessionToken);
        try {
            systemAdminService.revokeMemberByAdmin(adminId, memberId);
        } catch (Exception e) {
            throw new PresentationException(e.getMessage());
        }
    }

    public List<SuspentionUserDTO> viewSuspendedMembers(String sessionToken) throws Exception {
        long adminId = validateAndGetAdminId(sessionToken);
        try {
            return systemAdminService.viewSuspendedMembersByAdmin(adminId);
        } catch (IllegalStateException e) {
            // Return an empty list gracefully if there are no suspended members
            return List.of();
        } catch (Exception e) {
            throw new PresentationException(e.getMessage());
        }
    }

    // SYSTEM LOGS

    public List<String> viewEventLogs(String sessionToken) throws Exception {
        long adminId = validateAndGetAdminId(sessionToken);
        try {
            return systemAdminService.viewEventLogs(adminId);
        } catch (Exception e) {
            throw new PresentationException(e.getMessage());
        }
    }

    public List<String> viewErrorLogs(String sessionToken) throws Exception {
        long adminId = validateAndGetAdminId(sessionToken);
        try {
            return systemAdminService.viewErrorLogs(adminId);
        } catch (Exception e) {
            throw new PresentationException(e.getMessage());
        }
    }
}