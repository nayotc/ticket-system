package ticketsystem.PresentationLayer.Presenters;

import org.springframework.stereotype.Component;

import ticketsystem.ApplicationLayer.CompanyService;
import ticketsystem.ApplicationLayer.ITokenService;
import ticketsystem.ApplicationLayer.SystemAdminService;
import ticketsystem.ApplicationLayer.UserService;
import ticketsystem.ApplicationLayer.EventService;
import ticketsystem.DTO.CompanyDTO;
import ticketsystem.DTO.OrderDTO;
import ticketsystem.DTO.SuspentionUserDTO;
import ticketsystem.PresentationLayer.Views.SystemAdminDashboard.AdminUserRow;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SystemAdminPresenter {

    private final SystemAdminService systemAdminService;
    private final UserService userService;
    private final CompanyService companyService;
    private final EventService eventService;
    private final ITokenService tokenService;

    public SystemAdminPresenter(SystemAdminService systemAdminService, 
                                UserService userService, 
                                CompanyService companyService, 
                                ITokenService tokenService,
                                EventService eventService) {
        this.systemAdminService = systemAdminService;
        this.userService = userService;
        this.companyService = companyService;
        this.tokenService = tokenService;
        this.eventService = eventService;
    }

    /**
     * Helper method to securely extract the admin's user ID from the session token.
     */
    private long validateAndGetAdminId(String token) throws PresentationException {
        if (token == null || token.isBlank()) {
            throw new PresentationException("Session token is missing. Please log in.");
        }
        try {
            return tokenService.extractUserId(token);
        } catch (Exception e) {
            throw new PresentationException("Invalid or expired session token.");
        }
    }

    public long getCurrentAdminId(String token) throws PresentationException {
        return validateAndGetAdminId(token);
    }

    // USERS & COMPANIES MANAGEMENT

    public List<AdminUserRow> loadActiveUsers(String token) throws PresentationException {
        try {
            validateAndGetAdminId(token);

            // Fetch all users and map them correctly using Member's domain fields
            return userService.getAllUsers().stream()
                    .map(user -> new AdminUserRow(
                            user.getId(),
                            user.getUserName(), // Using UserName (serves as email)
                            user.getFullName(), // Using FullName
                            user.isSuspended() ? "מושעה" : "פעיל" // Dynamically set status based on suspension
                    ))
                    .collect(Collectors.toList());
        } catch (PresentationException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(e.getMessage());
        } catch (Exception e) {
            throw new PresentationException("Failed to load active users. Please try again.");
        }

    }

    public List<CompanyDTO> loadActiveCompanies(String token) throws PresentationException {
        try {
            validateAndGetAdminId(token);
            return companyService.getAllCompanies();
        } catch (PresentationException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(e.getMessage());
        } catch (Exception e) {
            throw new PresentationException("Failed to load active companies. Please try again.");
        }
    }

    public void deleteUser(String token, long memberId) throws PresentationException {
        long adminId = validateAndGetAdminId(token);
        
        // systemAdminService returns a String message starting with "ERROR" or "SUCCESS"
        String result = systemAdminService.deleteMemberByAdmin(adminId, memberId);
        
        if (result != null && result.startsWith("ERROR")) {
            throw new PresentationException(result.replace("ERROR: ", "").trim());
        }
    }

    public void removeUserFromAllCompanies(String token, long memberId) throws PresentationException {
        try {
            validateAndGetAdminId(token);  
            systemAdminService.removeUserFromAllCompanies(memberId);
        } catch (PresentationException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(e.getMessage());
        } catch (Exception e) {
            throw new PresentationException("Failed to remove user from companies. Please try again.");
        }
    }

    public void closeCompany(String token, long companyId) throws PresentationException {
        try {
            long adminId = validateAndGetAdminId(token);
            systemAdminService.closeProductionCompanyByAdmin(adminId, companyId);
        } catch (PresentationException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(e.getMessage());
        } catch (Exception e) {
            throw new PresentationException("Failed to close company. Please try again.");
        }
    }

    // PURCHASE HISTORY

    public Map<Long, Map<String, List<OrderDTO>>> loadPurchaseHistoryByCompanyAndEvent(String token) throws PresentationException { 
        try {
            long adminId = validateAndGetAdminId(token);
            return systemAdminService.getPurchaseHistoryByCompanyAndEvent(adminId);
        } catch (IllegalStateException e) {
            return Map.of();
        } catch (PresentationException e) {
            throw e;
        } catch (Exception e) {
            throw new PresentationException("Failed to load purchase history by company and event. Please try again.");
        }
    }

    public Map<Long, List<OrderDTO>> loadPurchaseHistoryByBuyer(String token) throws PresentationException {
        try {
            long adminId = validateAndGetAdminId(token);
            return systemAdminService.getPurchaseHistoryByBuyer(adminId);
        } catch (IllegalStateException e) {
            return Map.of();
        } catch (PresentationException e) {
            throw e;
        } catch (Exception e) {
            throw new PresentationException("Failed to load purchase history by buyer. Please try again.");
        }
    }

    public String getEventDateFormatted(String token, long eventId) {
        try {
            validateAndGetAdminId(token);

            var event = eventService.getEvent(token, eventId);
            
            if (event != null && event.date() != null) {
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                return event.date().format(formatter);
            }
            return "לא זמין";
        } catch (Exception e) {
            return "לא זמין";
        }
    }

    // SUSPENSIONS

    public void suspendMember(String token, long memberId, LocalDateTime startDate, LocalDateTime endDate, String reason) throws PresentationException {
        try {
            long adminId = validateAndGetAdminId(token);
            systemAdminService.suspendMemberByAdmin(adminId, memberId, startDate, endDate, reason);
        } catch (PresentationException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(e.getMessage());
        } catch (Exception e) {
            throw new PresentationException("Suspension failed. Please try again.");
        }
    }

    public void revokeSuspension(String token, long memberId) throws PresentationException {
        try {
            long adminId = validateAndGetAdminId(token);
            systemAdminService.revokeMemberByAdmin(adminId, memberId);
        } catch (PresentationException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(e.getMessage());
        } catch (Exception e) {
            throw new PresentationException("Revocation of suspension failed. Please try again.");
        }
    }

    public List<SuspentionUserDTO> viewSuspendedMembers(String token) throws PresentationException {
        try {
            long adminId = validateAndGetAdminId(token);
            return systemAdminService.viewSuspendedMembersByAdmin(adminId);
        } catch (IllegalStateException e) {
            return List.of();
        } catch (PresentationException e) {
            throw e;
        } catch (Exception e) {
            throw new PresentationException("Suspended members retrieval failed. Please try again.");
        }
    }

    // SYSTEM LOGS

    public List<String> viewEventLogs(String token) throws PresentationException {
        try {
            long adminId = validateAndGetAdminId(token);
            return systemAdminService.viewEventLogs(adminId);
        } catch (PresentationException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(e.getMessage());
        } catch (Exception e) {
            throw new PresentationException("Event logs retrieval failed. Please try again.");
        }
    }

    public List<String> viewErrorLogs(String token) throws PresentationException {
        try {
            long adminId = validateAndGetAdminId(token);
            return systemAdminService.viewErrorLogs(adminId);
        } catch (PresentationException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(e.getMessage());
        } catch (Exception e) {
            throw new PresentationException("Error logs retrieval failed. Please try again.");
        }
    }

    public boolean canAccessSystemAdmin(String token) throws PresentationException {
        try {
            long userId = validateAndGetAdminId(token);
            return systemAdminService.isSystemAdmin(userId);
        } catch (PresentationException e) {
            throw e;
        } catch (Exception e) {
            throw new PresentationException("Failed to verify admin access. Please log in again.");
        }
    }
}