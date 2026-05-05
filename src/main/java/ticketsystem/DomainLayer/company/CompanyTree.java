package ticketsystem.DomainLayer.company;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompanyTree {
    private final Map<Long, List<Long>> hierarchy;
    private final Map<Long, String> roles;
    private final long founderId;

    public CompanyTree(long founderId) {
        this.founderId = founderId;
        this.hierarchy = new HashMap<>();
        this.hierarchy.put(founderId, new ArrayList<>());
        this.roles = new HashMap<>();
        this.roles.put(founderId, "FOUNDER"); 
    }

    public void addAppointment(long appointerId, long appointeeId, String role) {
        hierarchy.computeIfAbsent(appointerId, k -> new ArrayList<>()).add(appointeeId);
        hierarchy.putIfAbsent(appointeeId, new ArrayList<>());
        roles.put(appointeeId, role); // Save the role of the new appointee
    }

   // --- Deletion Methods ---
    /**
     * Removes a user from the roles tree and reassigns their appointees to their appointer.
     */
    public void removeNode(long memberIdToRemove) throws Exception {
        // Edge case: Cannot remove the founder
        if (memberIdToRemove == founderId) {
            throw new Exception("Error: Cannot remove the Founder from the company tree."); 
        }

        Long appointerOfDeletedUser = null;

        // 1. Find the parent (appointer) of the user we want to remove
        for (Map.Entry<Long, List<Long>> entry : hierarchy.entrySet()) {
            if (entry.getValue().contains(memberIdToRemove)) {
                appointerOfDeletedUser = entry.getKey();
                // Remove the user from their appointer's list
                entry.getValue().remove(Long.valueOf(memberIdToRemove));
                break;
            }
        }

        // If the user wasn't found in the tree at all, throw an exception
        if (appointerOfDeletedUser == null) {
            throw new Exception("Error: Member ID " + memberIdToRemove + " was not found in the roles tree.");
        }

        // 2. Extract all the appointees (children) of the user to be removed
        List<Long> orphanedAppointees = hierarchy.remove(memberIdToRemove);
        
        // Remove the role mapping of the deleted user
        roles.remove(memberIdToRemove);

        // 3. Reassign the orphaned appointees to the appointer of the deleted user
        if (orphanedAppointees != null && !orphanedAppointees.isEmpty()) {
            hierarchy.get(appointerOfDeletedUser).addAll(orphanedAppointees);
        }
    }
    
    // ---------------------------------

    public String getStructuredData(Map<Long, String> userPermissions) {
        StringBuilder sb = new StringBuilder();
        buildTreeString(founderId, 0, sb, userPermissions);
        return sb.toString();
    }

    private void buildTreeString(long current, int depth, StringBuilder sb, Map<Long, String> userPermissions) {
        for (int i = 0; i < depth; i++) sb.append("  ");
        
        // MODIFIED: Fetch the role from the internal map and add it to the string
        String role = roles.getOrDefault(current, "UNKNOWN");
        sb.append("- ID: ").append(current).append(" (Role: ").append(role).append(")");

        // Add specific permissions from the external map (if any exist)
        if (userPermissions != null && userPermissions.containsKey(current)) {
            sb.append(" [Permissions: ").append(userPermissions.get(current)).append("]");
        }
        sb.append("\n");

        List<Long> appointees = hierarchy.get(current);
        if (appointees != null) {
            for (Long appointee : appointees) {
                buildTreeString(appointee, depth + 1, sb, userPermissions);
            }
        }
    }
}