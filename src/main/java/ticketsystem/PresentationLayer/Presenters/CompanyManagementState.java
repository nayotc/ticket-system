package ticketsystem.PresentationLayer.Presenters;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import ticketsystem.DomainLayer.user.Permission;

// The main state record encapsulating all the data required for the Company Management view
public record CompanyManagementState(
        List<ManagedCompanyItem> companies,
        ManagedCompanyItem selectedCompany,
        boolean owner,
        boolean canManageTeam,
        List<TeamMemberItem> teamMembers,
        List<EventManagementItem> events,
        CompanyStats stats,
        PolicySummary policySummary
) {

    // UI representation of role types
    public enum RoleType {
        FOUNDER,
        OWNER,
        MANAGER
    }

    // Sub-model representing a managed company
    public record ManagedCompanyItem(long id, String name, long founderId, String founderName, boolean active) {}

    // Sub-model representing an event within the company
    public record EventManagementItem(long eventId, String title) {}

    // Sub-model for company statistics
    public record CompanyStats(int activeEvents, int pendingAssignments) {}

    // Sub-model for policy summaries
    public record PolicySummary(String purchasePolicySummary, String discountPolicySummary) {}

    // Sub-model representing a team member (uses Domain Permissions for Vaadin checkbox rendering)
    public record TeamMemberItem(
            Long memberId,
            String displayName,
            String roleLabel,
            RoleType roleType,
            Set<Permission> permissions,
            boolean removable
    ) {
        // Helper method to format permissions for UI display
        public String permissionLabels() {
            if (permissions == null || permissions.isEmpty()) {
                return "";
            }
            return permissions.stream()
                    .map(permission -> permission.name().replace('_', ' '))
                    .collect(Collectors.joining(", "));
        }
    }
}