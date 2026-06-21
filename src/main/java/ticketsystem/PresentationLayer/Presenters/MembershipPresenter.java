package ticketsystem.PresentationLayer.Presenters;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import ticketsystem.ApplicationLayer.CompanyService;
import ticketsystem.ApplicationLayer.MembershipService;
import ticketsystem.ApplicationLayer.UserService;
import ticketsystem.ApplicationLayer.EventService;
import ticketsystem.DTO.CompanyDTO;
import ticketsystem.DTO.CompanyRoleDTO;
import ticketsystem.DTO.MemberDTO;
import ticketsystem.DTO.Event.EventDTO;
import ticketsystem.DomainLayer.user.Permission;

import ticketsystem.PresentationLayer.Presenters.CompanyManagementState.CompanyStats;
import ticketsystem.PresentationLayer.Presenters.CompanyManagementState.ManagedCompanyItem;
import ticketsystem.PresentationLayer.Presenters.CompanyManagementState.EventManagementItem;
import ticketsystem.PresentationLayer.Presenters.CompanyManagementState.TeamMemberItem;
import ticketsystem.PresentationLayer.Presenters.CompanyManagementState.PolicySummary;
import ticketsystem.PresentationLayer.Presenters.CompanyManagementState.RoleType;

@Component
public class MembershipPresenter {
    
    private final MembershipService membershipService;
    private final UserService userService;
    private final CompanyService companyService;
    private final EventService eventService;

    public MembershipPresenter(MembershipService membershipService, UserService userService, CompanyService companyService, EventService eventService) {
        this.membershipService = membershipService;
        this.userService = userService;
        this.companyService = companyService;
        this.eventService = eventService;
    }

    public CompanyManagementState loadCompanyManagement(String sessionToken, Long requestedCompanyId) {
        try {
            // 1. Validate token and extract current user ID
            Long currentUserId = membershipService.getCurrentUserId(sessionToken);
            if (currentUserId == null) {
                throw new PresentationException("פג תוקף החיבור של המשתמש. אנא התחבר מחדש.");
            }

            // 2. Fetch companies the user is a member of from the application layer
            List<CompanyDTO> userCompanies = membershipService.getCompaniesByMember(sessionToken);
            
            if (userCompanies.isEmpty()) {
                return new CompanyManagementState(
                        List.of(), null, false, false, false, false, false, List.of(), List.of(), 
                        new CompanyStats(0, 0), null
                );
            }

            // 3. Set selected company (if no specific ID is provided, default to the first in the list)
            CompanyDTO selectedCompanyDto = userCompanies.stream()
                    .filter(c -> requestedCompanyId != null && c.getId() == requestedCompanyId)
                    .findFirst()
                    .orElse(userCompanies.get(0));

            Long companyId = selectedCompanyDto.getId();

            String founderName = userService.getUserNameById(selectedCompanyDto.getFounderId());

            // Convert company DTOs to ManagedCompanyItem list for the UI menu
            List<ManagedCompanyItem> managedCompanies = userCompanies.stream()
                    .map(dto -> new ManagedCompanyItem(
                            dto.getId(),
                            dto.getName(),
                            dto.getFounderId(),
                            founderName != null ? founderName : "מייסד (ללא אימייל)", 
                            dto.isActive()
                    ))
                    .collect(Collectors.toList());

            ManagedCompanyItem selectedCompanyItem = managedCompanies.stream()
                    .filter(c -> c.id() == companyId)
                    .findFirst()
                    .orElse(managedCompanies.get(0));

            // 4. Fetch team members of the selected company from the application layer (now receiving DTOs)
            List<MemberDTO> teamMembersDto = membershipService.getCompanyTeamMembers(sessionToken, companyId);
            List<TeamMemberItem> uiTeamMembers = new ArrayList<>();

            boolean isCurrentUserFounder = false;
            boolean isCurrentUserOwner = false;
            boolean canCurrentUserManageTeam = false;
            boolean canManageEvents = false;
            boolean canManagePolicies = false;

            // 5. Polymorphic mapping of team members from DTO
            for (MemberDTO member : teamMembersDto) {
                CompanyRoleDTO role = member.getRoles().stream()
                        .filter(r -> r.getCompanyId().equals(companyId))
                        .findFirst()
                        .orElse(null);

                if (role == null || "CANCELLED".equals(role.getStatus())) {
                    continue; // Skip cancelled roles
                }

                RoleType uiRoleType;
                String roleLabel;
                Set<Permission> uiPermissions = Set.of();

                if ("FOUNDER".equals(role.getRoleType())) {
                    uiRoleType = RoleType.FOUNDER;
                    roleLabel = "Founder";
                } else if ("OWNER".equals(role.getRoleType())) {
                    uiRoleType = RoleType.OWNER;
                    roleLabel = "Owner";
                } else if ("MANAGER".equals(role.getRoleType())) {
                    uiRoleType = RoleType.MANAGER;
                    roleLabel = "Manager";
                    
                    if (role.getPermissions() != null) {
                        uiPermissions = role.getPermissions().stream()
                                .map(permStr -> {
                                    java.util.Optional<Permission> opt = Permission.fromKey(permStr);
                                    if (opt.isPresent()) {
                                        return opt.get();
                                    }
                                    
                                    for (Permission p : Permission.values()) {
                                        if (p.name().equals(permStr) || p.toString().equals(permStr)) {
                                            return p;
                                        }
                                    }
                                    return null; // If the permission wasn't recognized at all
                                })
                                .filter(p -> p != null) // Filter out nulls to prevent UI crashes
                                .collect(Collectors.toSet());
                    }
                } else {
                    continue;
                }

                // Check current logged-in user status to set access flags (founder, owner, canManageTeam)
                if (member.getMemberId().equals(currentUserId)) {
                    if (uiRoleType == RoleType.FOUNDER) {
                        isCurrentUserFounder = true;
                        isCurrentUserOwner = true;
                        canCurrentUserManageTeam = true;
                        canManageEvents = true;
                        canManagePolicies = true;
                    } else if (uiRoleType == RoleType.OWNER) {
                        isCurrentUserOwner = true;
                        canCurrentUserManageTeam = true;
                        canManageEvents = true;
                        canManagePolicies = true;
                    } else if (uiRoleType == RoleType.MANAGER) {
                        canManageEvents = uiPermissions.contains(Permission.MANAGE_EVENT_INVENTORY);
                        canManagePolicies = uiPermissions.contains(Permission.SET_PURCHASING_POLICY) || 
                                                        uiPermissions.contains(Permission.SET_DISCOUNT_POLICY);
                    }
                }

                // Determine if the current user is the appointer of this team member (hierarchy rules for removal)
                boolean removable = false;
                if (role.getAppointedByMemberId() != null && role.getAppointedByMemberId().equals(currentUserId)) {
                    removable = true;
                }

                uiTeamMembers.add(new TeamMemberItem(
                        member.getMemberId(),
                        member.getUserName(), 
                        roleLabel,
                        uiRoleType,
                        uiPermissions,
                        removable
                ));
            }

            // 6. Fetch company events and map them via EventService
            List<EventDTO> domainEvents = eventService.getEventsByCompany(sessionToken, companyId);

            List<EventManagementItem> uiEvents = domainEvents.stream()
                    .map(e -> new EventManagementItem(
                            e.id(),
                            e.name(), 
                            e.status() != null ? e.status() : "פעיל" 
                    )) 
                    .collect(Collectors.toList());
            
            // 7. Build statistics and policy summaries
            int activeEventsCount = (int) domainEvents.stream()
                    // Check if the status field exists and equals the string "ACTIVE"
                    .filter(e -> e.status() != null && e.status().equals("ACTIVE")) 
                    .count();
                    
            int pendingAssignmentsCount = membershipService.getPendingAssignmentsCount(companyId);
            CompanyStats uiStats = new CompanyStats(activeEventsCount, pendingAssignmentsCount);

            String purchasePolicy = companyService.getPurchasePolicySummary(companyId);
            String discountPolicy = companyService.getDiscountPolicySummary(companyId);
            PolicySummary uiPolicySummary = new PolicySummary(purchasePolicy, discountPolicy);

            // 8. Return the complete, structured state directly to the View
            return new CompanyManagementState(
                    managedCompanies,
                    selectedCompanyItem,
                    isCurrentUserFounder,
                    isCurrentUserOwner,
                    canCurrentUserManageTeam,
                    canManageEvents,
                    canManagePolicies,
                    uiTeamMembers,
                    uiEvents,
                    uiStats,
                    uiPolicySummary
            );

        } catch (PresentationException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw presentationException(e.getMessage());
        } catch (Exception e) {
            throw presentationException(extractUsefulMessage(e));
        }
    }

    public void requestManagerAssignment(String sessionToken, Long companyId, String targetName, Set<Permission> permissions) {
        try {
            boolean success = membershipService.requestManagerAssignment(sessionToken, companyId, targetName, permissions);
            if (!success) {
                throw new PresentationException("בקשת מינוי המנהל נכשלה.");
            }
        } catch (PresentationException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw presentationException(e.getMessage());
        } catch (Exception e) {
            throw presentationException(extractUsefulMessage(e));
        }
    }

    public void requestOwnerAssignment(String sessionToken, Long companyId, String targetName) {
        try {
            boolean success = membershipService.requestOwnerAssignment(sessionToken, companyId, targetName);
            if (!success) {
                throw new PresentationException("בקשת מינוי הבעלים נכשלה.");
            }
        } catch (PresentationException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw presentationException(e.getMessage());
        } catch (Exception e) {
            throw presentationException(extractUsefulMessage(e));
        }
    }

    public void updateManagerPermissions(String sessionToken, Long companyId, String targetName, Set<Permission> permissions) {
        try {
            boolean success = membershipService.updateManagerPermissions(sessionToken, companyId, targetName, permissions);
            if (!success) {
                throw new PresentationException("עדכון ההרשאות נכשל.");
            }
        } catch (PresentationException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw presentationException(e.getMessage());
        } catch (Exception e) {
            throw presentationException(extractUsefulMessage(e));
        }
    }

    public void removeManagerAssignment(String sessionToken, Long companyId, Long targetMemberId) {
        try {
            boolean success = membershipService.removeManagerAssignment(sessionToken, companyId, targetMemberId);
            if (!success) {
                throw new PresentationException("הסרת המנהל נכשלה.");
            }
        } catch (PresentationException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw presentationException(e.getMessage());
        } catch (Exception e) {
            throw presentationException(extractUsefulMessage(e));
        }
    }

    public void removeOwnerAssignment(String sessionToken, Long companyId, Long targetMemberId) {
        try {
            boolean success = membershipService.removeOwnerAssignment(sessionToken, companyId, targetMemberId);
            if (!success) {
                throw new PresentationException("הסרת הבעלים נכשלה.");
            }
        } catch (PresentationException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw presentationException(e.getMessage());
        } catch (Exception e) {
            throw presentationException(extractUsefulMessage(e));
        }
    }

    public void giveUpOwnership(String sessionToken, Long companyId) {
        try {
            boolean success = membershipService.giveUpOwnership(sessionToken, companyId);
            if (!success) {
                throw new PresentationException("ויתור הבעלות נכשל.");
            }
        } catch (PresentationException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw presentationException(e.getMessage());
        } catch (Exception e) {
            throw presentationException(extractUsefulMessage(e));
        }
    }

    public void cancelEvent(String sessionToken, Long companyId, Long eventId) {
        try {
             boolean success = eventService.cancelEvent(sessionToken, eventId);
            if (!success) {
                throw new PresentationException("ביטול אירוע טרם מומש.");
            }
        } catch (PresentationException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw presentationException(e.getMessage());
        } catch (Exception e) {
            throw presentationException(extractUsefulMessage(e));
        }
    }

    public void approveAssignment(String sessionToken, Long companyId) {
        try {
            boolean success = membershipService.approveAssignment(sessionToken, companyId);
            if (!success) {
                throw new PresentationException("אישור המינוי נכשל.");
            }
        } catch (PresentationException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw presentationException(e.getMessage());
        } catch (Exception e) {
            throw presentationException(extractUsefulMessage(e));
        }
    }

    public void rejectAssignment(String sessionToken, Long companyId) {
        try {
            boolean success = membershipService.rejectAssignment(sessionToken, companyId);
            if (!success) {
                throw new PresentationException("דחיית המינוי נכשלה.");
            }
        } catch (PresentationException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw presentationException(e.getMessage());
        } catch (Exception e) {
            throw presentationException(extractUsefulMessage(e));
        }
    }

    private PresentationException presentationException(String message) {
        return new PresentationException(translateError(message));
    }

    private String translateError(String message) {
        String cleanMessage = cleanErrorMessage(message);

        if (cleanMessage == null || cleanMessage.isBlank()) {
            return "אירעה שגיאה. נסו שוב.";
        }
        
        if (message != null && (
                message.contains("JWT") ||
                message.contains("expired") ||
                message.contains("Invalid") ||
                message.contains("Invalid session ID") ||
                message.contains("Token is missing or null") ||
                message.contains("Session is no longer active") ||
                message.contains("Invalid or expired security token")
        )) {
            return message;
        }

        return switch (cleanMessage) {
            case "Session authentication failed.",
                 "Invalid session ID",
                 "Invalid token.",
                 "Member ID not found in token." ->
                    "פג תוקף החיבור. יש להתחבר מחדש.";

            case "Appointer not found." ->
                    "המשתמש שמבצע את המינוי לא נמצא במערכת.";

            case "Target Member not found." ->
                    "המשתמש שברצונך למנות לא נמצא במערכת.";

            case "Target Manager not found." ->
                    "המנהל שברצונך לעדכן לא נמצא במערכת.";

            case "Company not found.",
                 "Error: Company not found." ->
                    "החברה לא נמצאה.";

            case "This user already has an active or pending role in this company" ->
                    "למשתמש הזה כבר יש תפקיד פעיל או בקשת מינוי ממתינה בחברה הזו.";

            case "The appointer ID could not be determined." ->
                    "לא ניתן לזהות מי יצר את בקשת המינוי.";

            case "Appointee not found." ->
                    "המשתמש שקיבל את בקשת המינוי לא נמצא.";

            case "User does not have permission to assign manager.",
                 "User does not have permission to assign owner.",
                 "User does not have permission to manage team.",
                 "User does not have permission." ->
                    "אין לך הרשאה לבצע את המינוי הזה.";

            case "Cannot assign role to yourself.",
                 "User cannot assign himself." ->
                    "לא ניתן למנות את עצמך לתפקיד הזה.";

            case "No pending assignment found.",
                 "No pending role assignment found." ->
                    "לא נמצאה בקשת מינוי שממתינה לאישור.";
            case "Event cancellation failed. Please try again later to complete the cancellation process." -> "ביטול אירוע לא הושלם, נסה מאוחר יותר להשלים את הפעולה"; 

            default -> translateDynamicError(cleanMessage);
        };
    }

    private String translateDynamicError(String message) {
        if (message.startsWith("Target member with name '") && message.endsWith("' not found.")) {
            String userName = extractBetween(message, "Target member with name '", "' not found.");
            return "המשתמש \"" + userName + "\" לא נמצא במערכת.";
        }

        if (message.startsWith("Manager with name '") && message.endsWith("' not found.")) {
            String userName = extractBetween(message, "Manager with name '", "' not found.");
            return "המנהל \"" + userName + "\" לא נמצא במערכת.";
        }

        if (message.contains("already has an active or pending role")) {
            return "למשתמש הזה כבר יש תפקיד פעיל או בקשת מינוי ממתינה בחברה הזו.";
        }

        if (message.contains("permission")) {
            return "אין לך הרשאה לבצע את הפעולה הזו.";
        }

        if (message.contains("is the only active owner in the company")) {
            return "לא ניתן להסיר: משתמש זה הוא הבעלים הפעיל היחיד שנותר בחברה.";
        }

        return message;
    }

    private String extractUsefulMessage(Exception exception) {
        if (exception == null) {
            return null;
        }

        Throwable current = exception;

        while (current.getCause() != null) {
            current = current.getCause();
        }

        if (current.getMessage() != null && !current.getMessage().isBlank()) {
            return current.getMessage();
        }

        return exception.getMessage();
    }

    private String cleanErrorMessage(String message) {
        if (message == null) {
            return null;
        }

        String clean = message.trim();

        clean = removePrefix(clean, "An error occurred while requesting manager assignment: ");
        clean = removePrefix(clean, "An error occurred while requesting owner assignment: ");
        clean = removePrefix(clean, "An error occurred while updating manager permissions: ");
        clean = removePrefix(clean, "An error occurred while removing manager assignment: ");
        clean = removePrefix(clean, "An error occurred while removing owner assignment: ");
        clean = removePrefix(clean, "An error occurred while approving assignment: ");
        clean = removePrefix(clean, "An error occurred while rejecting assignment: ");
        clean = removePrefix(clean, "An error occurred while retrieving companies for member: ");
        clean = removePrefix(clean, "An error occurred while retrieving company team members: ");

        return clean.trim();
    }

    private String removePrefix(String value, String prefix) {
        if (value == null || prefix == null) {
            return value;
        }

        if (value.startsWith(prefix)) {
            return value.substring(prefix.length());
        }

        return value;
    }

    private String extractBetween(String value, String prefix, String suffix) {
        if (value == null || prefix == null || suffix == null) {
            return "";
        }

        int start = value.indexOf(prefix);
        if (start < 0) {
            return "";
        }

        start += prefix.length();

        int end = value.indexOf(suffix, start);
        if (end < 0 || end <= start) {
            return "";
        }

        return value.substring(start, end);
    }
}