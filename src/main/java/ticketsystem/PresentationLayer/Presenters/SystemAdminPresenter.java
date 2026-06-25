package ticketsystem.PresentationLayer.Presenters;

import org.springframework.stereotype.Component;

import ticketsystem.ApplicationLayer.CompanyService;
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

    public SystemAdminPresenter(SystemAdminService systemAdminService, 
                                UserService userService, 
                                CompanyService companyService, 
                                EventService eventService) {
        this.systemAdminService = systemAdminService;
        this.userService = userService;
        this.companyService = companyService;
        this.eventService = eventService;
    }

    public long getCurrentAdminId(String token) throws PresentationException {
        try {
            return systemAdminService.getCurrentAdminId(token);
        
        } catch (Exception e) {
            throw new PresentationException(translateError(extractUsefulMessage(e), PresentationException.SESSION_TOKEN_EXPIRED));
        }
    }

    // USERS & COMPANIES MANAGEMENT

    public List<AdminUserRow> loadActiveUsers(String token) throws PresentationException {
        try {
            if (!systemAdminService.isSystemAdmin(token)) {
                throw new PresentationException("גישה נדחתה: הפעולה מורשית למנהלי מערכת בלבד.");
            }
            return userService.getAllUsers().stream()
                    .map(user -> new AdminUserRow(
                            user.getId(),
                            user.getUserName(), // Using UserName (serves as email)
                            user.getFullName(), // Using FullName
                            user.isSuspended() ? "מושעה" : "פעיל", // Dynamically set status based on suspension
                            user.isActive() // Using isActive flag
                    ))
                    .collect(Collectors.toList());
        
        } catch (PresentationException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(translateError(e.getMessage(), e.getMessage()));
        } catch (Exception e) {
            throw new PresentationException(translateError(extractUsefulMessage(e), "טעינת רשימת המשתמשים נכשלה. אנא נסו שוב."));
        }
    }

    public List<CompanyDTO> loadActiveCompanies(String token) throws PresentationException {
        try {
            if (!systemAdminService.isSystemAdmin(token)) {
                throw new PresentationException("גישה נדחתה: הפעולה מורשית למנהלי מערכת בלבד.");
            }
            return companyService.getAllCompanies();

        } catch (PresentationException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(translateError(e.getMessage(), e.getMessage()));
        } catch (Exception e) {
            throw new PresentationException(translateError(extractUsefulMessage(e), "טעינת רשימת החברות נכשלה. אנא נסו שוב."));
        }
    }

    public void deleteUser(String token, long memberId) throws PresentationException {
        try {
            if (!systemAdminService.isSystemAdmin(token)) {
                throw new PresentationException("גישה נדחתה: הפעולה מורשית למנהלי מערכת בלבד.");
            }
            String result = systemAdminService.deleteMemberByAdmin(token, memberId);
            if (result != null && result.startsWith("ERROR")) {
                throw new PresentationException(result.replace("ERROR: ", "").trim());
            }
        
        } catch (PresentationException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(translateError(e.getMessage(), e.getMessage()));
        } catch (Exception e) {
            throw new PresentationException(translateError(extractUsefulMessage(e), "מחיקת המשתמש נכשלה. אנא נסו שוב."));
        }

    }

    public void removeUserFromAllCompanies(String token, long memberId) throws PresentationException {
        try {
            if (!systemAdminService.isSystemAdmin(token)) {
                throw new PresentationException("גישה נדחתה: הפעולה מורשית למנהלי מערכת בלבד.");
            }
            systemAdminService.removeUserFromAllCompanies(memberId);

        } catch (PresentationException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(translateError(e.getMessage(), e.getMessage()));
        } catch (Exception e) {
            throw new PresentationException(translateError(extractUsefulMessage(e), "הסרת המשתמש מחברות ההפקה נכשלה. אנא נסו שוב."));
        }
    }

    public void closeCompany(String token, long companyId) throws PresentationException {
        try {
            systemAdminService.closeProductionCompanyByAdmin(token, companyId);
        
        } catch (PresentationException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(translateError(e.getMessage(), e.getMessage()));
        } catch (Exception e) {
            throw new PresentationException(translateError(extractUsefulMessage(e), "סגירת חברת ההפקה נכשלה. אנא נסו שוב."));
        }
    }

    // PURCHASE HISTORY

    public Map<Long, Map<String, List<OrderDTO>>> loadPurchaseHistoryByCompanyAndEvent(String token) throws PresentationException { 
        try {
            return systemAdminService.getPurchaseHistoryByCompanyAndEvent(token);
        
        } catch (IllegalStateException e) {
            return Map.of();
        } catch (PresentationException e) {
            throw e;
        } catch (Exception e) {
            throw new PresentationException(translateError(extractUsefulMessage(e), "טעינת היסטוריית הרכישות לפי חברה ואירוע נכשלה. אנא נסו שוב."));
        }
    }

    public Map<Long, List<OrderDTO>> loadPurchaseHistoryByBuyer(String token) throws PresentationException {
        try {
            return systemAdminService.getPurchaseHistoryByBuyer(token);
        
        } catch (IllegalStateException e) {
            return Map.of();
        } catch (PresentationException e) {
            throw e;
        } catch (Exception e) {
            throw new PresentationException(translateError(extractUsefulMessage(e), "טעינת היסטוריית הרכישות לפי משתמשים נכשלה. אנא נסו שוב."));
        }
    }

    public String getEventDateFormatted(String token, long eventId) {
        try {
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
            systemAdminService.suspendMemberByAdmin(token, memberId, startDate, endDate, reason);
        
        } catch (PresentationException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(translateError(e.getMessage(), e.getMessage()));
        } catch (Exception e) {
            throw new PresentationException(translateError(extractUsefulMessage(e), "השעיית המשתמש נכשלה. אנא נסו שוב."));
        }
    }

    public void revokeSuspension(String token, long memberId) throws PresentationException {
        try {
            systemAdminService.revokeMemberByAdmin(token, memberId);
        
        } catch (PresentationException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(translateError(e.getMessage(), e.getMessage()));
        } catch (Exception e) {
            throw new PresentationException(translateError(extractUsefulMessage(e), "ביטול השעיית המשתמש נכשל. אנא נסו שוב."));
        }
    }

    public List<SuspentionUserDTO> viewSuspendedMembers(String token) throws PresentationException {
        try {
            return systemAdminService.viewSuspendedMembersByAdmin(token);
        
        } catch (IllegalStateException e) {
            return List.of();
        } catch (PresentationException e) {
            throw e;
        } catch (Exception e) {
            throw new PresentationException(translateError(extractUsefulMessage(e), "טעינת רשימת המשתמשים המושהים נכשלה. אנא נסו שוב."));
        }
    }

    // SYSTEM LOGS

    public List<String> viewEventLogs(String token) throws PresentationException {
        try {
            return systemAdminService.viewEventLogs(token);
        
        } catch (PresentationException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(translateError(e.getMessage(), e.getMessage()));
        } catch (Exception e) {
            throw new PresentationException(translateError(extractUsefulMessage(e), "טעינת יומני האירועים במערכת נכשלה. אנא נסו שוב."));
        }
    }

    public List<String> viewErrorLogs(String token) throws PresentationException {
        try {
            return systemAdminService.viewErrorLogs(token);
        
        } catch (PresentationException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(translateError(e.getMessage(), e.getMessage()));
        } catch (Exception e) {
            throw new PresentationException(translateError(extractUsefulMessage(e), "טעינת יומני השגיאות במערכת נכשלה. אנא נסו שוב."));
        }
    }

    public boolean canAccessSystemAdmin(String token) throws PresentationException {
        try {
            return systemAdminService.isSystemAdmin(token);
        
        } catch (Exception e) {
            throw new PresentationException(translateError(extractUsefulMessage(e), "אימות הרשאות מנהל המערכת נכשל. אנא התחברו מחדש."));
        }
    }

    private String translateError(String message, String fallbackMessage) {
        if (message == null || message.isBlank()) {
            return fallbackMessage;
        }

        String cleanMessage = message.trim();

        if (PresentationException.isDbDisconnectMessage(cleanMessage)) {
            return PresentationException.DB_DISCONNECT_HEBREW_MSG;
        }

        if (PresentationException.isSessionTimeoutMessage(cleanMessage)) {
            return PresentationException.SESSION_TOKEN_EXPIRED;
        }

        return switch (cleanMessage) {
            case "Failed to verify admin access. Please log in again." ->
                    PresentationException.SESSION_TOKEN_EXPIRED;

            case "גישה נדחתה: הפעולה מורשית למנהלי מערכת בלבד." ->
                    cleanMessage;

            case "Failed to load active users. Please try again." ->
                    "טעינת רשימת המשתמשים נכשלה. אנא נסו שוב.";

            case "Failed to load active companies. Please try again." ->
                    "טעינת רשימת החברות נכשלה. אנא נסו שוב.";

            case "Failed to delete user. Please try again." ->
                    "מחיקת המשתמש נכשלה. אנא נסו שוב.";

            case "Failed to remove user from companies. Please try again." ->
                    "הסרת המשתמש מחברות ההפקה נכשלה. אנא נסו שוב.";

            case "Failed to close company. Please try again." ->
                    "סגירת חברת ההפקה נכשלה. אנא נסו שוב.";

            case "Failed to load purchase history by company and event. Please try again.",
                 "Failed to load purchase history by buyer. Please try again." ->
                    "טעינת היסטוריית הרכישות נכשלה. אנא נסו שוב.";

            case "Suspension failed. Please try again." ->
                    "השעיית המשתמש נכשלה. אנא נסו שוב.";

            case "Revocation of suspension failed. Please try again." ->
                    "ביטול השעיית המשתמש נכשל. אנא נסו שוב.";

            case "Suspended members retrieval failed. Please try again." ->
                    "טעינת רשימת המשתמשים המושהים נכשלה. אנא נסו שוב.";

            case "Event logs retrieval failed. Please try again." ->
                    "טעינת יומני האירועים במערכת נכשלה. אנא נסו שוב.";

            case "Error logs retrieval failed. Please try again." ->
                    "טעינת יומני השגיאות במערכת נכשלה. אנא נסו שוב.";

            default -> fallbackMessage;
        };
    }

    /**
     * חולצת את השגיאה המקורית ממעמקי ה-Stack Trace כדי לזהות ניתוקי DB
     */
    private String extractUsefulMessage(Exception exception) {
        if (exception == null) {
            return "";
        }

        Throwable current = exception;
        while (current.getCause() != null) {
            current = current.getCause();
        }

        if (current.getMessage() != null && !current.getMessage().isBlank()) {
            return current.getMessage();
        }

        return exception.getMessage() != null ? exception.getMessage() : "";
    }

}